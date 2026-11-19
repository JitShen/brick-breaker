package io.github.jitshen.brickbreaker.model;

/** A brick in the wall. Breakable bricks lose one hit point per hit. */
public final class Brick {

    private final Rect bounds;
    private final BrickType type;
    private int hitsLeft;

    public Brick(Rect bounds, BrickType type) {
        this.bounds = bounds;
        this.type = type;
        this.hitsLeft = type.hitPoints();
    }

    public Rect bounds() {
        return bounds;
    }

    public BrickType type() {
        return type;
    }

    public int hitsLeft() {
        return hitsLeft;
    }

    public boolean isDestroyed() {
        return type.isBreakable() && hitsLeft == 0;
    }

    /** Applies one hit and returns {@code true} if that hit destroyed the brick. */
    public boolean hit() {
        if (!type.isBreakable() || isDestroyed()) {
            return false;
        }
        hitsLeft--;
        return hitsLeft == 0;
    }
}
