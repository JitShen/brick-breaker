package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class PowerUpTest {

    private static final InputState LAUNCH = new InputState(false, false, OptionalDouble.empty(), true);

    /** Random source that always drops a power-up of the given type. */
    private static RandomGenerator alwaysDrops(PowerUpType type) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return 0;
            }

            @Override
            public double nextDouble() {
                return 0;
            }

            @Override
            public int nextInt(int bound) {
                return type.ordinal();
            }
        };
    }

    /** A launched game whose level has four full-width rows of one-hit bricks. */
    private static Game game(PowerUpType type) {
        Level level = LevelLoader.parse("rows", List.of("1", "1", "1", "1"));
        Game game = new Game(List.of(level), alwaysDrops(type));
        game.update(0.001, LAUNCH);
        return game;
    }

    private static Ball ball(Game game) {
        return game.balls().getFirst();
    }

    /** Fires the ball at the lowest brick, then parks it so it can't be lost. */
    private static List<GameEvent> breakBrick(Game game) {
        ball(game).setPosition(new Vec2(400, 200));
        ball(game).setVelocity(new Vec2(0, -600));
        List<GameEvent> events = game.update(0.25, InputState.NONE);
        park(game);
        return events;
    }

    private static void park(Game game) {
        for (Ball ball : game.balls()) {
            ball.setPosition(new Vec2(100, 300));
            ball.setVelocity(Vec2.ZERO);
        }
    }

    /** Runs the game until a power-up is caught by the paddle (which sits under it). */
    private static List<GameEvent> waitForCatch(Game game) {
        List<GameEvent> events = new ArrayList<>();
        for (int i = 0; i < 100 && !game.powerUps().isEmpty(); i++) {
            events.addAll(game.update(0.1, InputState.NONE));
        }
        return events;
    }

    private static void advance(Game game, double seconds) {
        for (double t = 0; t < seconds; t += 0.1) {
            game.update(0.1, InputState.NONE);
        }
    }

    @Test
    void destroyedBrickCanDropPowerUp() {
        Game game = game(PowerUpType.WIDE_PADDLE);

        List<GameEvent> events = breakBrick(game);

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.PowerUpSpawned.class);
        assertThat(game.powerUps()).singleElement()
                .satisfies(p -> assertThat(p.type()).isEqualTo(PowerUpType.WIDE_PADDLE));
    }

    @Test
    void uncaughtPowerUpFallsOffScreen() {
        Game game = game(PowerUpType.WIDE_PADDLE);
        breakBrick(game);
        game.update(0.01, new InputState(false, false, OptionalDouble.of(50), false));

        advance(game, 5);

        assertThat(game.powerUps()).isEmpty();
        assertThat(game.paddle().width()).isEqualTo(GameConfig.PADDLE_WIDTH);
    }

    @Test
    void catchingWidePaddleWidensThenExpires() {
        Game game = game(PowerUpType.WIDE_PADDLE);
        breakBrick(game);

        List<GameEvent> events = waitForCatch(game);

        assertThat(events).contains(new GameEvent.PowerUpCollected(PowerUpType.WIDE_PADDLE));
        assertThat(game.paddle().width()).isEqualTo(GameConfig.PADDLE_WIDE_WIDTH);

        advance(game, GameConfig.WIDE_DURATION + 0.5);

        assertThat(game.paddle().width()).isEqualTo(GameConfig.PADDLE_WIDTH);
    }

    @Test
    void multiBallAddsTwoBalls() {
        Game game = game(PowerUpType.MULTI_BALL);
        breakBrick(game);

        waitForCatch(game);

        assertThat(game.balls()).hasSize(3);
    }

    @Test
    void extraLifeCapsAtFive() {
        Game game = game(PowerUpType.EXTRA_LIFE);

        for (int i = 0; i < 3; i++) {
            breakBrick(game);
            waitForCatch(game);
        }

        assertThat(game.lives()).isEqualTo(GameConfig.MAX_LIVES);
    }

    @Test
    void slowBallSlowsMovementThenWearsOff() {
        Game game = game(PowerUpType.SLOW_BALL);
        breakBrick(game);
        waitForCatch(game);

        ball(game).setPosition(new Vec2(400, 300));
        ball(game).setVelocity(new Vec2(0, -100));
        game.update(0.1, InputState.NONE);

        assertThat(ball(game).position().y()).isCloseTo(300 - 10 * GameConfig.SLOW_FACTOR, within(1e-9));

        park(game);
        advance(game, GameConfig.SLOW_DURATION + 0.5);
        ball(game).setPosition(new Vec2(400, 300));
        ball(game).setVelocity(new Vec2(0, -100));
        game.update(0.1, InputState.NONE);

        assertThat(ball(game).position().y()).isCloseTo(290, within(1e-9));
    }

    @Test
    void losingLifeClearsPowerUps() {
        Game game = game(PowerUpType.WIDE_PADDLE);
        breakBrick(game);
        waitForCatch(game);
        breakBrick(game);

        ball(game).setPosition(new Vec2(400, 590));
        ball(game).setVelocity(new Vec2(0, 400));
        game.update(0.1, InputState.NONE);

        assertThat(game.lives()).isEqualTo(GameConfig.START_LIVES - 1);
        assertThat(game.powerUps()).isEmpty();
        assertThat(game.paddle().width()).isEqualTo(GameConfig.PADDLE_WIDTH);
    }
}
