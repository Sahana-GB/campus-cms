package com.campus.cms.grading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Faculty UI panel summarizing assignment statistics (graded count, average, min, max) for a course.
 */
public class FacultyReportsPanel extends JPanel {
    private final int courseId;
    private final AssignmentDAO assignmentDAO;
    private final GradeDAO gradeDAO;
    private final DefaultTableModel tableModel;

    public FacultyReportsPanel(int courseId) {
        this.courseId = courseId;
        this.assignmentDAO = new AssignmentDAO();
        this.gradeDAO = new GradeDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("Course Assessment Analytics & Grade Summary");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(header, BorderLayout.NORTH);

        String[] cols = {"Assignment", "Type", "Max Marks", "Weight %", "Graded Students", "Published", "Average", "Highest", "Lowest"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh Report");
        refreshBtn.addActionListener(e -> refresh());
        footer.add(refreshBtn);
        add(footer, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        tableModel.setRowCount(0);
        List<Assignment> assignments = assignmentDAO.getAssignmentsByCourse(courseId);

        for (Assignment a : assignments) {
            List<Grade> grades = gradeDAO.getGradesForAssignment(a.getAssignmentId());
            int count = grades.size();
            long publishedCount = grades.stream().filter(g -> g.getPublished() == 1).count();

            if (count == 0) {
                tableModel.addRow(new Object[]{
                        a.getTitle(), a.getType(), a.getMaxMarks(), a.getWeight(),
                        0, 0, "-", "-", "-"
                });
            } else {
                double sum = 0.0;
                double max = Double.MIN_VALUE;
                double min = Double.MAX_VALUE;
                for (Grade g : grades) {
                    double m = g.getMarks();
                    sum += m;
                    if (m > max) max = m;
                    if (m < min) min = m;
                }
                double avg = sum / count;
                tableModel.addRow(new Object[]{
                        a.getTitle(),
                        a.getType(),
                        a.getMaxMarks(),
                        a.getWeight(),
                        count,
                        publishedCount,
                        String.format("%.2f", avg),
                        String.format("%.2f", max),
                        String.format("%.2f", min)
                });
            }
        }
    }
}