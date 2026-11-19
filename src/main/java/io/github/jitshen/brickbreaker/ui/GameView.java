package io.github.jitshen.brickbreaker.ui;

import static io.github.jitshen.brickbreaker.model.GameConfig.HEIGHT;
import static io.github.jitshen.brickbreaker.model.GameConfig.WIDTH;

import io.github.jitshen.brickbreaker.model.Ball;
import io.github.jitshen.brickbreaker.model.Brick;
import io.github.jitshen.brickbreaker.model.Game;
import io.github.jitshen.brickbreaker.model.Rect;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Draws the game onto a canvas. The 800 x 600 playfield is scaled to fit the canvas, keeping its
 * shape, with dark bars filling any leftover space.
 */
final class GameView {

    private static final Font HUD_FONT = Font.font("System", FontWeight.BOLD, 18);
    private static final Font TITLE_FONT = Font.font("System", FontWeight.BOLD, 44);
    private static final Font HINT_FONT = Font.font("System", FontWeight.NORMAL, 18);
    private static final double HUD_Y = 30;

    private final Canvas canvas;
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

    void render(Game game) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        fitPlayfield();

        g.setTransform(1, 0, 0, 1, 0, 0);
        g.setFill(Palette.LETTERBOX);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        g.setTransform(scale, 0, 0, scale, offsetX, offsetY);
        g.setFill(Palette.BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (Brick brick : game.bricks()) {
            drawBrick(g, brick);
        }
        drawPaddle(g, game);
        g.setFill(Palette.BALL);
        for (Ball ball : game.balls()) {
            double r = ball.radius();
            g.fillOval(ball.position().x() - r, ball.position().y() - r, 2 * r, 2 * r);
        }
        drawHud(g, game);
        drawMessage(g, game);
    }

    private void fitPlayfield() {
        scale = Math.min(canvas.getWidth() / WIDTH, canvas.getHeight() / HEIGHT);
        offsetX = (canvas.getWidth() - WIDTH * scale) / 2;
        offsetY = (canvas.getHeight() - HEIGHT * scale) / 2;
    }

    private static void drawBrick(GraphicsContext g, Brick brick) {
        Rect b = brick.bounds();
        g.setFill(Palette.brick(brick));
        g.fillRoundRect(b.x(), b.y(), b.width(), b.height(), 6, 6);
        g.setFill(Palette.BALL.deriveColor(0, 1, 1, 0.18));
        g.fillRoundRect(b.x() + 2, b.y() + 2, b.width() - 4, 4, 4, 4);
    }

    private static void drawPaddle(GraphicsContext g, Game game) {
        Rect p = game.paddle().bounds();
        g.setFill(Palette.PADDLE);
        g.fillRoundRect(p.x(), p.y(), p.width(), p.height(), p.height(), p.height());
    }

    private static void drawHud(GraphicsContext g, Game game) {
        g.setFont(HUD_FONT);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Palette.TEXT);

        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("Score " + game.score(), 20, HUD_Y);
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("Level " + (game.levelIndex() + 1) + " / " + game.levelCount(), WIDTH / 2, HUD_Y);
        g.setTextAlign(TextAlignment.RIGHT);
        g.fillText("Lives " + game.lives(), WIDTH - 20, HUD_Y);
    }

    private static void drawMessage(GraphicsContext g, Game game) {
        switch (game.state()) {
            case SERVING -> hint(g, "Press Space or click to launch");
            case PAUSED -> banner(g, "Paused", "Press Esc to resume");
            case LEVEL_CLEARED -> banner(g, "Level cleared!",
                    "Press Space for level " + (game.levelIndex() + 2));
            case GAME_OVER -> banner(g, "Game over", "Press R to play again");
            case WON -> banner(g, "You win!", "Press R to play again");
            case PLAYING -> { }
        }
    }

    private static void hint(GraphicsContext g, String text) {
        g.setFont(HINT_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Palette.TEXT_DIM);
        g.fillText(text, WIDTH / 2, HEIGHT * 0.65);
    }

    private static void banner(GraphicsContext g, String title, String subtitle) {
        g.setFill(Palette.OVERLAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(TITLE_FONT);
        g.setFill(Palette.ACCENT);
        g.fillText(title, WIDTH / 2, HEIGHT / 2 - 24);
        g.setFont(HINT_FONT);
        g.setFill(Palette.TEXT);
        g.fillText(subtitle, WIDTH / 2, HEIGHT / 2 + 24);
    }
}
