package com.example;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class MainDataService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final SupabaseRestClient client;

    public MainDataService(EnvConfig envConfig) {
        this.client = new SupabaseRestClient(
                envConfig.get("SUPABASE_URL"),
                envConfig.getOrDefault("SUPABASE_ANON_KEY", envConfig.get("SUPABASE_PUBLISHABLE_KEY"))
        );
    }

    public ReferenceData loadReferenceData() throws IOException, InterruptedException {
        List<AirportOption> airports = loadAirports();
        Map<String, String> airlineNamesById = loadAirlines();
        List<String> classNames = loadClassNames();
        return new ReferenceData(
                airports,
                airports.stream().collect(Collectors.toMap(AirportOption::getId, airport -> airport)),
                airlineNamesById,
                classNames
        );
    }

    public SearchResponse searchFlights(SearchCriteria criteria, ReferenceData referenceData) throws IOException, InterruptedException {
        List<FlightRecord> outboundFlights = loadFlights(criteria.originAirportId(), criteria.destinationAirportId(), criteria.departDate());

        List<FlightRecord> returnFlights = List.of();
        String note = "";
        if (criteria.returnDate() != null) {
            if (criteria.originAirportId() != null && criteria.destinationAirportId() != null) {
                returnFlights = loadFlights(criteria.destinationAirportId(), criteria.originAirportId(), criteria.returnDate());
            } else {
                note = "Return date was provided without both origin and destination, so only outbound flights were loaded.";
            }
        }

        Map<String, FlightClassInfo> classInfoByFlightId = loadClassInfo(criteria.className());
        boolean seatFilterApplied = criteria.seatPreference() != null
                && !criteria.seatPreference().isBlank()
                && !"Any".equalsIgnoreCase(criteria.seatPreference());
        Set<String> seatEligibleFlightIds = loadSeatEligibleFlightIds(criteria.className(), criteria.seatPreference());

        List<FlightSearchResult> results = new ArrayList<>();
        results.addAll(buildResults(outboundFlights, "Outbound", criteria, referenceData, classInfoByFlightId, seatEligibleFlightIds, seatFilterApplied));
        results.addAll(buildResults(returnFlights, "Return", criteria, referenceData, classInfoByFlightId, seatEligibleFlightIds, seatFilterApplied));

        results.sort(Comparator.comparing(FlightSearchResult::getDepartureSortKey));
        return new SearchResponse(results, outboundFlights.size(), returnFlights.size(), note);
    }

    private List<FlightSearchResult> buildResults(
            Collection<FlightRecord> flights,
            String segment,
            SearchCriteria criteria,
            ReferenceData referenceData,
            Map<String, FlightClassInfo> classInfoByFlightId,
            Set<String> seatEligibleFlightIds,
            boolean seatFilterApplied
    ) {
        List<FlightSearchResult> results = new ArrayList<>();

        for (FlightRecord flight : flights) {
            FlightClassInfo classInfo = classInfoByFlightId.get(flight.id());
            if (classInfo == null) {
                continue;
            }

            if (seatFilterApplied && !seatEligibleFlightIds.contains(flight.id())) {
                continue;
            }

            if (classInfo.seatsAvailable() < criteria.passengerCount()) {
                continue;
            }

            AirportOption origin = referenceData.airportsById().get(flight.originAirportId());
            AirportOption destination = referenceData.airportsById().get(flight.destinationAirportId());
            String airlineName = referenceData.airlineNamesById().get(flight.airlineId());

            double totalPrice = flight.basePrice() * classInfo.priceModifier() * criteria.passengerCount();
            results.add(new FlightSearchResult(
                    flight.id(),
                    segment,
                    criteria.className(),
                    flight.departureAt().toString(),
                    airlineName == null || airlineName.isBlank() ? "Unknown Airline" : airlineName,
                    flight.flightNumber(),
                    origin == null ? flight.originAirportId() : origin.getDisplayCode(),
                    destination == null ? flight.destinationAirportId() : destination.getDisplayCode(),
                    formatTimestamp(flight.departureAt()),
                    formatTimestamp(flight.arrivalAt()),
                    String.format(Locale.US, "$%,.2f", totalPrice),
                    String.valueOf(classInfo.seatsAvailable())
            ));
        }

        return results;
    }

    private List<AirportOption> loadAirports() throws IOException, InterruptedException {
        String json = client.loadRows(
                "airports",
                "id,iata_code,icao_code,name,city,country",
                1000,
                "city",
                "asc",
                List.of()
        );

        List<AirportOption> airports = new ArrayList<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            airports.add(new AirportOption(
                    SimpleJson.getString(row, "id"),
                    SimpleJson.getString(row, "iata_code"),
                    SimpleJson.getString(row, "icao_code"),
                    SimpleJson.getString(row, "name"),
                    SimpleJson.getString(row, "city"),
                    SimpleJson.getString(row, "country")
            ));
        }

        airports.sort(Comparator.comparing(AirportOption::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return airports;
    }

    private Map<String, String> loadAirlines() throws IOException, InterruptedException {
        String json = client.loadRows(
                "airlines",
                "id,code,name",
                500,
                "name",
                "asc",
                List.of()
        );

        Map<String, String> airlines = new HashMap<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String id = SimpleJson.getString(row, "id");
            String code = SimpleJson.getString(row, "code");
            String name = SimpleJson.getString(row, "name");
            if (!id.isBlank()) {
                airlines.put(id, code.isBlank() ? name : name + " (" + code + ")");
            }
        }

        return airlines;
    }

    private List<String> loadClassNames() throws IOException, InterruptedException {
        String json = client.loadRows(
                "flight_classes",
                "class_name",
                1000,
                "class_name",
                "asc",
                List.of()
        );

        Set<String> classNames = new LinkedHashSet<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String className = SimpleJson.getString(row, "class_name");
            if (!className.isBlank()) {
                classNames.add(className);
            }
        }

        List<String> sorted = new ArrayList<>(classNames);
        sorted.sort(Comparator.comparingInt(MainDataService::classRank).thenComparing(String::compareToIgnoreCase));
        if (sorted.isEmpty()) {
            sorted.addAll(List.of("economy", "business", "first"));
        }
        return sorted;
    }

    private List<FlightRecord> loadFlights(String originAirportId, String destinationAirportId, LocalDate date) throws IOException, InterruptedException {
        List<SupabaseRestClient.QueryFilter> filters = new ArrayList<>();
        if (originAirportId != null && !originAirportId.isBlank()) {
            filters.add(new SupabaseRestClient.QueryFilter("origin_airport_id", "eq", originAirportId));
        }
        if (destinationAirportId != null && !destinationAirportId.isBlank()) {
            filters.add(new SupabaseRestClient.QueryFilter("destination_airport_id", "eq", destinationAirportId));
        }
        if (date != null) {
            OffsetDateTime start = date.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            OffsetDateTime end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
            filters.add(new SupabaseRestClient.QueryFilter("departure_at", "gte", start.toString()));
            filters.add(new SupabaseRestClient.QueryFilter("departure_at", "lt", end.toString()));
        }

        String json = client.loadRows(
                "flights",
                "id,airline_id,flight_number,origin_airport_id,destination_airport_id,departure_at,arrival_at,base_price,available_seats",
                250,
                "departure_at",
                "asc",
                filters
        );

        List<FlightRecord> flights = new ArrayList<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            flights.add(new FlightRecord(
                    SimpleJson.getString(row, "id"),
                    SimpleJson.getString(row, "airline_id"),
                    SimpleJson.getString(row, "flight_number"),
                    SimpleJson.getString(row, "origin_airport_id"),
                    SimpleJson.getString(row, "destination_airport_id"),
                    parseTimestamp(SimpleJson.getString(row, "departure_at")),
                    parseTimestamp(SimpleJson.getString(row, "arrival_at")),
                    SimpleJson.getDouble(row, "base_price", 0.0),
                    SimpleJson.getInt(row, "available_seats", 0)
            ));
        }

        return flights;
    }

    private Map<String, FlightClassInfo> loadClassInfo(String className) throws IOException, InterruptedException {
        String json = client.loadRows(
                "flight_classes",
                "flight_id,class_name,seats_available,price_modifier",
                1000,
                "created_at",
                "desc",
                List.of(new SupabaseRestClient.QueryFilter("class_name", "eq", className))
        );

        Map<String, FlightClassInfo> classInfoByFlightId = new HashMap<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String flightId = SimpleJson.getString(row, "flight_id");
            if (flightId.isBlank()) {
                continue;
            }
            classInfoByFlightId.put(flightId, new FlightClassInfo(
                    flightId,
                    SimpleJson.getString(row, "class_name"),
                    SimpleJson.getInt(row, "seats_available", 0),
                    SimpleJson.getDouble(row, "price_modifier", 1.0)
            ));
        }

        return classInfoByFlightId;
    }

    private Set<String> loadSeatEligibleFlightIds(String className, String seatPreference) throws IOException, InterruptedException {
        if (seatPreference == null || seatPreference.isBlank() || "Any".equalsIgnoreCase(seatPreference)) {
            return Set.of();
        }

        String seatColumn = switch (seatPreference.toLowerCase(Locale.ROOT)) {
            case "window" -> "is_window";
            case "aisle" -> "is_aisle";
            case "middle" -> "is_middle";
            default -> "";
        };

        if (seatColumn.isBlank()) {
            return Set.of();
        }

        String json = client.loadRows(
                "seat_map",
                "flight_id",
                1000,
                "created_at",
                "desc",
                List.of(
                        new SupabaseRestClient.QueryFilter("class_name", "eq", className),
                        new SupabaseRestClient.QueryFilter("is_available", "eq", "true"),
                        new SupabaseRestClient.QueryFilter(seatColumn, "eq", "true")
                )
        );

        Set<String> flightIds = new LinkedHashSet<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(json))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String flightId = SimpleJson.getString(row, "flight_id");
            if (!flightId.isBlank()) {
                flightIds.add(flightId);
            }
        }

        return flightIds;
    }

    private static OffsetDateTime parseTimestamp(String value) {
        return OffsetDateTime.parse(value);
    }

    private static String formatTimestamp(OffsetDateTime timestamp) {
        return timestamp.atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY_TIME);
    }

    private static int classRank(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "economy" -> 0;
            case "business" -> 1;
            case "first" -> 2;
            default -> 10;
        };
    }

    private record FlightRecord(
            String id,
            String airlineId,
            String flightNumber,
            String originAirportId,
            String destinationAirportId,
            OffsetDateTime departureAt,
            OffsetDateTime arrivalAt,
            double basePrice,
            int availableSeats
    ) {
    }

    private record FlightClassInfo(
            String flightId,
            String className,
            int seatsAvailable,
            double priceModifier
    ) {
    }

    public record ReferenceData(
            List<AirportOption> airports,
            Map<String, AirportOption> airportsById,
            Map<String, String> airlineNamesById,
            List<String> classNames
    ) {
    }

    public record SearchCriteria(
            String originAirportId,
            String destinationAirportId,
            LocalDate departDate,
            LocalDate returnDate,
            String className,
            String seatPreference,
            int passengerCount
    ) {
    }

    public record SearchResponse(
            List<FlightSearchResult> results,
            int outboundFetched,
            int returnFetched,
            String note
    ) {
    }
}
