package io.github.jitshen.brickbreaker.ui;

import io.github.jitshen.brickbreaker.model.InputState;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Turns keyboard and mouse events into {@link InputState}s. Presses (launch, pause, restart) are
 * remembered until the next poll so a quick tap is never missed between game steps.
 */
final class InputHandler {

    private final Set<KeyCode> held = new HashSet<>();
    private OptionalDouble pointerX = OptionalDouble.empty();
    private boolean launch;
    private boolean pause;
    private boolean restart;
    private boolean quit;

    /**
     * @param node node receiving key and mouse events
     * @param toPlayfieldX converts a mouse x on the node to a playfield x
     */
    InputHandler(Node node, DoubleUnaryOperator toPlayfieldX) {
        node.addEventHandler(KeyEvent.KEY_PRESSED, e -> onKeyPressed(e.getCode()));
        node.addEventHandler(KeyEvent.KEY_RELEASED, e -> held.remove(e.getCode()));
        node.addEventHandler(MouseEvent.MOUSE_MOVED,
                e -> pointerX = OptionalDouble.of(toPlayfieldX.applyAsDouble(e.getX())));
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                e -> pointerX = OptionalDouble.of(toPlayfieldX.applyAsDouble(e.getX())));
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> launch = true);
    }

    private void onKeyPressed(KeyCode code) {
        held.add(code);
        switch (code) {
            case SPACE, ENTER -> launch = true;
            case ESCAPE, P -> pause = true;
            case R -> restart = true;
            case Q -> quit = true;
            default -> { }
        }
    }

    boolean pollQuit() {
        boolean pressed = quit;
        quit = false;
        return pressed;
    }

    /** Input for the next game step. The mouse only moves the paddle when it has moved. */
    InputState poll() {
        boolean left = held.contains(KeyCode.A) || held.contains(KeyCode.LEFT);
        boolean right = held.contains(KeyCode.D) || held.contains(KeyCode.RIGHT);
        InputState state = new InputState(left, right, pointerX, launch);
        pointerX = OptionalDouble.empty();
        launch = false;
        return state;
    }

    boolean pollPause() {
        boolean pressed = pause;
        pause = false;
        return pressed;
    }

    boolean pollRestart() {
        boolean pressed = restart;
        restart = false;
        return pressed;
    }

    /** Forgets held keys, e.g. when the window loses focus and key releases would be missed. */
    void releaseAll() {
        held.clear();
    }
}
