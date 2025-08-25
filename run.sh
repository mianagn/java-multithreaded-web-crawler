#!/bin/bash

echo "Java Multithreaded Web Crawler - Build & Run Script"
echo "======================================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 21 or higher and try again"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "Error: Java 21 or higher is required (found Java $JAVA_VERSION)"
    echo "Please upgrade Java and try again"
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven 3.6+ and try again"
    exit 1
fi

echo "Maven version: $(mvn -version | head -n 1)"
echo ""

echo "Building project and downloading dependencies..."
echo "This may take a few minutes on first run..."
echo ""

# Clean and compile
if mvn clean compile -q; then
    echo "Build successful!"
    echo ""
    echo "Starting Java Multithreaded Web Crawler..."
    echo "=============================================="
    echo ""
    
    # Run the application
    mvn exec:java -Dexec.mainClass="com.crawler.Main"
else
    echo "Build failed! Please check the error messages above."
    exit 1
fi
