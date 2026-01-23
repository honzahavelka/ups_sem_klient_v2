package com.honzahavelka.client.model;

// třída drží pozici a hejbe s pádlem
public class Paddle {

    // Konstanty pro design, stejné jako v GameCanvas
    public static final double WIDTH = 10;
    public static final double HEIGHT = 100;

    // Rychlost pohybu pálky px za sec
    private static final double SPEED = 400.0;

    private double x;
    private double y;

    // Aktuální směr pohybu (-1 nahoru, 0 stojí, 1 dolů)
    private int directionY = 0;

    // Rozměr hřiště pro kolize
    private final double fieldHeight;

    // konst
    public Paddle(double startX, double startY, double fieldHeight) {
        this.x = startX;
        this.y = startY;
        this.fieldHeight = fieldHeight;
    }

    // update pádla podle času dt
    public void update(double dt) {
        if (directionY != 0) {
            // Vzorec: nová_pozice = stará + (rychlost * čas)
            y += directionY * SPEED * dt;
            clampPosition();
        }
    }

    // zabrání vyjetí pálky z obrazovky
    private void clampPosition() {
        if (y < 0) {
            y = 0;
        } else if (y + HEIGHT > fieldHeight) {
            y = fieldHeight - HEIGHT;
        }
    }

    // ovládání
    public void stop() {
        this.directionY = 0;
    }
    public void moveUp() {
        this.directionY = -1;
    }
    public void moveDown() {
        this.directionY = 1;
    }

    // pro synchronizaci se serverem
    public void setY(double y) {
        this.y = y;
        clampPosition();
    }

    // gettery
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return WIDTH; }
    public double getHeight() { return HEIGHT; }
}