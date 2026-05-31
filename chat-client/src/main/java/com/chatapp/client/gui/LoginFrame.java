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
    private static final Color BG_DARK = new Color(30, 30, 35);
    private static final Color BG_PANEL = new Color(45, 45, 50);
    private static final Color BG_INPUT = new Color(60, 60, 65);
    private static final Color FG_TEXT = new Color(220, 220, 220);
    private static final Color FG_HINT = new Color(140, 140, 140);
    private static final Color ACCENT_BLUE = new Color(0, 122, 204);
    private static final Color ACCENT_GREEN = new Color(46, 160, 67);
    private static final Color ACCENT_RED = new Color(218, 54, 51);

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
        setSize(420, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // --- Main panel ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Title
        JLabel titleLabel = new JLabel("💬 Chat App");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(ACCENT_BLUE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));

        JLabel subtitleLabel = new JLabel("Connect & Sign In");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(FG_HINT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(25));

        // --- Connection Section ---
        mainPanel.add(createConnectionPanel());
        mainPanel.add(Box.createVerticalStrut(20));

        // --- Separator ---
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(70, 70, 75));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(separator);
        mainPanel.add(Box.createVerticalStrut(20));

        // --- Auth Section ---
        mainPanel.add(createAuthPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);

        JLabel sectionLabel = new JLabel("Server Connection");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sectionLabel.setForeground(FG_TEXT);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(10));

        // Server list combo + manage buttons
        JPanel serverRow = new JPanel(new BorderLayout(6, 0));
        serverRow.setBackground(BG_DARK);
        serverRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        serverRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        serverCombo = new JComboBox<>();
        serverCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        serverCombo.setBackground(BG_INPUT);
        serverCombo.setForeground(FG_TEXT);
        refreshServerCombo();
        serverCombo.addActionListener(e -> onServerSelected());
        serverRow.add(serverCombo, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        btnRow.setBackground(BG_DARK);
        JButton addBtn = createSmallButton("+");
        addBtn.setToolTipText("Add server");
        addBtn.addActionListener(e -> onAddServer());
        JButton editBtn = createSmallButton("✎");
        editBtn.setToolTipText("Edit server");
        editBtn.addActionListener(e -> onEditServer());
        JButton delBtn = createSmallButton("✕");
        delBtn.setToolTipText("Delete server");
        delBtn.addActionListener(e -> onDeleteServer());
        btnRow.add(addBtn);
        btnRow.add(editBtn);
        btnRow.add(delBtn);
        serverRow.add(btnRow, BorderLayout.EAST);

        panel.add(serverRow);
        panel.add(Box.createVerticalStrut(8));

        // Host + Port row
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_DARK);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        hostField = createStyledTextField("localhost");
        hostField.setPreferredSize(new Dimension(200, 38));
        row.add(hostField, BorderLayout.CENTER);

        portField = createStyledTextField("12345");
        portField.setPreferredSize(new Dimension(80, 38));
        row.add(portField, BorderLayout.EAST);

        panel.add(row);
        panel.add(Box.createVerticalStrut(10));

        // Connect button + status
        JPanel connectRow = new JPanel(new BorderLayout(10, 0));
        connectRow.setBackground(BG_DARK);
        connectRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        connectRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        connectButton = createStyledButton("Connect", ACCENT_BLUE);
        connectButton.addActionListener(this::onConnectClicked);
        connectRow.add(connectButton, BorderLayout.WEST);

        connectionStatus = new JLabel("● Disconnected");
        connectionStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        connectionStatus.setForeground(ACCENT_RED);
        connectRow.add(connectionStatus, BorderLayout.CENTER);

        panel.add(connectRow);

        // Auto-select first server
        if (serverCombo.getItemCount() > 0) {
            serverCombo.setSelectedIndex(0);
        }

        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);

        JLabel authLabel = new JLabel("Login");
        authLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        authLabel.setForeground(FG_TEXT);
        authLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(authLabel);
        panel.add(Box.createVerticalStrut(10));

        // Username
        usernameField = createStyledTextField("");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75)),
                new EmptyBorder(8, 12, 8, 12)));
        addPlaceholder(usernameField, "Username");
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(10));

        // Password
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBackground(BG_INPUT);
        passwordField.setForeground(FG_TEXT);
        passwordField.setCaretColor(FG_TEXT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75)),
                new EmptyBorder(8, 12, 8, 12)));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(15));

        // Login button
        loginButton = createStyledButton("Login", ACCENT_GREEN);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.addActionListener(this::onLoginClicked);
        loginButton.setEnabled(false);
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(8));

        // Register button (hidden in login mode)
        registerButton = createStyledButton("Register", ACCENT_BLUE);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(this::onRegisterClicked);
        registerButton.setVisible(false);
        panel.add(registerButton);
        panel.add(Box.createVerticalStrut(12));

        // Message label (for success/error)
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        messageLabel.setForeground(FG_HINT);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(8));

        // Switch mode link
        switchModeButton = new JButton("Don't have an account? Register");
        switchModeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        switchModeButton.setForeground(ACCENT_BLUE);
        switchModeButton.setBackground(BG_DARK);
        switchModeButton.setBorderPainted(false);
        switchModeButton.setContentAreaFilled(false);
        switchModeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchModeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        switchModeButton.addActionListener(e -> toggleMode());
        panel.add(switchModeButton);

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
            switchModeButton.setText("Don't have an account? Register");
        } else {
            loginButton.setVisible(false);
            registerButton.setVisible(true);
            switchModeButton.setText("Already have an account? Login");
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
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(new Color(70, 70, 75));
        btn.setForeground(FG_TEXT);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(28, 28));
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
