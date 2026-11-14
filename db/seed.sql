-- ============================================================
-- seed.sql: small demo data for the Campus CMS
-- Run AFTER schema.sql.  Re-runnable together with schema.sql.
--
-- Demo logins:
--   admin     / admin123
--   faculty1  / pass123
--   faculty2  / pass123
--   student1..student5 / pass123
--
-- password_hash format: salt_hex:hash_hex
--   PBKDF2-HMAC-SHA256, 10000 iterations, 16-byte salt, 32-byte hash.
--   Java and Python must both use exactly this.
-- ============================================================

PRAGMA foreign_keys = ON;

-- ---------- USERS (id, username, password_hash, role, is_active) ----------
INSERT INTO users (user_id, username, password_hash, role, is_active) VALUES
    (1, 'admin', '680059c1c3c12513809e6b58bfb2cf39:57733272c2c500916e2f8c95996a21f11bd14bd0620223c521025b972a352def', 'ADMIN', 1),
    (2, 'faculty1', 'b7dd97a5ead33b2174b7bbbceeefd9ca:366ae88eb603fd972fde1446243878e16cca806205a97d6060dd2cd9043138d7', 'FACULTY', 1),
    (3, 'faculty2', '036a2cfea24219cc88aff11120e34fb2:260cd49f91695bf329508d7282df697cb60ff27c1e75ead7d99748d6bbeb04c4', 'FACULTY', 1),
    (4, 'student1', '9b6305a4c0bb1f84e2dd188b403f24b5:fa156d835a1e289c3cc0fb1ed74800d9fba917c06737614f113fc2af05375501', 'STUDENT', 1),
    (5, 'student2', 'ca0f2132f89ce46f8da8e7e414574790:54ca86458e08055a69d2146e524ecc54b748244b205cc2a80b49bb0e8fd76489', 'STUDENT', 1),
    (6, 'student3', 'a86de1904c6edbae7b305de40be48db8:3e5ea36ab751e0c9fe0966f464044cacf6d15ea9e3a71a3ae7fe3a55bf208fce', 'STUDENT', 1),
    (7, 'student4', '4ce3bd491bf791f0e58f537073351267:d34ab620093d0b3300be2324974f143872e8b089e791f59e44a00f9930e37d11', 'STUDENT', 1),
    (8, 'student5', 'a4f0d7d4fb06a624f18cc6b3fcc07795:27496aad7489fe9df9178870700cf83468f72d32d8015529a893567be794e9e9', 'STUDENT', 1);

-- ---------- FACULTY (admin has no row here) ----------
INSERT INTO faculty (faculty_id, user_id, name, department, email, status) VALUES
    (1, 2, 'Dr. Lakshmi Narayan', 'Computer Science', 'lakshmi@campus.edu', 'ACTIVE'),
    (2, 3, 'Dr. Suresh Babu',     'Computer Science', 'suresh@campus.edu',  'ACTIVE');

-- ---------- STUDENTS ----------
INSERT INTO students (student_id, user_id, roll_number, name, branch, year, email, status) VALUES
    (1, 4, 'CS2401', 'Asha Reddy',  'CSE', 2, 'asha@campus.edu',  'ACTIVE'),
    (2, 5, 'CS2402', 'Ravi Kumar',  'CSE', 2, 'ravi@campus.edu',  'ACTIVE'),
    (3, 6, 'CS2403', 'Meena Iyer',  'CSE', 2, 'meena@campus.edu', 'ACTIVE'),
    (4, 7, 'EC2401', 'Karthik Rao', 'ECE', 2, 'karthik@campus.edu','ACTIVE'),
    (5, 8, 'EC2402', 'Divya Nair',  'ECE', 3, 'divya@campus.edu', 'ACTIVE');

-- ---------- COURSES ----------
INSERT INTO courses (course_id, code, title, credits, capacity, semester, slot, faculty_id, status) VALUES
    (1, 'CS101', 'Intro to Programming', 4, 60, 'Fall 2026', 'B', 1, 'ACTIVE'),
    (2, 'CS201', 'Data Structures',      3, 50, 'Fall 2026', 'C', 2, 'ACTIVE'),
    (3, 'CS301', 'Databases',            3,  3, 'Fall 2026', 'D', 1, 'ACTIVE');
-- Every course has its own slot (B, C, D), so none of them clash.
-- CS301 has capacity 3 on purpose, so you can demo the "Course is full" error.

-- ---------- ENROLLMENTS (student_id, course_id, status) ----------
INSERT INTO enrollments (student_id, course_id, status, request_date) VALUES
    (1, 1, 'APPROVED', '2026-08-01'),
    (2, 1, 'APPROVED', '2026-08-01'),
    (3, 1, 'APPROVED', '2026-08-02'),
    (1, 2, 'APPROVED', '2026-08-02'),
    (2, 2, 'APPROVED', '2026-08-03'),
    (4, 2, 'PENDING',  '2026-08-04'),   -- waiting for admin approval
    (5, 1, 'DROPPED',  '2026-08-01'),   -- student dropped this course
    (1, 3, 'APPROVED', '2026-08-03'),
    (2, 3, 'APPROVED', '2026-08-03'),
    (3, 3, 'APPROVED', '2026-08-04');   -- CS301 is now full (3/3)

-- ---------- ASSIGNMENTS ----------
INSERT INTO assignments (assignment_id, course_id, title, type, description, due_date, max_marks, weight, status) VALUES
    (1, 1, 'Assignment 1: Loops',  'ASSIGNMENT', 'Solve 5 loop problems.',      '2026-09-10', 20, 20, 'CLOSED'),
    (2, 1, 'Quiz 1',               'QUIZ',       'Basics of Java syntax.',      '2026-09-25', 10, 10, 'OPEN'),
    (3, 2, 'Assignment 1: Arrays', 'ASSIGNMENT', 'Implement array operations.', '2026-09-15', 25, 25, 'OPEN'),
    (4, 3, 'ER Diagram Task',      'ASSIGNMENT', 'Design an ER diagram.',       '2026-10-01', 30, 30, 'OPEN');

-- ---------- GRADES (published = 1 visible to student, 0 = draft) ----------
INSERT INTO grades (assignment_id, student_id, marks, feedback, published) VALUES
    (1, 1, 18, 'Good work.',            1),
    (1, 2, 15, 'Check loop conditions.', 1),
    (1, 3, 12, 'Needs more practice.',   1),
    (2, 1,  8, NULL,                     0),   -- draft, not yet published
    (2, 2,  6, NULL,                     0),
    (3, 1, 22, 'Nice solution.',         1),
    (3, 2, 19, NULL,                     1);