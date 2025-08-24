# Java Multithreaded Web Crawler

A multithreaded web crawler built in Java with Swing GUI, demonstrating concurrent programming concepts.

## Installation

### Prerequisites
- Java 21
- Maven 3.6+

### Setup
```bash
git clone https://github.com/mianagn/java-multithreaded-web-crawler
cd java-multithreaded-web-crawler
```

## Dependencies

The project uses the following dependencies:
- **JSoup**: HTML parsing and DOM manipulation
- **SLF4J**: Logging framework


## Running the Application

### Option 1: Maven Execution
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.crawler.Main"
```

### Option 2: Build and Run JAR
```bash
mvn clean package
java -jar target/java-multithreaded-web-crawler-1.0.0.jar
```

### Option 3: Run Main Class Directly
```bash
mvn clean compile
java -cp target/classes com.crawler.Main
```

## Multithreading and Concurrency

### Thread Pool Architecture
The crawler implements a **thread pool pattern** where multiple worker threads concurrently process URLs from a shared queue. This design allows for efficient resource utilization and prevents thread creation overhead.

**Location**: `WebCrawler.java` - Main coordinator class
- Creates a fixed thread pool using `ExecutorService`
- Manages worker thread lifecycle and coordination
- Implements thread-safe URL distribution

### Concurrent Data Structures
**Location**: `CrawlerWorker.java` and `WebCrawler.java`
- **`ConcurrentHashMap<String, CrawledPage>`**: Thread-safe storage of crawled pages
- **`ConcurrentHashMap<String, Boolean>`**: Thread-safe tracking of seen URLs
- **`BlockingQueue<UrlWithDepth>`**: Thread-safe queue for URL distribution

### Worker Thread Implementation
**Location**: `CrawlerWorker.java`
- Each worker thread runs independently, polling URLs from the shared queue
- Implements `Runnable` interface for concurrent execution
- Uses `Thread.sleep()` and `BlockingQueue.poll()` for efficient waiting
- Thread-safe access to shared data structures

### Synchronization Mechanisms
**Location**: Throughout the codebase
- **AtomicInteger**: Thread-safe counters for pages crawled and current depth
- **ScheduledExecutorService**: Background thread for GUI updates
- **SwingUtilities.invokeLater()**: Thread-safe GUI updates from worker threads

### Why This Design?
1. **Scalability**: Multiple threads can crawl different URLs simultaneously
2. **Efficiency**: Prevents blocking on network I/O operations
3. **Resource Management**: Controlled thread pool prevents resource exhaustion
4. **Responsiveness**: GUI remains responsive while crawling occurs in background
5. **Domain Isolation**: Each thread processes URLs independently, reducing contention

### Concurrency Challenges Addressed
- **Race Conditions**: Prevented through thread-safe data structures
- **Deadlocks**: Avoided by using non-blocking operations and timeouts
- **Memory Consistency**: Ensured through proper synchronization and volatile variables
- **Thread Coordination**: Managed through shared queues and atomic operations
