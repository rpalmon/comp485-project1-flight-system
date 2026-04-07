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
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
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

        Dialog<ButtonType> dialog = new Dialog<>();
        initOwnerIfReady(dialog, ownerStage);
        dialog.setTitle("Configuration file missing");
        dialog.setHeaderText("No env file was found in the project root.");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Paste env file contents here, or leave blank to upload an existing file.");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(12);
        dialog.getDialogPane().setContent(textArea);

        ButtonType uploadButton = new ButtonType("Upload file", ButtonBar.ButtonData.LEFT);
        ButtonType createButton = new ButtonType("Create .env", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(uploadButton, createButton, cancelButton);

        var createButtonNode = dialog.getDialogPane().lookupButton(createButton);
        createButtonNode.setDisable(true);
        textArea.textProperty().addListener((observable, oldValue, newValue) -> createButtonNode.setDisable(newValue == null || newValue.trim().isEmpty()));

        dialog.setResultConverter(button -> button);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == cancelButton) {
            Platform.exit();
            return false;
        }

        if (result.get() == uploadButton) {
            return showEnvFileUploadDialog(ownerStage);
        }

        String envText = textArea.getText();
        if (envText == null || envText.trim().isEmpty()) {
            Platform.exit();
            return false;
        }

        Files.writeString(DOT_ENV_PATH, envText);
        return true;
    }

    private boolean showEnvFileUploadDialog(Stage ownerStage) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select env file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config files", "*.env", "env", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(ownerStage);
        if (selectedFile == null) {
            Platform.exit();
            return false;
        }

        Files.copy(selectedFile.toPath(), DOT_ENV_PATH, StandardCopyOption.REPLACE_EXISTING);
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

    private void initOwnerIfReady(Dialog<?> dialog, Stage ownerStage) {
        if (ownerStage != null && ownerStage.getScene() != null) {
            dialog.initOwner(ownerStage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
