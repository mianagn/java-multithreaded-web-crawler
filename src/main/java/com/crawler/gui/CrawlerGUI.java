package com.crawler.gui;

import com.crawler.core.WebCrawler;
import com.crawler.model.CrawlerConfig;
import com.crawler.model.CrawledPage;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrawlerGUI extends JFrame {
    private final WebCrawler crawler;
    private final CrawlerConfig config;
    
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
        setTitle("🌐 Java Multithreaded Web Crawler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            UIManager.put("Button.background", new Color(46, 204, 113));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.focus", false);
            UIManager.put("Button.select", new Color(46, 204, 113));
            
        } catch (Exception e) {
        }
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(248, 249, 250));
        
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 1),
                "Crawler Controls",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(52, 73, 94)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);
        
        // URL input panel
        JPanel urlPanel = new JPanel(new BorderLayout(10, 0));
        JLabel urlLabel = new JLabel("Start URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        urlLabel.setForeground(new Color(52, 73, 94));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        
        urlField = new JTextField("https://promotelio.gr");
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        urlPanel.add(urlField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        buttonPanel.setOpaque(true);
        buttonPanel.setBackground(new Color(200, 200, 200));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        startButton = createStyledButton("START CRAWLING", new Color(46, 204, 113));
        stopButton = createStyledButton("STOP", new Color(231, 76, 60));
        pauseButton = createStyledButton("PAUSE", new Color(241, 196, 15));
        resumeButton = createStyledButton("RESUME", new Color(52, 152, 219));
        
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(resumeButton);
        
        // Set initial button states
        updateButtonStates();
        
        JPanel configPanel = createConfigPanel();
        
        panel.add(urlPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(configPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 1),
                "Configuration",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(52, 73, 94)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(new Color(245, 247, 250));
        
        // Thread count
        JLabel threadLabel = new JLabel("Threads:");
        threadLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        threadLabel.setForeground(new Color(52, 73, 94));
        panel.add(threadLabel);
        
        JSpinner threadSpinner = createStyledSpinner(config.getThreadCount(), 1, 20, 1);
        threadSpinner.addChangeListener(e -> config.setThreadCount((Integer) threadSpinner.getValue()));
        panel.add(threadSpinner);
        
        JLabel maxPagesLabel = new JLabel("Max Pages:");
        maxPagesLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        maxPagesLabel.setForeground(new Color(52, 73, 94));
        panel.add(maxPagesLabel);
        
        JSpinner maxPagesSpinner = createStyledSpinner(config.getMaxPages(), 1, 1000, 10);
        maxPagesSpinner.addChangeListener(e -> config.setMaxPages((Integer) maxPagesSpinner.getValue()));
        panel.add(maxPagesSpinner);
        
        JLabel depthLabel = new JLabel("Max Depth:");
        depthLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        depthLabel.setForeground(new Color(52, 73, 94));
        panel.add(depthLabel);
        
        JSpinner depthSpinner = createStyledSpinner(config.getMaxDepth(), 1, 10, 1);
        depthSpinner.addChangeListener(e -> config.setMaxDepth((Integer) depthSpinner.getValue()));
        panel.add(depthSpinner);
        
        JLabel delayLabel = new JLabel("Delay (ms):");
        delayLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        delayLabel.setForeground(new Color(52, 73, 94));
        panel.add(delayLabel);
        
        JSpinner delaySpinner = createStyledSpinner((int) config.getDelayBetweenRequests(), 0, 10000, 100);
        delaySpinner.addChangeListener(e -> config.setDelayBetweenRequests((Integer) delaySpinner.getValue()));
        panel.add(delaySpinner);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
                "Crawled Pages",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(52, 73, 94)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);
        
        String[] columnNames = {"URL", "Title", "Status", "Links", "Response Time (ms)", "Crawl Time"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resultsTable.setRowHeight(25);
        resultsTable.setGridColor(new Color(189, 195, 199));
        resultsTable.setSelectionBackground(new Color(52, 152, 219));
        resultsTable.setSelectionForeground(Color.WHITE);
        
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultsTable.getTableHeader().setBackground(new Color(52, 73, 94));
        resultsTable.getTableHeader().setForeground(Color.WHITE);
        resultsTable.getTableHeader().setBorder(BorderFactory.createLineBorder(new Color(52, 73, 94)));
        
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
                "Status & Logs",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                new Color(52, 73, 94)
            ),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(Color.WHITE);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        statusPanel.setBackground(new Color(248, 249, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setForeground(new Color(52, 73, 94));
        
        statsLabel = new JLabel("Pages: 0 | Depth: 0 | Queue: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statsLabel.setForeground(new Color(52, 73, 94));
        
        statusPanel.add(statusLabel);
        statusPanel.add(new JLabel(" | "));
        statusPanel.add(statsLabel);
        
        logArea = new JTextArea(8, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(248, 249, 250));
        logArea.setForeground(new Color(52, 73, 94));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
        
        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(logScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        startButton.addActionListener(e -> startCrawling());
        stopButton.addActionListener(e -> stopCrawling());
        pauseButton.addActionListener(e -> pauseCrawling());
        resumeButton.addActionListener(e -> resumeCrawling());
        
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
        boolean canStart = crawler.canStart();
        
        startButton.setEnabled(canStart);
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
                statusLabel.setForeground(new Color(241, 196, 15));
            } else {
                statusLabel.setText("RUNNING");
                statusLabel.setForeground(new Color(46, 204, 113));
            }
        } else {
            statusLabel.setText("STOPPED");
            statusLabel.setForeground(new Color(231, 76, 60));
        }
        
        statsLabel.setText(String.format("Pages: %d | Depth: %d | Queue: %d | Total URLs: %d", 
            stats.getPagesCrawled(), stats.getCurrentDepth(), stats.getQueueSize(), stats.getTotalUrlsSeen()));
            
        updateButtonStates();
        });
    }
    
    private void updateResultsTable() {
        Collection<CrawledPage> pages = crawler.getCrawledPages();
        
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            
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
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
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
    
    private JButton createStyledButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(backgroundColor.darker(), 2),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.revalidate();
        
        return button;
    }
    
    private JSpinner createStyledSpinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        spinner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return spinner;
    }
}
