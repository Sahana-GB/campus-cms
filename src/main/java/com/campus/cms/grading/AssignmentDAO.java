package com.campus.cms.grading;

import com.campus.cms.core.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO {

    public boolean createAssignment(Assignment assignment) {
        String sql = "INSERT INTO assignments (course_id, title, type, description, due_date, max_marks, weight, status, material_path) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, assignment.getCourseId());
            pstmt.setString(2, assignment.getTitle());
            pstmt.setString(3, assignment.getType());
            pstmt.setString(4, assignment.getDescription());
            pstmt.setString(5, assignment.getDueDate());
            pstmt.setDouble(6, assignment.getMaxMarks());
            pstmt.setDouble(7, assignment.getWeight());
            pstmt.setString(8, assignment.getStatus());
            pstmt.setString(9, assignment.getMaterialPath());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Assignment> getAssignmentsByCourse(int courseId) {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT * FROM assignments WHERE course_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAssignment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateStatus(int assignmentId, String status) {
        String sql = "UPDATE assignments SET status = ? WHERE assignment_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, assignmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        return new Assignment(
            rs.getInt("assignment_id"),
            rs.getInt("course_id"),
            rs.getString("title"),
            rs.getString("type"),
            rs.getString("description"),
            rs.getString("due_date"),
            rs.getDouble("max_marks"),
            rs.getDouble("weight"),
            rs.getString("status"),
            rs.getString("material_path")
        );
    }
}
