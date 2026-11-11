package com.campus.cms.core;

import com.campus.cms.auth.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    private Session session;

    @BeforeEach
    void setUp() {
        session = Session.getInstance();
        session.logout();
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void sessionInitialStateIsLoggedOut() {
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
        assertTrue(session.getUser().isEmpty());
        assertNull(session.getRole());
        assertFalse(session.hasRole(Role.ADMIN));
    }

    @Test
    void loginWithActiveUserSetsSession() {
        User user = new User(1, "admin", "hash123", Role.ADMIN, true);
        session.login(user);

        assertTrue(session.isLoggedIn());
        assertEquals(user, session.getCurrentUser());
        assertEquals(Role.ADMIN, session.getRole());
        assertTrue(session.hasRole(Role.ADMIN));
        assertFalse(session.hasRole(Role.STUDENT));
        assertFalse(session.hasRole(Role.FACULTY));
    }

    @Test
    void loginWithInactiveUserThrowsException() {
        User inactiveUser = new User(2, "disabled", "hash123", Role.STUDENT, false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> session.login(inactiveUser));
        assertTrue(ex.getMessage().contains("Inactive"));
        assertFalse(session.isLoggedIn());
    }

    @Test
    void loginWithNullUserThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> session.login(null));
        assertFalse(session.isLoggedIn());
    }

    @Test
    void logoutClearsSession() {
        User user = new User(3, "prof", "hash123", Role.FACULTY, true);
        session.login(user);
        assertTrue(session.isLoggedIn());

        session.logout();

        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUser());
        assertTrue(session.getUser().isEmpty());
        assertNull(session.getRole());
        assertFalse(session.hasRole(Role.FACULTY));
    }

    @Test
    void hasRoleWithNullOrLoggedOutReturnsFalse() {
        assertFalse(session.hasRole(null));
        assertFalse(session.hasRole(Role.ADMIN));

        User user = new User(4, "student", "hash123", Role.STUDENT, true);
        session.login(user);

        assertFalse(session.hasRole(null));
    }

    @Test
    void loginWithSecondUserReplacesPreviousSession() {
        User user1 = new User(1, "admin", "hash1", Role.ADMIN, true);
        User user2 = new User(2, "faculty1", "hash2", Role.FACULTY, true);

        session.login(user1);
        assertEquals(user1, session.getCurrentUser());
        assertEquals(Role.ADMIN, session.getRole());

        session.login(user2);
        assertEquals(user2, session.getCurrentUser());
        assertEquals(Role.FACULTY, session.getRole());
        assertTrue(session.hasRole(Role.FACULTY));
        assertFalse(session.hasRole(Role.ADMIN));
    }
}
