# Java Multithreaded Web Crawler

A multithreaded web crawler built in Java with Swing GUI.

## Features

- **Multithreaded**: Configurable thread pool for concurrent crawling
- **Depth Control**: Limit crawling depth to prevent infinite loops
- **Page Limits**: Set maximum number of pages to crawl
- **Domain Restriction**: Crawl only within the same domain
- **Robots.txt**: Respects robots.txt files and crawl delays
- **GUI Interface**: Swing-based user interface for easy control

## Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+

### Build & Run
```bash
# Build
mvn clean compile

# Run
mvn exec:java -Dexec.mainClass="com.crawler.Main"
```

### Build JAR
```bash
mvn clean package
java -jar target/java-multithreaded-web-crawler-1.0.0.jar
```

## Configuration

Default settings:
- **Threads**: 10 (auto-optimized)
- **Max Depth**: 3
- **Max Pages**: 100
- **Delay**: 500ms between requests
- **Timeout**: 10 seconds

All settings can be adjusted through the GUI.

## Usage

1. Launch the application
2. Enter a starting URL (e.g., `https://example.com`)
3. Configure settings if needed
4. Click "Start Crawling"
5. Monitor progress in real-time
6. Use Pause/Resume/Stop controls as needed

## Architecture

- **WebCrawler**: Main coordinator managing worker threads
- **CrawlerWorker**: Individual threads for crawling URLs
- **RobotsTxtParser**: Handles robots.txt compliance
- **HttpResponseHandler**: Manages HTTP responses and retries
- **CrawlerGUI**: Swing-based user interface

## Testing

```bash
mvn test
```

## Dependencies

- JSoup (HTML parsing)
- SLF4J (logging)
- JUnit 5 (testing)
