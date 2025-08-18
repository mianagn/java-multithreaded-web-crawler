package com.crawler.model;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

/**
 * Represents a crawled web page with its metadata and content
 */
public class CrawledPage {
    private String url;
    private String title;
    private String content;
    private List<String> links;
    private LocalDateTime crawlTime;
    private int statusCode;
    private long responseTime;

    public CrawledPage(String url) {
        this.url = url;
        this.links = new ArrayList<>();
        this.crawlTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public void addLink(String link) {
        if (link != null && !link.trim().isEmpty()) {
            this.links.add(link);
        }
    }

    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }

    public void setCrawlTime(LocalDateTime crawlTime) {
        this.crawlTime = crawlTime;
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
        this.responseTime = responseTime;
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
