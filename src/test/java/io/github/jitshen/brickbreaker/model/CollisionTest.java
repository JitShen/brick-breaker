package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CollisionTest {

    private static final Rect BOX = new Rect(50, 40, 20, 20);

    @Test
    void hitsLeftFace() {
        Optional<Collision.Hit> hit = Collision.sweep(new Vec2(0, 50), new Vec2(100, 0), 5, BOX);

        assertThat(hit).isPresent();
        assertThat(hit.get().time()).isCloseTo(0.45, within(1e-9));
        assertThat(hit.get().normal()).isEqualTo(new Vec2(-1, 0));
    }

    @Test
    void hitsTopFace() {
        Optional<Collision.Hit> hit = Collision.sweep(new Vec2(60, 0), new Vec2(0, 100), 5, BOX);

        assertThat(hit).isPresent();
        assertThat(hit.get().time()).isCloseTo(0.35, within(1e-9));
        assertThat(hit.get().normal()).isEqualTo(new Vec2(0, -1));
    }

    @Test
    void missesWhenPassingBeside() {
        assertThat(Collision.sweep(new Vec2(0, 0), new Vec2(100, 0), 5, BOX)).isEmpty();
    }

    @Test
    void noHitBeyondDelta() {
        assertThat(Collision.sweep(new Vec2(0, 50), new Vec2(10, 0), 5, BOX)).isEmpty();
    }

    @Test
    void fastBallDoesNotTunnel() {
        Rect thin = new Rect(500, 45, 4, 10);

        Optional<Collision.Hit> hit = Collision.sweep(new Vec2(0, 50), new Vec2(10_000, 0), 5, thin);

        assertThat(hit).isPresent();
        assertThat(hit.get().normal()).isEqualTo(new Vec2(-1, 0));
    }

    @Test
    void startingInsideIsIgnored() {
        assertThat(Collision.sweep(new Vec2(60, 50), new Vec2(5, 0), 5, BOX)).isEmpty();
    }

    @Test
    void zeroDeltaNeverHits() {
        assertThat(Collision.sweep(new Vec2(40, 50), Vec2.ZERO, 5, BOX)).isEmpty();
    }
}
