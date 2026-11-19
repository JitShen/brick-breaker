package io.github.jitshen.brickbreaker.ui;

import io.github.jitshen.brickbreaker.model.Game;
import io.github.jitshen.brickbreaker.model.GameEvent;
import io.github.jitshen.brickbreaker.model.GameState;
import io.github.jitshen.brickbreaker.model.LevelLoader;
import io.github.jitshen.brickbreaker.score.HighScoreStore;
import java.util.List;
import java.util.random.RandomGenerator;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

/** The in-game screen: owns the game, its loop, input, drawing and high score. */
public final class GameScreen extends Pane {

    private final Game game = new Game(LevelLoader.loadAll(), RandomGenerator.getDefault());
    private final HighScoreStore highScores;
    private final Runnable onQuit;
    private final Canvas canvas = new Canvas();
    private final GameView view = new GameView(canvas);
    private final InputHandler input = new InputHandler();
    private final GameLoop loop = new GameLoop(this::step, this::render);
    private int bestScore;
    private boolean newBest;

    /**
     * @param highScores where the best score is loaded from and saved to
     * @param onQuit called when the player leaves for the title screen
     */
    public GameScreen(HighScoreStore highScores, Runnable onQuit) {
        this.highScores = highScores;
        this.onQuit = onQuit;
        this.bestScore = highScores.load();
        getChildren().add(canvas);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setFocusTraversable(true);
        input.attach(this, view::toPlayfieldX);
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

    /** Keeps the current score if it is a new best, e.g. when the player leaves mid-game. */
    public void saveScore() {
        if (highScores.saveIfHigher(game.score())) {
            bestScore = game.score();
            newBest = true;
        }
    }

    private void step(double dt) {
        boolean pausePressed = input.pollPause();
        boolean backPressed = input.pollBack();
        boolean quitPressed = input.pollQuit();
        if (isOver() && backPressed || game.state() == GameState.PAUSED && quitPressed) {
            leave();
            return;
        }
        if (pausePressed && !isOver()) {
            game.togglePause();
        }
        if (input.pollRestart() && isOver()) {
            game.restart();
            newBest = false;
        }

        List<GameEvent> events = game.update(dt, input.poll());
        for (GameEvent event : events) {
            if (event instanceof GameEvent.GameOver || event instanceof GameEvent.GameWon) {
                saveScore();
            }
        }
        view.react(events);
        view.updateEffects(dt);
    }

    private void leave() {
        stop();
        saveScore();
        onQuit.run();
    }

    private void render() {
        view.render(game, bestScore, newBest);
    }

    private boolean isOver() {
        return game.state() == GameState.GAME_OVER || game.state() == GameState.WON;
    }
}
