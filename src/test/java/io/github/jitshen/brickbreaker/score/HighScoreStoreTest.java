package io.github.jitshen.brickbreaker.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HighScoreStoreTest {

    @TempDir
    Path dir;

    @Test
    void missingFileIsZero() {
        assertThat(new HighScoreStore(dir.resolve("none.properties")).load()).isZero();
    }

    @Test
    void savedScoreRoundTrips() {
        HighScoreStore store = new HighScoreStore(dir.resolve("nested/highscore.properties"));

        store.save(1234);

        assertThat(store.load()).isEqualTo(1234);
    }

    @Test
    void saveIfHigherKeepsTheBest() {
        HighScoreStore store = new HighScoreStore(dir.resolve("highscore.properties"));
        store.save(100);

        assertThat(store.saveIfHigher(50)).isFalse();
        assertThat(store.load()).isEqualTo(100);
        assertThat(store.saveIfHigher(150)).isTrue();
        assertThat(store.load()).isEqualTo(150);
    }

    @Test
    void bareFileNameSavesInWorkingDirectory() throws IOException {
        Path bare = Path.of("brick-breaker-test-" + System.nanoTime() + ".properties");
        try {
            HighScoreStore store = new HighScoreStore(bare);

            store.save(42);

            assertThat(store.load()).isEqualTo(42);
        } finally {
            Files.deleteIfExists(bare);
        }
    }

    @Test
    void corruptFileIsZero() throws IOException {
        Path file = dir.resolve("highscore.properties");
        Files.writeString(file, "highScore=banana");

        assertThat(new HighScoreStore(file).load()).isZero();
    }

    @Test
    void malformedEscapeIsZero() throws IOException {
        Path file = dir.resolve("highscore.properties");
        Files.writeString(file, "highScore=\\u12G4");

        assertThat(new HighScoreStore(file).load()).isZero();
    }

    @Test
    void saveToUnwritablePathDoesNotThrow() throws IOException {
        Path notADirectory = Files.writeString(dir.resolve("file"), "x");
        HighScoreStore store = new HighScoreStore(notADirectory.resolve("highscore.properties"));

        assertThatCode(() -> store.save(10)).doesNotThrowAnyException();
        assertThat(store.load()).isZero();
    }
}
