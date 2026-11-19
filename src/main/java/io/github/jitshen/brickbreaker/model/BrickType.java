package io.github.jitshen.brickbreaker.model;

import java.util.Arrays;
import java.util.Optional;

/** Kinds of brick, each with the symbol used for it in level files. */
public enum BrickType {
    ONE('1', 1, 10),
    TWO('2', 2, 20),
    THREE('3', 3, 40),
    UNBREAKABLE('#', 0, 0);

    private final char symbol;
    private final int hitPoints;
    private final int score;

    BrickType(char symbol, int hitPoints, int score) {
        this.symbol = symbol;
        this.hitPoints = hitPoints;
        this.score = score;
    }

    public char symbol() {
        return symbol;
    }

    public int hitPoints() {
        return hitPoints;
    }

    public int score() {
        return score;
    }

    public boolean isBreakable() {
        return this != UNBREAKABLE;
    }

    public static Optional<BrickType> fromSymbol(char symbol) {
        return Arrays.stream(values()).filter(type -> type.symbol == symbol).findFirst();
    }
}
