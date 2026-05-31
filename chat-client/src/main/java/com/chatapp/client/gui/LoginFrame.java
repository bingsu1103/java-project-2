package com.chatapp.client.gui;

import com.chatapp.client.config.ServerConfigManager;
import com.chatapp.client.config.ServerConfigManager.ServerEntry;
import com.chatapp.client.network.ChatClient;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

/**
 * Login and Registration window for the chat client.
 * Allows connecting to server, login, and register.
 */
public class LoginFrame extends JFrame implements ChatClient.MessageListener {

    // --- Colors ---
    private static final Color BG_DARK = new Color(24, 24, 28);
    private static final Color BG_PANEL = new Color(34, 34, 40);
    private static final Color BG_INPUT = new Color(45, 45, 52);
    private static final Color FG_TEXT = new Color(240, 240, 240);
    private static final Color FG_HINT = new Color(150, 150, 160);
    private static final Color ACCENT_BLUE = new Color(50, 160, 255);
    private static final Color ACCENT_GREEN = new Color(50, 180, 80);
    private static final Color ACCENT_RED = new Color(230, 70, 70);

    // --- UI Components ---
    private JComboBox<String> serverCombo;
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private JLabel connectionStatus;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JButton switchModeButton;
    private JLabel messageLabel;
    private JLabel authLabel;

    private boolean isLoginMode = true;
    private boolean isConnected = false;

    // --- Network ---
    private ChatClient chatClient;
    private String currentUsername;

    // --- Config ---
    private ServerConfigManager configManager;

    // --- Callback ---
    private LoginCallback callback;

    public LoginFrame() {
        super("Chat Application - Login");
        chatClient = new ChatClient();
        chatClient.addListener(this);
        configManager = new ServerConfigManager();
        initUI();
    }

    public void setLoginCallback(LoginCallback callback) {
        this.callback = callback;
    }

    private void initUI() {
        setSize(440, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);
        setLayout(new GridBagLayout()); // Centered panel in the frame

        // --- Centered container panel ---
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.setBackground(BG_DARK);
        containerPanel.setPreferredSize(new Dimension(360, 560));
        containerPanel.setMaximumSize(new Dimension(360, 560));
        containerPanel.setMinimumSize(new Dimension(360, 560));

        // Title
        JLabel titleLabel = new JLabel("💬 Chat App", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(ACCENT_BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(titleLabel);
        containerPanel.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Connect & Sign In", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(FG_HINT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(subtitleLabel);
        containerPanel.add(Box.createVerticalStrut(25));

        // Connection Panel
        JPanel connPanel = createConnectionPanel();
        connPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(connPanel);
        containerPanel.add(Box.createVerticalStrut(15));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(60, 60, 65));
        separator.setMaximumSize(new Dimension(360, 1));
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(separator);
        containerPanel.add(Box.createVerticalStrut(15));

        // Auth Panel
        JPanel authPanel = createAuthPanel();
        authPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        containerPanel.add(authPanel);

        add(containerPanel);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setPreferredSize(new Dimension(360, 185));
        panel.setMaximumSize(new Dimension(360, 185));
        panel.setMinimumSize(new Dimension(360, 185));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        int r = 0;

        JLabel sectionLabel = new JLabel("Server Connection");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sectionLabel.setForeground(FG_TEXT);
        g.gridy = r++; g.insets = new Insets(0, 0, 8, 0);
        panel.add(sectionLabel, g);

        // Server combo
        serverCombo = new JComboBox<>();
        serverCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        serverCombo.setBackground(BG_INPUT);
        serverCombo.setForeground(FG_TEXT);
        refreshServerCombo();
        serverCombo.addActionListener(e -> onServerSelected());
        g.gridy = r++; g.insets = new Insets(0, 0, 6, 0);
        panel.add(serverCombo, g);

        // Manage buttons row using BoxLayout to prevent FlowLayout margins
        JPanel btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setBackground(BG_DARK);
        JButton addBtn = createSmallButton("Add");
        addBtn.addActionListener(e -> onAddServer());
        JButton editBtn = createSmallButton("Edit");
        editBtn.addActionListener(e -> onEditServer());
        JButton delBtn = createSmallButton("Delete");
        delBtn.addActionListener(e -> onDeleteServer());
        btnRow.add(addBtn);
        btnRow.add(Box.createHorizontalStrut(6));
        btnRow.add(editBtn);
        btnRow.add(Box.createHorizontalStrut(6));
        btnRow.add(delBtn);
        g.gridy = r++; g.insets = new Insets(0, 0, 10, 0);
        panel.add(btnRow, g);

        // Host + Port
        JPanel hostPortRow = new JPanel(new BorderLayout(8, 0));
        hostPortRow.setBackground(BG_DARK);
        hostField = createStyledTextField("localhost");
        hostPortRow.add(hostField, BorderLayout.CENTER);
        portField = createStyledTextField("12345");
        portField.setPreferredSize(new Dimension(80, 34));
        hostPortRow.add(portField, BorderLayout.EAST);
        g.gridy = r++; g.insets = new Insets(0, 0, 10, 0);
        panel.add(hostPortRow, g);

        // Connect + status
        JPanel connectRow = new JPanel(new BorderLayout(10, 0));
        connectRow.setBackground(BG_DARK);
        connectButton = createStyledButton("Connect", ACCENT_BLUE);
        connectButton.addActionListener(this::onConnectClicked);
        connectRow.add(connectButton, BorderLayout.WEST);
        connectionStatus = new JLabel("● Disconnected");
        connectionStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        connectionStatus.setForeground(ACCENT_RED);
        connectRow.add(connectionStatus, BorderLayout.CENTER);
        g.gridy = r++; g.insets = new Insets(0, 0, 0, 0);
        panel.add(connectRow, g);

        // Auto-select first server
        if (serverCombo.getItemCount() > 0) {
            serverCombo.setSelectedIndex(0);
        }

        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setPreferredSize(new Dimension(360, 230));
        panel.setMaximumSize(new Dimension(360, 230));
        panel.setMinimumSize(new Dimension(360, 230));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        int r = 0;

        authLabel = new JLabel("Login");
        authLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        authLabel.setForeground(FG_TEXT);
        g.gridy = r++; g.insets = new Insets(0, 0, 8, 0);
        panel.add(authLabel, g);

        // Username
        usernameField = createStyledTextField("");
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75)),
                new EmptyBorder(8, 12, 8, 12)));
        addPlaceholder(usernameField, "Username");
        g.gridy = r++; g.insets = new Insets(0, 0, 8, 0);
        panel.add(usernameField, g);

        // Password
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBackground(BG_INPUT);
        passwordField.setForeground(FG_TEXT);
        passwordField.setCaretColor(FG_TEXT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75)),
                new EmptyBorder(8, 12, 8, 12)));
        g.gridy = r++; g.insets = new Insets(0, 0, 12, 0);
        panel.add(passwordField, g);

        // Login button
        loginButton = createStyledButton("Login", ACCENT_GREEN);
        loginButton.addActionListener(this::onLoginClicked);
        loginButton.setEnabled(false);
        g.gridy = r++; g.insets = new Insets(0, 0, 6, 0);
        panel.add(loginButton, g);

        // Register button (hidden)
        registerButton = createStyledButton("Register", ACCENT_BLUE);
        registerButton.addActionListener(this::onRegisterClicked);
        registerButton.setVisible(false);
        g.gridy = r++; g.insets = new Insets(0, 0, 10, 0);
        panel.add(registerButton, g);

        // Message label
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        messageLabel.setForeground(FG_HINT);
        g.gridy = r++; g.insets = new Insets(0, 0, 6, 0);
        panel.add(messageLabel, g);

        // Switch mode link
        switchModeButton = new JButton("<html><u>Don't have an account? Register</u></html>");
        switchModeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        switchModeButton.setForeground(new Color(80, 200, 255));
        switchModeButton.setBackground(BG_DARK);
        switchModeButton.setBorderPainted(false);
        switchModeButton.setContentAreaFilled(false);
        switchModeButton.setOpaque(false);
        switchModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchModeButton.setHorizontalAlignment(SwingConstants.CENTER);
        switchModeButton.addActionListener(e -> toggleMode());
        g.gridy = r++; g.insets = new Insets(0, 0, 0, 0);
        panel.add(switchModeButton, g);

        return panel;
    }

    // --- Actions ---

    private void onConnectClicked(ActionEvent e) {
        if (isConnected) {
            chatClient.disconnect();
            setDisconnectedState();
            return;
        }

        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            showMessage("Please enter host and port.", ACCENT_RED);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            connectButton.setEnabled(false);
            connectButton.setText("Connecting...");

            // Connect in background thread
            new Thread(() -> {
                try {
                    chatClient.connect(host, port);
                    SwingUtilities.invokeLater(this::setConnectedState);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showMessage("Connection failed: " + ex.getMessage(), ACCENT_RED);
                        connectButton.setEnabled(true);
                        connectButton.setText("Connect");
                    });
                }
            }).start();

        } catch (NumberFormatException ex) {
            showMessage("Invalid port number.", ACCENT_RED);
        }
    }

    private void onLoginClicked(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter username and password.", ACCENT_RED);
            return;
        }

        currentUsername = username;
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        chatClient.login(username, password);
    }

    private void onRegisterClicked(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter username and password.", ACCENT_RED);
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        chatClient.register(username, password);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            loginButton.setVisible(true);
            registerButton.setVisible(false);
            if (authLabel != null) authLabel.setText("Login");
            switchModeButton.setText("<html><u>Don't have an account? Register</u></html>");
        } else {
            loginButton.setVisible(false);
            registerButton.setVisible(true);
            if (authLabel != null) authLabel.setText("Register");
            switchModeButton.setText("<html><u>Already have an account? Login</u></html>");
        }
        messageLabel.setText(" ");
    }

    // --- State changes ---

    private void setConnectedState() {
        isConnected = true;
        connectButton.setText("Disconnect");
        connectButton.setBackground(ACCENT_RED);
        connectButton.setEnabled(true);
        connectionStatus.setText("● Connected");
        connectionStatus.setForeground(ACCENT_GREEN);
        hostField.setEnabled(false);
        portField.setEnabled(false);
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        showMessage("Connected to server!", ACCENT_GREEN);
    }

    private void setDisconnectedState() {
        isConnected = false;
        connectButton.setText("Connect");
        connectButton.setBackground(ACCENT_BLUE);
        connectButton.setEnabled(true);
        connectionStatus.setText("● Disconnected");
        connectionStatus.setForeground(ACCENT_RED);
        hostField.setEnabled(true);
        portField.setEnabled(true);
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        showMessage("Disconnected from server.", FG_HINT);
    }

    private void showMessage(String text, Color color) {
        messageLabel.setText(text);
        messageLabel.setForeground(color);
    }

    // --- ChatClient.MessageListener ---

    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case LOGIN_SUCCESS:
                    showMessage("Login successful!", ACCENT_GREEN);
                    if (callback != null) {
                        callback.onLoginSuccess(chatClient, currentUsername);
                    }
                    break;

                case LOGIN_FAIL:
                    showMessage(message.getContent(), ACCENT_RED);
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    break;

                case REGISTER_SUCCESS:
                    showMessage(message.getContent(), ACCENT_GREEN);
                    registerButton.setEnabled(true);
                    registerButton.setText("Register");
                    // Auto switch to login mode
                    if (!isLoginMode) {
                        toggleMode();
                    }
                    break;

                case REGISTER_FAIL:
                    showMessage(message.getContent(), ACCENT_RED);
                    registerButton.setEnabled(true);
                    registerButton.setText("Register");
                    break;

                default:
                    break;
            }
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            setDisconnectedState();
            showMessage("Lost connection to server.", ACCENT_RED);
        });
    }

    // --- Server management ---

    private void refreshServerCombo() {
        serverCombo.removeAllItems();
        for (ServerEntry entry : configManager.getServers()) {
            serverCombo.addItem(entry.toString());
        }
    }

    private void onServerSelected() {
        int idx = serverCombo.getSelectedIndex();
        if (idx >= 0) {
            ServerEntry entry = configManager.getServer(idx);
            if (entry != null) {
                hostField.setText(entry.getHost());
                portField.setText(String.valueOf(entry.getPort()));
            }
        }
    }

    private void onAddServer() {
        String name = JOptionPane.showInputDialog(this, "Server name:", "Add Server", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        String host = JOptionPane.showInputDialog(this, "Host:", "localhost");
        if (host == null || host.trim().isEmpty()) return;
        String portStr = JOptionPane.showInputDialog(this, "Port:", "12345");
        if (portStr == null) return;
        try {
            int port = Integer.parseInt(portStr.trim());
            configManager.addServer(name.trim(), host.trim(), port);
            refreshServerCombo();
            serverCombo.setSelectedIndex(serverCombo.getItemCount() - 1);
        } catch (NumberFormatException ex) {
            showMessage("Invalid port number.", ACCENT_RED);
        }
    }

    private void onEditServer() {
        int idx = serverCombo.getSelectedIndex();
        if (idx < 0) return;
        ServerEntry entry = configManager.getServer(idx);
        if (entry == null) return;

        String name = JOptionPane.showInputDialog(this, "Server name:", entry.getName());
        if (name == null || name.trim().isEmpty()) return;
        String host = JOptionPane.showInputDialog(this, "Host:", entry.getHost());
        if (host == null || host.trim().isEmpty()) return;
        String portStr = JOptionPane.showInputDialog(this, "Port:", String.valueOf(entry.getPort()));
        if (portStr == null) return;
        try {
            int port = Integer.parseInt(portStr.trim());
            configManager.updateServer(idx, name.trim(), host.trim(), port);
            refreshServerCombo();
            serverCombo.setSelectedIndex(idx);
        } catch (NumberFormatException ex) {
            showMessage("Invalid port number.", ACCENT_RED);
        }
    }

    private void onDeleteServer() {
        int idx = serverCombo.getSelectedIndex();
        if (idx < 0) return;
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this server?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            configManager.removeServer(idx);
            refreshServerCombo();
            if (serverCombo.getItemCount() > 0) {
                serverCombo.setSelectedIndex(0);
            }
        }
    }

    // --- Helper methods ---

    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(BG_INPUT);
        field.setForeground(FG_TEXT);
        field.setCaretColor(FG_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75)),
                new EmptyBorder(8, 12, 8, 12)));
        return field;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 20, 8, 20));
        button.setOpaque(true);
        return button;
    }

    private JButton createSmallButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setBackground(new Color(70, 70, 75));
        btn.setForeground(new Color(200, 200, 200));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.setOpaque(true);
        return btn;
    }

    private void addPlaceholder(JTextField field, String placeholder) {
        field.setToolTipText(placeholder);
        if (field.getText().isEmpty()) {
            field.setText(placeholder);
            field.setForeground(FG_HINT);
        }
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(FG_TEXT);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(FG_HINT);
                }
            }
        });
    }

    /**
     * Callback interface for successful login.
     */
    public interface LoginCallback {
        void onLoginSuccess(ChatClient client, String username);
    }
}
