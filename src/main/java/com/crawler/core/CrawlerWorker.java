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
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
// New components for advanced features
import com.crawler.core.RobotsTxtParser;
import com.crawler.core.HttpResponseHandler;
import java.util.concurrent.TimeUnit;

/**
 * Worker thread for crawling individual web pages
 */
public class CrawlerWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerWorker.class);
    
    private final BlockingQueue<UrlWithDepth> urlQueue;
    private final ConcurrentHashMap<String, CrawledPage> crawledPages;
    private final ConcurrentHashMap<String, Boolean> seenUrls;
    private final CrawlerConfig config;
    private final AtomicInteger pagesCrawled;
    private final String baseUrl;
    // New components for advanced features
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
        // Initialize new components
        this.robotsParser = new RobotsTxtParser(config.getUserAgent(), config.getRobotsTxtTimeout());
        this.httpHandler = new HttpResponseHandler(config.getMaxRetries(), config.getRetryBaseDelay());
    }

    @Override
    public void run() {
        logger.info("Worker thread {} started", Thread.currentThread().getName());
        int urlsProcessed = 0;
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                UrlWithDepth urlWithDepth = urlQueue.poll(1, TimeUnit.SECONDS); // Poll with 1 second timeout
                if (urlWithDepth == null) {
                    // No URL available after timeout, check if we should continue
                    continue;
                }

                String url = urlWithDepth.getUrl();
                int depth = urlWithDepth.getDepth();

                if (shouldCrawlUrl(url, depth)) {
                    crawlPage(url, depth);
                    urlsProcessed++;
                }

                // Add delay between requests to be respectful
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
        // Check if we've already crawled this URL
        if (crawledPages.containsKey(url)) {
            return false;
        }

        // Check if we've reached the maximum number of pages
        if (pagesCrawled.get() >= config.getMaxPages()) {
            return false;
        }

        // Check robots.txt if enabled
        if (config.isRespectRobotsTxt() && !robotsParser.isAllowed(url)) {
            logger.info("URL blocked by robots.txt: {}", url);
            return false;
        }

        // Check if URL is from the same domain (basic check)
        try {
            URL urlObj = new URL(url);
            URL baseUrlObj = new URL(baseUrl);
            return urlObj.getHost().equals(baseUrlObj.getHost());
        } catch (Exception e) {
            logger.warn("Invalid URL format: {}", url);
            return false;
        }
    }

    /**
     * Check if a URL should be added to the queue (prevents duplicates)
     */
    private boolean shouldAddToQueue(String url, int currentDepth) {
        // Check if we've already seen this URL
        if (seenUrls.containsKey(url)) {
            return false;
        }

        // Check if URL is from the same domain
        try {
            URL urlObj = new URL(url);
            URL baseUrlObj = new URL(baseUrl);
            if (!urlObj.getHost().equals(baseUrlObj.getHost())) {
                return false;
            }
        } catch (Exception e) {
            logger.warn("Invalid URL format: {}", url);
            return false;
        }

        // Optional smart filtering
        if (config.isFilterNonContent() && isNonContentUrl(url)) {
            return false;
        }
        
        return true;
    }

    /**
     * Basic heuristic to filter non-content URLs (assets, auth, admin, feeds, etc.)
     * Made less aggressive to avoid filtering out legitimate content
     */
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

    /**
     * Heuristic to detect links inside typical UI containers (nav, header, footer, sidebar, menu, breadcrumbs, pagination)
     * Made less aggressive to avoid filtering out legitimate content links
     */
    private boolean isUiLink(Element linkEl) {
        Element el = linkEl;
        int steps = 0;
        while (el != null && steps < 4) { // Reduced from 6 to 4 levels
            String tag = el.tagName().toLowerCase();
            String cls = el.className().toLowerCase();
            String id = el.id() != null ? el.id().toLowerCase() : "";

            // Only filter obvious navigation containers
            if (tag.equals("nav")) {
                return true;
            }
            
            // More specific class filtering - only filter obvious navigation
            if (cls.contains("navbar") || cls.contains("breadcrumbs") || cls.contains("pagination")) {
                return true;
            }
            
            // More specific ID filtering
            if (id.contains("navbar") || id.contains("breadcrumbs") || id.contains("pagination")) {
                return true;
            }
            
            el = el.parent();
            steps++;
        }
        return false;
    }

    /**
     * Normalize URL to handle common variations and remove fragments
     */
    private String normalizeUrl(String url) {
        try {
            URL urlObj = new URL(url);
            // Remove fragments (#) and normalize
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

    private void crawlPage(String url, int currentDepth) {
        long startTime = System.currentTimeMillis();
        CrawledPage page = new CrawledPage(url);
        int redirectCount = 0;

        try {
            logger.info("Crawling: {} (depth: {})", url, currentDepth);

            // Connect to the URL with configuration
            Document doc = Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getConnectionTimeout())
                    .followRedirects(config.isFollowRedirects())
                    .maxBodySize(0) // No size limit
                    .get();

            // Get response details
            int statusCode = doc.connection().response().statusCode();
            String contentType = doc.connection().response().contentType();
            
            // Validate content type if enabled
            if (config.isValidContentType() && !httpHandler.isCrawlableContentType(contentType)) {
                logger.info("Skipping non-crawlable content type: {} for {}", contentType, url);
                page.setStatusCode(statusCode);
                page.setContent("Non-crawlable content type: " + contentType);
                page.setResponseTime(System.currentTimeMillis() - startTime);
                crawledPages.put(url, page);
                seenUrls.put(url, true);
                return;
            }

            // Check if response is successful
            if (!httpHandler.isSuccess(statusCode)) {
                if (httpHandler.isRedirect(statusCode) && config.isFollowRedirects()) {
                    // Handle redirects
                    if (httpHandler.shouldFollowRedirect(url, redirectCount)) {
                        String location = doc.connection().response().header("Location");
                        if (location != null) {
                            logger.info("Following redirect from {} to {}", url, location);
                            redirectCount++;
                            // Add redirect URL to queue if it's from same domain
                            if (shouldAddToQueue(location, currentDepth + 1)) {
                                String normalizedLocation = normalizeUrl(location);
                                if (normalizedLocation != null) {
                                    seenUrls.put(normalizedLocation, false);
                                    urlQueue.offer(new UrlWithDepth(normalizedLocation, currentDepth + 1));
                                }
                            }
                        }
                    }
                } else {
                    // Handle other non-success status codes
                    String statusDesc = httpHandler.getStatusDescription(statusCode);
                    logger.warn("HTTP {} ({}) for URL: {}", statusCode, statusDesc, url);
                    
                    // Check if we should retry
                    if (httpHandler.shouldRetry(statusCode, url)) {
                        long retryDelay = httpHandler.calculateRetryDelay(url);
                        logger.info("Will retry {} after {}ms (attempt {}/{})", 
                                  url, retryDelay, httpHandler.getRetryCount(url) + 1, config.getMaxRetries());
                        
                        // Add back to queue for retry
                        if (shouldAddToQueue(url, currentDepth)) {
                            String normalizedUrl = normalizeUrl(url);
                            if (normalizedUrl != null) {
                                seenUrls.put(normalizedUrl, false);
                                urlQueue.offer(new UrlWithDepth(normalizedUrl, currentDepth));
                            }
                        }
                        
                        // Sleep before next attempt
                        Thread.sleep(retryDelay);
                        return;
                    }
                }
                
                // If we get here, the request failed and won't be retried
                page.setStatusCode(statusCode);
                page.setContent("HTTP Error: " + httpHandler.getStatusDescription(statusCode));
                page.setResponseTime(System.currentTimeMillis() - startTime);
                crawledPages.put(url, page);
                seenUrls.put(url, true);
                return;
            }

            // Success! Extract page information
            page.setTitle(doc.title());
            page.setContent(doc.text());
            page.setStatusCode(statusCode);
            page.setResponseTime(System.currentTimeMillis() - startTime);

            // Reset retry count on success
            httpHandler.resetRetryCount(url);

            // Extract links
            Elements links = doc.select("a[href]");
            int processedLinks = 0;
            int totalLinks = links.size();
            int filteredLinks = 0;
            
            logger.info("Found {} total links on page: {}", totalLinks, url);
            
            for (Element link : links) {
                // Skip links that are likely part of UI chrome (navbars, headers, footers, sidebars, pagination)
                if (config.isFilterNonContent() && isUiLink(link)) {
                    filteredLinks++;
                    continue;
                }
                String href = link.attr("abs:href");
                if (href != null && !href.isEmpty()) {
                    page.addLink(href);
                    
                    // Add new URLs to the queue if we haven't reached max depth
                    // and if they should be added (prevents duplicates)
                    if (currentDepth < config.getMaxDepth()
                            && processedLinks < config.getMaxLinksPerPage()
                            && shouldAddToQueue(href, currentDepth + 1)) {
                        // Normalize the URL and mark as seen
                        String normalizedHref = normalizeUrl(href);
                        if (normalizedHref != null) {
                            seenUrls.put(normalizedHref, false); // false means "queued but not yet crawled"
                            urlQueue.offer(new UrlWithDepth(normalizedHref, currentDepth + 1));
                            processedLinks++;
                            logger.debug("Added to queue: {} (depth: {})", normalizedHref, currentDepth);
                        }
                    }
                }
            }
            
            logger.info("Page {}: {} total links, {} filtered, {} processed, {} added to queue", 
                       url, totalLinks, filteredLinks, page.getLinks().size(), processedLinks);

            // Store the crawled page and mark URL as crawled
            crawledPages.put(url, page);
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.put(normalizedUrl, true); // true means "crawled"
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
                seenUrls.put(normalizedUrl, true); // Mark as crawled even if it failed
            }
        } catch (Exception e) {
            logger.error("Unexpected error crawling {}: {}", url, e.getMessage());
            page.setStatusCode(500);
            page.setContent("Error: " + e.getMessage());
            page.setResponseTime(System.currentTimeMillis() - startTime);
            crawledPages.put(url, page);
            String normalizedUrl = normalizeUrl(url);
            if (normalizedUrl != null) {
                seenUrls.put(normalizedUrl, true); // Mark as crawled even if it failed
            }
        }
    }
}
