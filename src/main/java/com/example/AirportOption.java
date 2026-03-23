package com.example;

public final class AirportOption {
    private final String id;
    private final String iataCode;
    private final String icaoCode;
    private final String name;
    private final String city;
    private final String country;

    public AirportOption(String id, String iataCode, String icaoCode, String name, String city, String country) {
        this.id = id;
        this.iataCode = safe(iataCode);
        this.icaoCode = safe(icaoCode);
        this.name = safe(name);
        this.city = safe(city);
        this.country = safe(country);
    }

    public String getId() {
        return id;
    }

    public String getDisplayCode() {
        if (!iataCode.isBlank()) {
            return iataCode;
        }
        if (!icaoCode.isBlank()) {
            return icaoCode;
        }
        return city.isBlank() ? "Unknown" : city;
    }

    public String getDisplayName() {
        String location = city.isBlank() ? name : city;
        if (!country.isBlank()) {
            location = location + ", " + country;
        }
        return getDisplayCode() + " - " + location;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
