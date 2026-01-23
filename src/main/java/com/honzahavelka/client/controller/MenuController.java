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


// controller pro menu - login a připojení do lobby
public class MenuController {

    @FXML private TextField nickField;
    @FXML private Button loginBtn;
    @FXML private VBox lobbyBox;
    @FXML private TextField lobbyField;
    @FXML private Label statusLabel;

    @FXML private Button leaveBtn;
    @FXML private Button joinBtn;

    // escape menu
    @FXML private StackPane rootPane; // Kořen (kvůli focusu)
    @FXML private VBox escMenuBox;    // Overlay menu
    @FXML private VBox contentBox;    // Původní obsah (pro rozostření/disable)

    private NetworkClient networkClient;

    // init funkce
    @FXML
    public void initialize() {
        networkClient = Main.getNetworkClient();
        if (networkClient == null) {
            statusLabel.setText("Chyba: Nepřipojeno!");
            loginBtn.setDisable(true);
            return;
        }

        // nastavíme listener
        networkClient.setOnMessageReceived(this::processMessage);

        // jsme přihlášeni?
        String savedNick = Main.getLoggedInNick();

        if (savedNick != null) {
            // Hráč už je přihlášený z minula a socket stále běží!
            // vyplníme pole
            nickField.setText(savedNick);

            // zamkneme Login sekci (už není potřeba posílat LOGI)
            nickField.setDisable(true);
            loginBtn.setDisable(true);

            // odemkneme Lobby sekci
            lobbyBox.setDisable(false);

            // informujeme uživatele
            statusLabel.setText("Vítej zpět, " + savedNick);
            statusLabel.setStyle("-fx-text-fill: green;");
        }
        Platform.runLater(() -> rootPane.requestFocus());
    }

    // klávesnice ESC
    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            toggleEscMenu();
        }
    }

    // přepnou mezi escape menu
    private void toggleEscMenu() {
        boolean isVisible = escMenuBox.isVisible();
        escMenuBox.setVisible(!isVisible);
        contentBox.setDisable(!isVisible); // Deaktivujeme tlačítka vespod, aby nešlo klikat skrz

        if (!isVisible) {
            // Pokud jsme menu právě otevřeli
            statusLabel.setText("Menu otevřeno");
        }
    }

    @FXML
    protected void onResumeClick() {
        toggleEscMenu(); // Jen zavře menu
    }

    // funkce odpojení od serveru
    @FXML
    protected void onDisconnectClick() {
        // zavřít spojení
        if (networkClient != null) {
            networkClient.close();
        }

        // vyčistit globální stav v Main
        Main.setNetworkClient(null);
        Main.setLoggedInNick(null); // Zapomeneme nick, aby se příště musel přihlásit znovu

        // přepnout na Connect obrazovku
        Main.showConnectScreen();
    }

    // ukončí celej program
    @FXML
    protected void onExitClick() {
        // Ukončit celou aplikaci
        if (networkClient != null) {
            networkClient.close();
        }
        Platform.exit();
        System.exit(0);
    }

    // pokusí se zaregistrovat
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

    // pokusí se připojit do lobby
    @FXML
    protected void onJoinClick() {
        String lobbyId = lobbyField.getText();
        if (lobbyId.isEmpty()) {
            statusLabel.setText("Zadej ID lobby!");
            return;
        }
        // odeslání protokolu: JOIN <LobbyID>
        networkClient.send("JOIN " + lobbyId);
        statusLabel.setText("Připojuji do lobby " + lobbyId + "...");
    }

    // pokusí se opustit lobby
    @FXML
    protected void onLeaveClick() {
        networkClient.send("LEAV");
        statusLabel.setText("Opouštím lobby...");
        leaveBtn.setDisable(true); // aby na to neklikal víckrát
    }

    // Metoda pro zpracování odpovědí ze serveru
    private void processMessage(String msg) {
        //System.out.println("Message received: " + msg);

        Platform.runLater(() -> {
            try {
                String[] parts = msg.split(" ");

                // Ochrana proti prázdné zprávě
                if (parts.length == 0) {
                    throw new IllegalArgumentException("Prázdná zpráva");
                }

                String cmd = parts[0];
                switch (cmd) {
                    case "LOOK":
                        // Očekáváme: LOOK <nick>
                        if (parts.length < 2) throw new IllegalArgumentException("Chybí nick v LOOK");

                        String nick = parts[1];
                        Main.setLoggedInNick(nick);

                        statusLabel.setText("Přihlášen jako: " + nick);
                        statusLabel.setStyle("-fx-text-fill: green;");
                        nickField.setDisable(true);
                        loginBtn.setDisable(true);
                        lobbyBox.setDisable(false);
                        break;

                    case "JOOK":
                        // Očekáváme: JOOK
                        statusLabel.setText("Vstoupil jsi do lobby. Čekám na hru...");
                        statusLabel.setStyle("-fx-text-fill: orange;");

                        lobbyField.setDisable(true);
                        joinBtn.setDisable(true);
                        leaveBtn.setDisable(false);
                        break;

                    case "LEOK":
                        // Očekáváme: LEOK
                        statusLabel.setText("Lobby opuštěno. Vyber nové.");
                        statusLabel.setStyle("-fx-text-fill: black;");

                        lobbyField.setDisable(false);
                        joinBtn.setDisable(false);
                        leaveBtn.setDisable(true);
                        break;

                    case "GAST":
                        // Očekáváme: GAST <side_id>
                        if (parts.length < 2) throw new IllegalArgumentException("Chybí side_id v GAST");

                        // Bezpečně určíme stranu (0 = Left, 1 = Right)
                        boolean amILeft = parts[1].equals("0");
                        Main.switchToGame(amILeft);
                        break;

                    case "ERRO":
                        // Očekáváme: ERRO <msg>
                        String errorMsg = msg.length() > 5 ? msg.substring(5) : "Neznámá chyba";
                        statusLabel.setText("Chyba: " + errorMsg);
                        statusLabel.setStyle("-fx-text-fill: red;");
                        break;

                    case "KICK":
                        break;

                    // ochrana proti neznamim příkazům
                    default:
                        throw new IllegalArgumentException("Neznámý příkaz v menu: " + cmd);
                }

            } catch (Exception e) {
                // cokoliv se stane špatně kvůli nevalidní zprávě jde sem
                String errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();
                Main.handleProtocolError(errorDetail + " [Msg: " + msg + "]");
            }
        });
    }
}