package com.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles HTTP responses, status codes, and retry logic
 */
public class HttpResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);
    
    private final ConcurrentHashMap<String, AtomicInteger> retryCounts = new ConcurrentHashMap<>();
    private final int maxRetries;
    private final int baseDelay;
    
    public HttpResponseHandler(int maxRetries, int baseDelay) {
        this.maxRetries = maxRetries;
        this.baseDelay = baseDelay;
    }
    
    /**
     * Check if a response indicates success
     */
    public boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Check if a response indicates a redirect
     */
    public boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }
    
    /**
     * Check if a response indicates a client error
     */
    public boolean isClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Check if a response indicates a server error
     */
    public boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Check if a response indicates rate limiting
     */
    public boolean isRateLimited(int statusCode) {
        return statusCode == 429 || statusCode == 503;
    }
    
    /**
     * Check if a response indicates forbidden access
     */
    public boolean isForbidden(int statusCode) {
        return statusCode == 403;
    }
    
    /**
     * Check if a response indicates not found
     */
    public boolean isNotFound(int statusCode) {
        return statusCode == 404;
    }
    
    /**
     * Get a human-readable description of the status code
     */
    public String getStatusDescription(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found (Temporary Redirect)";
            case 304: return "Not Modified";
            case 307: return "Temporary Redirect";
            case 308: return "Permanent Redirect";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 429: return "Too Many Requests (Rate Limited)";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "Unknown Status";
        }
    }
    
    /**
     * Check if content type is crawlable
     */
    public boolean isCrawlableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String lowerType = contentType.toLowerCase();
        
        // HTML content types
        if (lowerType.contains("text/html") || lowerType.contains("application/xhtml+xml")) {
            return true;
        }
        
        // XML content types
        if (lowerType.contains("text/xml") || lowerType.contains("application/xml")) {
            return true;
        }
        
        // Plain text
        if (lowerType.contains("text/plain")) {
            return true;
        }
        
        // JSON (for API responses)
        if (lowerType.contains("application/json")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if we should retry based on status code and current retry count
     */
    public boolean shouldRetry(int statusCode, String url) {
        // Don't retry on client errors (4xx) except rate limiting
        if (isClientError(statusCode) && !isRateLimited(statusCode)) {
            return false;
        }
        
        // Don't retry if we've exceeded max retries
        AtomicInteger retryCount = retryCounts.get(url);
        if (retryCount != null && retryCount.get() >= maxRetries) {
            return false;
        }
        
        // Retry on server errors (5xx) and rate limiting (429, 503)
        return isServerError(statusCode) || isRateLimited(statusCode);
    }
    
    /**
     * Calculate delay for retry with exponential backoff
     */
    public long calculateRetryDelay(String url) {
        AtomicInteger retryCount = retryCounts.computeIfAbsent(url, k -> new AtomicInteger(0));
        int currentRetries = retryCount.get();
        
        // Exponential backoff: baseDelay * 2^retryCount
        long delay = baseDelay * (long) Math.pow(2, currentRetries);
        
        // Cap at 5 minutes
        delay = Math.min(delay, 300000);
        
        // Add some jitter to prevent thundering herd
        delay += (long) (Math.random() * 1000);
        
        return delay;
    }
    
    /**
     * Increment retry count for a URL
     */
    public void incrementRetryCount(String url) {
        retryCounts.computeIfAbsent(url, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Reset retry count for a URL (on successful request)
     */
    public void resetRetryCount(String url) {
        retryCounts.remove(url);
    }
    
    /**
     * Get current retry count for a URL
     */
    public int getRetryCount(String url) {
        AtomicInteger retryCount = retryCounts.get(url);
        return retryCount != null ? retryCount.get() : 0;
    }
    
    /**
     * Check if URL should be followed (not a redirect loop)
     */
    public boolean shouldFollowRedirect(String url, int redirectCount) {
        // Prevent redirect loops
        if (redirectCount > 5) {
            logger.warn("Too many redirects for {}, stopping", url);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get recommended delay based on status code
     */
    public long getRecommendedDelay(int statusCode) {
        if (isRateLimited(statusCode)) {
            return 60000; // 1 minute for rate limiting
        }
        
        if (isServerError(statusCode)) {
            return 10000; // 10 seconds for server errors
        }
        
        return 0; // No additional delay needed
    }
}
