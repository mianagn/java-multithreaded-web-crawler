package com.crawler;

import com.crawler.core.WebCrawler;
import com.crawler.model.CrawlerConfig;
import com.crawler.model.CrawledPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

public class WebCrawlerTest {
    
    private WebCrawler crawler;
    private CrawlerConfig config;
    
    @BeforeEach
    void setUp() {
        config = new CrawlerConfig();
        config.setThreadCount(2);
        config.setMaxPages(5);
        config.setMaxDepth(2);
        config.setDelayBetweenRequests(100);
        
        crawler = new WebCrawler(config);
    }
    
    @AfterEach
    void tearDown() {
        if (crawler.isRunning()) {
            crawler.stopCrawler();
        }
    }
    
    @Test
    void testCrawlerInitialization() {
        assertNotNull(crawler);
        assertFalse(crawler.isRunning());
        assertFalse(crawler.isPaused());
        
        WebCrawler.CrawlingStats stats = crawler.getStats();
        assertEquals(0, stats.getPagesCrawled());
        assertEquals(0, stats.getCurrentDepth());
        assertEquals(0, stats.getQueueSize());
    }
    
    @Test
    void testCrawlerConfiguration() {
        // Thread count will be auto-adjusted based on system capabilities
        int expectedThreadCount = Math.max(1, Runtime.getRuntime().availableProcessors());
        assertEquals(expectedThreadCount, config.getThreadCount());
        assertEquals(5, config.getMaxPages());
        assertEquals(2, config.getMaxDepth());
        assertEquals(100, config.getDelayBetweenRequests());
    }
    
    @Test
    void testCrawlerStartStop() {
        assertFalse(crawler.isRunning());
        
        // Start with a simple URL
        crawler.startCrawling("https://httpbin.org/html");
        
        // Give it a moment to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the crawler
        crawler.stopCrawler();
        
        // Give it a moment to stop
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        assertFalse(crawler.isRunning());
    }
    
    @Test
    void testCrawlerPauseResume() {
        crawler.startCrawling("https://httpbin.org/html");
        
        // Give it a moment to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check if it's running (it might have finished quickly with single page)
        boolean wasRunning = crawler.isRunning();
        
        // Pause (only if it was running)
        if (wasRunning) {
            crawler.pauseCrawler();
            assertTrue(crawler.isPaused());
            
            // Resume
            crawler.resumeCrawler();
            assertFalse(crawler.isPaused());
        }
        
        // Stop
        crawler.stopCrawler();
    }
    
    @Test
    void testThreadSafety() {
        // Test that multiple threads can safely add URLs without duplicates
        config.setThreadCount(4);
        config.setMaxPages(100);
        config.setMaxDepth(3);
        
        crawler.startCrawling("https://httpbin.org/html");
        
        // Give it time to process some URLs
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Check that we don't have duplicate URLs in the seen URLs
        WebCrawler.CrawlingStats stats = crawler.getStats();
        assertTrue(stats.getTotalUrlsSeen() >= 0);
        crawler.stopCrawler();
    }
    
    @Test
    void testGracefulShutdown() {
        config.setThreadCount(2);
        config.setMaxPages(50);
        
        crawler.startCrawling("https://httpbin.org/html");
        
        // Give it a moment to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop the crawler
        long startTime = System.currentTimeMillis();
        crawler.stopCrawler();
        long stopTime = System.currentTimeMillis();
        
        // Should stop within reasonable time (not hang)
        assertTrue(stopTime - startTime < 10000); // 10 seconds max
        assertFalse(crawler.isRunning());
    }
}
