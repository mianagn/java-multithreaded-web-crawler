package com.crawler.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private int maxQueueSize;
    private boolean filterNonContent;
    private int maxLinksPerPage;
    private int maxRetries;
    private int retryBaseDelay;
    private int maxRedirects;
    private boolean validateContentType;
    private int robotsTxtTimeout;

    public CrawlerConfig() {
        this.threadCount = 10;
        this.maxDepth = 3;
        this.maxPages = 100;
        this.delayBetweenRequests = 500;
        this.connectionTimeout = 10000;
        this.userAgent = "Java-Multithreaded-WebCrawler/1.0";
        this.followRedirects = true;
        this.respectRobotsTxt = true;
        this.maxQueueSize = 500;
        this.filterNonContent = true;
        this.maxLinksPerPage = 50;
        this.maxRetries = 3;
        this.retryBaseDelay = 1000;
        this.maxRedirects = 5;
        this.validateContentType = true;
        this.robotsTxtTimeout = 10000;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int maxThreads = Math.min(availableProcessors * 2, 32);
        int minThreads = Math.max(1, availableProcessors / 2);
        
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

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = Math.max(50, maxQueueSize);
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
