package io.github.jitshen.brickbreaker;

import io.github.jitshen.brickbreaker.score.HighScoreStore;
import io.github.jitshen.brickbreaker.ui.GameScreen;
import io.github.jitshen.brickbreaker.ui.TitleScreen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Entry point: opens the window and switches between the title and game screens. */
public final class BrickBreakerApp extends Application {

    private final HighScoreStore highScores = HighScoreStore.inUserHome();
    private Scene scene;
    private GameScreen gameScreen;

    @Override
    public void start(Stage stage) {
        scene = new Scene(new StackPane(), 800, 600);
        stage.setTitle("Brick Breaker");
        stage.setScene(scene);
        stage.setMinWidth(400);
        stage.setMinHeight(330);
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused && gameScreen != null) {
                gameScreen.pause();
            }
        });
        showTitle();
        stage.show();
    }

    private void showTitle() {
        gameScreen = null;
        TitleScreen title = new TitleScreen(highScores.load(), this::showGame, Platform::exit);
        scene.setRoot(title);
        title.focusPlay();
    }

    private void showGame() {
        gameScreen = new GameScreen(highScores, this::showTitle);
        scene.setRoot(gameScreen);
        gameScreen.start();
    }

    /** Called when the window closes: keep the score of a game left mid-way. */
    @Override
    public void stop() {
        if (gameScreen != null) {
            gameScreen.saveScore();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
