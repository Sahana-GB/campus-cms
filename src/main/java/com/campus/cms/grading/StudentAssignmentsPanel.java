package com.campus.cms.grading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Student UI panel displaying all assignments, deadlines, weights, and study materials for a course.
 */
public class StudentAssignmentsPanel extends JPanel {
    private final int courseId;
    private final AssignmentDAO assignmentDAO;
    private final DefaultTableModel tableModel;

    public StudentAssignmentsPanel(int courseId) {
        this.courseId = courseId;
        this.assignmentDAO = new AssignmentDAO();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel header = new JLabel("Course Assignments & Quizzes");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(header, BorderLayout.NORTH);

        String[] cols = {"Title", "Type", "Due Date", "Max Marks", "Weight %", "Status", "Material", "Description"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(25);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        footer.add(refreshBtn);
        add(footer, BorderLayout.SOUTH);

        refresh();
    }

    public void refresh() {
        tableModel.setRowCount(0);
        List<Assignment> assignments = assignmentDAO.getAssignmentsByCourse(courseId);
        for (Assignment a : assignments) {
            Material m = Material.fromAssignment(a);
            tableModel.addRow(new Object[]{
                    a.getTitle(),
                    a.getType(),
                    a.getDueDate(),
                    a.getMaxMarks(),
                    a.getWeight(),
                    a.getStatus(),
                    m.hasAttachment() ? m.getMaterialPath() : "None",
                    a.getDescription() == null ? "" : a.getDescription()
            });
        }
    }
}
