package io.github.jitshen.brickbreaker.model;

/** What the game is currently doing. */
public enum GameState {
    /** The ball rests on the paddle, waiting to be launched. */
    SERVING,
    PLAYING,
    PAUSED,
    /** Every breakable brick is gone; waiting for the player to start the next level. */
    LEVEL_CLEARED,
    GAME_OVER,
    WON
}
