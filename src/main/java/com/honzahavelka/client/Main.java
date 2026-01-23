package com.honzahavelka.client;

import com.honzahavelka.client.controller.ConnectController;
import com.honzahavelka.client.net.NetworkClient;
import com.honzahavelka.client.controller.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// hlavní třída, přepíná scény
public class Main extends Application {

    // statické reference pro přístup z kontrolerů
    private static NetworkClient networkClient;
    private static Stage primaryStage;
    private static String loggedInNick = null;

    // pokud server řiká blbosti, počítadlo na chybné zprávy
    private static int protocolErrorCount = 0;
    private static final int MAX_PROTOCOL_ERRORS = 3;

    // metoda pokud controller zachytí nevalidní zprávu
    public static void handleProtocolError(String details) {
        protocolErrorCount++;

        System.err.println("VAROVÁNÍ: Chyba protokolu (" + protocolErrorCount + "/" + MAX_PROTOCOL_ERRORS + "): " + details);

        // pokud jsme ještě nepřekročili limit, jen to ignorujeme (return)
        if (protocolErrorCount < MAX_PROTOCOL_ERRORS) {
            return;
        }

        // pokud jsme limit překročili, provedeme tvrdé odpojení
        performForceDisconnect("Překročen limit chyb protokolu (" + protocolErrorCount + "). Poslední: " + details);
    }

    // odpojení od serveru
    private static void performForceDisconnect(String reason) {
        System.err.println("CRITICAL: Odpojuji klienta...");

        if (networkClient != null) {
            try {
                networkClient.close();
            } catch (Exception e) {
                // ignorujem
            }
            networkClient = null;
        }
        setLoggedInNick(null);

        // zobrazíme ConnectScreen s důvodem
        showConnectScreen("Odpojeno od serveru:\n" + reason);
    }

    // handle pád serveru
    public static void handleConnectionLost() {
        // vyčistíme staré spojení
        if (networkClient != null) {
            networkClient.close();
            networkClient = null;
        }
        // odregistrujem
        setLoggedInNick(null);

        // zobrazíme ConnectScreen s textem v labelu
        showConnectScreen("Spojení se serverem bylo ztraceno.");
    }

    // metoda pro zobrazení prvotní obrazovky
    public static void showConnectScreen(String errorMessage) {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("connect.fxml"));

                // načtem view - vytvoří se kontroler
                Parent root = fxmlLoader.load();

                // získáme instanci Controlleru, kterou FXMLLoader vytvoril
                ConnectController controller = fxmlLoader.getController();

                // pokud máme chybovou hlášku, předáme ji Controlleru
                if (errorMessage != null) {
                    controller.setErrorMessage(errorMessage);
                }

                Scene scene = new Scene(root, 350, 300);
                primaryStage.setTitle("Pong Client - Connect");
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
                primaryStage.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // start fce
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        showConnectScreen();
    }

    // metoda pro connectscreen ale bez chybové hlášky
    public static void showConnectScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("connect.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 350, 300);
            primaryStage.setTitle("Pong Client - Connect");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // načtení ban obrazovky
    public static void showBanScreen(String rawMessage) {
        Platform.runLater(() -> {
            try {
                // získání důvodu (FBAN <důvod>)
                String reason = "Neznámý důvod";
                if (rawMessage.length() > 5) {
                    reason = rawMessage.substring(5);
                }

                // vytvoření UI celkem na hulváta
                javafx.scene.control.Label titleLabel = new javafx.scene.control.Label("BANNED");
                titleLabel.setFont(javafx.scene.text.Font.font("Impact", 80)); // Velké písmo
                titleLabel.setTextFill(javafx.scene.paint.Color.RED);

                javafx.scene.control.Label reasonLabel = new javafx.scene.control.Label("Důvod: " + reason);
                reasonLabel.setFont(javafx.scene.text.Font.font("Arial", 24));
                reasonLabel.setTextFill(javafx.scene.paint.Color.WHITE);
                reasonLabel.setWrapText(true);

                javafx.scene.control.Button exitBtn = new javafx.scene.control.Button("Ukončit aplikaci");
                exitBtn.setStyle("-fx-base: black; -fx-text-fill: white; -fx-border-color: red;");
                exitBtn.setOnAction(e -> System.exit(0));

                javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(30, titleLabel, reasonLabel, exitBtn);
                root.setAlignment(javafx.geometry.Pos.CENTER);
                root.setStyle("-fx-background-color: black;");

                // setup scény
                Scene banScene = new Scene(root, 800, 600);
                primaryStage.setScene(banScene);
                primaryStage.setTitle("BANNED FROM SERVER");

                // odpojení od serveru
                if (networkClient != null) {
                    networkClient.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // metoda pro přepnutí z menu do hry
    public static void switchToGame(boolean amILeft) {
        Platform.runLater(() -> {
            try {
                GameController gameController = new GameController();

                // předáme informaci o straně do GameControlleru
                gameController.setInitialSide(amILeft);

                gameController.initNetwork(networkClient);

                Scene gameScene = new Scene(gameController.getView());
                gameScene.setOnKeyPressed(gameController::onKeyPressed);
                gameScene.setOnKeyReleased(gameController::onKeyReleased);

                primaryStage.setTitle("Pong Client - Game");
                primaryStage.setScene(gameScene);
                primaryStage.centerOnScreen();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // přechod z connectu do menu
    public static void switchToMenu() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("menu.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 400, 300);
                primaryStage.setTitle("Pong Client - Main Menu");
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // nastavení networkclienta - dělá connectcontroller pří správným připojení
    public static void setNetworkClient(NetworkClient client) {
        networkClient = client;
        protocolErrorCount = 0;
    }

    // získání nicku
    public static String getLoggedInNick() {
        return loggedInNick;
    }

    // nastavení nicku - delá menu controller
    public static void setLoggedInNick(String nick) {
        loggedInNick = nick;
    }

    // getter pro controllery
    public static NetworkClient getNetworkClient() {
        return networkClient;
    }

    // ukončení app
    @Override
    public void stop() throws Exception {
        // Zavřeme socket při ukončení aplikace křížkem
        System.out.println("Ukončuji aplikaci...");
        if (networkClient != null) {
            networkClient.close();
        }
        super.stop();
    }

    // hlavní fce
    public static void main(String[] args) {
        launch();
    }
}