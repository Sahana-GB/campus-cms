package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.catalog.CourseDAO;
import com.campus.cms.catalog.CoursesPanel;
import com.campus.cms.catalog.FacultyDAO;
import com.campus.cms.catalog.FacultyPanel;
import com.campus.cms.enrollment.AdminEnrollmentsPanel;
import com.campus.cms.enrollment.EnrollmentService;
import com.campus.cms.students.Student;
import com.campus.cms.students.StudentDAO;
import com.campus.cms.students.StudentsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Main application dashboard for users with the ADMIN role.
 * Features sidebar navigation across Overview, Student Management, Enrollment Management,
 * and Profile, with real student KPIs and clean logout handling.
 */
public class AdminDashboard extends JFrame {

    public static final String CARD_OVERVIEW = "OVERVIEW";
    public static final String CARD_STUDENTS = "STUDENTS";
    public static final String CARD_ENROLLMENTS = "ENROLLMENTS";
    public static final String CARD_FACULTY = "FACULTY";
    public static final String CARD_COURSES = "COURSES";
    public static final String CARD_PROFILE = "PROFILE";

    private final StudentDAO studentDAO;
    private final AuthService authService;
    private final Session session;
    private final EnrollmentService enrollmentService;
    private final FacultyDAO facultyDAO;
    private final CourseDAO courseDAO;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private StudentsPanel studentsPanel;
    private AdminEnrollmentsPanel enrollmentsPanel;
    private FacultyPanel facultyPanel;
    private CoursesPanel coursesPanel;
    private ProfilePanel profilePanel;

    private DashboardCard totalStudentsCard;
    private DashboardCard activeStudentsCard;
    private DashboardCard inactiveStudentsCard;

    private JButton overviewNavBtn;
    private JButton studentsNavBtn;
    private JButton enrollmentsNavBtn;
    private JButton facultyNavBtn;
    private JButton coursesNavBtn;
    private JButton profileNavBtn;
    private JButton logoutNavBtn;

    public AdminDashboard() {
        this(new StudentDAO(), new AuthService(), Session.getInstance(), new EnrollmentService(), new FacultyDAO(), new CourseDAO());
    }

    public AdminDashboard(StudentDAO studentDAO, AuthService authService, Session session) {
        this(studentDAO, authService, session, new EnrollmentService(), new FacultyDAO(), new CourseDAO());
    }

    public AdminDashboard(StudentDAO studentDAO, AuthService authService, Session session, EnrollmentService enrollmentService) {
        this(studentDAO, authService, session, enrollmentService, new FacultyDAO(), new CourseDAO());
    }

    public AdminDashboard(StudentDAO studentDAO, AuthService authService, Session session,
                          EnrollmentService enrollmentService, FacultyDAO facultyDAO, CourseDAO courseDAO) {
        this.session = Objects.requireNonNull(session);
        if (!this.session.hasRole(Role.ADMIN)) {
            throw new SecurityException("Unauthorized access: AdminDashboard requires an active ADMIN session.");
        }
        this.studentDAO = Objects.requireNonNull(studentDAO);
        this.authService = Objects.requireNonNull(authService);
        this.enrollmentService = Objects.requireNonNull(enrollmentService);
        this.facultyDAO = Objects.requireNonNull(facultyDAO);
        this.courseDAO = Objects.requireNonNull(courseDAO);

        initUI();
        refreshOverviewMetrics();
    }

    private void initUI() {
        setTitle("Campus CMS - Administrator Dashboard");
        setSize(1020, 700);
        setMinimumSize(new Dimension(860, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ------------------ 1. Sidebar Navigation (WEST) ------------------
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(225, 230, 235)),
                new EmptyBorder(20, 16, 20, 16)
        ));

        // Brand Banner
        JLabel brandTitle = new JLabel("Campus CMS");
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandRole = new JLabel("Administrator");
        brandRole.setFont(new Font("SansSerif", Font.PLAIN, 12));
        brandRole.setForeground(Color.GRAY);
        brandRole.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brandTitle);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(brandRole);
        sidebar.add(Box.createVerticalStrut(24));

        // Navigation Buttons
        overviewNavBtn = createNavButton("Overview");
        studentsNavBtn = createNavButton("Student Management");
        enrollmentsNavBtn = createNavButton("Enrollment Management");
        facultyNavBtn = createNavButton("Faculty Management");
        coursesNavBtn = createNavButton("Course Management");
        profileNavBtn = createNavButton("My Profile");
        logoutNavBtn = createNavButton("Logout");

        overviewNavBtn.addActionListener(e -> {
            showCard(CARD_OVERVIEW);
            refreshOverviewMetrics();
        });
        studentsNavBtn.addActionListener(e -> {
            showCard(CARD_STUDENTS);
            studentsPanel.loadStudents();
        });
        enrollmentsNavBtn.addActionListener(e -> {
            showCard(CARD_ENROLLMENTS);
            if (enrollmentsPanel != null) {
                enrollmentsPanel.refresh();
            }
        });
        facultyNavBtn.addActionListener(e -> {
            showCard(CARD_FACULTY);
            facultyPanel.refresh();
        });
        coursesNavBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            coursesPanel.refresh();
        });
        profileNavBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });
        logoutNavBtn.addActionListener(e -> handleLogout());

        sidebar.add(overviewNavBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(studentsNavBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(enrollmentsNavBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(facultyNavBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(coursesNavBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(profileNavBtn);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutNavBtn);

        add(sidebar, BorderLayout.WEST);

        // ------------------ 2. Content Area (CENTER with CardLayout) ------------------
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Card 1: Overview
        JPanel overviewCard = createOverviewPanel();
        contentPanel.add(overviewCard, CARD_OVERVIEW);

        // Card 2: Student Management
        studentsPanel = new StudentsPanel(studentDAO);
        contentPanel.add(studentsPanel, CARD_STUDENTS);

        // Card 3: Enrollment Management
        enrollmentsPanel = new AdminEnrollmentsPanel(enrollmentService);
        contentPanel.add(enrollmentsPanel, CARD_ENROLLMENTS);

        // Card 4: Faculty Management
        facultyPanel = new FacultyPanel(facultyDAO);
        contentPanel.add(facultyPanel, CARD_FACULTY);

        // Card 5: Course Management
        coursesPanel = new CoursesPanel(courseDAO, facultyDAO);
        contentPanel.add(coursesPanel, CARD_COURSES);

        // Card 6: Profile
        profilePanel = new ProfilePanel(authService, session);
        contentPanel.add(profilePanel, CARD_PROFILE);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        button.setPreferredSize(new Dimension(190, 38));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        return button;
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Welcome Header
        String username = session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Admin";
        JLabel welcomeTitle = new JLabel("Welcome back, " + username + "!");
        welcomeTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcomeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel welcomeSubtitle = new JLabel("Campus Administration Overview & Metrics");
        welcomeSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        welcomeSubtitle.setForeground(Color.GRAY);
        welcomeSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(welcomeTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(welcomeSubtitle);
        panel.add(Box.createVerticalStrut(20));

        // Metrics Grid (DashboardCard components)
        JPanel cardsRow = new JPanel(new GridLayout(1, 3, 18, 18));
        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        totalStudentsCard = new DashboardCard("Total Students", "0", "Registered student records");
        activeStudentsCard = new DashboardCard("Active Students", "0", "Eligible for enrollment");
        inactiveStudentsCard = new DashboardCard("Inactive Students", "0", "Deactivated accounts");

        cardsRow.add(totalStudentsCard);
        cardsRow.add(activeStudentsCard);
        cardsRow.add(inactiveStudentsCard);

        panel.add(cardsRow);
        panel.add(Box.createVerticalStrut(28));

        // Quick Actions Card
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        quickActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Quick Access"),
                new EmptyBorder(10, 14, 10, 14)
        ));
        quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        JButton gotoStudentsBtn = new JButton("Open Student Management \u2192");
        gotoStudentsBtn.addActionListener(e -> {
            showCard(CARD_STUDENTS);
            studentsPanel.loadStudents();
        });

        JButton gotoEnrollmentsBtn = new JButton("Manage Enrollments \u2192");
        gotoEnrollmentsBtn.addActionListener(e -> {
            showCard(CARD_ENROLLMENTS);
            if (enrollmentsPanel != null) {
                enrollmentsPanel.refresh();
            }
        });

        JButton gotoFacultyBtn = new JButton("Manage Faculty \u2192");
        gotoFacultyBtn.addActionListener(e -> {
            showCard(CARD_FACULTY);
            facultyPanel.refresh();
        });

        JButton gotoCoursesBtn = new JButton("Manage Courses \u2192");
        gotoCoursesBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            coursesPanel.refresh();
        });

        JButton gotoProfileBtn = new JButton("View My Profile \u2192");
        gotoProfileBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });

        quickActions.add(gotoStudentsBtn);
        quickActions.add(gotoEnrollmentsBtn);
        quickActions.add(gotoFacultyBtn);
        quickActions.add(gotoCoursesBtn);
        quickActions.add(gotoProfileBtn);

        panel.add(quickActions);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    /**
     * Refreshes the KPI metric cards from real StudentDAO data.
     */
    public void refreshOverviewMetrics() {
        try {
            List<Student> students = studentDAO.findAll();
            long total = students.size();
            long active = students.stream().filter(Student::isActive).count();
            long inactive = total - active;

            totalStudentsCard.setValue(total);
            totalStudentsCard.setSubtitle("Registered student records");

            activeStudentsCard.setValue(active);
            activeStudentsCard.setSubtitle("Eligible for enrollment");

            inactiveStudentsCard.setValue(inactive);
            inactiveStudentsCard.setSubtitle("Deactivated accounts");
        } catch (SQLException e) {
            totalStudentsCard.setValue("N/A");
            totalStudentsCard.setSubtitle("Database unavailable");

            activeStudentsCard.setValue("N/A");
            activeStudentsCard.setSubtitle("Database unavailable");

            inactiveStudentsCard.setValue("N/A");
            inactiveStudentsCard.setSubtitle("Database unavailable");
        }
    }

    /**
     * Switches the active CardLayout view.
     */
    public void showCard(String cardName) {
        cardLayout.show(contentPanel, cardName);
    }

    void handleLogout() {
        logoutNavBtn.setEnabled(false);
        session.logout();
        dispose();
        new LoginFrame(authService).setVisible(true);
    }

    // Accessors for testing
    public CardLayout getCardLayout() {
        return cardLayout;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public DashboardCard getTotalStudentsCard() {
        return totalStudentsCard;
    }

    public DashboardCard getActiveStudentsCard() {
        return activeStudentsCard;
    }

    public DashboardCard getInactiveStudentsCard() {
        return inactiveStudentsCard;
    }

    public StudentsPanel getStudentsPanel() {
        return studentsPanel;
    }

    public AdminEnrollmentsPanel getAdminEnrollmentsPanel() {
        return enrollmentsPanel;
    }

    public ProfilePanel getProfilePanel() {
        return profilePanel;
    }

    public JButton getOverviewNavBtn() {
        return overviewNavBtn;
    }

    public JButton getStudentsNavBtn() {
        return studentsNavBtn;
    }

    public JButton getEnrollmentsNavBtn() {
        return enrollmentsNavBtn;
    }

    public JButton getFacultyNavBtn() {
        return facultyNavBtn;
    }

    public JButton getCoursesNavBtn() {
        return coursesNavBtn;
    }

    public FacultyPanel getFacultyPanel() {
        return facultyPanel;
    }

    public CoursesPanel getCoursesPanel() {
        return coursesPanel;
    }

    public JButton getProfileNavBtn() {
        return profileNavBtn;
    }

    public JButton getLogoutNavBtn() {
        return logoutNavBtn;
    }
}
