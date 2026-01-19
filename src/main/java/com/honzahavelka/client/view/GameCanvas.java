package com.honzahavelka.client.view;

import com.honzahavelka.client.model.GameState;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class GameCanvas extends Canvas {

    private final GameState gameState;
    private final AnimationTimer timer;

    private static final double PADDLE_WIDTH = 10;
    private static final double PADDLE_HEIGHT = 100;
    private static final double BALL_SIZE = 10;
    private static final double SCORE_PIXEL_SIZE = 20;

    // --- OPTIMALIZACE: Definice čísel jako statická konstanta (načte se jen jednou) ---
    // 3D pole: [číslo][řádek][sloupec]
    private static final int[][][] PIXEL_NUMBERS = {
            {{1,1,1},{1,0,1},{1,0,1},{1,0,1},{1,1,1}}, // 0
            {{0,0,1},{0,0,1},{0,0,1},{0,0,1},{0,0,1}}, // 1
            {{1,1,1},{0,0,1},{1,1,1},{1,0,0},{1,1,1}}, // 2
            {{1,1,1},{0,0,1},{1,1,1},{0,0,1},{1,1,1}}, // 3
            {{1,0,1},{1,0,1},{1,1,1},{0,0,1},{0,0,1}}, // 4
            {{1,1,1},{1,0,0},{1,1,1},{0,0,1},{1,1,1}}, // 5
            {{1,1,1},{1,0,0},{1,1,1},{1,0,1},{1,1,1}}, // 6
            {{1,1,1},{0,0,1},{0,0,1},{0,0,1},{0,0,1}}, // 7
            {{1,1,1},{1,0,1},{1,1,1},{1,0,1},{1,1,1}}, // 8
            {{1,1,1},{1,0,1},{1,1,1},{0,0,1},{1,1,1}}  // 9
    };
    // Zástupný znak pro chybu (otazník nebo plný blok)
    private static final int[][] ERROR_SHAPE = {{1,0,1},{0,1,0},{1,0,1},{0,1,0},{1,0,1}};
    private long lastTime = 0;

    public GameCanvas(GameState gameState) {

        super(800, 600);
        this.gameState = gameState;


        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double dt = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                if (dt > 0.1) dt = 0.1;

                gameState.updatePhysics(dt);

                draw();
            }
        };
    }

    public void start() { timer.start(); }
    public void stop() { timer.stop(); }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // Vykreslování (stejné jako předtím)
        gc.setImageSmoothing(false);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);
        gc.setFill(Color.WHITE);

        // Středová čára
        double dashHeight = 20;
        double gap = 20;
        double xCenter = w / 2 - 5;
        for (double y = gap / 2; y < h; y += dashHeight + gap) {
            gc.fillRect(xCenter, y, 10, dashHeight);
        }

        // Pálky a míček
        gc.fillRect(10, gameState.player1.getY(), PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.fillRect(w - 10 - PADDLE_WIDTH, gameState.player2.getY(), PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.fillRect(gameState.ball.getX() - BALL_SIZE / 2, gameState.ball.getY() - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);

        // Skóre
        drawPixelNumber(gc, gameState.score1, w / 2 - 100, 50);
        drawPixelNumber(gc, gameState.score2, w / 2 + 60, 50);
    }

    private void drawPixelNumber(GraphicsContext gc, int number, double x, double y) {
        // Ochrana proti číslům mimo rozsah 0-9
        int[][] shape = (number >= 0 && number <= 9) ? PIXEL_NUMBERS[number] : ERROR_SHAPE;

        double s = SCORE_PIXEL_SIZE;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                if (shape[row][col] == 1) {
                    gc.fillRect(x + col * s, y + row * s, s, s);
                }
            }
        }
    }
}