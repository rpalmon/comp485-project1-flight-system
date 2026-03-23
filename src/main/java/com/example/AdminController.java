package com.example;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AdminController {
    private static final List<String> TABLES = List.of(
            "aircraft_models",
            "airlines",
            "airports",
            "bookings",
            "change_history",
            "flight_classes",
            "flights",
            "passengers",
            "payments",
            "profiles",
            "seat_map"
    );
    private static final Map<String, TableTemplate> TABLE_TEMPLATES = createTableTemplates();
    private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[H:mm][HH:mm][H:mm:ss][HH:mm:ss]")
            .toFormatter(Locale.US);
    private static final StringConverter<LookupOption> LOOKUP_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(LookupOption object) {
            return object == null ? "" : object.getLabel();
        }

        @Override
        public LookupOption fromString(String string) {
            return null;
        }
    };

    @FXML
    private TextField supabaseUrlField;

    @FXML
    private TextField anonKeyField;

    @FXML
    private ComboBox<String> tableCombo;

    @FXML
    private Label templateInfoLabel;

    @FXML
    private TextField selectColumnsField;

    @FXML
    private TextField orderByField;

    @FXML
    private ComboBox<String> orderDirectionCombo;

    @FXML
    private Spinner<Integer> limitSpinner;

    @FXML
    private ComboBox<LookupOption> flightAirlineCombo;

    @FXML
    private ComboBox<LookupOption> aircraftModelCombo;

    @FXML
    private ComboBox<LookupOption> originAirportCombo;

    @FXML
    private ComboBox<LookupOption> destinationAirportCombo;

    @FXML
    private TextField flightNumberField;

    @FXML
    private TextField basePriceField;

    @FXML
    private DatePicker departureDatePicker;

    @FXML
    private TextField departureTimeField;

    @FXML
    private DatePicker arrivalDatePicker;

    @FXML
    private TextField arrivalTimeField;

    @FXML
    private Spinner<Integer> totalSeatsSpinner;

    @FXML
    private Spinner<Integer> availableSeatsSpinner;

    @FXML
    private CheckBox createClassesCheckBox;

    @FXML
    private CheckBox generateSeatMapCheckBox;

    @FXML
    private Label builderInfoLabel;

    @FXML
    private TextField filterColumnField;

    @FXML
    private ComboBox<String> filterOperatorCombo;

    @FXML
    private TextField filterValueField;

    @FXML
    private TextArea insertPayloadArea;

    @FXML
    private TextField matchColumnField;

    @FXML
    private TextField matchValueField;

    @FXML
    private TextArea updatePayloadArea;

    @FXML
    private TextArea responseArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Button reloadEnvButton;

    @FXML
    private Button loadRowsButton;

    @FXML
    private Button applyTemplateButton;

    @FXML
    private Button refreshBuilderDataButton;

    @FXML
    private Button newAirlineButton;

    @FXML
    private Button newOriginAirportButton;

    @FXML
    private Button newDestinationAirportButton;

    @FXML
    private Button newAircraftModelButton;

    @FXML
    private Button createFlightButton;

    @FXML
    private Button clearFilterButton;

    @FXML
    private Button insertButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private AdminFlightBuilderService.ReferenceCatalog referenceCatalog;

    @FXML
    private void initialize() {
        tableCombo.getItems().setAll(TABLES);
        tableCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                applyTemplate(newValue, "Loaded template for " + newValue + ".");
            }
        });

        orderDirectionCombo.getItems().setAll("desc", "asc");
        orderDirectionCombo.getSelectionModel().select("desc");

        filterOperatorCombo.getItems().setAll("eq", "neq", "gt", "gte", "lt", "lte", "like", "ilike");
        filterOperatorCombo.getSelectionModel().select("eq");

        limitSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 500, 25));
        totalSeatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1200, 180));
        availableSeatsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1200, 180));
        totalSeatsSpinner.setEditable(true);
        availableSeatsSpinner.setEditable(true);

        configureLookupCombo(flightAirlineCombo);
        configureLookupCombo(originAirportCombo);
        configureLookupCombo(destinationAirportCombo);
        configureLookupCombo(aircraftModelCombo);
        aircraftModelCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyAircraftModelDefaults(newValue));

        departureDatePicker.setValue(LocalDate.now().plusDays(7));
        arrivalDatePicker.setValue(LocalDate.now().plusDays(7));
        departureTimeField.setText("08:00");
        arrivalTimeField.setText("12:00");
        basePriceField.setText("749.99");

        tableCombo.getSelectionModel().select("flights");
        statusLabel.setText("Ready.");
        builderInfoLabel.setText("Flight builder lookups will load from Supabase when this window opens.");
    }

    public void configure(EnvConfig envConfig) {
        supabaseUrlField.setText(envConfig.get("SUPABASE_URL"));
        anonKeyField.setText(envConfig.getOrDefault("SUPABASE_ANON_KEY", envConfig.get("SUPABASE_PUBLISHABLE_KEY")));
        statusLabel.setText("Loaded Supabase settings from " + envConfig.sourcePath() + ".");
        loadBuilderReferences();
    }

    @FXML
    private void handleReloadEnv() {
        EnvConfig envConfig = EnvConfig.loadDefault();
        configure(envConfig);
        setStatus("Reloaded Supabase settings from " + envConfig.sourcePath() + ".", false);
    }

    @FXML
    private void handleReloadBuilderReferences() {
        loadBuilderReferences();
    }

    @FXML
    private void handleCreateAirline() {
        Optional<AdminFlightBuilderService.AirlineDraft> draft = showAirlineDialog(flightAirlineCombo.getEditor().getText());
        if (draft.isEmpty()) {
            setStatus("Airline creation canceled.", false);
            return;
        }

        runTask("Creating airline...", () -> builderService().createAirline(draft.get()), option -> {
            ensureReferenceCatalog();
            upsertLookupOption(referenceCatalog.airlines(), option);
            refreshLookupCombos();
            selectLookupOption(flightAirlineCombo, option);
            responseArea.setText("""
                    {
                      "created": "airline",
                      "id": "%s",
                      "label": "%s"
                    }
                    """.formatted(option.getId(), option.getLabel()));
            builderInfoLabel.setText("Created airline and selected it for the flight builder.");
            setStatus("Created airline " + option.getLabel() + ".", false);
        });
    }

    @FXML
    private void handleCreateOriginAirport() {
        createAirportForCombo(originAirportCombo, "origin airport");
    }

    @FXML
    private void handleCreateDestinationAirport() {
        createAirportForCombo(destinationAirportCombo, "destination airport");
    }

    @FXML
    private void handleCreateAircraftModel() {
        Optional<AdminFlightBuilderService.AircraftModelDraft> draft = showAircraftModelDialog(aircraftModelCombo.getEditor().getText());
        if (draft.isEmpty()) {
            setStatus("Aircraft model creation canceled.", false);
            return;
        }

        runTask("Creating aircraft model...", () -> builderService().createAircraftModel(draft.get()), spec -> {
            ensureReferenceCatalog();
            referenceCatalog.aircraftSpecsById().put(spec.id(), spec);
            LookupOption option = spec.toLookupOption();
            upsertLookupOption(referenceCatalog.aircraftModels(), option);
            refreshLookupCombos();
            selectLookupOption(aircraftModelCombo, option);
            applyAircraftModelDefaults(option);
            responseArea.setText("""
                    {
                      "created": "aircraft_model",
                      "id": "%s",
                      "label": "%s"
                    }
                    """.formatted(option.getId(), option.getLabel()));
            builderInfoLabel.setText("Created aircraft model and applied its seat counts to the flight builder.");
            setStatus("Created aircraft model " + option.getLabel() + ".", false);
        });
    }

    @FXML
    private void handleCreateFlight() {
        try {
            ResolvedReference<AdminFlightBuilderService.AirlineDraft> airline = resolveAirline();
            if (airline == null) {
                return;
            }

            ResolvedReference<AdminFlightBuilderService.AirportDraft> origin = resolveAirport(originAirportCombo, "origin airport");
            if (origin == null) {
                return;
            }

            ResolvedReference<AdminFlightBuilderService.AirportDraft> destination = resolveAirport(destinationAirportCombo, "destination airport");
            if (destination == null) {
                return;
            }

            ResolvedReference<AdminFlightBuilderService.AircraftModelDraft> aircraftModel = resolveAircraftModel();
            if (aircraftModel == null) {
                return;
            }

            if (sameReference(origin, destination)) {
                setStatus("Origin and destination airports must be different.", true);
                return;
            }

            String flightNumber = requireText(flightNumberField.getText(), "Flight number");
            OffsetDateTime departureAt = parseTimestamp(departureDatePicker, departureTimeField, "departure");
            OffsetDateTime arrivalAt = parseTimestamp(arrivalDatePicker, arrivalTimeField, "arrival");
            if (!arrivalAt.isAfter(departureAt)) {
                setStatus("Arrival time must be after departure time.", true);
                return;
            }

            double basePrice = Double.parseDouble(requireText(basePriceField.getText(), "Base price"));
            int totalSeats = totalSeatsSpinner.getValue();
            int availableSeats = availableSeatsSpinner.getValue();
            if (totalSeats <= 0) {
                setStatus("Total seats must be greater than zero.", true);
                return;
            }
            if (availableSeats < 0 || availableSeats > totalSeats) {
                setStatus("Available seats must be between 0 and total seats.", true);
                return;
            }

            runTask("Creating flight...", () -> {
                AdminFlightBuilderService service = builderService();

                LookupOption airlineOption = airline.existingOption();
                if (airline.requiresCreation()) {
                    airlineOption = service.createAirline(airline.draft());
                }

                LookupOption originOption = origin.existingOption();
                if (origin.requiresCreation()) {
                    originOption = service.createAirport(origin.draft());
                }

                LookupOption destinationOption = destination.existingOption();
                if (destination.requiresCreation()) {
                    destinationOption = service.createAirport(destination.draft());
                }

                LookupOption aircraftModelOption = aircraftModel.existingOption();
                AdminFlightBuilderService.AircraftModelSpec aircraftSpec = aircraftModelOption == null
                        ? null
                        : referenceCatalog.aircraftSpecsById().get(aircraftModelOption.getId());
                if (aircraftModel.requiresCreation()) {
                    aircraftSpec = service.createAircraftModel(aircraftModel.draft());
                    aircraftModelOption = aircraftSpec.toLookupOption();
                }

                AdminFlightBuilderService.FlightCreationRequest request = new AdminFlightBuilderService.FlightCreationRequest(
                        airlineOption.getId(),
                        flightNumber,
                        originOption.getId(),
                        destinationOption.getId(),
                        departureAt,
                        arrivalAt,
                        aircraftModelOption == null ? null : aircraftModelOption.getId(),
                        totalSeats,
                        availableSeats,
                        basePrice,
                        createClassesCheckBox.isSelected(),
                        generateSeatMapCheckBox.isSelected()
                );

                AdminFlightBuilderService.FlightCreationResult result = service.createFlight(request, aircraftSpec);
                return new FlightBuildOutcome(airlineOption, originOption, destinationOption, aircraftModelOption, aircraftSpec, result);
            }, outcome -> {
                ensureReferenceCatalog();
                upsertLookupOption(referenceCatalog.airlines(), outcome.airline());
                upsertLookupOption(referenceCatalog.airports(), outcome.originAirport());
                upsertLookupOption(referenceCatalog.airports(), outcome.destinationAirport());
                if (outcome.aircraftModelOption() != null) {
                    upsertLookupOption(referenceCatalog.aircraftModels(), outcome.aircraftModelOption());
                }
                if (outcome.aircraftModelSpec() != null) {
                    referenceCatalog.aircraftSpecsById().put(outcome.aircraftModelSpec().id(), outcome.aircraftModelSpec());
                }

                refreshLookupCombos();
                selectLookupOption(flightAirlineCombo, outcome.airline());
                selectLookupOption(originAirportCombo, outcome.originAirport());
                selectLookupOption(destinationAirportCombo, outcome.destinationAirport());
                if (outcome.aircraftModelOption() != null) {
                    selectLookupOption(aircraftModelCombo, outcome.aircraftModelOption());
                }

                responseArea.setText("""
                        {
                          "created": "flight",
                          "flight_id": "%s",
                          "class_rows_created": %d,
                          "seat_rows_created": %d
                        }
                        """.formatted(
                        outcome.result().flightId(),
                        outcome.result().classRowsCreated(),
                        outcome.result().seatRowsCreated()
                ));

                String seatMapNote = generateSeatMapCheckBox.isSelected() && outcome.aircraftModelSpec() == null
                        ? " Seat map generation was skipped because no aircraft model was selected."
                        : "";
                builderInfoLabel.setText("Created flight " + flightNumber + " with "
                        + outcome.result().classRowsCreated() + " class row(s) and "
                        + outcome.result().seatRowsCreated() + " seat row(s)." + seatMapNote);
                setStatus("Created flight " + flightNumber + ".", false);
            });
        } catch (NumberFormatException exception) {
            setStatus("Price and time fields must be valid numbers and HH:mm values.", true);
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage(), true);
        }
    }

    @FXML
    private void handleClearFilter() {
        filterColumnField.clear();
        filterValueField.clear();
        filterOperatorCombo.getSelectionModel().select("eq");
        setStatus("Filter cleared.", false);
    }

    @FXML
    private void handleApplyTemplate() {
        applyTemplate(tableName(), "Reloaded template for " + tableName() + ".");
    }

    @FXML
    private void handleLoadRows() {
        runRequest("Load rows", () -> builderService().loadRows(
                tableName(),
                selectColumnsField.getText(),
                limitSpinner.getValue(),
                orderByField.getText(),
                orderDirectionCombo.getValue(),
                filterColumnField.getText(),
                filterOperatorCombo.getValue(),
                filterValueField.getText()
        ));
    }

    @FXML
    private void handleInsertRow() {
        runRequest("Insert row", () -> builderService().insert(tableName(), insertPayloadArea.getText()));
    }

    @FXML
    private void handleUpdateRows() {
        runRequest("Update row", () -> builderService().update(
                tableName(),
                matchColumnField.getText(),
                matchValueField.getText(),
                updatePayloadArea.getText()
        ));
    }

    @FXML
    private void handleDeleteRows() {
        runRequest("Delete row", () -> builderService().delete(
                tableName(),
                matchColumnField.getText(),
                matchValueField.getText()
        ));
    }

    private void runRequest(String action, Callable<String> request) {
        runTask(action, request, response -> {
            responseArea.setText(response);
            setStatus(action + " complete.", false);
        });
    }

    private <T> void runTask(String actionMessage, Callable<T> request, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return request.call();
            }
        };

        task.setOnRunning(event -> setBusyState(true, actionMessage));
        task.setOnSucceeded(event -> {
            setBusyState(false, actionMessage + " complete.");
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            responseArea.setText(exception == null ? "" : exception.getMessage());
            setBusyState(false, actionMessage + " failed: " + (exception == null ? "unknown error" : exception.getMessage()), true);
        });

        Thread worker = new Thread(task, "admin-console-task");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusyState(boolean busy, String message) {
        setBusyState(busy, message, false);
    }

    private void setBusyState(boolean busy, String message, boolean error) {
        reloadEnvButton.setDisable(busy);
        loadRowsButton.setDisable(busy);
        applyTemplateButton.setDisable(busy);
        refreshBuilderDataButton.setDisable(busy);
        newAirlineButton.setDisable(busy);
        newOriginAirportButton.setDisable(busy);
        newDestinationAirportButton.setDisable(busy);
        newAircraftModelButton.setDisable(busy);
        createFlightButton.setDisable(busy);
        clearFilterButton.setDisable(busy);
        insertButton.setDisable(busy);
        updateButton.setDisable(busy);
        deleteButton.setDisable(busy);
        setStatus(message, error);
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setStyle(error ? "-fx-text-fill: #b22222;" : "");
        statusLabel.setText(message);
    }

    private String tableName() {
        String table = tableCombo.getValue();
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("Select a table first.");
        }
        return table;
    }

    private void configureLookupCombo(ComboBox<LookupOption> comboBox) {
        comboBox.setEditable(true);
        comboBox.setConverter(LOOKUP_CONVERTER);
    }

    private void loadBuilderReferences() {
        runTask("Loading builder lookups...", () -> new AdminFlightBuilderService(supabaseUrlField.getText(), anonKeyField.getText()).loadReferenceCatalog(), catalog -> {
            referenceCatalog = catalog;
            refreshLookupCombos();
            builderInfoLabel.setText("Loaded " + catalog.airlines().size() + " airlines, "
                    + catalog.airports().size() + " airports, and "
                    + catalog.aircraftModels().size() + " aircraft models.");
            setStatus("Builder lookups refreshed.", false);
        });
    }

    private void refreshLookupCombos() {
        flightAirlineCombo.setItems(FXCollections.observableArrayList(referenceCatalog.airlines()));
        originAirportCombo.setItems(FXCollections.observableArrayList(referenceCatalog.airports()));
        destinationAirportCombo.setItems(FXCollections.observableArrayList(referenceCatalog.airports()));
        aircraftModelCombo.setItems(FXCollections.observableArrayList(referenceCatalog.aircraftModels()));
    }

    private void applyAircraftModelDefaults(LookupOption option) {
        if (option == null || referenceCatalog == null) {
            return;
        }

        AdminFlightBuilderService.AircraftModelSpec spec = referenceCatalog.aircraftSpecsById().get(option.getId());
        if (spec == null) {
            return;
        }

        totalSeatsSpinner.getValueFactory().setValue(Math.max(1, spec.seatsTotal()));
        availableSeatsSpinner.getValueFactory().setValue(Math.max(0, spec.seatsTotal()));
        builderInfoLabel.setText("Applied aircraft model defaults for " + option.getLabel()
                + ". Cabin split: economy " + spec.seatsEconomy()
                + ", business " + spec.seatsBusiness()
                + ", first " + spec.seatsFirst() + ".");
    }

    private void createAirportForCombo(ComboBox<LookupOption> comboBox, String description) {
        Optional<AdminFlightBuilderService.AirportDraft> draft = showAirportDialog(comboBox.getEditor().getText());
        if (draft.isEmpty()) {
            setStatus("Airport creation canceled.", false);
            return;
        }

        runTask("Creating airport...", () -> builderService().createAirport(draft.get()), option -> {
            ensureReferenceCatalog();
            upsertLookupOption(referenceCatalog.airports(), option);
            refreshLookupCombos();
            selectLookupOption(comboBox, option);
            responseArea.setText("""
                    {
                      "created": "airport",
                      "id": "%s",
                      "label": "%s"
                    }
                    """.formatted(option.getId(), option.getLabel()));
            builderInfoLabel.setText("Created " + description + " and selected it in the flight builder.");
            setStatus("Created airport " + option.getLabel() + ".", false);
        });
    }

    private ResolvedReference<AdminFlightBuilderService.AirlineDraft> resolveAirline() {
        LookupOption existing = findExistingLookup(flightAirlineCombo, "airline");
        if (existing != null) {
            return new ResolvedReference<>(existing, null);
        }

        String typed = typedText(flightAirlineCombo);
        if (typed.isBlank()) {
            throw new IllegalArgumentException("Select or type an airline.");
        }

        Optional<AdminFlightBuilderService.AirlineDraft> draft = showAirlineDialog(typed);
        if (draft.isEmpty()) {
            setStatus("Flight creation canceled before airline creation.", false);
            return null;
        }

        return new ResolvedReference<>(null, draft.get());
    }

    private ResolvedReference<AdminFlightBuilderService.AirportDraft> resolveAirport(ComboBox<LookupOption> comboBox, String description) {
        LookupOption existing = findExistingLookup(comboBox, description);
        if (existing != null) {
            return new ResolvedReference<>(existing, null);
        }

        String typed = typedText(comboBox);
        if (typed.isBlank()) {
            throw new IllegalArgumentException("Select or type a " + description + ".");
        }

        Optional<AdminFlightBuilderService.AirportDraft> draft = showAirportDialog(typed);
        if (draft.isEmpty()) {
            setStatus("Flight creation canceled before " + description + " creation.", false);
            return null;
        }

        return new ResolvedReference<>(null, draft.get());
    }

    private ResolvedReference<AdminFlightBuilderService.AircraftModelDraft> resolveAircraftModel() {
        LookupOption existing = findExistingLookup(aircraftModelCombo, "aircraft model");
        if (existing != null) {
            return new ResolvedReference<>(existing, null);
        }

        String typed = typedText(aircraftModelCombo);
        if (typed.isBlank()) {
            return new ResolvedReference<>(null, null);
        }

        Optional<AdminFlightBuilderService.AircraftModelDraft> draft = showAircraftModelDialog(typed);
        if (draft.isEmpty()) {
            setStatus("Flight creation canceled before aircraft model creation.", false);
            return null;
        }

        return new ResolvedReference<>(null, draft.get());
    }

    private LookupOption findExistingLookup(ComboBox<LookupOption> comboBox, String label) {
        if (comboBox.getValue() != null) {
            return comboBox.getValue();
        }

        String typed = typedText(comboBox);
        if (typed.isBlank()) {
            return null;
        }

        List<LookupOption> exactMatches = comboBox.getItems().stream()
                .filter(option -> option.matchesExact(typed))
                .toList();
        if (exactMatches.size() == 1) {
            LookupOption option = exactMatches.get(0);
            selectLookupOption(comboBox, option);
            return option;
        }

        List<LookupOption> looseMatches = comboBox.getItems().stream()
                .filter(option -> option.matchesLoose(typed))
                .toList();
        if (looseMatches.size() == 1) {
            LookupOption option = looseMatches.get(0);
            selectLookupOption(comboBox, option);
            return option;
        }

        if (looseMatches.size() > 1) {
            throw new IllegalArgumentException("Multiple " + label + " matches were found for \"" + typed + "\". Pick one from the dropdown.");
        }

        return null;
    }

    private OffsetDateTime parseTimestamp(DatePicker datePicker, TextField timeField, String label) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Select a " + label + " date.");
        }

        LocalTime time = LocalTime.parse(requireText(timeField.getText(), capitalize(label) + " time"), TIME_FORMATTER);
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private String typedText(ComboBox<LookupOption> comboBox) {
        return comboBox.getEditor().getText() == null ? "" : comboBox.getEditor().getText().trim();
    }

    private boolean sameReference(ResolvedReference<AdminFlightBuilderService.AirportDraft> origin, ResolvedReference<AdminFlightBuilderService.AirportDraft> destination) {
        if (origin.existingOption() != null && destination.existingOption() != null) {
            return origin.existingOption().getId().equals(destination.existingOption().getId());
        }

        if (origin.draft() != null && destination.draft() != null) {
            return normalize(origin.draft().name()).equals(normalize(destination.draft().name()))
                    && normalize(origin.draft().city()).equals(normalize(destination.draft().city()));
        }

        return false;
    }

    private void selectLookupOption(ComboBox<LookupOption> comboBox, LookupOption option) {
        comboBox.getSelectionModel().select(option);
        comboBox.getEditor().setText(option.getLabel());
    }

    private void upsertLookupOption(List<LookupOption> options, LookupOption option) {
        options.removeIf(existing -> existing.getId().equals(option.getId()));
        options.add(option);
        options.sort((left, right) -> left.getLabel().compareToIgnoreCase(right.getLabel()));
    }

    private void ensureReferenceCatalog() {
        if (referenceCatalog == null) {
            referenceCatalog = new AdminFlightBuilderService.ReferenceCatalog(
                    FXCollections.observableArrayList(),
                    FXCollections.observableArrayList(),
                    FXCollections.observableArrayList(),
                    new LinkedHashMap<>()
            );
        }
    }

    private AdminFlightBuilderService builderService() {
        return new AdminFlightBuilderService(supabaseUrlField.getText(), anonKeyField.getText());
    }

    private Optional<AdminFlightBuilderService.AirlineDraft> showAirlineDialog(String initialText) {
        Dialog<AdminFlightBuilderService.AirlineDraft> dialog = new Dialog<>();
        dialog.setTitle("Create Airline");
        dialog.setHeaderText("Enter the airline details.");

        ButtonType saveButtonType = new ButtonType("Create Airline", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField codeField = new TextField();
        TextField nameField = new TextField();
        TextField countryField = new TextField();

        String trimmed = initialText == null ? "" : initialText.trim();
        if (trimmed.length() <= 3 && trimmed.equals(trimmed.toUpperCase(Locale.ROOT))) {
            codeField.setText(trimmed);
        } else {
            nameField.setText(trimmed);
        }

        codeField.setPromptText("AA");
        nameField.setPromptText("Airline name");
        countryField.setPromptText("Country");

        GridPane grid = createDialogGrid();
        grid.add(new Label("Code"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Name"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Country"), 0, 2);
        grid.add(countryField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        keepDialogOpenOnValidationFailure(dialog, saveButtonType, () -> {
            if (nameField.getText() == null || nameField.getText().trim().isBlank()) {
                return "Airline name is required.";
            }
            return null;
        });
        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                return new AdminFlightBuilderService.AirlineDraft(
                        upperOrNull(codeField.getText()),
                        nameField.getText().trim(),
                        emptyToNull(countryField.getText())
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Optional<AdminFlightBuilderService.AirportDraft> showAirportDialog(String initialText) {
        Dialog<AdminFlightBuilderService.AirportDraft> dialog = new Dialog<>();
        dialog.setTitle("Create Airport");
        dialog.setHeaderText("Enter the airport details.");

        ButtonType saveButtonType = new ButtonType("Create Airport", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField iataField = new TextField();
        TextField icaoField = new TextField();
        TextField nameField = new TextField();
        TextField cityField = new TextField();
        TextField countryField = new TextField();
        TextField timezoneField = new TextField();

        String trimmed = initialText == null ? "" : initialText.trim();
        if (trimmed.length() == 3 && trimmed.equals(trimmed.toUpperCase(Locale.ROOT))) {
            iataField.setText(trimmed);
        } else if (trimmed.length() == 4 && trimmed.equals(trimmed.toUpperCase(Locale.ROOT))) {
            icaoField.setText(trimmed);
        } else {
            cityField.setText(trimmed);
            nameField.setText(trimmed);
        }

        iataField.setPromptText("LAX");
        icaoField.setPromptText("KLAX");
        nameField.setPromptText("Los Angeles International Airport");
        cityField.setPromptText("Los Angeles");
        countryField.setPromptText("United States");
        timezoneField.setPromptText("America/Los_Angeles");

        GridPane grid = createDialogGrid();
        grid.add(new Label("IATA"), 0, 0);
        grid.add(iataField, 1, 0);
        grid.add(new Label("ICAO"), 0, 1);
        grid.add(icaoField, 1, 1);
        grid.add(new Label("Name"), 0, 2);
        grid.add(nameField, 1, 2);
        grid.add(new Label("City"), 0, 3);
        grid.add(cityField, 1, 3);
        grid.add(new Label("Country"), 0, 4);
        grid.add(countryField, 1, 4);
        grid.add(new Label("Timezone"), 0, 5);
        grid.add(timezoneField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        keepDialogOpenOnValidationFailure(dialog, saveButtonType, () -> {
            if (nameField.getText() == null || nameField.getText().trim().isBlank()) {
                return "Airport name is required.";
            }
            if (cityField.getText() == null || cityField.getText().trim().isBlank()) {
                return "Airport city is required.";
            }
            if (countryField.getText() == null || countryField.getText().trim().isBlank()) {
                return "Airport country is required.";
            }
            return null;
        });
        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                return new AdminFlightBuilderService.AirportDraft(
                        upperOrNull(iataField.getText()),
                        upperOrNull(icaoField.getText()),
                        nameField.getText().trim(),
                        cityField.getText().trim(),
                        countryField.getText().trim(),
                        emptyToNull(timezoneField.getText())
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Optional<AdminFlightBuilderService.AircraftModelDraft> showAircraftModelDialog(String initialText) {
        Dialog<AdminFlightBuilderService.AircraftModelDraft> dialog = new Dialog<>();
        dialog.setTitle("Create Aircraft Model");
        dialog.setHeaderText("Enter the aircraft model details.");

        ButtonType saveButtonType = new ButtonType("Create Model", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField modelCodeField = new TextField(initialText == null ? "" : initialText.trim());
        TextField manufacturerField = new TextField();
        TextField seatsTotalField = new TextField("180");
        TextField seatsEconomyField = new TextField("150");
        TextField seatsBusinessField = new TextField("24");
        TextField seatsFirstField = new TextField("6");

        modelCodeField.setPromptText("A321NEO");
        manufacturerField.setPromptText("Airbus");

        GridPane grid = createDialogGrid();
        grid.add(new Label("Model Code"), 0, 0);
        grid.add(modelCodeField, 1, 0);
        grid.add(new Label("Manufacturer"), 0, 1);
        grid.add(manufacturerField, 1, 1);
        grid.add(new Label("Total Seats"), 0, 2);
        grid.add(seatsTotalField, 1, 2);
        grid.add(new Label("Economy Seats"), 0, 3);
        grid.add(seatsEconomyField, 1, 3);
        grid.add(new Label("Business Seats"), 0, 4);
        grid.add(seatsBusinessField, 1, 4);
        grid.add(new Label("First Seats"), 0, 5);
        grid.add(seatsFirstField, 1, 5);

        dialog.getDialogPane().setContent(grid);
        keepDialogOpenOnValidationFailure(dialog, saveButtonType, () -> validateAircraftModelFields(
                modelCodeField,
                seatsTotalField,
                seatsEconomyField,
                seatsBusinessField,
                seatsFirstField
        ));
        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                return new AdminFlightBuilderService.AircraftModelDraft(
                        modelCodeField.getText().trim(),
                        emptyToNull(manufacturerField.getText()),
                        Integer.parseInt(seatsTotalField.getText().trim()),
                        Integer.parseInt(seatsEconomyField.getText().trim()),
                        Integer.parseInt(seatsBusinessField.getText().trim()),
                        Integer.parseInt(seatsFirstField.getText().trim())
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void keepDialogOpenOnValidationFailure(Dialog<?> dialog, ButtonType saveButtonType, Supplier<String> validator) {
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            String validationMessage = validator.get();
            if (validationMessage != null) {
                setStatus(validationMessage, true);
                event.consume();
            }
        });
    }

    private String validateAircraftModelFields(
            TextField modelCodeField,
            TextField seatsTotalField,
            TextField seatsEconomyField,
            TextField seatsBusinessField,
            TextField seatsFirstField
    ) {
        if (modelCodeField.getText() == null || modelCodeField.getText().trim().isBlank()) {
            return "Model code is required.";
        }

        try {
            int totalSeats = Integer.parseInt(seatsTotalField.getText().trim());
            int economySeats = Integer.parseInt(seatsEconomyField.getText().trim());
            int businessSeats = Integer.parseInt(seatsBusinessField.getText().trim());
            int firstSeats = Integer.parseInt(seatsFirstField.getText().trim());

            if (totalSeats <= 0) {
                return "Total seats must be greater than zero.";
            }
            if (economySeats < 0 || businessSeats < 0 || firstSeats < 0) {
                return "Cabin seat counts cannot be negative.";
            }
            if (economySeats + businessSeats + firstSeats > totalSeats) {
                return "Cabin seat counts cannot exceed total seats.";
            }
        } catch (NumberFormatException exception) {
            return "Seat counts must be whole numbers.";
        }

        return null;
    }

    private GridPane createDialogGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    private String requireDialogValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }

    private int parseDialogInt(String value, String label) {
        return Integer.parseInt(requireDialogValue(value, label));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upperOrNull(String value) {
        String trimmed = emptyToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private void applyTemplate(String tableName, String statusMessage) {
        TableTemplate template = TABLE_TEMPLATES.get(tableName);
        if (template == null) {
            return;
        }

        selectColumnsField.setText(template.selectColumns());
        orderByField.setText(template.orderBy());
        filterColumnField.setText(template.filterColumn());
        filterValueField.clear();
        filterValueField.setPromptText(template.filterValueExample());
        matchColumnField.setText(template.matchColumn());
        matchValueField.clear();
        matchValueField.setPromptText(template.matchValueExample());
        insertPayloadArea.setText(template.insertPayload());
        updatePayloadArea.setText(template.updatePayload());
        templateInfoLabel.setText(template.helpText());
        setStatus(statusMessage, false);
    }

    private record ResolvedReference<T>(LookupOption existingOption, T draft) {
        private boolean requiresCreation() {
            return draft != null;
        }
    }

    private record FlightBuildOutcome(
            LookupOption airline,
            LookupOption originAirport,
            LookupOption destinationAirport,
            LookupOption aircraftModelOption,
            AdminFlightBuilderService.AircraftModelSpec aircraftModelSpec,
            AdminFlightBuilderService.FlightCreationResult result
    ) {
    }

    private static Map<String, TableTemplate> createTableTemplates() {
        Map<String, TableTemplate> templates = new LinkedHashMap<>();

        templates.put("aircraft_models", new TableTemplate(
                "*",
                "created_at",
                "model_code",
                "A321NEO",
                "id",
                "existing-aircraft-model-uuid",
                """
                {
                  "model_code": "A321NEO",
                  "manufacturer": "Airbus",
                  "seats_total": 180,
                  "seats_economy": 150,
                  "seats_business": 24,
                  "seats_first": 6
                }
                """,
                """
                {
                  "manufacturer": "Airbus",
                  "seats_total": 186,
                  "seats_economy": 156,
                  "seats_business": 24,
                  "seats_first": 6
                }
                """,
                "Aircraft models define seat capacity. Keep seats_total aligned with the cabin split fields."
        ));

        templates.put("airlines", new TableTemplate(
                "*",
                "created_at",
                "code",
                "DL",
                "code",
                "DL",
                """
                {
                  "code": "OA",
                  "name": "OpenAI Airways",
                  "country": "United States"
                }
                """,
                """
                {
                  "name": "OpenAI Airways International",
                  "country": "United States"
                }
                """,
                "Airline code is unique when present. Use a short carrier code for easier lookups."
        ));

        templates.put("airports", new TableTemplate(
                "*",
                "created_at",
                "iata_code",
                "LAX",
                "iata_code",
                "LAX",
                """
                {
                  "iata_code": "LAX",
                  "icao_code": "KLAX",
                  "name": "Los Angeles International Airport",
                  "city": "Los Angeles",
                  "country": "United States",
                  "timezone": "America/Los_Angeles",
                  "lat": 33.9416,
                  "lng": -118.4085
                }
                """,
                """
                {
                  "name": "Los Angeles International Airport",
                  "city": "Los Angeles",
                  "country": "United States",
                  "timezone": "America/Los_Angeles",
                  "lat": 33.9416,
                  "lng": -118.4085
                }
                """,
                "Airport records usually use a 3-letter IATA code and a 4-letter ICAO code."
        ));

        templates.put("bookings", new TableTemplate(
                "*",
                "booked_at",
                "booking_reference",
                "BK-20260322-001",
                "booking_reference",
                "BK-20260322-001",
                """
                {
                  "user_id": null,
                  "booking_reference": "BK-20260322-001",
                  "flight_id": "existing-flight-uuid",
                  "class_name": "economy",
                  "num_passengers": 2,
                  "total_price": 899.98,
                  "status": "pending",
                  "depart_at": "2026-04-10T08:00:00Z",
                  "arrive_at": "2026-04-10T19:45:00Z",
                  "contact_email": "traveler@example.com",
                  "contact_phone": "+1-555-0100"
                }
                """,
                """
                {
                  "class_name": "business",
                  "num_passengers": 2,
                  "total_price": 1299.98,
                  "status": "confirmed",
                  "contact_email": "traveler@example.com",
                  "contact_phone": "+1-555-0100"
                }
                """,
                "Bookings must reference an existing flight. user_id can be null for guest flows if your app allows it."
        ));

        templates.put("change_history", new TableTemplate(
                "*",
                "created_at",
                "entity_table",
                "flights",
                "id",
                "existing-change-history-uuid",
                """
                {
                  "entity_table": "flights",
                  "entity_id": "existing-entity-uuid",
                  "changed_by": null,
                  "change_type": "update",
                  "change": {
                    "field": "base_price",
                    "from": 799.99,
                    "to": 749.99
                  }
                }
                """,
                """
                {
                  "change_type": "manual_review",
                  "change": {
                    "note": "Reviewed by admin after fare adjustment."
                  }
                }
                """,
                "Use change_history for audit-style rows. The change column accepts nested JSON."
        ));

        templates.put("flight_classes", new TableTemplate(
                "*",
                "created_at",
                "class_name",
                "economy",
                "id",
                "existing-flight-class-uuid",
                """
                {
                  "flight_id": "existing-flight-uuid",
                  "class_name": "economy",
                  "seats_total": 120,
                  "seats_available": 88,
                  "price_modifier": 1.0
                }
                """,
                """
                {
                  "seats_total": 120,
                  "seats_available": 80,
                  "price_modifier": 1.15
                }
                """,
                "flight_classes should line up with the booking class names your UI uses."
        ));

        templates.put("flights", new TableTemplate(
                "*",
                "departure_at",
                "flight_number",
                "OA101",
                "id",
                "existing-flight-uuid",
                """
                {
                  "airline_id": "existing-airline-uuid",
                  "flight_number": "OA101",
                  "origin_airport_id": "existing-origin-airport-uuid",
                  "destination_airport_id": "existing-destination-airport-uuid",
                  "departure_at": "2026-04-10T08:00:00Z",
                  "arrival_at": "2026-04-10T19:45:00Z",
                  "aircraft_model_id": "existing-aircraft-model-uuid",
                  "total_seats": 180,
                  "available_seats": 180,
                  "base_price": 749.99
                }
                """,
                """
                {
                  "departure_at": "2026-04-10T08:30:00Z",
                  "arrival_at": "2026-04-10T20:15:00Z",
                  "available_seats": 172,
                  "base_price": 719.99
                }
                """,
                "Flights require valid airline, airport, and optionally aircraft model UUIDs."
        ));

        templates.put("passengers", new TableTemplate(
                "*",
                "created_at",
                "full_name",
                "Taylor Example",
                "id",
                "existing-passenger-uuid",
                """
                {
                  "booking_id": "existing-booking-uuid",
                  "full_name": "Taylor Example",
                  "passenger_type": "adult",
                  "seat_label": "12A",
                  "seat_preferences": ["window"],
                  "meal_preferences": ["vegetarian"],
                  "beverages": ["water", "coffee"]
                }
                """,
                """
                {
                  "seat_label": "14C",
                  "seat_preferences": ["aisle"],
                  "meal_preferences": ["standard"],
                  "beverages": ["tea"]
                }
                """,
                "Passenger preference fields accept JSON arrays when sent through Supabase REST."
        ));

        templates.put("payments", new TableTemplate(
                "*",
                "created_at",
                "status",
                "initiated",
                "id",
                "existing-payment-uuid",
                """
                {
                  "booking_id": "existing-booking-uuid",
                  "provider": "stripe",
                  "provider_charge_id": "pi_demo_123",
                  "amount": 899.98,
                  "currency": "USD",
                  "status": "initiated",
                  "metadata": {
                    "card_last4": "4242",
                    "source": "admin"
                  }
                }
                """,
                """
                {
                  "provider_charge_id": "pi_live_456",
                  "status": "succeeded",
                  "metadata": {
                    "receipt_sent": true
                  },
                  "updated_at": "2026-03-22T18:30:00Z"
                }
                """,
                "Payment metadata is JSON. Update updated_at yourself unless you add a database trigger."
        ));

        templates.put("profiles", new TableTemplate(
                "*",
                "created_at",
                "email",
                "user@example.com",
                "id",
                "existing-auth-user-uuid",
                """
                {
                  "id": "existing-auth-user-uuid",
                  "email": "user@example.com",
                  "full_name": "Jordan Example",
                  "phone": "+1-555-0101"
                }
                """,
                """
                {
                  "email": "user@example.com",
                  "full_name": "Jordan Example",
                  "phone": "+1-555-0102",
                  "updated_at": "2026-03-22T18:30:00Z"
                }
                """,
                "profiles.id must match an existing auth.users id. Insert only when that auth row already exists."
        ));

        templates.put("seat_map", new TableTemplate(
                "*",
                "created_at",
                "seat_label",
                "12A",
                "id",
                "existing-seat-map-uuid",
                """
                {
                  "flight_id": "existing-flight-uuid",
                  "seat_label": "12A",
                  "class_name": "economy",
                  "is_window": true,
                  "is_aisle": false,
                  "is_middle": false,
                  "is_available": true
                }
                """,
                """
                {
                  "class_name": "economy",
                  "is_window": true,
                  "is_aisle": false,
                  "is_middle": false,
                  "is_available": false
                }
                """,
                "seat_map stores one row per seat on a flight. Keep seat labels unique within a flight."
        ));

        return Map.copyOf(templates);
    }

    private record TableTemplate(
            String selectColumns,
            String orderBy,
            String filterColumn,
            String filterValueExample,
            String matchColumn,
            String matchValueExample,
            String insertPayload,
            String updatePayload,
            String helpText
    ) {
    }
}
