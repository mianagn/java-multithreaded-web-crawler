# Setup Guide for Java Multithreaded Web Crawler

This guide will help you set up the development environment and run the web crawler application.

## Prerequisites

### 1. Install Java Development Kit (JDK)

**For macOS:**
```bash
# Using Homebrew (recommended)
brew install openjdk@11

# Or download from Oracle
# Visit: https://www.oracle.com/java/technologies/downloads/
```

**For Windows:**
- Download JDK 11 or higher from Oracle's website
- Run the installer and follow the setup wizard
- Add Java to your system PATH

**For Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-11-jdk
```

**Verify Java installation:**
```bash
java -version
javac -version
```

### 2. Install Maven

**For macOS:**
```bash
brew install maven
```

**For Windows:**
- Download Maven from: https://maven.apache.org/download.cgi
- Extract to a directory (e.g., `C:\Program Files\Apache\maven`)
- Add Maven's bin directory to your system PATH

**For Linux (Ubuntu/Debian):**
```bash
sudo apt install maven
```

**Verify Maven installation:**
```bash
mvn -version
```

## Project Setup

### 1. Clone or Download the Project
```bash
git clone <repository-url>
cd java-multithreaded-web-crawler
```

### 2. Build the Project
```bash
mvn clean compile
```

### 3. Run the Application

**Option 1: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.crawler.Main"
```

**Option 2: Using the provided scripts**

**On macOS/Linux:**
```bash
./run.sh
```

**On Windows:**
```cmd
run.bat
```

**Option 3: Create and run JAR file**
```bash
mvn clean package
java -jar target/java-multithreaded-web-crawler-1.0.0.jar
```

## Running Tests

```bash
mvn test
```

## Project Structure

```
java-multithreaded-web-crawler/
├── src/
│   ├── main/java/com/crawler/
│   │   ├── Main.java              # Application entry point
│   │   ├── core/                  # Core crawler logic
│   │   ├── gui/                   # Swing GUI components
│   │   └── model/                 # Data models
│   ├── main/resources/            # Configuration files
│   └── test/java/com/crawler/     # Unit tests
├── pom.xml                        # Maven configuration
├── README.md                      # Project documentation
├── SETUP.md                       # This setup guide
├── run.sh                         # Unix/Linux/macOS runner script
└── run.bat                        # Windows runner script
```

## Troubleshooting

### Common Issues

**1. "Java not found" error**
- Ensure Java is installed and in your system PATH
- Verify with `java -version`

**2. "Maven not found" error**
- Ensure Maven is installed and in your system PATH
- Verify with `mvn -version`

**3. Build failures**
- Check that you have Java 11 or higher
- Ensure all dependencies can be downloaded
- Check your internet connection for Maven dependency downloads

**4. GUI not displaying**
- Ensure you're running on a system with display capabilities
- For headless systems, consider running without GUI

### Getting Help

If you encounter issues:
1. Check the console output for error messages
2. Verify all prerequisites are installed correctly
3. Check that you're in the correct project directory
4. Ensure you have sufficient permissions to create files/directories

## Next Steps

Once the application is running:
1. Enter a URL to start crawling
2. Adjust configuration parameters as needed
3. Monitor the crawling process through the GUI
4. Explore the code to understand the multithreading implementation

## Development

To modify or extend the project:
1. Make your changes in the source code
2. Rebuild with `mvn clean compile`
3. Run tests with `mvn test`
4. Test your changes by running the application

Happy crawling! 🕷️
