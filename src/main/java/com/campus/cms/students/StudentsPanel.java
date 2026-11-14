package com.campus.cms.students;

import com.campus.cms.core.ui.FilterBar;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Admin view panel for Student Management, supporting listing, searching,
 * adding, editing, and deactivating students.
 */
public class StudentsPanel extends JPanel {

    private final StudentDAO studentDAO;
    private final List<Student> displayedStudents = new ArrayList<>();

    private JTextField searchField;
    private JComboBox<String> branchFilter;
    private JComboBox<String> yearFilter;
    private JComboBox<String> statusFilter;

    private JButton addStudentButton;
    private JButton editStudentButton;
    private JButton deactivateStudentButton;
    private JButton reactivateStudentButton;
    private JButton refreshButton;

    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    public StudentsPanel() {
        this(new StudentDAO());
    }

    public StudentsPanel(StudentDAO studentDAO) {
        this.studentDAO = Objects.requireNonNull(studentDAO);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(Styles.pagePadding());

        // ------------------ Top: header, action toolbar, filter bar ------------------
        PageHeader header = new PageHeader("Student Management",
                "Add, edit and manage student accounts.");

        // Action toolbar (separate from title and filters)
        JPanel actionToolbar = new JPanel(new FilterBar.WrapLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_XS));
        actionToolbar.setOpaque(false);

        addStudentButton = Styles.primary(new JButton("Add Student"));
        addStudentButton.addActionListener(e -> showAddStudentDialog());

        editStudentButton = Styles.secondary(new JButton("Edit Student"));
        editStudentButton.setEnabled(false);
        editStudentButton.addActionListener(e -> showEditStudentDialog());

        deactivateStudentButton = Styles.destructive(new JButton("Deactivate Student"));
        deactivateStudentButton.setEnabled(false);
        deactivateStudentButton.addActionListener(e -> handleDeactivateStudent());

        reactivateStudentButton = Styles.secondary(new JButton("Reactivate Student"));
        reactivateStudentButton.setEnabled(false);
        reactivateStudentButton.addActionListener(e -> handleReactivateStudent());

        refreshButton = Styles.secondary(new JButton("Refresh"));
        refreshButton.addActionListener(e -> loadStudents());

        actionToolbar.add(addStudentButton);
        actionToolbar.add(editStudentButton);
        actionToolbar.add(deactivateStudentButton);
        actionToolbar.add(reactivateStudentButton);
        actionToolbar.add(refreshButton);

        // Filter bar (wraps on narrow windows so nothing is clipped)
        FilterBar filterPanel = new FilterBar();

        searchField = new JTextField();
        Styles.input(searchField, 200);
        searchField.putClientProperty("JTextField.placeholderText", "Search name or roll number...");
        searchField.addActionListener(e -> loadStudents());

        JButton searchButton = Styles.primary(new JButton("Search"));
        searchButton.addActionListener(e -> loadStudents());

        branchFilter = new JComboBox<>(new String[]{"All Branches", "CSE", "ECE", "ME", "CIVIL"});
        Styles.input(branchFilter, 130);
        branchFilter.addActionListener(e -> loadStudents());

        yearFilter = new JComboBox<>(new String[]{"All Years", "1", "2", "3", "4", "5", "6"});
        Styles.input(yearFilter, 105);
        yearFilter.addActionListener(e -> loadStudents());

        statusFilter = new JComboBox<>(new String[]{"All Students", "Active Only", "Inactive Only"});
        Styles.input(statusFilter, 140);
        statusFilter.addActionListener(e -> loadStudents());

        JButton resetButton = Styles.secondary(new JButton("Reset Filters"));
        resetButton.addActionListener(e -> resetFilters());

        filterPanel.addLabeled("SEARCH", searchField);
        filterPanel.add(searchButton);
        filterPanel.addLabeled("BRANCH", branchFilter);
        filterPanel.addLabeled("YEAR", yearFilter);
        filterPanel.addLabeled("STATUS", statusFilter);
        filterPanel.add(resetButton);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(header);
        topPanel.add(actionToolbar);
        topPanel.add(Box.createVerticalStrut(Theme.SPACE_MD));
        topPanel.add(filterPanel);
        topPanel.add(Box.createVerticalStrut(Theme.SPACE_MD));

        add(topPanel, BorderLayout.NORTH);

        // ------------------ Center: Table ------------------
        String[] columnNames = {"Roll Number", "Name", "Branch", "Year", "Email", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(studentTable);
        studentTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadge.Renderer());
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });

        JScrollPane scrollPane = Styles.scroll(studentTable);
        add(scrollPane, BorderLayout.CENTER);

        // ------------------ Bottom: Status bar ------------------
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(6, 4, 4, 4));

        countLabel = new JLabel("No students loaded.");
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(Color.DARK_GRAY);

        bottomPanel.add(countLabel, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Queries and refreshes the student roster according to active filter and search controls.
     */
    public void loadStudents() {
        String query = searchField.getText() != null ? searchField.getText().trim() : "";
        String branch = (String) branchFilter.getSelectedItem();
        if ("All Branches".equals(branch)) {
            branch = null;
        }

        String yearStr = (String) yearFilter.getSelectedItem();
        Integer year = null;
        if (yearStr != null && !"All Years".equals(yearStr)) {
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException ignored) {
            }
        }

        String status = (String) statusFilter.getSelectedItem();

        try {
            List<Student> students;
            if (query.isEmpty() && branch == null && year == null) {
                students = studentDAO.findAll();
            } else {
                students = studentDAO.search(query, branch, year);
            }

            // Apply status filter
            if ("Active Only".equals(status)) {
                students = students.stream().filter(Student::isActive).toList();
            } else if ("Inactive Only".equals(status)) {
                students = students.stream().filter(s -> !s.isActive()).toList();
            }

            displayedStudents.clear();
            displayedStudents.addAll(students);

            tableModel.setRowCount(0);
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                        s.getRollNumber(),
                        s.getName(),
                        s.getBranch(),
                        s.getYear(),
                        s.getEmail() != null ? s.getEmail() : "",
                        s.getStatus()
                });
            }

            updateActionButtons();

            if (students.isEmpty()) {
                countLabel.setText("No students found.");
            } else {
                countLabel.setText("Showing " + students.size() + " student" + (students.size() == 1 ? "" : "s") + ".");
            }
        } catch (SQLException e) {
            showErrorMessage("Database error loading students: " + e.getMessage());
        }
    }

    private void resetFilters() {
        searchField.setText("");
        branchFilter.setSelectedIndex(0);
        yearFilter.setSelectedIndex(0);
        statusFilter.setSelectedIndex(0);
        loadStudents();
    }

    private void updateActionButtons() {
        int selectedRow = studentTable.getSelectedRow();
        int modelRow = selectedRow >= 0 ? studentTable.convertRowIndexToModel(selectedRow) : -1;
        boolean hasSelection = modelRow >= 0 && modelRow < displayedStudents.size();

        editStudentButton.setEnabled(hasSelection);
        if (hasSelection) {
            Student selected = displayedStudents.get(modelRow);
            deactivateStudentButton.setEnabled(selected.isActive());
            reactivateStudentButton.setEnabled(!selected.isActive());
        } else {
            deactivateStudentButton.setEnabled(false);
            reactivateStudentButton.setEnabled(false);
        }
    }

    Student getSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        int modelRow = selectedRow >= 0 ? studentTable.convertRowIndexToModel(selectedRow) : -1;
        if (modelRow >= 0 && modelRow < displayedStudents.size()) {
            return displayedStudents.get(modelRow);
        }
        return null;
    }

    void showAddStudentDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JTextField rollNumberField = new JTextField(15);
        JTextField nameField = new JTextField(15);
        JComboBox<String> branchCombo = new JComboBox<>(new String[]{"CSE", "ECE", "ME", "CIVIL"});
        JComboBox<Integer> yearCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        JTextField emailField = new JTextField(15);

        JPanel form = Styles.formPanel();
        Styles.addFormRow(form, 0, "Username:", usernameField);
        Styles.addFormRow(form, 1, "Password:", passwordField);
        Styles.addFormRow(form, 2, "Roll Number:", rollNumberField);
        Styles.addFormRow(form, 3, "Full Name:", nameField);
        Styles.addFormRow(form, 4, "Branch:", branchCombo);
        Styles.addFormRow(form, 5, "Academic Year:", yearCombo);
        Styles.addFormRow(form, 6, "Email Address:", emailField);

        int result = showConfirmDialog(form, "Add New Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            char[] passwordChars = passwordField.getPassword();
            String password = passwordChars != null ? new String(passwordChars) : "";
            String rollNumber = rollNumberField.getText().trim();
            String name = nameField.getText().trim();
            String branch = (String) branchCombo.getSelectedItem();
            Integer year = (Integer) yearCombo.getSelectedItem();
            String email = emailField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || rollNumber.isEmpty() || name.isEmpty() || branch == null || year == null) {
                showErrorMessage("Please fill in all required fields.");
                return;
            }

            try {
                studentDAO.createStudent(username, password, rollNumber, name, branch, year, email.isEmpty() ? null : email);
                showSuccessMessage("Student added successfully.");
                loadStudents();
            } catch (SQLException e) {
                showErrorMessage("Failed to create student: " + e.getMessage());
            }
        }
    }

    void showEditStudentDialog() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            showInfoMessage("Please select a student to edit.");
            return;
        }

        JTextField rollNumberField = new JTextField(selected.getRollNumber(), 15);
        rollNumberField.setEnabled(false); // Roll number is identity, cannot be changed
        JTextField nameField = new JTextField(selected.getName(), 15);
        JComboBox<String> branchCombo = new JComboBox<>(new String[]{"CSE", "ECE", "ME", "CIVIL"});
        branchCombo.setSelectedItem(selected.getBranch());
        JComboBox<Integer> yearCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        yearCombo.setSelectedItem(selected.getYear());
        JTextField emailField = new JTextField(selected.getEmail() != null ? selected.getEmail() : "", 15);

        JPanel form = Styles.formPanel();
        Styles.addFormRow(form, 0, "Roll Number (ID):", rollNumberField);
        Styles.addFormRow(form, 1, "Full Name:", nameField);
        Styles.addFormRow(form, 2, "Branch:", branchCombo);
        Styles.addFormRow(form, 3, "Academic Year:", yearCombo);
        Styles.addFormRow(form, 4, "Email Address:", emailField);

        int result = showConfirmDialog(form, "Edit Student - " + selected.getRollNumber(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String branch = (String) branchCombo.getSelectedItem();
            Integer year = (Integer) yearCombo.getSelectedItem();
            String email = emailField.getText().trim();

            if (name.isEmpty() || branch == null || year == null) {
                showErrorMessage("Name, branch, and academic year are required.");
                return;
            }

            try {
                boolean updated = studentDAO.update(selected.getStudentId(), name, branch, year, email.isEmpty() ? null : email);
                if (updated) {
                    showSuccessMessage("Student details updated successfully.");
                    loadStudents();
                } else {
                    showErrorMessage("Could not update student details.");
                }
            } catch (SQLException e) {
                showErrorMessage("Database error updating student: " + e.getMessage());
            }
        }
    }

    void handleDeactivateStudent() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            showInfoMessage("Please select a student to deactivate.");
            return;
        }

        if (!selected.isActive()) {
            showInfoMessage("This student is already inactive.");
            return;
        }

        int confirm = showConfirmDialog(
                "Are you sure you want to deactivate student " + selected.getName() + " (" + selected.getRollNumber() + ")?\n" +
                        "This will preserve student records but disable their login access.",
                "Confirm Student Deactivation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deactivated = studentDAO.deactivate(selected.getStudentId());
                if (deactivated) {
                    showSuccessMessage("Student deactivated successfully.");
                    loadStudents();
                } else {
                    showErrorMessage("Failed to deactivate student.");
                }
            } catch (SQLException e) {
                showErrorMessage("Database error during deactivation: " + e.getMessage());
            }
        }
    }

    void handleReactivateStudent() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            showInfoMessage("Please select a student to reactivate.");
            return;
        }

        if (selected.isActive()) {
            showInfoMessage("This student is already active.");
            return;
        }

        int confirm = showConfirmDialog(
                "Are you sure you want to reactivate student " + selected.getName() + " (" + selected.getRollNumber() + ")?\n" +
                        "This will restore student records and enable their login access.",
                "Confirm Student Reactivation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean reactivated = studentDAO.activate(selected.getStudentId());
                if (reactivated) {
                    showSuccessMessage("Student reactivated successfully.");
                    loadStudents();
                } else {
                    showErrorMessage("Failed to reactivate student.");
                }
            } catch (SQLException e) {
                showErrorMessage("Database error during reactivation: " + e.getMessage());
            }
        }
    }

    protected int showConfirmDialog(Object message, String title, int optionType, int messageType) {
        return JOptionPane.showConfirmDialog(this, message, title, optionType, messageType);
    }

    protected void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void showSuccessMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // Accessors for testing
    JTable getStudentTable() {
        return studentTable;
    }

    DefaultTableModel getTableModel() {
        return tableModel;
    }

    JTextField getSearchField() {
        return searchField;
    }

    JComboBox<String> getBranchFilter() {
        return branchFilter;
    }

    JComboBox<String> getYearFilter() {
        return yearFilter;
    }

    JComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    JButton getAddStudentButton() {
        return addStudentButton;
    }

    JButton getEditStudentButton() {
        return editStudentButton;
    }

    JButton getDeactivateStudentButton() {
        return deactivateStudentButton;
    }

    JButton getReactivateStudentButton() {
        return reactivateStudentButton;
    }

    JLabel getCountLabel() {
        return countLabel;
    }

    List<Student> getDisplayedStudents() {
        return displayedStudents;
    }
}
