package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.enrollment.EnrollmentException;
import com.campus.cms.enrollment.BrowsePanel;
import com.campus.cms.enrollment.EnrollmentService;
import com.campus.cms.enrollment.StudentCoursesPanel;
import com.campus.cms.enrollment.TimetablePanel;
import com.campus.cms.auth.User;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.students.Student;
import com.campus.cms.students.StudentDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Main application dashboard for users with the STUDENT role.
 * Features sidebar navigation across Overview, My Courses, Assignments & Grades,
 * and Profile, with personalized student metadata, placeholder cards for pending modules,
 * and secure session logout handling.
 */
public class StudentDashboard extends JFrame {

    public static final String CARD_OVERVIEW = "OVERVIEW";
    public static final String CARD_COURSES = "COURSES";
    public static final String CARD_ASSIGNMENTS = "ASSIGNMENTS";
    public static final String CARD_PROFILE = "PROFILE";

    private final StudentDAO studentDAO;
    private final AuthService authService;
    private final Session session;

    private Student currentStudent;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private ProfilePanel profilePanel;

    private DashboardCard enrolledCoursesCard;
    private DashboardCard pendingAssignmentsCard;
    private DashboardCard completedAssignmentsCard;
    private DashboardCard publishedGradesCard;

    private JButton overviewNavBtn;
    private JButton coursesNavBtn;
    private JButton assignmentsNavBtn;
    private JButton profileNavBtn;
    private JButton logoutNavBtn;

    private JLabel welcomeTitle;
    private JLabel welcomeSubtitle;

    public StudentDashboard() {
        this(new StudentDAO(), new AuthService(), Session.getInstance());
    }

    public StudentDashboard(StudentDAO studentDAO, AuthService authService, Session session) {
        this.session = Objects.requireNonNull(session);
        if (!this.session.hasRole(Role.STUDENT)) {
            throw new SecurityException("Unauthorized access: StudentDashboard requires an active STUDENT session.");
        }
        this.studentDAO = Objects.requireNonNull(studentDAO);
        this.authService = Objects.requireNonNull(authService);

        loadStudentProfile();
        initUI();
    }

    private void loadStudentProfile() {
        User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            try {
                this.currentStudent = studentDAO.findByUserId(currentUser.getUserId()).orElse(null);
            } catch (SQLException e) {
                // Graceful fallback: profile details remain null if database read encounters an issue
                this.currentStudent = null;
            }
        }
    }

    private void initUI() {
        setTitle("Campus CMS - Student Portal");
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

        JLabel brandRole = new JLabel("Student Portal");
        brandRole.setFont(new Font("SansSerif", Font.PLAIN, 12));
        brandRole.setForeground(Color.GRAY);
        brandRole.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(brandTitle);
        sidebar.add(Box.createVerticalStrut(4));
        sidebar.add(brandRole);
        sidebar.add(Box.createVerticalStrut(24));

        // Navigation Buttons
        overviewNavBtn = createNavButton("Overview");
        coursesNavBtn = createNavButton("My Courses");
        assignmentsNavBtn = createNavButton("Assignments & Grades");
        profileNavBtn = createNavButton("My Profile");
        logoutNavBtn = createNavButton("Logout");

        overviewNavBtn.addActionListener(e -> showCard(CARD_OVERVIEW));
        coursesNavBtn.addActionListener(e -> showCard(CARD_COURSES));
        assignmentsNavBtn.addActionListener(e -> showCard(CARD_ASSIGNMENTS));
        profileNavBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });
        logoutNavBtn.addActionListener(e -> handleLogout());

        sidebar.add(overviewNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(coursesNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(assignmentsNavBtn);
        sidebar.add(Box.createVerticalStrut(8));
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
        overviewCard.addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && overviewCard.isShowing()) {
                refreshEnrollmentMetrics();
            }
        });

        // Card 2: My Courses (Person C module placeholder)
        javax.swing.JTabbedPane coursesTabs = new javax.swing.JTabbedPane();
        EnrollmentService enrollmentService = new EnrollmentService();
        coursesTabs.addTab("Browse Courses", new BrowsePanel(enrollmentService));
        coursesTabs.addTab("My Courses", new StudentCoursesPanel(enrollmentService));
        coursesTabs.addTab("Timetable", new TimetablePanel(enrollmentService));
        JPanel coursesCard = new JPanel(new BorderLayout());
        coursesCard.add(coursesTabs, BorderLayout.CENTER);
        contentPanel.add(coursesCard, CARD_COURSES);

        // Card 3: Assignments & Grades (Person D module placeholder)
        JPanel assignmentsCard = createPlaceholderPanel(
                "Assignments & Grades",
                "Assignment submissions, quiz schedules, grading feedback, and grade report cards will be available once the Grading & Materials module (Person D) is integrated.",
                "Grading Module Integration (Person D)"
        );
        contentPanel.add(assignmentsCard, CARD_ASSIGNMENTS);

        // Card 4: Profile
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Welcome Header
        String displayName;
        String subtitleText;

        if (currentStudent != null) {
            displayName = currentStudent.getName();
            subtitleText = String.format("Roll Number: %s  |  Branch: %s  |  Year %d  |  Status: %s",
                    currentStudent.getRollNumber(),
                    currentStudent.getBranch(),
                    currentStudent.getYear(),
                    currentStudent.getStatus());
        } else {
            displayName = session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Student";
            subtitleText = "Student Academic Portal & Course Overview";
        }

        welcomeTitle = new JLabel("Welcome, " + displayName + "!");
        welcomeTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcomeTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        welcomeSubtitle = new JLabel(subtitleText);
        welcomeSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        welcomeSubtitle.setForeground(Color.GRAY);
        welcomeSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(welcomeTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(welcomeSubtitle);
        panel.add(Box.createVerticalStrut(22));

        // Metrics Grid (4 DashboardCard components)
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, 14, 14));
        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsRow.setMaximumSize(new Dimension(1000, 110));

        enrolledCoursesCard = new DashboardCard("Enrolled Courses", "—", "Awaiting Course Module");
        pendingAssignmentsCard = new DashboardCard("Pending Tasks", "—", "Awaiting Assignment Module");
        completedAssignmentsCard = new DashboardCard("Completed Tasks", "—", "Awaiting Assignment Module");
        publishedGradesCard = new DashboardCard("Published Grades", "—", "Awaiting Grading Module");

        cardsRow.add(enrolledCoursesCard);
        cardsRow.add(pendingAssignmentsCard);
        cardsRow.add(completedAssignmentsCard);
        cardsRow.add(publishedGradesCard);

        panel.add(cardsRow);
        panel.add(Box.createVerticalStrut(28));

        // Quick Actions Card
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        quickActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Quick Access"),
                new EmptyBorder(10, 14, 10, 14)
        ));
        quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickActions.setMaximumSize(new Dimension(1000, 80));

        JButton gotoCoursesBtn = new JButton("View My Courses \u2192");
        gotoCoursesBtn.addActionListener(e -> showCard(CARD_COURSES));

        JButton gotoAssignmentsBtn = new JButton("Check Assignments & Grades \u2192");
        gotoAssignmentsBtn.addActionListener(e -> showCard(CARD_ASSIGNMENTS));

        JButton gotoProfileBtn = new JButton("View My Profile \u2192");
        gotoProfileBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });

        quickActions.add(gotoCoursesBtn);
        quickActions.add(gotoAssignmentsBtn);
        quickActions.add(gotoProfileBtn);

        panel.add(quickActions);
        panel.add(Box.createVerticalStrut(24));

        // Module Integration Notice
        JPanel noticeCard = new JPanel();
        noticeCard.setLayout(new BoxLayout(noticeCard, BoxLayout.Y_AXIS));
        noticeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(16, 18, 16, 18)
        ));
        noticeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        noticeCard.setMaximumSize(new Dimension(1000, 100));

        JLabel noticeTitle = new JLabel("Academic Modules Status");
        noticeTitle.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel noticeBody = new JLabel("<html>Real-time course enrollments, pending assignment submissions, and published grade reports<br>will become fully interactive once the respective modules (Persons C & D) are integrated.</html>");
        noticeBody.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noticeBody.setForeground(Color.DARK_GRAY);

        noticeCard.add(noticeTitle);
        noticeCard.add(Box.createVerticalStrut(6));
        noticeCard.add(noticeBody);

        panel.add(noticeCard);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createPlaceholderPanel(String title, String description, String badge) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(30, 36, 30, 36));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badgeLabel = new JLabel(badge);
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        badgeLabel.setForeground(new Color(60, 110, 180));
        badgeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235), 1),
                new EmptyBorder(20, 24, 20, 24)
        ));
        infoBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoBox.setMaximumSize(new Dimension(900, 130));

        JLabel infoHeader = new JLabel("Module Under Construction");
        infoHeader.setFont(new Font("SansSerif", Font.BOLD, 15));

        JLabel infoText = new JLabel("<html><body style='width: 600px;'>" + description + "</body></html>");
        infoText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        infoText.setForeground(Color.DARK_GRAY);

        infoBox.add(infoHeader);
        infoBox.add(Box.createVerticalStrut(8));
        infoBox.add(infoText);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(badgeLabel);
        panel.add(Box.createVerticalStrut(24));
        panel.add(infoBox);
        panel.add(Box.createVerticalGlue());

        return panel;
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

    public Student getCurrentStudent() {
        return currentStudent;
    }

    /** Fills the Enrolled Courses card from the database. Keeps the placeholder if nobody is logged in as a student. */
    public void refreshEnrollmentMetrics() {
        try {
            EnrollmentService enrollment = new EnrollmentService();
            int me = enrollment.myStudentId();
            int approved = enrollment.countMine(me, "APPROVED");
            int pending = enrollment.countMine(me, "PENDING");
            enrolledCoursesCard.setValue(approved);
            enrolledCoursesCard.setSubtitle(pending == 0 ? "No pending requests" : pending + " pending request(s)");
        } catch (EnrollmentException | IllegalStateException e) {
            // no student session or a database problem: leave the card as it is
        }
    }

    public DashboardCard getEnrolledCoursesCard() {
        return enrolledCoursesCard;
    }

    public DashboardCard getPendingAssignmentsCard() {
        return pendingAssignmentsCard;
    }

    public DashboardCard getCompletedAssignmentsCard() {
        return completedAssignmentsCard;
    }

    public DashboardCard getPublishedGradesCard() {
        return publishedGradesCard;
    }

    public ProfilePanel getProfilePanel() {
        return profilePanel;
    }

    public JButton getOverviewNavBtn() {
        return overviewNavBtn;
    }

    public JButton getCoursesNavBtn() {
        return coursesNavBtn;
    }

    public JButton getAssignmentsNavBtn() {
        return assignmentsNavBtn;
    }

    public JButton getProfileNavBtn() {
        return profileNavBtn;
    }

    public JButton getLogoutNavBtn() {
        return logoutNavBtn;
    }

    public JLabel getWelcomeTitle() {
        return welcomeTitle;
    }

    public JLabel getWelcomeSubtitle() {
        return welcomeSubtitle;
    }
}
