package io.github.jitshen.brickbreaker.model;

import static io.github.jitshen.brickbreaker.model.GameConfig.BALL_MAX_SPEED;
import static io.github.jitshen.brickbreaker.model.GameConfig.BALL_RADIUS;
import static io.github.jitshen.brickbreaker.model.GameConfig.BALL_SPEED_UP;
import static io.github.jitshen.brickbreaker.model.GameConfig.BALL_START_SPEED;
import static io.github.jitshen.brickbreaker.model.GameConfig.HEIGHT;
import static io.github.jitshen.brickbreaker.model.GameConfig.MAX_LIVES;
import static io.github.jitshen.brickbreaker.model.GameConfig.MIN_VERTICAL_RATIO;
import static io.github.jitshen.brickbreaker.model.GameConfig.MULTI_BALL_SPREAD;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_SPEED;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_WIDE_WIDTH;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_WIDTH;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_Y;
import static io.github.jitshen.brickbreaker.model.GameConfig.POWER_UP_DROP_CHANCE;
import static io.github.jitshen.brickbreaker.model.GameConfig.SLOW_DURATION;
import static io.github.jitshen.brickbreaker.model.GameConfig.SLOW_FACTOR;
import static io.github.jitshen.brickbreaker.model.GameConfig.START_LIVES;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDE_DURATION;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDTH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * The game world and its rules. Call {@link #update} once per fixed time step; it returns what
 * happened during that step so the UI can react.
 */
public final class Game {

    /** Longest step simulated at once, so a stalled frame can't fling things across the screen. */
    static final double MAX_STEP = 0.25;
    /** Most contacts resolved for one ball in one step. */
    private static final int MAX_CONTACTS_PER_STEP = 4;
    /** Distance a ball is pushed off a surface after contact so it doesn't touch it again. */
    private static final double SKIN = 1e-6;

    private static final double WALL = 1_000;
    private static final List<Rect> WALLS = List.of(
            new Rect(-WALL, -WALL, WALL, HEIGHT + 2 * WALL), // left
            new Rect(WIDTH, -WALL, WALL, HEIGHT + 2 * WALL), // right
            new Rect(-WALL, -WALL, WIDTH + 2 * WALL, WALL)); // top

    private enum Surface { WALL, PADDLE, BRICK }

    private record Contact(Collision.Hit hit, Surface surface, Brick brick) {}

    private final List<Level> levels;
    private final RandomGenerator random;
    private final Paddle paddle = new Paddle(WIDTH / 2);
    private final List<Ball> balls = new ArrayList<>();
    private final List<Brick> bricks = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();

    private GameState state;
    private GameState stateBeforePause;
    private int lives;
    private int score;
    private int levelIndex;
    private double wideTimeLeft;
    private double slowTimeLeft;

    public Game(List<Level> levels, RandomGenerator random) {
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("A game needs at least one level");
        }
        this.levels = List.copyOf(levels);
        this.random = random;
        restart();
    }

    /** Starts again from the first level with full lives and no score. */
    public void restart() {
        lives = START_LIVES;
        score = 0;
        loadLevel(0);
    }

    public void togglePause() {
        if (state == GameState.PAUSED) {
            state = stateBeforePause;
        } else if (state == GameState.PLAYING || state == GameState.SERVING) {
            stateBeforePause = state;
            state = GameState.PAUSED;
        }
    }

    public List<GameEvent> update(double dt, InputState input) {
        double step = Math.min(dt, MAX_STEP);
        List<GameEvent> events = new ArrayList<>();
        switch (state) {
            case SERVING -> serve(step, input, events);
            case PLAYING -> play(step, input, events);
            case LEVEL_CLEARED -> {
                if (input.launch()) {
                    loadLevel(levelIndex + 1);
                }
            }
            case PAUSED, GAME_OVER, WON -> {
                // Nothing moves until the player resumes or restarts.
            }
        }
        return events;
    }

    private void serve(double dt, InputState input, List<GameEvent> events) {
        movePaddle(dt, input);
        Ball ball = balls.getFirst();
        ball.setPosition(servePosition());
        if (input.launch()) {
            Vec2 direction = paddle.bounceDirection(paddle.centerX() + paddle.width() * 0.1);
            ball.setVelocity(direction.scale(BALL_START_SPEED));
            state = GameState.PLAYING;
            events.add(new GameEvent.BallLaunched());
        }
    }

    private void play(double dt, InputState input, List<GameEvent> events) {
        movePaddle(dt, input);
        tickTimers(dt);
        double ballDt = slowTimeLeft > 0 ? dt * SLOW_FACTOR : dt;
        for (Ball ball : List.copyOf(balls)) {
            moveBall(ball, ballDt, events);
            if (ball.position().y() - ball.radius() > HEIGHT) {
                balls.remove(ball);
            }
        }
        updatePowerUps(dt, events);
        if (balls.isEmpty()) {
            loseLife(events);
        } else if (bricks.stream().noneMatch(b -> b.type().isBreakable())) {
            finishLevel(events);
        }
    }

    private void movePaddle(double dt, InputState input) {
        if (input.pointerX().isPresent()) {
            paddle.moveTo(input.pointerX().getAsDouble());
        } else if (input.left() != input.right()) {
            paddle.moveBy((input.left() ? -1 : 1) * PADDLE_SPEED * dt);
        }
    }

    /** Moves a ball for one step, bouncing off everything it touches along the way. */
    private void moveBall(Ball ball, double dt, List<GameEvent> events) {
        // The paddle can jump (mouse) or slide under a ball that is already touching its top.
        // A swept test can't see a contact that starts overlapped, so catch that case here.
        if (canHitPaddle(ball) && paddle.bounds().expand(ball.radius()).contains(ball.position())) {
            Contact touching = new Contact(new Collision.Hit(0, new Vec2(0, -1)), Surface.PADDLE, null);
            bounce(ball, touching, events);
        }
        double remaining = 1.0;
        for (int i = 0; i < MAX_CONTACTS_PER_STEP && remaining > 0; i++) {
            Vec2 delta = ball.velocity().scale(dt * remaining);
            Optional<Contact> contact = earliestContact(ball, delta);
            if (contact.isEmpty()) {
                ball.setPosition(ball.position().plus(delta));
                return;
            }
            Collision.Hit hit = contact.get().hit();
            ball.setPosition(ball.position()
                    .plus(delta.scale(hit.time()))
                    .plus(hit.normal().scale(SKIN)));
            remaining *= 1 - hit.time();
            bounce(ball, contact.get(), events);
        }
    }

    private Optional<Contact> earliestContact(Ball ball, Vec2 delta) {
        Contact best = null;
        for (Rect wall : WALLS) {
            best = earlier(best, ball, delta, wall, Surface.WALL, null);
        }
        if (canHitPaddle(ball)) {
            best = earlier(best, ball, delta, paddle.bounds(), Surface.PADDLE, null);
        }
        for (Brick brick : bricks) {
            best = earlier(best, ball, delta, brick.bounds(), Surface.BRICK, brick);
        }
        return Optional.ofNullable(best);
    }

    private static Contact earlier(
            Contact best, Ball ball, Vec2 delta, Rect rect, Surface surface, Brick brick) {
        Optional<Collision.Hit> hit = Collision.sweep(ball.position(), delta, ball.radius(), rect);
        if (hit.isPresent() && (best == null || hit.get().time() < best.hit().time())) {
            return new Contact(hit.get(), surface, brick);
        }
        return best;
    }

    /** Only a falling ball that is still above the paddle can bounce off it. */
    private boolean canHitPaddle(Ball ball) {
        return ball.velocity().y() > 0 && ball.position().y() < PADDLE_Y;
    }

    private void bounce(Ball ball, Contact contact, List<GameEvent> events) {
        switch (contact.surface()) {
            case WALL -> {
                ball.setVelocity(steepen(ball.velocity().reflect(contact.hit().normal())));
                events.add(new GameEvent.WallHit());
            }
            case PADDLE -> {
                double speed = Math.min(ball.speed() + BALL_SPEED_UP, BALL_MAX_SPEED);
                ball.setVelocity(paddle.bounceDirection(ball.position().x()).scale(speed));
                events.add(new GameEvent.PaddleHit());
            }
            case BRICK -> {
                Brick brick = contact.brick();
                ball.setVelocity(steepen(ball.velocity().reflect(contact.hit().normal())));
                events.add(new GameEvent.BrickHit(brick));
                if (brick.hit()) {
                    bricks.remove(brick);
                    score += brick.type().score();
                    events.add(new GameEvent.BrickDestroyed(brick));
                    maybeDropPowerUp(brick, events);
                }
            }
        }
    }

    private void maybeDropPowerUp(Brick brick, List<GameEvent> events) {
        if (random.nextDouble() >= POWER_UP_DROP_CHANCE) {
            return;
        }
        PowerUpType[] types = PowerUpType.values();
        PowerUpType type = types[random.nextInt(types.length)];
        PowerUp powerUp = new PowerUp(type, new Vec2(brick.bounds().centerX(), brick.bounds().centerY()));
        powerUps.add(powerUp);
        events.add(new GameEvent.PowerUpSpawned(powerUp));
    }

    private void updatePowerUps(double dt, List<GameEvent> events) {
        for (PowerUp powerUp : List.copyOf(powerUps)) {
            powerUp.fall(dt);
            if (powerUp.bounds().intersects(paddle.bounds())) {
                powerUps.remove(powerUp);
                apply(powerUp.type());
                events.add(new GameEvent.PowerUpCollected(powerUp.type()));
            } else if (powerUp.bounds().minY() > HEIGHT) {
                powerUps.remove(powerUp);
            }
        }
    }

    private void apply(PowerUpType type) {
        switch (type) {
            case WIDE_PADDLE -> {
                wideTimeLeft = WIDE_DURATION;
                paddle.setWidth(PADDLE_WIDE_WIDTH);
            }
            case MULTI_BALL -> {
                if (balls.isEmpty()) {
                    return; // the last ball fell out this step; the life is lost anyway
                }
                // Copy the ball furthest from the bottom, so the copies aren't already lost.
                Ball original = balls.stream()
                        .min(Comparator.comparingDouble(b -> b.position().y()))
                        .orElseThrow();
                for (double angle : new double[] {-MULTI_BALL_SPREAD, MULTI_BALL_SPREAD}) {
                    Vec2 velocity = steepen(original.velocity().rotate(angle));
                    balls.add(new Ball(original.position(), velocity));
                }
            }
            case SLOW_BALL -> slowTimeLeft = SLOW_DURATION;
            case EXTRA_LIFE -> lives = Math.min(lives + 1, MAX_LIVES);
        }
    }

    private void tickTimers(double dt) {
        slowTimeLeft = Math.max(0, slowTimeLeft - dt);
        if (wideTimeLeft > 0) {
            wideTimeLeft -= dt;
            if (wideTimeLeft <= 0) {
                wideTimeLeft = 0;
                paddle.setWidth(PADDLE_WIDTH);
            }
        }
    }

    /** Removes falling power-ups and ends active effects, e.g. after a lost life. */
    private void clearPowerUps() {
        powerUps.clear();
        wideTimeLeft = 0;
        slowTimeLeft = 0;
        paddle.setWidth(PADDLE_WIDTH);
    }

    /** Keeps the ball from settling into a near-horizontal path that never comes back down. */
    private static Vec2 steepen(Vec2 velocity) {
        double speed = velocity.length();
        double minVertical = MIN_VERTICAL_RATIO * speed;
        if (Math.abs(velocity.y()) >= minVertical) {
            return velocity;
        }
        double vy = velocity.y() > 0 ? minVertical : -minVertical;
        double vx = Math.copySign(Math.sqrt(speed * speed - vy * vy), velocity.x());
        return new Vec2(vx, vy);
    }

    private void loseLife(List<GameEvent> events) {
        lives--;
        clearPowerUps();
        events.add(new GameEvent.LifeLost());
        if (lives == 0) {
            state = GameState.GAME_OVER;
            events.add(new GameEvent.GameOver());
        } else {
            resetBall();
        }
    }

    private void finishLevel(List<GameEvent> events) {
        if (levelIndex == levels.size() - 1) {
            state = GameState.WON;
            events.add(new GameEvent.GameWon());
        } else {
            state = GameState.LEVEL_CLEARED;
            events.add(new GameEvent.LevelCleared(levelIndex));
        }
    }

    private void loadLevel(int index) {
        levelIndex = index;
        bricks.clear();
        bricks.addAll(levels.get(index).createBricks());
        clearPowerUps();
        paddle.moveTo(WIDTH / 2);
        resetBall();
    }

    private void resetBall() {
        balls.clear();
        balls.add(new Ball(servePosition(), Vec2.ZERO));
        state = GameState.SERVING;
    }

    private Vec2 servePosition() {
        return new Vec2(paddle.centerX(), PADDLE_Y - BALL_RADIUS - 1);
    }

    public GameState state() {
        return state;
    }

    public Paddle paddle() {
        return paddle;
    }

    public List<Ball> balls() {
        return Collections.unmodifiableList(balls);
    }

    /** Bricks still standing. */
    public List<Brick> bricks() {
        return Collections.unmodifiableList(bricks);
    }

    /** Power-ups currently falling. */
    public List<PowerUp> powerUps() {
        return Collections.unmodifiableList(powerUps);
    }

    /** Seconds left on the wide-paddle power-up, or 0 if it isn't active. */
    public double wideTimeLeft() {
        return wideTimeLeft;
    }

    /** Seconds left on the slow-ball power-up, or 0 if it isn't active. */
    public double slowTimeLeft() {
        return slowTimeLeft;
    }

    public int lives() {
        return lives;
    }

    public int score() {
        return score;
    }

    public int levelIndex() {
        return levelIndex;
    }

    public int levelCount() {
        return levels.size();
    }
}
