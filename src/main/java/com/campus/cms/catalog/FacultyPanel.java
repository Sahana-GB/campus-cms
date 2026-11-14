package com.campus.cms.catalog;

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
        setLayout(new BorderLayout(0, Theme.SPACE_MD));
        setBackground(Theme.BACKGROUND);
        setBorder(Styles.pagePadding());

        JPanel top = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        top.setOpaque(false);

        PageHeader header = new PageHeader("Faculty Management",
                "Create, search, and manage faculty members and their accounts.");
        top.add(header, BorderLayout.NORTH);

        FilterBar filterBar = new FilterBar();

        addButton = Styles.primary(new JButton("Add Faculty"));
        editButton = Styles.secondary(new JButton("Edit Faculty"));
        deactivateButton = Styles.destructive(new JButton("Deactivate"));
        reactivateButton = Styles.secondary(new JButton("Reactivate"));
        refreshButton = Styles.secondary(new JButton("Refresh"));

        editButton.setEnabled(false);
        deactivateButton.setEnabled(false);
        reactivateButton.setEnabled(false);

        addButton.addActionListener(e -> showAddFacultyDialog());
        editButton.addActionListener(e -> showEditFacultyDialog());
        deactivateButton.addActionListener(e -> changeSelectedStatus(false));
        reactivateButton.addActionListener(e -> changeSelectedStatus(true));
        refreshButton.addActionListener(e -> loadFaculty());

        filterBar.add(addButton);
        filterBar.add(editButton);
        filterBar.add(deactivateButton);
        filterBar.add(reactivateButton);
        filterBar.add(refreshButton);

        searchField = new JTextField();
        Styles.input(searchField, 200);
        searchField.putClientProperty("JTextField.placeholderText", "Search name, department...");
        searchField.addActionListener(e -> loadFaculty());

        JButton searchButton = Styles.secondary(new JButton("Search"));
        searchButton.addActionListener(e -> loadFaculty());

        statusFilter = new JComboBox<>(new String[]{"All Faculty", "Active Only", "Inactive Only"});
        Styles.input(statusFilter, 130);
        statusFilter.addActionListener(e -> loadFaculty());

        JButton resetButton = Styles.secondary(new JButton("Reset"));
        resetButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadFaculty();
        });

        filterBar.addLabeled("SEARCH", searchField);
        filterBar.add(searchButton);
        filterBar.addLabeled("STATUS", statusFilter);
        filterBar.add(resetButton);

        top.add(filterBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        String[] columns = {"ID", "Name", "Department", "Email", "Username", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        facultyTable = new JTable(tableModel);
        facultyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(facultyTable);
        facultyTable.getColumnModel().getColumn(5).setCellRenderer(new StatusBadge.Renderer());
        facultyTable.getTableHeader().setReorderingAllowed(false);
        facultyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActionButtons();
        });
        add(Styles.scroll(facultyTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(Theme.SPACE_SM, Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS));
        countLabel = new JLabel("No faculty loaded.");
        countLabel.setFont(Theme.SMALL);
        countLabel.setForeground(Theme.TEXT_MUTED);
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
