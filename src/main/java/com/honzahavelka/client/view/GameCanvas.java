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

    // Konstanty rozměrů (dle specifikace serveru)
    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private static final double PADDLE_WIDTH = 10;
    private static final double PADDLE_HEIGHT = 100;
    private static final double BALL_SIZE = 10;
    private static final double SCORE_PIXEL_SIZE = 20;

    public GameCanvas(GameState gameState) {
        super(WIDTH, HEIGHT);
        this.gameState = gameState;

        // Smyčka, která běží synchronizovaně s obnovovací frekvencí monitoru (cca 60 FPS)
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                draw();
            }
        };
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. DŮLEŽITÉ: Vypnutí vyhlazování pro retro vzhled
        gc.setImageSmoothing(false);

        // 2. Pozadí (Čistá černá)
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // Všechno ostatní bude bílé
        gc.setFill(Color.WHITE);

        // 3. Středová přerušovaná čára
        double dashWidth = 10;
        double dashHeight = 20;
        double gap = 20;
        double xCenter = w / 2 - dashWidth / 2;
        for (double y = gap / 2; y < h; y += dashHeight + gap) {
            gc.fillRect(xCenter, y, dashWidth, dashHeight);
        }

        // 4. Pálky (Hranaté)
        // Levá
        gc.fillRect(10, gameState.paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        // Pravá (odsazená od kraje)
        gc.fillRect(w - 10 - PADDLE_WIDTH, gameState.paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT);

        // 5. Míček (Čtvercový)
        // Souřadnice míčku ze serveru jsou jeho střed, musíme posunout na levý horní roh
        gc.fillRect(gameState.ballX - BALL_SIZE / 2, gameState.ballY - BALL_SIZE / 2, BALL_SIZE, BALL_SIZE);

        // 6. Retro Skóre
        // Kreslíme vlastní pixelová čísla
        drawPixelNumber(gc, gameState.score1, w / 2 - 100, 50);
        drawPixelNumber(gc, gameState.score2, w / 2 + 60, 50);
    }

    // --- Pomocná metoda pro kreslení pixelových čísel ---
    private void drawPixelNumber(GraphicsContext gc, int number, double x, double y) {
        double s = SCORE_PIXEL_SIZE; // Zkratka pro velikost

        // Definice tvarů čísel (pomocí matice 3x5 pixelů)
        // 1 = kreslit pixel, 0 = nekreslit
        int[][] shape = switch (number) {
            case 0 -> new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 0, 1}, {1, 0, 1}, {1, 1, 1}};
            case 1 -> new int[][]{{0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}};
            case 2 -> new int[][]{{1, 1, 1}, {0, 0, 1}, {1, 1, 1}, {1, 0, 0}, {1, 1, 1}};
            case 3 -> new int[][]{{1, 1, 1}, {0, 0, 1}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
            case 4 -> new int[][]{{1, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 0, 1}, {0, 0, 1}};
            case 5 -> new int[][]{{1, 1, 1}, {1, 0, 0}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
            case 6 -> new int[][]{{1, 1, 1}, {1, 0, 0}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
            case 7 -> new int[][]{{1, 1, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}, {0, 0, 1}};
            case 8 -> new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {1, 0, 1}, {1, 1, 1}};
            case 9 -> new int[][]{{1, 1, 1}, {1, 0, 1}, {1, 1, 1}, {0, 0, 1}, {1, 1, 1}};
            default -> new int[][]{{1, 0, 1}, {0, 1, 0}, {1, 0, 1}, {0, 1, 0}, {1, 0, 1}}; // ? pro chybu
        };

        // Vykreslení podle matice
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                if (shape[row][col] == 1) {
                    gc.fillRect(x + col * s, y + row * s, s, s);
                }
            }
        }
    }
}