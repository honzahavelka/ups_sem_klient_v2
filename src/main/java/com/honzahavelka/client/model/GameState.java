package com.honzahavelka.client.model;

public class GameState {
    // Pozice a skóre (volatile = bezpečné pro čtení z jiného vlákna)
    public volatile double ballX = 400;
    public volatile double ballY = 300;

    public volatile double paddle1Y = 250; // Levá pálka
    public volatile double paddle2Y = 250; // Pravá pálka

    public volatile int score1 = 0;
    public volatile int score2 = 0;

    // Herní stavy
    public volatile boolean amILeft = true; // Default, upraví GAST příkaz
    public volatile boolean gameOver = false;
    public volatile String winnerText = "";
}