package com.campus.cms.enrollment;

import com.campus.cms.core.Role;
import com.campus.cms.reports.ReportTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the enrollment period setting and the grade reports.
 * Seed: students 1 and 2; courses 1 = CS101 (slot A), 2 = MA101 (slot B, credits 3).
 * MA101 has a Final (100 marks, weight 100) with PUBLISHED grades 60 (student 1) and 90 (student 2),
 * and a Quiz with a DRAFT grade for student 1 that must be ignored.
 */
class EnrollmentPeriodAndReportsTest {

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
            for (int i = 1; i <= 2; i++) {
                s.execute("INSERT INTO users (username, password_hash, role) VALUES ('s" + i + "','x','STUDENT')");
                s.execute("INSERT INTO students (user_id, roll_number, name, branch, year) "
                        + "VALUES (" + (i + 1) + ", 'R00" + i + "', 'Student " + i + "', 'CSE', 2)");
            }
            s.execute("INSERT INTO courses (code, title, credits, capacity, semester, slot) "
                    + "VALUES ('CS101','Intro',4,5,'S1','A')");
            s.execute("INSERT INTO courses (code, title, credits, capacity, semester, slot) "
                    + "VALUES ('MA101','Maths',3,5,'S1','B')");
            s.execute("INSERT INTO assignments (course_id, title, due_date, max_marks, weight) "
                    + "VALUES (2,'Final','2026-12-01',100,100)");
            s.execute("INSERT INTO assignments (course_id, title, type, due_date, max_marks, weight) "
                    + "VALUES (2,'Quiz','QUIZ','2026-11-20',10,0)");
            s.execute("INSERT INTO grades (assignment_id, student_id, marks, published) VALUES (1,1,60,1)");
            s.execute("INSERT INTO grades (assignment_id, student_id, marks, published) VALUES (1,2,90,1)");
            s.execute("INSERT INTO grades (assignment_id, student_id, marks, published) VALUES (2,1,0,0)");
        }
    }

    private EnrollmentService as(Role role, int studentId) {
        return new EnrollmentService(factory) {
            @Override protected Role currentRole() { return role; }
            @Override protected int currentStudentId() { return studentId; }
        };
    }

    @Test
    void enrollmentIsOpenByDefault() throws Exception {
        assertTrue(as(Role.STUDENT, 1).isEnrollmentOpen());
    }

    @Test
    void requestsAreBlockedWhileClosedAndWorkAgainWhenReopened() throws Exception {
        as(Role.ADMIN, 0).setEnrollmentOpen(false);
        EnrollmentService student = as(Role.STUDENT, 1);
        EnrollmentException e = assertThrows(EnrollmentException.class, () -> student.requestEnrollment(1, 1));
        assertTrue(e.getMessage().toLowerCase().contains("closed"));

        as(Role.ADMIN, 0).setEnrollmentOpen(true);
        assertEquals("PENDING", student.requestEnrollment(1, 1));
    }

    @Test
    void studentCannotChangeThePeriod() {
        EnrollmentService student = as(Role.STUDENT, 1);
        assertThrows(EnrollmentException.class, () -> student.setEnrollmentOpen(false));
    }

    @Test
    void adminCanStillApproveAfterTheWindowCloses() throws Exception {
        as(Role.STUDENT, 1).requestEnrollment(1, 1);
        EnrollmentService admin = as(Role.ADMIN, 0);
        admin.setEnrollmentOpen(false);
        int id = admin.listEnrollments(1, "PENDING", false).get(0).getEnrollmentId();
        admin.approve(id);   // must not throw
        assertEquals(1, admin.countByStatus("APPROVED"));
    }

    @Test
    void gradeAveragesUsePublishedGradesOnly() throws Exception {
        ReportTable t = as(Role.ADMIN, 0).gradeAveragesReport();
        List<Object> ma = t.rows().stream().filter(r -> "MA101".equals(r.get(0))).findFirst().orElseThrow();
        assertEquals(2, ((Number) ma.get(2)).intValue());              // students graded
        assertEquals(75.0, ((Number) ma.get(3)).doubleValue(), 0.01);  // average
        assertEquals(90.0, ((Number) ma.get(4)).doubleValue(), 0.01);  // highest
        assertEquals(60.0, ((Number) ma.get(5)).doubleValue(), 0.01);  // lowest
    }

    @Test
    void transcriptShowsOwnCoursesWithLetterGrade() throws Exception {
        ReportTable t = as(Role.STUDENT, 1).studentTranscriptReport(1);
        assertEquals(1, t.rows().size());
        assertEquals("MA101", t.rows().get(0).get(0));
        assertEquals(60.0, ((Number) t.rows().get(0).get(3)).doubleValue(), 0.01);
        assertEquals("D", t.rows().get(0).get(4));
        assertTrue(t.title().contains("CGPA"));
    }

    @Test
    void studentCannotReadSomeoneElsesTranscript() {
        EnrollmentService student = as(Role.STUDENT, 1);
        assertThrows(EnrollmentException.class, () -> student.studentTranscriptReport(2));
    }

    @Test
    void studentCannotRunTheAdminGradeAverages() {
        EnrollmentService student = as(Role.STUDENT, 1);
        assertThrows(EnrollmentException.class, student::gradeAveragesReport);
    }
}
