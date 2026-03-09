@echo off
REM Run the JavaFX app using the local lib folder for JavaFX modules
setlocal enabledelayedexpansion
@echo off
REM Run the JavaFX app using the local lib folder for JavaFX modules
setlocal enabledelayedexpansion

REM Use JAVA_HOME if set, otherwise expect java on PATH
if defined JAVA_HOME (
	set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
	set "JAVACMD=java"
)

REM Project root (this script's folder)
set "PRJ=%~dp0"

REM Use lib folder in project for JavaFX jars
set "LIB=%PRJ%lib"

REM Classpath: compiled classes
set "CLASSPATH=%PRJ%target\classes"

"%JAVACMD%" --module-path "%LIB%" --add-modules=javafx.controls,javafx.fxml -cp "%CLASSPATH%" com.example.MainApp

endlocal