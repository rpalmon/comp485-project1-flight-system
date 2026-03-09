package ui;

import com.example.db.Database;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;

public class FlightBookingController {

    @FXML private ComboBox<String> fromCombo;
    @FXML private ComboBox<String> toCombo;
    @FXML private DatePicker departDate;
    @FXML private DatePicker returnDate;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private ComboBox<String> seatCombo;

    @FXML private ToggleGroup classGroup;
    @FXML private RadioButton economyRadio;
    @FXML private RadioButton businessRadio;
    @FXML private RadioButton firstRadio;

    @FXML private CheckBox mealVegetarian;
    @FXML private CheckBox mealVegan;
    @FXML private CheckBox Pesketarian;
    @FXML private CheckBox mealKosher;
    @FXML private CheckBox mealGlutenFree;
    @FXML private CheckBox mealStandard;

    @FXML private CheckBox bevWater;
    @FXML private CheckBox bevSoda;
    @FXML private CheckBox bevJuice;
    @FXML private CheckBox bevCoffee;
    @FXML private CheckBox bevTea;

    @FXML private TableView<FlightRow> resultsTable;
    @FXML private TableColumn<FlightRow, String> colAirline;
    @FXML private TableColumn<FlightRow, String> colFlightNo;
    @FXML private TableColumn<FlightRow, String> colFrom;
    @FXML private TableColumn<FlightRow, String> colTo;
    @FXML private TableColumn<FlightRow, String> colDepart;
    @FXML private TableColumn<FlightRow, String> colArrive;
    @FXML private TableColumn<FlightRow, Double> colPrice;
    @FXML private TableColumn<FlightRow, Integer> colSeats;

    @FXML private Label statusLabel;

    private final ObservableList<FlightRow> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        passengersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        seatCombo.getItems().setAll("Window", "Aisle", "Middle");

        colAirline.setCellValueFactory(new PropertyValueFactory<>("airline"));
        colFlightNo.setCellValueFactory(new PropertyValueFactory<>("flightNo"));
        colFrom.setCellValueFactory(new PropertyValueFactory<>("from"));
        colTo.setCellValueFactory(new PropertyValueFactory<>("to"));
        colDepart.setCellValueFactory(new PropertyValueFactory<>("depart"));
        colArrive.setCellValueFactory(new PropertyValueFactory<>("arrive"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new PropertyValueFactory<>("seats"));

        resultsTable.setItems(data);

        statusLabel.setText("Controller loaded. Loading airports...");
        loadAirportCodes();
    }

    private void loadAirportCodes() {
        fromCombo.getItems().clear();
        toCombo.getItems().clear();

        String sql = """
            SELECT DISTINCT Origin_Code AS Code FROM dbo.Flight_Info
            UNION
            SELECT DISTINCT Destination_Code AS Code FROM dbo.Flight_Info
            ORDER BY Code
        """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String code = rs.getString("Code");
                fromCombo.getItems().add(code);
                toCombo.getItems().add(code);
            }
            statusLabel.setText("Loaded airports from DB.");

        } catch (Exception e) {
            statusLabel.setText("Could not load airports: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String from = fromCombo.getValue();
        String to = toCombo.getValue();

        if (from == null || to == null) {
            statusLabel.setText("Select From and To first.");
            return;
        }

        data.clear();

        String sql = """
            SELECT
                f.Flight_ID,
                f.Flight_Num,
                f.Origin_Code,
                f.Destination_Code,
                f.Departure_Time,
                f.Arrival_Time,
                f.Seats_Remaining
            FROM dbo.Flight_Info f
            WHERE f.Origin_Code = ?
              AND f.Destination_Code = ?
              AND f.Seats_Remaining > 0
            ORDER BY f.Departure_Time
        """;

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, from);
            ps.setString(2, to);

            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String flightId = rs.getString("Flight_ID");
                    String flightNum = rs.getString("Flight_Num");
                    String origin = rs.getString("Origin_Code");
                    String dest = rs.getString("Destination_Code");
                    String depart = String.valueOf(rs.getTimestamp("Departure_Time"));
                    String arrive = String.valueOf(rs.getTimestamp("Arrival_Time"));
                    int seats = rs.getInt("Seats_Remaining");

                    data.add(new FlightRow(
                            flightId,
                            "Unknown",
                            flightNum,
                            origin,
                            dest,
                            depart,
                            arrive,
                            0.0,
                            seats
                    ));
                    count++;
                }
                statusLabel.setText("Found " + count + " flights.");
            }

        } catch (Exception e) {
            statusLabel.setText("Search failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBook() {
        FlightRow selected = resultsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a flight row first.");
            return;
        }

        int qty = passengersSpinner.getValue();
        int passengerId = 2; // TEMP
        String bookingId = "B" + System.currentTimeMillis();

        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);

            // 1) Insert booking
            String insertBookingSql =
                    "INSERT INTO dbo.Booking (Booking_ID, Passenger_ID, Flight_ID, Payment_Status) VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = con.prepareStatement(insertBookingSql)) {
                ps.setString(1, bookingId);
                ps.setInt(2, passengerId);
                ps.setString(3, selected.getFlightId());
                ps.setString(4, "Pending");
                ps.executeUpdate();
            }

            // 2) Reduce seats
            String updateSeatsSql =
                    "UPDATE dbo.Flight_Info SET Seats_Remaining = Seats_Remaining - ? " +
                            "WHERE Flight_ID = ? AND Seats_Remaining >= ?";

            try (PreparedStatement ps = con.prepareStatement(updateSeatsSql)) {
                ps.setInt(1, qty);
                ps.setString(2, selected.getFlightId());
                ps.setInt(3, qty);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    con.rollback();
                    statusLabel.setText("Not enough seats remaining.");
                    return;
                }
            }

            con.commit();

            // Label message
            statusLabel.setText("Booked! Booking_ID: " + bookingId + " (Payment Pending)");

            // Popup confirmation (you can’t miss this)
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking Confirmed");
            alert.setHeaderText("Your flight has been booked!");
            alert.setContentText("Booking ID: " + bookingId + "\nFlight: " + selected.getFlightNo() + "\nPassengers: " + qty);
            alert.showAndWait();

            // Refresh table (shows new seat count)
            handleSearch();

        } catch (Exception e) {
            statusLabel.setText("Booking failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}