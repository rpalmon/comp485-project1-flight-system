package com.example;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AdminFlightBuilderService {
    private final SupabaseRestClient client;

    public AdminFlightBuilderService(String baseUrl, String apiKey) {
        this.client = new SupabaseRestClient(baseUrl, apiKey);
    }

    public String loadRows(
            String table,
            String selectColumns,
            Integer limit,
            String orderBy,
            String direction,
            String filterColumn,
            String filterOperator,
            String filterValue
    ) throws IOException, InterruptedException {
        return client.loadRows(table, selectColumns, limit, orderBy, direction, filterColumn, filterOperator, filterValue);
    }

    public String insert(String table, String payloadJson) throws IOException, InterruptedException {
        return client.insert(table, payloadJson);
    }

    public String update(String table, String matchColumn, String matchValue, String payloadJson) throws IOException, InterruptedException {
        return client.update(table, matchColumn, matchValue, payloadJson);
    }

    public String delete(String table, String matchColumn, String matchValue) throws IOException, InterruptedException {
        return client.delete(table, matchColumn, matchValue);
    }

    public ReferenceCatalog loadReferenceCatalog() throws IOException, InterruptedException {
        List<LookupOption> airlines = loadAirlines();
        List<LookupOption> airports = loadAirports();
        List<AircraftModelSpec> aircraftModels = loadAircraftModels();

        Map<String, AircraftModelSpec> specsById = new LinkedHashMap<>();
        List<LookupOption> modelOptions = new ArrayList<>();
        for (AircraftModelSpec spec : aircraftModels) {
            specsById.put(spec.id(), spec);
            modelOptions.add(spec.toLookupOption());
        }

        return new ReferenceCatalog(airlines, airports, modelOptions, specsById);
    }

    public LookupOption createAirline(AirlineDraft draft) throws IOException, InterruptedException {
        String response = client.insert("airlines", """
                [{
                  "code": %s,
                  "name": %s,
                  "country": %s
                }]
                """.formatted(
                quoteOrNull(draft.code()),
                quote(draft.name()),
                quoteOrNull(draft.country())
        ));

        Map<String, Object> row = firstRow(response);
        return new LookupOption(
                SimpleJson.getString(row, "id"),
                airlineLabel(SimpleJson.getString(row, "code"), SimpleJson.getString(row, "name")),
                String.join(" ",
                        SimpleJson.getString(row, "code"),
                        SimpleJson.getString(row, "name"),
                        SimpleJson.getString(row, "country")
                )
        );
    }

    public LookupOption createAirport(AirportDraft draft) throws IOException, InterruptedException {
        String response = client.insert("airports", """
                [{
                  "iata_code": %s,
                  "icao_code": %s,
                  "name": %s,
                  "city": %s,
                  "country": %s,
                  "timezone": %s
                }]
                """.formatted(
                quoteOrNull(draft.iataCode()),
                quoteOrNull(draft.icaoCode()),
                quote(draft.name()),
                quote(draft.city()),
                quote(draft.country()),
                quoteOrNull(draft.timezone())
        ));

        Map<String, Object> row = firstRow(response);
        return new LookupOption(
                SimpleJson.getString(row, "id"),
                airportLabel(
                        SimpleJson.getString(row, "iata_code"),
                        SimpleJson.getString(row, "icao_code"),
                        SimpleJson.getString(row, "city"),
                        SimpleJson.getString(row, "country")
                ),
                String.join(" ",
                        SimpleJson.getString(row, "iata_code"),
                        SimpleJson.getString(row, "icao_code"),
                        SimpleJson.getString(row, "name"),
                        SimpleJson.getString(row, "city"),
                        SimpleJson.getString(row, "country")
                )
        );
    }

    public AircraftModelSpec createAircraftModel(AircraftModelDraft draft) throws IOException, InterruptedException {
        int totalSeats = draft.seatsTotal() > 0
                ? draft.seatsTotal()
                : draft.seatsEconomy() + draft.seatsBusiness() + draft.seatsFirst();

        String response = client.insert("aircraft_models", """
                [{
                  "model_code": %s,
                  "manufacturer": %s,
                  "seats_total": %d,
                  "seats_economy": %d,
                  "seats_business": %d,
                  "seats_first": %d
                }]
                """.formatted(
                quote(draft.modelCode()),
                quoteOrNull(draft.manufacturer()),
                totalSeats,
                draft.seatsEconomy(),
                draft.seatsBusiness(),
                draft.seatsFirst()
        ));

        Map<String, Object> row = firstRow(response);
        return new AircraftModelSpec(
                SimpleJson.getString(row, "id"),
                SimpleJson.getString(row, "model_code"),
                SimpleJson.getString(row, "manufacturer"),
                SimpleJson.getInt(row, "seats_total", totalSeats),
                SimpleJson.getInt(row, "seats_economy", draft.seatsEconomy()),
                SimpleJson.getInt(row, "seats_business", draft.seatsBusiness()),
                SimpleJson.getInt(row, "seats_first", draft.seatsFirst())
        );
    }

    public FlightCreationResult createFlight(FlightCreationRequest request, AircraftModelSpec aircraftModel) throws IOException, InterruptedException {
        String response = String.format(Locale.US, """
                [{
                  "airline_id": %s,
                  "flight_number": %s,
                  "origin_airport_id": %s,
                  "destination_airport_id": %s,
                  "departure_at": %s,
                  "arrival_at": %s,
                  "aircraft_model_id": %s,
                  "total_seats": %d,
                  "available_seats": %d,
                  "base_price": %.2f
                }]
                """,
                quote(request.airlineId()),
                quote(request.flightNumber()),
                quote(request.originAirportId()),
                quote(request.destinationAirportId()),
                quote(request.departureAt().toString()),
                quote(request.arrivalAt().toString()),
                quoteOrNull(request.aircraftModelId()),
                request.totalSeats(),
                request.availableSeats(),
                request.basePrice()
        );
        response = client.insert("flights", response);

        Map<String, Object> createdFlight = firstRow(response);
        String flightId = SimpleJson.getString(createdFlight, "id");
        int classRowsCreated = 0;
        int seatRowsCreated = 0;

        if (request.createClassRows()) {
            classRowsCreated = aircraftModel == null
                    ? createDefaultEconomyClass(flightId, request.totalSeats(), request.availableSeats())
                    : createFlightClasses(flightId, request.availableSeats(), aircraftModel);
        }

        if (request.createSeatMap() && aircraftModel != null) {
            seatRowsCreated = createSeatMap(flightId, aircraftModel);
        }

        return new FlightCreationResult(flightId, classRowsCreated, seatRowsCreated);
    }

    private int createFlightClasses(String flightId, int availableSeats, AircraftModelSpec aircraftModel) throws IOException, InterruptedException {
        List<ClassSpec> classes = List.of(
                new ClassSpec("first", aircraftModel.seatsFirst(), 2.4),
                new ClassSpec("business", aircraftModel.seatsBusiness(), 1.6),
                new ClassSpec("economy", aircraftModel.seatsEconomy(), 1.0)
        );

        List<String> payloadRows = new ArrayList<>();
        for (ClassSpec classSpec : classes) {
            if (classSpec.seatsTotal() <= 0) {
                continue;
            }
            int classAvailable = Math.min(classSpec.seatsTotal(), availableSeats);
            availableSeats -= classAvailable;
            payloadRows.add(String.format(Locale.US, """
                    {
                      "flight_id": %s,
                      "class_name": %s,
                      "seats_total": %d,
                      "seats_available": %d,
                      "price_modifier": %.2f
                    }
                    """,
                    quote(flightId),
                    quote(classSpec.className()),
                    classSpec.seatsTotal(),
                    classAvailable,
                    classSpec.priceModifier()
            ));
        }

        if (payloadRows.isEmpty()) {
            return 0;
        }

        client.insert("flight_classes", "[\n" + String.join(",\n", payloadRows) + "\n]");
        return payloadRows.size();
    }

    private int createDefaultEconomyClass(String flightId, int totalSeats, int availableSeats) throws IOException, InterruptedException {
        client.insert("flight_classes", """
                [{
                  "flight_id": %s,
                  "class_name": "economy",
                  "seats_total": %d,
                  "seats_available": %d,
                  "price_modifier": 1.0
                }]
                """.formatted(
                quote(flightId),
                totalSeats,
                availableSeats
        ));
        return 1;
    }

    private int createSeatMap(String flightId, AircraftModelSpec aircraftModel) throws IOException, InterruptedException {
        List<String> payloadRows = new ArrayList<>();
        int rowNumber = 1;
        rowNumber = appendSeatRows(payloadRows, flightId, "first", aircraftModel.seatsFirst(), rowNumber);
        rowNumber = appendSeatRows(payloadRows, flightId, "business", aircraftModel.seatsBusiness(), rowNumber);
        appendSeatRows(payloadRows, flightId, "economy", aircraftModel.seatsEconomy(), rowNumber);

        if (payloadRows.isEmpty()) {
            return 0;
        }

        client.insert("seat_map", "[\n" + String.join(",\n", payloadRows) + "\n]");
        return payloadRows.size();
    }

    private int appendSeatRows(List<String> payloadRows, String flightId, String className, int seatCount, int startingRow) {
        if (seatCount <= 0) {
            return startingRow;
        }

        String[] seatLetters = {"A", "B", "C", "D", "E", "F"};
        int rowNumber = startingRow;
        int seatsPlaced = 0;

        while (seatsPlaced < seatCount) {
            for (String letter : seatLetters) {
                if (seatsPlaced >= seatCount) {
                    break;
                }

                boolean window = "A".equals(letter) || "F".equals(letter);
                boolean aisle = "C".equals(letter) || "D".equals(letter);
                boolean middle = "B".equals(letter) || "E".equals(letter);
                payloadRows.add("""
                        {
                          "flight_id": %s,
                          "seat_label": %s,
                          "class_name": %s,
                          "is_window": %s,
                          "is_aisle": %s,
                          "is_middle": %s,
                          "is_available": true
                        }
                        """.formatted(
                        quote(flightId),
                        quote(rowNumber + letter),
                        quote(className),
                        window,
                        aisle,
                        middle
                ));
                seatsPlaced++;
            }
            rowNumber++;
        }

        return rowNumber;
    }

    private List<LookupOption> loadAirlines() throws IOException, InterruptedException {
        String response = client.loadRows("airlines", "id,code,name,country", 500, "name", "asc", List.of());
        List<LookupOption> options = new ArrayList<>();

        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(response))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String code = SimpleJson.getString(row, "code");
            String name = SimpleJson.getString(row, "name");
            options.add(new LookupOption(
                    SimpleJson.getString(row, "id"),
                    airlineLabel(code, name),
                    String.join(" ", code, name, SimpleJson.getString(row, "country"))
            ));
        }

        options.sort(Comparator.comparing(LookupOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private List<LookupOption> loadAirports() throws IOException, InterruptedException {
        String response = client.loadRows("airports", "id,iata_code,icao_code,name,city,country", 1000, "city", "asc", List.of());
        List<LookupOption> options = new ArrayList<>();

        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(response))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            String iata = SimpleJson.getString(row, "iata_code");
            String icao = SimpleJson.getString(row, "icao_code");
            String name = SimpleJson.getString(row, "name");
            String city = SimpleJson.getString(row, "city");
            String country = SimpleJson.getString(row, "country");
            options.add(new LookupOption(
                    SimpleJson.getString(row, "id"),
                    airportLabel(iata, icao, city.isBlank() ? name : city, country),
                    String.join(" ", iata, icao, name, city, country)
            ));
        }

        options.sort(Comparator.comparing(LookupOption::getLabel, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private List<AircraftModelSpec> loadAircraftModels() throws IOException, InterruptedException {
        String response = client.loadRows(
                "aircraft_models",
                "id,model_code,manufacturer,seats_total,seats_economy,seats_business,seats_first",
                500,
                "model_code",
                "asc",
                List.of()
        );

        List<AircraftModelSpec> specs = new ArrayList<>();
        for (Object rowObject : SimpleJson.asArray(SimpleJson.parse(response))) {
            Map<String, Object> row = SimpleJson.asObject(rowObject);
            specs.add(new AircraftModelSpec(
                    SimpleJson.getString(row, "id"),
                    SimpleJson.getString(row, "model_code"),
                    SimpleJson.getString(row, "manufacturer"),
                    SimpleJson.getInt(row, "seats_total", 0),
                    SimpleJson.getInt(row, "seats_economy", 0),
                    SimpleJson.getInt(row, "seats_business", 0),
                    SimpleJson.getInt(row, "seats_first", 0)
            ));
        }

        specs.sort(Comparator.comparing(AircraftModelSpec::label, String.CASE_INSENSITIVE_ORDER));
        return specs;
    }

    private static Map<String, Object> firstRow(String response) {
        List<Object> rows = SimpleJson.asArray(SimpleJson.parse(response));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Supabase returned no rows.");
        }
        return SimpleJson.asObject(rows.get(0));
    }

    private static String airlineLabel(String code, String name) {
        return code == null || code.isBlank() ? name : name + " (" + code + ")";
    }

    private static String airportLabel(String iata, String icao, String city, String country) {
        String code = iata == null || iata.isBlank() ? icao : iata;
        if (code == null || code.isBlank()) {
            code = city;
        }
        return country == null || country.isBlank()
                ? code + " - " + city
                : code + " - " + city + ", " + country;
    }

    private static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private static String quoteOrNull(String value) {
        return value == null || value.isBlank() ? "null" : quote(value);
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record ClassSpec(String className, int seatsTotal, double priceModifier) {
    }

    public record ReferenceCatalog(
            List<LookupOption> airlines,
            List<LookupOption> airports,
            List<LookupOption> aircraftModels,
            Map<String, AircraftModelSpec> aircraftSpecsById
    ) {
    }

    public record AirlineDraft(String code, String name, String country) {
    }

    public record AirportDraft(String iataCode, String icaoCode, String name, String city, String country, String timezone) {
    }

    public record AircraftModelDraft(String modelCode, String manufacturer, int seatsTotal, int seatsEconomy, int seatsBusiness, int seatsFirst) {
    }

    public record FlightCreationRequest(
            String airlineId,
            String flightNumber,
            String originAirportId,
            String destinationAirportId,
            OffsetDateTime departureAt,
            OffsetDateTime arrivalAt,
            String aircraftModelId,
            int totalSeats,
            int availableSeats,
            double basePrice,
            boolean createClassRows,
            boolean createSeatMap
    ) {
    }

    public record FlightCreationResult(String flightId, int classRowsCreated, int seatRowsCreated) {
    }

    public record AircraftModelSpec(
            String id,
            String modelCode,
            String manufacturer,
            int seatsTotal,
            int seatsEconomy,
            int seatsBusiness,
            int seatsFirst
    ) {
        public LookupOption toLookupOption() {
            return new LookupOption(id, label(), String.join(" ", modelCode, manufacturer));
        }

        public String label() {
            String manufacturerText = manufacturer == null || manufacturer.isBlank() ? "" : manufacturer + " ";
            return manufacturerText + modelCode + " (" + seatsTotal + " seats)";
        }
    }
}
