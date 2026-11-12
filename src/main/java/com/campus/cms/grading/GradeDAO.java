package com.campus.cms.grading;

import com.campus.cms.core.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO {

    public boolean saveOrUpdateGrade(Grade grade) {
        String sql = "INSERT INTO grades (assignment_id, student_id, marks, feedback, published) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(assignment_id, student_id) DO UPDATE SET " +
                     "marks = excluded.marks, feedback = excluded.feedback, published = excluded.published";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grade.getAssignmentId());
            pstmt.setInt(2, grade.getStudentId());
            pstmt.setDouble(3, grade.getMarks());
            pstmt.setString(4, grade.getFeedback());
            pstmt.setInt(5, grade.getPublished());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean publishGrades(int assignmentId) {
        String sql = "UPDATE grades SET published = 1 WHERE assignment_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, assignmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Grade> getGradesForAssignment(int assignmentId) {
        List<Grade> list = new ArrayList<>();
        String sql = "SELECT * FROM grades WHERE assignment_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToGrade(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Grade> getStudentPublishedGrades(int studentId) {
        List<Grade> list = new ArrayList<>();
        String sql = "SELECT * FROM grades WHERE student_id = ? AND published = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToGrade(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Grade mapResultSetToGrade(ResultSet rs) throws SQLException {
        return new Grade(
            rs.getInt("grade_id"),
            rs.getInt("assignment_id"),
            rs.getInt("student_id"),
            rs.getDouble("marks"),
            rs.getString("feedback"),
            rs.getInt("published")
        );
    }
}
