module com.honzahavelka.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.honzahavelka.client to javafx.fxml;
    opens com.honzahavelka.client.controller to javafx.fxml;

    exports com.honzahavelka.client;
}