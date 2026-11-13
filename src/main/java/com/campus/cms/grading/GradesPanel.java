package com.campus.cms.grading;

import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class GradesPanel extends JPanel {
    private final GradeService gradeService = new GradeService();
    private final int courseId;
    private final boolean isFaculty;
    private final int currentUserId;

    private JComboBox<Assignment> assignmentCombo;
    private JTable gradesTable;
    private DefaultTableModel tableModel;
    private JButton saveBtn;
    private JButton publishBtn;
    private JLabel statusLabel;

    public GradesPanel(int courseId, boolean isFaculty, int currentUserId) {
        this.courseId = courseId;
        this.isFaculty = isFaculty;
        this.currentUserId = currentUserId;
        initUI();
        loadAssignments();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        // Top Control Bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Select Assignment:"));
        assignmentCombo = new JComboBox<>();
        Styles.input(assignmentCombo, 260);
        assignmentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Assignment a) {
                    setText(a.getTitle() != null ? a.getTitle() : "Assignment #" + a.getAssignmentId());
                } else if (value != null) {
                    setText(value.toString());
                }
                return this;
            }
        });
        assignmentCombo.addActionListener(e -> loadGradesForSelectedAssignment());
        topPanel.add(assignmentCombo);

        if (isFaculty) {
            JButton newAssignBtn = Styles.secondary(new JButton("+ New Assignment"));
            newAssignBtn.addActionListener(e -> showNewAssignmentDialog());
            topPanel.add(newAssignBtn);
        }

        statusLabel = new JLabel("Status: -");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Student ID", "Marks", "Max Marks", "Feedback"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return isFaculty && (column == 1 || column == 3);
            }
        };

        gradesTable = new JTable(tableModel);
        Styles.table(gradesTable);

        if (!isFaculty) {
            // Hide Student ID column in student mode
            gradesTable.removeColumn(gradesTable.getColumn("Student ID"));
        }

        add(Styles.scroll(gradesTable), BorderLayout.CENTER);

        // Bottom Action Bar (Faculty Only)
        if (isFaculty) {
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            saveBtn = Styles.secondary(new JButton("Save Draft Marks"));
            publishBtn = Styles.primary(new JButton("Publish Grades"));

            saveBtn.addActionListener(e -> saveAllGrades());
            publishBtn.addActionListener(e -> togglePublishStatus());

            bottomPanel.add(saveBtn);
            bottomPanel.add(publishBtn);
            add(bottomPanel, BorderLayout.SOUTH);
        }
    }

    private void loadAssignments() {
        assignmentCombo.removeAllItems();
        try {
            List<Assignment> assignments = gradeService.getAssignmentsByCourse(courseId);
            for (Assignment a : assignments) {
                assignmentCombo.addItem(a);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading assignments: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadGradesForSelectedAssignment() {
        tableModel.setRowCount(0);
        Assignment selected = (Assignment) assignmentCombo.getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Status: No Assignment Selected");
            return;
        }

        try {
            List<Grade> grades = gradeService.getGradesByAssignment(selected.getAssignmentId());
            boolean anyPublished = !grades.isEmpty() && grades.get(0).getPublished() == 1;
            statusLabel.setText(anyPublished ? "Status: PUBLISHED" : "Status: DRAFT");

            if (isFaculty && publishBtn != null) {
                publishBtn.setText(anyPublished ? "Unpublish (Revert to Draft)" : "Publish Grades");
            }

            for (Grade g : grades) {
                // Students only see their own published grades
                if (!isFaculty && (g.getStudentId() != currentUserId || g.getPublished() == 0)) {
                    continue;
                }
                tableModel.addRow(new Object[]{
                        g.getStudentId(),
                        g.getMarks(),
                        selected.getMaxMarks(),
                        g.getFeedback() != null ? g.getFeedback() : ""
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading grades: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAllGrades() {
        if (gradesTable.isEditing()) {
            gradesTable.getCellEditor().stopCellEditing();
        }
        Assignment selected = (Assignment) assignmentCombo.getSelectedItem();
        if (selected == null) return;

        try {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                int studentId = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                double marks = Double.parseDouble(tableModel.getValueAt(row, 1).toString());
                String feedback = tableModel.getValueAt(row, 3).toString();

                Grade grade = new Grade(0, selected.getAssignmentId(), studentId, marks, feedback, 0);
                gradeService.saveOrUpdateGrade(grade);
            }
            JOptionPane.showMessageDialog(this, "All grades saved successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving grades: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void togglePublishStatus() {
        Assignment selected = (Assignment) assignmentCombo.getSelectedItem();
        if (selected == null) return;

        try {
            boolean currentlyPublished = statusLabel.getText().contains("PUBLISHED");
            int newStatus = currentlyPublished ? 0 : 1;
            gradeService.publishGrades(selected.getAssignmentId(), newStatus == 1);
            loadGradesForSelectedAssignment();
            JOptionPane.showMessageDialog(this, newStatus == 1 ? "Grades published to students!" : "Grades reverted to draft.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating publish status: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showNewAssignmentDialog() {
        JTextField titleField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"ASSIGNMENT", "QUIZ"});
        JTextField descField = new JTextField();
        JTextField dueDateField = new JTextField("2026-11-30");
        JTextField maxMarksField = new JTextField("100");
        JTextField weightField = new JTextField("20");

        Object[] fields = {
                "Title:", titleField,
                "Type:", typeBox,
                "Description:", descField,
                "Due Date (YYYY-MM-DD):", dueDateField,
                "Max Marks:", maxMarksField,
                "Weight (%):", weightField
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Create Assignment", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Assignment a = new Assignment();
                a.setCourseId(courseId);
                a.setTitle(titleField.getText());
                a.setType((String) typeBox.getSelectedItem());
                a.setDescription(descField.getText());
                a.setDueDate(dueDateField.getText());
                a.setMaxMarks(Double.parseDouble(maxMarksField.getText()));
                a.setWeight(Double.parseDouble(weightField.getText()));
                a.setStatus("OPEN");

                gradeService.createAssignment(a);
                loadAssignments();
                JOptionPane.showMessageDialog(this, "Assignment created!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error creating assignment: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public JComboBox<Assignment> getAssignmentCombo() {
        return assignmentCombo;
    }

    public JTable getGradesTable() {
        return gradesTable;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JButton getSaveBtn() {
        return saveBtn;
    }

    public JButton getPublishBtn() {
        return publishBtn;
    }

    public void refreshGrades() {
        loadGradesForSelectedAssignment();
    }
}
