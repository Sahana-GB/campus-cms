package com.campus.cms;

import com.campus.cms.auth.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point for Campus Course Management System (CCMS).
 */
public class Main {

    public static void main(String[] args) {
        // Initialize modern look and feel (FlatLaf) with fallback to system look and feel
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to standard Swing Look and Feel
            }
        }

        // Launch LoginFrame on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
