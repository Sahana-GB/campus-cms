package com.campus.cms.auth;

import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LoginFrameTest {

    private Session session;

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        session = Session.getInstance();
        session.logout();
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void emptySubmissionDisplaysErrorWithoutCallingAuthService() {
        AtomicBoolean authCalled = new AtomicBoolean(false);
        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username) {
                authCalled.set(true);
                return Optional.empty();
            }
        };

        AuthService authService = new AuthService(mockDao, session);
        LoginFrame frame = new LoginFrame(authService);

        frame.getUsernameField().setText("");
        frame.getPasswordField().setText("");
        frame.handleLogin();

        assertFalse(authCalled.get());
        assertTrue(frame.getErrorLabel().getText().contains("required"));
        frame.dispose();
    }

    @Test
    void failedAuthenticationClearsPasswordAndDisplaysError() {
        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username) {
                return Optional.empty();
            }
        };

        AuthService authService = new AuthService(mockDao, session);
        LoginFrame frame = new LoginFrame(authService);

        frame.getUsernameField().setText("alice");
        frame.getPasswordField().setText("secret123");
        frame.handleLogin();

        assertEquals("", new String(frame.getPasswordField().getPassword()));
        assertTrue(frame.getErrorLabel().getText().contains("Invalid username or password"));
        frame.dispose();
    }

    @Test
    void databaseErrorDisplaysAppropriateMessage() {
        UserDAO failingDao = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username) throws SQLException {
                throw new SQLException("Connection refused");
            }
        };

        AuthService authService = new AuthService(failingDao, session);
        LoginFrame frame = new LoginFrame(authService);

        frame.getUsernameField().setText("alice");
        frame.getPasswordField().setText("secret123");
        frame.handleLogin();

        assertEquals("", new String(frame.getPasswordField().getPassword()));
        assertTrue(frame.getErrorLabel().getText().contains("Database error"));
        assertTrue(frame.getErrorLabel().getText().contains("Connection refused"));
        frame.dispose();
    }

    @Test
    void successfulAuthenticationRoutesToDashboardAndDisposes() {
        String hash = PasswordUtil.hashPassword("secret123");
        User user = new User(1, "admin_user", hash, Role.ADMIN, true);

        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username) {
                if ("admin_user".equals(username)) {
                    return Optional.of(user);
                }
                return Optional.empty();
            }
        };

        AuthService authService = new AuthService(mockDao, session);

        AtomicReference<Role> routedRole = new AtomicReference<>();
        LoginFrame frame = new LoginFrame(authService) {
            @Override
            protected void openDashboard(String title, Role role) {
                routedRole.set(role);
            }
        };

        frame.getUsernameField().setText("admin_user");
        frame.getPasswordField().setText("secret123");
        frame.handleLogin();

        assertEquals(Role.ADMIN, routedRole.get());
        assertTrue(session.isLoggedIn());
        assertEquals("admin_user", session.getCurrentUser().getUsername());
        assertFalse(frame.isDisplayable()); // Frame was disposed
    }

    @Test
    void successfulStudentAuthenticationRoutesToStudentDashboard() {
        String hash = PasswordUtil.hashPassword("pass123");
        User studentUser = new User(4, "student1", hash, Role.STUDENT, true);

        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username) {
                if ("student1".equals(username)) {
                    return Optional.of(studentUser);
                }
                return Optional.empty();
            }
        };

        AuthService authService = new AuthService(mockDao, session);

        AtomicReference<Role> routedRole = new AtomicReference<>();
        LoginFrame frame = new LoginFrame(authService) {
            @Override
            protected void openDashboard(String title, Role role) {
                routedRole.set(role);
            }
        };

        frame.getUsernameField().setText("student1");
        frame.getPasswordField().setText("pass123");
        frame.handleLogin();

        assertEquals(Role.STUDENT, routedRole.get());
        assertTrue(session.isLoggedIn());
        assertEquals("student1", session.getCurrentUser().getUsername());
        assertFalse(frame.isDisplayable());
    }
}
