package com.campus.cms.core;

import com.campus.cms.auth.User;

import java.util.Optional;

/**
 * Manages the authenticated user session across the application.
 * Thread-safe singleton providing login, logout, and role-checking utilities.
 */
public class Session {

    private static final Session INSTANCE = new Session();

    private User currentUser;

    private Session() {
        // Private constructor for singleton
    }

    public static Session getInstance() {
        return INSTANCE;
    }

    /**
     * Authenticates and stores the current user session.
     *
     * @param user the authenticated user
     * @throws IllegalArgumentException if user is null or inactive
     */
    public synchronized void login(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("Inactive user cannot be logged in");
        }
        this.currentUser = user;
    }

    /**
     * Clears the current user session.
     */
    public synchronized void logout() {
        this.currentUser = null;
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if a session is active, false otherwise
     */
    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Returns the currently logged in user, if any.
     *
     * @return the current User, or null if logged out
     */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns an Optional containing the currently logged in user.
     *
     * @return Optional of current User
     */
    public synchronized Optional<User> getUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Checks if the currently logged in user has the specified role.
     *
     * @param role the role to check
     * @return true if logged in and matches the role, false otherwise
     */
    public synchronized boolean hasRole(Role role) {
        if (currentUser == null || role == null) {
            return false;
        }
        return currentUser.getRole() == role;
    }

    /**
     * Convenience check for the user's role.
     *
     * @return the Role of the logged-in user, or null if not logged in
     */
    public synchronized Role getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }
}
