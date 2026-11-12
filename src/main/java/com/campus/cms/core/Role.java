package com.campus.cms.core;

import java.util.Locale;

/**
 * The three user roles. The names must match the values allowed by the
 * CHECK constraint on users.role in db/schema.sql.
 */
public enum Role {
    ADMIN,
    FACULTY,
    STUDENT;

    /** Converts text from the database (e.g. "STUDENT") into a Role. */
    public static Role fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Role is null");
        }
        try {
            return Role.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown role: " + text);
        }
    }
}
