# Java Multithreaded Web Crawler

A Java-based multithreaded web crawler application with a Swing GUI interface, designed for educational purposes to demonstrate multithreading concepts in Java.

## Features

- **Multithreaded Crawling**: Configurable thread pool for concurrent page processing
- **Swing GUI Interface**: User-friendly graphical interface for controlling the crawler
- **Configurable Parameters**: Adjustable thread count, max pages, max depth, and delays
- **Real-time Monitoring**: Live statistics and status updates
- **Pause/Resume Functionality**: Control crawling process without losing progress
- **Comprehensive Logging**: Detailed logging of all crawling activities
- **Error Handling**: Robust error handling for network issues and invalid URLs

## Project Structure

```
src/
├── main/java/com/crawler/
│   ├── Main.java                 # Application entry point
│   ├── core/
│   │   ├── WebCrawler.java      # Main crawler coordinator
│   │   └── CrawlerWorker.java   # Individual worker threads
│   ├── gui/
│   │   └── CrawlerGUI.java      # Swing GUI implementation
│   └── model/
│       ├── CrawledPage.java     # Data model for crawled pages
│       └── CrawlerConfig.java   # Configuration settings
└── test/java/com/crawler/
    └── WebCrawlerTest.java      # Unit tests
```

## Key Components

### 1. WebCrawler (Core Coordinator)
- Manages the thread pool and worker threads
- Coordinates the crawling process
- Provides statistics and control methods

### 2. CrawlerWorker (Thread Worker)
- Implements `Runnable` interface
- Processes individual URLs in separate threads
- Uses JSoup for HTML parsing and link extraction

### 3. CrawlerGUI (User Interface)
- Swing-based graphical interface
- Real-time status updates
- Configuration controls
- Results display table

### 4. Data Models
- `CrawledPage`: Stores page information, links, and metadata
- `CrawlerConfig`: Holds all crawler configuration parameters

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

## Building and Running

### 1. Build the Project
```bash
mvn clean compile
```

### 2. Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.crawler.Main"
```

### 3. Run Tests
```bash
mvn test
```

### 4. Create Executable JAR
```bash
mvn clean package
java -jar target/java-multithreaded-web-crawler-1.0.0.jar
```

## Usage

1. **Launch the Application**: Run the main class to open the GUI
2. **Enter Start URL**: Input the website URL you want to crawl
3. **Configure Settings**: Adjust thread count, max pages, depth, and delays
4. **Start Crawling**: Click "Start Crawling" to begin the process
5. **Monitor Progress**: Watch real-time statistics and logs
6. **Control Operations**: Use Pause/Resume/Stop buttons as needed
7. **View Results**: See crawled pages in the results table

## Configuration Options

- **Thread Count**: Number of concurrent worker threads (1-20)
- **Max Pages**: Maximum number of pages to crawl
- **Max Depth**: Maximum depth of crawling from start URL
- **Delay**: Milliseconds between requests (for respectful crawling)
- **Connection Timeout**: Network connection timeout in milliseconds

## Multithreading Implementation

The application demonstrates several key multithreading concepts:

1. **Thread Pool**: Uses `ExecutorService` with fixed thread pool
2. **Concurrent Collections**: `ConcurrentHashMap` and `BlockingQueue` for thread-safe data sharing
3. **Atomic Variables**: `AtomicInteger` for thread-safe counters
4. **Synchronization**: Proper synchronization between worker threads
5. **Thread Coordination**: Coordinated start/stop/pause/resume operations

## Dependencies

- **JSoup**: HTML parsing and DOM manipulation
- **SLF4J**: Logging framework
- **JUnit 5**: Testing framework

## Educational Value

This project demonstrates:
- Java multithreading concepts
- Swing GUI development
- Concurrent programming patterns
- Web scraping techniques
- Maven project structure
- Unit testing practices
- Error handling and logging

## Safety Features

- **Rate Limiting**: Configurable delays between requests
- **Domain Restriction**: Only crawls pages from the same domain
- **Maximum Limits**: Configurable limits on pages and depth
- **User Agent**: Proper identification in HTTP requests
- **Error Handling**: Graceful handling of network errors

## Limitations

- Basic robots.txt support (can be enhanced)
- Single-domain crawling only
- No JavaScript execution
- Basic link extraction (can be enhanced for specific content types)

## Future Enhancements

- Support for multiple domains
- Enhanced robots.txt parsing
- JavaScript rendering support
- Content filtering and analysis
- Export functionality (CSV, JSON)
- Advanced scheduling and retry logic
- Database storage for large crawls

## Contributing

This is an educational project, but suggestions and improvements are welcome. The code is structured to be easily understandable and extensible.

## License

This project is created for educational purposes. Feel free to use and modify for learning Java multithreading concepts.
