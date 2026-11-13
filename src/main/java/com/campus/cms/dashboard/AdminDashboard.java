package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.LoginFrame;
import com.campus.cms.auth.ProfilePanel;
import com.campus.cms.core.DashboardCard;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.SidebarNav;
import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;
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
    public static final String CARD_PROFILE = "PROFILE";

    private final StudentDAO studentDAO;
    private final AuthService authService;
    private final Session session;
    private final EnrollmentService enrollmentService;

    private CardLayout cardLayout;
    private JPanel contentPanel;

    private StudentsPanel studentsPanel;
    private AdminEnrollmentsPanel enrollmentsPanel;
    private ProfilePanel profilePanel;

    private DashboardCard totalStudentsCard;
    private DashboardCard activeStudentsCard;
    private DashboardCard inactiveStudentsCard;

    private JButton overviewNavBtn;
    private JButton studentsNavBtn;
    private JButton enrollmentsNavBtn;
    private JButton profileNavBtn;
    private JButton logoutNavBtn;
    private SidebarNav sidebar;
    private StatusBadge periodBadge;

    public AdminDashboard() {
        this(new StudentDAO(), new AuthService(), Session.getInstance(), new EnrollmentService());
    }

    public AdminDashboard(StudentDAO studentDAO, AuthService authService, Session session) {
        this(studentDAO, authService, session, new EnrollmentService());
    }

    public AdminDashboard(StudentDAO studentDAO, AuthService authService, Session session, EnrollmentService enrollmentService) {
        this.session = Objects.requireNonNull(session);
        if (!this.session.hasRole(Role.ADMIN)) {
            throw new SecurityException("Unauthorized access: AdminDashboard requires an active ADMIN session.");
        }
        this.studentDAO = Objects.requireNonNull(studentDAO);
        this.authService = Objects.requireNonNull(authService);
        this.enrollmentService = Objects.requireNonNull(enrollmentService);

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
        String adminName = session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Admin";
        sidebar = new SidebarNav("Campus CMS", "Administrator", adminName);

        overviewNavBtn = sidebar.addNav(new JButton("Overview"));
        studentsNavBtn = sidebar.addNav(new JButton("Student Management"));
        enrollmentsNavBtn = sidebar.addNav(new JButton("Enrollment Management"));
        profileNavBtn = sidebar.addNav(new JButton("My Profile"));
        logoutNavBtn = sidebar.setFooter(new JButton("Logout"));

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

        // Card 2: Student Management
        studentsPanel = new StudentsPanel(studentDAO);
        contentPanel.add(studentsPanel, CARD_STUDENTS);

        // Card 3: Enrollment Management
        enrollmentsPanel = new AdminEnrollmentsPanel(enrollmentService);
        contentPanel.add(enrollmentsPanel, CARD_ENROLLMENTS);

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

        String username = session.getCurrentUser() != null ? session.getCurrentUser().getUsername() : "Admin";
        PageHeader header = new PageHeader("Welcome back, " + username + "!",
                "Campus Administration Overview & Metrics");
        periodBadge = new StatusBadge("");
        periodBadge.setVisible(false);
        header.setTrailing(periodBadge);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(header);

        // Metrics grid: wraps to fewer columns on narrow windows
        JPanel cardsRow = new JPanel(new GridLayout(1, 3, Theme.SPACE_LG, Theme.SPACE_LG));
        cardsRow.setOpaque(false);
        cardsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalStudentsCard = new DashboardCard("Total Students", "0", "Registered student records", Theme.INFO);
        activeStudentsCard = new DashboardCard("Active Students", "0", "Eligible for enrollment", Theme.SUCCESS);
        inactiveStudentsCard = new DashboardCard("Inactive Students", "0", "Deactivated accounts", Theme.DANGER);

        cardsRow.add(totalStudentsCard);
        cardsRow.add(activeStudentsCard);
        cardsRow.add(inactiveStudentsCard);
        cardsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardsRow.getPreferredSize().height));

        body.add(cardsRow);
        body.add(Box.createVerticalStrut(Theme.SPACE_XL));

        // Quick access
        JPanel quickActions = Styles.surface(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_MD, 0));
        quickActions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton gotoStudentsBtn = Styles.primary(new JButton("Open Student Management \u2192"));
        gotoStudentsBtn.addActionListener(e -> {
            showCard(CARD_STUDENTS);
            studentsPanel.loadStudents();
        });

        JButton gotoEnrollmentsBtn = Styles.secondary(new JButton("Manage Enrollments \u2192"));
        gotoEnrollmentsBtn.addActionListener(e -> {
            showCard(CARD_ENROLLMENTS);
            if (enrollmentsPanel != null) {
                enrollmentsPanel.refresh();
            }
        });

        JButton gotoProfileBtn = Styles.secondary(new JButton("View My Profile \u2192"));
        gotoProfileBtn.addActionListener(e -> {
            showCard(CARD_PROFILE);
            profilePanel.refreshProfile();
        });

        JLabel quickTitle = new JLabel("Quick Access");
        quickTitle.setFont(Theme.HEADING);
        quickTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(quickTitle);
        body.add(Box.createVerticalStrut(Theme.SPACE_SM));

        quickActions.add(gotoStudentsBtn);
        quickActions.add(gotoEnrollmentsBtn);
        quickActions.add(gotoProfileBtn);
        quickActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, quickActions.getPreferredSize().height));
        body.add(quickActions);

        panel.add(body, BorderLayout.NORTH);
        return panel;
    }

    /**
     * Refreshes the KPI metric cards from real StudentDAO data.
     */
    public void refreshOverviewMetrics() {
        refreshPeriodBadge();
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
        if (sidebar != null) {
            sidebar.setActive(switch (cardName) {
                case CARD_STUDENTS -> studentsNavBtn;
                case CARD_ENROLLMENTS -> enrollmentsNavBtn;
                case CARD_PROFILE -> profileNavBtn;
                default -> overviewNavBtn;
            });
        }
    }

    /** Shows the live enrollment-period state as a badge (hidden if unavailable). */
    private void refreshPeriodBadge() {
        if (periodBadge == null) {
            return;
        }
        try {
            boolean open = enrollmentService.isEnrollmentOpen();
            periodBadge.setStatus(open ? "OPEN" : "CLOSED");
            periodBadge.setToolTipText("Enrollment period is " + (open ? "open" : "closed"));
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

    public JButton getProfileNavBtn() {
        return profileNavBtn;
    }

    public JButton getLogoutNavBtn() {
        return logoutNavBtn;
    }
}
