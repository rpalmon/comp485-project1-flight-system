package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FlightBookingApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/scenes/MainScene.fxml")
        );

        Scene scene = new Scene(loader.load());
        stage.setTitle("Flight Booking System");
        stage.setScene(scene);
        stage.show();
    }
}
