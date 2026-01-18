package com.honzahavelka.client.controller;

import com.honzahavelka.client.Main;
import com.honzahavelka.client.net.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.input.KeyEvent;

public class MenuController {

    @FXML private TextField nickField;
    @FXML private Button loginBtn;
    @FXML private VBox lobbyBox;
    @FXML private TextField lobbyField;
    @FXML private Label statusLabel;

    @FXML private Button leaveBtn;
    @FXML private Button joinBtn;

    // --- NOVÉ PRO ESC MENU ---
    @FXML private StackPane rootPane; // Kořen (kvůli focusu)
    @FXML private VBox escMenuBox;    // Overlay menu
    @FXML private VBox contentBox;    // Původní obsah (pro rozostření/disable)

    private NetworkClient networkClient;

    @FXML
    public void initialize() {
        networkClient = Main.getNetworkClient();
        if (networkClient == null) {
            statusLabel.setText("Chyba: Nepřipojeno!");
            loginBtn.setDisable(true);
            return;
        }

        // Nastavíme listener
        networkClient.setOnMessageReceived(this::processMessage);

        // --- NOVÉ: KONTROLA PŘEDCHOZÍHO PŘIHLÁŠENÍ ---
        String savedNick = Main.getLoggedInNick();

        if (savedNick != null) {
            // Hráč už je přihlášený z minula a socket stále běží!
            // 1. Vyplníme pole
            nickField.setText(savedNick);

            // 2. Zamkneme Login sekci (už není potřeba posílat LOGI)
            nickField.setDisable(true);
            loginBtn.setDisable(true);

            // 3. Odemkneme Lobby sekci
            lobbyBox.setDisable(false);

            // 4. Informujeme uživatele
            statusLabel.setText("Vítej zpět, " + savedNick);
            statusLabel.setStyle("-fx-text-fill: green;");
        }
        Platform.runLater(() -> rootPane.requestFocus());
    }

    // --- OVLÁDÁNÍ KLÁVESNICE ---
    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            toggleEscMenu();
        }
    }

    private void toggleEscMenu() {
        boolean isVisible = escMenuBox.isVisible();
        escMenuBox.setVisible(!isVisible);
        contentBox.setDisable(!isVisible); // Deaktivujeme tlačítka vespod, aby nešlo klikat "skrz"

        if (!isVisible) {
            // Pokud jsme menu právě otevřeli
            statusLabel.setText("Menu otevřeno");
        }
    }

    @FXML
    protected void onResumeClick() {
        toggleEscMenu(); // Jen zavře menu
    }

    @FXML
    protected void onDisconnectClick() {
        // 1. Zavřít spojení
        if (networkClient != null) {
            // Pošleme serveru sbohem (volitelné, server to pozná i tak)
            // networkClient.send("LEAV");
            networkClient.close();
        }

        // 2. Vyčistit globální stav v Main
        Main.setNetworkClient(null);
        Main.setLoggedInNick(null); // Zapomeneme nick, aby se příště musel přihlásit znovu

        // 3. Přepnout na Connect obrazovku
        Main.showConnectScreen();
    }

    @FXML
    protected void onExitClick() {
        // Ukončit celou aplikaci
        if (networkClient != null) {
            networkClient.close();
        }
        Platform.exit();
        System.exit(0);
    }

    @FXML
    protected void onLoginClick() {
        String nick = nickField.getText();
        if (nick.isEmpty()) {
            statusLabel.setText("Zadej nick!");
            return;
        }
        // Odeslání protokolu: LOGI <Nick>
        networkClient.send("LOGI " + nick);
        statusLabel.setText("Přihlašuji...");
    }

    @FXML
    protected void onJoinClick() {
        String lobbyId = lobbyField.getText();
        if (lobbyId.isEmpty()) {
            statusLabel.setText("Zadej ID lobby!");
            return;
        }
        // Odeslání protokolu: JOIN <LobbyID>
        networkClient.send("JOIN " + lobbyId);
        statusLabel.setText("Připojuji do lobby " + lobbyId + "...");
    }

    // --- NOVÁ METODA PRO TLAČÍTKO LEAVE ---
    @FXML
    protected void onLeaveClick() {
        networkClient.send("LEAV");
        statusLabel.setText("Opouštím lobby...");
        leaveBtn.setDisable(true); // Aby na to neklikal víckrát
    }

    // Metoda pro zpracování odpovědí ze serveru
    // V MenuController.java uprav metodu processMessage:

    private void processMessage(String msg) {
        System.out.println("Message received: " + msg);
        Platform.runLater(() -> {
            String[] parts = msg.split(" ");
            String cmd = parts[0];

            switch (cmd) {
                case "LOOK":
                    String nick = parts[1];

                    // --- NOVÉ: ULOŽÍME NICK PRO PŘÍŠTĚ ---
                    Main.setLoggedInNick(nick);

                    statusLabel.setText("Přihlášen jako: " + nick);
                    statusLabel.setStyle("-fx-text-fill: green;");
                    nickField.setDisable(true);
                    loginBtn.setDisable(true);
                    lobbyBox.setDisable(false);
                    break;

                case "JOOK":
                    // ZMĚNA: Tady už nepřepínáme scénu!
                    // Jen zamkneme UI a informujeme uživatele.
                    statusLabel.setText("Vstoupil jsi do lobby. Čekám na hru...");
                    statusLabel.setStyle("-fx-text-fill: orange;");


                    // Zamkneme možnost připojit se jinam
                    lobbyField.setDisable(true);
                    joinBtn.setDisable(true);

                    // Odemkneme možnost odejít
                    leaveBtn.setDisable(false);
                    break;

                case "LEOK":
                    statusLabel.setText("Lobby opuštěno. Vyber nové.");
                    statusLabel.setStyle("-fx-text-fill: black;");

                    // Vrátíme UI do stavu pro výběr lobby
                    lobbyField.setDisable(false);
                    joinBtn.setDisable(false);
                    leaveBtn.setDisable(true); // Už není co opouštět
                    break;

                case "GAST":
                    // NOVINKA: Toto je ten moment startu!
                    // Protokol: GAST LEFT nebo GAST RIGHT
                    boolean amILeft = parts[1].equals("0");

                    // Přepneme do hry a předáme stranu
                    Main.switchToGame(amILeft);
                    break;

                case "ERRO":
                    String errorMsg = msg.substring(5);
                    statusLabel.setText("Chyba: " + errorMsg);
                    statusLabel.setStyle("-fx-text-fill: red;");
                    break;
            }
        });
    }
}