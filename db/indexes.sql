-- Starting point for profiling (Person A + Person C). Do NOT apply before profiling/before.txt is recorded.
-- UNIQUE (student_id, course_id) and UNIQUE (assignment_id, student_id) already give automatic indexes
-- that cover lookups by student_id and by assignment_id, so only the gaps are listed here.

-- Seats left, approvals, rosters, "enrollments of one course with a status"
CREATE INDEX IF NOT EXISTS idx_enrollments_course_status ON enrollments(course_id, status);

-- Admin dashboard counts and the admin enrollments filter by status
CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status);

-- CGPA and "my grades": all grades of one student
CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id);

-- Student search by name on the admin Students screen
CREATE INDEX IF NOT EXISTS idx_students_name ON students(name);
