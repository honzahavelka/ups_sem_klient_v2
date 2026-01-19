package com.honzahavelka.client.model;

import com.honzahavelka.client.model.Ball;
import com.honzahavelka.client.model.Paddle;

public class GameState {

    public Paddle player1;
    public Paddle player2;
    public Ball ball;
    public volatile int score1 = 0;
    public volatile int score2 = 0;

    // Herní stavy
    public volatile boolean amILeft = true; // Default, upraví GAST příkaz
    public volatile boolean gameOver = false;

    public boolean p1_paused = false;
    public boolean p2_paused = false;

    public boolean reconnecting = false;

    public volatile String winnerText = "";

    public GameState() {
        player1 = new Paddle(10, 250, 600);
        player2 = new Paddle(780, 250, 600);
        ball = new Ball(395, 295, 500);
    }

    public void updatePhysics(double dt) {
        if (!p1_paused && !p2_paused) {
            player1.update(dt);
            player2.update(dt);
            ball.update(dt);
        }
    }

    public void resetPhysics() {
        p1_paused = false;
        p2_paused = false;
        player1.setY(250);
        player2.setY(250);
        ball.setPosition(395, 295);
        ball.setDx(500);
        ball.setDy(0);
        score1 = 0;
        score2 = 0;
    }
}