@echo off
chcp 65001 >nul

echo Java Multithreaded Web Crawler - Build ^& Run Script
echo ======================================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 21 or higher and try again
    pause
    exit /b 1
)

REM Check Java version (simplified check for Windows)
echo Java version: 
java -version 2>&1 | findstr "version"

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6+ and try again
    pause
    exit /b 1
)

echo Maven version:
mvn -version | findstr "Apache Maven"
echo.

echo Building project and downloading dependencies...
echo This may take a few minutes on first run...
echo.

REM Clean and compile
mvn clean compile -q
if %errorlevel% equ 0 (
    echo Build successful!
    echo.
    echo Starting Java Multithreaded Web Crawler...
    echo ==============================================
    echo.
    
    REM Run the application
    mvn exec:java -Dexec.mainClass="com.crawler.Main"
) else (
    echo Build failed! Please check the error messages above.
    pause
    exit /b 1
)

pause
