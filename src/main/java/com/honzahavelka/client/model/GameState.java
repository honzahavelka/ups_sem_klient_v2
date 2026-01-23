package com.honzahavelka.client.model;

import com.honzahavelka.client.model.Ball;
import com.honzahavelka.client.model.Paddle;

// třída ukládá celej herní stav a aktualizuje ho
public class GameState {

    // pádlo levá
    public Paddle player1;
    // pádlo pravá
    public Paddle player2;

    // míč a skóre
    public Ball ball;
    public volatile int score1 = 0;
    public volatile int score2 = 0;

    // herní stavy
    public volatile boolean amILeft = true; // upraví GAST příkaz
    public volatile boolean gameOver = false; // zastavi update

    // pauzy
    public boolean p1_paused = false;
    public boolean p2_paused = false;

    // reconnect stopper
    public boolean reconnecting = false;

    // text po hře
    public volatile String winnerText = "";

    // konst
    public GameState() {
        player1 = new Paddle(10, 250, 600);
        player2 = new Paddle(780, 250, 600);
        ball = new Ball(395, 295, 500);
    }

    // update pozicí podle času dt
    public void updatePhysics(double dt) {
        if (!p1_paused && !p2_paused) {
            player1.update(dt);
            player2.update(dt);
            ball.update(dt);
        }
    }

    // resetování hry
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