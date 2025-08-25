package com.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpResponseHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);
    
    private final ConcurrentHashMap<String, AtomicInteger> retryCounts = new ConcurrentHashMap<>();
    private final int maxRetries;
    private final int baseDelay;
    
    public HttpResponseHandler(int maxRetries, int baseDelay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        if (baseDelay < 0) {
            throw new IllegalArgumentException("Base delay cannot be negative");
        }
        
        this.maxRetries = maxRetries;
        this.baseDelay = baseDelay;
    }
    
    public boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
    
    public boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }
    
    public boolean isClientError(int statusCode) {
        return statusCode >= 400 && statusCode < 500;
    }
    
    public boolean isServerError(int statusCode) {
        return statusCode >= 500 && statusCode < 600;
    }
    
    public boolean isRateLimited(int statusCode) {
        return statusCode == 429 || statusCode == 503;
    }
    
    public boolean isForbidden(int statusCode) {
        return statusCode == 403;
    }
    
    public boolean isNotFound(int statusCode) {
        return statusCode == 404;
    }
    
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
    
    public boolean isCrawlableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        String lowerType = contentType.toLowerCase();
        
        if (lowerType.contains("text/html") || lowerType.contains("application/xhtml+xml")) {
            return true;
        }
        
        if (lowerType.contains("text/xml") || lowerType.contains("application/xml")) {
            return true;
        }
        
        if (lowerType.contains("text/plain")) {
            return true;
        }
        
        if (lowerType.contains("application/json")) {
            return true;
        }
        
        return false;
    }
    
    public boolean shouldRetry(int statusCode, String url) {
        if (url == null) {
            return false;
        }
        
        if (isClientError(statusCode) && !isRateLimited(statusCode)) {
            return false;
        }
        
        AtomicInteger retryCount = retryCounts.get(url);
        if (retryCount != null && retryCount.get() >= maxRetries) {
            return false;
        }
        
        return isServerError(statusCode) || isRateLimited(statusCode);
    }
    
    public long calculateRetryDelay(String url) {
        if (url == null) {
            return baseDelay;
        }
        
        AtomicInteger retryCount = retryCounts.computeIfAbsent(url, k -> new AtomicInteger(0));
        int currentRetries = retryCount.get();
        
        long delay = baseDelay * (long) Math.pow(2, currentRetries);
        delay = Math.min(delay, 300000);
        delay += (long) (Math.random() * 1000);
        
        return delay;
    }
    
    public void incrementRetryCount(String url) {
        if (url != null) {
            retryCounts.computeIfAbsent(url, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }
    
    public void resetRetryCount(String url) {
        if (url != null) {
            retryCounts.remove(url);
        }
    }
    
    public int getRetryCount(String url) {
        if (url == null) {
            return 0;
        }
        
        AtomicInteger retryCount = retryCounts.get(url);
        return retryCount != null ? retryCount.get() : 0;
    }
    
    public void cleanupRetryCounts() {
        retryCounts.entrySet().removeIf(entry -> 
            entry.getValue().get() >= maxRetries);
    }
    
    public int getRetryCountsSize() {
        return retryCounts.size();
    }
    
    public boolean shouldFollowRedirect(String url, int redirectCount) {
        if (redirectCount > 5) {
            logger.warn("Too many redirects for {}, stopping", url);
            return false;
        }
        
        return true;
    }
    
    public long getRecommendedDelay(int statusCode) {
        if (isRateLimited(statusCode)) {
            return 60000;
        }
        
        if (isServerError(statusCode)) {
            return 10000;
        }
        
        return 0;
    }
}
