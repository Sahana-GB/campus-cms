package com.campus.cms.auth;

import com.campus.cms.core.Session;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Objects;

/**
 * UI panel displaying the authenticated user's profile and providing a secure change-password interface.
 */
public class ProfilePanel extends JPanel {

    private final AuthService authService;
    private final Session session;

    private JLabel usernameValueLabel;
    private JLabel roleValueLabel;

    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JButton updatePasswordButton;
    private JLabel messageLabel;

    public ProfilePanel() {
        this(new AuthService(), Session.getInstance());
    }

    public ProfilePanel(AuthService authService, Session session) {
        this.authService = Objects.requireNonNull(authService);
        this.session = Objects.requireNonNull(session);

        initUI();
        refreshProfile();
    }

    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.BACKGROUND);
        setBorder(Styles.pagePadding());

        // ------------------ Header ------------------
        PageHeader header = new PageHeader("My Profile",
                "View your account details and manage password credentials");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ------------------ Profile Info Card ------------------
        JPanel infoCard = new JPanel(new GridLayout(2, 2, 12, 10));
        infoCard.setBackground(Theme.SURFACE);
        infoCard.setBorder(Styles.cardBorder());
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(520, 110));

        JLabel userHeaderLabel = new JLabel("Username:");
        userHeaderLabel.setFont(Theme.BODY_BOLD);
        userHeaderLabel.setForeground(Theme.TEXT_MUTED);
        usernameValueLabel = new JLabel("—");
        usernameValueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel roleHeaderLabel = new JLabel("Role:");
        roleHeaderLabel.setFont(Theme.BODY_BOLD);
        roleHeaderLabel.setForeground(Theme.TEXT_MUTED);
        roleValueLabel = new JLabel("—");
        roleValueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        infoCard.add(userHeaderLabel);
        infoCard.add(usernameValueLabel);
        infoCard.add(roleHeaderLabel);
        infoCard.add(roleValueLabel);

        // ------------------ Change Password Form ------------------
        JPanel passwordCard = new JPanel();
        passwordCard.setLayout(new BoxLayout(passwordCard, BoxLayout.Y_AXIS));
        passwordCard.setBackground(Theme.SURFACE);
        passwordCard.setBorder(Styles.cardBorder());
        JLabel passwordTitle = new JLabel("Change Password");
        passwordTitle.setFont(Theme.HEADING);
        passwordTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordCard.add(passwordTitle);
        passwordCard.add(Box.createVerticalStrut(Theme.SPACE_MD));
        passwordCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordCard.setMaximumSize(new Dimension(520, 300));

        JPanel formGrid = new JPanel(new GridLayout(3, 2, 10, 10));
        formGrid.setOpaque(false);
        formGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel currentLabel = new JLabel("Current Password:");
        currentPasswordField = new JPasswordField();

        JLabel newLabel = new JLabel("New Password:");
        newPasswordField = new JPasswordField();

        JLabel confirmLabel = new JLabel("Confirm New Password:");
        confirmPasswordField = new JPasswordField();

        formGrid.add(currentLabel);
        formGrid.add(currentPasswordField);
        formGrid.add(newLabel);
        formGrid.add(newPasswordField);
        formGrid.add(confirmLabel);
        formGrid.add(confirmPasswordField);

        // Feedback / Status message label
        messageLabel = new JLabel(" ");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Action Button
        updatePasswordButton = Styles.primary(new JButton("Update Password"));
        updatePasswordButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        updatePasswordButton.addActionListener(e -> handleChangePassword());

        passwordCard.add(formGrid);
        passwordCard.add(Box.createVerticalStrut(10));
        passwordCard.add(messageLabel);
        passwordCard.add(Box.createVerticalStrut(8));
        passwordCard.add(updatePasswordButton);

        // Assemble overall panel
        add(header);
        add(infoCard);
        add(Box.createVerticalStrut(Theme.SPACE_LG));
        add(passwordCard);
        add(Box.createVerticalGlue());
    }

    /**
     * Refreshes the displayed username and role from the current active session.
     */
    public void refreshProfile() {
        User user = session.getCurrentUser();
        if (user != null) {
            usernameValueLabel.setText(user.getUsername());
            roleValueLabel.setText(user.getRole() != null ? user.getRole().name() : "N/A");
        } else {
            usernameValueLabel.setText("Not logged in");
            roleValueLabel.setText("N/A");
        }
    }

    void handleChangePassword() {
        char[] currentChars = currentPasswordField.getPassword();
        char[] newChars = newPasswordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();

        String currentPass = currentChars == null ? "" : new String(currentChars);
        String newPass = newChars == null ? "" : new String(newChars);
        String confirmPass = confirmChars == null ? "" : new String(confirmChars);

        // Clear password fields immediately
        currentPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");

        User user = session.getCurrentUser();
        if (user == null) {
            showErrorMessage("No user is currently logged in.");
            return;
        }

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showErrorMessage("All password fields are required.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showErrorMessage("New password and confirmation do not match.");
            return;
        }

        if (newPass.equals(currentPass)) {
            showErrorMessage("New password must be different from current password.");
            return;
        }

        try {
            boolean success = authService.changePassword(user.getUserId(), currentPass, newPass);
            if (success) {
                showSuccessMessage("Password updated successfully.");
            } else {
                showErrorMessage("Incorrect current password.");
            }
        } catch (SQLException e) {
            showErrorMessage("Database error: " + e.getMessage());
        }
    }

    void showErrorMessage(String msg) {
        messageLabel.setForeground(new Color(192, 0, 0));
        messageLabel.setText(msg);
    }

    void showSuccessMessage(String msg) {
        messageLabel.setForeground(new Color(0, 128, 0));
        messageLabel.setText(msg);
    }

    // Accessors for unit testing
    JLabel getUsernameValueLabel() {
        return usernameValueLabel;
    }

    JLabel getRoleValueLabel() {
        return roleValueLabel;
    }

    JPasswordField getCurrentPasswordField() {
        return currentPasswordField;
    }

    JPasswordField getNewPasswordField() {
        return newPasswordField;
    }

    JPasswordField getConfirmPasswordField() {
        return confirmPasswordField;
    }

    JButton getUpdatePasswordButton() {
        return updatePasswordButton;
    }

    JLabel getMessageLabel() {
        return messageLabel;
    }
}
