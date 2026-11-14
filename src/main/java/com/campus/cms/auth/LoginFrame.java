package com.campus.cms.auth;

import com.campus.cms.core.Role;
import com.campus.cms.core.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Swing login window for CCMS authenticating users and routing to dashboards based on role.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;

    public LoginFrame() {
        this(new AuthService());
    }

    public LoginFrame(AuthService authService) {
        this.authService = Objects.requireNonNull(authService);
        initUI();
    }

    private void initUI() {
        setTitle("Campus CMS - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header Title
        JLabel titleLabel = new JLabel("Campus Course Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Please enter your credentials to sign in");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Form Fields
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        formPanel.setBorder(new EmptyBorder(15, 0, 10, 0));

        JLabel userLabel = new JLabel("Username:");
        usernameField = new JTextField();
        JLabel passLabel = new JLabel("Password:");
        passwordField = new JPasswordField();

        formPanel.add(userLabel);
        formPanel.add(usernameField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);

        // Error message label
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(192, 0, 0));
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        loginButton.setPreferredSize(new Dimension(120, 34));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> handleLogin());

        // Assemble panels
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(formPanel);
        mainPanel.add(errorLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(loginButton);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(loginButton);
    }

    void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = passwordChars == null ? "" : new String(passwordChars);

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required.");
            return;
        }

        try {
            boolean success = authService.login(username, password);
            if (success) {
                clearError();
                dispose();
                routeToDashboard();
            } else {
                showError("Invalid username or password, or account is inactive.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }
        } catch (SQLException e) {
            showError("Database error occurred: " + e.getMessage());
            passwordField.setText("");
        }
    }

    void showError(String message) {
        errorLabel.setText(message);
    }

    void clearError() {
        errorLabel.setText(" ");
    }

    void routeToDashboard() {
        Role role = Session.getInstance().getRole();
        if (role == null) {
            showError("Authentication error: No active role found in session.");
            return;
        }

        switch (role) {
            case ADMIN -> openDashboard("Admin Dashboard", Role.ADMIN);
            case FACULTY -> openDashboard("Faculty Dashboard", Role.FACULTY);
            case STUDENT -> openDashboard("Student Dashboard", Role.STUDENT);
        }
    }

    protected void openDashboard(String title, Role role) {
        if (role == Role.ADMIN) {
            new com.campus.cms.dashboard.AdminDashboard().setVisible(true);
            return;
        }
        if (role == Role.STUDENT) {
            new com.campus.cms.dashboard.StudentDashboard().setVisible(true);
            return;
        }
        if (role == Role.FACULTY) {
            new com.campus.cms.dashboard.FacultyDashboard().setVisible(true);
            return;
        }

        // Defensive fallback: all supported roles should be handled above.
        JFrame dashboardFrame = new JFrame(title);
        dashboardFrame.setSize(600, 400);
        dashboardFrame.setLocationRelativeTo(null);
        dashboardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String username = Session.getInstance().getCurrentUser() != null
                ? Session.getInstance().getCurrentUser().getUsername()
                : "Unknown";

        JLabel welcomeLabel = new JLabel("Welcome, " + username + " (" + role + ")!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            authService.logout();
            dashboardFrame.dispose();
            new LoginFrame(authService).setVisible(true);
        });

        panel.add(welcomeLabel, BorderLayout.CENTER);
        panel.add(logoutButton, BorderLayout.SOUTH);

        dashboardFrame.setContentPane(panel);
        dashboardFrame.setVisible(true);
    }

    // Accessors for testing
    JTextField getUsernameField() {
        return usernameField;
    }

    JPasswordField getPasswordField() {
        return passwordField;
    }

    JButton getLoginButton() {
        return loginButton;
    }

    JLabel getErrorLabel() {
        return errorLabel;
    }
}
