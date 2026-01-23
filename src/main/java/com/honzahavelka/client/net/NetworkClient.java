package com.honzahavelka.client.net;

import com.honzahavelka.client.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    // soket, writer, reader
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    // vlákno na čtení
    private Thread listenerThread;
    private boolean running = false;

    // Callback - funkce, kterou zavoláme, když přijde zpráva
    private Consumer<String> onMessageReceived;


    // kost
    public NetworkClient(String host, int port, Consumer<String> onMessageReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;

        // pokud to selže chybu chytí connect controller a ví, že se nepřipojil
        // pak si poradí
        System.out.println("Připojuji se k " + host + ":" + port + "...");
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        startListening();
        System.out.println("Připojeno!");
    }

    // init poslouchací vlákno
    private void startListening() {
        running = true;
        listenerThread = new Thread(() -> {
            try {
                String line;
                // čteme dokud je spojení a chodí data
                while (running && (line = in.readLine()) != null) {

                    if (line.startsWith("FBAN")) {
                        // pokud je to BAN, okamžitě to řešíme globálně
                        // nemusíme to posílat do Controlleru, ten už nic nezmůže
                        Main.showBanScreen(line);

                        // ukončíme smyčku čtení, protože jsme skončili
                        running = false;
                        break;
                    }
                    onMessageReceived.accept(line); // předáme zprávu dál
                }

                // detekce ukončení ze strany serveru
                if (running) {
                    System.out.println("Server ukončil spojení (EOF).");
                    handleServerDisconnect();
                }
            } catch (IOException e) {
                if (running) System.out.println("Spojení přerušeno serverem.");
            }
        });
        listenerThread.setDaemon(true); // vlákno se ukončí, když se vypne aplikace
        listenerThread.start();
    }

    public void send(String message) {
        if (out != null) {
            System.out.println("Odesílám: " + message); // debug výpis
            out.println(message); // println přidá na konec \n, což server vyžaduje
        }
    }

    // ukončení vlákna
    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // setter pro callback fci
    public void setOnMessageReceived(Consumer<String> listener) {
        this.onMessageReceived = listener;
    }

    // co dělat když server ukončí spojení
    private void handleServerDisconnect() {
        running = false; // už nemá smysl číst dál
        com.honzahavelka.client.Main.handleConnectionLost();
    }
}