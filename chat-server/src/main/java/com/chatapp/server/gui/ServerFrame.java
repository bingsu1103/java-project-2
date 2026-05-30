package com.chatapp.server.gui;

import com.chatapp.server.network.ChatServer;
import com.chatapp.server.network.ServerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Server GUI window.
 * Displays server config, start/stop controls, connected clients list, and log.
 */
public class ServerFrame extends JFrame implements ChatServer.ServerListener {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // --- UI Components ---
    private JTextField portField;
    private JButton startStopButton;
    private JLabel statusLabel;
    private JLabel clientCountLabel;
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JTextArea logArea;

    // --- Server ---
    private ChatServer chatServer;
    private boolean serverRunning = false;

    public ServerFrame() {
        super("Chat Server");
        initUI();
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onExit();
            }
        });
    }

    private void initUI() {
        setSize(700, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(45, 45, 48));

        // --- Top Panel: Config & Controls ---
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // --- Center: Split between client list and log ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(200);
        splitPane.setBorder(new EmptyBorder(0, 10, 0, 10));

        // Left: Client list
        JPanel clientPanel = createClientPanel();
        splitPane.setLeftComponent(clientPanel);

        // Right: Log area
        JPanel logPanel = createLogPanel();
        splitPane.setRightComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);

        // --- Bottom: Status bar ---
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(new Color(60, 60, 65));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel portLabel = new JLabel("Port:");
        portLabel.setForeground(Color.WHITE);
        portLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(portLabel);

        portField = new JTextField("12345", 8);
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(portField);

        startStopButton = new JButton("▶ Start Server");
        startStopButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startStopButton.setBackground(new Color(46, 160, 67));
        startStopButton.setForeground(Color.WHITE);
        startStopButton.setFocusPainted(false);
        startStopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startStopButton.addActionListener(e -> toggleServer());
        panel.add(startStopButton);

        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(50, 50, 55));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 85)),
                "Connected Clients"
        );
        border.setTitleColor(Color.WHITE);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.setBorder(border);

        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setBackground(new Color(40, 40, 43));
        clientList.setForeground(new Color(200, 200, 200));
        clientList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clientList.setSelectionBackground(new Color(0, 120, 215));

        JScrollPane scrollPane = new JScrollPane(clientList);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        clientCountLabel = new JLabel(" 0 clients");
        clientCountLabel.setForeground(new Color(150, 150, 150));
        clientCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        panel.add(clientCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(50, 50, 55));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 85)),
                "Server Log"
        );
        border.setTitleColor(Color.WHITE);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.setBorder(border);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 200, 83));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Clear log button
        JButton clearBtn = new JButton("Clear Log");
        clearBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearBtn.addActionListener(e -> logArea.setText(""));
        panel.add(clearBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(0, 122, 204));
        panel.setBorder(new EmptyBorder(3, 10, 3, 10));

        statusLabel = new JLabel("● Server Stopped");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(statusLabel);

        return panel;
    }

    // --- Server Control ---

    private void toggleServer() {
        if (!serverRunning) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());
            chatServer = new ChatServer(port);
            chatServer.setListener(this);
            chatServer.start();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to start server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (chatServer != null) {
            chatServer.stop();
        }
    }

    private void onExit() {
        if (serverRunning) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Server is running. Stop server and exit?",
                    "Confirm Exit", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
            stopServer();
        }
        dispose();
        System.exit(0);
    }

    // --- ServerListener callbacks (called from server threads) ---

    @Override
    public void onServerStarted(int port) {
        SwingUtilities.invokeLater(() -> {
            serverRunning = true;
            portField.setEnabled(false);
            startStopButton.setText("■ Stop Server");
            startStopButton.setBackground(new Color(218, 54, 51));
            statusLabel.setText("● Server Running on port " + port);
            appendLog("Server started on port " + port);
        });
    }

    @Override
    public void onServerStopped() {
        SwingUtilities.invokeLater(() -> {
            serverRunning = false;
            portField.setEnabled(true);
            startStopButton.setText("▶ Start Server");
            startStopButton.setBackground(new Color(46, 160, 67));
            statusLabel.setText("● Server Stopped");
            clientListModel.clear();
            updateClientCount();
            appendLog("Server stopped.");
        });
    }

    @Override
    public void onClientConnected(String username) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.addElement(username);
            updateClientCount();
            appendLog("→ " + username + " connected.");
        });
    }

    @Override
    public void onClientDisconnected(String username) {
        SwingUtilities.invokeLater(() -> {
            clientListModel.removeElement(username);
            updateClientCount();
            appendLog("← " + username + " disconnected.");
        });
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> appendLog(message));
    }

    private void appendLog(String message) {
        String time = LocalDateTime.now().format(TIME_FMT);
        logArea.append("[" + time + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void updateClientCount() {
        clientCountLabel.setText(" " + clientListModel.size() + " client(s)");
    }
}
