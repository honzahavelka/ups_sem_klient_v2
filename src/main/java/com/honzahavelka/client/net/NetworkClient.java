package com.honzahavelka.client.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private boolean running = false;

    // Callback - funkce, kterou zavoláme, když přijde zpráva
    private Consumer<String> onMessageReceived;

    public NetworkClient(String host, int port, Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        try {
            System.out.println("Připojuji se k " + host + ":" + port + "...");
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true); // true = autoFlush (důležité pro \n)
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Spustíme naslouchání
            startListening();
            System.out.println("Připojeno!");

        } catch (IOException e) {
            System.err.println("Chyba připojení: " + e.getMessage());
        }
    }

    private void startListening() {
        running = true;
        listenerThread = new Thread(() -> {
            try {
                String line;
                // Čteme dokud je spojení a chodí data
                while (running && (line = in.readLine()) != null) {
                    onMessageReceived.accept(line); // Předáme zprávu dál
                }
            } catch (IOException e) {
                if (running) System.out.println("Spojení přerušeno serverem.");
            }
        });
        listenerThread.setDaemon(true); // Vlákno se ukončí, když se vypne aplikace
        listenerThread.start();
    }

    public void send(String message) {
        if (out != null) {
            System.out.println("Odesílám: " + message); // Debug výpis
            out.println(message); // println přidá na konec \n, což server vyžaduje
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Přidej toto do NetworkClient.java
    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener; // Poznámka: odstraň 'final' u proměnné onMessageReceived
    }
}