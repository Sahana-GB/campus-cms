package com.campus.cms.grading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Student UI panel displaying published grades and feedback for a student.
 */
public class StudentGradesPanel extends JPanel {
    private final int studentId;
    private final int courseId;
    private final GradeDAO gradeDAO;
    private final AssignmentDAO assignmentDAO;
    private final DefaultTableModel tableModel;
    private final JLabel summaryLabel;

    public StudentGradesPanel(int studentId) {
        this(studentId, -1);
    }

    public StudentGradesPanel(int studentId, int courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.gradeDAO = new GradeDAO();
        this.assignmentDAO = new AssignmentDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("My Published Grades");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(header, BorderLayout.NORTH);

        String[] cols = {"Assignment", "Type", "Marks Obtained", "Max Marks", "Percentage", "Feedback"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        summaryLabel = new JLabel("Published Grades: 0");
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        footer.add(summaryLabel, BorderLayout.WEST);
        footer.add(refreshBtn, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        tableModel.setRowCount(0);
        Map<Integer, Assignment> assignmentMap = new HashMap<>();
        if (courseId > 0) {
            for (Assignment a : assignmentDAO.getAssignmentsByCourse(courseId)) {
                assignmentMap.put(a.getAssignmentId(), a);
            }
        }

        List<Grade> grades = gradeDAO.getStudentPublishedGrades(studentId);
        double totalScored = 0.0;
        double totalMax = 0.0;
        int count = 0;

        for (Grade g : grades) {
            Assignment a = assignmentMap.get(g.getAssignmentId());
            if (courseId > 0 && a == null) {
                continue;
            }
            String title = a != null ? a.getTitle() : "Assignment #" + g.getAssignmentId();
            String type = a != null ? a.getType() : "-";
            double max = a != null ? a.getMaxMarks() : 0.0;
            String pct = max > 0 ? String.format("%.1f%%", (g.getMarks() / max) * 100.0) : "-";

            tableModel.addRow(new Object[]{
                    title,
                    type,
                    g.getMarks(),
                    max > 0 ? max : "-",
                    pct,
                    g.getFeedback() == null ? "" : g.getFeedback()
            });

            if (max > 0) {
                totalScored += g.getMarks();
                totalMax += max;
            }
            count++;
        }

        String avgText = totalMax > 0 ? String.format(" | Overall Score: %.1f%%", (totalScored / totalMax) * 100.0) : "";
        summaryLabel.setText("Published Grades: " + count + avgText);
    }
}
