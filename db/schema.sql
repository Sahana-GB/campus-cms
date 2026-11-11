-- ============================================================
-- Campus Course Management System: schema.sql
-- Database: SQLite
-- Scope: single semester (no timetable, no prerequisites)
-- Safe to re-run: drops and recreates every table.
-- ============================================================

PRAGMA foreign_keys = ON;   -- also run this on EVERY Java/Python connection

-- Drop in reverse dependency order
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS faculty;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

-- ------------------------------------------------------------
-- USERS: login and roles (admins only have a row here)
-- password_hash format: "salt:hash" (same algorithm in Java and Python)
-- ------------------------------------------------------------
CREATE TABLE users (
    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('ADMIN', 'FACULTY', 'STUDENT')),
    is_active     INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
);

-- ------------------------------------------------------------
-- STUDENTS
-- CGPA / current average is NOT stored; compute it from grades.
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- FACULTY
-- faculty_id is the unique faculty ID.
-- ------------------------------------------------------------
CREATE TABLE faculty (
    faculty_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL UNIQUE,
    name       TEXT    NOT NULL,
    department TEXT    NOT NULL,
    email      TEXT,
    status     TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ------------------------------------------------------------
-- COURSES
-- faculty_id may be NULL until the admin assigns a faculty member.
-- "Seats left" is computed: capacity - count of APPROVED enrollments.
-- ARCHIVED courses must not accept new enrollments (check in Java).
-- ------------------------------------------------------------
CREATE TABLE courses (
    course_id  INTEGER PRIMARY KEY AUTOINCREMENT,
    code       TEXT    NOT NULL UNIQUE,
    title      TEXT    NOT NULL,
    credits    INTEGER NOT NULL CHECK (credits > 0),
    capacity   INTEGER NOT NULL CHECK (capacity > 0),
    semester   TEXT    NOT NULL,
    faculty_id INTEGER,
    status     TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    FOREIGN KEY (faculty_id) REFERENCES faculty(faculty_id)
);

-- ------------------------------------------------------------
-- ENROLLMENTS
-- Student requests (PENDING) -> admin approves (APPROVED) or rejects.
-- Dropping/removing sets status = 'DROPPED' (row is kept for history).
-- Re-enrolling after a drop: UPDATE the old row back to PENDING.
-- ------------------------------------------------------------
CREATE TABLE enrollments (
    enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id    INTEGER NOT NULL,
    course_id     INTEGER NOT NULL,
    status        TEXT    NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'DROPPED', 'COMPLETED')),
    request_date  TEXT    NOT NULL DEFAULT (date('now')),
    UNIQUE (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id)  REFERENCES courses(course_id)
);

-- ------------------------------------------------------------
-- ASSIGNMENTS (assignments and quizzes)
-- No submission table: "Closed" just marks the assignment as closed.
-- weight = percentage of the course total (e.g. 20 means 20%).
-- Only the faculty assigned to the course may create these (check in Java).
-- ------------------------------------------------------------
CREATE TABLE assignments (
    assignment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id     INTEGER NOT NULL,
    title         TEXT    NOT NULL,
    type          TEXT    NOT NULL DEFAULT 'ASSIGNMENT' CHECK (type IN ('ASSIGNMENT', 'QUIZ')),
    description   TEXT,
    due_date      TEXT    NOT NULL,                 -- ISO format: YYYY-MM-DD
    max_marks     REAL    NOT NULL CHECK (max_marks > 0),
    weight        REAL    NOT NULL DEFAULT 0 CHECK (weight >= 0 AND weight <= 100),
    status        TEXT    NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    material_path TEXT,                             -- optional attachment (stretch feature)
    FOREIGN KEY (course_id) REFERENCES courses(course_id)
);

-- ------------------------------------------------------------
-- GRADES
-- published = 0 -> draft (faculty only), published = 1 -> visible to the student.
-- marks <= assignments.max_marks must be checked in Java
-- (a CHECK constraint cannot compare across tables).
-- Students may only see their own published grades (filter in Java).
-- ------------------------------------------------------------
CREATE TABLE grades (
    grade_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    assignment_id INTEGER NOT NULL,
    student_id    INTEGER NOT NULL,
    marks         REAL    NOT NULL CHECK (marks >= 0),
    feedback      TEXT,
    published     INTEGER NOT NULL DEFAULT 0 CHECK (published IN (0, 1)),
    UNIQUE (assignment_id, student_id),
    FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id),
    FOREIGN KEY (student_id)    REFERENCES students(student_id)
);

-- ------------------------------------------------------------
-- INDEXES: intentionally NOT created yet.
-- Add them later in db/indexes.sql after profiling, e.g.:
--   CREATE INDEX idx_students_name      ON students(name);
--   CREATE INDEX idx_enrollments_course ON enrollments(course_id);
--   CREATE INDEX idx_grades_student     ON grades(student_id);
-- (UNIQUE columns already get an automatic index.)
-- ------------------------------------------------------------
