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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudentDashboardTest {

    private Session session;
    private StubStudentDAO stubDAO;
    private AuthService authService;

    private static class StubStudentDAO extends StudentDAO {
        final List<Student> list = new ArrayList<>();
        boolean findAllCalled = false;
        boolean searchCalled = false;
        boolean findByUserIdCalled = false;
        boolean shouldThrowOnFindByUserId = false;

        @Override
        public List<Student> findAll() {
            findAllCalled = true;
            return new ArrayList<>(list);
        }

        @Override
        public List<Student> search(String query, String branch, Integer year) {
            searchCalled = true;
            return new ArrayList<>(list);
        }

        @Override
        public Optional<Student> findByUserId(int userId) throws SQLException {
            findByUserIdCalled = true;
            if (shouldThrowOnFindByUserId) {
                throw new SQLException("Simulated database failure");
            }
            return list.stream().filter(s -> s.getUserId() == userId).findFirst();
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        session = Session.getInstance();
        session.logout();

        User studentUser = new User(4, "student1", "hash", Role.STUDENT, true);
        session.login(studentUser);

        stubDAO = new StubStudentDAO();
        stubDAO.list.add(new Student(1, 4, "CS2401", "Asha Reddy", "CSE", 2, "asha@campus.edu", "ACTIVE", "student1"));

        authService = new AuthService(new UserDAO(), session);
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void dashboardInitializesForStudentWithLinkedProfile() {
        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);

        assertNotNull(dashboard.getCurrentStudent());
        assertEquals("CS2401", dashboard.getCurrentStudent().getRollNumber());
        assertEquals("Asha Reddy", dashboard.getCurrentStudent().getName());

        assertTrue(dashboard.getWelcomeTitle().getText().contains("Asha Reddy"));
        assertTrue(dashboard.getWelcomeSubtitle().getText().contains("CS2401"));
        assertTrue(dashboard.getWelcomeSubtitle().getText().contains("CSE"));
        assertTrue(dashboard.getWelcomeSubtitle().getText().contains("Year 2"));

        assertEquals("—", dashboard.getEnrolledCoursesCard().getValue());
        assertEquals("—", dashboard.getPendingAssignmentsCard().getValue());
        assertEquals("—", dashboard.getCompletedAssignmentsCard().getValue());
        assertEquals("—", dashboard.getPublishedGradesCard().getValue());

        assertTrue(dashboard.getTitle().contains("Student Portal"));
        dashboard.dispose();
    }

    @Test
    void dashboardInitializesForStudentWithoutLinkedProfile() {
        // User with no matching record in students table
        User unlinkedStudent = new User(99, "unlinked_student", "hash", Role.STUDENT, true);
        session.login(unlinkedStudent);

        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);

        assertNull(dashboard.getCurrentStudent());
        assertTrue(dashboard.getWelcomeTitle().getText().contains("unlinked_student"));
        assertTrue(dashboard.getWelcomeSubtitle().getText().contains("Student Academic Portal"));

        dashboard.dispose();
    }

    @Test
    void nonStudentRolesAndUnauthenticatedSessionsAreRejected() {
        // 1. ADMIN session rejected
        User admin = new User(1, "admin", "hash", Role.ADMIN, true);
        session.login(admin);
        assertThrows(SecurityException.class,
                () -> new StudentDashboard(stubDAO, authService, session));

        // 2. FACULTY session rejected
        User faculty = new User(2, "faculty1", "hash", Role.FACULTY, true);
        session.login(faculty);
        assertThrows(SecurityException.class,
                () -> new StudentDashboard(stubDAO, authService, session));

        // 3. Unauthenticated session rejected
        session.logout();
        assertThrows(SecurityException.class,
                () -> new StudentDashboard(stubDAO, authService, session));
    }

    @Test
    void navigationButtonsTriggerCardSwitches() {
        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);

        // My Courses
        dashboard.getCoursesNavBtn().doClick();
        assertNotNull(dashboard.getContentPanel());

        // Assignments & Grades
        dashboard.getAssignmentsNavBtn().doClick();
        assertNotNull(dashboard.getContentPanel());

        // My Profile
        dashboard.getProfileNavBtn().doClick();
        assertNotNull(dashboard.getProfilePanel());

        // Overview
        dashboard.getOverviewNavBtn().doClick();
        assertEquals("—", dashboard.getEnrolledCoursesCard().getValue());

        dashboard.dispose();
    }

    @Test
    void logoutClearsSessionDisablesButtonAndDisposesWindow() {
        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);
        assertTrue(session.isLoggedIn());

        dashboard.handleLogout();

        assertFalse(session.isLoggedIn());
        assertFalse(dashboard.isDisplayable());
        assertFalse(dashboard.getLogoutNavBtn().isEnabled());
    }

    @Test
    void inactivePlaceholderPanelsDoNotExecutePrematureQueries() {
        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);

        // Neither findAll nor search should be called during construction or card navigation
        assertFalse(stubDAO.findAllCalled, "StudentDashboard must not call StudentDAO.findAll()");
        assertFalse(stubDAO.searchCalled, "StudentDashboard must not call StudentDAO.search()");

        dashboard.getCoursesNavBtn().doClick();
        assertFalse(stubDAO.findAllCalled);

        dashboard.getAssignmentsNavBtn().doClick();
        assertFalse(stubDAO.findAllCalled);

        dashboard.dispose();
    }

    @Test
    void databaseFailureDuringProfileLookupHandledGracefully() {
        stubDAO.shouldThrowOnFindByUserId = true;

        StudentDashboard dashboard = new StudentDashboard(stubDAO, authService, session);

        assertNull(dashboard.getCurrentStudent());
        assertTrue(dashboard.getWelcomeTitle().getText().contains("student1"));
        assertTrue(dashboard.getWelcomeSubtitle().getText().contains("Student Academic Portal"));

        dashboard.dispose();
    }
}
