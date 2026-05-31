package com.chatapp.server;

import com.chatapp.server.gui.ServerFrame;

import javax.swing.*;

/**
 * Entry point for the Chat Server application.
 */
public class ServerApp {
    public static void main(String[] args) {
        // Use cross-platform LAF to ensure custom colors work on all OS
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default LAF
        }

        SwingUtilities.invokeLater(() -> {
            ServerFrame serverFrame = new ServerFrame();
            serverFrame.setVisible(true);
        });
    }
}
