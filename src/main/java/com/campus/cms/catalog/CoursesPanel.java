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
 * Administrator panel for maintaining the course catalogue and faculty assignments.
 * Course rules and persistence are delegated to CourseDAO; faculty choices come from FacultyDAO.
 */
public class CoursesPanel extends JPanel {

    private final CourseDAO courseDAO;
    private final FacultyDAO facultyDAO;
    private final List<Course> displayedCourses = new ArrayList<>();

    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    private JButton addButton;
    private JButton editButton;
    private JButton archiveButton;
    private JButton activateButton;
    private JButton refreshButton;

    public CoursesPanel() {
        this(new CourseDAO(), new FacultyDAO());
    }

    public CoursesPanel(CourseDAO courseDAO) {
        this(courseDAO, new FacultyDAO());
    }

    public CoursesPanel(CourseDAO courseDAO, FacultyDAO facultyDAO) {
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO");
        this.facultyDAO = Objects.requireNonNull(facultyDAO, "facultyDAO");
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, Theme.SPACE_MD));
        setBackground(Theme.BACKGROUND);
        setBorder(Styles.pagePadding());

        JPanel top = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        top.setOpaque(false);

        PageHeader header = new PageHeader("Course Catalogue",
                "Manage course offerings, capacity, semester schedules, and faculty assignments.");
        top.add(header, BorderLayout.NORTH);

        FilterBar filterBar = new FilterBar();

        addButton = Styles.primary(new JButton("Add Course"));
        editButton = Styles.secondary(new JButton("Edit Course"));
        archiveButton = Styles.destructive(new JButton("Archive Course"));
        activateButton = Styles.secondary(new JButton("Reactivate Course"));
        refreshButton = Styles.secondary(new JButton("Refresh"));
        editButton.setEnabled(false);
        archiveButton.setEnabled(false);
        activateButton.setEnabled(false);

        addButton.addActionListener(e -> showAddCourseDialog());
        editButton.addActionListener(e -> showEditCourseDialog());
        archiveButton.addActionListener(e -> changeSelectedStatus(false));
        activateButton.addActionListener(e -> changeSelectedStatus(true));
        refreshButton.addActionListener(e -> loadCourses());

        filterBar.add(addButton);
        filterBar.add(editButton);
        filterBar.add(archiveButton);
        filterBar.add(activateButton);
        filterBar.add(refreshButton);

        searchField = new JTextField();
        Styles.input(searchField, 220);
        searchField.putClientProperty("JTextField.placeholderText", "Search code, title, semester...");
        searchField.addActionListener(e -> loadCourses());

        JButton searchButton = Styles.secondary(new JButton("Search"));
        searchButton.addActionListener(e -> loadCourses());

        statusFilter = new JComboBox<>(new String[]{"All Courses", "Active Only", "Archived Only"});
        Styles.input(statusFilter, 130);
        statusFilter.addActionListener(e -> loadCourses());

        JButton resetButton = Styles.secondary(new JButton("Reset"));
        resetButton.addActionListener(e -> {
            searchField.setText("");
            statusFilter.setSelectedIndex(0);
            loadCourses();
        });

        filterBar.addLabeled("SEARCH", searchField);
        filterBar.add(searchButton);
        filterBar.addLabeled("STATUS", statusFilter);
        filterBar.add(resetButton);

        top.add(filterBar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        String[] columns = {"ID", "Code", "Course Title", "Credits", "Capacity", "Semester",
                "Slot", "Assigned Faculty", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        courseTable = new JTable(tableModel);
        courseTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(courseTable);
        courseTable.getColumnModel().getColumn(8).setCellRenderer(new StatusBadge.Renderer());
        courseTable.getTableHeader().setReorderingAllowed(false);
        courseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActionButtons();
        });
        add(Styles.scroll(courseTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(Theme.SPACE_SM, Theme.SPACE_XS, Theme.SPACE_XS, Theme.SPACE_XS));
        countLabel = new JLabel("No courses loaded.");
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

    /** Reloads the catalogue according to the current search and status filters. */
    public void loadCourses() {
        String status = switch ((String) statusFilter.getSelectedItem()) {
            case "Active Only" -> Course.ACTIVE;
            case "Archived Only" -> Course.ARCHIVED;
            default -> "ALL";
        };
        try {
            List<Course> rows = courseDAO.search(searchField.getText().trim(), status, null);
            displayedCourses.clear();
            displayedCourses.addAll(rows);
            tableModel.setRowCount(0);
            for (Course c : rows) {
                tableModel.addRow(new Object[]{c.getCourseId(), c.getCode(), c.getTitle(), c.getCredits(),
                        c.getCapacity(), c.getSemester(), c.getSlot(),
                        c.getFacultyName() == null ? "Unassigned" : c.getFacultyName(), c.getStatus()});
            }
            updateActionButtons();
            countLabel.setText(rows.isEmpty() ? "No courses found."
                    : "Showing " + rows.size() + " course" + (rows.size() == 1 ? "." : "s."));
        } catch (SQLException | RuntimeException ex) {
            showError("Unable to load courses: " + ex.getMessage());
        }
    }

    private void updateActionButtons() {
        Course selected = getSelectedCourse();
        editButton.setEnabled(selected != null);
        archiveButton.setEnabled(selected != null && selected.isActive());
        activateButton.setEnabled(selected != null && !selected.isActive());
    }

    Course getSelectedCourse() {
        int viewRow = courseTable.getSelectedRow();
        if (viewRow < 0) return null;
        int row = courseTable.convertRowIndexToModel(viewRow);
        return row >= 0 && row < displayedCourses.size() ? displayedCourses.get(row) : null;
    }

    void showAddCourseDialog() {
        CourseFields fields = new CourseFields(null);
        if (showCourseForm(fields, "Add Course") != JOptionPane.OK_OPTION) return;
        try {
            courseDAO.createCourse(fields.code.getText().trim(), fields.title.getText().trim(),
                    fields.integer(fields.credits, "Credits"), fields.integer(fields.capacity, "Capacity"),
                    fields.semester.getText().trim(), fields.slot.getText().trim(), fields.facultyId());
            showInfo("Course created successfully.");
            loadCourses();
        } catch (SQLException | IllegalArgumentException ex) {
            showError("Could not create course: " + ex.getMessage());
        }
    }

    void showEditCourseDialog() {
        Course selected = getSelectedCourse();
        if (selected == null) {
            showInfo("Select a course first.");
            return;
        }
        CourseFields fields = new CourseFields(selected);
        if (showCourseForm(fields, "Edit Course - " + selected.getCode()) != JOptionPane.OK_OPTION) return;
        try {
            boolean updated = courseDAO.update(selected.getCourseId(), fields.code.getText().trim(),
                    fields.title.getText().trim(), fields.integer(fields.credits, "Credits"),
                    fields.integer(fields.capacity, "Capacity"), fields.semester.getText().trim(),
                    fields.slot.getText().trim(), fields.facultyId());
            if (updated) showInfo("Course details updated successfully.");
            else showError("Course record could not be updated.");
            loadCourses();
        } catch (SQLException | IllegalArgumentException ex) {
            showError("Could not update course: " + ex.getMessage());
        }
    }

    private int showCourseForm(CourseFields fields, String title) {
        return JOptionPane.showConfirmDialog(this, fields.panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    private void changeSelectedStatus(boolean activate) {
        Course selected = getSelectedCourse();
        if (selected == null) {
            showInfo("Select a course first.");
            return;
        }
        String action = activate ? "reactivate" : "archive";
        if (JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + action + " " + selected.getCode() + " - " + selected.getTitle() + "?",
                "Confirm Course " + (activate ? "Reactivation" : "Archiving"),
                JOptionPane.YES_NO_OPTION, activate ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        try {
            boolean changed = activate ? courseDAO.activate(selected.getCourseId())
                    : courseDAO.archive(selected.getCourseId());
            if (changed) showInfo("Course " + (activate ? "reactivated" : "archived") + " successfully.");
            else showError("Course record was not found.");
            loadCourses();
        } catch (SQLException ex) {
            showError("Could not " + action + " course: " + ex.getMessage());
        }
    }

    private final class CourseFields {
        final JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        final JTextField code = new JTextField(18);
        final JTextField title = new JTextField(18);
        final JTextField credits = new JTextField(18);
        final JTextField capacity = new JTextField(18);
        final JTextField semester = new JTextField(18);
        final JTextField slot = new JTextField(18);
        final JComboBox<FacultyChoice> faculty = new JComboBox<>();

        CourseFields(Course course) {
            if (course != null) {
                code.setText(course.getCode());
                title.setText(course.getTitle());
                credits.setText(String.valueOf(course.getCredits()));
                capacity.setText(String.valueOf(course.getCapacity()));
                semester.setText(course.getSemester());
                slot.setText(course.getSlot());
            } else {
                credits.setText("3");
                capacity.setText("60");
            }

            faculty.addItem(new FacultyChoice(null, "Unassigned"));
            try {
                for (Faculty f : facultyDAO.findAll()) {
                    if (f.isActive()) faculty.addItem(new FacultyChoice(f.getFacultyId(), f.getName()));
                }
            } catch (SQLException ex) {
                showError("Could not load faculty choices: " + ex.getMessage());
            }
            if (course != null && course.getFacultyId() != null) {
                for (int i = 0; i < faculty.getItemCount(); i++) {
                    FacultyChoice choice = faculty.getItemAt(i);
                    if (Objects.equals(choice.id, course.getFacultyId())) {
                        faculty.setSelectedIndex(i);
                        break;
                    }
                }
            }

            addField("Course code *:", code);
            addField("Course title *:", title);
            addField("Credits *:", credits);
            addField("Capacity *:", capacity);
            addField("Semester *:", semester);
            addField("Slot *:", slot);
            addField("Assigned faculty:", faculty);
            panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        }

        private void addField(String label, Component field) {
            panel.add(new JLabel(label));
            panel.add(field);
        }

        int integer(JTextField field, String label) {
            try {
                return Integer.parseInt(field.getText().trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(label + " must be a whole number.");
            }
        }

        Integer facultyId() {
            FacultyChoice choice = (FacultyChoice) faculty.getSelectedItem();
            return choice == null ? null : choice.id;
        }
    }

    private static final class FacultyChoice {
        final Integer id;
        final String name;
        FacultyChoice(Integer id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }

    private static String nullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Course Catalogue", JOptionPane.ERROR_MESSAGE);
    }
    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Course Catalogue", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Convenience refresh hook for dashboard navigation. */
    public void refresh() {
        loadCourses();
    }

    // Package-visible accessors are useful for Swing tests.
    JTable getCourseTable() { return courseTable; }
    DefaultTableModel getTableModel() { return tableModel; }
    JTextField getSearchField() { return searchField; }
    JComboBox<String> getStatusFilter() { return statusFilter; }
    JButton getAddButton() { return addButton; }
    JButton getEditButton() { return editButton; }
    JButton getArchiveButton() { return archiveButton; }
    JButton getActivateButton() { return activateButton; }
    JLabel getCountLabel() { return countLabel; }
    List<Course> getDisplayedCourses() { return displayedCourses; }
}
