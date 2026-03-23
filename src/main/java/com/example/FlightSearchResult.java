package com.example;

public final class FlightSearchResult {
    private final String flightId;
    private final String segment;
    private final String className;
    private final String departureSortKey;
    private final String airline;
    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final String depart;
    private final String arrive;
    private final String price;
    private final String seats;

    public FlightSearchResult(
            String flightId,
            String segment,
            String className,
            String departureSortKey,
            String airline,
            String flightNumber,
            String origin,
            String destination,
            String depart,
            String arrive,
            String price,
            String seats
    ) {
        this.flightId = flightId;
        this.segment = segment;
        this.className = className;
        this.departureSortKey = departureSortKey;
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.depart = depart;
        this.arrive = arrive;
        this.price = price;
        this.seats = seats;
    }

    public String getFlightId() {
        return flightId;
    }

    public String getSegment() {
        return segment;
    }

    public String getClassName() {
        return className;
    }

    public String getDepartureSortKey() {
        return departureSortKey;
    }

    public String getAirline() {
        return airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getDepart() {
        return depart;
    }

    public String getArrive() {
        return arrive;
    }

    public String getPrice() {
        return price;
    }

    public String getSeats() {
        return seats;
    }
}
