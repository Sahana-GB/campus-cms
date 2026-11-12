
package com.campus.cms.auth;

import com.campus.cms.core.Role;

public class User {

    private final int userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public User(int userId, String username, String passwordHash,
                Role role, boolean active) {

        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
