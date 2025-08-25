package com.crawler;

import com.crawler.gui.CrawlerGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        setupLogging();
        
        logger.info("Starting Java Multithreaded Web Crawler Application");
        
        try {
            setupLookAndFeel();
        } catch (Exception e) {
            logger.warn("Could not set system look and feel: {}", e.getMessage());
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                CrawlerGUI gui = new CrawlerGUI();
                gui.setVisible(true);
                logger.info("GUI launched successfully");
            } catch (Exception e) {
                logger.error("Failed to launch GUI: {}", e.getMessage(), e);
                showErrorDialog("Failed to launch application: " + e.getMessage());
                System.exit(1);
            }
        });
        
        setupShutdownHook();
    }
    
    private static void setupLogging() {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss");
    }
    
    private static void setupLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    
    private static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutting down");
        }));
    }
    
    private static void showErrorDialog(String message) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel if system look fails
        }
        
        JOptionPane.showMessageDialog(null, 
            message,
            "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
}
