package com.honzahavelka.client.controller;

import com.honzahavelka.client.Main;
import com.honzahavelka.client.model.GameState;
import com.honzahavelka.client.net.NetworkClient;
import com.honzahavelka.client.view.GameCanvas;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import java.util.Objects;

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

    private VBox reconnectBox;
    private Label reconnectMsgLabel;
    private Label reconnectTimerLabel;
    private Timeline reconnectTimeline; // Objekt pro odpočet času
    private int reconnectSecondsLeft = 30; // Výchozí čas

    public GameController() {
        this.gameState = new GameState();
        this.gameCanvas = new GameCanvas(gameState);

        // --- VYTVOŘENÍ GAME OVER OVERLAY ---
        createGameOverOverlay();
        createPauseOverlay();
        createReconnectOverlay();

        // Root obsahuje Canvas (vespod) a Overlay (navrchu)
        this.root = new StackPane(this.gameCanvas, this.gameOverBox, this.pauseBox, this.reconnectBox);
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

    private void createReconnectOverlay() {
        reconnectMsgLabel = new Label("Čekám na soupeře...");
        reconnectMsgLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        reconnectMsgLabel.setTextFill(Color.LIGHTBLUE);

        reconnectTimerLabel = new Label("30");
        reconnectTimerLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 60));
        reconnectTimerLabel.setTextFill(Color.WHITE);

        Label subLabel = new Label("Pokud se nepřipojí, hra skončí.");
        subLabel.setTextFill(Color.GRAY);

        Button leaveBtn = new Button("Opustit Lobby");
        leaveBtn.setStyle("-fx-font-size: 16px; -fx-base: #f44336;");
        leaveBtn.setOnAction(e -> handleLeave());

        reconnectBox = new VBox(20, reconnectMsgLabel, reconnectTimerLabel, subLabel, leaveBtn);
        reconnectBox.setAlignment(Pos.CENTER);
        reconnectBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // Tmavší pozadí

        // Roztáhneme na celé okno
        reconnectBox.setPrefSize(800, 600);
        reconnectBox.setVisible(false);
    }

    private void startReconnectCountdown() {
        pausedByMe = false;
        pausedByOpponent = false;
        // 1. Resetujeme čas a UI
        reconnectSecondsLeft = 30;
        reconnectTimerLabel.setText(String.valueOf(reconnectSecondsLeft));
        reconnectBox.setVisible(true);

        // 2. Pokud už timeline běží z minula, zastavíme ji
        if (reconnectTimeline != null) {
            reconnectTimeline.stop();
        }

        // 3. Vytvoříme novou časovou osu
        reconnectTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            reconnectSecondsLeft--;
            reconnectTimerLabel.setText(String.valueOf(reconnectSecondsLeft));

            if (reconnectSecondsLeft <= 0) {
                // Čas vypršel!
                reconnectTimeline.stop();
            }
        }));

        // 4. Nastavíme počet opakování a spustíme
        reconnectTimeline.setCycleCount(30);
        reconnectTimeline.play();
    }

    private void stopReconnectCountdown() {
        if (reconnectTimeline != null) {
            reconnectTimeline.stop();
        }
        reconnectBox.setVisible(false);
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
        this.networkClient.send("REDY");
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
        stopReconnectCountdown();
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
        stopReconnectCountdown();
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

            if (pauserSlot == 0) {
                gameState.p1_paused = true;
            }
            else if (pauserSlot == 1) {
                gameState.p2_paused = true;
            }
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
        stopReconnectCountdown();
        // Msg: RESU <slot_id>
        try {
            String[] parts = msg.split(" ");
            int resumerSlot = Integer.parseInt(parts[1]);
            int mySlot = gameState.amILeft ? 0 : 1;

            if (resumerSlot == 0) {
                gameState.p1_paused = false;
            }
            else if (resumerSlot == 1) {
                gameState.p2_paused = false;
            }

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

    private void handleReconnect(String msg) {
        try {
            // Msg: RECO <slot_id>
            String[] parts = msg.split(" ");
            int disconnectedSlot = Integer.parseInt(parts[1]);
            int mySlot = gameState.amILeft ? 0 : 1;

            gameState.reconnecting = true;

            // Overlay zobrazíme jen tehdy, pokud se odpojil SOUPEŘ.
            // (Pokud server pošle RECO i pro mě, tak to ignoruji, protože já jsem tady)
            if (disconnectedSlot != mySlot) {
                startReconnectCountdown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleContinue() {
        System.out.println("Soupeř se vrátil, hra pokračuje (CONT).");
        gameState.reconnecting = false;
        // 1. Zastavíme odpočet a schováme overlay
        stopReconnectCountdown();

        // 2. Pro jistotu schováme i pauzu, pokud by tam nějaká visela
        if (pauseBox.isVisible()) {
            pauseBox.setVisible(false);
            pausedByMe = false;
            pausedByOpponent = false;
        }

        // 3. DŮLEŽITÉ: Vrátíme focus hře, aby fungovaly šipky/WS
        root.requestFocus();
        networkClient.send("REDY");
    }

    private void processMessage(String msg) {
        // STAT zprávy nezpracováváme přes Platform.runLater kvůli výkonu (jdou rovnou do modelu)
        // Ale GAOV (změna UI) musíme obalit do runLater!

        // System.out.println("Msg: " + msg); // Pro debug dobré, pro produkci u STAT zpráv raději vypnout (zahltí konzoli)

        try {
            if (msg.startsWith("MOVE")) {
                Platform.runLater(() -> handleMove(msg));
            }
            else if (msg.startsWith("STAT")) {
                Platform.runLater(() -> handleStat(msg));
            }
            else if (msg.startsWith("BALL")) {
                Platform.runLater(() -> handleBall(msg));
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
            else if (msg.startsWith("KICK")) {
                Platform.runLater(this::handleKick);
            }
            else if (msg.startsWith("RECO")) {
                Platform.runLater(() -> handleReconnect(msg));
            }
            else if (msg.startsWith("CONT")) {
                Platform.runLater(this::handleContinue);
            }
            else if (msg.startsWith("GOAL") || msg.startsWith("PING")) {
                // Odpověď serveru, že jsme připraveni (po gólu) nebo že žijeme (ping)
                Platform.runLater(() -> handleGoal(msg));
            }
            else if (msg.startsWith("LEOK")) {
                //opuštění lobby
            }
            else if (msg.startsWith("REUP")) {
                //informace o rematchy
            }
            else if (msg.startsWith("ERRO")) {
                System.out.println(msg);
                //ingno chybu prozatim
            }
            // --- OCHRANA PROTI NEZNÁMÝM ZPRÁVÁM ---
            else {
                // Pokud přijde něco, co neznáme (např. "BLABLA 123")
                throw new IllegalArgumentException("Neznámý příkaz: " + msg.split(" ")[0]);
            }

        } catch (Exception e) {
            // Zde chytáme vše:
            // 1. NumberFormatException (z parsování STAT)
            // 2. IllegalArgumentException (z validace délky nebo neznámého příkazu)
            // 3. Jiné RuntimeExceptions

            String errorDetail = e.getClass().getSimpleName() + ": " + e.getMessage();

            // Zalogujeme a pošleme do Mainu k započítání (3 chyby -> disconnect)
            // Používáme runLater, protože Main může chtít přepnout scénu na ConnectScreen
            Platform.runLater(() -> Main.handleProtocolError(errorDetail));
        }
    }

    private void handleStat(String msg) {
        String[] parts = msg.split(" ");

        gameState.ball.setPosition(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        gameState.player1.setY(Double.parseDouble(parts[3]));
        gameState.player2.setY(Double.parseDouble(parts[4]));
        gameState.score1 = Integer.parseInt(parts[5]);
        gameState.score2 = Integer.parseInt(parts[6]);
    }

    private void handleGoal(String msg) {
        this.networkClient.send("REDY");
        if (msg.startsWith("PING")) return;

        String[] parts = msg.split(" ");
        gameState.score1 = Integer.parseInt(parts[1]);
        gameState.score2 = Integer.parseInt(parts[2]);

        gameState.player1.setY(250);
        gameState.player2.setY(250);
        gameState.ball.setDx(500);
        gameState.ball.setDy(0);
        gameState.ball.setPosition(395, 295);
    }

    private void handleBall(String msg) {
        String[] parts = msg.split(" ");
        gameState.ball.setPosition(Double.parseDouble(parts[1]),  Double.parseDouble(parts[2]));
        gameState.ball.setDx(Double.parseDouble(parts[3]));
        gameState.ball.setDy(Double.parseDouble(parts[4]));
    }

    private void handleMove(String msg) {
        String[] parts = msg.split(" ");

        if (parts[1].equals("0")) {
            if (Objects.equals(parts[2], "UP")) gameState.player1.moveUp();
            else if (Objects.equals(parts[2], "DOWN")) gameState.player1.moveDown();
            else if (Objects.equals(parts[2], "NONE")) gameState.player1.stop();

            gameState.player1.setY(Double.parseDouble(parts[3]));
        }
        else if (parts[1].equals("1")) {
            if (Objects.equals(parts[2], "UP")) gameState.player2.moveUp();
            else if (Objects.equals(parts[2], "DOWN")) gameState.player2.moveDown();
            else if (Objects.equals(parts[2], "NONE")) gameState.player2.stop();

            gameState.player2.setY(Double.parseDouble(parts[3]));
        }
    }

    private void showGameOver(String msg) {
        stopReconnectCountdown();
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
        gameState.resetPhysics();
        stopReconnectCountdown();
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
        networkClient.send("REDY");
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