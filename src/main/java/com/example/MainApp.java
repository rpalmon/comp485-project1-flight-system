package com.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainApp extends Application {
    private static final Path DOT_ENV_PATH = Path.of(".env");
    private static final Path ENV_PATH = Path.of("env");

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("MainApp.start: begin");
            if (!ensureEnvFilePresent(primaryStage)) {
                return;
            }
            Parent root = FXMLLoader.load(getClass().getResource("/scenes/MainScene.fxml"));
            System.out.println("MainApp.start: FXML loaded");
            Scene scene = new Scene(root, 650, 600);
            primaryStage.setTitle("Flight Reservation System");
            primaryStage.setScene(scene);
            primaryStage.show();
            System.out.println("MainApp.start: shown");
        } catch (IOException exception) {
            showStartupError(primaryStage, "Unable to load the application.", exception);
            Platform.exit();
        } catch (RuntimeException exception) {
            showStartupError(primaryStage, "Unexpected startup error.", exception);
            Platform.exit();
        }
    }

    private boolean ensureEnvFilePresent(Stage ownerStage) throws IOException {
        if (Files.exists(DOT_ENV_PATH) || Files.exists(ENV_PATH)) {
            return true;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        initOwnerIfReady(alert, ownerStage);
        alert.setTitle("Configuration file missing");
        alert.setHeaderText("No env file was found in the project root.");
        alert.setContentText("Choose a file to upload. The app will save it as env in the root folder.");

        ButtonType uploadButton = new ButtonType("Upload", ButtonBar.ButtonData.OK_DONE);
        ButtonType exitButton = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(uploadButton, exitButton);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() != uploadButton) {
            Platform.exit();
            return false;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select env file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files", "*.env", "env", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            Platform.exit();
            return false;
        }

        Files.copy(selectedFile.toPath(), ENV_PATH, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private void showStartupError(Stage ownerStage, String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        initOwnerIfReady(alert, ownerStage);
        alert.setTitle("Startup error");
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage() == null ? exception.toString() : exception.getMessage());
        alert.showAndWait();
    }

    private void initOwnerIfReady(Alert alert, Stage ownerStage) {
        if (ownerStage != null && ownerStage.getScene() != null) {
            alert.initOwner(ownerStage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
