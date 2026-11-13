package com.campus.cms.students;

import com.campus.cms.auth.AuthService;
import com.campus.cms.auth.UserDAO;
import com.campus.cms.core.Database;
import com.campus.cms.core.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudentDAOTest {

    @TempDir
    Path tempDir;

    private StudentDAO studentDAO;
    private UserDAO userDAO;
    private AuthService authService;
    private String originalDbPath;

    @BeforeEach
    void setUp() throws SQLException {
        originalDbPath = Database.getPath();
        Path testDb = tempDir.resolve("student_test_campus.db");
        Database.setPath(testDb.toString());

        // Initialize SQLite schema with users and students tables
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + testDb);
             Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute("""
                CREATE TABLE users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT    NOT NULL UNIQUE,
                    password_hash TEXT    NOT NULL,
                    role          TEXT    NOT NULL CHECK (role IN ('ADMIN', 'FACULTY', 'STUDENT')),
                    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
                );
            """);

            st.execute("""
                CREATE TABLE students (
                    student_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL UNIQUE,
                    roll_number TEXT    NOT NULL UNIQUE,
                    name        TEXT    NOT NULL,
                    branch      TEXT    NOT NULL,
                    year        INTEGER NOT NULL CHECK (year BETWEEN 1 AND 6),
                    email       TEXT,
                    status      TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id)
                );
            """);
        }

        studentDAO = new StudentDAO();
        userDAO = new UserDAO();
        authService = new AuthService(userDAO, Session.getInstance());
        Session.getInstance().logout();
    }

    @AfterEach
    void tearDown() {
        Session.getInstance().logout();
        Database.setPath(originalDbPath);
    }

    @Test
    void createStudentSuccessfullyCreatesUserAndStudentRecords() throws SQLException {
        Student student = studentDAO.createStudent(
                "john_doe", "password123", "CS2401", "John Doe", "CSE", 2, "john@campus.edu"
        );

        assertNotNull(student);
        assertTrue(student.getStudentId() > 0);
        assertTrue(student.getUserId() > 0);
        assertEquals("CS2401", student.getRollNumber());
        assertEquals("John Doe", student.getName());
        assertEquals("CSE", student.getBranch());
        assertEquals(2, student.getYear());
        assertEquals("john@campus.edu", student.getEmail());
        assertEquals("ACTIVE", student.getStatus());
        assertTrue(student.isActive());
        assertEquals("john_doe", student.getUsername());

        // Verify the created student can authenticate through AuthService
        assertTrue(authService.login("john_doe", "password123"));
        assertTrue(Session.getInstance().isLoggedIn());
    }

    @Test
    void createStudentWithDuplicateUsernameRollsBackTransaction() throws SQLException {
        studentDAO.createStudent("student_dup", "pass1", "CS2402", "Student One", "CSE", 1, "s1@campus.edu");

        // Attempt duplicate username
        assertThrows(SQLException.class, () ->
                studentDAO.createStudent("student_dup", "pass2", "CS2403", "Student Two", "CSE", 1, "s2@campus.edu")
        );

        // Verify student record for CS2403 was never created
        assertFalse(studentDAO.findByRollNumber("CS2403").isPresent());
    }

    @Test
    void createStudentWithDuplicateRollNumberRollsBackTransaction() throws SQLException {
        studentDAO.createStudent("user_one", "pass1", "ROLL999", "Student One", "ECE", 3, "one@campus.edu");

        // Attempt duplicate roll number
        assertThrows(SQLException.class, () ->
                studentDAO.createStudent("user_two", "pass2", "ROLL999", "Student Two", "ECE", 3, "two@campus.edu")
        );

        // Verify user_two was rolled back and does not exist in users table
        assertFalse(userDAO.findByUsername("user_two").isPresent());
    }

    @Test
    void findByIdAndFindByRollNumberReturnCorrectStudent() throws SQLException {
        Student created = studentDAO.createStudent("find_me", "pass", "CS2405", "Find Target", "ME", 4, "find@campus.edu");

        Optional<Student> byId = studentDAO.findById(created.getStudentId());
        assertTrue(byId.isPresent());
        assertEquals("CS2405", byId.get().getRollNumber());
        assertEquals("find_me", byId.get().getUsername());

        Optional<Student> byRoll = studentDAO.findByRollNumber("CS2405");
        assertTrue(byRoll.isPresent());
        assertEquals(created.getStudentId(), byRoll.get().getStudentId());

        assertFalse(studentDAO.findById(99999).isPresent());
        assertFalse(studentDAO.findByRollNumber("NONEXISTENT").isPresent());
        assertFalse(studentDAO.findByRollNumber(null).isPresent());
    }

    @Test
    void findByUserIdReturnsMatchingStudent() throws SQLException {
        Student created = studentDAO.createStudent("by_uid", "pass", "CS2406", "User ID Test", "CSE", 2, "uid@campus.edu");

        Optional<Student> byUid = studentDAO.findByUserId(created.getUserId());
        assertTrue(byUid.isPresent());
        assertEquals("CS2406", byUid.get().getRollNumber());

        assertFalse(studentDAO.findByUserId(88888).isPresent());
    }

    @Test
    void findAllReturnsAllStudentsInOrder() throws SQLException {
        studentDAO.createStudent("s1", "pass", "R101", "Alice", "CSE", 1, "a@campus.edu");
        studentDAO.createStudent("s2", "pass", "R102", "Bob", "ECE", 2, "b@campus.edu");
        studentDAO.createStudent("s3", "pass", "R103", "Charlie", "ME", 3, "c@campus.edu");

        List<Student> all = studentDAO.findAll();
        assertEquals(3, all.size());
        assertEquals("R101", all.get(0).getRollNumber());
        assertEquals("R102", all.get(1).getRollNumber());
        assertEquals("R103", all.get(2).getRollNumber());
    }

    @Test
    void searchFiltersByNameRollBranchAndYear() throws SQLException {
        studentDAO.createStudent("asha_u", "pass", "CS2401", "Asha Reddy", "CSE", 2, "asha@campus.edu");
        studentDAO.createStudent("ravi_u", "pass", "CS2402", "Ravi Kumar", "CSE", 3, "ravi@campus.edu");
        studentDAO.createStudent("karthik_u", "pass", "EC2401", "Karthik Rao", "ECE", 2, "karthik@campus.edu");

        // Substring search on name
        List<Student> nameSearch = studentDAO.search("asha", null, null);
        assertEquals(1, nameSearch.size());
        assertEquals("CS2401", nameSearch.get(0).getRollNumber());

        // Substring search on roll number
        List<Student> rollSearch = studentDAO.search("EC24", null, null);
        assertEquals(1, rollSearch.size());
        assertEquals("Karthik Rao", rollSearch.get(0).getName());

        // Filter by branch
        List<Student> branchSearch = studentDAO.search(null, "CSE", null);
        assertEquals(2, branchSearch.size());

        // Filter by year
        List<Student> yearSearch = studentDAO.search(null, null, 2);
        assertEquals(2, yearSearch.size());

        // Combined filter
        List<Student> combined = studentDAO.search("Rao", "ECE", 2);
        assertEquals(1, combined.size());
        assertEquals("EC2401", combined.get(0).getRollNumber());

        // No match
        List<Student> noMatch = studentDAO.search("Nonexistent", "CIVIL", 4);
        assertTrue(noMatch.isEmpty());
    }

    @Test
    void searchHandlesLiteralWildcardsAndEscapeCharacters() throws SQLException {
        // Create students with special characters in roll numbers or names
        studentDAO.createStudent("u_spec1", "pass", "CS_101", "Regular Name", "CSE", 1, "s1@c.edu");
        studentDAO.createStudent("u_spec2", "pass", "CS2101", "Another Name", "CSE", 1, "s2@c.edu");
        studentDAO.createStudent("u_spec3", "pass", "CS%200", "Percent Student", "ECE", 2, "s3@c.edu");
        studentDAO.createStudent("u_spec4", "pass", "CS2400", "Normal Student", "ECE", 2, "s4@c.edu");
        studentDAO.createStudent("u_spec5", "pass", "SLASH\\1", "Backslash\\Student", "ME", 3, "s5@c.edu");

        // 1. Searching for "CS_1" should match ONLY "CS_101", NOT "CS2101"
        List<Student> underscoreSearch = studentDAO.search("CS_1", null, null);
        assertEquals(1, underscoreSearch.size());
        assertEquals("CS_101", underscoreSearch.get(0).getRollNumber());

        // 2. Searching for "CS%" should match ONLY "CS%200", NOT "CS2400"
        List<Student> percentSearch = studentDAO.search("CS%", null, null);
        assertEquals(1, percentSearch.size());
        assertEquals("CS%200", percentSearch.get(0).getRollNumber());

        // 3. Searching for a backslash "\" should match "SLASH\1"
        List<Student> backslashSearch = studentDAO.search("\\", null, null);
        assertEquals(1, backslashSearch.size());
        assertEquals("SLASH\\1", backslashSearch.get(0).getRollNumber());

        // 4. Normal substring searches still work properly
        List<Student> normalSearch = studentDAO.search("Percent", null, null);
        assertEquals(1, normalSearch.size());
        assertEquals("CS%200", normalSearch.get(0).getRollNumber());
    }

    @Test
    void updateSuccessfullyModifiesStudentFields() throws SQLException {
        Student student = studentDAO.createStudent("upd_u", "pass", "CS2407", "Initial Name", "CSE", 1, "init@campus.edu");

        boolean updated = studentDAO.update(student.getStudentId(), "Updated Name", "ECE", 2, "updated@campus.edu");
        assertTrue(updated);

        Student fetched = studentDAO.findById(student.getStudentId()).orElseThrow();
        assertEquals("Updated Name", fetched.getName());
        assertEquals("ECE", fetched.getBranch());
        assertEquals(2, fetched.getYear());
        assertEquals("updated@campus.edu", fetched.getEmail());
    }

    @Test
    void deactivateSetsBothStudentAndUserInactive() throws SQLException {
        Student student = studentDAO.createStudent("deact_u", "password123", "CS2408", "Deact Student", "CSE", 2, "deact@campus.edu");

        // Verify initially active and can log in
        assertTrue(authService.login("deact_u", "password123"));
        authService.logout();

        boolean deactivated = studentDAO.deactivate(student.getStudentId());
        assertTrue(deactivated);

        // Verify student record status is INACTIVE
        Student fetched = studentDAO.findById(student.getStudentId()).orElseThrow();
        assertEquals("INACTIVE", fetched.getStatus());
        assertFalse(fetched.isActive());

        // Verify user account is inactive
        var user = userDAO.findById(student.getUserId()).orElseThrow();
        assertFalse(user.isActive());

        // Verify student can no longer log in
        assertFalse(authService.login("deact_u", "password123"));
        assertFalse(Session.getInstance().isLoggedIn());
    }

    @Test
    void activateSetsBothStudentAndUserActive() throws SQLException {
        Student student = studentDAO.createStudent("act_u", "password123", "CS2409", "Activate Student", "CSE", 2, "act@campus.edu");

        // First deactivate
        assertTrue(studentDAO.deactivate(student.getStudentId()));
        Student deact = studentDAO.findById(student.getStudentId()).orElseThrow();
        assertEquals("INACTIVE", deact.getStatus());
        assertFalse(userDAO.findById(student.getUserId()).orElseThrow().isActive());

        // Now activate
        boolean activated = studentDAO.activate(student.getStudentId());
        assertTrue(activated);

        // Verify student record status is ACTIVE
        Student fetched = studentDAO.findById(student.getStudentId()).orElseThrow();
        assertEquals("ACTIVE", fetched.getStatus());
        assertTrue(fetched.isActive());

        // Verify user account is active
        var user = userDAO.findById(student.getUserId()).orElseThrow();
        assertTrue(user.isActive());

        // Verify student can now log in
        assertTrue(authService.login("act_u", "password123"));
        assertTrue(Session.getInstance().isLoggedIn());
    }

    @Test
    void deactivateAndReactivateCycleRestoresAuthentication() throws SQLException {
        Student student = studentDAO.createStudent("cycle_u", "password123", "CS2410", "Cycle Student", "ECE", 3, "cycle@campus.edu");

        // 1. Initial active state
        assertTrue(authService.login("cycle_u", "password123"));
        authService.logout();

        // 2. Deactivate
        assertTrue(studentDAO.deactivate(student.getStudentId()));
        assertFalse(authService.login("cycle_u", "password123"));

        // 3. Reactivate
        assertTrue(studentDAO.activate(student.getStudentId()));
        assertTrue(authService.login("cycle_u", "password123"));
    }

    @Test
    void activateNonExistentStudentReturnsFalse() throws SQLException {
        assertFalse(studentDAO.activate(999999));
    }

    @Test
    void activateRollsBackOnSqlFailure() throws SQLException {
        Student student = studentDAO.createStudent("rb_u", "password123", "CS2411", "Rollback Student", "ME", 1, "rb@campus.edu");
        assertTrue(studentDAO.deactivate(student.getStudentId()));

        // Create a temporary trigger to cause failure during users table update
        try (Connection con = Database.getConnection();
             Statement st = con.createStatement()) {
            st.execute("CREATE TRIGGER fail_user_update BEFORE UPDATE ON users BEGIN SELECT RAISE(ABORT, 'Simulated failure on user update'); END;");
        }

        try {
            assertThrows(SQLException.class, () -> studentDAO.activate(student.getStudentId()));

            // Verify rollback: student status must still be INACTIVE
            Student fetched = studentDAO.findById(student.getStudentId()).orElseThrow();
            assertEquals("INACTIVE", fetched.getStatus(), "Transaction rollback must keep student INACTIVE on failure");
        } finally {
            try (Connection con = Database.getConnection();
                 Statement st = con.createStatement()) {
                st.execute("DROP TRIGGER IF EXISTS fail_user_update;");
            }
        }
    }

    @Test
    void inputValidationEnforcesConstraints() {
        assertThrows(IllegalArgumentException.class, () ->
                studentDAO.createStudent(null, "p", "R1", "Name", "CSE", 1, "e@c.edu")
        );
        assertThrows(IllegalArgumentException.class, () ->
                studentDAO.createStudent("u", "p", "R1", "Name", "CSE", 0, "e@c.edu") // year < 1
        );
        assertThrows(IllegalArgumentException.class, () ->
                studentDAO.createStudent("u", "p", "R1", "Name", "CSE", 7, "e@c.edu") // year > 6
        );
    }
}
