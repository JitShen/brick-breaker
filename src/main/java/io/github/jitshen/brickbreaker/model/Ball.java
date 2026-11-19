package io.github.jitshen.brickbreaker.model;

/** A ball: a position (its centre) and a velocity in pixels per second. */
public final class Ball {

    private Vec2 position;
    private Vec2 velocity;

    public Ball(Vec2 position, Vec2 velocity) {
        this.position = position;
        this.velocity = velocity;
    }

    public Vec2 position() {
        return position;
    }

    public void setPosition(Vec2 position) {
        this.position = position;
    }

    public Vec2 velocity() {
        return velocity;
    }

    public void setVelocity(Vec2 velocity) {
        this.velocity = velocity;
    }

    public double radius() {
        return GameConfig.BALL_RADIUS;
    }

    public double speed() {
        return velocity.length();
    }
}
