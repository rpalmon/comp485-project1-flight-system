package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import java.sql.*;
import javafx.event.ActionEvent;
import javafx.scene.control.SpinnerValueFactory;

public class MainController {

    @FXML private ComboBox<String> fromCombo;
    @FXML private ComboBox<String> toCombo;
    @FXML private DatePicker departDate;
    @FXML private DatePicker returnDate;
    @FXML private Spinner<Integer> passengersSpinner;
    @FXML private ComboBox<String> seatCombo;
    
    
    @FXML private RadioButton economyRadio;
    @FXML private RadioButton businessRadio;
    @FXML private RadioButton firstRadio;
    
    @FXML private CheckBox mealVegetarian;
    @FXML private CheckBox mealVegan;
    @FXML private CheckBox mealPesketarian;
    @FXML private CheckBox mealKosher;
    @FXML private CheckBox mealGlutenFree;
    @FXML private CheckBox mealStandard;
    
    @FXML private CheckBox bevWater;
    @FXML private CheckBox bevSoda;
    @FXML private CheckBox bevJuice;
    @FXML private CheckBox bevCoffee;
    @FXML private CheckBox bevTea;
    
    @FXML private TableView<Flight> resultsTable;
    @FXML private TableColumn<Flight, String> colAirline;
    @FXML private TableColumn<Flight, String> colFlightNo;
    @FXML private TableColumn<Flight, String> colFrom;
    @FXML private TableColumn<Flight, String> colTo;
    @FXML private TableColumn<Flight, String> colDepart;
    @FXML private TableColumn<Flight, String> colArrive;
    @FXML private TableColumn<Flight, String> colPrice;
    @FXML private TableColumn<Flight, String> colSeats;
    
    @FXML private Label statusLabel;
    
        String url = "jdbc:sqlserver://localhost;databaseName=Flight_Sys;encrypt=true;trustServerCertificate=true";
        String user = "sa";
        String password = "60627958";
    
    
    @FXML
    public void initialize() {
        colAirline.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("airlineName"));
        colFlightNo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("flightNo"));
        colFrom.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("origin"));
        colTo.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("destination"));
        colDepart.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("departTime"));
        colArrive.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("arriveTime"));
        colPrice.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("price"));
        colSeats.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("seatsRemain"));
        passengersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        
        ObservableList<String> origins = FXCollections.observableArrayList();
        ObservableList<String> destinations = FXCollections.observableArrayList();
      
         try {
         Connection con = DriverManager.getConnection(url, user, password);
         Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT DISTINCT Origin_Code FROM dbo.Flight_Info");
         
         while(rs.next()) {
            origins.add(rs.getString("Origin_Code"));
         }
         
        rs = stmt.executeQuery("SELECT DISTINCT Destination_Code FROM dbo.Flight_Info");
         
         while(rs.next()) {
         destinations.add(rs.getString("Destination_Code"));
         }
    } catch (SQLException e) {
      e.printStackTrace();
    }
      fromCombo.setItems(origins);
      toCombo.setItems(destinations);
      seatCombo.setItems(FXCollections.observableArrayList("Window", "Aisle", "Middle"));
    }
   
   
    @FXML
    private void handleSearch() {
      statusLabel.setText("Searching...");
      ObservableList<Flight> results = FXCollections.observableArrayList();
      
      StringBuilder query = new StringBuilder("SELECT * FROM dbo.Flight_Info WHERE 1=1");
      
      String from = fromCombo.getValue();
      String to = toCombo.getValue();
      String seat = seatCombo.getValue();
      
      if (fromCombo.getValue() == null || toCombo.getValue() == null || departDate.getValue() == null) {
        if(!economyRadio.isSelected() && !businessRadio.isSelected() && !firstRadio.isSelected()) {
        statusLabel.setText("Please select the required filters.");
        resultsTable.setItems(FXCollections.observableArrayList());
        return;
        }
    } 
      
         if (from != null && !from.isEmpty()) {
        query.append(" AND Origin_Code = '").append(from).append("'");
        }
         if (to != null && !to.isEmpty()) {
        query.append(" AND Destination_Code = '").append(to).append("'");
        }
        if (departDate.getValue() != null) {
        query.append(" AND CAST(Departure_Time AS DATE) = '").append(departDate.getValue()).append("'");
        } 
        query.append(" AND CAST(Seats_Remaining AS INT) >= ").append(passengersSpinner.getValue());
      
         try { 
         Connection con = DriverManager.getConnection(url, user, password);
         
         System.out.println("Sucessfully connected.");
         Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(query.toString());
         
         
         while (rs.next()) {
                results.add(new Flight(
                    rs.getString("Airline_Name"),
                    rs.getString("Flight_Num"),
                    rs.getString("Origin_Code"),
                    rs.getString("Destination_Code"),
                    rs.getString("Departure_Time"),
                    rs.getString("Arrival_Time"),
                    rs.getString("Price"),
                    rs.getString("Seats_Remaining"),
                    rs.getString("Plane_Type")
                ));
            }
            resultsTable.setItems(results);
            statusLabel.setText("Found " + results.size() + " flights");
         
        } catch(SQLException ex) {
         System.out.println("Database error.");
         ex.printStackTrace();
         
        }  
   
    }
    @FXML
    private void handleBook() {
      Flight selected = resultsTable.getSelectionModel().getSelectedItem();
      if(selected == null) {
         statusLabel.setText("Please select a flight first"); 
         return;
      }
      statusLabel.setText("Booked Flight " + selected.getFlightNo());
    } 

}