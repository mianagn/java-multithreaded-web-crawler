package com.crawler.core;

/**
 * Wrapper class to track URL with its depth level
 */
public class UrlWithDepth {
    private final String url;
    private final int depth;
    
    public UrlWithDepth(String url, int depth) {
        this.url = url;
        this.depth = depth;
    }
    
    public String getUrl() {
        return url;
    }
    
    public int getDepth() {
        return depth;
    }
    
    @Override
    public String toString() {
        return "UrlWithDepth{url='" + url + "', depth=" + depth + '}';
    }
}
