package io.github.jitshen.brickbreaker.model;

import java.util.Optional;

/**
 * Continuous (swept) collision between a moving circle and a rectangle.
 *
 * <p>The circle is treated as a point and the rectangle is grown by the circle's radius, then a
 * ray-vs-box "slab" test finds when the point enters the grown box. Because the whole movement is
 * tested, a fast ball can never skip over a thin brick between two frames.
 */
public final class Collision {

    /** A contact: {@code time} is the fraction of the movement (0..1) at which it happens. */
    public record Hit(double time, Vec2 normal) {}

    private Collision() {}

    public static Optional<Hit> sweep(Vec2 start, Vec2 delta, double radius, Rect rect) {
        Rect box = rect.expand(radius);

        Slab xs = Slab.of(start.x(), delta.x(), box.minX(), box.maxX());
        Slab ys = Slab.of(start.y(), delta.y(), box.minY(), box.maxY());
        if (xs == null || ys == null) {
            return Optional.empty();
        }

        double enter = Math.max(xs.enter, ys.enter);
        double exit = Math.min(xs.exit, ys.exit);
        if (enter > exit || enter < 0 || enter > 1) {
            return Optional.empty();
        }

        Vec2 normal = xs.enter >= ys.enter
                ? new Vec2(-Math.signum(delta.x()), 0)
                : new Vec2(0, -Math.signum(delta.y()));
        return Optional.of(new Hit(enter, normal));
    }

    /** Entry and exit times along one axis; null when the movement can never overlap on it. */
    private record Slab(double enter, double exit) {

        static Slab of(double start, double delta, double min, double max) {
            if (delta == 0) {
                boolean inside = start > min && start < max;
                return inside ? new Slab(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) : null;
            }
            double t1 = (min - start) / delta;
            double t2 = (max - start) / delta;
            return new Slab(Math.min(t1, t2), Math.max(t1, t2));
        }
    }
}
