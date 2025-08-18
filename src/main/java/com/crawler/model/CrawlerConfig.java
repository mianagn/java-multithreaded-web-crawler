package com.crawler.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for the web crawler
 */
public class CrawlerConfig {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerConfig.class);
    private int threadCount;
    private int maxDepth;
    private int maxPages;
    private long delayBetweenRequests;
    private int connectionTimeout;
    private String userAgent;
    private boolean followRedirects;
    private boolean respectRobotsTxt;
    // New tuning options
    private int maxQueueSize;
    private boolean filterNonContent;
    private int maxLinksPerPage;
    // Advanced features
    private int maxRetries;
    private int retryBaseDelay;
    private int maxRedirects;
    private boolean validateContentType;
    private int robotsTxtTimeout;

    public CrawlerConfig() {
        // Default values
        this.threadCount = 10;        // Optimized for M4 MacBook Pro (8 cores + I/O bound nature)
        this.maxDepth = 3;
        this.maxPages = 100;
        this.delayBetweenRequests = 500; // Reduced to 500ms for better performance on fast sites
        this.connectionTimeout = 10000; // 10 seconds
        this.userAgent = "Java-Multithreaded-WebCrawler/1.0";
        this.followRedirects = true;
        this.respectRobotsTxt = true;
        // Defaults for tuning options
        this.maxQueueSize = 500;       // Prevent unbounded memory growth
        this.filterNonContent = true;  // Re-enabled now that depth tracking is fixed
        this.maxLinksPerPage = 50;     // Limit links processed per page
        // Defaults for advanced features
        this.maxRetries = 3;           // Retry failed requests up to 3 times
        this.retryBaseDelay = 1000;   // Base delay for exponential backoff (1 second)
        this.maxRedirects = 5;         // Maximum redirects to prevent loops
        this.validateContentType = true; // Validate content type before crawling
        this.robotsTxtTimeout = 10000; // 10 second timeout for robots.txt
    }

    // Getters and Setters
    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int maxThreads = Math.min(availableProcessors * 2, 32); // Cap at 2x cores or 32, whichever is lower
        int minThreads = Math.max(1, availableProcessors / 2);  // Minimum of 1 or half cores
        
        if (threadCount < minThreads || threadCount > maxThreads) {
            logger.warn("Thread count {} is outside recommended range [{}-{}] for {} available processors. " +
                       "Setting to optimal value.", threadCount, minThreads, maxThreads, availableProcessors);
            this.threadCount = Math.max(minThreads, Math.min(maxThreads, availableProcessors));
        } else {
            this.threadCount = threadCount;
        }
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = Math.max(1, maxDepth);
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = Math.max(1, maxPages);
    }

    public long getDelayBetweenRequests() {
        return delayBetweenRequests;
    }

    public void setDelayBetweenRequests(long delayBetweenRequests) {
        this.delayBetweenRequests = Math.max(0, delayBetweenRequests);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = Math.max(1000, connectionTimeout);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public boolean isRespectRobotsTxt() {
        return respectRobotsTxt;
    }

    public void setRespectRobotsTxt(boolean respectRobotsTxt) {
        this.respectRobotsTxt = respectRobotsTxt;
    }

    // New getters/setters
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = Math.max(50, maxQueueSize); // enforce reasonable minimum
    }

    public boolean isFilterNonContent() {
        return filterNonContent;
    }

    public void setFilterNonContent(boolean filterNonContent) {
        this.filterNonContent = filterNonContent;
    }

    public int getMaxLinksPerPage() {
        return maxLinksPerPage;
    }

    public void setMaxLinksPerPage(int maxLinksPerPage) {
        this.maxLinksPerPage = Math.max(1, maxLinksPerPage);
    }

    // Advanced feature getters and setters
    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    public int getRetryBaseDelay() {
        return retryBaseDelay;
    }

    public void setRetryBaseDelay(int retryBaseDelay) {
        this.retryBaseDelay = Math.max(100, retryBaseDelay);
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = Math.max(1, Math.min(maxRedirects, 10));
    }

    public boolean isValidContentType() {
        return validateContentType;
    }

    public void setValidContentType(boolean validateContentType) {
        this.validateContentType = validateContentType;
    }

    public int getRobotsTxtTimeout() {
        return robotsTxtTimeout;
    }

    public void setRobotsTxtTimeout(int robotsTxtTimeout) {
        this.robotsTxtTimeout = Math.max(1000, robotsTxtTimeout);
    }

    @Override
    public String toString() {
        return "CrawlerConfig{" +
                "threadCount=" + threadCount +
                ", maxDepth=" + maxDepth +
                ", maxPages=" + maxPages +
                ", delayBetweenRequests=" + delayBetweenRequests + "ms" +
                ", connectionTimeout=" + connectionTimeout + "ms" +
                ", userAgent='" + userAgent + '\'' +
                ", followRedirects=" + followRedirects +
                ", respectRobotsTxt=" + respectRobotsTxt +
                ", maxQueueSize=" + maxQueueSize +
                ", filterNonContent=" + filterNonContent +
                ", maxLinksPerPage=" + maxLinksPerPage +
                ", maxRetries=" + maxRetries +
                ", retryBaseDelay=" + retryBaseDelay + "ms" +
                ", maxRedirects=" + maxRedirects +
                ", validateContentType=" + validateContentType +
                ", robotsTxtTimeout=" + robotsTxtTimeout + "ms" +
                '}';
    }
}
