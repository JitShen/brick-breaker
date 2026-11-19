package io.github.jitshen.brickbreaker.model;

import static io.github.jitshen.brickbreaker.model.GameConfig.BRICK_GAP;
import static io.github.jitshen.brickbreaker.model.GameConfig.BRICK_HEIGHT;
import static io.github.jitshen.brickbreaker.model.GameConfig.BRICK_SIDE_MARGIN;
import static io.github.jitshen.brickbreaker.model.GameConfig.BRICK_TOP;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDTH;

import java.util.ArrayList;
import java.util.List;

/**
 * A level layout: one string per row, one character per brick cell. Use {@link LevelLoader} to
 * create validated levels.
 */
public record Level(String name, List<String> rows) {

    public Level {
        rows = List.copyOf(rows);
    }

    /** Builds a fresh set of bricks for this layout, spread across the playfield width. */
    public List<Brick> createBricks() {
        int columns = rows.getFirst().length();
        double brickWidth = (WIDTH - 2 * BRICK_SIDE_MARGIN - (columns - 1) * BRICK_GAP) / columns;

        List<Brick> bricks = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            String line = rows.get(row);
            double y = BRICK_TOP + row * (BRICK_HEIGHT + BRICK_GAP);
            for (int col = 0; col < columns; col++) {
                double x = BRICK_SIDE_MARGIN + col * (brickWidth + BRICK_GAP);
                Rect bounds = new Rect(x, y, brickWidth, BRICK_HEIGHT);
                BrickType.fromSymbol(line.charAt(col)).ifPresent(type -> bricks.add(new Brick(bounds, type)));
            }
        }
        return bricks;
    }
}
