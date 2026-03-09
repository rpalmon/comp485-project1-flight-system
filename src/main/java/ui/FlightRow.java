package ui;

public class FlightRow {
    private final String flightId;
    private final String airline;
    private final String flightNo;
    private final String from;
    private final String to;
    private final String depart;
    private final String arrive;
    private final double price;
    private final int seats;

    public FlightRow(String flightId, String airline, String flightNo, String from, String to,
                     String depart, String arrive, double price, int seats) {
        this.flightId = flightId;
        this.airline = airline;
        this.flightNo = flightNo;
        this.from = from;
        this.to = to;
        this.depart = depart;
        this.arrive = arrive;
        this.price = price;
        this.seats = seats;
    }

    public String getFlightId() { return flightId; }
    public String getAirline() { return airline; }
    public String getFlightNo() { return flightNo; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getDepart() { return depart; }
    public String getArrive() { return arrive; }
    public double getPrice() { return price; }
    public int getSeats() { return seats; }
}