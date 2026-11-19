package io.github.jitshen.brickbreaker.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Short-lived sparks thrown out when a brick breaks. Purely visual. */
final class Particles {

    private static final int PER_BURST = 16;
    private static final double LIFETIME = 0.6;
    private static final double GRAVITY = 400;
    private static final double SIZE = 4;

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double age;
        final Color color;

        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
        }
    }

    private final List<Particle> particles = new ArrayList<>();

    void burst(double x, double y, Color color) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < PER_BURST; i++) {
            double angle = random.nextDouble(0, 2 * Math.PI);
            double speed = random.nextDouble(60, 220);
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    void update(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age += dt;
            if (p.age >= LIFETIME) {
                it.remove();
                continue;
            }
            p.vy += GRAVITY * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    void render(GraphicsContext g) {
        for (Particle p : particles) {
            double alpha = 1 - p.age / LIFETIME;
            g.setFill(p.color.deriveColor(0, 1, 1, alpha));
            g.fillRect(p.x - SIZE / 2, p.y - SIZE / 2, SIZE, SIZE);
        }
    }

    void clear() {
        particles.clear();
    }
}
