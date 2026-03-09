# comp485-project1-flight-system

# JavaFX Template Project

This is a basic JavaFX template project using Maven. It includes a simple application that displays a window with the message "Hello, JavaFX!".

## How to Run

Prerequisites
- JDK 17 or newer (project uses Java 21 in `pom.xml`).

Options

- Run with the bundled launcher (Windows):

```powershell
.\run-javafx.bat
```

- Run directly with `java` (explicit module-path):

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' --module-path 'E:\comp485-project1-flight-system\lib' --add-modules=javafx.controls,javafx.fxml -cp 'target\classes;lib/*' com.example.MainApp
```

- Run with Maven (requires Maven installed):

```bash
mvn clean javafx:run -DskipTests
```

- Run from VS Code: open the Run view, select `Launch JavaFX MainApp`, then Start Debugging (F5). The workspace launch config points `vmArgs` at the project's `lib` folder.

Notes
- The `lib/` folder already contains JavaFX 17 jars; keep `--module-path` pointed to that folder when running without Maven.
- If you don't have Maven installed, consider adding the Maven Wrapper (`mvnw`) or use the `run-javafx.bat` launcher.

## Project Structure

- `src/main/java/com/example/MainApp.java`: Main JavaFX application class
- `pom.xml`: Maven build file with JavaFX dependencies
