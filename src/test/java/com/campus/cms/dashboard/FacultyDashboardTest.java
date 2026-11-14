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
import com.campus.cms.grading.AssignmentDAO;
import com.campus.cms.grading.Assignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FacultyDashboardTest {
    private final Session session = Session.getInstance();

    private static class TestableFacultyDashboard extends FacultyDashboard {
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

    private static class StubFacultyDAO extends FacultyDAO {
        Faculty faculty;
        @Override public Optional<Faculty> findByUserId(int userId) {
            return Optional.ofNullable(faculty);
        }
    }

    private static class StubAssignmentDAO extends AssignmentDAO {
        @Override public List<Assignment> getAssignmentsByCourse(int courseId) {
            return List.of();
        }
    }

    private static class StubCourseDAO extends CourseDAO {
        List<Course> courses = List.of(
                new Course(7, "CS701", "Software Engineering", 4, 60,
                        "Fall 2026", "A", 3, "ACTIVE"));
        boolean failSearch;
        @Override public List<Course> search(String query, String status, Integer facultyId)
                throws SQLException {
            if (failSearch) throw new SQLException("simulated database failure");
            return courses.stream().filter(c -> c.getFacultyId() != null
                    && c.getFacultyId().equals(facultyId)).toList();
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        session.logout();
        session.login(new User(12, "faculty1", "hash", Role.FACULTY, true));
    }

    @AfterEach
    void tearDown() {
        session.logout();
    }

    @Test
    void dashboardLoadsLinkedFacultyAndAssignedCourses() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        FacultyDashboard dashboard = new TestableFacultyDashboard(new AuthService(new UserDAO(), session),
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        assertEquals("Dr. Rao", dashboard.getCurrentFaculty().getName());
        assertNull(dashboard.getFacultyProfileIssue());
        assertEquals(1, dashboard.getCoursesTable().getRowCount());
        assertEquals("1", dashboard.getAssignedCoursesCard().getValue());
        assertNotNull(dashboard.getGradesNavBtn());
        dashboard.dispose();
    }

    @Test
    void dashboardExplainsWhenFacultyProfileIsMissing() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        FacultyDashboard dashboard = new TestableFacultyDashboard(new AuthService(new UserDAO(), session),
                session, facultyDAO, new StubCourseDAO(), new StubAssignmentDAO());

        assertNull(dashboard.getCurrentFaculty());
        assertTrue(dashboard.getFacultyProfileIssue().contains("No faculty profile is linked"));
        assertEquals("N/A", dashboard.getAssignedCoursesCard().getValue());
        assertEquals(0, dashboard.getCoursesTable().getRowCount());
        dashboard.dispose();
    }


    @Test
    void refreshRemovesPreviouslySelectedCourseWhenItIsNoLongerAssigned() {
        StubFacultyDAO facultyDAO = new StubFacultyDAO();
        facultyDAO.faculty = new Faculty(3, 12, "Dr. Rao", "CSE",
                "rao@campus.edu", "ACTIVE", "faculty1");
        StubCourseDAO courseDAO = new StubCourseDAO();
        FacultyDashboard dashboard = new TestableFacultyDashboard(new AuthService(new UserDAO(), session),
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        assertNotNull(dashboard.getGradeCourseSelector().getSelectedItem());
        courseDAO.courses = List.of();
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
        FacultyDashboard dashboard = new TestableFacultyDashboard(new AuthService(new UserDAO(), session),
                session, facultyDAO, courseDAO, new StubAssignmentDAO());

        courseDAO.failSearch = true;
        dashboard.refreshCourses();

        assertEquals(0, dashboard.getCoursesTable().getRowCount());
        assertEquals(0, dashboard.getGradeCourseSelector().getItemCount());
        assertNull(dashboard.getGradeCourseSelector().getSelectedItem());
        dashboard.dispose();
    }

    @Test
    void rejectsNonFacultySession() {
        session.logout();
        session.login(new User(13, "student1", "hash", Role.STUDENT, true));
        assertThrows(SecurityException.class, () -> new FacultyDashboard(
                new AuthService(new UserDAO(), session), session,
                new StubFacultyDAO(), new StubCourseDAO(), new StubAssignmentDAO()));
    }
}
