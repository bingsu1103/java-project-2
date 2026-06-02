package com.chatapp.client.gui;

import com.chatapp.client.media.VoiceCallManager;
import com.chatapp.client.media.VideoCallManager;
import com.chatapp.client.network.ChatClient;
import com.chatapp.common.model.Message;
import com.chatapp.common.protocol.MessageType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Modern Swing Dialog representing an active voice or video call.
 * Manages UI feedback, Call signal response, and media pipelines.
 */
public class CallDialog extends JDialog {
    private static final Color BG_DARK = new Color(30, 30, 35);
    private static final Color FG_TEXT = new Color(220, 220, 220);
    private static final Color ACCENT_GREEN = new Color(46, 160, 67);
    private static final Color ACCENT_RED = new Color(218, 54, 51);

    private final ChatClient chatClient;
    private final String myUsername;
    private final String targetUser;
    private final boolean isVideo;
    private final boolean isIncoming;

    private JLabel statusLabel;
    private JPanel buttonPanel;
    private JButton hangUpBtn;
    private JButton acceptBtn;
    private JButton rejectBtn;

    private VoiceCallManager voiceCallManager;
    private VideoCallManager videoCallManager;

    private boolean isConnected = false;

    public CallDialog(JFrame parent, ChatClient chatClient, String myUsername, String targetUser, boolean isVideo, boolean isIncoming) {
        super(parent, (isVideo ? "Video Call" : "Voice Call") + " - " + targetUser, false);
        this.chatClient = chatClient;
        this.myUsername = myUsername;
        this.targetUser = targetUser;
        this.isVideo = isVideo;
        this.isIncoming = isIncoming;

        if (isVideo) {
            videoCallManager = new VideoCallManager(chatClient, myUsername, targetUser);
        } else {
            voiceCallManager = new VoiceCallManager(chatClient, myUsername, targetUser);
        }

        initUI();
    }

    private void initUI() {
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // Header status info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(20, 20, 15, 20));

        JLabel titleLabel = new JLabel((isVideo ? "📹 Video Call" : "📞 Voice Call") + " with " + targetUser, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(FG_TEXT);
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel(isIncoming ? "Incoming call..." : "Calling...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        statusLabel.setForeground(new Color(150, 150, 155));
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        headerPanel.add(statusLabel, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        if (isVideo) {
            JPanel videoGrid = new JPanel(new GridLayout(1, 2, 10, 0));
            videoGrid.setOpaque(false);
            videoGrid.setBorder(new EmptyBorder(10, 20, 10, 20));
            videoGrid.add(videoCallManager.getLocalPanel());
            videoGrid.add(videoCallManager.getRemotePanel());
            add(videoGrid, BorderLayout.CENTER);
            setSize(700, 420);
        } else {
            // Animated voice screen placeholder
            JPanel voiceScreen = new JPanel(new GridBagLayout());
            voiceScreen.setOpaque(false);
            JLabel avatar = new JLabel("👤");
            avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
            avatar.setForeground(new Color(100, 150, 200));
            voiceScreen.add(avatar);
            add(voiceScreen, BorderLayout.CENTER);
            setSize(360, 300);
        }

        // Bottom Controls
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        if (isIncoming) {
            acceptBtn = new JButton("Accept");
            acceptBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            acceptBtn.setBackground(ACCENT_GREEN);
            acceptBtn.setForeground(Color.WHITE);
            acceptBtn.setOpaque(true);
            acceptBtn.setBorderPainted(false);
            acceptBtn.setFocusPainted(false);
            acceptBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            acceptBtn.addActionListener(e -> onAcceptCall());
            buttonPanel.add(acceptBtn);

            rejectBtn = new JButton("Reject");
            rejectBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            rejectBtn.setBackground(ACCENT_RED);
            rejectBtn.setForeground(Color.WHITE);
            rejectBtn.setOpaque(true);
            rejectBtn.setBorderPainted(false);
            rejectBtn.setFocusPainted(false);
            rejectBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            rejectBtn.addActionListener(e -> onRejectCall());
            buttonPanel.add(rejectBtn);
        } else {
            hangUpBtn = new JButton("Hang Up");
            hangUpBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
            hangUpBtn.setBackground(ACCENT_RED);
            hangUpBtn.setForeground(Color.WHITE);
            hangUpBtn.setOpaque(true);
            hangUpBtn.setBorderPainted(false);
            hangUpBtn.setFocusPainted(false);
            hangUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            hangUpBtn.addActionListener(e -> onHangUp());
            buttonPanel.add(hangUpBtn);
        }

        add(buttonPanel, BorderLayout.SOUTH);
        setLocationRelativeTo(getOwner());

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onHangUp();
            }
        });
    }

    public void handleCallAccepted() {
        isConnected = true;
        statusLabel.setText("Connected");
        statusLabel.setForeground(ACCENT_GREEN);

        // Switch buttons from Accept/Reject to Hang Up
        buttonPanel.removeAll();
        hangUpBtn = new JButton("Hang Up");
        hangUpBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hangUpBtn.setBackground(ACCENT_RED);
        hangUpBtn.setForeground(Color.WHITE);
        hangUpBtn.setOpaque(true);
        hangUpBtn.setBorderPainted(false);
        hangUpBtn.setFocusPainted(false);
        hangUpBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        hangUpBtn.addActionListener(e -> onHangUp());
        buttonPanel.add(hangUpBtn);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        // Start media streams
        if (isVideo) {
            videoCallManager.startCall();
        } else {
            voiceCallManager.startCall();
        }
    }

    public void handleCallRejected() {
        statusLabel.setText("Call Rejected");
        statusLabel.setForeground(ACCENT_RED);
        cleanupAndClose(1500);
    }

    public void handleCallEnded() {
        statusLabel.setText("Call Ended");
        cleanupAndClose(1500);
    }

    public void handleVoiceData(String base64Data) {
        if (voiceCallManager != null) {
            voiceCallManager.receiveAudio(base64Data);
        }
    }

    public void handleVideoData(String base64Data) {
        if (videoCallManager != null) {
            videoCallManager.receiveVideo(base64Data);
        }
    }

    public void setRemoteUdpAddress(String address) {
        if (address != null && address.contains(":")) {
            String[] parts = address.split(":", 2);
            String ip = parts[0];
            int port = Integer.parseInt(parts[1]);
            if (videoCallManager != null) {
                videoCallManager.setRemoteAddress(ip, port);
            }
            if (voiceCallManager != null) {
                voiceCallManager.setRemoteAddress(ip, port);
            }
        }
    }

    public int getLocalUdpPort() {
        if (isVideo && videoCallManager != null) {
            return videoCallManager.getLocalPort();
        }
        if (!isVideo && voiceCallManager != null) {
            return voiceCallManager.getLocalPort();
        }
        return 0;
    }

    private void onAcceptCall() {
        int localPort = getLocalUdpPort();
        MessageType type = isVideo ? MessageType.VIDEO_CALL_ACCEPT : MessageType.VOICE_CALL_ACCEPT;
        chatClient.sendMessage(new Message(type, myUsername, targetUser, String.valueOf(localPort)));
        handleCallAccepted();
    }

    private void onRejectCall() {
        MessageType type = isVideo ? MessageType.VIDEO_CALL_REJECT : MessageType.VOICE_CALL_REJECT;
        chatClient.sendMessage(new Message(type, myUsername, targetUser, ""));
        statusLabel.setText("Call Rejected");
        cleanupAndClose(100);
    }

    private void onHangUp() {
        if (isConnected || !isIncoming) {
            MessageType type = isVideo ? MessageType.VIDEO_CALL_END : MessageType.VOICE_CALL_END;
            chatClient.sendMessage(new Message(type, myUsername, targetUser, ""));
        } else {
            // Reject incoming before accept
            MessageType type = isVideo ? MessageType.VIDEO_CALL_REJECT : MessageType.VOICE_CALL_REJECT;
            chatClient.sendMessage(new Message(type, myUsername, targetUser, ""));
        }
        statusLabel.setText("Call Ended");
        cleanupAndClose(100);
    }

    private void cleanupAndClose(int delayMs) {
        if (voiceCallManager != null) {
            voiceCallManager.stopCall();
        }
        if (videoCallManager != null) {
            videoCallManager.stopCall();
        }

        // Give a short delay to let user see "Call Ended" / "Rejected"
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(this::dispose);
        }).start();
    }
}
