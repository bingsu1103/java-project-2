package com.chatapp.client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Chat panel for a single conversation (1-to-1 or group).
 * Contains: message display area, input area, send button.
 */
public class ChatPanel extends JPanel {

    // --- Colors ---
    private static final Color BG_DARK = new Color(40, 40, 45);
    private static final Color BG_INPUT = new Color(55, 55, 60);
    private static final Color BG_MSG_SELF = new Color(0, 102, 180);
    private static final Color BG_MSG_OTHER = new Color(60, 60, 65);
    private static final Color FG_TEXT = new Color(220, 220, 220);
    private static final Color FG_HINT = new Color(140, 140, 140);
    private static final Color FG_TIME = new Color(160, 160, 165);
    private static final Color ACCENT_BLUE = new Color(0, 122, 204);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // --- UI Components ---
    private JTextPane messageArea;
    private JTextArea inputArea;
    private JButton sendButton;
    private JCheckBox enterToSendCheckbox;

    // --- State ---
    private String myUsername;
    private String targetName; // User or group name
    private boolean enterToSend = true;

    // --- Callback ---
    private ChatSendListener sendListener;

    public ChatPanel(String myUsername, String targetName) {
        this.myUsername = myUsername;
        this.targetName = targetName;
        initUI();
    }

    public void setSendListener(ChatSendListener listener) {
        this.sendListener = listener;
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_DARK);

        // --- Top: Chat header ---
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // --- Center: Message display ---
        messageArea = new JTextPane();
        messageArea.setEditable(false);
        messageArea.setBackground(BG_DARK);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane msgScroll = new JScrollPane(messageArea);
        msgScroll.setBorder(null);
        msgScroll.getViewport().setBackground(BG_DARK);
        msgScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(msgScroll, BorderLayout.CENTER);

        // --- Bottom: Input area ---
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(50, 50, 55));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel nameLabel = new JLabel("💬 " + targetName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(FG_TEXT);
        header.add(nameLabel, BorderLayout.WEST);

        JLabel statusLabel = new JLabel("Online ●");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(46, 160, 67));
        header.add(statusLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(50, 50, 55));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Input text area (supports multi-line)
        inputArea = new JTextArea(2, 1);
        inputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputArea.setBackground(BG_INPUT);
        inputArea.setForeground(FG_TEXT);
        inputArea.setCaretColor(FG_TEXT);
        inputArea.setCaretColor(FG_TEXT);
        inputArea.setSelectionColor(new Color(50, 160, 255, 120)); // Beautiful translucent blue
        inputArea.setSelectedTextColor(Color.WHITE);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Support macOS Cmd shortcut mappings (Cmd+C, Cmd+V, Cmd+X, Cmd+A)
        InputMap im = inputArea.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke("meta C"), javax.swing.text.DefaultEditorKit.copyAction);
        im.put(KeyStroke.getKeyStroke("meta V"), javax.swing.text.DefaultEditorKit.pasteAction);
        im.put(KeyStroke.getKeyStroke("meta X"), javax.swing.text.DefaultEditorKit.cutAction);
        im.put(KeyStroke.getKeyStroke("meta A"), javax.swing.text.DefaultEditorKit.selectAllAction);
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (enterToSend && !e.isShiftDown()) {
                        e.consume();
                        doSend();
                    } else if (!enterToSend && e.isShiftDown()) {
                        e.consume();
                        doSend();
                    }
                    // Otherwise: default behavior (new line)
                }
            }
        });

        // Left side: emoji + file buttons
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(50, 50, 55));

        JButton emojiButton = new JButton("😀");
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        emojiButton.setBackground(new Color(50, 50, 55));
        emojiButton.setForeground(FG_TEXT);
        emojiButton.setBorderPainted(false);
        emojiButton.setFocusPainted(false);
        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        emojiButton.setToolTipText("Insert emoji");

        EmojiPicker emojiPicker = new EmojiPicker();
        emojiPicker.setEmojiSelectListener(emoji -> {
            inputArea.insert(emoji, inputArea.getCaretPosition());
            inputArea.requestFocusInWindow();
        });
        emojiButton.addActionListener(e -> {
            emojiPicker.show(emojiButton, 0, -emojiPicker.getPreferredSize().height);
        });
        leftPanel.add(emojiButton);

        JButton fileButton = new JButton("📎");
        fileButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        fileButton.setBackground(new Color(50, 50, 55));
        fileButton.setForeground(FG_TEXT);
        fileButton.setBorderPainted(false);
        fileButton.setFocusPainted(false);
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileButton.setToolTipText("Send file");
        fileButton.addActionListener(e -> doSendFile());
        leftPanel.add(fileButton);

        panel.add(leftPanel, BorderLayout.WEST);

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75)));
        inputScroll.setPreferredSize(new Dimension(0, 55));
        panel.add(inputScroll, BorderLayout.CENTER);

        // Right side: send button + enter option
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(50, 50, 55));

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(ACCENT_BLUE);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.setBorder(new EmptyBorder(8, 18, 8, 18));
        sendButton.setOpaque(true);
        sendButton.addActionListener(e -> doSend());
        rightPanel.add(sendButton);

        rightPanel.add(Box.createVerticalStrut(4));

        enterToSendCheckbox = new JCheckBox("Enter=Send", true);
        enterToSendCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        enterToSendCheckbox.setForeground(FG_HINT);
        enterToSendCheckbox.setBackground(new Color(50, 50, 55));
        enterToSendCheckbox.addActionListener(e -> {
            enterToSend = enterToSendCheckbox.isSelected();
        });
        rightPanel.add(enterToSendCheckbox);

        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    // --- Send message ---

    private void doSend() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        // Display own message
        appendMessage(myUsername, text, true);

        // Notify listener
        if (sendListener != null) {
            sendListener.onSendMessage(targetName, text);
        }

        inputArea.setText("");
        inputArea.requestFocusInWindow();
    }

    // --- Display messages ---

    /**
     * Append a message to the chat display.
     */
    public void appendMessage(String sender, String text, boolean isSelf) {
        String time = LocalDateTime.now().format(TIME_FMT);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        // Bubble panel with rounded corners
        JPanel bubble = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setBackground(isSelf ? BG_MSG_SELF : BG_MSG_OTHER);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Header (Sender + Time)
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);
        JLabel senderLabel = new JLabel(isSelf ? "You" : sender);
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        senderLabel.setForeground(isSelf ? new Color(200, 230, 255) : new Color(255, 180, 100));
        
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(isSelf ? new Color(180, 210, 255) : FG_HINT);
        
        headerPanel.add(senderLabel, BorderLayout.WEST);
        headerPanel.add(timeLabel, BorderLayout.EAST);
        bubble.add(headerPanel, BorderLayout.NORTH);

        // Message text
        JTextArea textArea = new JTextArea(text);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setForeground(FG_TEXT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setSelectionColor(new Color(255, 255, 255, 80));
        textArea.setSelectedTextColor(Color.WHITE);
        textArea.setBorder(null);

        // Constrain text width for multi-line bubbles
        int maxWidth = 350;
        textArea.setSize(new Dimension(maxWidth, Short.MAX_VALUE));
        Dimension pref = textArea.getPreferredSize();
        if (pref.width > maxWidth) {
            textArea.setPreferredSize(new Dimension(maxWidth, textArea.getPreferredSize().height));
        }

        bubble.add(textArea, BorderLayout.CENTER);

        JPanel alignPanel = new JPanel(new FlowLayout(isSelf ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 2));
        alignPanel.setOpaque(false);
        alignPanel.add(bubble);

        wrapper.add(alignPanel, BorderLayout.CENTER);

        // Add macOS command shortcuts for textArea
        InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke("meta C"), javax.swing.text.DefaultEditorKit.copyAction);
        im.put(KeyStroke.getKeyStroke("meta A"), javax.swing.text.DefaultEditorKit.selectAllAction);

        try {
            StyledDocument doc = messageArea.getStyledDocument();
            messageArea.setCaretPosition(doc.getLength());
            messageArea.insertComponent(wrapper);
            doc.insertString(doc.getLength(), "\n", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    /**
     * Append a system message (e.g., user joined, error).
     */
    public void appendSystemMessage(String text) {
        StyledDocument doc = messageArea.getStyledDocument();
        try {
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, FG_HINT);
            StyleConstants.setItalic(style, true);
            StyleConstants.setFontSize(style, 12);
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_CENTER);

            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", style);
            }
            doc.insertString(doc.getLength(), "— " + text + " —\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        messageArea.setCaretPosition(doc.getLength());
    }

    // --- File transfer ---

    private void doSendFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select file to send");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (file.length() > 10 * 1024 * 1024) { // 10MB limit
            JOptionPane.showMessageDialog(this,
                    "File too large (max 10MB).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            appendSystemMessage("Sending file: " + file.getName() + " (" + formatFileSize(file.length()) + ")");

            if (sendListener != null) {
                sendListener.onSendFile(targetName, file.getName(), data);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to read file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Display a received file message with a save button.
     */
    public void appendFileMessage(String sender, String fileName, long fileSize, byte[] fileData, boolean isSelf) {
        StyledDocument doc = messageArea.getStyledDocument();
        try {
            String time = LocalDateTime.now().format(TIME_FMT);

            SimpleAttributeSet senderStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(senderStyle, isSelf ? new Color(100, 180, 255) : new Color(255, 180, 100));
            StyleConstants.setBold(senderStyle, true);
            StyleConstants.setFontSize(senderStyle, 12);

            SimpleAttributeSet timeStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(timeStyle, FG_TIME);
            StyleConstants.setFontSize(timeStyle, 10);

            SimpleAttributeSet fileStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(fileStyle, new Color(100, 200, 255));
            StyleConstants.setFontSize(fileStyle, 13);

            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", fileStyle);
            }

            String senderLabel = isSelf ? "You" : sender;
            doc.insertString(doc.getLength(), senderLabel, senderStyle);
            doc.insertString(doc.getLength(), "  " + time + "\n", timeStyle);
            doc.insertString(doc.getLength(), "📎 " + fileName + " (" + formatFileSize(fileSize) + ")\n", fileStyle);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        messageArea.setCaretPosition(doc.getLength());

        // Add save button if received (not self)
        if (!isSelf && fileData != null) {
            JButton saveBtn = new JButton("💾 Save File");
            saveBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            saveBtn.setBackground(new Color(60, 60, 65));
            saveBtn.setForeground(new Color(100, 200, 255));
            saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveBtn.addActionListener(e -> saveReceivedFile(fileName, fileData));

            // Insert button into text pane
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
            messageArea.insertComponent(saveBtn);

            try {
                StyledDocument d = messageArea.getStyledDocument();
                d.insertString(d.getLength(), "\n", new SimpleAttributeSet());
            } catch (BadLocationException ex) {
                // ignore
            }
        }
    }

    private void saveReceivedFile(String fileName, byte[] data) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                fos.write(data);
                appendSystemMessage("File saved: " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // --- Getters ---

    public String getTargetName() {
        return targetName;
    }

    /**
     * Callback for when user sends a message or file.
     */
    public interface ChatSendListener {
        void onSendMessage(String target, String text);
        void onSendFile(String target, String fileName, byte[] data);
    }
}
