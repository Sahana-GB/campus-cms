package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.User;
import com.campus.cms.auth.UserDAO;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.students.Student;
import com.campus.cms.students.StudentDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardTest {

    private Session session;
    private StubStudentDAO stubDAO;
    private AuthService authService;

    private static class StubStudentDAO extends StudentDAO {
        final List<Student> list = new ArrayList<>();

        @Override
        public List<Student> findAll() {
            return new ArrayList<>(list);
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        session = Session.getInstance();
        session.logout();

        User admin = new User(1, "admin_test", "hash", Role.ADMIN, true);
        session.login(admin);

        stubDAO = new StubStudentDAO();
        stubDAO.list.add(new Student(1, 10, "CS2401", "Asha", "CSE", 2, "a@c.edu", "ACTIVE", "asha"));
        stubDAO.list.add(new Student(2, 11, "CS2402", "Ravi", "CSE", 3, "r@c.edu", "ACTIVE", "ravi"));
        stubDAO.list.add(new Student(3, 12, "EC2401", "Karthik", "ECE", 2, "k@c.edu", "INACTIVE", "karthik"));

        authService = new AuthService(new UserDAO(), session);
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void dashboardInitializesAndCalculatesStudentKPIs() {
        AdminDashboard dashboard = new AdminDashboard(stubDAO, authService, session);

        assertEquals("3", dashboard.getTotalStudentsCard().getValue());
        assertEquals("2", dashboard.getActiveStudentsCard().getValue());
        assertEquals("1", dashboard.getInactiveStudentsCard().getValue());

        assertTrue(dashboard.getTitle().contains("Administrator"));
        dashboard.dispose();
    }

    @Test
    void navigationButtonsTriggerCardSwitches() {
        AdminDashboard dashboard = new AdminDashboard(stubDAO, authService, session);

        // Click Student Management
        dashboard.getStudentsNavBtn().doClick();
        assertNotNull(dashboard.getStudentsPanel());

        // Click My Profile
        dashboard.getProfileNavBtn().doClick();
        assertNotNull(dashboard.getProfilePanel());

        // Click Overview
        dashboard.getOverviewNavBtn().doClick();
        assertEquals("3", dashboard.getTotalStudentsCard().getValue());

        dashboard.dispose();
    }

    @Test
    void logoutClearsSessionAndDisposesWindow() {
        AdminDashboard dashboard = new AdminDashboard(stubDAO, authService, session);
        assertTrue(session.isLoggedIn());

        dashboard.handleLogout();

        assertFalse(session.isLoggedIn());
        assertFalse(dashboard.isDisplayable());
    }

    @Test
    void adminSessionSuccessfullyOpensDashboard() {
        assertDoesNotThrow(() -> {
            AdminDashboard dashboard = new AdminDashboard(stubDAO, authService, session);
            dashboard.dispose();
        });
    }

    @Test
    void facultySessionRejectedWithSecurityException() {
        User faculty = new User(2, "faculty_user", "hash", Role.FACULTY, true);
        session.login(faculty);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> new AdminDashboard(stubDAO, authService, session));
        assertTrue(ex.getMessage().contains("ADMIN session"));
    }

    @Test
    void studentSessionRejectedWithSecurityException() {
        User student = new User(3, "student_user", "hash", Role.STUDENT, true);
        session.login(student);

        SecurityException ex = assertThrows(SecurityException.class,
                () -> new AdminDashboard(stubDAO, authService, session));
        assertTrue(ex.getMessage().contains("ADMIN session"));
    }

    @Test
    void unauthenticatedAccessRejectedWithSecurityException() {
        session.logout();

        SecurityException ex = assertThrows(SecurityException.class,
                () -> new AdminDashboard(stubDAO, authService, session));
        assertTrue(ex.getMessage().contains("ADMIN session"));
    }

    @Test
    void databaseFailureDisplaysNAAndUnavailableSubtitle() {
        StudentDAO failingDAO = new StudentDAO() {
            @Override
            public List<Student> findAll() throws java.sql.SQLException {
                throw new java.sql.SQLException("Disk read error");
            }
        };

        AdminDashboard dashboard = new AdminDashboard(failingDAO, authService, session);

        assertEquals("N/A", dashboard.getTotalStudentsCard().getValue());
        assertEquals("Database unavailable", dashboard.getTotalStudentsCard().getSubtitle());

        assertEquals("N/A", dashboard.getActiveStudentsCard().getValue());
        assertEquals("Database unavailable", dashboard.getActiveStudentsCard().getSubtitle());

        assertEquals("N/A", dashboard.getInactiveStudentsCard().getValue());
        assertEquals("Database unavailable", dashboard.getInactiveStudentsCard().getSubtitle());

        dashboard.dispose();
    }

    @Test
    void successfulRefreshRestoresNormalMetricValues() {
        boolean[] fail = {true};
        StudentDAO togglingDAO = new StudentDAO() {
            @Override
            public List<Student> findAll() throws java.sql.SQLException {
                if (fail[0]) {
                    throw new java.sql.SQLException("Initial error");
                }
                return stubDAO.list;
            }
        };

        AdminDashboard dashboard = new AdminDashboard(togglingDAO, authService, session);
        assertEquals("N/A", dashboard.getTotalStudentsCard().getValue());
        assertEquals("Database unavailable", dashboard.getTotalStudentsCard().getSubtitle());

        // Now database recovers
        fail[0] = false;
        dashboard.refreshOverviewMetrics();

        assertEquals("3", dashboard.getTotalStudentsCard().getValue());
        assertEquals("Registered student records", dashboard.getTotalStudentsCard().getSubtitle());

        assertEquals("2", dashboard.getActiveStudentsCard().getValue());
        assertEquals("Eligible for enrollment", dashboard.getActiveStudentsCard().getSubtitle());

        assertEquals("1", dashboard.getInactiveStudentsCard().getValue());
        assertEquals("Deactivated accounts", dashboard.getInactiveStudentsCard().getSubtitle());

        dashboard.dispose();
    }

    @Test
    void logoutDisablesLogoutButton() {
        AdminDashboard dashboard = new AdminDashboard(stubDAO, authService, session);
        assertTrue(dashboard.getLogoutNavBtn().isEnabled());

        dashboard.handleLogout();

        assertFalse(dashboard.getLogoutNavBtn().isEnabled());
    }
}
