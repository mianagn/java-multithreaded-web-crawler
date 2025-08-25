package com.crawler.core;

import com.crawler.model.CrawledPage;
import com.crawler.model.CrawlerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadPoolExecutor;

public class WebCrawler {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);
    
    private final CrawlerConfig config;
    private final BlockingQueue<UrlWithDepth> urlQueue;
    private final ConcurrentHashMap<String, CrawledPage> crawledPages;
    private final ConcurrentHashMap<String, Boolean> seenUrls;
    private ExecutorService executorService;
    private final AtomicInteger pagesCrawled;
    
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    public WebCrawler(CrawlerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.config = config;
        this.urlQueue = new LinkedBlockingQueue<>();
        this.crawledPages = new ConcurrentHashMap<>();
        this.seenUrls = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        this.pagesCrawled = new AtomicInteger(0);
    }

    public void startCrawling(String startUrl) {
        if (startUrl == null || startUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Start URL cannot be null or empty");
        }
        
        if (isRunning) {
            logger.warn("Crawler is already running");
            return;
        }

        if (executorService.isTerminated() || executorService.isShutdown()) {
            logger.info("Previous crawling session completed. Recreating executor service for new session.");
            recreateExecutorService();
        }

        logger.info("Starting web crawler with {} threads", config.getThreadCount());
        logger.info("Configuration: {}", config);
        
        isRunning = true;
        isPaused = false;
        
        pagesCrawled.set(0);
        crawledPages.clear();
        seenUrls.clear();
        urlQueue.clear();
        
        addUrlToQueue(startUrl, 0);
        
        for (int i = 0; i < config.getThreadCount(); i++) {
            CrawlerWorker worker = new CrawlerWorker(
                urlQueue, crawledPages, seenUrls, config, pagesCrawled, startUrl
            );
            executorService.submit(worker);
        }
        
        startMonitoringThread();
    }

    public boolean addUrlToQueue(String url, int depth) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        String normalizedUrl = normalizeUrl(url);
        if (normalizedUrl == null) {
            return false;
        }
        
        Boolean wasSeen = seenUrls.putIfAbsent(normalizedUrl, false);
        if (wasSeen != null) {
            logger.debug("URL already seen, skipping: {}", normalizedUrl);
            return false;
        }
        
        if (pagesCrawled.get() >= config.getMaxPages()) {
            logger.info("Max pages reached ({}), skipping {}", config.getMaxPages(), normalizedUrl);
            seenUrls.remove(normalizedUrl);
            return false;
        }
        
        if (urlQueue.size() >= config.getMaxQueueSize()) {
            logger.info("Queue size limit reached ({}), skipping {}", config.getMaxQueueSize(), normalizedUrl);
            seenUrls.remove(normalizedUrl);
            return false;
        }
        
        urlQueue.offer(new UrlWithDepth(normalizedUrl, depth));
        logger.debug("Added URL to queue: {} (depth: {}, queue size: {})", 
                    normalizedUrl, depth, urlQueue.size());
        return true;
    }

    private String normalizeUrl(String url) {
        try {
            URL urlObj = URI.create(url).toURL();
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

    public void markUrlAsCrawled(String url) {
        if (url != null) {
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.replace(normalizedUrl, false, true);
            }
        }
    }

    private void recreateExecutorService() {
        if (executorService != null && !executorService.isTerminated()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    logger.warn("Executor service did not terminate within timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for executor shutdown");
            }
        }
        
        this.executorService = Executors.newFixedThreadPool(config.getThreadCount());
        logger.info("Created new executor service with {} threads", config.getThreadCount());
    }

    public void stopCrawler() {
        logger.info("Stopping web crawler");
        isRunning = false;
        isPaused = false;
        
        urlQueue.clear();
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.info("Forcing shutdown of executor service");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted while waiting for executor shutdown");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void pauseCrawler() {
        logger.info("Pausing web crawler");
        isPaused = true;
    }

    public void resumeCrawler() {
        logger.info("Resuming web crawler");
        isPaused = false;
    }

    public Collection<CrawledPage> getCrawledPages() {
        return crawledPages.values();
    }

    public CrawlingStats getStats() {
        return new CrawlingStats(
            pagesCrawled.get(),
            urlQueue.size(),
            seenUrls.size(),
            isRunning,
            isPaused
        );
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean canStart() {
        if (isRunning) {
            return false;
        }
        
        if (executorService == null) {
            return true;
        }
        
        if (executorService.isTerminated() || executorService.isShutdown()) {
            return true;
        }
        
        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            return tpe.getActiveCount() == 0;
        }
        
        return true;
    }

    private void startMonitoringThread() {
        Thread monitorThread = new Thread(() -> {
            int cleanupCounter = 0;
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    cleanupCounter++;
                    
                    if (!isPaused) {
                        if (pagesCrawled.get() >= config.getMaxPages()) {
                            logger.info("Reached maximum pages limit: {}", config.getMaxPages());
                            stopCrawler();
                            break;
                        }
                        
                        if (urlQueue.isEmpty() && pagesCrawled.get() > 0) {
                            Thread.sleep(3000);
                            
                            if (urlQueue.isEmpty()) {
                                logger.info("No more URLs to process, stopping crawler. Final stats: pages={}, queue={}, seen={}", 
                                          pagesCrawled.get(), urlQueue.size(), seenUrls.size());
                                stopCrawler();
                                break;
                            }
                        }
                    }
                    
                    if (cleanupCounter >= 30) {
                        cleanupRetryCounts();
                        cleanupCounter = 0;
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
    
    private void cleanupRetryCounts() {
        try {
            if (executorService instanceof ThreadPoolExecutor) {
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
                if (tpe.getActiveCount() > 0) {
                    logger.debug("Skipping retry count cleanup - workers are active");
                    return;
                }
            }
            
            int beforeSize = seenUrls.size();
            seenUrls.entrySet().removeIf(entry -> {
                return entry.getValue() && !urlQueue.stream()
                    .anyMatch(urlWithDepth -> urlWithDepth.getUrl().equals(entry.getKey()));
            });
            int afterSize = seenUrls.size();
            
            if (beforeSize != afterSize) {
                logger.debug("Cleaned up {} seen URLs, map size: {} -> {}", 
                           beforeSize - afterSize, beforeSize, afterSize);
            }
        } catch (Exception e) {
            logger.warn("Error during cleanup: {}", e.getMessage());
        }
    }

    public static class CrawlingStats {
        private final int pagesCrawled;
        private final int queueSize;
        private final int totalUrlsSeen;
        private final boolean isRunning;
        private final boolean isPaused;

        public CrawlingStats(int pagesCrawled, int queueSize, 
                           int totalUrlsSeen, boolean isRunning, boolean isPaused) {
            this.pagesCrawled = pagesCrawled;
            this.queueSize = queueSize;
            this.totalUrlsSeen = totalUrlsSeen;
            this.isRunning = isRunning;
            this.isPaused = isPaused;
        }

        public int getPagesCrawled() { return pagesCrawled; }
        public int getQueueSize() { return queueSize; }
        public int getTotalUrlsSeen() { return totalUrlsSeen; }
        public boolean isRunning() { return isRunning; }
        public boolean isPaused() { return isPaused; }

        @Override
        public String toString() {
            return "CrawlingStats{" +
                    "pagesCrawled=" + pagesCrawled +
                    ", queueSize=" + queueSize +
                    ", totalUrlsSeen=" + totalUrlsSeen +
                    ", isRunning=" + isRunning +
                    ", isPaused=" + isPaused +
                    '}';
        }
    }
}
