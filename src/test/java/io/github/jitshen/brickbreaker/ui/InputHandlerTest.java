package io.github.jitshen.brickbreaker.ui;

import static org.assertj.core.api.Assertions.assertThat;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

class InputHandlerTest {

    private final InputHandler input = new InputHandler();

    @Test
    void spaceLaunchesOnce() {
        input.keyPressed(KeyCode.SPACE);

        assertThat(input.poll().launch()).isTrue();
        assertThat(input.poll().launch()).isFalse();
    }

    @Test
    void heldKeyAutoRepeatDoesNotRetrigger() {
        input.keyPressed(KeyCode.SPACE);
        input.keyPressed(KeyCode.ESCAPE);
        input.poll();
        input.pollPause();

        input.keyPressed(KeyCode.SPACE); // auto-repeat while held
        input.keyPressed(KeyCode.ESCAPE);

        assertThat(input.poll().launch()).isFalse();
        assertThat(input.pollPause()).isFalse();
    }

    @Test
    void pressingAgainAfterReleaseTriggers() {
        input.keyPressed(KeyCode.SPACE);
        input.poll();
        input.keyReleased(KeyCode.SPACE);

        input.keyPressed(KeyCode.SPACE);

        assertThat(input.poll().launch()).isTrue();
    }

    @Test
    void enterDoesNotLaunch() {
        input.keyPressed(KeyCode.ENTER);

        assertThat(input.poll().launch()).isFalse();
    }

    @Test
    void escapeIsBackButPIsOnlyPause() {
        input.keyPressed(KeyCode.P);

        assertThat(input.pollPause()).isTrue();
        assertThat(input.pollBack()).isFalse();

        input.keyPressed(KeyCode.ESCAPE);

        assertThat(input.pollPause()).isTrue();
        assertThat(input.pollBack()).isTrue();
    }

    @Test
    void heldMovementKeysSteer() {
        input.keyPressed(KeyCode.LEFT);

        assertThat(input.poll().left()).isTrue();

        input.keyReleased(KeyCode.LEFT);

        assertThat(input.poll().left()).isFalse();
    }
}
