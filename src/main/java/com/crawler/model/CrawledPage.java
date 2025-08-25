package com.crawler.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDateTime;
import java.util.Objects;

public class CrawledPage {
    private final String url;
    private String title;
    private String content;
    private final List<String> links;
    private final LocalDateTime crawlTime;
    private int statusCode;
    private long responseTime;

    public CrawledPage(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        
        this.url = url;
        this.links = Collections.synchronizedList(new ArrayList<>());
        this.crawlTime = LocalDateTime.now();
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getLinks() {
        return new ArrayList<>(links);
    }

    public void addLink(String link) {
        if (link != null && !link.trim().isEmpty()) {
            this.links.add(link);
        }
    }

    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        if (responseTime < 0) {
            throw new IllegalArgumentException("Response time cannot be negative");
        }
        this.responseTime = responseTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawledPage that = (CrawledPage) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "CrawledPage{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", linksCount=" + links.size() +
                ", statusCode=" + statusCode +
                ", responseTime=" + responseTime + "ms" +
                ", crawlTime=" + crawlTime +
                '}';
    }
}
