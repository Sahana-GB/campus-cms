#!/usr/bin/env python3
"""Adds courses, assignments, enrollments and grades on top of EXISTING students for profiling.
Run it AFTER python/generate_data.py. It only ADDS rows (course codes PC001...); nothing existing is changed.

  bash scripts/setup.sh
  python3 python/generate_data.py --count 10000 --seed 42
  python3 python/generate_enrollment_data.py                 # 40 courses, ~5 requests per student
  python3 python/generate_enrollment_data.py --courses 60 --per-student 6
"""
import argparse
import random
import sqlite3
import sys
from pathlib import Path

DEFAULT_DB = Path(__file__).resolve().parent.parent / "data" / "campus.db"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default=str(DEFAULT_DB))
    ap.add_argument("--courses", type=int, default=40)
    ap.add_argument("--per-student", type=int, default=5, help="course requests per student")
    ap.add_argument("--seed", type=int, default=42)
    a = ap.parse_args()

    if not Path(a.db).exists():
        sys.exit(f"Database not found: {a.db}")
    rng = random.Random(a.seed)
    c = sqlite3.connect(a.db)
    c.execute("PRAGMA foreign_keys = ON")
    if c.execute("SELECT COUNT(*) FROM courses WHERE code LIKE 'PC%'").fetchone()[0]:
        sys.exit("PC courses already exist. Run: bash scripts/setup.sh and load the data again.")

    students = [r[0] for r in c.execute("SELECT student_id FROM students WHERE status = 'ACTIVE'")]
    if not students:
        sys.exit("No students found. Run python/generate_data.py first.")
    faculty = [r[0] for r in c.execute("SELECT faculty_id FROM faculty WHERE status = 'ACTIVE'")] or [None]
    used_slots = {r[0] for r in c.execute("SELECT slot FROM courses WHERE status = 'ACTIVE'")}

    # capacity: roughly 1.3x the expected approved seats, so some courses fill up and others stay open
    per_course = len(students) * a.per_student / a.courses
    capacity = int(per_course * 0.7 * 1.3) + 1

    course_ids, assignments = [], {}
    for k in range(1, a.courses + 1):
        slot = f"PS{k:03d}"
        assert slot not in used_slots
        cid = c.execute(
            "INSERT INTO courses(code,title,credits,capacity,semester,slot,faculty_id) VALUES(?,?,?,?,?,?,?)",
            (f"PC{k:03d}", f"Profiling Course {k}", rng.randint(2, 4), capacity, "S1", slot,
             rng.choice(faculty))).lastrowid
        course_ids.append(cid)
        mid = c.execute("INSERT INTO assignments(course_id,title,type,due_date,max_marks,weight,status) "
                        "VALUES(?,?,'ASSIGNMENT','2026-10-01',50,40,'CLOSED')", (cid, "Midterm")).lastrowid
        fin = c.execute("INSERT INTO assignments(course_id,title,type,due_date,max_marks,weight,status) "
                        "VALUES(?,?,'ASSIGNMENT','2026-12-01',100,60,'CLOSED')", (cid, "Final")).lastrowid
        assignments[cid] = ((mid, 50), (fin, 100))

    enr_rows, grade_rows = [], []
    for sid in students:
        for cid in rng.sample(course_ids, min(a.per_student, len(course_ids))):
            status = rng.choices(["APPROVED", "PENDING", "REJECTED", "DROPPED"], [70, 15, 8, 7])[0]
            enr_rows.append((sid, cid, status))
            if status == "APPROVED":
                for aid, mx in assignments[cid]:
                    marks = round(min(mx, max(0.0, rng.gauss(0.68, 0.15) * mx)), 1)
                    grade_rows.append((aid, sid, marks, 1 if rng.random() < 0.9 else 0))
    c.executemany("INSERT INTO enrollments(student_id,course_id,status) VALUES(?,?,?)", enr_rows)
    c.executemany("INSERT INTO grades(assignment_id,student_id,marks,published) VALUES(?,?,?,?)", grade_rows)
    c.commit()
    over = c.execute("SELECT COUNT(*) FROM (SELECT course_id FROM enrollments WHERE status='APPROVED' "
                     "GROUP BY course_id HAVING COUNT(*) > (SELECT capacity FROM courses c2 "
                     "WHERE c2.course_id = enrollments.course_id))").fetchone()[0]
    print(f"Added {a.courses} courses, {len(enr_rows)} enrollments, {len(grade_rows)} grades "
          f"for {len(students)} students (capacity per course: {capacity}; courses over capacity: {over}).")


if __name__ == "__main__":
    main()
