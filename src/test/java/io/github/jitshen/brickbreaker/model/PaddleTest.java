package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class PaddleTest {

    private final Paddle paddle = new Paddle(400);

    @Test
    void centreHitGoesStraightUp() {
        Vec2 dir = paddle.bounceDirection(400);

        assertThat(dir.x()).isCloseTo(0, within(1e-12));
        assertThat(dir.y()).isCloseTo(-1, within(1e-12));
    }

    @Test
    void rightEdgeHitAngles60Degrees() {
        double angle = Math.toRadians(60);

        Vec2 edge = paddle.bounceDirection(450);
        Vec2 beyond = paddle.bounceDirection(500);

        assertThat(edge.x()).isCloseTo(Math.sin(angle), within(1e-12));
        assertThat(edge.y()).isCloseTo(-Math.cos(angle), within(1e-12));
        assertThat(beyond).isEqualTo(edge);
    }

    @Test
    void leftHalfSendsBallLeft() {
        assertThat(paddle.bounceDirection(380).x()).isNegative();
    }

    @Test
    void moveIsClampedToPlayfield() {
        paddle.moveBy(-10_000);
        assertThat(paddle.centerX()).isEqualTo(50);

        paddle.moveBy(10_000);
        assertThat(paddle.centerX()).isEqualTo(750);
    }

    @Test
    void boundsSitOnPaddleLine() {
        Rect bounds = paddle.bounds();

        assertThat(bounds.minY()).isEqualTo(GameConfig.PADDLE_Y);
        assertThat(bounds.centerX()).isEqualTo(400);
        assertThat(bounds.width()).isEqualTo(GameConfig.PADDLE_WIDTH);
    }
}
