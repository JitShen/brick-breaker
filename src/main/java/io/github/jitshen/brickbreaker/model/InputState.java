package io.github.jitshen.brickbreaker.model;

import java.util.OptionalDouble;

/**
 * The player's input for one update.
 *
 * @param left move-left key held
 * @param right move-right key held
 * @param pointerX where the mouse wants the paddle, in playfield x; takes priority over keys
 * @param launch launch was pressed since the previous update
 */
public record InputState(boolean left, boolean right, OptionalDouble pointerX, boolean launch) {

    public static final InputState NONE = new InputState(false, false, OptionalDouble.empty(), false);
}
