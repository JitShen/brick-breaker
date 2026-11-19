package io.github.jitshen.brickbreaker.model;

import java.util.Optional;

/**
 * Continuous (swept) collision between a moving circle and a rectangle.
 *
 * <p>A circle of radius r touches a rectangle exactly when its centre is inside the rectangle
 * grown by r with <em>rounded</em> corners. That shape is two crossing strips (the rectangle grown
 * sideways, and grown up and down) plus a circle of radius r on each corner. The movement of the
 * centre is tested against each part and the earliest contact wins. Because the whole movement is
 * tested, a fast ball can never skip over a thin brick between two frames.
 */
public final class Collision {

    /** A contact: {@code time} is the fraction of the movement (0..1) at which it happens. */
    public record Hit(double time, Vec2 normal) {}

    private Collision() {}

    public static Optional<Hit> sweep(Vec2 start, Vec2 delta, double radius, Rect rect) {
        if (delta.x() == 0 && delta.y() == 0) {
            return Optional.empty();
        }
        Rect wide = new Rect(rect.x() - radius, rect.y(), rect.width() + 2 * radius, rect.height());
        Rect tall = new Rect(rect.x(), rect.y() - radius, rect.width(), rect.height() + 2 * radius);
        Vec2[] corners = {
            new Vec2(rect.minX(), rect.minY()), new Vec2(rect.maxX(), rect.minY()),
            new Vec2(rect.minX(), rect.maxY()), new Vec2(rect.maxX(), rect.maxY())
        };

        if (wide.contains(start) || tall.contains(start) || insideAny(start, corners, radius)) {
            return Optional.empty(); // already touching: not a new contact
        }

        Hit best = earlier(null, boxHit(start, delta, wide));
        best = earlier(best, boxHit(start, delta, tall));
        for (Vec2 corner : corners) {
            best = earlier(best, circleHit(start, delta, corner, radius));
        }
        return Optional.ofNullable(best);
    }

    private static Hit earlier(Hit best, Hit candidate) {
        return candidate != null && (best == null || candidate.time() < best.time()) ? candidate : best;
    }

    private static boolean insideAny(Vec2 point, Vec2[] corners, double radius) {
        for (Vec2 corner : corners) {
            if (point.minus(corner).length() < radius) {
                return true;
            }
        }
        return false;
    }

    /** Ray-vs-box "slab" test: when the point enters {@code box}, and through which face. */
    private static Hit boxHit(Vec2 start, Vec2 delta, Rect box) {
        Slab xs = Slab.of(start.x(), delta.x(), box.minX(), box.maxX());
        Slab ys = Slab.of(start.y(), delta.y(), box.minY(), box.maxY());
        if (xs == null || ys == null) {
            return null;
        }
        double enter = Math.max(xs.enter, ys.enter);
        double exit = Math.min(xs.exit, ys.exit);
        if (enter > exit || enter < 0 || enter > 1) {
            return null;
        }
        Vec2 normal = xs.enter >= ys.enter
                ? new Vec2(-Math.signum(delta.x()), 0)
                : new Vec2(0, -Math.signum(delta.y()));
        return new Hit(enter, normal);
    }

    /** When the point comes within {@code radius} of {@code corner}; the normal points away from it. */
    private static Hit circleHit(Vec2 start, Vec2 delta, Vec2 corner, double radius) {
        Vec2 m = start.minus(corner);
        double a = delta.dot(delta);
        double b = 2 * m.dot(delta);
        double c = m.dot(m) - radius * radius;
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return null;
        }
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        if (t < 0 || t > 1) {
            return null;
        }
        Vec2 contact = start.plus(delta.scale(t));
        return new Hit(t, contact.minus(corner).scale(1 / radius));
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
