

-- Seats left, approvals, rosters, "enrollments of one course with a status"
CREATE INDEX IF NOT EXISTS idx_enrollments_course_status ON enrollments(course_id, status);

-- Admin dashboard counts and the admin enrollments filter by status
CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status);

-- CGPA and "my grades": all grades of one student
CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id);

-- Grade averages / transcript reports join assignments by course
CREATE INDEX IF NOT EXISTS idx_assignments_course ON assignments(course_id);

-- Student search by name. NOCASE lets SQLite use it for  name LIKE 'abc%'  (prefix search).
-- A "contains" search (LIKE '%abc%') can never use any index.
CREATE INDEX IF NOT EXISTS idx_students_name ON students(name COLLATE NOCASE);
