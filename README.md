# comp485-project1-flight-system

This repository currently contains:

- A basic JavaFX shell app in `src/main/java/com/example/MainApp.java`
- An FXML layout in `src/main/resources/scenes/MainScene.fxml`
- A JavaFX admin console in `src/main/resources/scenes/AdminScene.fxml`
- A static Supabase admin page in `web/admin.html`
- A sample Supabase schema and demo policies in `supabase/flights_setup.sql`

## JavaFX App

Prerequisites:

- JDK 17 or newer
- Maven if you want to run through Maven instead of the bundled batch file

Run options:

```powershell
.\run-javafx.bat
```

```powershell
mvn clean javafx:run -DskipTests
```

If you run directly with `java`, keep the `lib/` folder on the module path because it contains the JavaFX runtime jars bundled with the project.

The main JavaFX screen now includes an `Admin` button in the bottom-right corner.
That button:

- Prompts for a username and password
- Validates those values against `ADMIN_USERNAME` and `ADMIN_PASSWORD` in `.env`
- Opens a separate admin window for Supabase CRUD operations

The main search screen also now loads live reference data from Supabase:

- Airports populate the origin and destination dropdowns
- Flight classes populate from `flight_classes`
- Search results come from `flights`, `flight_classes`, and `seat_map`
- Passenger count, seat preference, and class affect which flights are shown and how pricing is calculated

The admin window loads `SUPABASE_URL` and `SUPABASE_ANON_KEY` from `.env` by default and lets you adjust them at runtime for the current session.

Before launching the app, change the demo login in `.env`:

```text
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-me
```

Inside the JavaFX admin window you can:

- Use an assisted flight builder to create flights without hand-writing UUID JSON
- Search existing airlines, airports, and aircraft models from Supabase in editable dropdowns
- Type a missing airline, airport, or aircraft model and get a guided popup to create it
- Auto-create matching `flight_classes` rows and a `seat_map` when you create a flight
- Load rows from the available Supabase tables
- Apply a basic filter
- Load table-specific insert and update templates
- Insert JSON rows
- Update rows that match a column/value pair
- Delete rows that match a column/value pair

The table list is preloaded with:

- `aircraft_models`
- `airlines`
- `airports`
- `bookings`
- `change_history`
- `flight_classes`
- `flights`
- `passengers`
- `payments`
- `profiles`
- `seat_map`

Because your Supabase dashboard showed these tables as `UNRESTRICTED`, the JavaFX admin console should be able to use the anon key for reads and writes.
The templates are based on your current Supabase schema, including UUID foreign key placeholders and JSON/array examples for tables like `payments`, `change_history`, and `passengers`.
The guided flight builder sits above the raw CRUD tools, so normal flight setup can stay form-based while the generic JSON editor is still available for edge cases.

## Static Admin Page

Open `web/admin.html` in a browser if you still want a simple standalone CRUD page.

## Demo Flights Table

If you want a concrete table for the admin page, run the SQL in `supabase/flights_setup.sql` inside the Supabase SQL Editor.

That script creates a `public.flights` table with sample rows and demo anon policies for:

- `SELECT`
- `INSERT`
- `UPDATE`
- `DELETE`

Those policies are intentionally open so the static admin page works without authentication. Tighten them before using this pattern outside a local demo or class project.
