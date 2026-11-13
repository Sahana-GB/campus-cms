# Synthetic Student Data Generator for Campus CMS

A scalable, deterministic synthetic student data generator designed for load testing, UI performance profiling, and query optimization in the Campus Course Management System.

## Features

- **Scalable Generation**: Generates from 10 to 10,000+ realistic, fictional student records in seconds.
- **Strict Schema Compliance**: Matches SQLite constraints, table schemas, foreign keys, and allowed branches/years.
- **Java PBKDF2-HMAC-SHA256 Compatibility**: Generates passwords matching `PasswordUtil.java` (10,000 iterations, 16-byte random salt, 32-byte derived hash, formatted as `salt_hex:hash_hex`). All generated accounts can log in directly via the Java application.
- **Zero External Dependencies**: Implemented purely using the Python standard library (`sqlite3`, `hashlib`, `secrets`, `argparse`).
- **Idempotent & Safe**: Automatically detects existing synthetic records (`syn_student_...`), avoiding duplicate insertions on repeated executions.
- **Dry-Run Mode**: Inspect generated data, distribution, and validation metrics without altering the database.

---

## Installation & Setup

No third-party packages are required. Python 3.10+ standard library is used.

Optional dependencies for reporting charts (in `python/reports.py`):
```bash
pip install -r python/requirements.txt
```

---

## Usage Guide

### 1. Dry Run (Preview without database modifications)
Test generation and view distribution metrics without writing to any database:
```bash
python python/generate_data.py --count 100 --dry-run
```

### 2. Generate 100 Students
Insert 100 synthetic student records into the default database (`data/campus.db`):
```bash
python python/generate_data.py --count 100
```

### 3. Generate 1,000 Students
Insert 1,000 synthetic student records:
```bash
python python/generate_data.py --count 1000
```

### 4. Generate 10,000 Students for Scale Testing
Generate and insert 10,000 records:
```bash
python python/generate_data.py --count 10000
```

### 5. Specifying a Custom or Temporary Database
Specify any target SQLite database using `--db`:
```bash
python python/generate_data.py --count 500 --db data/test_campus.db
```

### 6. Reproducible Deterministic Generation
Use `--seed` for reproducible names, branches, and distributions across runs:
```bash
python python/generate_data.py --count 100 --seed 42 --db data/test.db
```

### 7. Custom Account Passwords
All generated accounts default to `pass123`. You can specify a different test password using `--password`:
```bash
python python/generate_data.py --count 100 --password mycustompass
```

---

## CLI Options

| Option | Type | Default | Description |
|---|---|---|---|
| `--count` | `int` | *(Required)* | Desired total number of synthetic students to generate. |
| `--db` | `Path` | `data/campus.db` | Target SQLite database path. |
| `--seed` | `int` | `None` | Random seed for deterministic data generation. |
| `--password` | `str` | `pass123` | Shared plain-text password for generated test accounts. |
| `--dry-run` | `flag` | `False` | Run generation and validation without modifying database. |
| `--help` | `flag` | - | Display help and usage instructions. |

---

## Logging into Java Application with Generated Accounts

All generated accounts are fully active and authenticated against the system:
- **Username**: `syn_student_000001`, `syn_student_000002`, ..., `syn_student_010000`
- **Default Password**: `pass123`
- **Roll Number**: `SYN2600001`, `SYN2600002`, etc.
- **Role**: `STUDENT`

Run the Java application:
```powershell
java -jar target/campus-cms.jar
```
Log in with username `syn_student_000001` and password `pass123`.

---

## Running Automated Tests

Run the complete Python test suite:
```bash
python -m unittest python/test_generate_data.py
```
Tests cover:
- 100, 1,000, and 10,000 record generation and distribution
- Password hash format & PBKDF2 vector verification
- Unique username, roll number, and email guarantees
- Foreign key integrity (`PRAGMA foreign_key_check`)
- Atomic transaction rollback on insertion failure
- Dry-run verification
- Idempotency / repeated execution checks
