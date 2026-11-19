package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BrickTest {

    private static final Rect AREA = new Rect(0, 0, 40, 20);

    @Test
    void twoHitBrickDestroyedOnSecondHit() {
        Brick brick = new Brick(AREA, BrickType.TWO);

        assertThat(brick.hit()).isFalse();
        assertThat(brick.hitsLeft()).isEqualTo(1);
        assertThat(brick.hit()).isTrue();
        assertThat(brick.isDestroyed()).isTrue();
    }

    @Test
    void destroyedBrickIgnoresFurtherHits() {
        Brick brick = new Brick(AREA, BrickType.ONE);
        brick.hit();

        assertThat(brick.hit()).isFalse();
        assertThat(brick.hitsLeft()).isZero();
    }

    @Test
    void unbreakableNeverDestroyed() {
        Brick brick = new Brick(AREA, BrickType.UNBREAKABLE);

        for (int i = 0; i < 10; i++) {
            assertThat(brick.hit()).isFalse();
        }
        assertThat(brick.isDestroyed()).isFalse();
    }

    @Test
    void symbolsMapToTypes() {
        assertThat(BrickType.fromSymbol('3')).contains(BrickType.THREE);
        assertThat(BrickType.fromSymbol('#')).contains(BrickType.UNBREAKABLE);
        assertThat(BrickType.fromSymbol('x')).isEmpty();
    }
}
