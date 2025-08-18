package com.crawler.gui;

import com.crawler.core.WebCrawler;
import com.crawler.model.CrawlerConfig;
import com.crawler.model.CrawledPage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main GUI for the web crawler application
 */
public class CrawlerGUI extends JFrame {
    private final WebCrawler crawler;
    private final CrawlerConfig config;
    
    // GUI Components
    private JTextField urlField;
    private JButton startButton;
    private JButton stopButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel statsLabel;
    
    // Threading
    private final ScheduledExecutorService updateExecutor;
    
    public CrawlerGUI() {
        this.config = new CrawlerConfig();
        this.crawler = new WebCrawler(config);
        this.updateExecutor = Executors.newScheduledThreadPool(1);
        
        initializeGUI();
        setupEventHandlers();
        startStatusUpdates();
    }
    
    private void initializeGUI() {
        setTitle("Java Multithreaded Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel for URL input and controls
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel for results table
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel for logs and status
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Crawler Controls"));
        
        // URL input panel
        JPanel urlPanel = new JPanel(new BorderLayout(5, 0));
        urlPanel.add(new JLabel("Start URL:"), BorderLayout.WEST);
        urlField = new JTextField("https://example.com");
        urlPanel.add(urlField, BorderLayout.CENTER);
        
        // Control buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start Crawling");
        stopButton = new JButton("Stop");
        pauseButton = new JButton("Pause");
        resumeButton = new JButton("Resume");
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resumeButton);
        
        // Configuration panel
        JPanel configPanel = createConfigPanel();
        
        panel.add(urlPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(configPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        
        // Thread count
        panel.add(new JLabel("Threads:"));
        JSpinner threadSpinner = new JSpinner(new SpinnerNumberModel(config.getThreadCount(), 1, 20, 1));
        threadSpinner.addChangeListener(e -> config.setThreadCount((Integer) threadSpinner.getValue()));
        panel.add(threadSpinner);
        
        // Max pages
        panel.add(new JLabel("Max Pages:"));
        JSpinner maxPagesSpinner = new JSpinner(new SpinnerNumberModel(config.getMaxPages(), 1, 1000, 10));
        maxPagesSpinner.addChangeListener(e -> config.setMaxPages((Integer) maxPagesSpinner.getValue()));
        panel.add(maxPagesSpinner);
        
        // Max depth
        panel.add(new JLabel("Max Depth:"));
        JSpinner depthSpinner = new JSpinner(new SpinnerNumberModel(config.getMaxDepth(), 1, 10, 1));
        depthSpinner.addChangeListener(e -> config.setMaxDepth((Integer) depthSpinner.getValue()));
        panel.add(depthSpinner);
        
        // Delay
        panel.add(new JLabel("Delay (ms):"));
        JSpinner delaySpinner = new JSpinner(new SpinnerNumberModel(config.getDelayBetweenRequests(), 0, 10000, 100));
        delaySpinner.addChangeListener(e -> config.setDelayBetweenRequests((Integer) delaySpinner.getValue()));
        panel.add(delaySpinner);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crawled Pages"));
        
        // Create table
        String[] columnNames = {"URL", "Title", "Status", "Links", "Response Time (ms)", "Crawl Time"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Status & Logs"));
        
        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready");
        statsLabel = new JLabel("Pages: 0 | Depth: 0 | Queue: 0");
        statusPanel.add(statusLabel);
        statusPanel.add(new JLabel(" | "));
        statusPanel.add(statsLabel);
        
        // Log area
        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        
        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(logScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        startButton.addActionListener(e -> startCrawling());
        stopButton.addActionListener(e -> stopCrawling());
        pauseButton.addActionListener(e -> pauseCrawling());
        resumeButton.addActionListener(e -> resumeCrawling());
        
        // Initial button states
        updateButtonStates();
    }
    
    private void startCrawling() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid URL", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            crawler.startCrawling(url);
            log("Started crawling: " + url);
            updateButtonStates();
        } catch (Exception ex) {
            log("Error starting crawler: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error starting crawler: " + ex.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopCrawling() {
        crawler.stopCrawler();
        log("Stopped crawler");
        updateButtonStates();
    }
    
    private void pauseCrawling() {
        crawler.pauseCrawler();
        log("Paused crawler");
        updateButtonStates();
    }
    
    private void resumeCrawling() {
        crawler.resumeCrawler();
        log("Resumed crawler");
        updateButtonStates();
    }
    
    private void updateButtonStates() {
        boolean isRunning = crawler.isRunning();
        boolean isPaused = crawler.isPaused();
        
        startButton.setEnabled(!isRunning);
        stopButton.setEnabled(isRunning);
        pauseButton.setEnabled(isRunning && !isPaused);
        resumeButton.setEnabled(isRunning && isPaused);
        
        urlField.setEnabled(!isRunning);
    }
    
    private void startStatusUpdates() {
        updateExecutor.scheduleAtFixedRate(() -> {
            try {
                updateStatus();
                updateResultsTable();
            } catch (Exception e) {
                log("Error updating status: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    private void updateStatus() {
        WebCrawler.CrawlingStats stats = crawler.getStats();
        
        SwingUtilities.invokeLater(() -> {
            // Update status label
            if (stats.isRunning()) {
                if (stats.isPaused()) {
                    statusLabel.setText("PAUSED");
                    statusLabel.setForeground(Color.ORANGE);
                } else {
                    statusLabel.setText("RUNNING");
                    statusLabel.setForeground(Color.GREEN);
                }
            } else {
                statusLabel.setText("STOPPED");
                statusLabel.setForeground(Color.RED);
            }
            
            // Update stats label
            statsLabel.setText(String.format("Pages: %d | Depth: %d | Queue: %d | Total URLs: %d", 
                stats.getPagesCrawled(), stats.getCurrentDepth(), stats.getQueueSize(), stats.getTotalUrlsSeen()));
            
            // Update button states
            updateButtonStates();
        });
    }
    
    private void updateResultsTable() {
        Collection<CrawledPage> pages = crawler.getCrawledPages();
        
        SwingUtilities.invokeLater(() -> {
            // Clear existing rows
            tableModel.setRowCount(0);
            
            // Add new rows
            for (CrawledPage page : pages) {
                Object[] row = {
                    page.getUrl(),
                    page.getTitle() != null ? page.getTitle() : "N/A",
                    page.getStatusCode(),
                    page.getLinks().size(),
                    page.getResponseTime(),
                    page.getCrawlTime()
                };
                tableModel.addRow(row);
            }
        });
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + java.time.LocalTime.now() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    public void dispose() {
        if (crawler.isRunning()) {
            crawler.stopCrawler();
        }
        updateExecutor.shutdown();
        super.dispose();
    }
}
