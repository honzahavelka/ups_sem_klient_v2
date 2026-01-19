package com.honzahavelka.client.net;

import com.honzahavelka.client.Main;

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

    // V NetworkClient.java

    // Změň hlavičku konstruktoru a vyhoď try-catch kolem new Socket
    public NetworkClient(String host, int port, Consumer<String> onMessageReceived) throws IOException {
        this.onMessageReceived = onMessageReceived;

        // Tady už není try-catch! Pokud to selže, chybu chytí ConnectController
        System.out.println("Připojuji se k " + host + ":" + port + "...");
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        startListening();
        System.out.println("Připojeno!");
    }

    private void startListening() {
        running = true;
        listenerThread = new Thread(() -> {
            try {
                String line;
                // Čteme dokud je spojení a chodí data
                while (running && (line = in.readLine()) != null) {

                    if (line.startsWith("FBAN")) {
                        // Pokud je to BAN, okamžitě to řešíme globálně
                        // Nemusíme to posílat do Controlleru, ten už nic nezmůže
                        Main.showBanScreen(line);

                        // Ukončíme smyčku čtení, protože jsme skončili
                        running = false;
                        break;
                    }
                    onMessageReceived.accept(line); // Předáme zprávu dál
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

    private void handleServerDisconnect() {
        running = false; // Už nemá smysl číst dál

        // Musíme zavolat Main, aby přepnul grafiku (musí to být v runLater)
        // Můžeme Reuse metody showConnectScreen, nebo ukázat Alert
        com.honzahavelka.client.Main.handleConnectionLost();
    }
}