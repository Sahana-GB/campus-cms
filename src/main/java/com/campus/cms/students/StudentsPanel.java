package com.campus.cms.students;

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
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // ------------------ Top Header & Controls Panel ------------------
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 1. Title Row (Separated completely from action buttons)
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel titleLabel = new JLabel("Student Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleRow.add(titleLabel);

        // 2. Dedicated Action Toolbar Row
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionToolbar.setBorder(new EmptyBorder(8, 0, 8, 0));

        addStudentButton = new JButton("Add Student");
        addStudentButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        addStudentButton.setPreferredSize(new Dimension(addStudentButton.getPreferredSize().width, 30));
        addStudentButton.addActionListener(e -> showAddStudentDialog());

        editStudentButton = new JButton("Edit Student");
        editStudentButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        editStudentButton.setPreferredSize(new Dimension(editStudentButton.getPreferredSize().width, 30));
        editStudentButton.setEnabled(false);
        editStudentButton.addActionListener(e -> showEditStudentDialog());

        deactivateStudentButton = new JButton("Deactivate Student");
        deactivateStudentButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        deactivateStudentButton.setPreferredSize(new Dimension(deactivateStudentButton.getPreferredSize().width, 30));
        deactivateStudentButton.setEnabled(false);
        deactivateStudentButton.addActionListener(e -> handleDeactivateStudent());

        reactivateStudentButton = new JButton("Reactivate Student");
        reactivateStudentButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        reactivateStudentButton.setPreferredSize(new Dimension(reactivateStudentButton.getPreferredSize().width, 30));
        reactivateStudentButton.setEnabled(false);
        reactivateStudentButton.addActionListener(e -> handleReactivateStudent());

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        refreshButton.setPreferredSize(new Dimension(refreshButton.getPreferredSize().width, 30));
        refreshButton.addActionListener(e -> loadStudents());

        actionToolbar.add(addStudentButton);
        actionToolbar.add(editStudentButton);
        actionToolbar.add(deactivateStudentButton);
        actionToolbar.add(reactivateStudentButton);
        actionToolbar.add(refreshButton);

        // 3. Dedicated Filter Panel (Separate from actions and title, non-overlapping)
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(225, 230, 235)),
                new EmptyBorder(8, 2, 8, 2)
        ));

        // Sub-Row 3a: Search & Reset
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        searchField = new JTextField(18);
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.putClientProperty("JTextField.placeholderText", "Search name or roll number...");
        searchField.addActionListener(e -> loadStudents());

        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(searchButton.getPreferredSize().width, 28));
        searchButton.addActionListener(e -> loadStudents());

        JButton resetButton = new JButton("Reset Filters");
        resetButton.setPreferredSize(new Dimension(resetButton.getPreferredSize().width, 28));
        resetButton.addActionListener(e -> resetFilters());

        searchRow.add(new JLabel("Search:"));
        searchRow.add(searchField);
        searchRow.add(searchButton);
        searchRow.add(Box.createHorizontalStrut(8));
        searchRow.add(resetButton);

        // Sub-Row 3b: Branch, Year, Status Dropdowns
        JPanel dropdownRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));

        branchFilter = new JComboBox<>(new String[]{"All Branches", "CSE", "ECE", "ME", "CIVIL"});
        branchFilter.setPreferredSize(new Dimension(115, 28));
        branchFilter.addActionListener(e -> loadStudents());

        yearFilter = new JComboBox<>(new String[]{"All Years", "1", "2", "3", "4", "5", "6"});
        yearFilter.setPreferredSize(new Dimension(95, 28));
        yearFilter.addActionListener(e -> loadStudents());

        statusFilter = new JComboBox<>(new String[]{"All Students", "Active Only", "Inactive Only"});
        statusFilter.setPreferredSize(new Dimension(130, 28));
        statusFilter.addActionListener(e -> loadStudents());

        dropdownRow.add(new JLabel("Branch:"));
        dropdownRow.add(branchFilter);
        dropdownRow.add(new JLabel("Year:"));
        dropdownRow.add(yearFilter);
        dropdownRow.add(new JLabel("Status:"));
        dropdownRow.add(statusFilter);

        filterPanel.add(searchRow);
        filterPanel.add(Box.createVerticalStrut(4));
        filterPanel.add(dropdownRow);

        topPanel.add(titleRow);
        topPanel.add(Box.createVerticalStrut(6));
        topPanel.add(actionToolbar);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(filterPanel);

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
        studentTable.setRowHeight(26);
        studentTable.getTableHeader().setReorderingAllowed(false);
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionButtons();
            }
        });

        JScrollPane scrollPane = new JScrollPane(studentTable);
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
        boolean hasSelection = selectedRow >= 0 && selectedRow < displayedStudents.size();

        editStudentButton.setEnabled(hasSelection);
        if (hasSelection) {
            Student selected = displayedStudents.get(selectedRow);
            deactivateStudentButton.setEnabled(selected.isActive());
            reactivateStudentButton.setEnabled(!selected.isActive());
        } else {
            deactivateStudentButton.setEnabled(false);
            reactivateStudentButton.setEnabled(false);
        }
    }

    Student getSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < displayedStudents.size()) {
            return displayedStudents.get(selectedRow);
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

        JPanel form = new JPanel(new GridLayout(7, 2, 6, 8));
        form.add(new JLabel("Username:"));
        form.add(usernameField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);
        form.add(new JLabel("Roll Number:"));
        form.add(rollNumberField);
        form.add(new JLabel("Full Name:"));
        form.add(nameField);
        form.add(new JLabel("Branch:"));
        form.add(branchCombo);
        form.add(new JLabel("Academic Year:"));
        form.add(yearCombo);
        form.add(new JLabel("Email Address:"));
        form.add(emailField);

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

        JPanel form = new JPanel(new GridLayout(5, 2, 6, 8));
        form.add(new JLabel("Roll Number (ID):"));
        form.add(rollNumberField);
        form.add(new JLabel("Full Name:"));
        form.add(nameField);
        form.add(new JLabel("Branch:"));
        form.add(branchCombo);
        form.add(new JLabel("Academic Year:"));
        form.add(yearCombo);
        form.add(new JLabel("Email Address:"));
        form.add(emailField);

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
