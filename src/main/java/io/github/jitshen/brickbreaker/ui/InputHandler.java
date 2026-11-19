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
 * Turns keyboard and mouse events into {@link InputState}s. Presses (launch, pause, back,
 * restart, quit) are remembered until the next poll so a quick tap is never missed between game
 * steps. A held key's auto-repeat does not count as a new press.
 */
final class InputHandler {

    private final Set<KeyCode> held = new HashSet<>();
    private OptionalDouble pointerX = OptionalDouble.empty();
    private boolean launch;
    private boolean pause;
    private boolean back;
    private boolean restart;
    private boolean quit;

    /**
     * Listens for key and mouse events on {@code node}.
     *
     * @param toPlayfieldX converts a mouse x on the node to a playfield x
     */
    void attach(Node node, DoubleUnaryOperator toPlayfieldX) {
        node.addEventHandler(KeyEvent.KEY_PRESSED, e -> keyPressed(e.getCode()));
        node.addEventHandler(KeyEvent.KEY_RELEASED, e -> keyReleased(e.getCode()));
        node.addEventHandler(MouseEvent.MOUSE_MOVED,
                e -> pointerX = OptionalDouble.of(toPlayfieldX.applyAsDouble(e.getX())));
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED,
                e -> pointerX = OptionalDouble.of(toPlayfieldX.applyAsDouble(e.getX())));
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> launch = true);
    }

    void keyPressed(KeyCode code) {
        if (!held.add(code)) {
            return; // auto-repeat of a key that is already down
        }
        switch (code) {
            case SPACE -> launch = true;
            case ESCAPE -> {
                pause = true;
                back = true;
            }
            case P -> pause = true;
            case R -> restart = true;
            case Q -> quit = true;
            default -> { }
        }
    }

    void keyReleased(KeyCode code) {
        held.remove(code);
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

    /** Esc or P: pause or resume. */
    boolean pollPause() {
        boolean pressed = pause;
        pause = false;
        return pressed;
    }

    /** Esc only: leave the end screen for the title. */
    boolean pollBack() {
        boolean pressed = back;
        back = false;
        return pressed;
    }

    boolean pollRestart() {
        boolean pressed = restart;
        restart = false;
        return pressed;
    }

    /** Q: quit to the title from the pause menu. */
    boolean pollQuit() {
        boolean pressed = quit;
        quit = false;
        return pressed;
    }

    /** Forgets held keys, e.g. when the window loses focus and key releases would be missed. */
    void releaseAll() {
        held.clear();
    }
}
