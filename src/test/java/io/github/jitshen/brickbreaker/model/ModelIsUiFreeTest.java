package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The game rules must stay independent of JavaFX so they can be tested without a UI. */
class ModelIsUiFreeTest {

    @Test
    void modelDoesNotImportJavaFx() throws IOException {
        Path model = Path.of("src/main/java/io/github/jitshen/brickbreaker/model");

        List<Path> offenders;
        try (Stream<Path> files = Files.walk(model)) {
            offenders = files.filter(p -> p.toString().endsWith(".java"))
                    .filter(ModelIsUiFreeTest::importsJavaFx)
                    .toList();
        }

        assertThat(offenders).isEmpty();
    }

    private static boolean importsJavaFx(Path file) {
        try {
            return Files.readString(file).contains("import javafx");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
