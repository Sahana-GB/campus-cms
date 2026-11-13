#!/usr/bin/env python3
"""Times the queries the Java app runs most, on a big database (Person C + Person A).

  python3 python/profile_queries.py                       # timings + query plans
  python3 python/profile_queries.py --drop-indexes        # remove the profiling indexes (for a fair "before")
  python3 python/profile_queries.py --apply-sql db/indexes.sql
  python3 python/profile_queries.py --cprofile            # also show where Python spends time

The SQL below is copied from EnrollmentDAO / CgpaCalculator. If you change the Java SQL, change it here too.
"""
import argparse
import cProfile
import pstats
import random
import sqlite3
import statistics
import sys
import time
from pathlib import Path

DEFAULT_DB = Path(__file__).resolve().parent.parent / "data" / "campus.db"

# Keep in sync with db/indexes.sql
INDEX_NAMES = ["idx_enrollments_course_status", "idx_enrollments_status", "idx_grades_student",
               "idx_assignments_course", "idx_students_name"]

PCT = ("CASE WHEN SUM(a.weight) > 0 "
       "THEN SUM(g.marks * 1.0 / a.max_marks * a.weight) / SUM(a.weight) * 100.0 "
       "ELSE AVG(g.marks * 1.0 / a.max_marks) * 100.0 END")

JOINED = ("SELECT e.enrollment_id, e.student_id, e.course_id, e.status, e.request_date, "
          "s.roll_number, s.name AS student_name, c.code, c.title, c.slot, f.name AS faculty_name "
          "FROM enrollments e JOIN students s ON s.student_id = e.student_id "
          "JOIN courses c ON c.course_id = e.course_id "
          "LEFT JOIN faculty f ON f.faculty_id = c.faculty_id ")

# name -> (sql, parameter factory taking (rng, ids))
QUERIES = {
    "seats taken (one course)": (
        "SELECT COUNT(*) FROM enrollments WHERE course_id = ? AND status = 'APPROVED'",
        lambda r, ids: (r.choice(ids["courses"]),)),
    "admin card: count by status": (
        "SELECT COUNT(*) FROM enrollments WHERE status = ?",
        lambda r, ids: ("PENDING",)),
    "admin table: one course + PENDING": (
        JOINED + "WHERE 1 = 1 AND e.course_id = ? AND e.status = ? ORDER BY c.code, s.roll_number",
        lambda r, ids: (r.choice(ids["courses"]), "PENDING")),
    "admin table: all PENDING": (
        JOINED + "WHERE 1 = 1 AND e.status = ? ORDER BY c.code, s.roll_number",
        lambda r, ids: ("PENDING",)),
    "student: my enrollments": (
        JOINED + "WHERE e.student_id = ? ORDER BY e.request_date DESC, e.enrollment_id DESC",
        lambda r, ids: (r.choice(ids["students"]),)),
    "student: browse courses": (
        "SELECT c.course_id, c.code, c.title, c.credits, c.capacity, c.slot, "
        "COALESCE(f.name, '(not assigned)') AS faculty_name, "
        "(SELECT COUNT(*) FROM enrollments a WHERE a.course_id = c.course_id AND a.status = 'APPROVED') AS taken, "
        "(SELECT e.status FROM enrollments e WHERE e.course_id = c.course_id AND e.student_id = ?) AS my_status "
        "FROM courses c LEFT JOIN faculty f ON f.faculty_id = c.faculty_id "
        "WHERE c.status = 'ACTIVE' ORDER BY c.code",
        lambda r, ids: (r.choice(ids["students"]),)),
    "CGPA: grades of one student": (
        "SELECT a.course_id, co.credits, a.max_marks, a.weight, g.marks FROM grades g "
        "JOIN assignments a ON a.assignment_id = g.assignment_id "
        "JOIN courses co ON co.course_id = a.course_id "
        "WHERE g.student_id = ? AND g.published = 1",
        lambda r, ids: (r.choice(ids["students"]),)),
    "report: enrollment counts": (
        "SELECT c.code, c.title, c.slot, c.status, c.capacity, "
        "SUM(CASE WHEN e.status = 'APPROVED' THEN 1 ELSE 0 END), "
        "SUM(CASE WHEN e.status = 'PENDING' THEN 1 ELSE 0 END) "
        "FROM courses c LEFT JOIN enrollments e ON e.course_id = c.course_id "
        "GROUP BY c.course_id ORDER BY c.code",
        lambda r, ids: ()),
    "report: grade averages": (
        f"SELECT code, COUNT(*), AVG(pct), MAX(pct), MIN(pct) FROM ("
        f"SELECT co.course_id, co.code, g.student_id, {PCT} AS pct FROM grades g "
        f"JOIN assignments a ON a.assignment_id = g.assignment_id "
        f"JOIN courses co ON co.course_id = a.course_id WHERE g.published = 1 "
        f"GROUP BY co.course_id, g.student_id) GROUP BY course_id ORDER BY code",
        lambda r, ids: ()),
    "admin search: student name prefix": (
        "SELECT student_id, roll_number, name FROM students WHERE name LIKE ? ORDER BY name",
        lambda r, ids: (r.choice(ids["names"])[:3] + "%",)),
}


def sample_ids(conn, rng):
    return {
        "students": [r[0] for r in conn.execute("SELECT student_id FROM students")],
        "courses": [r[0] for r in conn.execute("SELECT course_id FROM courses")],
        "names": [r[0] for r in conn.execute("SELECT name FROM students LIMIT 500")],
    }


def count_rows(conn):
    out = {}
    for t in ("students", "courses", "enrollments", "assignments", "grades"):
        try:
            out[t] = conn.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
        except sqlite3.Error:
            out[t] = "?"
    return out


def run(conn, repeats, seed):
    rng = random.Random(seed)
    ids = sample_ids(conn, rng)
    print("rows:", count_rows(conn))
    existing = [r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'idx_%'")]
    print("indexes present:", sorted(existing) if existing else "none")
    print(f"repeats per query: {repeats}\n")
    print(f"{'query':40} {'avg ms':>9} {'min ms':>9} {'max ms':>9}")
    print("-" * 70)
    for name, (sql, make) in QUERIES.items():
        times = []
        for _ in range(repeats):
            params = make(rng, ids)
            t0 = time.perf_counter()
            conn.execute(sql, params).fetchall()
            times.append((time.perf_counter() - t0) * 1000)
        print(f"{name:40} {statistics.mean(times):9.3f} {min(times):9.3f} {max(times):9.3f}")
    print("\nQUERY PLANS (SCAN = reads the whole table, SEARCH = uses an index)")
    print("-" * 70)
    for name, (sql, make) in QUERIES.items():
        params = make(rng, ids)
        plan = conn.execute("EXPLAIN QUERY PLAN " + sql, params).fetchall()
        print(f"[{name}]")
        for row in plan:
            print("   ", row[-1])


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--db", default=str(DEFAULT_DB))
    p.add_argument("--repeats", type=int, default=30)
    p.add_argument("--seed", type=int, default=42, help="same seed = same sample, so before/after are comparable")
    p.add_argument("--drop-indexes", action="store_true")
    p.add_argument("--apply-sql", help="run this SQL file (e.g. db/indexes.sql), then ANALYZE")
    p.add_argument("--cprofile", action="store_true")
    args = p.parse_args()

    if not Path(args.db).exists():
        sys.exit(f"Database not found: {args.db}")
    conn = sqlite3.connect(args.db)
    try:
        if args.drop_indexes:
            for n in INDEX_NAMES:
                conn.execute(f"DROP INDEX IF EXISTS {n}")
            conn.commit()
            print("Dropped profiling indexes.")
            return
        if args.apply_sql:
            conn.executescript(Path(args.apply_sql).read_text())
            conn.execute("ANALYZE")
            conn.commit()
            print(f"Applied {args.apply_sql} and ran ANALYZE.")
            return
        if args.cprofile:
            prof = cProfile.Profile()
            prof.runcall(run, conn, args.repeats, args.seed)
            print("\ncProfile (top 10 by cumulative time)")
            pstats.Stats(prof).sort_stats("cumulative").print_stats(10)
        else:
            run(conn, args.repeats, args.seed)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
