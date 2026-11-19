package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class GameTest {

    private static final InputState LAUNCH = new InputState(false, false, OptionalDouble.empty(), true);
    private static final InputState RIGHT = new InputState(false, true, OptionalDouble.empty(), false);

    private static Game game(String... rows) {
        return new Game(List.of(LevelLoader.parse("test", List.of(rows))), new SplittableRandom(1));
    }

    private static Game launched(String... rows) {
        Game game = game(rows);
        game.update(0.001, LAUNCH);
        return game;
    }

    private static Ball place(Game game, double x, double y, double vx, double vy) {
        Ball ball = game.balls().getFirst();
        ball.setPosition(new Vec2(x, y));
        ball.setVelocity(new Vec2(vx, vy));
        return ball;
    }

    private static long count(List<GameEvent> events, Class<? extends GameEvent> type) {
        return events.stream().filter(type::isInstance).count();
    }

    @Test
    void servingBallFollowsPaddleUntilLaunch() {
        Game game = game("1");

        game.update(0.1, RIGHT);

        assertThat(game.state()).isEqualTo(GameState.SERVING);
        assertThat(game.paddle().centerX()).isGreaterThan(400);
        assertThat(game.balls().getFirst().position().x()).isEqualTo(game.paddle().centerX());

        List<GameEvent> events = game.update(0.01, LAUNCH);

        assertThat(game.state()).isEqualTo(GameState.PLAYING);
        assertThat(game.balls().getFirst().velocity().y()).isNegative();
        assertThat(events).hasAtLeastOneElementOfType(GameEvent.BallLaunched.class);
    }

    @Test
    void pointerMovesPaddleDirectly() {
        Game game = game("1");

        game.update(0.01, new InputState(false, false, OptionalDouble.of(250), false));

        assertThat(game.paddle().centerX()).isEqualTo(250);
    }

    @Test
    void ballBouncesOffSideWall() {
        Game game = launched("1");
        Ball ball = place(game, GameConfig.WIDTH - 20, 300, 300, -200);

        List<GameEvent> events = game.update(0.1, InputState.NONE);

        assertThat(ball.velocity().x()).isNegative();
        assertThat(events).hasAtLeastOneElementOfType(GameEvent.WallHit.class);
    }

    @Test
    void ballLandingOnPaddleBouncesUpAndAims() {
        Game game = launched("1");
        Ball ball = place(game, 420, 500, 0, 300);

        List<GameEvent> events = game.update(0.25, InputState.NONE);

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.PaddleHit.class);
        assertThat(ball.velocity().y()).isNegative();
        assertThat(ball.velocity().x()).isPositive();
    }

    @Test
    void destroyingLastBrickClearsLevel() {
        Level level = LevelLoader.parse("one", List.of("1"));
        Game game = new Game(List.of(level, level), new SplittableRandom(1));
        game.update(0.001, LAUNCH);
        place(game, 400, 150, 0, -300);

        List<GameEvent> events = game.update(0.25, InputState.NONE);

        assertThat(events).contains(new GameEvent.LevelCleared(0));
        assertThat(game.state()).isEqualTo(GameState.LEVEL_CLEARED);
        assertThat(game.score()).isEqualTo(BrickType.ONE.score());

        game.update(0.01, LAUNCH);

        assertThat(game.state()).isEqualTo(GameState.SERVING);
        assertThat(game.levelIndex()).isEqualTo(1);
        assertThat(game.bricks()).hasSize(1);
    }

    @Test
    void clearingLastLevelWins() {
        Game game = launched("1");
        place(game, 400, 150, 0, -300);

        List<GameEvent> events = game.update(0.25, InputState.NONE);

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.GameWon.class);
        assertThat(game.state()).isEqualTo(GameState.WON);
    }

    @Test
    void unbreakableBricksAreNotNeededToClearLevel() {
        Game game = launched("#1");
        place(game, 600, 150, 0, -300);

        game.update(0.25, InputState.NONE);

        assertThat(game.state()).isEqualTo(GameState.WON);
    }

    @Test
    void losingAllLivesIsGameOver() {
        Game game = game("1");
        List<GameEvent> events = new ArrayList<>();

        for (int life = 0; life < GameConfig.START_LIVES; life++) {
            game.update(0.001, LAUNCH);
            place(game, 400, 590, 0, 400);
            events.addAll(game.update(0.1, InputState.NONE));
        }

        assertThat(count(events, GameEvent.LifeLost.class)).isEqualTo(3);
        assertThat(count(events, GameEvent.GameOver.class)).isEqualTo(1);
        assertThat(game.state()).isEqualTo(GameState.GAME_OVER);
        assertThat(game.update(0.1, LAUNCH)).isEmpty();
    }

    @Test
    void restartResetsEverything() {
        Game game = launched("1");
        place(game, 400, 590, 0, 400);
        game.update(0.1, InputState.NONE);

        game.restart();

        assertThat(game.lives()).isEqualTo(GameConfig.START_LIVES);
        assertThat(game.score()).isZero();
        assertThat(game.state()).isEqualTo(GameState.SERVING);
    }

    @Test
    void pausedGameDoesNotMove() {
        Game game = launched("1");
        Vec2 before = game.balls().getFirst().position();

        game.togglePause();
        List<GameEvent> events = game.update(0.1, InputState.NONE);

        assertThat(events).isEmpty();
        assertThat(game.balls().getFirst().position()).isEqualTo(before);
        assertThat(game.state()).isEqualTo(GameState.PAUSED);

        game.togglePause();

        assertThat(game.state()).isEqualTo(GameState.PLAYING);
    }

    @Test
    void hugeFrameNeverSkipsBrick() {
        Game game = launched("1");
        Ball ball = place(game, 400, 200, 0, -GameConfig.BALL_MAX_SPEED);

        List<GameEvent> events = game.update(5.0, InputState.NONE);

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.BrickHit.class);
        assertThat(ball.position().x()).isBetween(0.0, GameConfig.WIDTH);
        assertThat(ball.position().y()).isBetween(0.0, GameConfig.HEIGHT);
    }

    @Test
    void largeFrameIsClampedToQuarterSecond() {
        Game clamped = launched("1");
        Game reference = launched("1");
        Ball a = place(clamped, 400, 400, 0, -100);
        Ball b = place(reference, 400, 400, 0, -100);

        clamped.update(5.0, InputState.NONE);
        reference.update(0.25, InputState.NONE);

        assertThat(a.position()).isEqualTo(b.position());
    }

    @Test
    void seamHitDamagesOneBrick() {
        Game game = launched("11");
        Ball ball = place(game, 400, 150, 0, -300);

        List<GameEvent> events = game.update(0.25, InputState.NONE);

        assertThat(count(events, GameEvent.BrickHit.class)).isEqualTo(1);
        assertThat(game.bricks()).hasSize(1);
        double brickBottom = GameConfig.BRICK_TOP + GameConfig.BRICK_HEIGHT;
        assertThat(ball.position().y()).isGreaterThan(brickBottom + ball.radius());
    }

    @Test
    void ballBelowPaddleTopIsNotBounced() {
        Game game = launched("1");
        Ball ball = place(game, 460, 570, 0, 100);

        List<GameEvent> events = game.update(0.1, RIGHT);

        assertThat(events).doesNotHaveAnyElementsOfTypes(GameEvent.PaddleHit.class);
        assertThat(ball.velocity().y()).isPositive();
    }

    @Test
    void ballTouchingPaddleTopBouncesWhenMouseJumpsPaddleUnderIt() {
        Game game = launched("1");
        Ball ball = place(game, 500, 556, 0, 300);

        List<GameEvent> events = game.update(0.01, new InputState(false, false, OptionalDouble.of(480), false));

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.PaddleHit.class);
        assertThat(ball.velocity().y()).isNegative();
        assertThat(game.lives()).isEqualTo(GameConfig.START_LIVES);
    }

    @Test
    void ballTouchingPaddleTopBouncesWhenPaddleSlidesIntoIt() {
        Game game = launched("1");
        Ball ball = place(game, 455, 553, 0, 300);

        List<GameEvent> events = game.update(0.02, RIGHT);

        assertThat(events).hasAtLeastOneElementOfType(GameEvent.PaddleHit.class);
        assertThat(ball.velocity().y()).isNegative();
    }

    @Test
    void ballNeverTravelsNearlyHorizontally() {
        Game game = launched("1");
        Ball ball = place(game, GameConfig.WIDTH - 20, 300, 400, -10);

        game.update(0.1, InputState.NONE);

        double ratio = Math.abs(ball.velocity().y()) / ball.speed();
        assertThat(ratio).isGreaterThanOrEqualTo(GameConfig.MIN_VERTICAL_RATIO - 1e-9);
    }
}
