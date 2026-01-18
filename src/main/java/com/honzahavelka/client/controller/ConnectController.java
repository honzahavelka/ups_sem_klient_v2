package com.honzahavelka.client.controller;

import com.honzahavelka.client.Main;
import com.honzahavelka.client.net.NetworkClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
public class ConnectController {

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectBtn;
    @FXML private Label errorLabel;

    // Dočasná reference, dokud nepotvrdíme HELO
    private NetworkClient tempClient;

    @FXML
    protected void onConnectClick() {
        errorLabel.setText("Připojuji...");
        connectBtn.setDisable(true); // Aby na to neklikal 10x

        String ip = ipField.getText();
        String portStr = portField.getText();

        // 1. Validace vstupu
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError("Port musí být číslo!");
            return;
        }

        // 2. Pokus o vytvoření socketu (neblokujeme UI vlákno dlouho, ale socket connect je rychlý)
        new Thread(() -> {
            try {
                // Vytvoříme klienta a rovnou nastavíme listener na handshake
                tempClient = new NetworkClient(ip, port, this::processHandshake);

            } catch (IOException e) {
                // Server neběží nebo špatná IP
                Platform.runLater(() -> showError("Nelze se připojit k serveru.\nzkontroluj IP a Port."));
            }
        }).start();
    }


    // Listener pro první zprávu (Handshake)
    private void processHandshake(String msg) {
        Platform.runLater(() -> {
            if (msg.startsWith("HELO")) {
                // --- ÚSPĚCH ---
                System.out.println("Server přijal spojení: " + msg);

                // 1. Uložíme klienta do Mainu (aby ho mělo Menu i Hra)
                Main.setNetworkClient(tempClient);

                // 2. Přepneme na Menu
                Main.switchToMenu();

            } else if (msg.startsWith("ERRO")) {
                // --- SERVER ODMÍTL (např. plno) ---
                String error = msg.length() > 5 ? msg.substring(5) : "Neznámá chyba";
                showError("Server odmítl připojení:\n" + error);
                closeTempClient();

            } else {
                // --- DIVNÁ ODPOVĚĎ ---
                showError("Neplatný protokol serveru.\nPřišlo: " + msg);
                closeTempClient();
            }
        });
    }

    private void showError(String text) {
        errorLabel.setText(text);
        connectBtn.setDisable(false); // Povolíme znovu tlačítko
    }

    private void closeTempClient() {
        if (tempClient != null) {
            tempClient.close();
            tempClient = null;
        }
    }
}
