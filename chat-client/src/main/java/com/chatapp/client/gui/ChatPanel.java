package com.chatapp.client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(8, 12, 8, 12));
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

        // Left side: emoji button
        JButton emojiButton = new JButton("😀");
        emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        emojiButton.setBackground(new Color(50, 50, 55));
        emojiButton.setForeground(FG_TEXT);
        emojiButton.setBorderPainted(false);
        emojiButton.setFocusPainted(false);
        emojiButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        emojiButton.setPreferredSize(new Dimension(42, 55));
        emojiButton.setToolTipText("Insert emoji");

        EmojiPicker emojiPicker = new EmojiPicker();
        emojiPicker.setEmojiSelectListener(emoji -> {
            inputArea.insert(emoji, inputArea.getCaretPosition());
            inputArea.requestFocusInWindow();
        });

        emojiButton.addActionListener(e -> {
            emojiPicker.show(emojiButton, 0, -emojiPicker.getPreferredSize().height);
        });
        panel.add(emojiButton, BorderLayout.WEST);

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
        StyledDocument doc = messageArea.getStyledDocument();

        try {
            // Timestamp
            String time = LocalDateTime.now().format(TIME_FMT);

            // Sender label style
            SimpleAttributeSet senderStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(senderStyle, isSelf ? new Color(100, 180, 255) : new Color(255, 180, 100));
            StyleConstants.setBold(senderStyle, true);
            StyleConstants.setFontSize(senderStyle, 12);

            // Message text style
            SimpleAttributeSet textStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(textStyle, FG_TEXT);
            StyleConstants.setFontSize(textStyle, 14);

            // Time style
            SimpleAttributeSet timeStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(timeStyle, FG_TIME);
            StyleConstants.setFontSize(timeStyle, 10);

            // Add spacing between messages
            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", textStyle);
            }

            // Insert: [sender] [time]
            String senderLabel = isSelf ? "You" : sender;
            doc.insertString(doc.getLength(), senderLabel, senderStyle);
            doc.insertString(doc.getLength(), "  " + time + "\n", timeStyle);

            // Insert: message text
            doc.insertString(doc.getLength(), text + "\n", textStyle);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // Auto-scroll to bottom
        messageArea.setCaretPosition(doc.getLength());
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

    // --- Getters ---

    public String getTargetName() {
        return targetName;
    }

    /**
     * Callback for when user sends a message.
     */
    public interface ChatSendListener {
        void onSendMessage(String target, String text);
    }
}
