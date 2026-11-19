package io.github.jitshen.brickbreaker.ui;

import io.github.jitshen.brickbreaker.model.Game;
import io.github.jitshen.brickbreaker.model.GameState;
import io.github.jitshen.brickbreaker.model.LevelLoader;
import java.util.random.RandomGenerator;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/** The in-game screen: owns the game, its loop, input and drawing. */
public final class GameScreen extends Pane {

    private final Game game = new Game(LevelLoader.loadAll(), RandomGenerator.getDefault());
    private final Canvas canvas = new Canvas();
    private final GameView view = new GameView(canvas);
    private final InputHandler input = new InputHandler(this, view::toPlayfieldX);
    private final GameLoop loop = new GameLoop(this::step, () -> view.render(game));

    public GameScreen() {
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setFocusTraversable(true);
    }

    public void start() {
        requestFocus();
        loop.start();
    }

    public void stop() {
        loop.stop();
    }

    /** Pauses a running game, e.g. when the window loses focus. */
    public void pause() {
        input.releaseAll();
        if (game.state() == GameState.PLAYING || game.state() == GameState.SERVING) {
            game.togglePause();
        }
    }

    private void step(double dt) {
        if (input.pollPause()) {
            game.togglePause();
        }
        if (input.pollRestart() && isOver()) {
            game.restart();
        }
        game.update(dt, input.poll());
    }

    private boolean isOver() {
        return game.state() == GameState.GAME_OVER || game.state() == GameState.WON;
    }
}
