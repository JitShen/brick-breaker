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
    void passingJustOutsideRoundedCornerMisses() {
        // Passes 5.66 px from the top-right corner: inside a square-cornered grown box, but
        // outside the radius-5 rounded corner the ball really traces.
        assertThat(Collision.sweep(new Vec2(54, 16), new Vec2(40, 40), 5, BOX)).isEmpty();
    }

    @Test
    void cornerHitPushesBackDiagonally() {
        Optional<Collision.Hit> hit = Collision.sweep(new Vec2(80, 30), new Vec2(-20, 20), 5, BOX);

        assertThat(hit).isPresent();
        double d = Math.sqrt(0.5);
        assertThat(hit.get().normal().x()).isCloseTo(d, within(1e-9));
        assertThat(hit.get().normal().y()).isCloseTo(-d, within(1e-9));
        Vec2 contact = new Vec2(80, 30).plus(new Vec2(-20, 20).scale(hit.get().time()));
        assertThat(contact.minus(new Vec2(70, 40)).length()).isCloseTo(5, within(1e-9));
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
