package com.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class RobotsTxtParser {
    private static final Logger logger = LoggerFactory.getLogger(RobotsTxtParser.class);
    
    private final ConcurrentHashMap<String, RobotsTxtRules> robotsCache = new ConcurrentHashMap<>();
    private final String userAgent;
    private final int timeout;
    
    public RobotsTxtParser(String userAgent, int timeout) {
        this.userAgent = userAgent;
        this.timeout = timeout;
    }
    
    /**
     * Check if a URL is allowed to be crawled according to robots.txt
     */
    public boolean isAllowed(String url) {
        try {
            String robotsUrl = getRobotsTxtUrl(url);
            RobotsTxtRules rules = getRobotsTxtRules(robotsUrl);
            return rules.isAllowed(url, userAgent);
        } catch (Exception e) {
            logger.warn("Error checking robots.txt for {}: {}", url, e.getMessage());
            // If we can't check robots.txt, be conservative and allow
            return true;
        }
    }
    
    /**
     * Get the delay required between requests for a domain
     */
    public int getCrawlDelay(String url) {
        try {
            String robotsUrl = getRobotsTxtUrl(url);
            RobotsTxtRules rules = getRobotsTxtRules(robotsUrl);
            return rules.getCrawlDelay(userAgent);
        } catch (Exception e) {
            logger.warn("Error getting crawl delay for {}: {}", url, e.getMessage());
            return 0; // No delay if we can't determine
        }
    }
    
    /**
     * Get the robots.txt URL for a given page URL
     */
    private String getRobotsTxtUrl(String pageUrl) throws Exception {
        URL url = new URL(pageUrl);
        return url.getProtocol() + "://" + url.getHost() + "/robots.txt";
    }
    
    /**
     * Fetch and parse robots.txt rules, with caching
     */
    private RobotsTxtRules getRobotsTxtRules(String robotsUrl) throws IOException {
        return robotsCache.computeIfAbsent(robotsUrl, url -> {
            try {
                return fetchAndParseRobotsTxt(url);
            } catch (IOException e) {
                logger.warn("Failed to fetch robots.txt from {}: {}", url, e.getMessage());
                return new RobotsTxtRules(); // Return permissive rules on failure
            }
        });
    }
    
    /**
     * Fetch and parse robots.txt content
     */
    private RobotsTxtRules fetchAndParseRobotsTxt(String robotsUrl) throws IOException {
        RobotsTxtRules rules = new RobotsTxtRules();
        
        URL url = new URL(robotsUrl);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("User-Agent", userAgent);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
            
            String line;
            String currentUserAgent = null;
            List<Pattern> currentDisallows = new ArrayList<>();
            List<Pattern> currentAllows = new ArrayList<>();
            int currentCrawlDelay = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse User-agent directive
                if (line.toLowerCase().startsWith("user-agent:")) {
                    // Save previous group if exists
                    if (currentUserAgent != null) {
                        rules.addRules(currentUserAgent, currentDisallows, currentAllows, currentCrawlDelay);
                    }
                    
                    // Start new group
                    currentUserAgent = line.substring(11).trim();
                    currentDisallows.clear();
                    currentAllows.clear();
                    currentCrawlDelay = 0;
                    
                    // Handle wildcard user agent
                    if (currentUserAgent.equals("*")) {
                        currentUserAgent = "*";
                    }
                }
                // Parse Disallow directive
                else if (line.toLowerCase().startsWith("disallow:")) {
                    String path = line.substring(9).trim();
                    if (!path.isEmpty()) {
                        currentDisallows.add(Pattern.compile(convertToRegex(path)));
                    }
                }
                // Parse Allow directive
                else if (line.toLowerCase().startsWith("allow:")) {
                    String path = line.substring(6).trim();
                    if (!path.isEmpty()) {
                        currentAllows.add(Pattern.compile(convertToRegex(path)));
                    }
                }
                // Parse Crawl-delay directive
                else if (line.toLowerCase().startsWith("crawl-delay:")) {
                    try {
                        currentCrawlDelay = Integer.parseInt(line.substring(12).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid crawl-delay value: {}", line);
                    }
                }
            }
            
            // Save last group
            if (currentUserAgent != null) {
                rules.addRules(currentUserAgent, currentDisallows, currentAllows, currentCrawlDelay);
            }
        }
        
        return rules;
    }
    
    /**
     * Convert robots.txt path pattern to regex pattern
     */
    private String convertToRegex(String path) {
        if (path.isEmpty()) {
            return ".*"; // Empty disallow means allow all
        }
        
        // Escape regex special characters and convert wildcards
        String regex = path.replaceAll("([\\\\\\.\\[\\]\\{\\}\\(\\)\\+\\?\\^\\$\\|])", "\\\\$1");
        regex = regex.replaceAll("\\*", ".*");
        regex = regex.replaceAll("\\?", "\\.");
        
        // Ensure it matches from the beginning of the path
        if (!regex.startsWith(".*")) {
            regex = "^" + regex;
        }
        
        return regex;
    }
    
    /**
     * Inner class to hold robots.txt rules for a specific user agent
     */
    private static class RobotsTxtRules {
        private final ConcurrentHashMap<String, UserAgentRules> userAgentRules = new ConcurrentHashMap<>();
        
        public void addRules(String userAgent, List<Pattern> disallows, List<Pattern> allows, int crawlDelay) {
            userAgentRules.put(userAgent, new UserAgentRules(disallows, allows, crawlDelay));
        }
        
        public boolean isAllowed(String url, String userAgent) {
            // Check specific user agent first
            UserAgentRules specificRules = userAgentRules.get(userAgent);
            if (specificRules != null && specificRules.isAllowed(url)) {
                return true;
            }
            
            // Check wildcard rules
            UserAgentRules wildcardRules = userAgentRules.get("*");
            if (wildcardRules != null) {
                return wildcardRules.isAllowed(url);
            }
            
            // Default: allow if no rules found
            return true;
        }
        
        public int getCrawlDelay(String userAgent) {
            // Check specific user agent first
            UserAgentRules specificRules = userAgentRules.get(userAgent);
            if (specificRules != null && specificRules.crawlDelay > 0) {
                return specificRules.crawlDelay;
            }
            
            // Check wildcard rules
            UserAgentRules wildcardRules = userAgentRules.get("*");
            if (wildcardRules != null) {
                return wildcardRules.crawlDelay;
            }
            
            return 0;
        }
        
        private static class UserAgentRules {
            private final List<Pattern> disallows;
            private final List<Pattern> allows;
            private final int crawlDelay;
            
            public UserAgentRules(List<Pattern> disallows, List<Pattern> allows, int crawlDelay) {
                this.disallows = new ArrayList<>(disallows);
                this.allows = new ArrayList<>(allows);
                this.crawlDelay = crawlDelay;
            }
            
            public boolean isAllowed(String url) {
                String path = extractPath(url);
                
                // Check allow patterns first (more specific)
                for (Pattern allow : allows) {
                    if (allow.matcher(path).matches()) {
                        return true;
                    }
                }
                
                // Check disallow patterns
                for (Pattern disallow : disallows) {
                    if (disallow.matcher(path).matches()) {
                        return false;
                    }
                }
                
                // Default: allow if no patterns match
                return true;
            }
            
            private String extractPath(String url) {
                try {
                    URL urlObj = new URL(url);
                    return urlObj.getPath();
                } catch (Exception e) {
                    return url;
                }
            }
        }
    }
}
