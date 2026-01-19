package com.honzahavelka.client.model;

public class Ball {
    public static final double SIZE = 10;

    private double x, y;
    private double dx, dy; // Vektor pohybu
    private double speed;  // Celková rychlost

    public Ball(double startX, double startY, double initialSpeed) {
        this.x = startX;
        this.y = startY;
        this.speed = initialSpeed;
        this.dx = speed;
        this.dy = 0;
    }

    // Volat v GameLoopu
    public void update(double dt) {
        // dx a dy jsou nyní v "pixelech za sekundu"
        x += dx * dt;
        y += dy * dt;

        checkWallCollision();
    }

    private void checkWallCollision() {
        // Jednoduchý odraz od stropu a podlahy (čistě klientská predikce)
        // 600 je výška okna (můžeš předávat v konstruktoru)
        if (y <= 0) {
            y = 0;
            dy = -dy;
        }
        else if (y + SIZE >= 600) {
            y = 600 - SIZE;
            dy = -dy;
        }
    }

    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    // Pro tvrdou synchronizaci pozice (když server pošle "Tady je míček")
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // --- Gettery ---
    public double getX() { return x; }
    public double getY() { return y; }
}
