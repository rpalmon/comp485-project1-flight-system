# comp485-project1-flight-system

# JavaFX Template Project

This is a basic JavaFX template project using Maven. It includes a simple application that displays a window with the message "Hello, JavaFX!".

## How to Run

1. Make sure you have JDK 17 or newer installed.
2. In this folder, run:

    mvn javafx:run

This will build and launch the JavaFX application.

## How to Run with Java.exe

If you want to run the program directly (not using Maven), first copy all JavaFX 17.0.2 jars to the `lib` folder (already done). Then use this command:

    & "C:\Program Files\Java\jdk-23\bin\java.exe" --module-path "D:\comp485-pro1\lib" --add-modules javafx.controls -cp "D:\comp485-pro1\target\classes" com.example.MainApp

Make sure to update the Java path if your JDK is installed elsewhere.

This will launch the JavaFX application with the required runtime components.

## Project Structure

- `src/main/java/com/example/MainApp.java`: Main JavaFX application class
- `pom.xml`: Maven build file with JavaFX dependencies
