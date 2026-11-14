package com.campus.cms.enrollment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CGPA is never stored; it is computed from PUBLISHED grades only (draft marks are ignored).
 *
 * Formula:
 *  1. Course percentage = sum(marks / max_marks * weight) / sum(weight) * 100.
 *     If the course has no weights set (all 0), use the plain average of marks / max_marks.
 *  2. CGPA = credit-weighted average of the course percentages, divided by 10 (so the range is 0 to 10).
 *  3. A student with no published grades has CGPA 0.0.
 */
public class CgpaCalculator {

    static final String GRADES_SQL =
            "SELECT a.course_id, co.credits, a.max_marks, a.weight, g.marks "
            + "FROM grades g "
            + "JOIN assignments a ON a.assignment_id = g.assignment_id "
            + "JOIN courses co ON co.course_id = a.course_id "
            + "WHERE g.student_id = ? AND g.published = 1";

    public double compute(Connection c, int studentId) throws SQLException {
        // per course: [credits, weightedSum, weightTotal, plainSum, count]
        Map<Integer, double[]> perCourse = new LinkedHashMap<>();
        try (PreparedStatement ps = c.prepareStatement(GRADES_SQL)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double[] acc = perCourse.computeIfAbsent(rs.getInt("course_id"),
                            k -> new double[5]);
                    double fraction = rs.getDouble("marks") / rs.getDouble("max_marks");
                    double weight = rs.getDouble("weight");
                    acc[0] = rs.getInt("credits");
                    acc[1] += fraction * weight;
                    acc[2] += weight;
                    acc[3] += fraction;
                    acc[4] += 1;
                }
            }
        }
        double creditSum = 0;
        double pointSum = 0;
        for (double[] acc : perCourse.values()) {
            double percentage = acc[2] > 0 ? acc[1] / acc[2] * 100.0 : acc[3] / acc[4] * 100.0;
            creditSum += acc[0];
            pointSum += acc[0] * (percentage / 10.0);
        }
        if (creditSum == 0) {
            return 0.0;
        }
        return Math.round(pointSum / creditSum * 100.0) / 100.0;
    }
}
