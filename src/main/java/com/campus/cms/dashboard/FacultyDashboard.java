package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.auth.User;
import com.campus.cms.catalog.Course;
import com.campus.cms.catalog.CourseDAO;
import com.campus.cms.catalog.Faculty;
import com.campus.cms.catalog.FacultyDAO;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.core.ui.FilterBar;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.SidebarNav;
import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;
import com.campus.cms.grading.Assignment;
import com.campus.cms.grading.AssignmentDAO;
import com.campus.cms.grading.GradesPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Main dashboard for authenticated FACULTY users.
 * Faculty can view their assigned courses, open the assignment/grade workspace
 * for a course, refresh their information, and manage their login profile.
 */
public class FacultyDashboard extends JFrame {

    public static final String CARD_OVERVIEW = "OVERVIEW";
    public static final String CARD_COURSES = "COURSES";
    public static final String CARD_GRADES = "GRADES";
    public static final String CARD_PROFILE = "PROFILE";

    private final AuthService authService;
    private final Session session;
    private final FacultyDAO facultyDAO;
    private final CourseDAO courseDAO;
    private final AssignmentDAO assignmentDAO;

    private Faculty currentFaculty;
    private String facultyProfileIssue;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTable coursesTable;
    private DefaultTableModel coursesTableModel;
    private JComboBox<Course> gradeCourseSelector;
    private JPanel gradesContainer;
    private JComponent currentGradesPanel;
    private ProfilePanel profilePanel;
    private boolean rebuildingCourseSelector;

    private DashboardCard assignedCoursesCard;
    private DashboardCard activeCoursesCard;
    private DashboardCard assignmentsCard;

    private JButton overviewNavBtn;
    private JButton coursesNavBtn;
    private JButton gradesNavBtn;
    private JButton profileNavBtn;
    private JButton logoutNavBtn;
    private SidebarNav sidebar;

    public FacultyDashboard() {
        this(new AuthService(), Session.getInstance(), new FacultyDAO(),
                new CourseDAO(), new AssignmentDAO());
    }

    public FacultyDashboard(AuthService authService, Session session) {
        this(authService, session, new FacultyDAO(), new CourseDAO(), new AssignmentDAO());
    }

    public FacultyDashboard(AuthService authService, Session session,
                            FacultyDAO facultyDAO, CourseDAO courseDAO,
                            AssignmentDAO assignmentDAO) {
        this.session = Objects.requireNonNull(session, "session");
        if (!session.hasRole(Role.FACULTY)) {
            throw new SecurityException("Unauthorized access: FacultyDashboard requires an active FACULTY session.");
        }
        this.authService = Objects.requireNonNull(authService, "authService");
        this.facultyDAO = Objects.requireNonNull(facultyDAO, "facultyDAO");
        this.courseDAO = Objects.requireNonNull(courseDAO, "courseDAO");
        this.assignmentDAO = Objects.requireNonNull(assignmentDAO, "assignmentDAO");

        loadFacultyProfile();
        initUI();
        refreshDashboard();
    }

    private void loadFacultyProfile() {
        User user = session.getCurrentUser();
        if (user == null) {
            facultyProfileIssue = "No authenticated faculty user was found in the current session.";
            return;
        }
        try {
            currentFaculty = facultyDAO.findByUserId(user.getUserId()).orElse(null);
            if (currentFaculty == null) {
                facultyProfileIssue = "No faculty profile is linked to login account '" + user.getUsername()
                        + "'. Ask an administrator to create or repair the faculty profile.";
            }
        } catch (SQLException ex) {
            currentFaculty = null;
            facultyProfileIssue = "Could not load your faculty profile: " + ex.getMessage();
            // Keep a visible diagnostic in the UI and retain the stack trace for developers.
            ex.printStackTrace();
        }
    }

    private int getFacultyId() {
        return currentFaculty == null ? -1 : currentFaculty.getFacultyId();
    }

    private void initUI() {
        setTitle("Campus CMS - Faculty Portal");
        setSize(1050, 700);
        setMinimumSize(new Dimension(880, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        String facultyName = currentFaculty != null ? currentFaculty.getName()
                : session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Faculty";
        sidebar = new SidebarNav("Campus CMS", "Faculty Portal", facultyName);

        overviewNavBtn = sidebar.addNav(new JButton("Overview"));
        coursesNavBtn = sidebar.addNav(new JButton("My Courses"));
        gradesNavBtn = sidebar.addNav(new JButton("Assignments & Grades"));
        profileNavBtn = sidebar.addNav(new JButton("My Profile"));
        logoutNavBtn = sidebar.setFooter(new JButton("Logout"));

        overviewNavBtn.addActionListener(e -> {
            showCard(CARD_OVERVIEW);
            refreshDashboard();
        });
        coursesNavBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            refreshCourses();
        });
        gradesNavBtn.addActionListener(e -> {
            showCard(CARD_GRADES);
            refreshGradesPanel();
        });
        profileNavBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });
        logoutNavBtn.addActionListener(e -> handleLogout());

        add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BACKGROUND);
        contentPanel.add(createOverviewPanel(), CARD_OVERVIEW);
        contentPanel.add(createCoursesPanel(), CARD_COURSES);
        contentPanel.add(createGradesPanel(), CARD_GRADES);
        profilePanel = new ProfilePanel(authService, session);
        contentPanel.add(profilePanel, CARD_PROFILE);
        add(contentPanel, BorderLayout.CENTER);
        sidebar.setActive(overviewNavBtn);
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(Styles.pagePadding());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        String name = currentFaculty != null ? currentFaculty.getName()
                : session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Faculty";
        String subtitleText = currentFaculty == null
                ? "Your teaching workspace and course overview"
                : currentFaculty.getDepartment() + "  |  " + safe(currentFaculty.getEmail());

        PageHeader header = new PageHeader("Welcome, " + name, subtitleText);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(header);

        if (facultyProfileIssue != null) {
            JLabel warning = new JLabel(facultyProfileIssue);
            warning.setForeground(Theme.DANGER);
            warning.setFont(Theme.SMALL);
            warning.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(Box.createVerticalStrut(Theme.SPACE_SM));
            body.add(warning);
        }

        JPanel metrics = new JPanel(new GridLayout(1, 3, Theme.SPACE_LG, Theme.SPACE_LG));
        metrics.setOpaque(false);
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);

        assignedCoursesCard = new DashboardCard("Assigned Courses", "0", "Courses assigned to you", Theme.INFO);
        activeCoursesCard = new DashboardCard("Active Courses", "0", "Currently active courses", Theme.SUCCESS);
        assignmentsCard = new DashboardCard("Assignments", "0", "Across your assigned courses", Theme.WARNING);
        metrics.add(assignedCoursesCard);
        metrics.add(activeCoursesCard);
        metrics.add(assignmentsCard);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, metrics.getPreferredSize().height));

        body.add(metrics);
        body.add(Box.createVerticalStrut(Theme.SPACE_XL));

        JLabel quickTitle = new JLabel("Quick Access");
        quickTitle.setFont(Theme.HEADING);
        quickTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(quickTitle);
        body.add(Box.createVerticalStrut(Theme.SPACE_SM));

        JPanel quick = Styles.actionBar();

        JButton coursesButton = Styles.primary(new JButton("View My Courses \u2192"));
        coursesButton.addActionListener(e -> {
            showCard(CARD_COURSES);
            refreshCourses();
        });
        JButton gradesButton = Styles.secondary(new JButton("Manage Assignments & Grades \u2192"));
        gradesButton.addActionListener(e -> {
            showCard(CARD_GRADES);
            refreshGradesPanel();
        });
        quick.add(coursesButton);
        quick.add(gradesButton);
        body.add(quick);

        panel.add(body, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(Styles.pagePadding());

        PageHeader header = new PageHeader("My Courses", "View all courses assigned to you this semester.");
        panel.add(header, BorderLayout.NORTH);

        String[] columns = {"Course ID", "Code", "Course Title", "Credits", "Semester", "Slot", "Status"};
        coursesTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        coursesTable = new JTable(coursesTableModel);
        coursesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(coursesTable);
        coursesTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadge.Renderer());
        panel.add(Styles.scroll(coursesTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        actions.setOpaque(false);
        JButton refresh = Styles.secondary(new JButton("Refresh"));
        refresh.addActionListener(e -> refreshCourses());
        JButton openGrades = Styles.primary(new JButton("Open Assignments & Grades"));
        openGrades.addActionListener(e -> openSelectedCourseGrades());
        actions.add(refresh);
        actions.add(openGrades);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(Styles.pagePadding());

        PageHeader header = new PageHeader("Assignments & Grades",
                "Review assignments, enter marks, and publish grades for your students.");

        FilterBar top = new FilterBar();
        gradeCourseSelector = new JComboBox<>();
        Styles.input(gradeCourseSelector, 320);
        gradeCourseSelector.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Course course) setText(course.getCode() + " - " + course.getTitle());
                return this;
            }
        });
        gradeCourseSelector.addActionListener(e -> {
            if (!rebuildingCourseSelector) loadGradesForSelectedCourse();
        });
        top.addLabeled("COURSE", gradeCourseSelector);

        JButton refresh = Styles.secondary(new JButton("Refresh"));
        refresh.addActionListener(e -> refreshGradesPanel());
        top.add(refresh);

        JPanel north = new JPanel(new BorderLayout(0, Theme.SPACE_MD));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(top, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);

        gradesContainer = new JPanel(new BorderLayout());
        gradesContainer.setOpaque(false);
        JLabel emptyMsg = new JLabel("Select one of your courses to view assignments and manage grades.",
                SwingConstants.CENTER);
        emptyMsg.setFont(Theme.BODY);
        emptyMsg.setForeground(Theme.TEXT_MUTED);
        gradesContainer.add(emptyMsg, BorderLayout.CENTER);
        panel.add(gradesContainer, BorderLayout.CENTER);
        return panel;
    }

    public void refreshDashboard() {
        refreshCourses();
        if (assignedCoursesCard == null) return;
        if (currentFaculty == null) {
            setUnavailableStatistics("Faculty profile unavailable");
            return;
        }
        try {
            List<Course> courses = courseDAO.search(null, "ALL", getFacultyId());
            long active = courses.stream().filter(Course::isActive).count();
            int assignmentCount = 0;
            for (Course course : courses) {
                assignmentCount += assignmentDAO.getAssignmentsByCourse(course.getCourseId()).size();
            }
            assignedCoursesCard.setValue(courses.size());
            activeCoursesCard.setValue(active);
            assignmentsCard.setValue(assignmentCount);
            assignedCoursesCard.setSubtitle("Courses assigned to you");
            activeCoursesCard.setSubtitle("Currently active courses");
            assignmentsCard.setSubtitle("Across your assigned courses");
        } catch (SQLException | RuntimeException ex) {
            setUnavailableStatistics("Could not load dashboard data");
            ex.printStackTrace();
        }
    }

    private void setUnavailableStatistics(String message) {
        assignedCoursesCard.setValue("N/A");
        activeCoursesCard.setValue("N/A");
        assignmentsCard.setValue("N/A");
        assignedCoursesCard.setSubtitle(message);
        activeCoursesCard.setSubtitle(message);
        assignmentsCard.setSubtitle(message);
    }

    public void refreshCourses() {
        if (coursesTableModel == null) return;
        try {
            List<Course> courses = currentFaculty == null
                    ? List.of() : courseDAO.search(null, "ALL", getFacultyId());
            coursesTableModel.setRowCount(0);
            for (Course course : courses) {
                coursesTableModel.addRow(new Object[]{course.getCourseId(), course.getCode(),
                        course.getTitle(), course.getCredits(), course.getSemester(),
                        course.getSlot(), course.getStatus()});
            }
            reloadCourseSelector(courses);
        } catch (SQLException | RuntimeException ex) {
            // A failed refresh must not leave old courses selectable. Keeping stale
            // selections here could expose a course that is no longer assigned.
            coursesTableModel.setRowCount(0);
            reloadCourseSelector(List.of());
            if (gradesContainer != null) {
                gradesContainer.removeAll();
                gradesContainer.add(new JLabel("Unable to load your courses. Please refresh again.",
                        SwingConstants.CENTER), BorderLayout.CENTER);
                gradesContainer.revalidate();
                gradesContainer.repaint();
            }
            currentGradesPanel = null;
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, "Unable to load your courses: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reloadCourseSelector(List<Course> courses) {
        if (gradeCourseSelector == null) return;
        Course previouslySelected = (Course) gradeCourseSelector.getSelectedItem();
        Integer previousCourseId = previouslySelected == null ? null : previouslySelected.getCourseId();

        rebuildingCourseSelector = true;
        try {
            DefaultComboBoxModel<Course> model = new DefaultComboBoxModel<>();
            for (Course course : courses) model.addElement(course);
            gradeCourseSelector.setModel(model);

            // Restore selection only when the same course is still assigned to this
            // faculty member. Course.equals() alone is not used as a stale-object test.
            if (previousCourseId != null) {
                for (Course course : courses) {
                    if (course.getCourseId() == previousCourseId) {
                        gradeCourseSelector.setSelectedItem(course);
                        break;
                    }
                }
            }
            if (gradeCourseSelector.getSelectedItem() == null && !courses.isEmpty()) {
                gradeCourseSelector.setSelectedIndex(0);
            }
        } finally {
            rebuildingCourseSelector = false;
        }
        loadGradesForSelectedCourse();
    }

    public void refreshGradesPanel() {
        refreshCourses();
        loadGradesForSelectedCourse();
    }

    private void loadGradesForSelectedCourse() {
        if (gradeCourseSelector == null || gradesContainer == null) return;
        Course selected = (Course) gradeCourseSelector.getSelectedItem();
        gradesContainer.removeAll();
        if (selected == null || currentFaculty == null) {
            JLabel msg = new JLabel(currentFaculty == null
                    ? "Your faculty profile is unavailable. Contact an administrator."
                    : "No courses are currently assigned to you.", SwingConstants.CENTER);
            msg.setFont(Theme.BODY);
            msg.setForeground(Theme.TEXT_MUTED);
            gradesContainer.add(msg, BorderLayout.CENTER);
            currentGradesPanel = null;
        } else {
            currentGradesPanel = createGradesPanelForCourse(selected.getCourseId());
            gradesContainer.add(currentGradesPanel, BorderLayout.CENTER);
        }
        gradesContainer.revalidate();
        gradesContainer.repaint();
    }

    /** Factory hook that keeps dashboard navigation testable without a live grading database. */
    protected JComponent createGradesPanelForCourse(int courseId) {
        return new GradesPanel(courseId, true, getFacultyId());
    }

    private void openSelectedCourseGrades() {
        int row = coursesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a course first.",
                    "No Course Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int courseId = (int) coursesTableModel.getValueAt(coursesTable.convertRowIndexToModel(row), 0);
        for (int i = 0; i < gradeCourseSelector.getItemCount(); i++) {
            Course course = gradeCourseSelector.getItemAt(i);
            if (course.getCourseId() == courseId) {
                gradeCourseSelector.setSelectedItem(course);
                break;
            }
        }
        showCard(CARD_GRADES);
        loadGradesForSelectedCourse();
    }

    public void showCard(String cardName) {
        cardLayout.show(contentPanel, cardName);
        if (sidebar != null) {
            sidebar.setActive(switch (cardName) {
                case CARD_COURSES -> coursesNavBtn;
                case CARD_GRADES -> gradesNavBtn;
                case CARD_PROFILE -> profileNavBtn;
                default -> overviewNavBtn;
            });
        }
    }

    void handleLogout() {
        logoutNavBtn.setEnabled(false);
        session.logout();
        dispose();
        new LoginFrame(authService).setVisible(true);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "No email on file" : value;
    }

    public Faculty getCurrentFaculty() { return currentFaculty; }
    public String getFacultyProfileIssue() { return facultyProfileIssue; }
    public CardLayout getCardLayout() { return cardLayout; }
    public JPanel getContentPanel() { return contentPanel; }
    public JButton getOverviewNavBtn() { return overviewNavBtn; }
    public JButton getCoursesNavBtn() { return coursesNavBtn; }
    public JButton getGradesNavBtn() { return gradesNavBtn; }
    public JButton getProfileNavBtn() { return profileNavBtn; }
    public JButton getLogoutNavBtn() { return logoutNavBtn; }
    public DashboardCard getAssignedCoursesCard() { return assignedCoursesCard; }
    public DashboardCard getActiveCoursesCard() { return activeCoursesCard; }
    public DashboardCard getAssignmentsCard() { return assignmentsCard; }
    public JTable getCoursesTable() { return coursesTable; }
    public JComboBox<Course> getGradeCourseSelector() { return gradeCourseSelector; }
}
