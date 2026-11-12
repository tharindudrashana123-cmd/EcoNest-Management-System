@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK 17 or newer is required. Install a JDK and restart Command Prompt.
  pause
  exit /b 1
)
if not exist build mkdir build
javac -encoding UTF-8 -d build src\EcoNestApp.java
if errorlevel 1 (
  echo Compilation failed.
  pause
  exit /b 1
)
java -cp build EcoNestApp
if errorlevel 1 pause
