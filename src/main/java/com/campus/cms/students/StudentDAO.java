package com.campus.cms.students;

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
 * Data Access Object for Student operations, managing transactional student
 * creation, querying, searching, updating, and deactivation.
 */
public class StudentDAO {

    /**
     * Creates a new Student and associated User record inside a transaction.
     *
     * @param username student's login username
     * @param password student's login password
     * @param rollNumber unique student roll number
     * @param name student's full name
     * @param branch academic branch
     * @param year academic year (between 1 and 6)
     * @param email student email address
     * @return the created Student entity with generated IDs
     * @throws SQLException if a database error or constraint violation occurs
     */
    public Student createStudent(String username, String password, String rollNumber,
                                 String name, String branch, int year, String email) throws SQLException {

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (branch == null || branch.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch cannot be empty");
        }
        if (year < 1 || year > 6) {
            throw new IllegalArgumentException("Year must be between 1 and 6");
        }

        String passwordHash = PasswordUtil.hashPassword(password);

        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1. Insert into users table
                int userId;
                String insertUserSql = "INSERT INTO users (username, password_hash, role, is_active) VALUES (?, ?, 'STUDENT', 1)";
                try (PreparedStatement psUser = con.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    psUser.setString(1, username.trim());
                    psUser.setString(2, passwordHash);
                    psUser.executeUpdate();

                    try (ResultSet rs = psUser.getGeneratedKeys()) {
                        if (rs.next()) {
                            userId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated user ID.");
                        }
                    }
                }

                // 2. Insert into students table
                int studentId;
                String insertStudentSql = "INSERT INTO students (user_id, roll_number, name, branch, year, email, status) VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement psStudent = con.prepareStatement(insertStudentSql, Statement.RETURN_GENERATED_KEYS)) {
                    psStudent.setInt(1, userId);
                    psStudent.setString(2, rollNumber.trim());
                    psStudent.setString(3, name.trim());
                    psStudent.setString(4, branch.trim());
                    psStudent.setInt(5, year);
                    psStudent.setString(6, email != null ? email.trim() : null);
                    psStudent.executeUpdate();

                    try (ResultSet rs = psStudent.getGeneratedKeys()) {
                        if (rs.next()) {
                            studentId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to retrieve generated student ID.");
                        }
                    }
                }

                con.commit();
                return new Student(studentId, userId, rollNumber.trim(), name.trim(),
                        branch.trim(), year, email != null ? email.trim() : null, "ACTIVE", username.trim());

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Finds a student by primary key student ID.
     */
    public Optional<Student> findById(int studentId) throws SQLException {
        String sql = "SELECT s.student_id, s.user_id, s.roll_number, s.name, s.branch, s.year, s.email, s.status, u.username " +
                "FROM students s JOIN users u ON s.user_id = u.user_id WHERE s.student_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a student by unique roll number.
     */
    public Optional<Student> findByRollNumber(String rollNumber) throws SQLException {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT s.student_id, s.user_id, s.roll_number, s.name, s.branch, s.year, s.email, s.status, u.username " +
                "FROM students s JOIN users u ON s.user_id = u.user_id WHERE s.roll_number = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds a student by foreign key user ID.
     */
    public Optional<Student> findByUserId(int userId) throws SQLException {
        String sql = "SELECT s.student_id, s.user_id, s.roll_number, s.name, s.branch, s.year, s.email, s.status, u.username " +
                "FROM students s JOIN users u ON s.user_id = u.user_id WHERE s.user_id = ?";

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
     * Retrieves all students ordered by student ID.
     */
    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.student_id, s.user_id, s.roll_number, s.name, s.branch, s.year, s.email, s.status, u.username " +
                "FROM students s JOIN users u ON s.user_id = u.user_id ORDER BY s.student_id ASC";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                students.add(mapRow(rs));
            }
        }
        return students;
    }

    /**
     * Searches students by query (name or roll number substring), branch, and academic year.
     *
     * @param query text to match in name or roll number (nullable)
     * @param branch branch name or "ALL" for all branches (nullable)
     * @param year academic year (nullable or <= 0 for all years)
     * @return matching list of students
     * @throws SQLException if a database error occurs
     */
    public List<Student> search(String query, String branch, Integer year) throws SQLException {
        List<Student> students = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT s.student_id, s.user_id, s.roll_number, s.name, s.branch, s.year, s.email, s.status, u.username " +
                        "FROM students s JOIN users u ON s.user_id = u.user_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append("AND (LOWER(s.name) LIKE ? ESCAPE '\\' OR LOWER(s.roll_number) LIKE ? ESCAPE '\\') ");
            String escaped = query.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String wildcard = "%" + escaped + "%";
            params.add(wildcard);
            params.add(wildcard);
        }

        if (branch != null && !branch.trim().isEmpty() && !"ALL".equalsIgnoreCase(branch.trim())) {
            sql.append("AND s.branch = ? ");
            params.add(branch.trim());
        }

        if (year != null && year > 0) {
            sql.append("AND s.year = ? ");
            params.add(year);
        }

        sql.append("ORDER BY s.student_id ASC");

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    students.add(mapRow(rs));
                }
            }
        }
        return students;
    }

    /**
     * Updates an existing student's name, branch, year, and email.
     */
    public boolean update(int studentId, String name, String branch, int year, String email) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (branch == null || branch.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch cannot be empty");
        }
        if (year < 1 || year > 6) {
            throw new IllegalArgumentException("Year must be between 1 and 6");
        }

        String sql = "UPDATE students SET name = ?, branch = ?, year = ?, email = ? WHERE student_id = ?";

        try (Connection con = Database.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, branch.trim());
            ps.setInt(3, year);
            ps.setString(4, email != null ? email.trim() : null);
            ps.setInt(5, studentId);

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deactivates a student account by setting students.status = 'INACTIVE'
     * and users.is_active = 0 in a transaction. Does NOT delete records.
     */
    public boolean deactivate(int studentId) throws SQLException {
        try (Connection con = Database.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Find associated user_id
                Integer userId = null;
                String findUserSql = "SELECT user_id FROM students WHERE student_id = ?";
                try (PreparedStatement ps = con.prepareStatement(findUserSql)) {
                    ps.setInt(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                        }
                    }
                }

                if (userId == null) {
                    con.rollback();
                    return false;
                }

                // Deactivate student record
                String updateStudentSql = "UPDATE students SET status = 'INACTIVE' WHERE student_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateStudentSql)) {
                    ps.setInt(1, studentId);
                    ps.executeUpdate();
                }

                // Deactivate user record
                String updateUserSql = "UPDATE users SET is_active = 0 WHERE user_id = ?";
                try (PreparedStatement ps = con.prepareStatement(updateUserSql)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                con.commit();
                return true;

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        int studentId = rs.getInt("student_id");
        int userId = rs.getInt("user_id");
        String rollNumber = rs.getString("roll_number");
        String name = rs.getString("name");
        String branch = rs.getString("branch");
        int year = rs.getInt("year");
        String email = rs.getString("email");
        String status = rs.getString("status");
        String username = rs.getString("username");

        return new Student(studentId, userId, rollNumber, name, branch, year, email, status, username);
    }
}
