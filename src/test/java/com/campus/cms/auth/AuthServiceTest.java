package com.campus.cms.auth;

import com.campus.cms.core.Database;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path tempDir;

    private Session session;
    private AuthService authService;
    private StubUserDAO stubUserDAO;

    private static class StubUserDAO extends UserDAO {
        private final Map<String, User> users = new HashMap<>();
        private final Map<Integer, User> usersById = new HashMap<>();

        void addUser(User user) {
            users.put(user.getUsername(), user);
            usersById.put(user.getUserId(), user);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public Optional<User> findById(int userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public boolean updatePassword(int userId, String newPasswordHash) {
            User existing = usersById.get(userId);
            if (existing != null) {
                User updated = new User(existing.getUserId(), existing.getUsername(), newPasswordHash, existing.getRole(), existing.isActive());
                users.put(existing.getUsername(), updated);
                usersById.put(userId, updated);
                return true;
            }
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        session = Session.getInstance();
        session.logout();

        stubUserDAO = new StubUserDAO();

        String activeHash = PasswordUtil.hashPassword("secret123");
        String inactiveHash = PasswordUtil.hashPassword("inactiveSecret");

        stubUserDAO.addUser(new User(1, "alice", activeHash, Role.ADMIN, true));
        stubUserDAO.addUser(new User(2, "bob", activeHash, Role.STUDENT, true));
        stubUserDAO.addUser(new User(3, "charlie", inactiveHash, Role.FACULTY, false));

        authService = new AuthService(stubUserDAO, session);
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void successfulLoginInitializesSession() throws SQLException {
        boolean success = authService.login("alice", "secret123");

        assertTrue(success);
        assertTrue(authService.isLoggedIn());
        assertTrue(session.isLoggedIn());
        assertEquals("alice", session.getCurrentUser().getUsername());
        assertEquals(Role.ADMIN, session.getRole());
        assertTrue(session.hasRole(Role.ADMIN));
    }

    @Test
    void incorrectPasswordFailsAndDoesNotInitializeSession() throws SQLException {
        boolean success = authService.login("alice", "wrongPassword");

        assertFalse(success);
        assertFalse(authService.isLoggedIn());
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
        assertTrue(authService.getCurrentUser().isEmpty());
    }

    @Test
    void nonexistentUsernameFailsAndDoesNotInitializeSession() throws SQLException {
        boolean success = authService.login("nonexistent", "secret123");

        assertFalse(success);
        assertFalse(authService.isLoggedIn());
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void inactiveAccountFailsEvenWithCorrectPassword() throws SQLException {
        boolean success = authService.login("charlie", "inactiveSecret");

        assertFalse(success);
        assertFalse(authService.isLoggedIn());
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void nullOrBlankCredentialsFailSafely() throws SQLException {
        assertFalse(authService.login(null, "secret123"));
        assertFalse(authService.login("", "secret123"));
        assertFalse(authService.login("   ", "secret123"));
        assertFalse(authService.login("alice", null));
        assertFalse(authService.login("alice", ""));
        assertFalse(session.isLoggedIn());
    }

    @Test
    void failedLoginDoesNotOverwriteExistingSession() throws SQLException {
        boolean firstLogin = authService.login("alice", "secret123");
        assertTrue(firstLogin);
        assertEquals("alice", session.getCurrentUser().getUsername());

        // Failed attempt with another user
        boolean failedAttempt = authService.login("bob", "wrongpass");
        assertFalse(failedAttempt);

        // Previous user's session is preserved
        assertTrue(session.isLoggedIn());
        assertEquals("alice", session.getCurrentUser().getUsername());
    }

    @Test
    void logoutClearsSession() throws SQLException {
        authService.login("bob", "secret123");
        assertTrue(authService.isLoggedIn());

        authService.logout();

        assertFalse(authService.isLoggedIn());
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
    }

    @Test
    void endToEndAuthenticationWithDefaultConstructorAgainstDatabase() throws SQLException {
        String originalDbPath = Database.getPath();
        Path testDb = tempDir.resolve("e2e_campus.db");
        Database.setPath(testDb.toString());

        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + testDb);
             Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute("""
                CREATE TABLE users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    role          TEXT    NOT NULL CHECK (role IN ('ADMIN', 'FACULTY', 'STUDENT')),
                    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
                );
            """);

            String passwordHash = PasswordUtil.hashPassword("pass123");
            st.execute(String.format(
                "INSERT INTO users (user_id, username, password_hash, role, is_active) VALUES (1, 'e2e_admin', '%s', 'ADMIN', 1);",
                passwordHash
            ));
        }

        try {
            AuthService e2eAuthService = new AuthService();
            assertTrue(e2eAuthService.login("e2e_admin", "pass123"));
            assertTrue(e2eAuthService.isLoggedIn());
            assertEquals("e2e_admin", e2eAuthService.getCurrentUser().get().getUsername());
        } finally {
            session.logout();
            Database.setPath(originalDbPath);
        }
    }

    @Test
    void databaseFailureIsPropagated() {
        UserDAO failingDAO = new UserDAO() {
            @Override
            public Optional<User> findByUsername(String username)
                    throws SQLException {
                throw new SQLException("Database unavailable");
            }
        };

        AuthService failingService = new AuthService(failingDAO, session);

        assertThrows(SQLException.class,
                () -> failingService.login("alice", "secret123"));

        assertFalse(session.isLoggedIn());
    }

    @Test
    void changePasswordSuccessfullyUpdatesPasswordAndRefreshesSession() throws SQLException {
        authService.login("alice", "secret123");
        assertEquals("alice", session.getCurrentUser().getUsername());

        boolean changed = authService.changePassword(1, "secret123", "newPass456");

        assertTrue(changed);
        assertTrue(PasswordUtil.verifyPassword("newPass456", session.getCurrentUser().getPasswordHash()));

        // Old password no longer works
        authService.logout();
        assertFalse(authService.login("alice", "secret123"));
        assertTrue(authService.login("alice", "newPass456"));
    }

    @Test
    void changePasswordWithIncorrectCurrentPasswordFails() throws SQLException {
        authService.login("alice", "secret123");

        boolean changed = authService.changePassword(1, "wrongCurrent", "newPass456");

        assertFalse(changed);
        // Password remains the old one
        authService.logout();
        assertTrue(authService.login("alice", "secret123"));
    }

    @Test
    void changePasswordWithNonExistentUserFails() throws SQLException {
        boolean changed = authService.changePassword(999, "secret123", "newPass456");

        assertFalse(changed);
    }

    @Test
    void changePasswordWithEmptyOrNullInputsFails() throws SQLException {
        assertFalse(authService.changePassword(1, null, "newPass"));
        assertFalse(authService.changePassword(1, "", "newPass"));
        assertFalse(authService.changePassword(1, "currPass", null));
        assertFalse(authService.changePassword(1, "currPass", ""));
    }

    @Test
    void changePasswordRejectsChangingAnotherUsersPassword() throws SQLException {
        authService.login("alice", "secret123");
        assertEquals(1, session.getCurrentUser().getUserId());

        // Attempting to change bob's password (userId = 2) while alice is logged in must be rejected
        boolean changed = authService.changePassword(2, "secret123", "hackedPass");
        assertFalse(changed);

        // Bob's password remains unchanged
        authService.logout();
        assertTrue(authService.login("bob", "secret123"));
    }

    @Test
    void changePasswordFailsWhenNoSessionIsActive() throws SQLException {
        assertFalse(session.isLoggedIn());

        boolean changed = authService.changePassword(1, "secret123", "newPass");
        assertFalse(changed);
    }

    @Test
    void changePasswordOverloadDerivesUserIdFromSession() throws SQLException {
        authService.login("bob", "secret123");

        boolean changed = authService.changePassword("secret123", "brandNewPass");
        assertTrue(changed);

        authService.logout();
        assertTrue(authService.login("bob", "brandNewPass"));
    }
}
