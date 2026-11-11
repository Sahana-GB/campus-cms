package com.campus.cms.auth;

import com.campus.cms.core.Database;
import com.campus.cms.core.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    @TempDir
    Path tempDir;

    private UserDAO userDAO;
    private String originalDbPath;

    @BeforeEach
    void setUp() throws SQLException {
        originalDbPath = Database.getPath();
        Path testDb = tempDir.resolve("test_campus.db");
        Database.setPath(testDb.toString());

        // Initialize schema and sample test data
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

            st.execute("""
                INSERT INTO users (user_id, username, password_hash, role, is_active) VALUES
                    (1, 'admin', 'salt:hash_admin', 'ADMIN', 1),
                    (2, 'faculty1', 'salt:hash_faculty', 'FACULTY', 1),
                    (3, 'student1', 'salt:hash_student', 'STUDENT', 1),
                    (4, 'disabled_user', 'salt:hash_disabled', 'STUDENT', 0);
            """);
        }

        userDAO = new UserDAO();
    }

    @AfterEach
    void tearDown() {
        Database.setPath(originalDbPath);
    }

    @Test
    void findByUsernameReturnsCorrectUserWhenExists() throws SQLException {
        Optional<User> userOpt = userDAO.findByUsername("admin");

        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(1, user.getUserId());
        assertEquals("admin", user.getUsername());
        assertEquals("salt:hash_admin", user.getPasswordHash());
        assertEquals(Role.ADMIN, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    void findByUsernameReturnsInactiveUserWhenDisabled() throws SQLException {
        Optional<User> userOpt = userDAO.findByUsername("disabled_user");

        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(4, user.getUserId());
        assertEquals("disabled_user", user.getUsername());
        assertFalse(user.isActive());
        assertEquals(Role.STUDENT, user.getRole());
    }

    @Test
    void findByUsernameReturnsEmptyWhenNotFound() throws SQLException {
        Optional<User> userOpt = userDAO.findByUsername("nonexistent");

        assertFalse(userOpt.isPresent());
    }

    @Test
    void findByUsernameHandlesNullAndEmpty() throws SQLException {
        assertFalse(userDAO.findByUsername(null).isPresent());
        assertFalse(userDAO.findByUsername("").isPresent());
        assertFalse(userDAO.findByUsername("   ").isPresent());
    }

    @Test
    void findByIdReturnsCorrectUserWhenExists() throws SQLException {
        Optional<User> userOpt = userDAO.findById(2);

        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals(2, user.getUserId());
        assertEquals("faculty1", user.getUsername());
        assertEquals(Role.FACULTY, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() throws SQLException {
        Optional<User> userOpt = userDAO.findById(999);

        assertFalse(userOpt.isPresent());
    }

    @Test
    void updatePasswordSuccessfullyUpdatesHash() throws SQLException {
        boolean updated = userDAO.updatePassword(1, "new_salt:new_hash");

        assertTrue(updated);
        Optional<User> updatedUser = userDAO.findById(1);
        assertTrue(updatedUser.isPresent());
        assertEquals("new_salt:new_hash", updatedUser.get().getPasswordHash());
    }

    @Test
    void updatePasswordReturnsFalseForNonExistentUser() throws SQLException {
        boolean updated = userDAO.updatePassword(9999, "new_salt:new_hash");

        assertFalse(updated);
    }

    @Test
    void updatePasswordThrowsOnEmptyOrNullHash() {
        assertThrows(IllegalArgumentException.class, () -> userDAO.updatePassword(1, null));
        assertThrows(IllegalArgumentException.class, () -> userDAO.updatePassword(1, ""));
        assertThrows(IllegalArgumentException.class, () -> userDAO.updatePassword(1, "   "));
    }
}
