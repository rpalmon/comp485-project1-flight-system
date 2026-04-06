package com.example;
public class Flight {
    private String airlineName;
    private String flightNo;
    private String flightID;
    private String origin;
    private String destination;
    private String departTime;
    private String arriveTime;
    private String price; 
    private String seatsRemain;
    private String planeType;

    public Flight(String airlineName, String flightNo, String origin, String destination, String departTime, String arriveTime, String price, String seatsRemain, String planeType) {
        this.airlineName = airlineName;
        this.flightNo = flightNo;
        this.origin = origin;
        this.destination = destination;
        this.departTime = departTime;
        this.arriveTime = arriveTime;
        this.price = price; 
        this.seatsRemain = seatsRemain;
        this.planeType = planeType;
    }

    // Getters - PropertyValueFactory needs these
    
    public String getAirlineName() {
      return airlineName;
    }
    
    public String getFlightNo() { 
      return flightNo; 
      }
      
    public String getOrigin() {
      return origin; 
    }
    
    public String getDestination() { 
      return destination; 
    }
    
    public String getDepartTime() {
      return departTime;
    }
    
    public String getArriveTime() {
      return arriveTime;
    }
    
    public String getPrice() {
      return price;
    }
    
   public String getSeatsRemain() {
      return seatsRemain;
   }
   
   public String planeType() {
      return planeType;
   }
}