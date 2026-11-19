package io.github.jitshen.brickbreaker.score;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Saves the best score to a small properties file. A missing or damaged file just means "no high
 * score yet", and a failed save is logged rather than interrupting the game.
 */
public final class HighScoreStore {

    private static final System.Logger LOG = System.getLogger(HighScoreStore.class.getName());
    private static final String KEY = "highScore";

    private final Path file;

    public HighScoreStore(Path file) {
        this.file = file;
    }

    /** The store in the user's home folder: {@code ~/.brick-breaker/highscore.properties}. */
    public static HighScoreStore inUserHome() {
        return new HighScoreStore(
                Path.of(System.getProperty("user.home"), ".brick-breaker", "highscore.properties"));
    }

    public int load() {
        if (!Files.isRegularFile(file)) {
            return 0;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            return Math.max(0, Integer.parseInt(properties.getProperty(KEY, "0").strip()));
        } catch (IOException | IllegalArgumentException e) { // covers bad numbers and bad unicode escapes
            LOG.log(Level.WARNING, "Ignoring unreadable high score file " + file, e);
            return 0;
        }
    }

    /** Saves {@code score} only if it beats the stored best; returns whether it did. */
    public boolean saveIfHigher(int score) {
        if (score <= load()) {
            return false;
        }
        save(score);
        return true;
    }

    public void save(int score) {
        Properties properties = new Properties();
        properties.setProperty(KEY, Integer.toString(score));
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Brick Breaker high score");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save high score to " + file, e);
        }
    }
}
