package com.campus.cms.core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The single place where the app opens a database connection.
 * Every DAO should call Database.getConnection() and nothing else.
 *
 * Usage (always in try-with-resources so the connection is closed):
 *
 *   try (Connection con = Database.getConnection();
 *        PreparedStatement ps = con.prepareStatement("SELECT ...")) {
 *       ...
 *   }
 *
 * The database file is created by scripts/setup.sh. It is NOT created
 * automatically, so a missing file gives a clear error instead of a silent,
 * empty database.
 *
 * Using a different file (e.g. for tests): run  scripts/setup.sh data/test.db
 * and then call Database.setPath("data/test.db"), or start Java with
 * -Dcampus.db=data/test.db
 */
public final class Database {

    public static final String DEFAULT_PATH = "data/campus.db";

    private static String path = System.getProperty("campus.db", DEFAULT_PATH);

    private Database() { }

    public static String getPath() {
        return path;
    }

    public static void setPath(String newPath) {
        path = newPath;
    }

    /** Opens a new connection with foreign keys switched on. */
    public static Connection getConnection() throws SQLException {
        File file = new File(path);
        if (!file.isFile()) {
            throw new SQLException("Database file not found: " + file.getAbsolutePath()
                    + "  (run scripts/setup.sh first)");
        }

        Connection con = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = con.createStatement()) {
            // SQLite ignores foreign keys unless this is set on EVERY connection.
            st.execute("PRAGMA foreign_keys = ON");
            // Wait up to 5 seconds instead of failing instantly if the file is busy.
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            con.close();
            throw e;
        }
        return con;
    }
}
