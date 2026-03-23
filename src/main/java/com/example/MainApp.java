package com.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("MainApp.start: begin");
            Parent root = FXMLLoader.load(getClass().getResource("/scenes/MainScene.fxml"));
            System.out.println("MainApp.start: FXML loaded");
            Scene scene = new Scene(root, 650, 600);
            primaryStage.setTitle("Flight Reservation System");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("MainApp.start: shown");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
