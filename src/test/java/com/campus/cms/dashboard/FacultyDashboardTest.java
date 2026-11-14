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

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FacultyDashboardTest {

    private final Session session = Session.getInstance();
    private AuthService authService;

    static class TestableFacultyDashboard extends FacultyDashboard {
        TestableFacultyDashboard(AuthService authService, Session session,
                                 FacultyDAO facultyDAO, CourseDAO courseDAO,
                                 AssignmentDAO assignmentDAO) {
            super(authService, session, facultyDAO, courseDAO, assignmentDAO);
        }

        @Override
        protected JComponent createGradesPanelForCourse(int courseId) {
            return new JPanel();
        }
    }

    static class StubFacultyDAO extends FacultyDAO {
        Faculty faculty;

        @Override
        public Optional<Faculty> findByUserId(int userId) {
            return Optional.ofNullable(faculty);
        }
    }

    static class StubCourseDAO extends CourseDAO {
        List<Course> courses = new ArrayList<>();
        boolean failSearch;

        @Override
        public List<Course> search(String query, String status, Integer facultyId) throws SQLException {
            if (failSearch) throw new SQLException("simulated database failure");
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

    static class StubAssignmentDAO extends AssignmentDAO {
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
        session.logout();
        session.login(new User(12, "faculty1", "hash", Role.FACULTY, true));
        authService = new AuthService(new UserDAO(), session);
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void rejectsNonFacultySessionAndUnauthorizedRoles() {
        session.logout();
        assertThrows(SecurityException.class, () -> new TestableFacultyDashboard(
                authService, session, new StubFacultyDAO(), new StubCourseDAO(), new StubAssignmentDAO()));

        session.login(new User(13, "student1", "hash", Role.STUDENT, true));
        assertThrows(SecurityException.class, () -> new TestableFacultyDashboard(
                authService, session, new StubFacultyDAO(), new StubCourseDAO(), new StubAssignmentDAO()));

        session.login(new User(1, "admin1", "hash", Role.ADMIN, true));
        assertThrows(SecurityException.class, () -> new TestableFacultyDashboard(
                authService, session, new StubFacultyDAO(), new StubCourseDAO(), new StubAssignmentDAO()));
    }

    @Test
    void dashboardLoadsLinkedFacultyAndAssignedCourses() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        courseDAO.courses.add(new Course(7, "CS701", "Software Engineering", 4, 60,
                "Fall 2026", "A", 3, "ACTIVE"));
        courseDAO.courses.add(new Course(8, "CS702", "Cloud Systems", 3, 50,
                "Fall 2026", "B", 3, "ARCHIVED"));

        StubAssignmentDAO assignmentDAO = new StubAssignmentDAO();
        assignmentDAO.assignments.add(new Assignment(1, 7, "HW1", "ASSIGNMENT", "Desc", "2026-11-20", 100, 10, "OPEN", ""));
        assignmentDAO.assignments.add(new Assignment(2, 7, "Quiz 1", "QUIZ", "Quiz", "2026-11-25", 50, 5, "OPEN", ""));

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, courseDAO, assignmentDAO);

        assertEquals("Dr. Rao", dashboard.getCurrentFaculty().getName());
        assertNull(dashboard.getFacultyProfileIssue());
        assertEquals(2, dashboard.getCoursesTable().getRowCount());
        assertEquals("2", dashboard.getAssignedCoursesCard().getValue());
        assertEquals("1", dashboard.getActiveCoursesCard().getValue());
        assertEquals("2", dashboard.getAssignmentsCard().getValue());
        assertNotNull(dashboard.getGradesNavBtn());
        assertTrue(dashboard.getTitle().contains("Faculty Portal"));
        dashboard.dispose();
    }

    @Test
    void dashboardExplainsWhenFacultyProfileIsMissing() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, new StubCourseDAO(), new StubAssignmentDAO());

        assertNull(dashboard.getCurrentFaculty());
        assertTrue(dashboard.getFacultyProfileIssue().contains("No faculty profile is linked"));
        assertEquals("N/A", dashboard.getAssignedCoursesCard().getValue());
        assertEquals(0, dashboard.getCoursesTable().getRowCount());
        dashboard.dispose();
    }

    @Test
    void navigationButtonsTriggerCardSwitches() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        courseDAO.courses.add(new Course(7, "CS701", "Software Engineering", 4, 60,
                "Fall 2026", "A", 3, "ACTIVE"));

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        dashboard.getCoursesNavBtn().doClick();
        assertEquals(1, dashboard.getCoursesTable().getRowCount());

        dashboard.getGradesNavBtn().doClick();
        assertEquals(1, dashboard.getGradeCourseSelector().getItemCount());

        dashboard.getProfileNavBtn().doClick();
        assertNotNull(dashboard.getContentPanel());

        dashboard.getOverviewNavBtn().doClick();
        assertEquals("1", dashboard.getAssignedCoursesCard().getValue());

        dashboard.dispose();
    }

    @Test
    void coursesTablePopulatesCorrectData() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        courseDAO.courses.add(new Course(7, "CS701", "Software Engineering", 4, 60,
                "Fall 2026", "A", 3, "ACTIVE"));

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        assertEquals(1, dashboard.getCoursesTable().getRowCount());
        assertEquals("CS701", dashboard.getCoursesTable().getValueAt(0, 1));
        assertEquals("Software Engineering", dashboard.getCoursesTable().getValueAt(0, 2));
        assertEquals("ACTIVE", dashboard.getCoursesTable().getValueAt(0, 6));

        dashboard.dispose();
    }

    @Test
    void refreshRemovesPreviouslySelectedCourseWhenItIsNoLongerAssigned() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        courseDAO.courses.add(new Course(7, "CS701", "Software Engineering", 4, 60,
                "Fall 2026", "A", 3, "ACTIVE"));

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        assertNotNull(dashboard.getGradeCourseSelector().getSelectedItem());
        courseDAO.courses.clear();
        dashboard.refreshCourses();

        assertEquals(0, dashboard.getGradeCourseSelector().getItemCount());
        assertNull(dashboard.getGradeCourseSelector().getSelectedItem());
        dashboard.dispose();
    }

    @Test
    void failedCourseRefreshClearsOldCourseSelection() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        courseDAO.courses.add(new Course(7, "CS701", "Software Engineering", 4, 60,
                "Fall 2026", "A", 3, "ACTIVE"));

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        courseDAO.failSearch = true;
        dashboard.refreshCourses();

        assertEquals(0, dashboard.getCoursesTable().getRowCount());
        assertEquals(0, dashboard.getGradeCourseSelector().getItemCount());
        assertNull(dashboard.getGradeCourseSelector().getSelectedItem());
        dashboard.dispose();
    }

    @Test
    void logoutClearsSessionAndDisposes() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");

        FacultyDashboard dashboard = new TestableFacultyDashboard(authService,
                session, facultyDAO, new StubCourseDAO(), new StubAssignmentDAO());
        assertTrue(session.isLoggedIn());

        dashboard.handleLogout();

        assertFalse(session.isLoggedIn());
        assertFalse(dashboard.getLogoutNavBtn().isEnabled());
    }
}
