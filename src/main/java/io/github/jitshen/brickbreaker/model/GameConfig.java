package io.github.jitshen.brickbreaker.model;

/** Tuning values for the game. Distances are playfield pixels, times are seconds. */
public final class GameConfig {

    public static final double WIDTH = 800;
    public static final double HEIGHT = 600;

    public static final double BALL_RADIUS = 8;
    public static final double BALL_START_SPEED = 360;
    public static final double BALL_MAX_SPEED = 620;
    /** Speed added each time the ball bounces off the paddle. */
    public static final double BALL_SPEED_UP = 8;
    /** The ball's vertical speed never drops below this share of its total speed. */
    public static final double MIN_VERTICAL_RATIO = 0.3;

    public static final double PADDLE_WIDTH = 100;
    public static final double PADDLE_WIDE_WIDTH = 150;
    public static final double PADDLE_HEIGHT = 14;
    public static final double PADDLE_Y = 560;
    public static final double PADDLE_SPEED = 600;
    public static final double MAX_BOUNCE_ANGLE = Math.toRadians(60);

    public static final int START_LIVES = 3;
    public static final int MAX_LIVES = 5;

    public static final double BRICK_TOP = 60;
    public static final double BRICK_HEIGHT = 24;
    public static final double BRICK_GAP = 4;
    public static final double BRICK_SIDE_MARGIN = 20;

    /** Chance that a destroyed brick drops a power-up. */
    public static final double POWER_UP_DROP_CHANCE = 0.15;
    public static final double POWER_UP_FALL_SPEED = 150;
    public static final double WIDE_DURATION = 10;
    public static final double SLOW_DURATION = 8;
    /** Ball movement speed multiplier while the slow-ball power-up is active. */
    public static final double SLOW_FACTOR = 0.7;
    /** Angle between the original ball and each extra multi-ball. */
    public static final double MULTI_BALL_SPREAD = Math.toRadians(20);

    private GameConfig() {}
}
