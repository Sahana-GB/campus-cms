# AI usage declaration: Person C (Enrollment and Reporting)

> Edit this so it is exactly true for you. Tick or change every [ ] item before submitting.

## Tool
Claude (Anthropic), used through the chat interface.

## What I used it for
- Planning the enrollment module: request / approve / reject / drop flow, capacity check, and CGPA-based
  allocation.
- Drafting code: `EnrollmentDAO`, `EnrollmentService`, the Browse, My Courses, Timetable and Admin Enrollments
  panels, `CsvExporter`, `Settings` (enrollment period), the grade averages and transcript reports,
  and the JUnit tests for them.
- Drafting the Python tools: `python/reports.py`, `python/profile_queries.py`,
  `python/generate_enrollment_data.py` and `scripts/profile_c.sh`.
- Explaining Git problems (merging `main`, branches, pull requests) and shell errors.
- Drafting `profiling/notes.md` and my report sections from my measured results.

## What I did myself
- Agreed the schema and the slot rule with the team, and decided to use the unique-slot rule instead of
  separate prerequisite and timetable-clash checks.
- Ran every command, merged with the other branches, and fixed conflicts.
- Loaded the 10,000-student dataset and ran the profiling on my own machine.
- [ ] Tried every feature in the running app (enrollment toggle, enroll, approve, drop, reports).

## How I checked the AI's work
- `mvn clean test`: 109 tests pass, including 22 for my feature (EnrollmentServiceTest,
  CsvExporterTest, EnrollmentPeriodAndReportsTest).
- Read the SQL and compared it with `schema.sql` (published grades only, status values, unique slot).
- Checked the profiling with `EXPLAIN QUERY PLAN` before and after adding the indexes, so the speed-ups
  are measured, not assumed.
- [ ] Manual testing in the app (list what you tried).

## Things the AI got wrong or I changed
- An early suggestion to add prerequisite and timetable-clash checks was dropped because our schema has
  no timetable or prerequisites and the slot rule prevents clashes.
- I adjusted `EnrollmentService` to read the logged-in student from `Session.getInstance()`.
- [ ] Add anything else you changed or rejected.

## Understanding
I can explain every part of my code in the demo, including the allocation logic and the indexes.
