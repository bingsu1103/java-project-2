package com.chatapp.client.gui;

import com.chatapp.client.network.ChatClient;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window shown after successful login.
 * Contains: user info, online users list, and chat area (tabs).
 */
public class MainFrame extends JFrame implements ChatClient.MessageListener {

    // --- Colors ---
    private static final Color BG_DARK = new Color(30, 30, 35);
    private static final Color BG_SIDEBAR = new Color(38, 38, 43);
    private static final Color BG_CONTENT = new Color(45, 45, 50);
    private static final Color FG_TEXT = new Color(220, 220, 220);
    private static final Color FG_HINT = new Color(140, 140, 140);
    private static final Color ACCENT_BLUE = new Color(0, 122, 204);
    private static final Color ACCENT_GREEN = new Color(46, 160, 67);
    private static final Color ACCENT_RED = new Color(218, 54, 51);
    private static final Color HOVER_BG = new Color(55, 55, 60);

    // --- UI Components ---
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private JLabel userCountLabel;
    private JLabel statusLabel;
    private JPanel chatAreaPanel; // Placeholder for chat tabs (Phase 7+8)

    // --- State ---
    private ChatClient chatClient;
    private String username;

    public MainFrame(ChatClient chatClient, String username) {
        super("Chat App - " + username);
        this.chatClient = chatClient;
        this.username = username;
        this.chatClient.addListener(this);

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
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        // --- Left Sidebar ---
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // --- Center: Chat Area ---
        chatAreaPanel = createChatArea();
        add(chatAreaPanel, BorderLayout.CENTER);

        // --- Bottom: Status Bar ---
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(BG_SIDEBAR);

        // --- User info header ---
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(50, 50, 55));
        header.setBorder(new EmptyBorder(12, 15, 12, 15));

        JLabel avatarLabel = new JLabel("👤");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        header.add(avatarLabel, BorderLayout.WEST);

        JPanel userInfo = new JPanel();
        userInfo.setLayout(new BoxLayout(userInfo, BoxLayout.Y_AXIS));
        userInfo.setBackground(new Color(50, 50, 55));

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(FG_TEXT);
        userInfo.add(nameLabel);

        JLabel onlineLabel = new JLabel("● Online");
        onlineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        onlineLabel.setForeground(ACCENT_GREEN);
        userInfo.add(onlineLabel);

        header.add(userInfo, BorderLayout.CENTER);

        JButton logoutBtn = new JButton("↩");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        logoutBtn.setToolTipText("Logout");
        logoutBtn.setBackground(new Color(50, 50, 55));
        logoutBtn.setForeground(FG_HINT);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> onExit());
        header.add(logoutBtn, BorderLayout.EAST);

        sidebar.add(header, BorderLayout.NORTH);

        // --- Online Users List ---
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(BG_SIDEBAR);

        JLabel listTitle = new JLabel("  Online Users");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        listTitle.setForeground(FG_HINT);
        listTitle.setBorder(new EmptyBorder(10, 10, 5, 10));
        listPanel.add(listTitle, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(BG_SIDEBAR);
        userList.setForeground(FG_TEXT);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userList.setSelectionBackground(ACCENT_BLUE);
        userList.setSelectionForeground(Color.WHITE);
        userList.setFixedCellHeight(36);
        userList.setCellRenderer(new UserCellRenderer());
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null) {
                        onUserDoubleClicked(selectedUser);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(userList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_SIDEBAR);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        userCountLabel = new JLabel("  0 users online");
        userCountLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        userCountLabel.setForeground(FG_HINT);
        userCountLabel.setBorder(new EmptyBorder(5, 10, 8, 10));
        listPanel.add(userCountLabel, BorderLayout.SOUTH);

        sidebar.add(listPanel, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createChatArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_CONTENT);

        // Placeholder - will be replaced with JTabbedPane in Phase 7/8
        JLabel placeholder = new JLabel("Double-click a user to start chatting", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        placeholder.setForeground(FG_HINT);
        panel.add(placeholder, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(0, 122, 204));
        panel.setBorder(new EmptyBorder(3, 10, 3, 10));

        statusLabel = new JLabel("● Connected to " + chatClient.getHost() + ":" + chatClient.getPort());
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(statusLabel);

        return panel;
    }

    // --- Actions ---

    private void onUserDoubleClicked(String targetUser) {
        // TODO: Open chat tab with targetUser (Phase 7)
        JOptionPane.showMessageDialog(this,
                "Chat with " + targetUser + " - Coming in next phase!",
                "Chat", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onExit() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Logout and exit?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            chatClient.logout();
            dispose();
            System.exit(0);
        }
    }

    // --- ChatClient.MessageListener ---

    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.getType()) {
                case USER_LIST:
                    handleUserList(message.getContent());
                    break;
                case USER_ONLINE:
                    handleUserOnline(message.getContent());
                    break;
                case USER_OFFLINE:
                    handleUserOffline(message.getContent());
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("● Disconnected");
            statusLabel.getParent().setBackground(ACCENT_RED);
            JOptionPane.showMessageDialog(this,
                    "Lost connection to server.", "Disconnected", JOptionPane.ERROR_MESSAGE);
        });
    }

    // --- Message handlers ---

    private void handleUserList(String content) {
        userListModel.clear();
        if (content != null && !content.isEmpty()) {
            String[] users = content.split(",");
            for (String user : users) {
                if (!user.trim().isEmpty()) {
                    userListModel.addElement(user.trim());
                }
            }
        }
        updateUserCount();
    }

    private void handleUserOnline(String user) {
        if (!userListModel.contains(user) && !user.equals(username)) {
            userListModel.addElement(user);
            updateUserCount();
        }
    }

    private void handleUserOffline(String user) {
        userListModel.removeElement(user);
        updateUserCount();
    }

    private void updateUserCount() {
        userCountLabel.setText("  " + userListModel.size() + " user(s) online");
    }

    // --- Custom cell renderer ---

    private class UserCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText("  🟢 " + value.toString());
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setBorder(new EmptyBorder(4, 10, 4, 10));

            if (isSelected) {
                label.setBackground(ACCENT_BLUE);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(BG_SIDEBAR);
                label.setForeground(FG_TEXT);
            }
            label.setOpaque(true);
            return label;
        }
    }
}
