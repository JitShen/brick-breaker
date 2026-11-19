package io.github.jitshen.brickbreaker.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelLoaderTest {

    @Test
    void parsesGridIntoBricks() {
        Level level = LevelLoader.parse("test", List.of("11.1", "#22."));

        List<Brick> bricks = level.createBricks();

        assertThat(bricks).hasSize(6);
        assertThat(bricks.get(0).bounds().x()).isEqualTo(20);
        assertThat(bricks.get(0).bounds().y()).isEqualTo(60);
        assertThat(bricks.get(3).type()).isEqualTo(BrickType.UNBREAKABLE);
    }

    @Test
    void bricksFillThePlayfieldWidth() {
        List<Brick> bricks = LevelLoader.parse("test", List.of("1111")).createBricks();

        Brick last = bricks.get(3);

        assertThat(last.bounds().maxX()).isEqualTo(GameConfig.WIDTH - GameConfig.BRICK_SIDE_MARGIN);
    }

    @Test
    void ignoresCommentsAndBlankLines() {
        Level level = LevelLoader.parse("test", List.of("; title", "", "11"));

        assertThat(level.createBricks()).hasSize(2);
    }

    @Test
    void rejectsRaggedRows() {
        assertThatThrownBy(() -> LevelLoader.parse("ragged", List.of("111", "11")))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("ragged")
                .hasMessageContaining("row");
    }

    @Test
    void rejectsUnknownSymbol() {
        assertThatThrownBy(() -> LevelLoader.parse("odd", List.of("1x1")))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("'x'");
    }

    @Test
    void rejectsLevelWithoutBreakableBricks() {
        assertThatThrownBy(() -> LevelLoader.parse("walls", List.of("##", "..")))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("walls");
    }

    @Test
    void rejectsEmptyLevel() {
        assertThatThrownBy(() -> LevelLoader.parse("empty", List.of()))
                .isInstanceOf(LevelFormatException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void bundledLevelsLoad() {
        List<Level> levels = LevelLoader.loadAll();

        assertThat(levels).isNotEmpty();
        assertThat(levels).allSatisfy(level -> assertThat(level.createBricks()).isNotEmpty());
    }
}
