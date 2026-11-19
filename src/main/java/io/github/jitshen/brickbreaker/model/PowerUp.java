package io.github.jitshen.brickbreaker.model;

/** A falling power-up capsule; the player collects it by catching it with the paddle. */
public final class PowerUp {

    public static final double WIDTH = 24;
    public static final double HEIGHT = 12;

    private final PowerUpType type;
    private Vec2 position;

    /** @param position the capsule's centre */
    public PowerUp(PowerUpType type, Vec2 position) {
        this.type = type;
        this.position = position;
    }

    public PowerUpType type() {
        return type;
    }

    public Vec2 position() {
        return position;
    }

    public Rect bounds() {
        return new Rect(position.x() - WIDTH / 2, position.y() - HEIGHT / 2, WIDTH, HEIGHT);
    }

    void fall(double dt) {
        position = position.plus(new Vec2(0, GameConfig.POWER_UP_FALL_SPEED * dt));
    }
}
