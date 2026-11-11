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

import static org.junit.jupiter.api.Assertions.*;

class ProfilePanelTest {

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
    void profileDisplaysCurrentLoggedInUserDetails() {
        User user = new User(10, "kavya_user", "hash123", Role.STUDENT, true);
        session.login(user);

        UserDAO dummyDao = new UserDAO();
        AuthService authService = new AuthService(dummyDao, session);

        ProfilePanel panel = new ProfilePanel(authService, session);

        assertEquals("kavya_user", panel.getUsernameValueLabel().getText());
        assertEquals("STUDENT", panel.getRoleValueLabel().getText());
    }

    @Test
    void emptySubmissionDisplaysErrorAndClearsFields() {
        User user = new User(10, "kavya_user", "hash123", Role.STUDENT, true);
        session.login(user);

        UserDAO dummyDao = new UserDAO();
        AuthService authService = new AuthService(dummyDao, session);

        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("");
        panel.getNewPasswordField().setText("newSecret");
        panel.getConfirmPasswordField().setText("newSecret");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("All password fields are required"));
        assertEquals("", new String(panel.getCurrentPasswordField().getPassword()));
        assertEquals("", new String(panel.getNewPasswordField().getPassword()));
        assertEquals("", new String(panel.getConfirmPasswordField().getPassword()));
    }

    @Test
    void passwordMismatchDisplaysErrorAndClearsFields() {
        User user = new User(10, "kavya_user", "hash123", Role.STUDENT, true);
        session.login(user);

        UserDAO dummyDao = new UserDAO();
        AuthService authService = new AuthService(dummyDao, session);

        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("currentPass");
        panel.getNewPasswordField().setText("newSecret1");
        panel.getConfirmPasswordField().setText("newSecret2");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("do not match"));
        assertEquals("", new String(panel.getCurrentPasswordField().getPassword()));
        assertEquals("", new String(panel.getNewPasswordField().getPassword()));
        assertEquals("", new String(panel.getConfirmPasswordField().getPassword()));
    }

    @Test
    void newPasswordSameAsCurrentDisplaysError() {
        User user = new User(10, "kavya_user", "hash123", Role.STUDENT, true);
        session.login(user);

        UserDAO dummyDao = new UserDAO();
        AuthService authService = new AuthService(dummyDao, session);

        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("samePass");
        panel.getNewPasswordField().setText("samePass");
        panel.getConfirmPasswordField().setText("samePass");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("must be different"));
    }

    @Test
    void successfulPasswordChangeDisplaysSuccessAndClearsFields() {
        String oldHash = PasswordUtil.hashPassword("oldSecret");
        User user = new User(10, "kavya_user", oldHash, Role.STUDENT, true);
        session.login(user);

        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findById(int userId) {
                return Optional.of(user);
            }

            @Override
            public boolean updatePassword(int userId, String newPasswordHash) {
                return true;
            }
        };

        AuthService authService = new AuthService(mockDao, session);
        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("oldSecret");
        panel.getNewPasswordField().setText("newBrandPass1");
        panel.getConfirmPasswordField().setText("newBrandPass1");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("successfully"));
        assertEquals("", new String(panel.getCurrentPasswordField().getPassword()));
        assertEquals("", new String(panel.getNewPasswordField().getPassword()));
        assertEquals("", new String(panel.getConfirmPasswordField().getPassword()));
    }

    @Test
    void incorrectCurrentPasswordDisplaysError() {
        String oldHash = PasswordUtil.hashPassword("oldSecret");
        User user = new User(10, "kavya_user", oldHash, Role.STUDENT, true);
        session.login(user);

        UserDAO mockDao = new UserDAO() {
            @Override
            public Optional<User> findById(int userId) {
                return Optional.of(user);
            }

            @Override
            public boolean updatePassword(int userId, String newPasswordHash) {
                return true;
            }
        };

        AuthService authService = new AuthService(mockDao, session);
        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("wrongCurrent");
        panel.getNewPasswordField().setText("newBrandPass1");
        panel.getConfirmPasswordField().setText("newBrandPass1");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("Incorrect current password"));
    }

    @Test
    void databaseErrorDisplaysError() {
        String oldHash = PasswordUtil.hashPassword("oldSecret");
        User user = new User(10, "kavya_user", oldHash, Role.STUDENT, true);
        session.login(user);

        UserDAO failingDao = new UserDAO() {
            @Override
            public Optional<User> findById(int userId) throws SQLException {
                throw new SQLException("DB connection failed");
            }
        };

        AuthService authService = new AuthService(failingDao, session);
        ProfilePanel panel = new ProfilePanel(authService, session);

        panel.getCurrentPasswordField().setText("oldSecret");
        panel.getNewPasswordField().setText("newBrandPass1");
        panel.getConfirmPasswordField().setText("newBrandPass1");

        panel.handleChangePassword();

        assertTrue(panel.getMessageLabel().getText().contains("Database error"));
        assertTrue(panel.getMessageLabel().getText().contains("DB connection failed"));
    }
}
