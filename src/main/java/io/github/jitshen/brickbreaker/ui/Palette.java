package io.github.jitshen.brickbreaker.ui;

import io.github.jitshen.brickbreaker.model.Brick;
import io.github.jitshen.brickbreaker.model.BrickType;
import javafx.scene.paint.Color;

/** Every colour the game uses, in one place. */
final class Palette {

    static final Color LETTERBOX = Color.web("#07090d");
    static final Color BACKGROUND = Color.web("#10131a");
    static final Color GRID = Color.web("#161b25");
    static final Color PADDLE = Color.web("#e8f1ff");
    static final Color BALL = Color.web("#ffffff");
    static final Color TEXT = Color.web("#e8f1ff");
    static final Color TEXT_DIM = Color.web("#8a94a8");
    static final Color ACCENT = Color.web("#4cc9f0");
    static final Color OVERLAY = Color.web("#07090d", 0.7);

    private Palette() {}

    static Color brickBase(BrickType type) {
        return switch (type) {
            case ONE -> Color.web("#4cc9f0");
            case TWO -> Color.web("#9d6bff");
            case THREE -> Color.web("#f72585");
            case UNBREAKABLE -> Color.web("#5c677d");
        };
    }

    /** A brick's colour, fading as it takes damage. */
    static Color brick(Brick brick) {
        Color base = brickBase(brick.type());
        if (!brick.type().isBreakable()) {
            return base;
        }
        double health = (double) brick.hitsLeft() / brick.type().hitPoints();
        return base.deriveColor(0, 0.6 + 0.4 * health, 0.55 + 0.45 * health, 1);
    }
}
