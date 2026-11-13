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
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JTable coursesTable;
    private DefaultTableModel coursesTableModel;
    private JComboBox<Course> gradeCourseSelector;
    private JPanel gradesContainer;
    private GradesPanel currentGradesPanel;
    private ProfilePanel profilePanel;

    private DashboardCard assignedCoursesCard;
    private DashboardCard activeCoursesCard;
    private DashboardCard assignmentsCard;

    private JButton overviewNavBtn;
    private JButton coursesNavBtn;
    private JButton gradesNavBtn;
    private JButton profileNavBtn;
    private JButton logoutNavBtn;

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
        if (user == null) return;
        try {
            currentFaculty = facultyDAO.findByUserId(user.getUserId()).orElse(null);
        } catch (SQLException ex) {
            currentFaculty = null;
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

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 230, 235)),
                new EmptyBorder(20, 16, 20, 16)));

        JLabel brand = new JLabel("Campus CMS");
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel role = new JLabel("Faculty Portal");
        role.setFont(new Font("SansSerif", Font.PLAIN, 12));
        role.setForeground(Color.GRAY);
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(role);
        sidebar.add(Box.createVerticalStrut(24));

        overviewNavBtn = createNavButton("Overview");
        coursesNavBtn = createNavButton("My Courses");
        gradesNavBtn = createNavButton("Assignments & Grades");
        profileNavBtn = createNavButton("My Profile");
        logoutNavBtn = createNavButton("Logout");

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

        sidebar.add(overviewNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(coursesNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(gradesNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(profileNavBtn);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutNavBtn);
        add(sidebar, BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(createOverviewPanel(), CARD_OVERVIEW);
        contentPanel.add(createCoursesPanel(), CARD_COURSES);
        contentPanel.add(createGradesPanel(), CARD_GRADES);
        profilePanel = new ProfilePanel(authService, session);
        contentPanel.add(profilePanel, CARD_PROFILE);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setMaximumSize(new Dimension(190, 36));
        button.setPreferredSize(new Dimension(190, 36));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        return button;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 22));
        panel.setBorder(new EmptyBorder(28, 30, 28, 30));

        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        String name = currentFaculty != null ? currentFaculty.getName()
                : session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Faculty";
        JLabel title = new JLabel("Welcome, " + name);
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel(currentFaculty == null
                ? "Your teaching workspace and course overview"
                : currentFaculty.getDepartment() + "  |  " + safe(currentFaculty.getEmail()));
        subtitle.setForeground(Color.GRAY);
        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(subtitle);
        panel.add(heading, BorderLayout.NORTH);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 16, 16));
        assignedCoursesCard = new DashboardCard("Assigned Courses", "0", "Courses assigned to you");
        activeCoursesCard = new DashboardCard("Active Courses", "0", "Currently active courses");
        assignmentsCard = new DashboardCard("Assignments", "0", "Across your assigned courses");
        metrics.add(assignedCoursesCard);
        metrics.add(activeCoursesCard);
        metrics.add(assignmentsCard);

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        quick.setBorder(BorderFactory.createTitledBorder("Quick Access"));
        JButton coursesButton = new JButton("View My Courses");
        coursesButton.addActionListener(e -> {
            showCard(CARD_COURSES);
            refreshCourses();
        });
        JButton gradesButton = new JButton("Manage Assignments & Grades");
        gradesButton.addActionListener(e -> {
            showCard(CARD_GRADES);
            refreshGradesPanel();
        });
        quick.add(coursesButton);
        quick.add(gradesButton);

        JPanel center = new JPanel(new BorderLayout(0, 22));
        center.add(metrics, BorderLayout.NORTH);
        center.add(quick, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(22, 24, 22, 24));
        JLabel title = new JLabel("My Courses");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        String[] columns = {"Course ID", "Code", "Course Title", "Credits", "Semester", "Slot", "Status"};
        coursesTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        coursesTable = new JTable(coursesTableModel);
        coursesTable.setRowHeight(27);
        coursesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(coursesTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshCourses());
        JButton openGrades = new JButton("Open Assignments & Grades");
        openGrades.addActionListener(e -> openSelectedCourseGrades());
        actions.add(refresh);
        actions.add(openGrades);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel title = new JLabel("Assignments & Grades");
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        top.add(title);
        top.add(Box.createHorizontalStrut(16));
        top.add(new JLabel("Course:"));
        gradeCourseSelector = new JComboBox<>();
        gradeCourseSelector.setPreferredSize(new Dimension(300, 28));
        gradeCourseSelector.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Course course) setText(course.getCode() + " - " + course.getTitle());
                return this;
            }
        });
        gradeCourseSelector.addActionListener(e -> loadGradesForSelectedCourse());
        top.add(gradeCourseSelector);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshGradesPanel());
        top.add(refresh);
        panel.add(top, BorderLayout.NORTH);

        gradesContainer = new JPanel(new BorderLayout());
        gradesContainer.add(new JLabel("Select one of your courses to view assignments and manage grades.",
                SwingConstants.CENTER), BorderLayout.CENTER);
        panel.add(gradesContainer, BorderLayout.CENTER);
        return panel;
    }

    public void refreshDashboard() {
        refreshCourses();
        if (assignedCoursesCard == null) return;
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
        } catch (SQLException ex) {
            assignedCoursesCard.setValue("N/A");
            activeCoursesCard.setValue("N/A");
            assignmentsCard.setValue("N/A");
            assignedCoursesCard.setSubtitle("Could not load dashboard data");
        }
    }

    public void refreshCourses() {
        if (coursesTableModel == null) return;
        coursesTableModel.setRowCount(0);
        try {
            List<Course> courses = courseDAO.search(null, "ALL", getFacultyId());
            for (Course course : courses) {
                coursesTableModel.addRow(new Object[]{course.getCourseId(), course.getCode(),
                        course.getTitle(), course.getCredits(), course.getSemester(),
                        course.getSlot(), course.getStatus()});
            }
            reloadCourseSelector(courses);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load your courses: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reloadCourseSelector(List<Course> courses) {
        if (gradeCourseSelector == null) return;
        Course previouslySelected = (Course) gradeCourseSelector.getSelectedItem();
        gradeCourseSelector.removeAllItems();
        for (Course course : courses) gradeCourseSelector.addItem(course);
        if (previouslySelected != null) gradeCourseSelector.setSelectedItem(previouslySelected);
        if (gradeCourseSelector.getSelectedItem() == null) {
            gradesContainer.removeAll();
            gradesContainer.add(new JLabel("No courses are currently assigned to you.", SwingConstants.CENTER),
                    BorderLayout.CENTER);
            gradesContainer.revalidate();
            gradesContainer.repaint();
        }
    }

    public void refreshGradesPanel() {
        refreshCourses();
        loadGradesForSelectedCourse();
    }

    private void loadGradesForSelectedCourse() {
        if (gradeCourseSelector == null || gradesContainer == null) return;
        Course selected = (Course) gradeCourseSelector.getSelectedItem();
        gradesContainer.removeAll();
        if (selected == null) {
            gradesContainer.add(new JLabel("No assigned course selected.", SwingConstants.CENTER), BorderLayout.CENTER);
            currentGradesPanel = null;
        } else {
            currentGradesPanel = new GradesPanel(selected.getCourseId(), true, getFacultyId());
            gradesContainer.add(currentGradesPanel, BorderLayout.CENTER);
        }
        gradesContainer.revalidate();
        gradesContainer.repaint();
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
