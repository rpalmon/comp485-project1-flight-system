@echo off
REM Robust launcher: switch to script folder, use lib for module-path and lib\* on classpath
pushd "%~dp0" >nul || (echo Failed to change directory to script location & exit /b 1)

REM Use JAVA_HOME only when it points to a valid JDK/JRE root.
set "JAVACMD="
if defined JAVA_HOME (
	if exist "%JAVA_HOME%\bin\java.exe" (
		set "JAVACMD=%JAVA_HOME%\bin\java.exe"
	) else (
		echo WARNING: JAVA_HOME is set but invalid: %JAVA_HOME%
		echo          Expected java at: %JAVA_HOME%\bin\java.exe
		echo          Falling back to java on PATH.
	)
)

if not defined JAVACMD (
	set "JAVACMD=java"
)

where "%JAVACMD%" >nul 2>nul || (
	echo Java executable not found.
	echo Fix JAVA_HOME or install Java and add it to PATH.
	popd >nul
	exit /b 1
)

REM Module path (relative to project root)
set "MODULEPATH=lib"

REM Classpath: compiled classes and all jars in lib
set "CP=target\classes;lib\*"

echo Running Java at: %JAVACMD%
echo Module path: %MODULEPATH%
echo Classpath: %CP%

"%JAVACMD%" --module-path "%MODULEPATH%" --add-modules=javafx.controls,javafx.fxml -cp "%CP%" com.example.MainApp
set "EXITCODE=%ERRORLEVEL%"

popd >nul
exit /b %EXITCODE%