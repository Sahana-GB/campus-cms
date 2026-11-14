package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.auth.User;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.SidebarNav;
import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;
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
    private SidebarNav sidebar;
    private JLabel cgpaValueLabel;
    private StatusBadge periodBadge;

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
        String studentName = currentStudent != null ? currentStudent.getName()
                : (session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Student");
        sidebar = new SidebarNav("Campus CMS", "Student Portal", studentName);

        overviewNavBtn = sidebar.addNav(new JButton("Overview"));
        coursesNavBtn = sidebar.addNav(new JButton("My Courses"));
        assignmentsNavBtn = sidebar.addNav(new JButton("Assignments & Grades"));
        profileNavBtn = sidebar.addNav(new JButton("My Profile"));
        logoutNavBtn = sidebar.setFooter(new JButton("Logout"));

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

        contentPanel.setBackground(Theme.BACKGROUND);
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

        PageHeader header = new PageHeader("Welcome, " + displayName + "!", subtitleText);
        welcomeTitle = header.getTitleLabel();
        welcomeSubtitle = header.getSubtitleLabel();
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Trailing badges: CGPA and enrollment period (filled in by refreshOverviewMetrics)
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        badges.setOpaque(false);
        JLabel cgpaCaption = Styles.fieldLabel("CGPA");
        cgpaValueLabel = new JLabel("-");
        cgpaValueLabel.setFont(Theme.HEADING);
        cgpaValueLabel.setForeground(Theme.PRIMARY);
        periodBadge = new StatusBadge("");
        periodBadge.setVisible(false);
        badges.add(cgpaCaption);
        badges.add(cgpaValueLabel);
        badges.add(periodBadge);
        header.setTrailing(badges);
        body.add(header);

        // Metrics grid
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, Theme.SPACE_LG, Theme.SPACE_LG));
        cardsRow.setOpaque(false);
        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        enrolledCoursesCard = new DashboardCard("Enrolled Courses", "0", "Approved courses", Theme.INFO);
        pendingAssignmentsCard = new DashboardCard("Pending Tasks", "0", "Open assignments", Theme.WARNING);
        completedAssignmentsCard = new DashboardCard("Completed Tasks", "0", "Graded assignments", Theme.SUCCESS);
        publishedGradesCard = new DashboardCard("Published Grades", "0", "Available results", Theme.PRIMARY);

        cardsRow.add(enrolledCoursesCard);
        cardsRow.add(pendingAssignmentsCard);
        cardsRow.add(completedAssignmentsCard);
        cardsRow.add(publishedGradesCard);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardsRow.getPreferredSize().height));

        body.add(cardsRow);
        body.add(Box.createVerticalStrut(Theme.SPACE_XL));

        // Quick access
        JLabel quickTitle = new JLabel("Quick Access");
        quickTitle.setFont(Theme.HEADING);
        quickTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(quickTitle);
        body.add(Box.createVerticalStrut(Theme.SPACE_SM));

        JPanel quickActions = Styles.actionBar();

        JButton gotoCoursesBtn = Styles.primary(new JButton("View My Courses \u2192"));
        gotoCoursesBtn.addActionListener(e -> {
            showCard(CARD_COURSES);
            if (studentCoursesPanel != null) {
                studentCoursesPanel.refresh();
            }
        });

        JButton gotoAssignmentsBtn = Styles.secondary(new JButton("Check Assignments & Grades \u2192"));
        gotoAssignmentsBtn.addActionListener(e -> {
            showCard(CARD_ASSIGNMENTS);
            refreshAssignmentsAndGrades();
        });

        JButton gotoProfileBtn = Styles.secondary(new JButton("View My Profile \u2192"));
        gotoProfileBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });

        quickActions.add(gotoCoursesBtn);
        quickActions.add(gotoAssignmentsBtn);
        quickActions.add(gotoProfileBtn);
        body.add(quickActions);
        body.add(Box.createVerticalStrut(Theme.SPACE_XL));

        // Portal notice
        JPanel noticeCard = Styles.surface(new BorderLayout(0, Theme.SPACE_SM));
        noticeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel noticeTitle = new JLabel("Academic Portal Active");
        noticeTitle.setFont(Theme.HEADING);
        JLabel noticeBody = new JLabel("<html>Course enrollment browsing, timetable schedules, and assignment grade tracking "
                + "are integrated and live. Use the sidebar to navigate through your academic resources.</html>");
        noticeBody.setFont(Theme.SMALL);
        noticeBody.setForeground(Theme.TEXT_MUTED);
        noticeCard.add(noticeTitle, BorderLayout.NORTH);
        noticeCard.add(noticeBody, BorderLayout.CENTER);
        noticeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, noticeCard.getPreferredSize().height + 24));
        body.add(noticeCard);

        panel.add(body, BorderLayout.NORTH);
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
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(Styles.pagePadding());

        // Top Control Bar
        JPanel topPanel = new com.campus.cms.core.ui.FilterBar();

        JLabel selectLabel = Styles.fieldLabel("SELECT ENROLLED COURSE");

        courseSelectorCombo = new JComboBox<>();
        Styles.input(courseSelectorCombo, 340);
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

        JButton refreshBtn = Styles.secondary(new JButton("Refresh"));
        refreshBtn.addActionListener(e -> refreshAssignmentsAndGrades());

        topPanel.add(selectLabel);
        topPanel.add(courseSelectorCombo);
        topPanel.add(refreshBtn);

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(new PageHeader("Assignments & Grades", "Review published grades for your enrolled courses."),
                BorderLayout.NORTH);
        north.add(topPanel, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);

        gradesContainerPanel = new JPanel(new BorderLayout());
        gradesContainerPanel.setOpaque(false);
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

        refreshBadges(studentId);

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
        if (sidebar != null) {
            sidebar.setActive(switch (cardName) {
                case CARD_COURSES -> coursesNavBtn;
                case CARD_ASSIGNMENTS -> assignmentsNavBtn;
                case CARD_PROFILE -> profileNavBtn;
                default -> overviewNavBtn;
            });
        }
    }

    /** Updates the CGPA and enrollment-period badges from existing service methods (visual only). */
    private void refreshBadges(int studentId) {
        try {
            cgpaValueLabel.setText(String.format("%.2f", enrollmentService.cgpa(studentId)));
        } catch (Exception ex) {
            cgpaValueLabel.setText("-");
        }
        try {
            boolean open = enrollmentService.isEnrollmentOpen();
            periodBadge.setStatus(open ? "OPEN" : "CLOSED");
            periodBadge.setVisible(true);
        } catch (Exception ex) {
            periodBadge.setVisible(false);
        }
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
