package io.github.jitshen.brickbreaker.model;

/** Thrown when a level file can't be turned into a playable level. */
public final class LevelFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LevelFormatException(String levelName, String problem) {
        super("Level '" + levelName + "': " + problem);
    }

    public LevelFormatException(String levelName, String problem, Throwable cause) {
        super("Level '" + levelName + "': " + problem, cause);
    }
}
