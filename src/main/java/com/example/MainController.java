package com.example;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {
    @FXML
    private ComboBox<AirportOption> fromCombo;

    @FXML
    private ComboBox<AirportOption> toCombo;

    @FXML
    private DatePicker departDate;

    @FXML
    private DatePicker returnDate;

    @FXML
    private Spinner<Integer> passengersSpinner;

    @FXML
    private ComboBox<String> seatCombo;

    @FXML
    private ComboBox<String> classCombo;

    @FXML
    private CheckBox mealVegetarian;

    @FXML
    private CheckBox mealVegan;

    @FXML
    private CheckBox mealPescetarian;

    @FXML
    private CheckBox mealKosher;

    @FXML
    private CheckBox mealGlutenFree;

    @FXML
    private CheckBox mealStandard;

    @FXML
    private CheckBox bevWater;

    @FXML
    private CheckBox bevSoda;

    @FXML
    private CheckBox bevJuice;

    @FXML
    private CheckBox bevCoffee;

    @FXML
    private CheckBox bevTea;

    @FXML
    private Button searchButton;

    @FXML
    private Button resetSearchButton;

    @FXML
    private Button bookButton;

    @FXML
    private Label statusLabel;

    @FXML
    private TableView<FlightSearchResult> resultsTable;

    @FXML
    private TableColumn<FlightSearchResult, String> colAirline;

    @FXML
    private TableColumn<FlightSearchResult, String> colFlightNo;

    @FXML
    private TableColumn<FlightSearchResult, String> colFrom;

    @FXML
    private TableColumn<FlightSearchResult, String> colTo;

    @FXML
    private TableColumn<FlightSearchResult, String> colDepart;

    @FXML
    private TableColumn<FlightSearchResult, String> colArrive;

    @FXML
    private TableColumn<FlightSearchResult, String> colPrice;

    @FXML
    private TableColumn<FlightSearchResult, String> colSeats;

    private Stage adminStage;
    private MainDataService dataService;
    private MainDataService.ReferenceData referenceData;

    @FXML
    private void initialize() {
        configureStaticControls();
        configureResultsTable();

        try {
            dataService = new MainDataService(EnvConfig.loadDefault());
            loadReferenceData();
        } catch (IllegalArgumentException exception) {
            setStatus("Main search is not configured: " + exception.getMessage(), true);
        }
    }

    @FXML
    private void handleSearchFlights() {
        if (dataService == null || referenceData == null) {
            setStatus("Reference data has not finished loading yet.", true);
            return;
        }

        AirportOption origin = fromCombo.getValue();
        AirportOption destination = toCombo.getValue();
        LocalDate departure = departDate.getValue();
        LocalDate returning = returnDate.getValue();

        if (origin != null && destination != null && origin.getId().equals(destination.getId())) {
            setStatus("Origin and destination must be different airports.", true);
            return;
        }

        if (departure != null && returning != null && returning.isBefore(departure)) {
            setStatus("Return date cannot be earlier than the departure date.", true);
            return;
        }

        String cabinClass = classCombo.getValue();
        if (cabinClass == null || cabinClass.isBlank()) {
            setStatus("Select a travel class before searching.", true);
            return;
        }

        MainDataService.SearchCriteria criteria = new MainDataService.SearchCriteria(
                origin == null ? null : origin.getId(),
                destination == null ? null : destination.getId(),
                departure,
                returning,
                cabinClass,
                seatCombo.getValue(),
                passengersSpinner.getValue()
        );

        runBackgroundTask(
                "Searching flights...",
                () -> dataService.searchFlights(criteria, referenceData),
                response -> {
                    resultsTable.setItems(FXCollections.observableArrayList(response.results()));
                    if (response.results().isEmpty()) {
                        String message = "No matching flights were found.";
                        if (!response.note().isBlank()) {
                            message += " " + response.note();
                        }
                        setStatus(message, false);
                        return;
                    }

                    StringBuilder message = new StringBuilder("Loaded ")
                            .append(response.results().size())
                            .append(" matching flight option(s).");
                    if (!response.note().isBlank()) {
                        message.append(' ').append(response.note());
                    }
                    setStatus(message.toString(), false);
                }
        );
    }

    @FXML
    private void handleResetSearch() {
        fromCombo.getSelectionModel().clearSelection();
        fromCombo.getEditor().clear();
        toCombo.getSelectionModel().clearSelection();
        toCombo.getEditor().clear();
        departDate.setValue(null);
        returnDate.setValue(null);
        passengersSpinner.getValueFactory().setValue(1);
        seatCombo.getSelectionModel().select("Any");
        if (!classCombo.getItems().isEmpty()) {
            classCombo.getSelectionModel().selectFirst();
        } else {
            classCombo.getSelectionModel().clearSelection();
        }

        clearSelections(
                mealVegetarian,
                mealVegan,
                mealPescetarian,
                mealKosher,
                mealGlutenFree,
                mealStandard,
                bevWater,
                bevSoda,
                bevJuice,
                bevCoffee,
                bevTea
        );

        if (dataService == null || referenceData == null) {
            setStatus("Search filters reset.", false);
            return;
        }

        setStatus("Search filters reset. Loading all flights...", false);
        handleSearchFlights();
    }

    @FXML
    private void handleBookSelectedFlight() {
        FlightSearchResult selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a flight before creating a booking draft.", true);
            return;
        }

        String meals = String.join(", ", selectedValues(
                mealVegetarian,
                mealVegan,
                mealPescetarian,
                mealKosher,
                mealGlutenFree,
                mealStandard
        ));
        String beverages = String.join(", ", selectedValues(
                bevWater,
                bevSoda,
                bevJuice,
                bevCoffee,
                bevTea
        ));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Booking Draft");
        alert.setHeaderText("Selected flight is ready for the next booking step.");
        alert.setContentText("""
                Segment: %s
                Airline: %s
                Flight: %s
                Route: %s -> %s
                Depart: %s
                Arrive: %s
                Class: %s
                Passengers: %d
                Seat preference: %s
                Meals: %s
                Beverages: %s
                Estimated total: %s

                This screen is now loading real data, but booking submission is still a draft step.
                """.formatted(
                selected.getSegment(),
                selected.getAirline(),
                selected.getFlightNumber(),
                selected.getOrigin(),
                selected.getDestination(),
                selected.getDepart(),
                selected.getArrive(),
                selected.getClassName(),
                passengersSpinner.getValue(),
                defaultText(seatCombo.getValue(), "Any"),
                meals.isBlank() ? "None selected" : meals,
                beverages.isBlank() ? "None selected" : beverages,
                selected.getPrice()
        ));
        alert.showAndWait();
        setStatus("Booking draft prepared for " + selected.getFlightNumber() + ".", false);
    }

    @FXML
    private void handleAdminLogin() {
        if (adminStage != null && adminStage.isShowing()) {
            adminStage.requestFocus();
            return;
        }

        EnvConfig envConfig = EnvConfig.loadDefault();
        String expectedUsername = envConfig.get("ADMIN_USERNAME");
        String expectedPassword = envConfig.get("ADMIN_PASSWORD");

        if (expectedUsername.isBlank() || expectedPassword.isBlank()) {
            setStatus("Set ADMIN_USERNAME and ADMIN_PASSWORD in env before opening Admin.", true);
            return;
        }

        Optional<LoginAttempt> loginAttempt = showLoginDialog();
        if (loginAttempt.isEmpty()) {
            setStatus("Admin login canceled.", false);
            return;
        }

        LoginAttempt attempt = loginAttempt.get();
        if (!expectedUsername.equals(attempt.username()) || !expectedPassword.equals(attempt.password())) {
            setStatus("Admin login failed.", true);
            return;
        }

        try {
            openAdminWindow(envConfig);
            setStatus("Admin console opened.", false);
        } catch (IOException exception) {
            setStatus("Unable to open Admin window: " + exception.getMessage(), true);
        }
    }

    private void configureStaticControls() {
        seatCombo.getItems().setAll("Any", "Window", "Aisle", "Middle");
        seatCombo.getSelectionModel().select("Any");

        passengersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9, 1));
        passengersSpinner.setEditable(true);

        searchButton.setDisable(true);
        resetSearchButton.setDisable(true);
        bookButton.setDisable(true);
        resultsTable.setPlaceholder(new Label("Flights will load automatically when the app opens."));
        resultsTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            bookButton.setDisable(newValue == null);
            if (newValue != null) {
                setStatus("Selected " + newValue.getFlightNumber() + " for " + newValue.getClassName() + ".", false);
            }
        });
    }

    private void configureResultsTable() {
        colAirline.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAirline()));
        colFlightNo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getFlightNumber()));
        colFrom.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getOrigin()));
        colTo.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDestination()));
        colDepart.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDepart()));
        colArrive.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getArrive()));
        colPrice.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPrice()));
        colSeats.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeats()));
    }

    private void loadReferenceData() {
        runBackgroundTask(
                "Loading airports and classes...",
                dataService::loadReferenceData,
                data -> {
                    referenceData = data;
                    fromCombo.setItems(FXCollections.observableArrayList(data.airports()));
                    toCombo.setItems(FXCollections.observableArrayList(data.airports()));
                    classCombo.setItems(FXCollections.observableArrayList(data.classNames()));
                    if (!data.classNames().isEmpty()) {
                        classCombo.getSelectionModel().selectFirst();
                    }
                    searchButton.setDisable(false);
                    resetSearchButton.setDisable(false);
                    setStatus("Loaded " + data.airports().size() + " airport option(s) and " + data.classNames().size() + " travel class option(s).", false);
                    Platform.runLater(this::handleSearchFlights);
                }
        );
    }

    private <T> void runBackgroundTask(String runningMessage, Callable<T> action, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return action.call();
            }
        };

        task.setOnRunning(event -> {
            searchButton.setDisable(true);
            resetSearchButton.setDisable(true);
            setStatus(runningMessage, false);
        });
        task.setOnSucceeded(event -> {
            searchButton.setDisable(referenceData == null && !"Loading airports and classes...".equals(runningMessage));
            resetSearchButton.setDisable(referenceData == null);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            searchButton.setDisable(referenceData == null);
            resetSearchButton.setDisable(referenceData == null);
            Throwable exception = task.getException();
            setStatus(exception == null ? "Unknown error." : exception.getMessage(), true);
        });

        Thread worker = new Thread(task, "main-screen-data-task");
        worker.setDaemon(true);
        worker.start();
    }

    private Optional<LoginAttempt> showLoginDialog() {
        Dialog<LoginAttempt> dialog = new Dialog<>();
        dialog.setTitle("Admin Login");
        dialog.setHeaderText("Enter the admin credentials from env.");

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Username"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(usernameField::requestFocus);
        dialog.setResultConverter(button -> {
            if (button == loginButtonType) {
                return new LoginAttempt(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void openAdminWindow(EnvConfig envConfig) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/scenes/AdminScene.fxml"));
        Parent root = loader.load();

        AdminController controller = loader.getController();
        controller.configure(envConfig);

        adminStage = new Stage();
        adminStage.setTitle("Admin Console");
        adminStage.initModality(Modality.NONE);
        adminStage.setScene(new Scene(root));
        adminStage.setMinWidth(950);
        adminStage.setMinHeight(680);
        adminStage.setOnHidden(event -> adminStage = null);
        adminStage.show();
    }

    private List<String> selectedValues(CheckBox... checkBoxes) {
        List<String> values = new ArrayList<>();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isSelected()) {
                values.add(checkBox.getText());
            }
        }
        return values;
    }

    private void clearSelections(CheckBox... checkBoxes) {
        for (CheckBox checkBox : checkBoxes) {
            checkBox.setSelected(false);
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setStyle(error ? "-fx-text-fill: #b22222;" : "");
        statusLabel.setText(message);
    }

    private record LoginAttempt(String username, String password) {
    }
}
