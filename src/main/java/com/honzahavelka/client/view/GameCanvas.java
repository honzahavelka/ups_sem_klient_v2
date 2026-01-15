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
    private static final double PADDLE_W = 10;
    private static final double PADDLE_H = 100;
    private static final double BALL_S = 10;

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

        // 1. Vymazat pozadí (černá)
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Vykreslit skóre
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Monospaced", 40));
        gc.fillText(gameState.score1 + " : " + gameState.score2, WIDTH / 2 - 60, 50);

        // 3. Vykreslit pálky
        // Levá pálka (X=10 - jen odhad, záleží jak server počítá kolize, doladíme)
        gc.setFill(gameState.amILeft ? Color.CYAN : Color.WHITE); // Moje pálka modrá
        gc.fillRect(10, gameState.paddle1Y, PADDLE_W, PADDLE_H);

        // Pravá pálka (X=780)
        gc.setFill(!gameState.amILeft ? Color.CYAN : Color.WHITE);
        gc.fillRect(WIDTH - 20, gameState.paddle2Y, PADDLE_W, PADDLE_H);

        // 4. Vykreslit míček
        gc.setFill(Color.YELLOW);
        gc.fillRect(gameState.ballX, gameState.ballY, BALL_S, BALL_S);

        // 5. Game Over Text
        if (gameState.gameOver) {
            gc.setFill(Color.RED);
            gc.setFont(new Font("Arial", 60));
            gc.fillText(gameState.winnerText, WIDTH / 2 - 200, HEIGHT / 2);
        }
    }
}