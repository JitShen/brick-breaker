package io.github.jitshen.brickbreaker.ui;

import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** The first screen: game title, best score, and Play / Quit buttons. */
public final class TitleScreen extends VBox {

    private static final String BUTTON_STYLE = "-fx-font-size: 18px; -fx-min-width: 180px;"
            + " -fx-background-radius: 20; -fx-cursor: hand;";

    private final Button play = new Button("Play");

    public TitleScreen(int highScore, Runnable onPlay, Runnable onQuit) {
        super(18);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: " + toCss(Palette.BACKGROUND) + ";");

        Label title = new Label("BRICK BREAKER");
        title.setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: "
                + toCss(Palette.ACCENT) + ";");
        Label best = new Label(highScore > 0 ? "Best score: " + highScore : "No high score yet");
        best.setStyle("-fx-font-size: 18px; -fx-text-fill: " + toCss(Palette.TEXT_DIM) + ";");
        Label controls = new Label("A / D or arrows or mouse to move   Space or click to launch   Esc to pause");
        controls.setStyle("-fx-font-size: 14px; -fx-text-fill: " + toCss(Palette.TEXT_DIM) + ";");

        play.setDefaultButton(true);
        play.setStyle(BUTTON_STYLE + "-fx-background-color: " + toCss(Palette.ACCENT) + ";"
                + " -fx-text-fill: " + toCss(Palette.BACKGROUND) + ";");
        play.setOnAction(e -> onPlay.run());

        Button quit = new Button("Quit");
        quit.setStyle(BUTTON_STYLE + "-fx-background-color: transparent; -fx-border-radius: 20;"
                + " -fx-border-color: " + toCss(Palette.TEXT_DIM) + "; -fx-text-fill: "
                + toCss(Palette.TEXT) + ";");
        quit.setOnAction(e -> onQuit.run());

        getChildren().addAll(title, best, play, quit, controls);
        VBox.setMargin(play, new Insets(24, 0, 0, 0));
        VBox.setMargin(controls, new Insets(32, 0, 0, 0));
    }

    /** Gives the Play button keyboard focus so Enter or Space starts the game. */
    public void focusPlay() {
        play.requestFocus();
    }

    private static String toCss(Color color) {
        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                color.getOpacity());
    }
}
