package com.chatapp.client.gui;

import com.chatapp.client.network.ChatClient;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;
import com.chatapp.common.protocol.Protocol;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

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
    private JTabbedPane chatTabs;
    private final HashMap<String, ChatPanel> openChats = new HashMap<>();
    private final HashMap<String, String> groupNames = new HashMap<>();

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

        // --- Center: Chat Tabs ---
        chatTabs = createChatTabs();
        add(chatTabs, BorderLayout.CENTER);

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
        logoutBtn.addActionListener(e -> onLogout());
        header.add(logoutBtn, BorderLayout.EAST);

        sidebar.add(header, BorderLayout.NORTH);

        // --- Online Users List ---
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(BG_SIDEBAR);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BG_SIDEBAR);

        JLabel listTitle = new JLabel("  Online Users");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        listTitle.setForeground(FG_HINT);
        listTitle.setBorder(new EmptyBorder(10, 10, 5, 10));
        
        JButton createGroupBtn = new JButton("👥 New Group");
        createGroupBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        createGroupBtn.setForeground(FG_TEXT);
        createGroupBtn.setBackground(new Color(60, 60, 65));
        createGroupBtn.setFocusPainted(false);
        createGroupBtn.setBorder(new EmptyBorder(4, 8, 4, 8));
        createGroupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createGroupBtn.addActionListener(e -> onCreateGroupClicked());
        
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(createGroupBtn);
        
        titlePanel.add(listTitle, BorderLayout.WEST);
        titlePanel.add(btnPanel, BorderLayout.EAST);
        listPanel.add(titlePanel, BorderLayout.NORTH);

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

    private JTabbedPane createChatTabs() {
        UIManager.put("TabbedPane.background", BG_SIDEBAR);
        UIManager.put("TabbedPane.foreground", FG_TEXT);
        UIManager.put("TabbedPane.selected", BG_CONTENT);
        UIManager.put("TabbedPane.contentAreaColor", BG_CONTENT);
        UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(BG_SIDEBAR);
        tabs.setForeground(FG_TEXT);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Welcome tab
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(BG_CONTENT);
        JLabel placeholder = new JLabel("Double-click a user to start chatting", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        placeholder.setForeground(FG_HINT);
        welcomePanel.add(placeholder, BorderLayout.CENTER);
        tabs.addTab("Welcome", welcomePanel);

        return tabs;
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
        openChatTab(targetUser);
    }

    private void onCreateGroupClicked() {
        String groupName = JOptionPane.showInputDialog(this, "Enter Group Name:", "New Group", JOptionPane.PLAIN_MESSAGE);
        if (groupName == null || groupName.trim().isEmpty()) return;
        
        Object[] users = new Object[userListModel.size()];
        for (int i = 0; i < userListModel.size(); i++) {
            users[i] = userListModel.getElementAt(i);
        }
        
        if (users.length == 0) {
            JOptionPane.showMessageDialog(this, "No other users online to add.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JList<Object> list = new JList<>(users);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(200, 150));
        
        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Select Members", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            java.util.List<Object> selected = list.getSelectedValuesList();
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "You must select at least one member.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            StringBuilder members = new StringBuilder(username);
            for (Object obj : selected) {
                members.append(",").append(obj.toString());
            }
            
            Message createMsg = new Message(MessageType.GROUP_CREATE, username, members.toString(), groupName);
            chatClient.sendMessage(createMsg);
        }
    }

    /**
     * Open or focus a chat tab for the given user.
     */
    private void openChatTab(String targetUser) {
        // If tab already open, switch to it
        if (openChats.containsKey(targetUser)) {
            ChatPanel existing = openChats.get(targetUser);
            int index = chatTabs.indexOfComponent(existing);
            if (index >= 0) {
                chatTabs.setSelectedIndex(index);
            }
            return;
        }

        // Create new ChatPanel
        ChatPanel chatPanel = new ChatPanel(username, targetUser);
        chatPanel.setSendListener(new ChatPanel.ChatSendListener() {
            @Override
            public void onSendMessage(String target, String text) {
                chatClient.sendTextMessage(username, target, text);
            }

            @Override
            public void onSendFile(String target, String fileName, byte[] data) {
                Message fileMsg = new Message(MessageType.FILE_SEND, username, target, null);
                fileMsg.setFileName(fileName);
                fileMsg.setFileSize(data.length);
                fileMsg.setFileData(data);
                chatClient.sendMessage(fileMsg);
            }
        });

        // Add tab with close button
        openChats.put(targetUser, chatPanel);
        chatTabs.addTab(targetUser, chatPanel);
        int tabIndex = chatTabs.indexOfComponent(chatPanel);
        chatTabs.setTabComponentAt(tabIndex, createTabHeader(targetUser, targetUser));
        chatTabs.setSelectedIndex(tabIndex);
    }

    private void openGroupChatTab(String groupId, String groupName) {
        if (openChats.containsKey(groupId)) {
            ChatPanel existing = openChats.get(groupId);
            int index = chatTabs.indexOfComponent(existing);
            if (index >= 0) chatTabs.setSelectedIndex(index);
            return;
        }

        ChatPanel chatPanel = new ChatPanel(username, groupName + " (Group)");
        chatPanel.setSendListener(new ChatPanel.ChatSendListener() {
            @Override
            public void onSendMessage(String target, String text) {
                Message msg = new Message(MessageType.GROUP_TEXT, username, groupId, text);
                chatClient.sendMessage(msg);
            }
            @Override
            public void onSendFile(String target, String fileName, byte[] data) {
                // File to group not fully implemented yet
                JOptionPane.showMessageDialog(MainFrame.this, "File sending to groups coming soon!");
            }
        });

        openChats.put(groupId, chatPanel);
        chatTabs.addTab(groupName, chatPanel);
        int tabIndex = chatTabs.indexOfComponent(chatPanel);
        chatTabs.setTabComponentAt(tabIndex, createTabHeader(groupName, groupId));
        chatTabs.setSelectedIndex(tabIndex);
    }

    /**
     * Create a tab header with title and close button.
     */
    private JPanel createTabHeader(String title, String targetId) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        header.setOpaque(false);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(FG_TEXT); // Fix contrast
        header.add(label);

        JButton closeBtn = new JButton("x");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setForeground(FG_HINT);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setMargin(new Insets(0, 0, 0, 0));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(20, 20));
        closeBtn.addActionListener(e -> closeChatTab(targetId));
        header.add(closeBtn);

        return header;
    }

    /**
     * Close a chat tab.
     */
    private void closeChatTab(String targetUser) {
        ChatPanel panel = openChats.remove(targetUser);
        if (panel != null) {
            int index = chatTabs.indexOfComponent(panel);
            if (index >= 0) {
                chatTabs.removeTabAt(index);
            }
        }
    }

    private void onLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            chatClient.logout();
            dispose();
            com.chatapp.client.ClientApp.showLoginFrame();
        }
    }

    private void onExit() {
        chatClient.logout();
        dispose();
        System.exit(0);
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
                case TEXT:
                    handleIncomingText(message);
                    break;
                case GROUP_CREATE_SUCCESS:
                    String groupId = message.getReceiver();
                    String groupName = message.getContent();
                    groupNames.put(groupId, groupName);
                    openGroupChatTab(groupId, groupName);
                    break;
                case GROUP_TEXT:
                    handleIncomingGroupText(message);
                    break;
                case FILE_RECEIVE:
                    handleIncomingFile(message);
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

    private void handleIncomingText(Message message) {
        String sender = message.getSender();
        // Auto-open tab if not open
        if (!openChats.containsKey(sender)) {
            openChatTab(sender);
        }
        ChatPanel panel = openChats.get(sender);
        if (panel != null) {
            panel.appendMessage(sender, message.getContent(), false);
            // Flash tab if not currently selected
            int tabIndex = chatTabs.indexOfComponent(panel);
            if (tabIndex >= 0 && chatTabs.getSelectedIndex() != tabIndex) {
                chatTabs.setBackgroundAt(tabIndex, ACCENT_BLUE);
            }
        }
    }

    private void handleIncomingGroupText(Message message) {
        String groupId = message.getReceiver();
        if (!openChats.containsKey(groupId)) {
            String groupName = groupNames.getOrDefault(groupId, "Group Chat");
            openGroupChatTab(groupId, groupName);
        }
        ChatPanel panel = openChats.get(groupId);
        if (panel != null) {
            panel.appendMessage(message.getSender(), message.getContent(), false);
            int tabIndex = chatTabs.indexOfComponent(panel);
            if (tabIndex >= 0 && chatTabs.getSelectedIndex() != tabIndex) {
                chatTabs.setBackgroundAt(tabIndex, ACCENT_BLUE);
            }
        }
    }

    private void handleIncomingFile(Message message) {
        String sender = message.getSender();
        if (!openChats.containsKey(sender)) {
            openChatTab(sender);
        }
        ChatPanel panel = openChats.get(sender);
        if (panel != null) {
            panel.appendFileMessage(sender, message.getFileName(),
                    message.getFileSize(), message.getFileData(), false);
            int tabIndex = chatTabs.indexOfComponent(panel);
            if (tabIndex >= 0 && chatTabs.getSelectedIndex() != tabIndex) {
                chatTabs.setBackgroundAt(tabIndex, ACCENT_BLUE);
            }
        }
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
