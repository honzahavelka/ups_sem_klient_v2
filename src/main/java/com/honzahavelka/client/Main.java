package com.honzahavelka.client;

import com.honzahavelka.client.net.NetworkClient;
import com.honzahavelka.client.controller.GameController; // Vytvoříme později
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    // Statické reference, abychom k nim mohli přistupovat z Controllerů
    private static NetworkClient networkClient;
    private static Stage primaryStage;
    private static String loggedInNick = null;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // 1. Inicializace sítě (Připojíme se hned při startu)
        showConnectScreen();
    }

    public static void showConnectScreen() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("connect.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 350, 300); // Menší okno
            primaryStage.setTitle("Pong Client - Connect");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showBanScreen(String rawMessage) {
        Platform.runLater(() -> {
            try {
                // 1. Získání důvodu (FBAN <důvod>)
                String reason = "Neznámý důvod";
                if (rawMessage.length() > 5) {
                    reason = rawMessage.substring(5);
                }

                // 2. Vytvoření UI (Čistý Java kód, FXML není potřeba pro tak jednoduchou věc)
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
                root.setStyle("-fx-background-color: black;"); // Černé pozadí

                // 3. Nastavení scény
                Scene banScene = new Scene(root, 800, 600);
                primaryStage.setScene(banScene);
                primaryStage.setTitle("BANNED FROM SERVER");

                // 4. Odpojení sítě (už nemá smysl komunikovat)
                if (networkClient != null) {
                    networkClient.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Metoda pro přepnutí z Menu do Hry.
     * Zavolá ji MenuController, když přijde "JOOK".
     */
    public static void switchToGame(boolean amILeft) { // <--- Přidán parametr
        Platform.runLater(() -> {
            try {
                GameController gameController = new GameController();

                // Předáme informaci o straně do GameControlleru
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

    // Tuto metodu teď volá ConnectController po úspěšném HELO
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

    public static void setNetworkClient(NetworkClient client) {
        networkClient = client;
    }


    public static String getLoggedInNick() {
        return loggedInNick;
    }

    public static void setLoggedInNick(String nick) {
        loggedInNick = nick;
    }

    // Getter pro Controllery, aby mohly posílat data (client.send(...))
    public static NetworkClient getNetworkClient() {
        return networkClient;
    }

    @Override
    public void stop() throws Exception {
        // Zavřeme socket při ukončení aplikace křížkem
        System.out.println("Ukončuji aplikaci...");
        if (networkClient != null) {
            networkClient.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}