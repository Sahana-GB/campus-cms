#!/usr/bin/env python3
"""Campus CMS reports. Reads data/campus.db and prints a table, optionally saves CSV / a chart.

Examples (run from the project root):
  python3 python/reports.py enrollment-counts
  python3 python/reports.py course-roster --code CS101
  python3 python/reports.py grade-averages --plot grade_averages.png
  python3 python/reports.py transcript --roll R001 --csv transcript_R001.csv
  python3 python/reports.py bell-curve --code CS101 --plot bell_CS101.png

Only PUBLISHED grades are used, exactly like the Java side (CgpaCalculator).
Needs only the standard library; --plot needs matplotlib (pip install -r python/requirements.txt).
"""
import argparse
import csv
import sqlite3
import statistics
import sys
from pathlib import Path

DEFAULT_DB = Path(__file__).resolve().parent.parent / "data" / "campus.db"

# Course percentage for one student: weighted by assignment weight; plain average if no weights set.
PCT = ("CASE WHEN SUM(a.weight) > 0 "
       "THEN SUM(g.marks * 1.0 / a.max_marks * a.weight) / SUM(a.weight) * 100.0 "
       "ELSE AVG(g.marks * 1.0 / a.max_marks) * 100.0 END")

LETTER = ("CASE WHEN pct >= 90 THEN 'A' WHEN pct >= 80 THEN 'B' WHEN pct >= 70 THEN 'C' "
          "WHEN pct >= 60 THEN 'D' WHEN pct >= 50 THEN 'E' ELSE 'F' END")

QUERIES = {
    "enrollment-counts": (
        "Enrollment counts per course",
        """SELECT c.code AS "Code", c.title AS "Title", c.slot AS "Slot",
                  c.status AS "Course status", c.capacity AS "Capacity",
                  SUM(CASE WHEN e.status = 'APPROVED' THEN 1 ELSE 0 END) AS "Approved",
                  SUM(CASE WHEN e.status = 'PENDING' THEN 1 ELSE 0 END) AS "Pending",
                  c.capacity - SUM(CASE WHEN e.status = 'APPROVED' THEN 1 ELSE 0 END) AS "Seats left"
           FROM courses c LEFT JOIN enrollments e ON e.course_id = c.course_id
           GROUP BY c.course_id ORDER BY c.code""",
        ()),
    "course-roster": (
        "Course roster",
        """SELECT s.roll_number AS "Roll number", s.name AS "Name", s.branch AS "Branch",
                  s.year AS "Year", s.email AS "Email"
           FROM enrollments e
           JOIN students s ON s.student_id = e.student_id
           JOIN courses c ON c.course_id = e.course_id
           WHERE c.code = ? AND e.status = 'APPROVED' ORDER BY s.roll_number""",
        ("code",)),
    "grade-averages": (
        "Grade averages per course (published grades, percent)",
        f"""SELECT code AS "Code", title AS "Title", COUNT(*) AS "Students graded",
                   ROUND(AVG(pct), 2) AS "Average", ROUND(MAX(pct), 2) AS "Highest",
                   ROUND(MIN(pct), 2) AS "Lowest"
            FROM (SELECT co.course_id, co.code, co.title, g.student_id, {PCT} AS pct
                  FROM grades g
                  JOIN assignments a ON a.assignment_id = g.assignment_id
                  JOIN courses co ON co.course_id = a.course_id
                  WHERE g.published = 1
                  GROUP BY co.course_id, g.student_id)
            GROUP BY course_id ORDER BY code""",
        ()),
    "transcript": (
        "Student transcript (published grades)",
        f"""SELECT code AS "Code", title AS "Title", credits AS "Credits",
                   ROUND(pct, 2) AS "Percentage", {LETTER} AS "Letter grade"
            FROM (SELECT co.code, co.title, co.credits, {PCT} AS pct
                  FROM grades g
                  JOIN assignments a ON a.assignment_id = g.assignment_id
                  JOIN courses co ON co.course_id = a.course_id
                  JOIN students s ON s.student_id = g.student_id
                  WHERE s.roll_number = ? AND g.published = 1
                  GROUP BY co.course_id)
            ORDER BY code""",
        ("roll",)),
}


def run_query(conn, name, args):
    title, sql, params = QUERIES[name]
    values = []
    for p in params:
        v = getattr(args, p)
        if v is None:
            sys.exit(f"--{p} is required for {name}")
        values.append(v)
    cur = conn.execute(sql, values)
    return title, [d[0] for d in cur.description], cur.fetchall()


def print_table(title, headers, rows):
    print(title)
    print("=" * len(title))
    if not rows:
        print("(no rows)")
        return
    widths = [max(len(str(h)), *(len(str(r[i])) for r in rows)) for i, h in enumerate(headers)]
    line = "  ".join(str(h).ljust(w) for h, w in zip(headers, widths))
    print(line)
    print("-" * len(line))
    for r in rows:
        print("  ".join(str("" if v is None else v).ljust(w) for v, w in zip(r, widths)))
    print(f"\n{len(rows)} row(s)")


def write_csv(path, headers, rows):
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(headers)
        w.writerows(rows)
    print(f"Saved CSV: {path}")


def require_matplotlib():
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        return plt
    except ImportError:
        sys.exit("matplotlib is not installed. Run: pip install -r python/requirements.txt")


def plot_averages(headers, rows, path):
    plt = require_matplotlib()
    codes = [r[0] for r in rows]
    fig, ax = plt.subplots(figsize=(max(6, len(codes) * 0.6), 4))
    ax.bar(codes, [r[3] for r in rows], color="#4c78a8")
    ax.set_ylabel("Average (%)")
    ax.set_title("Grade averages per course")
    ax.set_ylim(0, 100)
    plt.xticks(rotation=60, ha="right")
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    print(f"Saved chart: {path}")


def bell_curve(conn, code, plot_path):
    """Per-student percentages for one course, with mean/std-dev, and an optional histogram + normal curve."""
    cur = conn.execute(
        f"""SELECT {PCT} AS pct
            FROM grades g
            JOIN assignments a ON a.assignment_id = g.assignment_id
            JOIN courses co ON co.course_id = a.course_id
            WHERE co.code = ? AND g.published = 1
            GROUP BY g.student_id""", (code,))
    marks = [r[0] for r in cur.fetchall()]
    if not marks:
        sys.exit(f"No published grades for course {code}.")
    mean = statistics.mean(marks)
    sd = statistics.pstdev(marks)
    print(f"Course {code}: students={len(marks)}  mean={mean:.2f}  std-dev={sd:.2f}  "
          f"highest={max(marks):.2f}  lowest={min(marks):.2f}")
    if plot_path:
        plt = require_matplotlib()
        import math
        fig, ax = plt.subplots(figsize=(7, 4))
        ax.hist(marks, bins=10, range=(0, 100), density=True, color="#9ecae9", edgecolor="white")
        if sd > 0:
            xs = [i / 2 for i in range(0, 201)]
            ys = [math.exp(-((x - mean) ** 2) / (2 * sd * sd)) / (sd * math.sqrt(2 * math.pi)) for x in xs]
            ax.plot(xs, ys, color="#e45756")
        ax.set_xlabel("Percentage")
        ax.set_ylabel("Density")
        ax.set_title(f"Grade distribution: {code}")
        fig.tight_layout()
        fig.savefig(plot_path, dpi=150)
        print(f"Saved chart: {plot_path}")


def main():
    p = argparse.ArgumentParser(description="Campus CMS reports")
    p.add_argument("report", choices=list(QUERIES) + ["bell-curve"])
    p.add_argument("--db", default=str(DEFAULT_DB), help="path to campus.db")
    p.add_argument("--code", help="course code (course-roster, bell-curve)")
    p.add_argument("--roll", help="student roll number (transcript)")
    p.add_argument("--csv", help="also save the table to this CSV file")
    p.add_argument("--plot", help="save a chart to this PNG file (grade-averages, bell-curve)")
    args = p.parse_args()

    if not Path(args.db).exists():
        sys.exit(f"Database not found: {args.db}  (run ./scripts/setup.sh first)")
    conn = sqlite3.connect(f"file:{args.db}?mode=ro", uri=True)   # read-only: reports never change data
    try:
        if args.report == "bell-curve":
            if not args.code:
                sys.exit("--code is required for bell-curve")
            bell_curve(conn, args.code, args.plot)
            return
        title, headers, rows = run_query(conn, args.report, args)
        print_table(title, headers, rows)
        if args.csv:
            write_csv(args.csv, headers, rows)
        if args.plot:
            if args.report != "grade-averages":
                sys.exit("--plot only works with grade-averages and bell-curve")
            plot_averages(headers, rows, args.plot)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
