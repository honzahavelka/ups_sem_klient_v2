package com.honzahavelka.client.controller;

import com.honzahavelka.client.Main;
import com.honzahavelka.client.model.GameState;
import com.honzahavelka.client.net.NetworkClient;
import com.honzahavelka.client.view.GameCanvas;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GameController {

    private NetworkClient networkClient;
    private GameState gameState;
    private GameCanvas gameCanvas;
    private StackPane root;

    // --- NOVÉ UI PRVKY PRO GAME OVER ---
    private VBox gameOverBox;
    private Label resultLabel;
    private Label scoreLabel;
    private Button rematchBtn;
    private Button leaveBtn;

    private boolean wasKicked = false; // Nový příznak

    private VBox pauseBox;
    private Label pauseLabel;
    private Button pauseLeaveBtn;

    private boolean pausedByMe = false;
    private boolean pausedByOpponent = false;

    public GameController() {
        this.gameState = new GameState();
        this.gameCanvas = new GameCanvas(gameState);

        // --- VYTVOŘENÍ GAME OVER OVERLAY ---
        createGameOverOverlay();
        createPauseOverlay();

        // Root obsahuje Canvas (vespod) a Overlay (navrchu)
        this.root = new StackPane(this.gameCanvas, this.gameOverBox, this.pauseBox);
        this.root.setPrefSize(800, 600);

        this.gameCanvas.start();
    }

    private void createGameOverOverlay() {
        // 1. Label pro výsledek (Výhra/Prohra)
        resultLabel = new Label("");
        resultLabel.setFont(Font.font("Arial", FontWeight.BOLD, 50));

        // 2. Label pro finální skóre
        scoreLabel = new Label("");
        scoreLabel.setFont(Font.font("Monospaced", 30));
        scoreLabel.setTextFill(Color.WHITE);

        // 3. Tlačítka
        rematchBtn = new Button("Odveta (REMATCH)");
        rematchBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 20; -fx-base: #4CAF50;");
        rematchBtn.setOnAction(e -> handleRematch());

        leaveBtn = new Button("Opustit Lobby");
        leaveBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 20; -fx-base: #f44336;");
        leaveBtn.setOnAction(e -> handleLeave());

        HBox buttons = new HBox(20, rematchBtn, leaveBtn);
        buttons.setAlignment(Pos.CENTER);

        // 4. Kontejner (VBox)
        gameOverBox = new VBox(20, resultLabel, scoreLabel, buttons);
        gameOverBox.setAlignment(Pos.CENTER);

        // Poloprůhledné černé pozadí, aby vynikl text
        gameOverBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8);");

        // Ve výchozím stavu skryté
        gameOverBox.setVisible(false);
    }

    private void createPauseOverlay() {
        pauseLabel = new Label("PAUZA");
        pauseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 40));
        pauseLabel.setTextFill(Color.YELLOW);


        pauseLeaveBtn = new Button("Opustit Lobby");
        pauseLeaveBtn.setStyle("-fx-font-size: 16px; -fx-padding: 8 16; -fx-base: #f44336;");
        pauseLeaveBtn.setOnAction(e -> handleLeave()); // Znovu použijeme stejnou logiku odchodu

        pauseBox = new VBox(20, pauseLabel, pauseLeaveBtn);
        pauseBox.setAlignment(Pos.CENTER);
        pauseBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);"); // Poloprůhledná

        pauseBox.setVisible(false); // Defaultně skryté
    }

    public void setInitialSide(boolean amILeft) {
        this.gameState.amILeft = amILeft;
    }

    public void initNetwork(NetworkClient client) {
        this.networkClient = client;
        this.networkClient.setOnMessageReceived(this::processMessage);
    }

    public Parent getView() {
        return root;
    }

    // --- LOGIKA TLAČÍTEK ---

    private void handleRematch() {
        // Odešle žádost o odvetu
        if (networkClient != null) {
            networkClient.send("REMA");
            rematchBtn.setDisable(true); // Aby nešlo spamovat
            rematchBtn.setText("Čekám na souhlas...");
        }
    }

    private void handleLeave() {
        if (networkClient != null) {
            // Pokud jsme NEBYLI vyhozeni, pošleme serveru slušné sbohem.
            // Pokud JSME byli vyhozeni (wasKicked == true), server už o nás ví, neposíláme nic.
            if (!wasKicked) {
                networkClient.send("LEAV");
            }

            // Zbytek je stejný - úklid a návrat
            gameCanvas.stop();
            Main.switchToMenu();
        }
    }

    private void handleKick() {
        wasKicked = true; // Nastavíme příznak

        // 2. Deaktivujeme odvetu (nemáme s kým hrát)
        rematchBtn.setDisable(true);
        rematchBtn.setText("Nelze hrát znovu");

        // 3. Upravíme tlačítko pro odchod
        leaveBtn.setText("Zpět do Menu");
        // (Funkcionalita leaveBtn se změní díky proměnné wasKicked v handleLeave)

        // 4. Zobrazíme overlay (pokud už není zobrazený)
        gameOverBox.setVisible(true);

        // 5. Zastavíme hru na pozadí
        gameState.gameOver = true;
    }

    private void handlePause(String msg) {
        // Msg: PAUS <slot_id>
        try {
            String[] parts = msg.split(" ");
            int pauserSlot = Integer.parseInt(parts[1]);
            int mySlot = gameState.amILeft ? 0 : 1;

            if (pauserSlot == mySlot) {
                pausedByMe = true;
            } else {
                pausedByOpponent = true;
            }

            updatePauseState();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResume(String msg) {
        // Msg: RESU <slot_id>
        try {
            String[] parts = msg.split(" ");
            int resumerSlot = Integer.parseInt(parts[1]);
            int mySlot = gameState.amILeft ? 0 : 1;

            if (resumerSlot == mySlot) {
                pausedByMe = false;
            } else {
                pausedByOpponent = false;
            }

            updatePauseState();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessage(String msg) {
        // STAT zprávy nezpracováváme přes Platform.runLater kvůli výkonu (jdou rovnou do modelu)
        // Ale GAOV (změna UI) musíme obalit do runLater!
        System.out.println("Message received: " + msg);
        try {
            if (msg.startsWith("STAT")) {
                String[] parts = msg.split(" ");
                if (parts.length >= 7) {
                    gameState.ballX = Double.parseDouble(parts[1]);
                    gameState.ballY = Double.parseDouble(parts[2]);
                    gameState.paddle1Y = Double.parseDouble(parts[3]);
                    gameState.paddle2Y = Double.parseDouble(parts[4]);
                    gameState.score1 = Integer.parseInt(parts[5]);
                    gameState.score2 = Integer.parseInt(parts[6]);
                }
            }
            else if (msg.startsWith("PAUS")) {
                Platform.runLater(() -> handlePause(msg));
            }

            else if (msg.startsWith("RESU")) {
                Platform.runLater(() -> handleResume(msg));
            }

            else if (msg.startsWith("GAOV")) {
                // Protokol: GAOV <winner_id> <score1> <score2>
                System.out.println(msg);
                Platform.runLater(() -> showGameOver(msg));
            }
            else if (msg.startsWith("GAST")) {
                // Pokud přijde GAST, znamená to, že odveta byla přijata a hra začíná znovu
                Platform.runLater(this::restartGame);
            }
            // ... uvnitř processMessage ...
            else if (msg.startsWith("KICK")) {
                Platform.runLater(this::handleKick);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showGameOver(String msg) {
        pauseBox.setVisible(false);

        String[] parts = msg.split(" ");
        int winnerId = Integer.parseInt(parts[1]);
        int score1 = Integer.parseInt(parts[2]);
        gameState.score1 = score1;
        int score2 = Integer.parseInt(parts[3]);
        gameState.score2 = score2;

        // Určení vítěze
        // Pokud jsem LEFT (hráč 1) a winner je 1 -> Vyhrál jsem
        // Pokud jsem RIGHT (hráč 2) a winner je 2 -> Vyhrál jsem
        boolean iWon = (gameState.amILeft && winnerId == 0) || (!gameState.amILeft && winnerId == 1);

        if (iWon) {
            resultLabel.setText("VÍTĚZSTVÍ!");
            resultLabel.setTextFill(Color.LIMEGREEN);
        } else {
            resultLabel.setText("PROHRA...");
            resultLabel.setTextFill(Color.RED);
        }

        scoreLabel.setText("Skóre: " + score1 + " : " + score2);

        // Zobrazíme overlay
        gameOverBox.setVisible(true);
        gameState.gameOver = true; // Zastaví logiku v Canvasu, pokud chceš
    }

    private void updatePauseState() {
        // 1. Pokud ani jeden nepauzuje, skryjeme overlay a povolíme hru
        if (!pausedByMe && !pausedByOpponent) {
            pauseBox.setVisible(false);
            return;
        }

        // 2. Jinak zobrazíme overlay
        pauseBox.setVisible(true);

        // 3. Rozhodneme o textu
        if (pausedByMe && pausedByOpponent) {
            // Oba mají pauzu
            pauseLabel.setText("PAUZA (Oba)");
            pauseLabel.setTextFill(Color.MAGENTA); // Nějaká výrazná barva
        } else if (pausedByMe) {
            // Jen já
            pauseLabel.setText("PAUZA (Ty)");
            pauseLabel.setTextFill(Color.YELLOW);
        } else {
            // Jen soupeř
            pauseLabel.setText("PAUZA (Soupeř)");
            pauseLabel.setTextFill(Color.ORANGE);
        }
    }

    private void restartGame() {
        // Reset UI pro novou hru
        gameOverBox.setVisible(false);
        pauseBox.setVisible(false);

        // RESET obou stavů
        pausedByMe = false;
        pausedByOpponent = false;

        rematchBtn.setDisable(false);
        rematchBtn.setText("Odveta (REMATCH)");
        gameState.gameOver = false;
        System.out.println("Nová hra začíná!");
    }

    // --- OVLÁDÁNÍ ---
    public void onKeyPressed(KeyEvent event) {
        if (networkClient == null || gameOverBox.isVisible()) return; // Zákaz pohybu při Game Over

        if (event.getCode() == KeyCode.ESCAPE) {
            networkClient.send("PAUS");
            return;
        }
        if (pausedByMe || pausedByOpponent) return;

        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) networkClient.send("INPT UP");
        else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) networkClient.send("INPT DOWN");
    }

    public void onKeyReleased(KeyEvent event) {
        if (networkClient == null) return;
        if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W ||
                event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) {
            networkClient.send("INPT STOP");
        }
    }
}