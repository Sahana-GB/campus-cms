package com.campus.cms.dashboard;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.User;
import com.campus.cms.auth.UserDAO;
import com.campus.cms.catalog.Course;
import com.campus.cms.catalog.CourseDAO;
import com.campus.cms.catalog.Faculty;
import com.campus.cms.catalog.FacultyDAO;
import com.campus.cms.core.Role;
import com.campus.cms.core.Session;
import com.campus.cms.grading.Assignment;
import com.campus.cms.grading.AssignmentDAO;
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

class FacultyDashboardTest {

    private Session session;
    private AuthService authService;
    private StubFacultyDAO stubFacultyDAO;
    private StubCourseDAO stubCourseDAO;
    private StubAssignmentDAO stubAssignmentDAO;

    private static class StubFacultyDAO extends FacultyDAO {
        Faculty faculty;

        @Override
        public Optional<Faculty> findByUserId(int userId) {
            if (faculty != null && faculty.getUserId() == userId) {
                return Optional.of(faculty);
            }
            return Optional.empty();
        }
    }

    private static class StubCourseDAO extends CourseDAO {
        final List<Course> courses = new ArrayList<>();

        @Override
        public List<Course> search(String query, String status, Integer facultyId) {
            List<Course> result = new ArrayList<>();
            for (Course c : courses) {
                if (facultyId != null && !facultyId.equals(c.getFacultyId())) {
                    continue;
                }
                if (status != null && !"ALL".equalsIgnoreCase(status) && !status.equalsIgnoreCase(c.getStatus())) {
                    continue;
                }
                result.add(c);
            }
            return result;
        }
    }

    private static class StubAssignmentDAO extends AssignmentDAO {
        final List<Assignment> assignments = new ArrayList<>();

        @Override
        public List<Assignment> getAssignmentsByCourse(int courseId) {
            List<Assignment> result = new ArrayList<>();
            for (Assignment a : assignments) {
                if (a.getCourseId() == courseId) {
                    result.add(a);
                }
            }
            return result;
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        session = Session.getInstance();
        session.logout();

        User facultyUser = new User(10, "prof_sharma", "hash", Role.FACULTY, true);
        session.login(facultyUser);

        stubFacultyDAO = new StubFacultyDAO();
        stubFacultyDAO.faculty = new Faculty(101, 10, "Dr. Sharma", "CSE", "sharma@campus.edu", "ACTIVE", "prof_sharma");

        stubCourseDAO = new StubCourseDAO();
        stubCourseDAO.courses.add(new Course(1, "CS101", "Intro to CS", 4, 60, "Mon/Wed", "A1", 101, Course.ACTIVE));
        stubCourseDAO.courses.add(new Course(2, "CS301", "Algorithms", 3, 50, "Tue/Thu", "B1", 101, Course.ARCHIVED));

        stubAssignmentDAO = new StubAssignmentDAO();
        stubAssignmentDAO.assignments.add(new Assignment(1, 1, "HW1", "ASSIGNMENT", "Basic", "2026-11-20", 100, 10, "OPEN", ""));
        stubAssignmentDAO.assignments.add(new Assignment(2, 1, "Quiz 1", "QUIZ", "Quiz 1", "2026-11-25", 50, 5, "OPEN", ""));
        stubAssignmentDAO.assignments.add(new Assignment(3, 2, "HW2", "ASSIGNMENT", "Trees", "2026-12-01", 100, 10, "CLOSED", ""));

        authService = new AuthService(new UserDAO(), session);
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void dashboardRejectsUnauthorizedRoles() {
        session.logout();
        assertThrows(SecurityException.class, () ->
                new FacultyDashboard(authService, session, stubFacultyDAO, stubCourseDAO, stubAssignmentDAO));

        User student = new User(2, "student_user", "hash", Role.STUDENT, true);
        session.login(student);
        assertThrows(SecurityException.class, () ->
                new FacultyDashboard(authService, session, stubFacultyDAO, stubCourseDAO, stubAssignmentDAO));

        User admin = new User(3, "admin_user", "hash", Role.ADMIN, true);
        session.login(admin);
        assertThrows(SecurityException.class, () ->
                new FacultyDashboard(authService, session, stubFacultyDAO, stubCourseDAO, stubAssignmentDAO));
    }

    @Test
    void dashboardInitializesAndCalculatesKPIs() {
        FacultyDashboard dashboard = new FacultyDashboard(authService, session,
                stubFacultyDAO, stubCourseDAO, stubAssignmentDAO);

        assertNotNull(dashboard.getCurrentFaculty());
        assertEquals(101, dashboard.getCurrentFaculty().getFacultyId());
        assertEquals("Dr. Sharma", dashboard.getCurrentFaculty().getName());

        assertEquals("2", dashboard.getAssignedCoursesCard().getValue());
        assertEquals("1", dashboard.getActiveCoursesCard().getValue());
        assertEquals("3", dashboard.getAssignmentsCard().getValue());

        assertTrue(dashboard.getTitle().contains("Faculty Portal"));
        dashboard.dispose();
    }

    @Test
    void navigationButtonsTriggerCardSwitches() {
        FacultyDashboard dashboard = new FacultyDashboard(authService, session,
                stubFacultyDAO, stubCourseDAO, stubAssignmentDAO);

        dashboard.getCoursesNavBtn().doClick();
        assertEquals(2, dashboard.getCoursesTable().getRowCount());

        dashboard.getGradesNavBtn().doClick();
        assertEquals(2, dashboard.getGradeCourseSelector().getItemCount());

        dashboard.getProfileNavBtn().doClick();
        assertNotNull(dashboard.getContentPanel());

        dashboard.getOverviewNavBtn().doClick();
        assertEquals("2", dashboard.getAssignedCoursesCard().getValue());

        dashboard.dispose();
    }

    @Test
    void coursesTablePopulatesCorrectData() {
        FacultyDashboard dashboard = new FacultyDashboard(authService, session,
                stubFacultyDAO, stubCourseDAO, stubAssignmentDAO);

        assertEquals(2, dashboard.getCoursesTable().getRowCount());
        assertEquals("CS101", dashboard.getCoursesTable().getValueAt(0, 1));
        assertEquals("ACTIVE", dashboard.getCoursesTable().getValueAt(0, 6));

        assertEquals("CS301", dashboard.getCoursesTable().getValueAt(1, 1));
        assertEquals("ARCHIVED", dashboard.getCoursesTable().getValueAt(1, 6));

        dashboard.dispose();
    }

    @Test
    void logoutClearsSessionAndDisposes() {
        FacultyDashboard dashboard = new FacultyDashboard(authService, session,
                stubFacultyDAO, stubCourseDAO, stubAssignmentDAO);
        assertTrue(session.isLoggedIn());

        dashboard.handleLogout();

        assertFalse(session.isLoggedIn());
        assertFalse(dashboard.getLogoutNavBtn().isEnabled());
    }
}
