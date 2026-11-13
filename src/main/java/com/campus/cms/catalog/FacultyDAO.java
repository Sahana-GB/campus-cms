package com.campus.cms.catalog;

import com.campus.cms.auth.PasswordUtil;
import com.campus.cms.core.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Data Access Object for faculty records.
 *
 * A faculty member has both a row in users (for login/authorization) and a row
 * in faculty (for academic details). Creation and account activation changes
 * therefore update both tables transactionally.
 */
public class FacultyDAO {

    private static final String SELECT_SQL =
            "SELECT f.faculty_id, f.user_id, f.name, f.department, f.email, f.status, u.username " +
            "FROM faculty f JOIN users u ON f.user_id = u.user_id ";

    /**
     * Creates a faculty login and its faculty profile in one transaction.
     */
    public Faculty createFaculty(String username, String password, String name,
                                 String department, String email) throws SQLException {
        requireText(username, "Username");
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        requireText(name, "Name");
        requireText(department, "Department");

        String passwordHash = PasswordUtil.hashPassword(password);

        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId;
                String userSql = "INSERT INTO users (username, password_hash, role, is_active) " +
                        "VALUES (?, ?, 'FACULTY', 1)";
                try (PreparedStatement ps = con.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, username.trim());
                    ps.setString(2, passwordHash);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Failed to retrieve generated user ID.");
                        userId = rs.getInt(1);
                    }
                }

                int facultyId;
                String facultySql = "INSERT INTO faculty (user_id, name, department, email, status) " +
                        "VALUES (?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement ps = con.prepareStatement(facultySql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setString(2, name.trim());
                    ps.setString(3, department.trim());
                    setNullableText(ps, 4, email);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Failed to retrieve generated faculty ID.");
                        facultyId = rs.getInt(1);
                    }
                }

                con.commit();
                return new Faculty(facultyId, userId, name.trim(), department.trim(),
                        cleanNullable(email), "ACTIVE", username.trim());
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public Optional<Faculty> findById(int facultyId) throws SQLException {
        return findOne("WHERE f.faculty_id = ?", facultyId);
    }

    public Optional<Faculty> findByUserId(int userId) throws SQLException {
        return findOne("WHERE f.user_id = ?", userId);
    }

    public List<Faculty> findAll() throws SQLException {
        return query(SELECT_SQL + "ORDER BY f.faculty_id");
    }

    /**
     * Searches by name, department, or email. A null/blank query returns all.
     * Status may be null, "ALL", "ACTIVE", or "INACTIVE".
     */
    public List<Faculty> search(String query, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_SQL).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(f.name) LIKE ? ESCAPE '\\' " +
                    "OR LOWER(f.department) LIKE ? ESCAPE '\\' " +
                    "OR LOWER(COALESCE(f.email, '')) LIKE ? ESCAPE '\\') ");
            String escaped = escapeLike(query.trim().toLowerCase(Locale.ROOT));
            String pattern = "%" + escaped + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append("AND f.status = ? ");
            params.add(status.trim().toUpperCase(Locale.ROOT));
        }
        sql.append("ORDER BY f.name COLLATE NOCASE, f.faculty_id");

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Faculty> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        }
    }

    /** Updates profile details; login username and password are unchanged. */
    public boolean update(int facultyId, String name, String department, String email) throws SQLException {
        requireText(name, "Name");
        requireText(department, "Department");

        String sql = "UPDATE faculty SET name = ?, department = ?, email = ? WHERE faculty_id = ?";
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, department.trim());
            setNullableText(ps, 3, email);
            ps.setInt(4, facultyId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deactivate(int facultyId) throws SQLException {
        return setActive(facultyId, false);
    }

    public boolean activate(int facultyId) throws SQLException {
        return setActive(facultyId, true);
    }

    private boolean setActive(int facultyId, boolean active) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT user_id FROM faculty WHERE faculty_id = ?")) {
                    ps.setInt(1, facultyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        userId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE faculty SET status = ? WHERE faculty_id = ?")) {
                    ps.setString(1, active ? "ACTIVE" : "INACTIVE");
                    ps.setInt(2, facultyId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE users SET is_active = ? WHERE user_id = ?")) {
                    ps.setInt(1, active ? 1 : 0);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private Optional<Faculty> findOne(String condition, int id) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_SQL + condition)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private List<Faculty> query(String sql) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Faculty> result = new ArrayList<>();
            while (rs.next()) result.add(mapRow(rs));
            return result;
        }
    }

    private Faculty mapRow(ResultSet rs) throws SQLException {
        return new Faculty(rs.getInt("faculty_id"), rs.getInt("user_id"),
                rs.getString("name"), rs.getString("department"), rs.getString("email"),
                rs.getString("status"), rs.getString("username"));
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty");
        }
    }

    private static String cleanNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static void setNullableText(PreparedStatement ps, int index, String value) throws SQLException {
        String cleaned = cleanNullable(value);
        if (cleaned == null) ps.setNull(index, java.sql.Types.VARCHAR);
        else ps.setString(index, cleaned);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }
}
