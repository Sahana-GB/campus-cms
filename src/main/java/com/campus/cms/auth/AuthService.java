package com.campus.cms.auth;

import com.campus.cms.core.Session;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/**
 * Service handling authentication, credential verification, and session lifecycle.
 */
public class AuthService {

    private final UserDAO userDAO;
    private final Session session;

    public AuthService() {
        this(new UserDAO(), Session.getInstance());
    }

    public AuthService(UserDAO userDAO) {
        this(userDAO, Session.getInstance());
    }

    public AuthService(UserDAO userDAO, Session session) {
        this.userDAO = Objects.requireNonNull(userDAO);
        this.session = Objects.requireNonNull(session);
    }

    /**
     * Authenticates a user with username and password.
     * If credentials are valid and the account is active, initializes the session.
     *
     * @param username the entered username
     * @param password the entered plaintext password
     * @return true if authentication succeeded and session initialized, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean login(String username, String password) throws SQLException {
        Optional<User> authenticatedUser = authenticate(username, password);
        if (authenticatedUser.isPresent()) {
            session.login(authenticatedUser.get());
            return true;
        }
        return false;
    }

    /**
     * Verifies user credentials without modifying the session.
     * Rejects nonexistent users, incorrect passwords, and inactive users.
     *
     * @param username the username
     * @param password the plaintext password
     * @return Optional containing the authenticated User, or empty Optional if rejected
     * @throws SQLException if a database error occurs
     */
    public Optional<User> authenticate(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            return Optional.empty();
        }

        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    /**
     * Logs out the current user by clearing the session.
     */
    public void logout() {
        session.logout();
    }

    /**
     * Checks whether a user is currently logged into the session.
     *
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    /**
     * Retrieves the currently logged in user from the session, if present.
     *
     * @return Optional containing the logged in User
     */
    public Optional<User> getCurrentUser() {
        return session.getUser();
    }

    /**
     * Changes the password for a user after verifying their current password.
     * Updates the password hash in the database and refreshes the current session if applicable.
     *
     * @param userId the ID of the user
     * @param currentPassword the user's existing plaintext password
     * @param newPassword the new plaintext password
     * @return true if password was successfully verified and updated, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) throws SQLException {
        if (currentPassword == null || currentPassword.isEmpty() ||
                newPassword == null || newPassword.isEmpty()) {
            return false;
        }

        User currentUser = session.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() != userId) {
            return false;
        }

        Optional<User> userOpt = userDAO.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
            return false;
        }

        String newHash = PasswordUtil.hashPassword(newPassword);
        boolean updated = userDAO.updatePassword(userId, newHash);

        if (updated) {
            User updatedUser = new User(user.getUserId(), user.getUsername(), newHash, user.getRole(), user.isActive());
            session.login(updatedUser);
            return true;
        }

        return false;
    }

    /**
     * Changes the password for the currently authenticated user in the session.
     *
     * @param currentPassword the user's existing plaintext password
     * @param newPassword the new plaintext password
     * @return true if password was successfully updated, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean changePassword(String currentPassword, String newPassword) throws SQLException {
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return changePassword(currentUser.getUserId(), currentPassword, newPassword);
    }
}
