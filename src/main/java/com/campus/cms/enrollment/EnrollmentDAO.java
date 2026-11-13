package com.campus.cms.enrollment;

import com.campus.cms.reports.ReportTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for enrollments. No business rules here: those live in EnrollmentService. */
public class EnrollmentDAO {

    /** One course as a student sees it on the Browse screen. */
    public record BrowseRow(int courseId, String code, String title, int credits, int capacity,
                            String slot, String facultyName, int taken, String myStatus) {
        public int seatsLeft() {
            return Math.max(0, capacity - taken);
        }
    }

    public record CourseInfo(int courseId, String code, String status, int capacity) { }

    /** An entry for the course drop-down on the admin screen. */
    public record CourseOption(int courseId, String code, String title) {
        @Override
        public String toString() {
            return code.isEmpty() ? title : code + " - " + title;
        }
    }

    // ------------------------------------------------------------------ SQL
    static final String JOINED_SQL =
            "SELECT e.enrollment_id, e.student_id, e.course_id, e.status, e.request_date, "
            + "s.roll_number, s.name AS student_name, c.code, c.title, c.slot, f.name AS faculty_name "
            + "FROM enrollments e "
            + "JOIN students s ON s.student_id = e.student_id "
            + "JOIN courses c ON c.course_id = e.course_id "
            + "LEFT JOIN faculty f ON f.faculty_id = c.faculty_id ";

    static final String FIND_BY_ID_SQL = JOINED_SQL + "WHERE e.enrollment_id = ?";

    static final String FIND_PAIR_SQL = JOINED_SQL + "WHERE e.student_id = ? AND e.course_id = ?";

    static final String FIND_BY_STUDENT_SQL = JOINED_SQL
            + "WHERE e.student_id = ? ORDER BY e.request_date DESC, e.enrollment_id DESC";

    static final String SEARCH_ORDER_SQL = "ORDER BY c.code, s.roll_number";

    static final String INSERT_SQL = "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)";

    static final String REOPEN_SQL =
            "UPDATE enrollments SET status = 'PENDING', request_date = date('now') WHERE enrollment_id = ?";

    static final String UPDATE_STATUS_SQL = "UPDATE enrollments SET status = ? WHERE enrollment_id = ?";

    /**
     * Approve one PENDING request only if the student and course are ACTIVE and a seat is free.
     * It is a single statement, so two approvals can never both take the last seat.
     */
    static final String APPROVE_SQL =
            "UPDATE enrollments SET status = 'APPROVED' "
            + "WHERE enrollment_id = ? AND status = 'PENDING' "
            + "AND EXISTS (SELECT 1 FROM students s "
            + "            WHERE s.student_id = enrollments.student_id AND s.status = 'ACTIVE') "
            + "AND EXISTS (SELECT 1 FROM courses c "
            + "            WHERE c.course_id = enrollments.course_id AND c.status = 'ACTIVE' "
            + "              AND c.capacity > (SELECT COUNT(*) FROM enrollments x "
            + "                                WHERE x.course_id = c.course_id AND x.status = 'APPROVED'))";

    static final String COUNT_APPROVED_SQL =
            "SELECT COUNT(*) FROM enrollments WHERE course_id = ? AND status = 'APPROVED'";

    static final String COUNT_BY_STATUS_SQL = "SELECT COUNT(*) FROM enrollments WHERE status = ?";

    static final String COUNT_FOR_STUDENT_SQL =
            "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND status = ?";

    static final String FIND_COURSE_SQL =
            "SELECT course_id, code, status, capacity FROM courses WHERE course_id = ?";

    static final String STUDENT_STATUS_SQL = "SELECT status FROM students WHERE student_id = ?";

    static final String COURSE_OPTIONS_SQL = "SELECT course_id, code, title FROM courses ORDER BY code";

    static final String BROWSE_SQL =
            "SELECT c.course_id, c.code, c.title, c.credits, c.capacity, c.slot, "
            + "COALESCE(f.name, '(not assigned)') AS faculty_name, "
            + "(SELECT COUNT(*) FROM enrollments a "
            + " WHERE a.course_id = c.course_id AND a.status = 'APPROVED') AS taken, "
            + "(SELECT e.status FROM enrollments e "
            + " WHERE e.course_id = c.course_id AND e.student_id = ?) AS my_status "
            + "FROM courses c LEFT JOIN faculty f ON f.faculty_id = c.faculty_id "
            + "WHERE c.status = 'ACTIVE' ORDER BY c.code";

    static final String REPORT_COUNTS_SQL =
            "SELECT c.code AS \"Code\", c.title AS \"Title\", c.slot AS \"Slot\", "
            + "c.status AS \"Course status\", c.capacity AS \"Capacity\", "
            + "SUM(CASE WHEN e.status = 'APPROVED' THEN 1 ELSE 0 END) AS \"Approved\", "
            + "SUM(CASE WHEN e.status = 'PENDING' THEN 1 ELSE 0 END) AS \"Pending\", "
            + "c.capacity - SUM(CASE WHEN e.status = 'APPROVED' THEN 1 ELSE 0 END) AS \"Seats left\" "
            + "FROM courses c LEFT JOIN enrollments e ON e.course_id = c.course_id "
            + "GROUP BY c.course_id ORDER BY c.code";

    static final String REPORT_ROSTER_SQL =
            "SELECT s.roll_number AS \"Roll number\", s.name AS \"Name\", s.branch AS \"Branch\", "
            + "s.year AS \"Year\", s.email AS \"Email\" "
            + "FROM enrollments e JOIN students s ON s.student_id = e.student_id "
            + "WHERE e.course_id = ? AND e.status = 'APPROVED' ORDER BY s.roll_number";

    private static final String COURSE_PCT =
            "CASE WHEN SUM(a.weight) > 0 "
            + "THEN SUM(g.marks * 1.0 / a.max_marks * a.weight) / SUM(a.weight) * 100.0 "
            + "ELSE AVG(g.marks * 1.0 / a.max_marks) * 100.0 END";

    static final String REPORT_GRADE_AVERAGES_SQL =
            "SELECT code AS \"Code\", title AS \"Title\", COUNT(*) AS \"Students graded\", "
            + "ROUND(AVG(pct), 2) AS \"Average\", ROUND(MAX(pct), 2) AS \"Highest\", "
            + "ROUND(MIN(pct), 2) AS \"Lowest\" "
            + "FROM (SELECT co.course_id, co.code, co.title, g.student_id, " + COURSE_PCT + " AS pct "
            + "      FROM grades g "
            + "      JOIN assignments a ON a.assignment_id = g.assignment_id "
            + "      JOIN courses co ON co.course_id = a.course_id "
            + "      WHERE g.published = 1 "
            + "      GROUP BY co.course_id, g.student_id) "
            + "GROUP BY course_id ORDER BY code";

    static final String REPORT_TRANSCRIPT_SQL =
            "SELECT code AS \"Code\", title AS \"Title\", credits AS \"Credits\", "
            + "ROUND(pct, 2) AS \"Percentage\", "
            + "CASE WHEN pct >= 90 THEN 'A' WHEN pct >= 80 THEN 'B' WHEN pct >= 70 THEN 'C' "
            + "     WHEN pct >= 60 THEN 'D' WHEN pct >= 50 THEN 'E' ELSE 'F' END AS \"Letter grade\" "
            + "FROM (SELECT co.code, co.title, co.credits, " + COURSE_PCT + " AS pct "
            + "      FROM grades g "
            + "      JOIN assignments a ON a.assignment_id = g.assignment_id "
            + "      JOIN courses co ON co.course_id = a.course_id "
            + "      WHERE g.student_id = ? AND g.published = 1 "
            + "      GROUP BY co.course_id) "
            + "ORDER BY code";

    static final String REPORT_HISTORY_SQL =
            "SELECT c.code AS \"Code\", c.title AS \"Title\", c.credits AS \"Credits\", "
            + "c.slot AS \"Slot\", e.status AS \"Status\", e.request_date AS \"Requested on\" "
            + "FROM enrollments e JOIN courses c ON c.course_id = e.course_id "
            + "WHERE e.student_id = ? ORDER BY e.request_date, c.code";

    // ------------------------------------------------------------ queries
    public Enrollment findById(Connection c, int enrollmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(FIND_BY_ID_SQL)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Enrollment find(Connection c, int studentId, int courseId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(FIND_PAIR_SQL)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Enrollment> findByStudent(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(FIND_BY_STUDENT_SQL)) {
            ps.setInt(1, studentId);
            return mapAll(ps);
        }
    }

    /** Filter by course and/or status. A null filter means "any". */
    public List<Enrollment> search(Connection c, Integer courseId, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(JOINED_SQL).append("WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (courseId != null) {
            sql.append("AND e.course_id = ? ");
            params.add(courseId);
        }
        if (status != null) {
            sql.append("AND e.status = ? ");
            params.add(status);
        }
        sql.append(SEARCH_ORDER_SQL);
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return mapAll(ps);
        }
    }

    public List<BrowseRow> findBrowseRows(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(BROWSE_SQL)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BrowseRow> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new BrowseRow(rs.getInt("course_id"), rs.getString("code"),
                            rs.getString("title"), rs.getInt("credits"), rs.getInt("capacity"),
                            rs.getString("slot"), rs.getString("faculty_name"),
                            rs.getInt("taken"), rs.getString("my_status")));
                }
                return rows;
            }
        }
    }

    public CourseInfo findCourse(Connection c, int courseId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(FIND_COURSE_SQL)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new CourseInfo(rs.getInt("course_id"), rs.getString("code"),
                                rs.getString("status"), rs.getInt("capacity"))
                        : null;
            }
        }
    }

    /** Returns the student's status (ACTIVE or INACTIVE), or null if there is no such student. */
    public String studentStatus(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(STUDENT_STATUS_SQL)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public List<CourseOption> findCourseOptions(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COURSE_OPTIONS_SQL);
             ResultSet rs = ps.executeQuery()) {
            List<CourseOption> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new CourseOption(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            return list;
        }
    }

    public int countApproved(Connection c, int courseId) throws SQLException {
        return count(c, COUNT_APPROVED_SQL, courseId);
    }

    public int countByStatus(Connection c, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COUNT_BY_STATUS_SQL)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public int countForStudent(Connection c, int studentId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(COUNT_FOR_STUDENT_SQL)) {
            ps.setInt(1, studentId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // ------------------------------------------------------------- writes
    public void insertPending(Connection c, int studentId, int courseId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);
            ps.executeUpdate();
        }
    }

    /** Re-enrolling after a drop: the old row goes back to PENDING (the table allows one row per pair). */
    public void reopen(Connection c, int enrollmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REOPEN_SQL)) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Connection c, int enrollmentId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(UPDATE_STATUS_SQL)) {
            ps.setString(1, status);
            ps.setInt(2, enrollmentId);
            ps.executeUpdate();
        }
    }

    /** @return true if the request was approved, false if it could not be (full, inactive, not pending...). */
    public boolean approveIfSeatAvailable(Connection c, int enrollmentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(APPROVE_SQL)) {
            ps.setInt(1, enrollmentId);
            return ps.executeUpdate() == 1;
        }
    }

    // ------------------------------------------------------------ reports
    public ReportTable enrollmentCountsReport(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REPORT_COUNTS_SQL);
             ResultSet rs = ps.executeQuery()) {
            return ReportTable.from("Enrollment counts per course", rs);
        }
    }

    public ReportTable courseRosterReport(Connection c, int courseId, String title) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REPORT_ROSTER_SQL)) {
            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return ReportTable.from(title, rs);
            }
        }
    }

    public ReportTable studentHistoryReport(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REPORT_HISTORY_SQL)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return ReportTable.from("Student enrollment history", rs);
            }
        }
    }

    // ------------------------------------------------------------ helpers
    private int count(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private List<Enrollment> mapAll(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Enrollment> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setEnrollmentId(rs.getInt("enrollment_id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setCourseId(rs.getInt("course_id"));
        e.setStatus(rs.getString("status"));
        e.setRequestDate(rs.getString("request_date"));
        e.setRollNumber(rs.getString("roll_number"));
        e.setStudentName(rs.getString("student_name"));
        e.setCourseCode(rs.getString("code"));
        e.setCourseTitle(rs.getString("title"));
        e.setSlot(rs.getString("slot"));
        String faculty = rs.getString("faculty_name");
        e.setFacultyName(faculty == null ? "(not assigned)" : faculty);
        return e;
    }

    public ReportTable gradeAveragesReport(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REPORT_GRADE_AVERAGES_SQL);
             ResultSet rs = ps.executeQuery()) {
            return ReportTable.from("Grade averages per course (published grades, percent)", rs);
        }
    }

    public ReportTable studentTranscriptReport(Connection c, int studentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(REPORT_TRANSCRIPT_SQL)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return ReportTable.from("Student transcript", rs);
            }
        }
    }
}
