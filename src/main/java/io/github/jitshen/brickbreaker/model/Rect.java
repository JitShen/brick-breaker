package io.github.jitshen.brickbreaker.model;

/** Axis-aligned rectangle; (x, y) is the top-left corner. */
public record Rect(double x, double y, double width, double height) {

    public double minX() {
        return x;
    }

    public double maxX() {
        return x + width;
    }

    public double minY() {
        return y;
    }

    public double maxY() {
        return y + height;
    }

    public double centerX() {
        return x + width / 2;
    }

    public double centerY() {
        return y + height / 2;
    }

    /** Returns this rectangle grown by {@code amount} on every side. */
    public Rect expand(double amount) {
        return new Rect(x - amount, y - amount, width + 2 * amount, height + 2 * amount);
    }

    public boolean intersects(Rect other) {
        return minX() < other.maxX()
                && other.minX() < maxX()
                && minY() < other.maxY()
                && other.minY() < maxY();
    }
}
