package com.crawler.core;

import com.crawler.model.CrawledPage;
import com.crawler.model.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main web crawler class that manages multiple worker threads
 */
public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);
    
    private final CrawlerConfig config;
    private final BlockingQueue<UrlWithDepth> urlQueue;
    private final ConcurrentHashMap<String, CrawledPage> crawledPages;
    private final ConcurrentHashMap<String, Boolean> seenUrls; // Track all URLs we've seen
    private final ExecutorService executorService;
    private final AtomicInteger pagesCrawled;
    private final AtomicInteger currentDepth;
    
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    public WebCrawler(CrawlerConfig config) {
        this.config = config;
        this.urlQueue = new LinkedBlockingQueue<>();
        this.crawledPages = new ConcurrentHashMap<>();
        this.seenUrls = new ConcurrentHashMap<>(); // Track all URLs we've seen (queued or crawled)
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        this.pagesCrawled = new AtomicInteger(0);
        this.currentDepth = new AtomicInteger(0);
    }

    /**
     * Start crawling from the given URL
     */
    public void startCrawling(String startUrl) {
        if (isRunning) {
            logger.warn("Crawler is already running");
            return;
        }

        logger.info("Starting web crawler with {} threads", config.getThreadCount());
        logger.info("Configuration: {}", config);
        
        isRunning = true;
        isPaused = false;
        
        // Reset counters
        pagesCrawled.set(0);
        currentDepth.set(0);
        crawledPages.clear();
        seenUrls.clear();
        urlQueue.clear();
        
        // Add the starting URL with depth 0
        addUrlToQueue(startUrl, 0);
        
        // Create and start worker threads
        for (int i = 0; i < config.getThreadCount(); i++) {
            CrawlerWorker worker = new CrawlerWorker(
                urlQueue, crawledPages, seenUrls, config, pagesCrawled, startUrl
            );
            executorService.submit(worker);
        }
        
        // Start monitoring thread
        startMonitoringThread();
    }

    /**
     * Add a URL to the queue if it hasn't been seen before
     */
    public boolean addUrlToQueue(String url, int depth) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Normalize the URL to handle common variations
        String normalizedUrl = normalizeUrl(url);
        if (normalizedUrl == null) {
            return false;
        }
        
        // Enforce queue size limit
        if (urlQueue.size() >= config.getMaxQueueSize()) {
            logger.info("Queue size limit reached ({}), skipping {}", config.getMaxQueueSize(), normalizedUrl);
            return false;
        }

        // Check if we've already seen this URL
        if (seenUrls.containsKey(normalizedUrl)) {
            logger.debug("URL already seen, skipping: {}", normalizedUrl);
            return false;
        }
        
        // Check if we've reached the maximum number of pages
        if (pagesCrawled.get() >= config.getMaxPages()) {
            logger.info("Max pages reached ({}), skipping {}", config.getMaxPages(), normalizedUrl);
            return false;
        }
        
        // Mark this URL as seen and add to queue
        seenUrls.put(normalizedUrl, false); // false means "queued but not yet crawled"
        urlQueue.offer(new UrlWithDepth(normalizedUrl, depth));
        logger.debug("Added URL to queue: {} (depth: {}, queue size: {})", 
                    normalizedUrl, depth, urlQueue.size());
        return true;
    }

    /**
     * Normalize URL to handle common variations and remove fragments
     */
    private String normalizeUrl(String url) {
        try {
            URL urlObj = new URL(url);
            // Remove fragments (#) and normalize
            String normalized = urlObj.getProtocol() + "://" + urlObj.getHost();
            if (urlObj.getPort() != -1) {
                normalized += ":" + urlObj.getPort();
            }
            normalized += urlObj.getPath();
            if (urlObj.getQuery() != null && !urlObj.getQuery().isEmpty()) {
                normalized += "?" + urlObj.getQuery();
            }
            return normalized;
        } catch (Exception e) {
            logger.warn("Invalid URL format: {}", url);
            return null;
        }
    }

    /**
     * Mark a URL as crawled
     */
    public void markUrlAsCrawled(String url) {
        if (url != null) {
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.put(normalizedUrl, true); // true means "crawled"
            }
        }
    }

    /**
     * Stop the crawler
     */
    public void stopCrawler() {
        logger.info("Stopping web crawler");
        isRunning = false;
        isPaused = false;
        
        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Pause the crawler
     */
    public void pauseCrawler() {
        logger.info("Pausing web crawler");
        isPaused = true;
    }

    /**
     * Resume the crawler
     */
    public void resumeCrawler() {
        logger.info("Resuming web crawler");
        isPaused = false;
    }

    /**
     * Get all crawled pages
     */
    public Collection<CrawledPage> getCrawledPages() {
        return crawledPages.values();
    }

    /**
     * Get crawling statistics
     */
    public CrawlingStats getStats() {
        return new CrawlingStats(
            pagesCrawled.get(),
            currentDepth.get(),
            urlQueue.size(),
            seenUrls.size(),
            isRunning,
            isPaused
        );
    }

    /**
     * Check if crawler is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Check if crawler is paused
     */
    public boolean isPaused() {
        return isPaused;
    }

    private void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000); // Check every second
                    
                    if (!isPaused) {
                        // Check if we've reached the maximum pages
                        if (pagesCrawled.get() >= config.getMaxPages()) {
                            logger.info("Reached maximum pages limit: {}", config.getMaxPages());
                            stopCrawler();
                            break;
                        }
                        
                        // Check if queue is empty and all workers are done
                        if (urlQueue.isEmpty() && pagesCrawled.get() > 0) {
                            // Give workers a moment to process any URLs that might be in flight
                            Thread.sleep(3000);
                            
                            if (urlQueue.isEmpty()) {
                                logger.info("No more URLs to process, stopping crawler. Final stats: pages={}, queue={}, seen={}", 
                                          pagesCrawled.get(), urlQueue.size(), seenUrls.size());
                                stopCrawler();
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Statistics class for the crawler
     */
    public static class CrawlingStats {
        private final int pagesCrawled;
        private final int currentDepth;
        private final int queueSize;
        private final int totalUrlsSeen;
        private final boolean isRunning;
        private final boolean isPaused;

        public CrawlingStats(int pagesCrawled, int currentDepth, int queueSize, 
                           int totalUrlsSeen, boolean isRunning, boolean isPaused) {
            this.pagesCrawled = pagesCrawled;
            this.currentDepth = currentDepth;
            this.queueSize = queueSize;
            this.totalUrlsSeen = totalUrlsSeen;
            this.isRunning = isRunning;
            this.isPaused = isPaused;
        }

        // Getters
        public int getPagesCrawled() { return pagesCrawled; }
        public int getCurrentDepth() { return currentDepth; }
        public int getQueueSize() { return queueSize; }
        public int getTotalUrlsSeen() { return totalUrlsSeen; }
        public boolean isRunning() { return isRunning; }
        public boolean isPaused() { return isPaused; }

        @Override
        public String toString() {
            return "CrawlingStats{" +
                    "pagesCrawled=" + pagesCrawled +
                    ", currentDepth=" + currentDepth +
                    ", queueSize=" + queueSize +
                    ", totalUrlsSeen=" + totalUrlsSeen +
                    ", isRunning=" + isRunning +
                    ", isPaused=" + isPaused +
                    '}';
        }
    }
}
