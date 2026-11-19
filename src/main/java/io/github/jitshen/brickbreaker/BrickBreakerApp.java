package io.github.jitshen.brickbreaker;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Entry point: opens the game window. */
public final class BrickBreakerApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Brick Breaker");
        stage.setScene(new Scene(new StackPane(), 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
