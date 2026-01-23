package com.honzahavelka.client.controller;

import com.honzahavelka.client.Main;
import com.honzahavelka.client.net.NetworkClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

// controller pro připojení se k serveru
public class ConnectController {

    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectBtn;
    @FXML private Label errorLabel;

    // dočasná reference, dokud nepotvrdíme HELO
    private NetworkClient tempClient;

    @FXML
    protected void onConnectClick() {
        errorLabel.setText("Připojuji...");
        connectBtn.setDisable(true); // Aby na to neklikal 10x

        String ip = ipField.getText();
        String portStr = portField.getText();

        // validace vstupu
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError("Port musí být číslo!");
            return;
        }

        // pokus o vytvoření socketu
        new Thread(() -> {
            try {
                // vytvoříme klienta a rovnou nastavíme listener na handshake
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
            try {
                if (msg.startsWith("HELO")) {
                    // úspěch
                    System.out.println("Server přijal spojení: " + msg);

                    // uložíme klienta do Mainu
                    Main.setNetworkClient(tempClient);

                    // přepneme na Menu
                    Main.switchToMenu();

                } else if (msg.startsWith("ERRO")) {
                    // server odmítl připojení
                    String error = msg.length() > 5 ? msg.substring(5) : "Neznámá chyba";
                    showError("Server odmítl připojení.");
                    closeTempClient();

                } else {
                    // nevalidní odpověď
                    showError("Neplatný protokol serveru.");
                    closeTempClient();
                }

            } catch (Exception e) {
                // jakákoli další chyba
                e.printStackTrace();
                showError("Chyba při inicializaci aplikace:\n" + e.getMessage());
                closeTempClient();
            }
        });
    }

    // ukaž chybu
    private void showError(String text) {
        errorLabel.setText(text);
        connectBtn.setDisable(false); // Povolíme znovu tlačítko
    }

    // zavři dočasnýho klienta
    private void closeTempClient() {
        if (tempClient != null) {
            tempClient.close();
            tempClient = null;
        }
    }

    // nastav chybu
    public void setErrorMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;"); // Pro jistotu červeně
    }
}
