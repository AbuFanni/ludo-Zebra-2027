@echo off
rem Runs Ludo Zebra. Keeps the window open if Java reports an error.
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java is not installed. Install Java 17 or newer from https://adoptium.net and run again.
  pause
  exit /b 1
)
java -jar "Ludo-Zebra.jar"
if errorlevel 1 (
  echo If the error says "UnsupportedClassVersionError", install Java 17 or newer from https://adoptium.net
  pause
)
