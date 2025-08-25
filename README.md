# Java Multithreaded Web Crawler

A multithreaded web crawler built in Java with Swing GUI, demonstrating concurrent programming concepts.
**Academic Project**: This project was created for my "Parallel and Distributed Computing" (Παράλληλος και Κατανεμημένος Υπολογισμός) class at university.

## Installation

### Prerequisites
- Java 21 or higher
- Maven 3.6+ or higher

### Setup
```bash
git clone https://github.com/mianagn/java-multithreaded-web-crawler
cd java-multithreaded-web-crawler
```

## Quick Start (Recommended)

For the easiest experience, use our convenient run scripts:

### On Mac/Linux:
```bash
./run.sh
```

### On Windows:
```bash
run.bat
```

These scripts will:
- ✅ Check if Java 21+ and Maven are installed
- 🔨 Automatically download all dependencies
- 🚀 Build and run the application

## Manual Running

If you prefer to run manually or the scripts don't work:

### Option 1: Maven Execution
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.crawler.Main"
```

### Option 2: Run Main Class Directly
```