@echo off
echo Java Multithreaded Web Crawler
echo ================================

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 11 or higher and try again
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed or not in PATH
    echo Please install Maven 3.6 or higher and try again
    pause
    exit /b 1
)

echo Java version:
java -version 2>&1 | findstr "version"
echo.
echo Maven version:
mvn -version 2>&1 | findstr "Apache Maven"
echo.

REM Build the project
echo Building project...
mvn clean compile

if %errorlevel% neq 0 (
    echo Build failed. Please check the errors above.
    pause
    exit /b 1
)

echo Build successful!
echo.

REM Run the application
echo Starting the Web Crawler application...
mvn exec:java -Dexec.mainClass="com.crawler.Main"

pause
