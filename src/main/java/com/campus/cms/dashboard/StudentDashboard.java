package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.auth.User;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.enrollment.BrowsePanel;
import com.campus.cms.enrollment.Enrollment;
import com.campus.cms.enrollment.EnrollmentException;
import com.campus.cms.enrollment.EnrollmentService;
import com.campus.cms.enrollment.StudentCoursesPanel;
import com.campus.cms.enrollment.TimetablePanel;
import com.campus.cms.grading.Assignment;
import com.campus.cms.grading.AssignmentDAO;
import com.campus.cms.grading.Grade;
import com.campus.cms.grading.GradeDAO;
import com.campus.cms.grading.GradeService;
import com.campus.cms.grading.GradesPanel;
import com.campus.cms.students.Student;
import com.campus.cms.students.StudentDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Main application dashboard for users with the STUDENT role.
 * Features sidebar navigation across Overview, My Courses (Enrollments, Catalog, Timetable),
 * Assignments & Grades, and Profile, with live metrics and secure session logout handling.
 */
public class StudentDashboard extends JFrame {

    public static final String CARD_OVERVIEW = "OVERVIEW";
    public static final String CARD_COURSES = "COURSES";
    public static final String CARD_ASSIGNMENTS = "ASSIGNMENTS";
    public static final String CARD_PROFILE = "PROFILE";

    private final StudentDAO studentDAO;
    private final AuthService authService;
    private final Session session;
    private final EnrollmentService enrollmentService;
    private final AssignmentDAO assignmentDAO;
    private final GradeDAO gradeDAO;

    private Student currentStudent;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private JTabbedPane coursesTabs;
    private BrowsePanel browsePanel;
    private StudentCoursesPanel studentCoursesPanel;
    private TimetablePanel timetablePanel;

    private JComboBox<Enrollment> courseSelectorCombo;
    private JPanel gradesContainerPanel;
    private GradesPanel currentGradesPanel;

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
        this(new StudentDAO(), new AuthService(), Session.getInstance(),
                new EnrollmentService(), new AssignmentDAO(), new GradeDAO());
    }

    public StudentDashboard(StudentDAO studentDAO, AuthService authService, Session session) {
        this(studentDAO, authService, session,
                new EnrollmentService(), new AssignmentDAO(), new GradeDAO());
    }

    public StudentDashboard(StudentDAO studentDAO, AuthService authService, Session session,
                            EnrollmentService enrollmentService, AssignmentDAO assignmentDAO, GradeDAO gradeDAO) {
        this.session = Objects.requireNonNull(session);
        if (!this.session.hasRole(Role.STUDENT)) {
            throw new SecurityException("Unauthorized access: StudentDashboard requires an active STUDENT session.");
        }
        this.studentDAO = Objects.requireNonNull(studentDAO);
        this.authService = Objects.requireNonNull(authService);
        this.enrollmentService = Objects.requireNonNull(enrollmentService);
        this.assignmentDAO = Objects.requireNonNull(assignmentDAO);
        this.gradeDAO = Objects.requireNonNull(gradeDAO);

        loadStudentProfile();
        initUI();
        refreshOverviewMetrics();
    }

    private void loadStudentProfile() {
        User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            try {
                this.currentStudent = studentDAO.findByUserId(currentUser.getUserId()).orElse(null);
            } catch (SQLException e) {
                this.currentStudent = null;
            }
        }
    }

    private int getStudentId() {
        if (currentStudent != null) {
            return currentStudent.getStudentId();
        }
        try {
            return enrollmentService.myStudentId();
        } catch (Exception e) {
            return -1;
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

        overviewNavBtn.addActionListener(e -> {
            showCard(CARD_OVERVIEW);
            refreshOverviewMetrics();
        });
        coursesNavBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            if (studentCoursesPanel != null) {
                studentCoursesPanel.refresh();
            }
        });
        assignmentsNavBtn.addActionListener(e -> {
            showCard(CARD_ASSIGNMENTS);
            refreshAssignmentsAndGrades();
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

        // Card 2: My Courses (Sub-navigation using JTabbedPane)
        JPanel coursesCard = createCoursesTabbedPanel();
        contentPanel.add(coursesCard, CARD_COURSES);

        // Card 3: Assignments & Grades
        JPanel assignmentsCard = createAssignmentsAndGradesPanel();
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

        enrolledCoursesCard = new DashboardCard("Enrolled Courses", "0", "Approved courses");
        pendingAssignmentsCard = new DashboardCard("Pending Tasks", "0", "Open assignments");
        completedAssignmentsCard = new DashboardCard("Completed Tasks", "0", "Graded assignments");
        publishedGradesCard = new DashboardCard("Published Grades", "0", "Available results");

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
        gotoCoursesBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            if (studentCoursesPanel != null) {
                studentCoursesPanel.refresh();
            }
        });

        JButton gotoAssignmentsBtn = new JButton("Check Assignments & Grades \u2192");
        gotoAssignmentsBtn.addActionListener(e -> {
            showCard(CARD_ASSIGNMENTS);
            refreshAssignmentsAndGrades();
        });

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

        // Academic Modules Status Card
        JPanel noticeCard = new JPanel();
        noticeCard.setLayout(new BoxLayout(noticeCard, BoxLayout.Y_AXIS));
        noticeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238), 1),
                new EmptyBorder(16, 18, 16, 18)
        ));
        noticeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        noticeCard.setMaximumSize(new Dimension(1000, 100));

        JLabel noticeTitle = new JLabel("Academic Portal Active");
        noticeTitle.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel noticeBody = new JLabel("<html>Course enrollment browsing, timetable schedules, and assignment grade tracking<br>are integrated and live. Use the sidebar to navigate through your academic resources.</html>");
        noticeBody.setFont(new Font("SansSerif", Font.PLAIN, 12));
        noticeBody.setForeground(Color.DARK_GRAY);

        noticeCard.add(noticeTitle);
        noticeCard.add(Box.createVerticalStrut(6));
        noticeCard.add(noticeBody);

        panel.add(noticeCard);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createCoursesTabbedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        coursesTabs = new JTabbedPane();

        studentCoursesPanel = new StudentCoursesPanel(enrollmentService);
        browsePanel = new BrowsePanel(enrollmentService);
        timetablePanel = new TimetablePanel(enrollmentService);

        coursesTabs.addTab("Enrolled Courses", studentCoursesPanel);
        coursesTabs.addTab("Browse & Enroll", browsePanel);
        coursesTabs.addTab("Timetable", timetablePanel);

        coursesTabs.addChangeListener(e -> {
            Component selected = coursesTabs.getSelectedComponent();
            if (selected == studentCoursesPanel) {
                studentCoursesPanel.refresh();
            } else if (selected == browsePanel) {
                browsePanel.refresh();
            } else if (selected == timetablePanel) {
                timetablePanel.refresh();
            }
        });

        panel.add(coursesTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAssignmentsAndGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Control Bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(225, 230, 235)),
                new EmptyBorder(0, 0, 8, 0)
        ));

        JLabel selectLabel = new JLabel("Select Enrolled Course:");
        selectLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        courseSelectorCombo = new JComboBox<>();
        courseSelectorCombo.setPreferredSize(new Dimension(340, 30));
        courseSelectorCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Enrollment e) {
                    setText(e.getCourseCode() + " - " + e.getCourseTitle() + " (Slot " + e.getSlot() + ")");
                } else if (value != null) {
                    setText(value.toString());
                }
                return this;
            }
        });

        courseSelectorCombo.addActionListener(e -> updateGradesView());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAssignmentsAndGrades());

        topPanel.add(selectLabel);
        topPanel.add(courseSelectorCombo);
        topPanel.add(refreshBtn);

        panel.add(topPanel, BorderLayout.NORTH);

        gradesContainerPanel = new JPanel(new BorderLayout());
        panel.add(gradesContainerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void updateGradesView() {
        gradesContainerPanel.removeAll();
        Object selected = courseSelectorCombo.getSelectedItem();
        if (selected instanceof Enrollment e) {
            int studentId = getStudentId();
            if (studentId > 0) {
                GradesPanel gp = new GradesPanel(e.getCourseId(), false, studentId);
                currentGradesPanel = gp;
                gradesContainerPanel.add(gp, BorderLayout.CENTER);
            } else {
                JLabel err = new JLabel("Could not identify current student ID.", SwingConstants.CENTER);
                gradesContainerPanel.add(err, BorderLayout.CENTER);
            }
        } else {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            JLabel emptyLabel = new JLabel("No enrolled courses available. Browse courses and request enrollment first.");
            emptyLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyPanel.add(emptyLabel);
            gradesContainerPanel.add(emptyPanel, BorderLayout.CENTER);
        }
        gradesContainerPanel.revalidate();
        gradesContainerPanel.repaint();
    }

    public void refreshAssignmentsAndGrades() {
        int studentId = getStudentId();
        courseSelectorCombo.removeAllItems();
        if (studentId > 0) {
            try {
                List<Enrollment> enrollments = enrollmentService.myEnrollments(studentId);
                for (Enrollment e : enrollments) {
                    if ("APPROVED".equalsIgnoreCase(e.getStatus())) {
                        courseSelectorCombo.addItem(e);
                    }
                }
            } catch (Exception ex) {
                // Ignore exception on refresh
            }
        }
        updateGradesView();
    }

    /**
     * Refreshes the KPI metric cards from live database data.
     */
    public void refreshOverviewMetrics() {
        int studentId = getStudentId();
        if (studentId <= 0) {
            return;
        }

        try {
            // 1. Enrolled Courses
            int approved = enrollmentService.countMine(studentId, "APPROVED");
            int pending = enrollmentService.countMine(studentId, "PENDING");
            enrolledCoursesCard.setValue(approved);
            enrolledCoursesCard.setSubtitle(pending == 0 ? "Active semester enrollments" : pending + " pending request(s)");

            // 2. Published Grades
            List<Grade> publishedGrades = gradeDAO.getStudentPublishedGrades(studentId);
            publishedGradesCard.setValue(publishedGrades.size());
            publishedGradesCard.setSubtitle(publishedGrades.size() == 1 ? "1 published grade" : publishedGrades.size() + " published grades");

            // 3. Pending & Completed Assignments
            List<Enrollment> approvedEnrollments = enrollmentService.myEnrollments(studentId).stream()
                    .filter(e -> "APPROVED".equalsIgnoreCase(e.getStatus()))
                    .toList();

            Set<Integer> gradedAssignmentIds = publishedGrades.stream()
                    .map(Grade::getAssignmentId)
                    .collect(Collectors.toSet());

            int pendingTasks = 0;
            int completedTasks = 0;

            for (Enrollment e : approvedEnrollments) {
                List<Assignment> assignments = assignmentDAO.getAssignmentsByCourse(e.getCourseId());
                for (Assignment a : assignments) {
                    if (gradedAssignmentIds.contains(a.getAssignmentId())) {
                        completedTasks++;
                    } else if ("OPEN".equalsIgnoreCase(a.getStatus())) {
                        pendingTasks++;
                    }
                }
            }

            pendingAssignmentsCard.setValue(pendingTasks);
            pendingAssignmentsCard.setSubtitle(pendingTasks == 1 ? "1 active task due" : pendingTasks + " active tasks due");

            completedAssignmentsCard.setValue(completedTasks);
            completedAssignmentsCard.setSubtitle(completedTasks == 1 ? "1 completed task" : completedTasks + " completed tasks");

        } catch (Exception e) {
            // Keep existing values if error occurs
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

    public Student getCurrentStudent() {
        return currentStudent;
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

    public JTabbedPane getCoursesTabs() {
        return coursesTabs;
    }

    public BrowsePanel getBrowsePanel() {
        return browsePanel;
    }

    public StudentCoursesPanel getStudentCoursesPanel() {
        return studentCoursesPanel;
    }

    public TimetablePanel getTimetablePanel() {
        return timetablePanel;
    }

    public JComboBox<Enrollment> getCourseSelectorCombo() {
        return courseSelectorCombo;
    }

    public GradesPanel getCurrentGradesPanel() {
        return currentGradesPanel;
    }
}
