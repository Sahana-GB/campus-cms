# Profiling notes (Person C, with Person A)

## Setup
- Data: 10,006 students (10,000 from `python/generate_data.py --count 10000 --seed 42`), 43 courses,
  50,040 enrollments, 84 assignments, 70,301 grades (courses, enrollments and grades added by
  `python/generate_enrollment_data.py`).
- Tool: `python/profile_queries.py` runs the SQL that the Java app uses most (copied from `EnrollmentDAO`
  and `CgpaCalculator`), 30 repeats per query, fixed random seed, so before and after use the same samples.
  `scripts/profile_c.sh` drops the profiling indexes, measures (`before.txt`), applies `db/indexes.sql`,
  runs `ANALYZE`, and measures again (`after.txt`).
- Machine: WSL on a laptop, SQLite through Python's `sqlite3`. Times are averages in milliseconds; they do
  not include Swing or JDBC overhead.

## Bottlenecks found
`EXPLAIN QUERY PLAN` before the fix showed full table scans (`SCAN enrollments`, `SCAN g`, `SCAN students`)
in every slow query. The worst was **Browse Courses (146 ms)**: for each of the 43 courses it counts the
APPROVED enrollments, and without an index each count read the whole `enrollments` table. This is the first
page a student opens.

## Fix
Five indexes in `db/indexes.sql`:

| Index | Helps |
|---|---|
| `enrollments(course_id, status)` | seats taken, Browse Courses, admin filter by course, enrollment counts |
| `enrollments(status)` | admin dashboard card counts |
| `grades(student_id)` | CGPA and My Grades |
| `assignments(course_id)` | grade averages / transcript joins |
| `students(name COLLATE NOCASE)` | student search by name prefix |

`UNIQUE (student_id, course_id)` and `UNIQUE (assignment_id, student_id)` already create indexes, so
lookups by student or assignment needed nothing new.

## Results (average ms, 30 runs)

| Query | Before | After | Speed-up |
|---|---:|---:|---:|
| Seats taken (one course) | 3.122 | 0.059 | 53x |
| Admin card: count by status | 3.442 | 0.274 | 12.6x |
| Admin table: one course + PENDING | 3.717 | 0.806 | 4.6x |
| Admin table: all PENDING | 34.019 | 30.478 | 1.1x |
| Student: my enrollments | 0.051 | 0.034 | already indexed |
| **Student: browse courses** | **146.494** | **1.983** | **74x** |
| CGPA: grades of one student | 2.512 | 0.025 | 100x |
| Report: enrollment counts | 63.052 | 8.785 | 7.2x |
| Report: grade averages | 98.840 | 99.633 | none |
| Admin search: name prefix | 1.017 | 0.175 | 5.8x |

## What did not improve, and why
- **Grade averages (about 99 ms)** must read every published grade (70,301 rows) to compute the average,
  so no index can skip the work. It is an admin report used rarely, so we accepted it.
- **All PENDING (about 30 ms)** returns thousands of rows; the time is spent building and sorting the result,
  not finding it. The admin screen could add paging if it ever mattered.
- **My enrollments** was already fast (it uses the automatic UNIQUE index).
- The name index only helps prefix searches (`LIKE 'abc%'`). A "contains" search (`LIKE '%abc%'`) can never
  use an index. The `NOCASE` collation is needed so SQLite can use the index for case-insensitive `LIKE`.

## Trade-off
Indexes make inserts and updates slightly slower and use disk space. Enrollment is read-heavy, so this is
worth it. The indexes were added only after measuring, which is why `schema.sql` has none.

## Reproduce
```bash
bash scripts/setup.sh
python3 python/generate_data.py --count 10000 --seed 42
python3 python/generate_enrollment_data.py
bash scripts/profile_c.sh
```
