package com.crawler.core;

import com.crawler.model.CrawledPage;
import com.crawler.model.CrawlerConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class CrawlerWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerWorker.class);
    
    private final BlockingQueue<UrlWithDepth> urlQueue;
    private final ConcurrentHashMap<String, CrawledPage> crawledPages;
    private final ConcurrentHashMap<String, Boolean> seenUrls;
    private final CrawlerConfig config;
    private final AtomicInteger pagesCrawled;
    private final String baseUrl;
    private final RobotsTxtParser robotsParser;
    private final HttpResponseHandler httpHandler;

    public CrawlerWorker(BlockingQueue<UrlWithDepth> urlQueue, 
                        ConcurrentHashMap<String, CrawledPage> crawledPages,
                        ConcurrentHashMap<String, Boolean> seenUrls,
                        CrawlerConfig config,
                        AtomicInteger pagesCrawled,
                        String baseUrl) {
        this.urlQueue = urlQueue;
        this.crawledPages = crawledPages;
        this.seenUrls = seenUrls;
        this.config = config;
        this.pagesCrawled = pagesCrawled;
        this.baseUrl = baseUrl;
        this.robotsParser = new RobotsTxtParser(config.getUserAgent(), config.getRobotsTxtTimeout());
        this.httpHandler = new HttpResponseHandler(config.getMaxRetries(), config.getRetryBaseDelay());
    }

    @Override
    public void run() {
        logger.info("Worker thread {} started", Thread.currentThread().getName());
        int urlsProcessed = 0;
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                UrlWithDepth urlWithDepth = urlQueue.poll(1, TimeUnit.SECONDS);
                if (urlWithDepth == null) {
                    continue;
                }

                String url = urlWithDepth.getUrl();
                int depth = urlWithDepth.getDepth();

                if (shouldCrawlUrl(url, depth)) {
                    crawlPage(url, depth);
                    urlsProcessed++;
                }

                if (config.getDelayBetweenRequests() > 0) {
                    Thread.sleep(config.getDelayBetweenRequests());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Worker thread interrupted: {}", Thread.currentThread().getName());
                break;
            } catch (Exception e) {
                logger.error("Error in worker thread {}: {}", Thread.currentThread().getName(), e.getMessage());
            }
        }
        
        logger.info("Worker thread {} finished (processed: {} URLs)", Thread.currentThread().getName(), urlsProcessed);
    }

    private boolean shouldCrawlUrl(String url, int currentDepth) {
        if (crawledPages.containsKey(url)) {
            return false;
        }

        if (pagesCrawled.get() >= config.getMaxPages()) {
            return false;
        }

        if (config.isRespectRobotsTxt() && !robotsParser.isAllowed(url)) {
            logger.info("URL blocked by robots.txt: {}", url);
            return false;
        }

        try {
            URL urlObj = URI.create(url).toURL();
            URL baseUrlObj = URI.create(baseUrl).toURL();
            return urlObj.getHost().equals(baseUrlObj.getHost());
        } catch (Exception e) {
            logger.warn("Invalid URL format: {}", url);
            return false;
        }
    }

    private boolean shouldAddToQueue(String url, int currentDepth) {
        if (seenUrls.containsKey(url)) {
            return false;
        }

        try {
            URL urlObj = URI.create(url).toURL();
            URL baseUrlObj = URI.create(baseUrl).toURL();
            if (!urlObj.getHost().equals(baseUrlObj.getHost())) {
                return false;
            }
        } catch (Exception e) {
            logger.warn("Invalid URL format: {}", url);
            return false;
        }

        if (config.isFilterNonContent() && isNonContentUrl(url)) {
            return false;
        }
        
        return true;
    }

    private boolean isNonContentUrl(String url) {
        String lowerUrl = url.toLowerCase();

        String[] patterns = new String[] {
            "/admin/", "/wp-admin/", "/wp-login.php", "/login", "/logout", "/register",
            "/cart/", "/checkout/", "/my-account/", "/account/", "/profile/",
            "/search", "/sitemap", "/robots.txt", "/favicon.ico",
            "/feed/", "/rss/", "/atom/", "/xmlrpc.php",
            "/wp-cron.php", "/wp-content/plugins/", "/wp-content/themes/",
            "/temp/", "/tmp/", "/cache/", "/logs/"
        };
        for (String pattern : patterns) {
            if (lowerUrl.contains(pattern)) {
                return true;
            }
        }

        String[] extensions = new String[] {
            ".css", ".js", ".map", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar",
            ".mp3", ".wav", ".mp4", ".avi", ".mov", ".wmv"
        };
        for (String ext : extensions) {
            if (lowerUrl.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUiLink(Element linkEl) {
        Element el = linkEl;
        int steps = 0;
        while (el != null && steps < 4) {
            String tag = el.tagName().toLowerCase();
            String cls = el.className().toLowerCase();
            String id = el.id() != null ? el.id().toLowerCase() : "";

            if (tag.equals("nav")) {
                return true;
            }
            
            if (cls.contains("navbar") || cls.contains("breadcrumbs") || cls.contains("pagination")) {
                return true;
            }
            
            if (id.contains("navbar") || id.contains("breadcrumbs") || id.contains("pagination")) {
                return true;
            }
            
            el = el.parent();
            steps++;
        }
        return false;
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

    private boolean addUrlToQueueSafely(String url, int depth) {
        try {
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl == null) {
                return false;
            }
            
            Boolean wasSeen = seenUrls.putIfAbsent(normalizedUrl, false);
            if (wasSeen != null) {
                return false;
            }
            
            if (pagesCrawled.get() >= config.getMaxPages()) {
                seenUrls.remove(normalizedUrl);
                return false;
            }
            
            if (urlQueue.size() >= config.getMaxQueueSize()) {
                seenUrls.remove(normalizedUrl);
                return false;
            }
            
            urlQueue.offer(new UrlWithDepth(normalizedUrl, depth));
            return true;
        } catch (Exception e) {
            logger.warn("Error adding URL to queue: {}", e.getMessage());
            return false;
        }
    }

    private void crawlPage(String url, int currentDepth) {
        long startTime = System.currentTimeMillis();
        CrawledPage page = new CrawledPage(url);
        int redirectCount = 0;

        try {
                        logger.info("Crawling: {} (depth: {})", url, currentDepth);

            Document doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getConnectionTimeout())
                    .followRedirects(config.isFollowRedirects())
                    .maxBodySize(0)
                    .get();

            int statusCode = doc.connection().response().statusCode();
            String contentType = doc.connection().response().contentType();
            
            if (config.isValidContentType() && !httpHandler.isCrawlableContentType(contentType)) {
                logger.info("Skipping non-crawlable content type: {} for {}", contentType, url);
                page.setStatusCode(statusCode);
                page.setContent("Non-crawlable content type: " + contentType);
                page.setResponseTime(System.currentTimeMillis() - startTime);
                crawledPages.put(url, page);
                seenUrls.put(url, true);
                return;
            }

            if (!httpHandler.isSuccess(statusCode)) {
                if (httpHandler.isRedirect(statusCode) && config.isFollowRedirects()) {
                    if (httpHandler.shouldFollowRedirect(url, redirectCount)) {
                        String location = doc.connection().response().header("Location");
                        if (location != null) {
                            logger.info("Following redirect from {} to {}", url, location);
                            redirectCount++;
                            if (shouldAddToQueue(location, currentDepth + 1)) {
                                addUrlToQueueSafely(location, currentDepth + 1);
                            }
                        }
                    }
                } else {
                    String statusDesc = httpHandler.getStatusDescription(statusCode);
                    logger.warn("HTTP {} ({}) for URL: {}", statusCode, statusDesc, url);
                    
                    if (httpHandler.shouldRetry(statusCode, url)) {
                        long retryDelay = httpHandler.calculateRetryDelay(url);
                        logger.info("Will retry {} after {}ms (attempt {}/{})", 
                                  url, retryDelay, httpHandler.getRetryCount(url) + 1, config.getMaxRetries());
                        
                        if (shouldAddToQueue(url, currentDepth)) {
                            addUrlToQueueSafely(url, currentDepth);
                        }
                        
                        Thread.sleep(retryDelay);
                        return;
                    }
                }
                
                page.setStatusCode(statusCode);
                page.setContent("HTTP Error: " + httpHandler.getStatusDescription(statusCode));
                page.setResponseTime(System.currentTimeMillis() - startTime);
                crawledPages.put(url, page);
                String normalizedUrl = normalizeUrl(url);
                if (normalizedUrl != null) {
                    seenUrls.replace(normalizedUrl, false, true);
                }
                return;
            }

            page.setTitle(doc.title());
            page.setContent(doc.text());
            page.setStatusCode(statusCode);
            page.setResponseTime(System.currentTimeMillis() - startTime);

            httpHandler.resetRetryCount(url);

            Elements links = doc.select("a[href]");
            int processedLinks = 0;
            int totalLinks = links.size();
            int filteredLinks = 0;
            
            logger.info("Found {} total links on page: {}", totalLinks, url);
            
            for (Element link : links) {
                if (config.isFilterNonContent() && isUiLink(link)) {
                    filteredLinks++;
                    continue;
                }
                String href = link.attr("abs:href");
                if (href != null && !href.isEmpty()) {
                    page.addLink(href);
                    
                    if (currentDepth < config.getMaxDepth()
                            && processedLinks < config.getMaxLinksPerPage()
                            && shouldAddToQueue(href, currentDepth + 1)) {
                        if (addUrlToQueueSafely(href, currentDepth + 1)) {
                            processedLinks++;
                            logger.debug("Added to queue: {} (depth: {})", href, currentDepth);
                        }
                    }
                }
            }
            
            logger.info("Page {}: {} total links, {} filtered, {} processed, {} added to queue", 
                       url, totalLinks, filteredLinks, page.getLinks().size(), processedLinks);

            crawledPages.put(url, page);
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.replace(normalizedUrl, false, true);
            }
            pagesCrawled.incrementAndGet();

            logger.info("Successfully crawled: {} ({} links found, depth: {})", url, page.getLinks().size(), currentDepth);

        } catch (IOException e) {
            logger.warn("Failed to crawl {}: {}", url, e.getMessage());
            page.setStatusCode(500);
            page.setContent("Error: " + e.getMessage());
            page.setResponseTime(System.currentTimeMillis() - startTime);
            crawledPages.put(url, page);
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.replace(normalizedUrl, false, true);
            }
        } catch (Exception e) {
            logger.error("Unexpected error crawling {}: {}", url, e.getMessage());
            page.setStatusCode(500);
            page.setContent("Error: " + e.getMessage());
            page.setResponseTime(System.currentTimeMillis() - startTime);
            crawledPages.put(url, page);
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.replace(normalizedUrl, false, true);
            }
        }
    }
}
