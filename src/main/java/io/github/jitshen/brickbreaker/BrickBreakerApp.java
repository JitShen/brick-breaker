package io.github.jitshen.brickbreaker;

import io.github.jitshen.brickbreaker.score.HighScoreStore;
import io.github.jitshen.brickbreaker.ui.GameScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Entry point: opens the game window. */
public final class BrickBreakerApp extends Application {

    @Override
    public void start(Stage stage) {
        GameScreen screen = new GameScreen(HighScoreStore.inUserHome());
        stage.setTitle("Brick Breaker");
        stage.setScene(new Scene(screen, 800, 600));
        stage.setMinWidth(400);
        stage.setMinHeight(330);
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                screen.pause();
            }
        });
        stage.show();
        screen.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
