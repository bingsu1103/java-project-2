package com.chatapp.client;

import com.chatapp.client.gui.LoginFrame;
import com.chatapp.client.gui.MainFrame;

import javax.swing.*;

/**
 * Entry point for the Chat Client application.
 */
public class ClientApp {
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default LAF
        }

        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setLoginCallback((client, username) -> {
                loginFrame.dispose();
                SwingUtilities.invokeLater(() -> {
                    MainFrame mainFrame = new MainFrame(client, username);
                    mainFrame.setVisible(true);
                });
            });
            loginFrame.setVisible(true);
        });
    }
}
