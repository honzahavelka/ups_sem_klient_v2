package com.honzahavelka.client.view;

import com.honzahavelka.client.model.GameState;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;


// třída co kreslí herní entity na plátno
public class GameCanvas extends Canvas {

    // gamestate
    private final GameState gameState;
    private final AnimationTimer timer;

    // setup pro pádla a míček, musí sedět s nastavením serveru
    private static final double PADDLE_WIDTH = 10;
    private static final double PADDLE_HEIGHT = 100;
    private static final double BALL_SIZE = 10;
    private static final double SCORE_PIXEL_SIZE = 20;

    // pixelový vykreslení skóre
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
    // pokud by byla nejaka chyba
    private static final int[][] ERROR_SHAPE = {{1,0,1},{0,1,0},{1,0,1},{0,1,0},{1,0,1}};

    // last time pro zjištění kolik času uběhlo mezi snímky
    private long lastTime = 0;

    // konst
    public GameCanvas(GameState gameState) {

        super(800, 600);
        this.gameState = gameState;


        // timer, volá update funkce gamestate
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

    // start
    public void start() { timer.start(); }
    // stop
    public void stop() { timer.stop(); }

    // nakreslí gamestate
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

    // pomocná funkce pro nakreslení skóre
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