# Campus CMS (Course Management System)

A desktop-based academic course management system built with Java 17, Swing, FlatLaf, and SQLite. Campus CMS streamlines academic administration, course offerings, enrollment workflows, assessments, and grade publication through dedicated, role-guarded portals for Administrators, Faculty, and Students.

---

## Key Features

### Administrator Portal
- **Dashboard & KPIs**: Overview cards displaying total, active, and inactive student accounts with dynamic Quick Access navigation.
- **Student & Faculty Management**: Add, update, activate/deactivate, and inspect student and faculty records.
- **Course Catalog Management**: Create and configure course offerings, assign faculty members, set credit weights and enrollment capacities. Active course slot uniqueness is strictly enforced.
- **Enrollment Management**: Toggle enrollment periods (OPEN / CLOSED), review pending enrollment requests, manually approve/reject requests, and trigger automatic CGPA-based seat allocation.
- **Academic Reports & CSV Export**: Generate course enrollment statistics, course rosters, class grade averages, and student transcripts with one-click CSV export.

### Faculty Portal
- **Overview & Load Summary**: View assigned courses, enrolled student tallies, and upcoming assessment schedules.
- **Course Roster**: Inspect approved student enrollments for assigned courses.
- **Assignments & Quizzes**: Create assessments with customizable weight, maximum marks, due dates, and status (`OPEN` / `CLOSED`).
- **Grading & Feedback**: Auto-populates all enrolled students into the grading table; allows saving draft scores and one-click publishing of final marks and feedback.

### Student Portal
- **Overview & Academic Standing**: Personalized dashboard displaying roll number, academic branch, year, enrolled courses, and grade summaries.
- **Course Catalog & Enrollment**: Browse available course offerings, inspect instructor and slot information, and submit enrollment requests or drop registered courses.
- **Timetable View**: Weekly schedule view of registered courses mapped by slot.
- **Assignments & Grades**: Track published assessment marks, weighted percentages, and instructor feedback.
- **Profile & Security**: Self-service credential review and secure password modification.

---

## Architecture & Technology Stack

- **GUI Framework**: Java Swing with modern **FlatLaf Light** look-and-feel.
- **UI Design System (`com.campus.cms.core.ui`)**: Tokenized styling (`Theme`), button/table factories (`Styles`), responsive multi-row wrapping layouts (`WrapLayout`, `Styles.actionBar()`), KPI metrics (`DashboardCard`), and state indicators (`StatusBadge`).
- **Database**: SQLite 3 with JDBC, foreign key enforcement (`PRAGMA foreign_keys = ON`), and partial unique indexes.
- **Security & Authentication**: PBKDF2-HMAC-SHA256 password hashing (10,000 iterations, 16-byte cryptographically secure salt, 32-byte key) and role-guarded session management.
- **Build System**: Apache Maven 3.9+ with `maven-shade-plugin` packaging an executable uber-jar.
- **Testing**: JUnit 5 (Java, 136 tests) and standard library `unittest` (Python, 11 tests).
- **Synthetic Data & Analytics**: Python 3 standard library tools for synthetic data generation (`generate_data.py`), query profiling (`profile_queries.py`), and visualization (`reports.py`).

---

## Project Structure

```
campus-cms/
├── db/
│   ├── schema.sql           # Base DDL: users, students, faculty, courses, enrollments, assignments, grades
│   ├── seed.sql             # Demonstration fixtures with default accounts and sample courses
│   └── indexes.sql          # Performance B-tree indexes for query optimization
├── data/
│   └── campus.db            # Local SQLite database (created via scripts/setup.sh)
├── src/
│   ├── main/java/com/campus/cms/
│   │   ├── auth/            # Authentication, UserDAO, PasswordUtil, LoginFrame, ProfilePanel
│   │   ├── catalog/         # Course model, CourseDAO, CourseService, CoursesPanel
│   │   ├── core/            # Database connection, Session management, core UI design system
│   │   │   └── ui/          # Theme, Styles, SidebarNav, PageHeader, FilterBar, DashboardCard, StatusBadge
│   │   ├── dashboard/       # AdminDashboard, FacultyDashboard, StudentDashboard
│   │   ├── enrollment/      # EnrollmentDAO, EnrollmentService, Browse/MyCourses/Timetable/Reports panels
│   │   ├── faculty/         # Faculty model, FacultyDAO, FacultyPanel
│   │   ├── grading/         # Assignment, Grade, AssignmentDAO, GradeDAO, GradesPanel
│   │   └── students/        # Student model, StudentDAO, StudentsPanel
│   └── test/java/           # 136 comprehensive JUnit 5 integration and unit tests
├── python/
│   ├── generate_data.py     # Deterministic synthetic student generator (scales to 10,000+ records)
│   ├── test_generate_data.py# 11 automated unit tests for data generator
│   ├── profile_queries.py   # Benchmark script for profiling critical database queries
│   ├── reports.py           # CLI reports and matplotlib grade distribution charts
│   └── README.md            # Synthetic generator documentation and CLI options
├── docs/
│   ├── ai_usage/            # AI collaboration declarations 
│   └── report/              # LaTeX project report and modular section source files
│       ├── main.tex         # Master report document
│       └── sections/        # intro.tex, database.tex, person_a_implementation.tex, etc.
└── scripts/
    ├── setup.sh             # Re-creates data/campus.db from schema.sql and seed.sql
    ├── build.sh             # Compiles and packages executable JAR with Maven
    ├── run.sh               # Launches application via target/campus-cms.jar
    ├── test.sh              # Runs complete JUnit 5 test suite
    └── profile.sh         # Runs query profiling before and after db/indexes.sql
```

---

## Quick Start Guide

### Prerequisites
- **Java Development Kit (JDK) 17** or newer
- **Apache Maven 3.8+**
- **Python 3.10+** (optional: required for synthetic data generation and profiling)

### 1. Initialize Database
Initialize or reset the SQLite database with seed demonstration data:
```bash
# On Linux / macOS / Git Bash:
bash scripts/setup.sh

# Or directly using sqlite3 / python:
python -c "import sqlite3; con = sqlite3.connect('data/campus.db'); con.executescript(open('db/schema.sql').read()); con.executescript(open('db/seed.sql').read()); con.close()"
```

### 2. Build Executable JAR
Compile sources, execute tests, and package the shaded JAR:
```bash
mvn clean package -DskipTests
```
The executable JAR is generated at `target/campus-cms.jar`.

### 3. Launch the Application
Run the packaged JAR:
```bash
java -jar target/campus-cms.jar
```
*(Alternatively on Bash, execute `bash scripts/run.sh`)*

---

## Default Login Credentials

For demonstration admin account uses **`admin123`** as password whereas other accounts use password: **`pass123`**

| Role | Username | Full Name / Description |
|---|---|---|
| **Administrator** | `admin` | Campus System Administrator |
| **Faculty** | `faculty1` | Dr. Lakshmi Narayan (Computer Science) |
| **Faculty** | `faculty2` | Prof. Rajesh Kumar (Electronics) |
| **Student** | `student1` | Asha Reddy (Roll: `CS2401`, CSE, Year 2) |
| **Student** | `student2` | Vikram Patel (Roll: `CS2402`, CSE, Year 2) |
| **Student** | `student3` | Sneha Kulkarni (Roll: `EC2401`, ECE, Year 2) |

---

## Synthetic Student Data Generator

For stress testing and performance benchmarking, Person A implemented a high-speed synthetic data generator that generates realistic student cohorts with full Java PBKDF2 password parity:

```bash
# Dry run: preview 100 records and check distribution without modifying database
python python/generate_data.py --count 100 --dry-run

# Insert 1,000 synthetic students into data/campus.db
python python/generate_data.py --count 1000

# Insert 10,000 synthetic students for scale testing (runs in ~40 seconds)
python python/generate_data.py --count 10000 --seed 42
```

All generated students can log in immediately:
- **Username**: `syn_student_000001` through `syn_student_010000`
- **Password**: `pass123`
- **Role**: `STUDENT`

---

## Running Automated Tests

### Java Test Suite (JUnit 5)
Run the entire suite of 136 tests:
```bash
mvn test
```

### Python Test Suite (unittest)
Run the 11 tests verifying the synthetic generator:
```bash
python -m unittest python/test_generate_data.py
```

---

## Performance Profiling & Optimization

Query profiling conducted on 10{,}006 student records and 50{,}040 enrollments demonstrated significant performance gains using the indexes in `db/indexes.sql`:
- **Browse Courses**: Reduced from **146.5 ms** to **1.98 ms** (**74$\times$ speed-up**).
- **CGPA Calculation**: Reduced from **2.51 ms** to **0.025 ms** (**100$\times$ speed-up**).
- **Course Seat Lookups**: Reduced from **3.12 ms** to **0.059 ms** (**53$\times$ speed-up**).

To reproduce the profiling results:
```bash
bash scripts/profile.sh
```
