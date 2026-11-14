package com.campus.cms.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Small key/value settings stored in the database.
 *
 * The table is created on first use with CREATE TABLE IF NOT EXISTS, so schema.sql does not
 * have to change. Today it holds one setting: whether the enrollment period is open.
 * The default is OPEN, so a fresh database behaves as before until the admin closes it.
 */
public final class Settings {

    public static final String ENROLLMENT_OPEN = "enrollment_open";

    private Settings() { }

    private static void ensureTable(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS settings ("
                    + "setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
        }
    }

    public static String get(Connection c, String key, String defaultValue) throws SQLException {
        ensureTable(c);
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT setting_value FROM settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        }
    }

    public static void set(Connection c, String key, String value) throws SQLException {
        ensureTable(c);
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    public static boolean isEnrollmentOpen(Connection c) throws SQLException {
        return "1".equals(get(c, ENROLLMENT_OPEN, "1"));
    }

    public static void setEnrollmentOpen(Connection c, boolean open) throws SQLException {
        set(c, ENROLLMENT_OPEN, open ? "1" : "0");
    }
}
