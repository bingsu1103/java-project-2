package com.chatapp.client;

import com.chatapp.client.gui.LoginFrame;

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
                // TODO: Open MainFrame after successful login
                System.out.println("Login successful! User: " + username);
                loginFrame.dispose();
            });
            loginFrame.setVisible(true);
        });
    }
}
