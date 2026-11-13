package com.campus.cms.catalog;

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
 * Data Access Object for course catalogue operations.
 *
 * This DAO handles course details, faculty assignment and archival. It does
 * not implement prerequisites or timetable/clash management.
 */
public class CourseDAO {

    private static final String SELECT_SQL =
            "SELECT c.course_id, c.code, c.title, c.credits, c.capacity, c.semester, " +
            "c.slot, c.faculty_id, c.status, f.name AS faculty_name " +
            "FROM courses c LEFT JOIN faculty f ON f.faculty_id = c.faculty_id ";

    /**
     * Creates a course. facultyId may be null when the course is not yet assigned.
     */
    public Course createCourse(String code, String title, int credits, int capacity,
                               String semester, String slot, Integer facultyId) throws SQLException {
        validateCourse(code, title, credits, capacity, semester, slot);
        try (Connection con = Database.getConnection()) {
            validateFaculty(con, facultyId);
            String sql = "INSERT INTO courses " +
                    "(code, title, credits, capacity, semester, slot, faculty_id, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, code.trim().toUpperCase(Locale.ROOT));
                ps.setString(2, title.trim());
                ps.setInt(3, credits);
                ps.setInt(4, capacity);
                ps.setString(5, semester.trim());
                ps.setString(6, slot.trim().toUpperCase(Locale.ROOT));
                if (facultyId == null) ps.setNull(7, java.sql.Types.INTEGER);
                else ps.setInt(7, facultyId);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Failed to retrieve generated course ID.");
                    return findById(con, rs.getInt(1)).orElseThrow(
                            () -> new SQLException("Course was created but could not be read back."));
                }
            }
        }
    }

    public Optional<Course> findById(int courseId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            return findById(con, courseId);
        }
    }

    public Optional<Course> findByCode(String code) throws SQLException {
        if (code == null || code.trim().isEmpty()) return Optional.empty();
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_SQL + "WHERE UPPER(c.code) = ?")) {
            ps.setString(1, code.trim().toUpperCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Course> findAll() throws SQLException {
        return query(SELECT_SQL + "ORDER BY c.code");
    }

    /**
     * Searches code/title/semester and optionally filters by status and faculty.
     * Use null or "ALL" to omit the status filter; use null to omit faculty filter.
     */
    public List<Course> search(String query, String status, Integer facultyId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_SQL).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(c.code) LIKE ? ESCAPE '\\' " +
                    "OR LOWER(c.title) LIKE ? ESCAPE '\\' " +
                    "OR LOWER(c.semester) LIKE ? ESCAPE '\\') ");
            String escaped = escapeLike(query.trim().toLowerCase(Locale.ROOT));
            String pattern = "%" + escaped + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append("AND c.status = ? ");
            params.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (facultyId != null) {
            sql.append("AND c.faculty_id = ? ");
            params.add(facultyId);
        }
        sql.append("ORDER BY c.code");

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<Course> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        }
    }

    /**
     * Updates course details. The capacity cannot be reduced below the number
     * of currently approved enrollments. A null facultyId removes the assignment.
     */
    public boolean update(int courseId, String code, String title, int credits, int capacity,
                          String semester, String slot, Integer facultyId) throws SQLException {
        validateCourse(code, title, credits, capacity, semester, slot);
        try (Connection con = Database.getConnection()) {
            validateFaculty(con, facultyId);
            int enrolled = countApproved(con, courseId);
            if (enrolled < 0) return false;
            if (capacity < enrolled) {
                throw new IllegalArgumentException(
                        "Capacity cannot be less than the " + enrolled + " approved enrollment(s).");
            }

            String sql = "UPDATE courses SET code = ?, title = ?, credits = ?, capacity = ?, " +
                    "semester = ?, slot = ?, faculty_id = ? WHERE course_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, code.trim().toUpperCase(Locale.ROOT));
                ps.setString(2, title.trim());
                ps.setInt(3, credits);
                ps.setInt(4, capacity);
                ps.setString(5, semester.trim());
                ps.setString(6, slot.trim().toUpperCase(Locale.ROOT));
                if (facultyId == null) ps.setNull(7, java.sql.Types.INTEGER);
                else ps.setInt(7, facultyId);
                ps.setInt(8, courseId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    /** Assigns or unassigns a faculty member without changing other course fields. */
    public boolean assignFaculty(int courseId, Integer facultyId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            validateFaculty(con, facultyId);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE courses SET faculty_id = ? WHERE course_id = ?")) {
                if (facultyId == null) ps.setNull(1, java.sql.Types.INTEGER);
                else ps.setInt(1, facultyId);
                ps.setInt(2, courseId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    /** Archives a course so it is no longer offered for new enrollments. */
    public boolean archive(int courseId) throws SQLException {
        return changeStatus(courseId, "ARCHIVED");
    }

    /** Reactivates an archived course, subject to the schema's active-slot constraint. */
    public boolean activate(int courseId) throws SQLException {
        return changeStatus(courseId, "ACTIVE");
    }

    private boolean changeStatus(int courseId, String status) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE courses SET status = ? WHERE course_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;
        }
    }

    private Optional<Course> findById(Connection con, int courseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(SELECT_SQL + "WHERE c.course_id = ?")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private List<Course> query(String sql) throws SQLException {
        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Course> result = new ArrayList<>();
            while (rs.next()) result.add(mapRow(rs));
            return result;
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        int rawFacultyId = rs.getInt("faculty_id");
        Integer facultyId = rs.wasNull() ? null : rawFacultyId;
        return new Course(rs.getInt("course_id"), rs.getString("code"),
                rs.getString("title"), rs.getInt("credits"), rs.getInt("capacity"),
                rs.getString("semester"), rs.getString("slot"), facultyId,
                rs.getString("status"), rs.getString("faculty_name"));
    }

    private int countApproved(Connection con, int courseId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM enrollments WHERE course_id = ? AND status = 'APPROVED'")) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void validateFaculty(Connection con, Integer facultyId) throws SQLException {
        if (facultyId == null) return;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT status FROM faculty WHERE faculty_id = ?")) {
            ps.setInt(1, facultyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Faculty member does not exist.");
                if (!"ACTIVE".equalsIgnoreCase(rs.getString("status"))) {
                    throw new IllegalArgumentException("Cannot assign an inactive faculty member.");
                }
            }
        }
    }

    private static void validateCourse(String code, String title, int credits, int capacity,
                                       String semester, String slot) {
        requireText(code, "Course code");
        requireText(title, "Course title");
        requireText(semester, "Semester");
        requireText(slot, "Slot");
        if (credits <= 0) throw new IllegalArgumentException("Credits must be greater than zero.");
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be greater than zero.");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
