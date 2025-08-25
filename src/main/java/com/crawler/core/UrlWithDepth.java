package com.crawler.core;

import java.util.Objects;

public final class UrlWithDepth {
    private final String url;
    private final int depth;
    
    public UrlWithDepth(String url, int depth) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Depth cannot be negative");
        }
        
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlWithDepth that = (UrlWithDepth) o;
        return depth == that.depth && Objects.equals(url, that.url);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(url, depth);
    }
    
    @Override
    public String toString() {
        return "UrlWithDepth{url='" + url + "', depth=" + depth + '}';
    }
}
