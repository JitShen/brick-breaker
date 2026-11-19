package io.github.jitshen.brickbreaker.ui;

import java.util.function.DoubleConsumer;
import javafx.animation.AnimationTimer;

/**
 * Fixed-timestep game loop. The game is always simulated in {@link #STEP}-second steps, however
 * fast or slow the display refreshes, so gameplay behaves the same on every machine. Rendering
 * happens once per display frame.
 */
final class GameLoop {

    static final double STEP = 1.0 / 120;
    private static final double MAX_FRAME = 0.25;

    private final AnimationTimer timer;

    GameLoop(DoubleConsumer step, Runnable render) {
        timer = new AnimationTimer() {
            private long last = -1;
            private double accumulator;

            @Override
            public void handle(long now) {
                if (last >= 0) {
                    accumulator += Math.min((now - last) / 1e9, MAX_FRAME);
                    while (accumulator >= STEP) {
                        step.accept(STEP);
                        accumulator -= STEP;
                    }
                }
                last = now;
                render.run();
            }
        };
    }

    void start() {
        timer.start();
    }

    void stop() {
        timer.stop();
    }
}
