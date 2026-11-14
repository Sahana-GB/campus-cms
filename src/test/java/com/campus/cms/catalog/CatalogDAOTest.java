package com.campus.cms.catalog;

import com.campus.cms.core.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CatalogDAOTest {
    @TempDir Path tempDir;
    private String originalDbPath;
    private FacultyDAO facultyDAO;
    private CourseDAO courseDAO;

    @BeforeEach
    void setUp() throws Exception {
        originalDbPath = Database.getPath();
        Path db = tempDir.resolve("catalog-test.db");
        Database.setPath(db.toString());
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            executeScript(statement, "db/schema.sql");
            executeScript(statement, "db/seed.sql");
            executeScript(statement, "db/indexes.sql");
        }
        facultyDAO = new FacultyDAO();
        courseDAO = new CourseDAO();
    }

    @AfterEach
    void tearDown() {
        Database.setPath(originalDbPath);
    }

    private static void executeScript(Statement statement, String path) throws Exception {
        String script = Files.readString(Path.of(path)).replaceAll("(?m)--[^\\n]*", "");
        for (String sql : script.split(";")) {
            if (!sql.isBlank()) statement.execute(sql);
        }
    }

    @Test
    void facultyCreationLinksLoginAndProfileTransactionally() throws SQLException {
        Faculty faculty = facultyDAO.createFaculty("catalog_faculty", "pass123",
                "Dr. Test", "CSE", "test@campus.edu");

        assertTrue(faculty.getFacultyId() > 0);
        assertTrue(faculty.getUserId() > 0);
        assertEquals("Dr. Test", facultyDAO.findByUserId(faculty.getUserId()).orElseThrow().getName());
        assertEquals("catalog_faculty", facultyDAO.findById(faculty.getFacultyId()).orElseThrow().getUsername());
    }

    @Test
    void duplicateFacultyUsernameDoesNotCreateOrphanProfile() throws SQLException {
        Faculty first = facultyDAO.createFaculty("duplicate_faculty", "pass123",
                "First Faculty", "CSE", null);
        assertThrows(SQLException.class, () -> facultyDAO.createFaculty("duplicate_faculty", "pass456",
                "Second Faculty", "ECE", null));
        assertTrue(facultyDAO.search("Second Faculty", "ALL").isEmpty());
        assertEquals(first.getFacultyId(), facultyDAO.findByUserId(first.getUserId()).orElseThrow().getFacultyId());
    }

    @Test
    void facultyDeactivationUpdatesBothProfileAndLoginAccount() throws SQLException {
        Faculty faculty = facultyDAO.createFaculty("deactivate_faculty", "pass123",
                "Inactive Candidate", "CSE", null);
        assertTrue(facultyDAO.deactivate(faculty.getFacultyId()));
        assertEquals("INACTIVE", facultyDAO.findById(faculty.getFacultyId()).orElseThrow().getStatus());
        assertThrows(IllegalArgumentException.class, () -> courseDAO.createCourse(
                "CS900", "Inactive Faculty Course", 3, 30, "Fall 2026", "X", faculty.getFacultyId()));
    }

    @Test
    void courseCreationSearchAndFacultyAssignmentWork() throws SQLException {
        Faculty faculty = facultyDAO.createFaculty("course_faculty", "pass123",
                "Course Faculty", "CSE", null);
        Course course = courseDAO.createCourse("cs901", "Database Systems", 4, 50,
                "Fall 2026", "LAB", null);

        assertEquals("CS901", course.getCode());
        assertTrue(courseDAO.assignFaculty(course.getCourseId(), faculty.getFacultyId()));
        Course fetched = courseDAO.findByCode("cs901").orElseThrow();
        assertEquals(faculty.getFacultyId(), fetched.getFacultyId());
        assertEquals("Course Faculty", fetched.getFacultyName());
        assertEquals(1, courseDAO.search("database", "ACTIVE", faculty.getFacultyId()).size());
    }

    @Test
    void activeCoursesCannotShareASlotBecauseTheSchemaEnforcesUniqueActiveSlots() throws SQLException {
        courseDAO.createCourse("CS911", "Course One", 3, 40,
                "Fall 2026", "A", null);

        assertThrows(SQLException.class, () -> courseDAO.createCourse(
                "CS912", "Course Two", 3, 40, "Fall 2026", "A", null));
    }

    @Test
    void courseValidationRejectsInvalidValuesAndUnknownFaculty() {
        assertThrows(IllegalArgumentException.class, () -> courseDAO.createCourse(
                "CS920", "Invalid Credits", 0, 20, "Fall 2026", "B", null));
        assertThrows(IllegalArgumentException.class, () -> courseDAO.createCourse(
                "CS921", "Unknown Faculty", 3, 20, "Fall 2026", "C", 999999));
    }
}
