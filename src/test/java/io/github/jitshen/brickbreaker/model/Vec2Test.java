package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class Vec2Test {

    @Test
    void reflectFlipsNormalComponent() {
        assertThat(new Vec2(3, 4).reflect(new Vec2(0, -1))).isEqualTo(new Vec2(3, -4));
    }

    @Test
    void normalizedHasUnitLength() {
        assertThat(new Vec2(3, 4).normalized().length()).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void rotateQuarterTurn() {
        Vec2 rotated = new Vec2(1, 0).rotate(Math.PI / 2);

        assertThat(rotated.x()).isCloseTo(0, within(1e-12));
        assertThat(rotated.y()).isCloseTo(1, within(1e-12));
    }
}
