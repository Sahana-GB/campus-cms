package com.campus.cms.grading;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Faculty UI panel to create, view, and open/close assignments and quizzes for a course.
 */
public class AssignmentsPanel extends JPanel {
    private final int courseId;
    private final AssignmentDAO assignmentDAO;
    private final DefaultTableModel tableModel;
    private final JTable table;

    private JTextField titleField;
    private JComboBox<String> typeCombo;
    private JTextField dueDateField;
    private JTextField maxMarksField;
    private JTextField weightField;
    private JTextField materialPathField;
    private JTextArea descriptionArea;

    public AssignmentsPanel(int courseId) {
        this.courseId = courseId;
        this.assignmentDAO = new AssignmentDAO();

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top Form: Create New Assignment
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Create New Assignment / Quiz"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        titleField = new JTextField(16);
        typeCombo = new JComboBox<>(new String[]{"ASSIGNMENT", "QUIZ"});
        dueDateField = new JTextField("2026-11-30", 10);
        maxMarksField = new JTextField("100", 6);
        weightField = new JTextField("20", 6);
        materialPathField = new JTextField(16);
        descriptionArea = new JTextArea(2, 24);

        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; formPanel.add(titleField, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 3; formPanel.add(typeCombo, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Due Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 5; formPanel.add(dueDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Max Marks:"), gbc);
        gbc.gridx = 1; formPanel.add(maxMarksField, gbc);
        gbc.gridx = 2; formPanel.add(new JLabel("Weight (%):"), gbc);
        gbc.gridx = 3; formPanel.add(weightField, gbc);
        gbc.gridx = 4; formPanel.add(new JLabel("Material Path (Optional):"), gbc);
        gbc.gridx = 5; formPanel.add(materialPathField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 4; formPanel.add(new JScrollPane(descriptionArea), gbc);
        gbc.gridwidth = 1;

        JButton createBtn = new JButton("Create Assignment");
        createBtn.addActionListener(e -> handleCreateAssignment());
        gbc.gridx = 5; formPanel.add(createBtn, gbc);

        add(formPanel, BorderLayout.NORTH);

        // Center Table: Existing Assignments
        String[] cols = {"ID", "Title", "Type", "Due Date", "Max Marks", "Weight %", "Status", "Material"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JButton toggleStatusBtn = new JButton("Toggle OPEN / CLOSED");
        toggleStatusBtn.addActionListener(e -> handleToggleStatus());

        bottomPanel.add(refreshBtn);
        bottomPanel.add(toggleStatusBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        refresh();
    }

    private void handleCreateAssignment() {
        String title = titleField.getText().trim();
        String dueDate = dueDateField.getText().trim();
        if (title.isEmpty() || dueDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Due Date are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            double maxMarks = Double.parseDouble(maxMarksField.getText().trim());
            double weight = Double.parseDouble(weightField.getText().trim());
            if (maxMarks <= 0 || weight < 0 || weight > 100) {
                JOptionPane.showMessageDialog(this, "Max marks must be > 0 and weight between 0 and 100.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String material = materialPathField.getText().trim();
            Assignment assignment = new Assignment(
                    0,
                    courseId,
                    title,
                    (String) typeCombo.getSelectedItem(),
                    descriptionArea.getText().trim(),
                    dueDate,
                    maxMarks,
                    weight,
                    "OPEN",
                    material.isEmpty() ? null : material
            );
            if (assignmentDAO.createAssignment(assignment)) {
                titleField.setText("");
                descriptionArea.setText("");
                materialPathField.setText("");
                refresh();
                JOptionPane.showMessageDialog(this, "Assignment created successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create assignment.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Max Marks and Weight must be valid numbers.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleToggleStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an assignment row first.");
            return;
        }
        int assignmentId = (int) tableModel.getValueAt(row, 0);
        String currentStatus = (String) tableModel.getValueAt(row, 6);
        String nextStatus = "OPEN".equalsIgnoreCase(currentStatus) ? "CLOSED" : "OPEN";
        if (assignmentDAO.updateStatus(assignmentId, nextStatus)) {
            refresh();
        }
    }

    public void refresh() {
        tableModel.setRowCount(0);
        List<Assignment> assignments = assignmentDAO.getAssignmentsByCourse(courseId);
        for (Assignment a : assignments) {
            Material m = Material.fromAssignment(a);
            tableModel.addRow(new Object[]{
                    a.getAssignmentId(),
                    a.getTitle(),
                    a.getType(),
                    a.getDueDate(),
                    a.getMaxMarks(),
                    a.getWeight(),
                    a.getStatus(),
                    m.hasAttachment() ? m.getMaterialPath() : "-"
            });
        }
    }
}
