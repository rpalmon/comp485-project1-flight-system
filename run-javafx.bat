@echo off
REM Robust launcher: switch to script folder, use lib for module-path and lib\* on classpath
pushd "%~dp0" >nul || (echo Failed to change directory to script location & exit /b 1)

REM Use JAVA_HOME if set, otherwise expect java on PATH
if defined JAVA_HOME (
	set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
	set "JAVACMD=java"
)

REM Module path (relative to project root)
set "MODULEPATH=lib"

REM Classpath: compiled classes and all jars in lib
set "CP=target\classes;lib\*"

echo Running Java at: %JAVACMD%
echo Module path: %MODULEPATH%
echo Classpath: %CP%

"%JAVACMD%" --module-path "%MODULEPATH%" --add-modules=javafx.controls,javafx.fxml -cp "%CP%" com.example.MainApp

popd >nul