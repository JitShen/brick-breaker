package io.github.jitshen.brickbreaker.model;

/** Immutable 2D vector in playfield units. */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2 plus(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 minus(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 scale(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    public double length() {
        return Math.hypot(x, y);
    }

    public Vec2 normalized() {
        double len = length();
        return len == 0 ? ZERO : scale(1 / len);
    }

    /** Mirrors this vector across a surface with the given unit normal. */
    public Vec2 reflect(Vec2 normal) {
        return minus(normal.scale(2 * dot(normal)));
    }

    public Vec2 rotate(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec2(x * cos - y * sin, x * sin + y * cos);
    }
}
