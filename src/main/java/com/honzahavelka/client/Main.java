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

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // 1. Inicializace sítě (Připojíme se hned při startu)
        connectToServer();

        // 2. Načtení Login Menu (FXML)
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("menu.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 300); // Menší okno pro login

        stage.setTitle("Pong Client - Login");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Vytvoří spojení se serverem.
     * Zatím jen vypisuje zprávy do konzole, dokud si je nepřevezme Controller.
     */
    private void connectToServer() {
        networkClient = new NetworkClient("127.0.0.1", 10000, (msg) -> {
            // Defaultní listener - jen pro debug, než si to převezme Menu nebo Hra
            System.out.println("RAW SERVER MSG: " + msg);
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

    public static void switchToMenu() {
        Platform.runLater(() -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("menu.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 400, 300);
                primaryStage.setTitle("Pong Client - Login");
                primaryStage.setScene(scene);
                primaryStage.centerOnScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String loggedInNick = null;

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