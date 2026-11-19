package io.github.jitshen.brickbreaker.ui;

import static io.github.jitshen.brickbreaker.model.GameConfig.HEIGHT;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDTH;

import io.github.jitshen.brickbreaker.model.Ball;
import io.github.jitshen.brickbreaker.model.Brick;
import io.github.jitshen.brickbreaker.model.Game;
import io.github.jitshen.brickbreaker.model.GameEvent;
import io.github.jitshen.brickbreaker.model.PowerUp;
import io.github.jitshen.brickbreaker.model.PowerUpType;
import io.github.jitshen.brickbreaker.model.Rect;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws the game onto a canvas, plus visual effects driven by game events. The 800 x 600
 * playfield is scaled to fit the canvas, keeping its shape, with dark bars filling any leftover
 * space.
 */
final class GameView {

    private static final Font HUD_FONT = Font.font("System", FontWeight.BOLD, 18);
    private static final Font TITLE_FONT = Font.font("System", FontWeight.BOLD, 44);
    private static final Font HINT_FONT = Font.font("System", FontWeight.NORMAL, 18);
    private static final Font SMALL_FONT = Font.font("System", FontWeight.NORMAL, 14);
    private static final Font POWER_UP_FONT = Font.font("System", FontWeight.BOLD, 11);
    private static final double HUD_Y = 30;
    private static final double FLASH_TIME = 0.08;

    private final Canvas canvas;
    private final Particles particles = new Particles();
    private final Map<Brick, Double> flashes = new IdentityHashMap<>();
    private double scale = 1;
    private double offsetX;
    private double offsetY;

    GameView(Canvas canvas) {
        this.canvas = canvas;
    }

    /** Converts an x position on the canvas to playfield coordinates. */
    double toPlayfieldX(double canvasX) {
        return (canvasX - offsetX) / scale;
    }

    /** Starts effects for what just happened in the game. */
    void react(List<GameEvent> events) {
        for (GameEvent event : events) {
            switch (event) {
                case GameEvent.BrickHit hit -> flashes.put(hit.brick(), FLASH_TIME);
                case GameEvent.BrickDestroyed destroyed -> {
                    Rect b = destroyed.brick().bounds();
                    particles.burst(b.centerX(), b.centerY(), Palette.brickBase(destroyed.brick().type()));
                }
                case GameEvent.LevelCleared cleared -> particles.clear();
                default -> { }
            }
        }
    }

    void updateEffects(double dt) {
        particles.update(dt);
        flashes.replaceAll((brick, time) -> time - dt);
        flashes.values().removeIf(time -> time <= 0);
    }

    void render(Game game, int bestScore, boolean newBest) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        fitPlayfield();

        g.setTransform(1, 0, 0, 1, 0, 0);
        g.setFill(Palette.LETTERBOX);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g.setTransform(scale, 0, 0, scale, offsetX, offsetY);
        g.setFill(Palette.BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (Brick brick : game.bricks()) {
            drawBrick(g, brick, flashes.containsKey(brick));
        }
        particles.render(g);
        for (PowerUp powerUp : game.powerUps()) {
            drawPowerUp(g, powerUp);
        }
        drawPaddle(g, game);
        g.setFill(Palette.BALL);
        for (Ball ball : game.balls()) {
            double r = ball.radius();
            g.fillOval(ball.position().x() - r, ball.position().y() - r, 2 * r, 2 * r);
        }
        drawHud(g, game, bestScore);
        drawMessage(g, game, newBest);
    }

    private void fitPlayfield() {
        scale = Math.min(canvas.getWidth() / WIDTH, canvas.getHeight() / HEIGHT);
        offsetX = (canvas.getWidth() - WIDTH * scale) / 2;
        offsetY = (canvas.getHeight() - HEIGHT * scale) / 2;
    }

    private static void drawBrick(GraphicsContext g, Brick brick, boolean flashing) {
        Rect b = brick.bounds();
        g.setFill(flashing ? Palette.BALL : Palette.brick(brick));
        g.fillRoundRect(b.x(), b.y(), b.width(), b.height(), 6, 6);
        g.setFill(Palette.BALL.deriveColor(0, 1, 1, 0.18));
        g.fillRoundRect(b.x() + 2, b.y() + 2, b.width() - 4, 4, 4, 4);
    }

    private static void drawPowerUp(GraphicsContext g, PowerUp powerUp) {
        Rect b = powerUp.bounds();
        g.setFill(Palette.powerUp(powerUp.type()));
        g.fillRoundRect(b.x(), b.y(), b.width(), b.height(), b.height(), b.height());
        g.setFill(Palette.BACKGROUND);
        g.setFont(POWER_UP_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(label(powerUp.type()), b.centerX(), b.centerY());
    }

    private static String label(PowerUpType type) {
        return switch (type) {
            case WIDE_PADDLE -> "W";
            case MULTI_BALL -> "M";
            case SLOW_BALL -> "S";
            case EXTRA_LIFE -> "+";
        };
    }

    private static void drawPaddle(GraphicsContext g, Game game) {
        Rect p = game.paddle().bounds();
        g.setFill(Palette.PADDLE);
        g.fillRoundRect(p.x(), p.y(), p.width(), p.height(), p.height(), p.height());
    }

    private static void drawHud(GraphicsContext g, Game game, int bestScore) {
        g.setFont(HUD_FONT);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Palette.TEXT);

        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("Score " + game.score(), 20, HUD_Y);
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("Level " + (game.levelIndex() + 1) + " / " + game.levelCount(), WIDTH / 2, HUD_Y);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("Lives " + game.lives(), WIDTH - 20, HUD_Y);

        g.setFont(SMALL_FONT);
        g.setFill(Palette.TEXT_DIM);
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("Best " + Math.max(bestScore, game.score()), 20, HEIGHT - 14);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText(activeEffects(game), WIDTH - 20, HEIGHT - 14);
    }

    private static String activeEffects(Game game) {
        StringBuilder text = new StringBuilder();
        if (game.wideTimeLeft() > 0) {
            text.append("Wide ").append((int) Math.ceil(game.wideTimeLeft())).append("s   ");
        }
        if (game.slowTimeLeft() > 0) {
            text.append("Slow ").append((int) Math.ceil(game.slowTimeLeft())).append('s');
        }
        return text.toString().strip();
    }

    private static void drawMessage(GraphicsContext g, Game game, boolean newBest) {
        String finalScore = "Score " + game.score() + (newBest ? "  -  new high score!" : "");
        switch (game.state()) {
            case SERVING -> hint(g, "Press Space or click to launch");
            case PAUSED -> banner(g, "Paused", "Esc to resume   Q to quit to title", null);
            case LEVEL_CLEARED -> banner(g, "Level cleared!",
                    "Press Space for level " + (game.levelIndex() + 2), null);
            case GAME_OVER -> banner(g, "Game over", "R to play again   Esc for title", finalScore);
            case WON -> banner(g, "You win!", "R to play again   Esc for title", finalScore);
            case PLAYING -> { }
        }
    }

    private static void hint(GraphicsContext g, String text) {
        g.setFont(HINT_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Palette.TEXT_DIM);
        g.fillText(text, WIDTH / 2, HEIGHT * 0.65);
    }

    private static void banner(GraphicsContext g, String title, String hint, String detail) {
        g.setFill(Palette.OVERLAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(TITLE_FONT);
        g.setFill(Palette.ACCENT);
        g.fillText(title, WIDTH / 2, HEIGHT / 2 - 40);
        g.setFont(HINT_FONT);
        if (detail != null) {
            g.setFill(Palette.TEXT);
            g.fillText(detail, WIDTH / 2, HEIGHT / 2 + 10);
        }
        g.setFill(Palette.TEXT_DIM);
        g.fillText(hint, WIDTH / 2, HEIGHT / 2 + 44);
    }
}
