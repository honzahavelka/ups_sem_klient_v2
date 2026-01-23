package com.honzahavelka.client.model;

// třída ukládá míč
public class Ball {
    // velikost
    public static final double SIZE = 10;

    // pozice
    private double x, y;
    private double dx, dy; // vektor pohybu
    private double speed;  // celková rychlost

    // konst
    public Ball(double startX, double startY, double initialSpeed) {
        this.x = startX;
        this.y = startY;
        this.speed = initialSpeed;
        this.dx = speed;
        this.dy = 0;
    }

    // update pozice podle času
    public void update(double dt) {
        x += dx * dt;
        y += dy * dt;

        checkWallCollision();
    }

    // invertuje dy při odrazu od stěny
    private void checkWallCollision() {
        // Jednoduchý odraz od stropu a podlahy (čistě klientská predikce)
        if (y <= 0) {
            y = 0;
            dy = -dy;
        }
        else if (y + SIZE >= 600) {
            y = 600 - SIZE;
            dy = -dy;
        }
    }

    // nastaví vektor
    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    // pro synchronizaci
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // gettery
    public double getX() { return x; }
    public double getY() { return y; }
}
