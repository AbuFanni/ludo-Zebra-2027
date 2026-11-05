@echo off
rem Builds Ludo-Zebra.jar on Windows and runs the model tests (same steps as build.sh).
rem Needs JDK 17 or newer. If javac is not on PATH, set JAVA_HOME to the JDK folder.
setlocal
cd /d "%~dp0"

set "BIN="
if defined JAVA_HOME set "BIN=%JAVA_HOME%\bin\"

"%BIN%javac" -version >nul 2>nul
if errorlevel 1 (
  echo javac was not found. Install JDK 17 or newer from https://adoptium.net or set JAVA_HOME.
  exit /b 1
)

if exist build rmdir /s /q build
mkdir build\classes build\test

rem Relative paths on purpose: a full path with Hebrew folder names breaks in cmd.
rem If you add a package, add it to these lists.
"%BIN%javac" --release 17 -encoding UTF-8 -d build\classes src\Model\*.java src\View\*.java src\Controller\*.java || exit /b 1
xcopy /e /i /q src\View\images build\classes\View\images >nul

"%BIN%javac" --release 17 -encoding UTF-8 -cp build\classes -d build\test test\Model\*.java || exit /b 1
"%BIN%java" -cp "build\classes;build\test" Model.ModelTests || exit /b 1

echo Main-Class: Controller.Main> build\manifest.txt
"%BIN%jar" cfm Ludo-Zebra.jar build\manifest.txt -C build\classes . || exit /b 1
echo Built Ludo-Zebra.jar
