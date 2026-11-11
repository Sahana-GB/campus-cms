package com.campus.cms.enrollment;

import com.campus.cms.core.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs against a temporary SQLite file built from the real db/schema.sql.
 * Needs sqlite-jdbc and junit-jupiter in pom.xml, and must run from the project root.
 *
 * Seed data: students 1, 2, 3 (ACTIVE); courses 1 = CS101 (capacity 1, slot A),
 * 2 = MA101 (capacity 2, slot B), 3 = OLD1 (ARCHIVED).
 * Published grades in MA101: student 1 scored 60, student 2 scored 90 (so student 2 has the higher CGPA).
 */
class EnrollmentServiceTest {

    @TempDir
    Path tmp;

    private ConnectionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        String url = "jdbc:sqlite:" + tmp.resolve("test.db");
        factory = () -> {
            Connection c = DriverManager.getConnection(url);
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            }
            return c;
        };

        String schema = Files.readString(Path.of("db/schema.sql")).replaceAll("--[^\\n]*", "");
        try (Connection c = factory.open(); Statement s = c.createStatement()) {
            for (String sql : schema.split(";")) {
                if (!sql.isBlank()) {
                    s.execute(sql);
                }
            }
            s.execute("INSERT INTO users (username, password_hash, role) VALUES ('admin','x','ADMIN')");
            for (int i = 1; i <= 3; i++) {
                s.execute("INSERT INTO users (username, password_hash, role) VALUES ('s" + i + "','x','STUDENT')");
                s.execute("INSERT INTO students (user_id, roll_number, name, branch, year) "
                        + "VALUES (" + (i + 1) + ", 'R00" + i + "', 'Student " + i + "', 'CSE', 2)");
            }
            s.execute("INSERT INTO courses (code, title, credits, capacity, semester, slot) "
                    + "VALUES ('CS101','Intro',4,1,'S1','A')");
            s.execute("INSERT INTO courses (code, title, credits, capacity, semester, slot) "
                    + "VALUES ('MA101','Maths',3,2,'S1','B')");
            s.execute("INSERT INTO courses (code, title, credits, capacity, semester, slot, status) "
                    + "VALUES ('OLD1','Old',2,5,'S1','C','ARCHIVED')");
            s.execute("INSERT INTO assignments (course_id, title, due_date, max_marks, weight) "
                    + "VALUES (2,'Final','2026-12-01',100,100)");
            s.execute("INSERT INTO grades (assignment_id, student_id, marks, published) VALUES (1,1,60,1)");
            s.execute("INSERT INTO grades (assignment_id, student_id, marks, published) VALUES (1,2,90,1)");
        }
    }

    /** A service that behaves as if this user is logged in. */
    private EnrollmentService as(Role role, int studentId) {
        return new EnrollmentService(factory) {
            @Override protected Role currentRole() { return role; }
            @Override protected int currentStudentId() { return studentId; }
        };
    }

    private int enrollmentId(int studentId, int courseId) throws SQLException {
        try (Connection c = factory.open(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT enrollment_id FROM enrollments WHERE student_id = "
                     + studentId + " AND course_id = " + courseId)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private String status(int enrollmentId) throws SQLException {
        try (Connection c = factory.open(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT status FROM enrollments WHERE enrollment_id = " + enrollmentId)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void requestCreatesPendingRow() throws Exception {
        assertEquals("PENDING", as(Role.STUDENT, 1).requestEnrollment(1, 1));
        assertEquals("PENDING", status(enrollmentId(1, 1)));
    }

    @Test
    void duplicateRequestIsRefused() throws Exception {
        EnrollmentService student = as(Role.STUDENT, 1);
        student.requestEnrollment(1, 1);
        EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.requestEnrollment(1, 1));
        assertTrue(e.getMessage().contains("already"));
    }

    @Test
    void archivedCourseIsRefused() {
        EnrollmentException e = assertThrows(EnrollmentException.class,
                () -> as(Role.STUDENT, 1).requestEnrollment(1, 3));
        assertTrue(e.getMessage().contains("archived"));
    }

    @Test
    void reEnrollAfterDropReusesTheSameRow() throws Exception {
        EnrollmentService student = as(Role.STUDENT, 1);
        student.requestEnrollment(1, 1);
        int id = enrollmentId(1, 1);
        student.drop(id);
        assertEquals("DROPPED", status(id));          // row is kept as history

        assertEquals("PENDING", student.requestEnrollment(1, 1));
        assertEquals(id, enrollmentId(1, 1));         // same row, not a new one
        assertEquals("PENDING", status(id));
    }

    @Test
    void approveIsRefusedWhenCourseIsFull() throws Exception {
        as(Role.STUDENT, 1).requestEnrollment(1, 1);
        as(Role.STUDENT, 2).requestEnrollment(2, 1);
        EnrollmentService admin = as(Role.ADMIN, 0);

        admin.approve(enrollmentId(1, 1));            // takes the only seat
        EnrollmentException e = assertThrows(EnrollmentException.class,
                () -> admin.approve(enrollmentId(2, 1)));
        assertTrue(e.getMessage().contains("full"));
        assertEquals(0, admin.seatsLeft(1));
    }

    @Test
    void studentCannotDropSomeoneElsesEnrollment() throws Exception {
        as(Role.STUDENT, 1).requestEnrollment(1, 1);
        int id = enrollmentId(1, 1);
        assertThrows(EnrollmentException.class, () -> as(Role.STUDENT, 2).drop(id));
        assertEquals("PENDING", status(id));
    }

    @Test
    void studentCannotApprove() throws Exception {
        as(Role.STUDENT, 1).requestEnrollment(1, 1);
        int id = enrollmentId(1, 1);
        assertThrows(EnrollmentException.class, () -> as(Role.STUDENT, 1).approve(id));
    }

    @Test
    void studentCannotActForAnotherStudent() {
        assertThrows(EnrollmentException.class, () -> as(Role.STUDENT, 1).requestEnrollment(2, 1));
    }

    @Test
    void autoAllocateGivesTheSeatToTheHigherCgpa() throws Exception {
        as(Role.STUDENT, 1).requestEnrollment(1, 1);   // CGPA 6.0
        as(Role.STUDENT, 2).requestEnrollment(2, 1);   // CGPA 9.0
        as(Role.STUDENT, 3).requestEnrollment(3, 1);   // no grades, CGPA 0.0

        EnrollmentService.AllocationResult result = as(Role.ADMIN, 0).autoAllocate(1);

        assertEquals(1, result.approved());
        assertEquals(2, result.stillPending());
        assertEquals("APPROVED", status(enrollmentId(2, 1)));
        assertEquals("PENDING", status(enrollmentId(1, 1)));
        assertEquals("PENDING", status(enrollmentId(3, 1)));
    }

    @Test
    void cgpaUsesPublishedGradesOnly() throws Exception {
        assertEquals(9.0, as(Role.ADMIN, 0).cgpa(2));
        assertEquals(6.0, as(Role.ADMIN, 0).cgpa(1));
        assertEquals(0.0, as(Role.ADMIN, 0).cgpa(3));
    }
}
