package com.campus.cms.catalog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Administrator panel for creating, searching, editing and managing faculty records.
 * Faculty login accounts and academic profiles are created together by FacultyDAO.
 */
public class FacultyPanel extends JPanel {

    private final FacultyDAO facultyDAO;
    private final List<Faculty> displayedFaculty = new ArrayList<>();

    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JTable facultyTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    private JButton addButton;
    private JButton editButton;
    private JButton deactivateButton;
    private JButton reactivateButton;
    private JButton refreshButton;

    public FacultyPanel() {
        this(new FacultyDAO());
    }

    public FacultyPanel(FacultyDAO facultyDAO) {
        this.facultyDAO = Objects.requireNonNull(facultyDAO, "facultyDAO");
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Faculty Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.add(title);
        top.add(titleRow);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        addButton = button("Add Faculty", Font.BOLD);
        editButton = button("Edit Faculty", Font.PLAIN);
        deactivateButton = button("Deactivate", Font.PLAIN);
        reactivateButton = button("Reactivate", Font.PLAIN);
        refreshButton = button("Refresh", Font.PLAIN);

        editButton.setEnabled(false);
        deactivateButton.setEnabled(false);
        reactivateButton.setEnabled(false);

        addButton.addActionListener(e -> showAddFacultyDialog());
        editButton.addActionListener(e -> showEditFacultyDialog());
        deactivateButton.addActionListener(e -> changeSelectedStatus(false));
        reactivateButton.addActionListener(e -> changeSelectedStatus(true));
        refreshButton.addActionListener(e -> loadFaculty());

        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deactivateButton);
        toolbar.add(reactivateButton);
        toolbar.add(refreshButton);
        top.add(toolbar);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(225, 230, 235)),
                new EmptyBorder(4, 2, 4, 2)));
        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, department or email...");
        searchField.addActionListener(e -> loadFaculty());
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> loadFaculty());
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadFaculty();
        });
        statusFilter = new JComboBox<>(new String[]{"All Faculty", "Active Only", "Inactive Only"});
        statusFilter.addActionListener(e -> loadFaculty());
        filters.add(new JLabel("Search:"));
        filters.add(searchField);
        filters.add(searchButton);
        filters.add(new JLabel("Status:"));
        filters.add(statusFilter);
        filters.add(resetButton);
        top.add(filters);
        add(top, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Department", "Email", "Username", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        facultyTable = new JTable(tableModel);
        facultyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        facultyTable.setRowHeight(26);
        facultyTable.getTableHeader().setReorderingAllowed(false);
        facultyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActionButtons();
        });
        add(new JScrollPane(facultyTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(6, 4, 4, 4));
        countLabel = new JLabel("No faculty loaded.");
        countLabel.setForeground(Color.DARK_GRAY);
        bottom.add(countLabel, BorderLayout.WEST);
        add(bottom, BorderLayout.SOUTH);
    }

    private JButton button(String text, int style) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", style, 12));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, 30));
        return button;
    }

    /** Reloads faculty using the current search and status filters. */
    public void loadFaculty() {
        String status = switch ((String) statusFilter.getSelectedItem()) {
            case "Active Only" -> "ACTIVE";
            case "Inactive Only" -> "INACTIVE";
            default -> "ALL";
        };
        try {
            List<Faculty> rows = facultyDAO.search(searchField.getText().trim(), status);
            displayedFaculty.clear();
            displayedFaculty.addAll(rows);
            tableModel.setRowCount(0);
            for (Faculty f : rows) {
                tableModel.addRow(new Object[]{f.getFacultyId(), f.getName(), f.getDepartment(),
                        value(f.getEmail()), value(f.getUsername()), f.getStatus()});
            }
            updateActionButtons();
            countLabel.setText(rows.isEmpty() ? "No faculty found."
                    : "Showing " + rows.size() + " faculty member" + (rows.size() == 1 ? "." : "s."));
        } catch (SQLException | RuntimeException ex) {
            showError("Unable to load faculty: " + ex.getMessage());
        }
    }

    private void updateActionButtons() {
        Faculty selected = getSelectedFaculty();
        editButton.setEnabled(selected != null);
        deactivateButton.setEnabled(selected != null && selected.isActive());
        reactivateButton.setEnabled(selected != null && !selected.isActive());
    }

    Faculty getSelectedFaculty() {
        int viewRow = facultyTable.getSelectedRow();
        if (viewRow < 0) return null;
        int row = facultyTable.convertRowIndexToModel(viewRow);
        return row >= 0 && row < displayedFaculty.size() ? displayedFaculty.get(row) : null;
    }

    void showAddFacultyDialog() {
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JTextField name = new JTextField(18);
        JTextField department = new JTextField(18);
        JTextField email = new JTextField(18);

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Username *:")); form.add(username);
        form.add(new JLabel("Temporary password *:")); form.add(password);
        form.add(new JLabel("Full name *:")); form.add(name);
        form.add(new JLabel("Department *:")); form.add(department);
        form.add(new JLabel("Email:")); form.add(email);

        if (JOptionPane.showConfirmDialog(this, form, "Add Faculty",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        char[] chars = password.getPassword();
        String rawPassword = chars == null ? "" : new String(chars);
        try {
            facultyDAO.createFaculty(username.getText().trim(), rawPassword, name.getText().trim(),
                    department.getText().trim(), nullable(email.getText()));
            showInfo("Faculty member and login account created successfully.");
            loadFaculty();
        } catch (SQLException | IllegalArgumentException ex) {
            showError("Could not create faculty member: " + ex.getMessage());
        } finally {
            if (chars != null) java.util.Arrays.fill(chars, '\0');
            password.setText("");
        }
    }

    void showEditFacultyDialog() {
        Faculty selected = getSelectedFaculty();
        if (selected == null) {
            showInfo("Select a faculty member first.");
            return;
        }

        JTextField name = new JTextField(selected.getName(), 18);
        JTextField department = new JTextField(selected.getDepartment(), 18);
        JTextField email = new JTextField(value(selected.getEmail()), 18);
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Full name *:")); form.add(name);
        form.add(new JLabel("Department *:")); form.add(department);
        form.add(new JLabel("Email:")); form.add(email);

        if (JOptionPane.showConfirmDialog(this, form, "Edit Faculty - " + selected.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            if (facultyDAO.update(selected.getFacultyId(), name.getText().trim(),
                    department.getText().trim(), nullable(email.getText()))) {
                showInfo("Faculty details updated successfully.");
            } else {
                showError("Faculty record could not be updated.");
            }
            loadFaculty();
        } catch (SQLException | IllegalArgumentException ex) {
            showError("Could not update faculty details: " + ex.getMessage());
        }
    }

    private void changeSelectedStatus(boolean activate) {
        Faculty selected = getSelectedFaculty();
        if (selected == null) {
            showInfo("Select a faculty member first.");
            return;
        }
        String action = activate ? "reactivate" : "deactivate";
        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + action + " " + selected.getName() + "?\n"
                        + "This also " + (activate ? "restores" : "disables") + " their login access.",
                "Confirm " + (activate ? "Reactivation" : "Deactivation"),
                JOptionPane.YES_NO_OPTION, activate ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        try {
            boolean changed = activate ? facultyDAO.activate(selected.getFacultyId())
                    : facultyDAO.deactivate(selected.getFacultyId());
            if (changed) showInfo("Faculty member " + (activate ? "reactivated" : "deactivated") + ".");
            else showError("Faculty record was not found.");
            loadFaculty();
        } catch (SQLException ex) {
            showError("Could not " + action + " faculty member: " + ex.getMessage());
        }
    }

    private static String nullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
    private static String value(String value) { return value == null ? "" : value; }
    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Faculty Management", JOptionPane.ERROR_MESSAGE);
    }
    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Faculty Management", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Convenience refresh hook for dashboard navigation. */
    public void refresh() {
        loadFaculty();
    }

    // Package-visible accessors are useful for Swing tests.
    JTable getFacultyTable() { return facultyTable; }
    DefaultTableModel getTableModel() { return tableModel; }
    JTextField getSearchField() { return searchField; }
    JComboBox<String> getStatusFilter() { return statusFilter; }
    JButton getAddButton() { return addButton; }
    JButton getEditButton() { return editButton; }
    JButton getDeactivateButton() { return deactivateButton; }
    JButton getReactivateButton() { return reactivateButton; }
    JLabel getCountLabel() { return countLabel; }
    List<Faculty> getDisplayedFaculty() { return displayedFaculty; }
}
