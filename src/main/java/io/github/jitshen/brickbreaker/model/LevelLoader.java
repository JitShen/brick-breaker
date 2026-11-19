package io.github.jitshen.brickbreaker.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads levels from text. Format: one row per line, {@code .} for an empty cell, a {@link
 * BrickType} symbol for a brick. Blank lines and lines starting with {@code ;} are ignored.
 */
public final class LevelLoader {

    private static final char EMPTY = '.';
    private static final String COMMENT = ";";
    private static final String INDEX = "/levels/levels.txt";

    private LevelLoader() {}

    public static Level parse(String name, List<String> lines) {
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            String row = line.stripTrailing();
            if (!row.isEmpty() && !row.startsWith(COMMENT)) {
                rows.add(row);
            }
        }
        validate(name, rows);
        return new Level(name, rows);
    }

    /** Loads every level listed, in order, in the bundled {@code levels/levels.txt}. */
    public static List<Level> loadAll() {
        List<Level> levels = new ArrayList<>();
        for (String file : readResource("index", INDEX)) {
            String fileName = file.strip();
            if (!fileName.isEmpty()) {
                levels.add(parse(fileName, readResource(fileName, "/levels/" + fileName)));
            }
        }
        return levels;
    }

    private static void validate(String name, List<String> rows) {
        if (rows.isEmpty()) {
            throw new LevelFormatException(name, "has no rows");
        }
        int width = rows.getFirst().length();
        boolean hasBreakable = false;
        for (int r = 0; r < rows.size(); r++) {
            String row = rows.get(r);
            if (row.length() != width) {
                throw new LevelFormatException(
                        name, "row " + (r + 1) + " has " + row.length() + " cells, expected " + width);
            }
            int rowNumber = r + 1;
            for (char cell : row.toCharArray()) {
                if (cell == EMPTY) {
                    continue;
                }
                BrickType type = BrickType.fromSymbol(cell)
                        .orElseThrow(() -> new LevelFormatException(
                                name, "unknown symbol '" + cell + "' in row " + rowNumber));
                hasBreakable |= type.isBreakable();
            }
        }
        if (!hasBreakable) {
            throw new LevelFormatException(name, "has no breakable bricks");
        }
    }

    private static List<String> readResource(String name, String path) {
        try (InputStream in = LevelLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new LevelFormatException(name, "resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException e) {
            throw new LevelFormatException(name, "could not be read", e);
        }
    }
}
