package com.campus.cms.auth;

import com.campus.cms.core.Database;
import com.campus.cms.core.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Data Access Object for User operations on the 'users' table.
 */
public class UserDAO {

    /**
     * Finds a user by their unique username.
     *
     * @param username the username to look up
     * @return an Optional containing the User if found, or empty Optional otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT user_id, username, password_hash, role, is_active FROM users WHERE username = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a user by their primary key user_id.
     *
     * @param userId the user ID to look up
     * @return an Optional containing the User if found, or empty Optional otherwise
     * @throws SQLException if a database access error occurs
     */
    public Optional<User> findById(int userId) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, role, is_active FROM users WHERE user_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Updates the password hash for a specific user.
     *
     * @param userId the ID of the user
     * @param newPasswordHash the new password hash
     * @return true if a row was updated, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean updatePassword(int userId, String newPasswordHash) throws SQLException {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }

        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String passwordHash = rs.getString("password_hash");
        Role role = Role.fromString(rs.getString("role"));
        boolean active = rs.getInt("is_active") == 1;

        return new User(userId, username, passwordHash, role, active);
    }
}
