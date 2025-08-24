package com.crawler;

import com.crawler.gui.CrawlerGUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "INFO");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss");
        
        logger.info("Starting Java Multithreaded Web Crawler Application");
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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
                JOptionPane.showMessageDialog(null, 
                    "Failed to launch application: " + e.getMessage(),
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutting down");
        }));
    }
}
