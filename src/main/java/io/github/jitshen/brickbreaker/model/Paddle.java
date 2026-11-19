package io.github.jitshen.brickbreaker.model;

import static io.github.jitshen.brickbreaker.model.GameConfig.MAX_BOUNCE_ANGLE;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_HEIGHT;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_WIDTH;
import static io.github.jitshen.brickbreaker.model.GameConfig.PADDLE_Y;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDTH;

/** The player's paddle. It only moves horizontally and always stays inside the playfield. */
public final class Paddle {

    private double centerX;
    private double width = PADDLE_WIDTH;

    public Paddle(double centerX) {
        moveTo(centerX);
    }

    public double centerX() {
        return centerX;
    }

    public double width() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
        moveTo(centerX);
    }

    public Rect bounds() {
        return new Rect(centerX - width / 2, PADDLE_Y, width, PADDLE_HEIGHT);
    }

    public void moveBy(double dx) {
        moveTo(centerX + dx);
    }

    public void moveTo(double x) {
        double half = width / 2;
        centerX = Math.clamp(x, half, WIDTH - half);
    }

    /**
     * Direction the ball leaves the paddle when it lands at {@code hitX}: straight up at the
     * centre, tilting up to {@link GameConfig#MAX_BOUNCE_ANGLE} towards the edges. This is what
     * lets the player aim.
     */
    public Vec2 bounceDirection(double hitX) {
        double offset = Math.clamp((hitX - centerX) / (width / 2), -1.0, 1.0);
        double angle = offset * MAX_BOUNCE_ANGLE;
        return new Vec2(Math.sin(angle), -Math.cos(angle));
    }
}
