@echo off
setlocal EnableDelayedExpansion
rem Ludo Zebra launcher: finds Java 17+ even when an old Java 8 comes first in PATH.
cd /d "%~dp0"

set "JAVA_EXE="
call :check "java"
if defined JAVA_EXE goto run
if defined JAVA_HOME call :check "%JAVA_HOME%\bin\java.exe"
if defined JAVA_EXE goto run
for /d %%D in ("%ProgramFiles%\Java\jdk-*" "%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Eclipse Adoptium\jre-*" "%ProgramFiles%\Microsoft\jdk-*" "%ProgramFiles%\Zulu\zulu-*") do (
  if not defined JAVA_EXE call :check "%%~D\bin\java.exe"
)
if defined JAVA_EXE goto run

echo.
echo Ludo Zebra needs Java 17 or newer, and none was found on this computer.
echo Install JDK 19 (the course version) and run this file again.
pause
exit /b 1

:run
echo Using: !JAVA_EXE!
"!JAVA_EXE!" -jar Ludo-Zebra.jar
if errorlevel 1 pause
exit /b 0

:check
rem Sets JAVA_EXE if %1 runs and reports a major version of 17 or more.
set "VER="
for /f "tokens=3" %%V in ('call "%~1" -version 2^>^&1 ^| findstr /i version') do set "VER=%%~V"
if not defined VER exit /b 0
for /f "delims=." %%M in ("!VER!") do set "MAJOR=%%M"
if "!MAJOR!"=="1" exit /b 0
if !MAJOR! GEQ 17 set "JAVA_EXE=%~1"
exit /b 0
