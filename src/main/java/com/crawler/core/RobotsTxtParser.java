package com.crawler.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
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
        if (userAgent == null || userAgent.trim().isEmpty()) {
            throw new IllegalArgumentException("User agent cannot be null or empty");
        }
        if (timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        this.userAgent = userAgent;
        this.timeout = timeout;
    }
    
    public boolean isAllowed(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        try {
            String robotsUrl = getRobotsTxtUrl(url);
            RobotsTxtRules rules = getRobotsTxtRules(robotsUrl);
            return rules.isAllowed(url, userAgent);
        } catch (Exception e) {
            logger.warn("Error checking robots.txt for {}: {}", url, e.getMessage());
            return true;
        }
    }
    
    public int getCrawlDelay(String url) {
        if (url == null || url.trim().isEmpty()) {
            return 0;
        }
        
        try {
            String robotsUrl = getRobotsTxtUrl(url);
            RobotsTxtRules rules = getRobotsTxtRules(robotsUrl);
            return rules.getCrawlDelay(userAgent);
        } catch (Exception e) {
            logger.warn("Error getting crawl delay for {}: {}", url, e.getMessage());
            return 0;
        }
    }
    
    private String getRobotsTxtUrl(String pageUrl) throws Exception {
        URI uri = new URI(pageUrl);
        return uri.getScheme() + "://" + uri.getHost() + "/robots.txt";
    }
    
    private RobotsTxtRules getRobotsTxtRules(String robotsUrl) throws IOException {
        return robotsCache.computeIfAbsent(robotsUrl, url -> {
            try {
                return fetchAndParseRobotsTxt(url);
            } catch (IOException e) {
                logger.warn("Failed to fetch robots.txt from {}: {}", url, e.getMessage());
                return new RobotsTxtRules();
            }
        });
    }
    
    private RobotsTxtRules fetchAndParseRobotsTxt(String robotsUrl) throws IOException {
        RobotsTxtRules rules = new RobotsTxtRules();
        
        URL url = URI.create(robotsUrl).toURL();
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
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.toLowerCase().startsWith("user-agent:")) {
                    if (currentUserAgent != null) {
                        rules.addRules(currentUserAgent, currentDisallows, currentAllows, currentCrawlDelay);
                    }
                    
                    currentUserAgent = line.substring(11).trim();
                    currentDisallows.clear();
                    currentAllows.clear();
                    currentCrawlDelay = 0;
                    
                    if (currentUserAgent.equals("*")) {
                        currentUserAgent = "*";
                    }
                }
                else if (line.toLowerCase().startsWith("disallow:")) {
                    String path = line.substring(9).trim();
                    if (!path.isEmpty()) {
                        currentDisallows.add(Pattern.compile(convertToRegex(path)));
                    }
                }
                else if (line.toLowerCase().startsWith("allow:")) {
                    String path = line.substring(6).trim();
                    if (!path.isEmpty()) {
                        currentAllows.add(Pattern.compile(convertToRegex(path)));
                    }
                }
                else if (line.toLowerCase().startsWith("crawl-delay:")) {
                    try {
                        currentCrawlDelay = Integer.parseInt(line.substring(12).trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid crawl-delay value: {}", line);
                    }
                }
            }
            
            if (currentUserAgent != null) {
                rules.addRules(currentUserAgent, currentDisallows, currentAllows, currentCrawlDelay);
            }
        }
        
        return rules;
    }
    
    private String convertToRegex(String path) {
        if (path.isEmpty()) {
            return ".*";
        }
        
        String regex = path.replaceAll("([\\\\\\.\\[\\]\\{\\}\\(\\)\\+\\?\\^\\$\\|])", "\\\\$1");
        regex = regex.replaceAll("\\*", ".*");
        regex = regex.replaceAll("\\?", "\\.");
        
        if (!regex.startsWith(".*")) {
            regex = "^" + regex;
        }
        
        return regex;
    }
    
    private static class RobotsTxtRules {
        private final ConcurrentHashMap<String, UserAgentRules> userAgentRules = new ConcurrentHashMap<>();
        
        public void addRules(String userAgent, List<Pattern> disallows, List<Pattern> allows, int crawlDelay) {
            userAgentRules.put(userAgent, new UserAgentRules(disallows, allows, crawlDelay));
        }
        
        public boolean isAllowed(String url, String userAgent) {
            UserAgentRules specificRules = userAgentRules.get(userAgent);
            if (specificRules != null && specificRules.isAllowed(url)) {
                return true;
            }
            
            UserAgentRules wildcardRules = userAgentRules.get("*");
            if (wildcardRules != null) {
                return wildcardRules.isAllowed(url);
            }
            
            return true;
        }
        
        public int getCrawlDelay(String userAgent) {
            UserAgentRules specificRules = userAgentRules.get(userAgent);
            if (specificRules != null && specificRules.crawlDelay > 0) {
                return specificRules.crawlDelay;
            }
            
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
                
                for (Pattern allow : allows) {
                    if (allow.matcher(path).matches()) {
                        return true;
                    }
                }
                
                for (Pattern disallow : disallows) {
                    if (disallow.matcher(path).matches()) {
                        return false;
                    }
                }
                
                return true;
            }
            
            private String extractPath(String url) {
                try {
                    URL urlObj = URI.create(url).toURL();
                    return urlObj.getPath();
                } catch (Exception e) {
                    return url;
                }
            }
        }
    }
}
