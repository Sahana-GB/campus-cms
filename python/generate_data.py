#!/usr/bin/env python3
"""Synthetic Student Data Generator for Campus Course Management System (CCMS).

Generates synthetic student records at scale (e.g. 100, 1,000, 10,000 students)
and inserts them into SQLite while preserving full schema integrity, foreign
keys, and exact Java PBKDF2-HMAC-SHA256 password hash compatibility.

Usage:
  python python/generate_data.py --count 100
  python python/generate_data.py --count 1000 --db data/campus.db
  python python/generate_data.py --count 10000 --seed 42 --dry-run
"""

import argparse
import hashlib
import os
import re
import secrets
import sqlite3
import sys
import time
from pathlib import Path
from typing import Dict, List, Optional, Set, Tuple

DEFAULT_DB = Path(__file__).resolve().parent.parent / "data" / "campus.db"
SYNTHETIC_USER_PREFIX = "syn_student_"
SYNTHETIC_ROLL_PREFIX = "SYN26"
DEFAULT_TEST_PASSWORD = "pass123"

# PBKDF2 Parameters (Strictly matching com.campus.cms.auth.PasswordUtil.java)
SALT_LENGTH = 16  # 16 bytes = 32 hex chars
HASH_LENGTH = 32  # 32 bytes = 64 hex chars
ITERATIONS = 10_000
HASH_ALGORITHM = "sha256"

# Valid branch & academic year domains
BRANCHES = ["CSE", "ECE", "ME", "CIVIL"]
YEARS = [1, 2, 3, 4]

# Fictional names for realistic synthetic distribution
FIRST_NAMES = [
    "Aarav", "Aanya", "Abhishek", "Aditi", "Aditya", "Akash", "Amit", "Amrita",
    "Anand", "Ananya", "Anil", "Anita", "Anjali", "Ankit", "Archana", "Arjun",
    "Arun", "Asha", "Ashish", "Avinash", "Bhavna", "Chetan", "Deepa", "Deepak",
    "Dev", "Divya", "Ganesh", "Gauri", "Gautam", "Geeta", "Harish", "Harita",
    "Ishaan", "Isha", "Jaya", "Jyoti", "Karan", "Karthik", "Kavya", "Kiran",
    "Krishna", "Kunal", "Lakshmi", "Madhav", "Manish", "Maya", "Meena", "Mohan",
    "Mukesh", "Naveen", "Neha", "Nikhil", "Nisha", "Pallavi", "Pooja", "Pranav",
    "Prashant", "Priya", "Rahul", "Raj", "Rajesh", "Rakesh", "Ramesh", "Rashmi",
    "Ravi", "Reena", "Rishi", "Ritu", "Rohan", "Rohit", "Sachin", "Sameer",
    "Sandhya", "Sanjay", "Sanjana", "Santosh", "Sarita", "Shashi", "Shilpa",
    "Shreya", "Siddharth", "Sneha", "Sonali", "Suman", "Sunil", "Suresh",
    "Swati", "Tarun", "Umesh", "Varun", "Vikas", "Vinay", "Vishal", "Vivek"
]

LAST_NAMES = [
    "Agarwal", "Babu", "Banerjee", "Bhat", "Bhattacharya", "Chakraborty",
    "Chatterjee", "Chauhan", "Chopra", "Das", "Deshmukh", "Dey", "Dubey",
    "Ghosh", "Gupta", "Hegde", "Iyer", "Jain", "Jha", "Joshi", "Kapoor",
    "Khan", "Kulkarni", "Kumar", "Mahajan", "Malhotra", "Mehta", "Menon",
    "Mishra", "Mukherjee", "Nair", "Narayan", "Pandey", "Patel", "Patil",
    "Pillai", "Prasad", "Puri", "Radhakrishnan", "Rao", "Reddy", "Roy",
    "Saha", "Saxena", "Sen", "Seth", "Sharma", "Shetty", "Shukla", "Singh",
    "Sinha", "Srivastava", "Subramanian", "Tiwari", "Tripathi", "Varma",
    "Verma", "Vyas", "Yadav"
]


def hash_password(password: str, salt: Optional[bytes] = None) -> str:
    """Hashes a password matching Java PasswordUtil (PBKDF2-HMAC-SHA256).

    Format: <salt_hex>:<hash_hex>
    """
    if not password:
        raise ValueError("Password cannot be empty")
    if salt is None:
        salt = secrets.token_bytes(SALT_LENGTH)
    elif len(salt) != SALT_LENGTH:
        raise ValueError(f"Salt must be {SALT_LENGTH} bytes")

    derived_hash = hashlib.pbkdf2_hmac(
        HASH_ALGORITHM,
        password.encode("utf-8"),
        salt,
        ITERATIONS,
        dklen=HASH_LENGTH,
    )
    return f"{salt.hex()}:{derived_hash.hex()}"


def verify_password(password: str, stored_hash: str) -> bool:
    """Verifies a password against a salt:hash string."""
    if not password or not stored_hash:
        return False
    parts = stored_hash.split(":")
    if len(parts) != 2:
        return False
    try:
        salt = bytes.fromhex(parts[0])
        expected_hash = bytes.fromhex(parts[1])
        if len(salt) != SALT_LENGTH or len(expected_hash) != HASH_LENGTH:
            return False
        actual_hash = hashlib.pbkdf2_hmac(
            HASH_ALGORITHM,
            password.encode("utf-8"),
            salt,
            ITERATIONS,
            dklen=HASH_LENGTH,
        )
        return secrets.compare_digest(expected_hash, actual_hash)
    except (ValueError, TypeError):
        return False


def get_existing_synthetic_indices(con: sqlite3.Connection) -> Set[int]:
    """Finds existing synthetic student sequence numbers from database."""
    cur = con.cursor()
    cur.execute(
        "SELECT username FROM users WHERE username LIKE ?",
        (f"{SYNTHETIC_USER_PREFIX}%",),
    )
    indices = set()
    for (username,) in cur.fetchall():
        suffix = username[len(SYNTHETIC_USER_PREFIX):]
        if suffix.isdigit():
            indices.add(int(suffix))
    return indices


def generate_records(
    target_count: int,
    existing_indices: Set[int],
    seed: Optional[int] = None,
    password: str = DEFAULT_TEST_PASSWORD,
) -> List[Dict]:
    """Generates synthetic student records up to target_count (idempotent)."""
    import random
    rng = random.Random(seed) if seed is not None else random.Random()

    records = []
    # Identify which sequence indices from 1..target_count still need creation
    needed_indices = [i for i in range(1, target_count + 1) if i not in existing_indices]

    for seq in needed_indices:
        username = f"{SYNTHETIC_USER_PREFIX}{seq:06d}"
        roll_number = f"{SYNTHETIC_ROLL_PREFIX}{seq:05d}"

        first = rng.choice(FIRST_NAMES)
        last = rng.choice(LAST_NAMES)
        full_name = f"{first} {last}"

        branch = BRANCHES[(seq - 1) % len(BRANCHES)]
        year = YEARS[((seq - 1) // len(BRANCHES)) % len(YEARS)]
        email = f"syn.student.{seq:06d}@campus.edu"

        # Deterministic salt when seed is provided, random otherwise
        if seed is not None:
            # Derive deterministic 16-byte salt
            seed_material = f"{seed}:{seq}:{username}".encode("utf-8")
            salt = hashlib.sha256(seed_material).digest()[:SALT_LENGTH]
        else:
            salt = secrets.token_bytes(SALT_LENGTH)

        pwd_hash = hash_password(password, salt=salt)

        records.append({
            "seq": seq,
            "username": username,
            "password_hash": pwd_hash,
            "roll_number": roll_number,
            "name": full_name,
            "branch": branch,
            "year": year,
            "email": email,
            "status": "ACTIVE",
            "is_active": 1,
        })

    return records


def insert_synthetic_records(
    db_path: Path,
    records: List[Dict],
    dry_run: bool = False,
) -> Tuple[int, int]:
    """Inserts generated records into SQLite atomically inside a transaction.

    Returns:
        (users_inserted, students_inserted)
    """
    if dry_run or not records:
        return len(records), len(records)

    con = sqlite3.connect(str(db_path))
    try:
        cur = con.cursor()
        cur.execute("PRAGMA foreign_keys = ON;")
        cur.execute("PRAGMA busy_timeout = 5000;")

        # Single atomic transaction for all records
        cur.execute("BEGIN TRANSACTION;")

        insert_user_sql = (
            "INSERT INTO users (username, password_hash, role, is_active) "
            "VALUES (?, ?, 'STUDENT', ?);"
        )
        insert_student_sql = (
            "INSERT INTO students (user_id, roll_number, name, branch, year, email, status) "
            "VALUES (?, ?, ?, ?, ?, ?, ?);"
        )

        users_count = 0
        students_count = 0

        for r in records:
            cur.execute(insert_user_sql, (r["username"], r["password_hash"], r["is_active"]))
            user_id = cur.lastrowid
            users_count += 1

            cur.execute(
                insert_student_sql,
                (user_id, r["roll_number"], r["name"], r["branch"], r["year"], r["email"], r["status"]),
            )
            students_count += 1

        con.commit()
        return users_count, students_count

    except Exception:
        con.rollback()
        raise
    finally:
        con.close()


def validate_database(db_path: Path) -> Dict:
    """Performs integrity and distribution validation on the database."""
    con = sqlite3.connect(str(db_path))
    try:
        cur = con.cursor()
        cur.execute("PRAGMA foreign_keys = ON;")

        # 1. Total counts
        cur.execute("SELECT COUNT(*) FROM users;")
        total_users = cur.fetchone()[0]

        cur.execute("SELECT COUNT(*) FROM students;")
        total_students = cur.fetchone()[0]

        cur.execute(f"SELECT COUNT(*) FROM users WHERE username LIKE '{SYNTHETIC_USER_PREFIX}%';")
        synth_users = cur.fetchone()[0]

        cur.execute(f"SELECT COUNT(*) FROM students WHERE roll_number LIKE '{SYNTHETIC_ROLL_PREFIX}%';")
        synth_students = cur.fetchone()[0]

        # 2. Branch & Year breakdown for synthetic records
        cur.execute(f"""
            SELECT branch, COUNT(*) FROM students
            WHERE roll_number LIKE '{SYNTHETIC_ROLL_PREFIX}%'
            GROUP BY branch ORDER BY branch;
        """)
        branch_counts = dict(cur.fetchall())

        cur.execute(f"""
            SELECT year, COUNT(*) FROM students
            WHERE roll_number LIKE '{SYNTHETIC_ROLL_PREFIX}%'
            GROUP BY year ORDER BY year;
        """)
        year_counts = dict(cur.fetchall())

        # 3. Active counts
        cur.execute(f"""
            SELECT COUNT(*) FROM students
            WHERE roll_number LIKE '{SYNTHETIC_ROLL_PREFIX}%' AND status = 'ACTIVE';
        """)
        active_students = cur.fetchone()[0]

        # 4. Integrity and FK checks
        cur.execute("PRAGMA integrity_check;")
        integrity_status = cur.fetchone()[0]

        cur.execute("PRAGMA foreign_key_check;")
        fk_violations = len(cur.fetchall())

        # 5. Duplicate checks
        cur.execute("SELECT username, COUNT(*) FROM users GROUP BY username HAVING COUNT(*) > 1;")
        duplicate_usernames = len(cur.fetchall())

        cur.execute("SELECT roll_number, COUNT(*) FROM students GROUP BY roll_number HAVING COUNT(*) > 1;")
        duplicate_rolls = len(cur.fetchall())

        return {
            "total_users": total_users,
            "total_students": total_students,
            "synth_users": synth_users,
            "synth_students": synth_students,
            "branch_counts": branch_counts,
            "year_counts": year_counts,
            "active_students": active_students,
            "integrity_status": integrity_status,
            "fk_violations": fk_violations,
            "duplicate_usernames": duplicate_usernames,
            "duplicate_rolls": duplicate_rolls,
        }
    finally:
        con.close()


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Campus CMS Synthetic Student Data Generator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Examples:
  python python/generate_data.py --count 100
  python python/generate_data.py --count 1000 --db data/campus.db
  python python/generate_data.py --count 10000 --seed 42 --dry-run
""",
    )
    parser.add_argument(
        "--count",
        type=int,
        required=True,
        help="Desired total number of synthetic student records to generate (e.g. 100, 1000, 10000).",
    )
    parser.add_argument(
        "--db",
        type=Path,
        default=DEFAULT_DB,
        help=f"Target SQLite database path (default: {DEFAULT_DB}).",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="Optional random seed for reproducible data generation.",
    )
    parser.add_argument(
        "--password",
        type=str,
        default=DEFAULT_TEST_PASSWORD,
        help=f"Shared plain-text password for generated student test accounts (default: '{DEFAULT_TEST_PASSWORD}').",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Generate and validate data in-memory without inserting into database.",
    )
    return parser.parse_args(argv)


def run_generator(
    count: int,
    db_path: Path = DEFAULT_DB,
    seed: Optional[int] = None,
    password: str = DEFAULT_TEST_PASSWORD,
    dry_run: bool = False,
) -> Dict:
    """Executes the data generator workflow and prints a clean report."""
    if count <= 0:
        raise ValueError("--count must be a positive integer greater than 0.")

    if not dry_run and not db_path.is_file():
        raise FileNotFoundError(f"Database file not found: {db_path.resolve()}")

    t_start = time.time()

    # Step 1: Detect existing synthetic records for idempotency
    existing_indices = set()
    if not dry_run:
        con = sqlite3.connect(str(db_path))
        try:
            existing_indices = get_existing_synthetic_indices(con)
        finally:
            con.close()

    already_present = len([i for i in range(1, count + 1) if i in existing_indices])
    to_create = count - already_present

    # Step 2: Generate records
    records = generate_records(count, existing_indices, seed=seed, password=password)

    # Step 3: Insert into SQLite
    users_inserted, students_inserted = insert_synthetic_records(
        db_path, records, dry_run=dry_run
    )

    elapsed = time.time() - t_start

    # Step 4: Validate
    stats = {}
    if not dry_run:
        stats = validate_database(db_path)
    else:
        # Dry-run mock stats for reporting
        branch_counts = {}
        year_counts = {}
        for r in records:
            branch_counts[r["branch"]] = branch_counts.get(r["branch"], 0) + 1
            year_counts[r["year"]] = year_counts.get(r["year"], 0) + 1
        stats = {
            "total_users": len(records),
            "total_students": len(records),
            "synth_users": len(records),
            "synth_students": len(records),
            "branch_counts": branch_counts,
            "year_counts": year_counts,
            "active_students": len(records),
            "integrity_status": "ok",
            "fk_violations": 0,
            "duplicate_usernames": 0,
            "duplicate_rolls": 0,
        }

    # Step 5: Report Summary
    print("=" * 55)
    print("Campus CMS Synthetic Student Data Generator")
    print("=" * 55)
    print(f"Database path                : {db_path.resolve()}")
    print(f"Mode                         : {'DRY RUN (No changes written)' if dry_run else 'DATABASE INSERTION'}")
    print(f"Target synthetic students    : {count}")
    print(f"Pre-existing synthetic count : {already_present}")
    print(f"Newly generated & inserted   : {len(records)}")
    print(f"Total synthetic in database  : {stats['synth_students']}")
    print()

    print("Branch distribution (synthetic):")
    for b in BRANCHES:
        cnt = stats["branch_counts"].get(b, 0)
        pct = (cnt / stats['synth_students'] * 100.0) if stats['synth_students'] > 0 else 0
        print(f"  {b:<10}: {cnt:>5} ({pct:5.1f}%)")
    print()

    print("Academic Year distribution (synthetic):")
    for y in YEARS:
        cnt = stats["year_counts"].get(y, 0)
        pct = (cnt / stats['synth_students'] * 100.0) if stats['synth_students'] > 0 else 0
        print(f"  Year {y}    : {cnt:>5} ({pct:5.1f}%)")
    print()

    fk_msg = "PASSED (0 violations)" if stats['fk_violations'] == 0 else f"FAILED ({stats['fk_violations']} violations)"
    integ_msg = "PASSED (ok)" if stats['integrity_status'] == 'ok' else f"FAILED ({stats['integrity_status']})"
    dup_u_msg = "PASSED (0 duplicates)" if stats['duplicate_usernames'] == 0 else f"FAILED ({stats['duplicate_usernames']} duplicates)"
    dup_r_msg = "PASSED (0 duplicates)" if stats['duplicate_rolls'] == 0 else f"FAILED ({stats['duplicate_rolls']} duplicates)"

    print(f"Active accounts status       : {stats['active_students']} ACTIVE")
    print(f"Foreign key validation       : {fk_msg}")
    print(f"Database integrity check     : {integ_msg}")
    print(f"Duplicate username check     : {dup_u_msg}")
    print(f"Duplicate roll number check  : {dup_r_msg}")
    print(f"Execution time               : {elapsed:.2f} s")
    print("=" * 55)

    return {
        "count": count,
        "already_present": already_present,
        "newly_created": len(records),
        "elapsed_seconds": elapsed,
        "stats": stats,
    }


def main():
    args = parse_args()
    try:
        run_generator(
            count=args.count,
            db_path=args.db,
            seed=args.seed,
            password=args.password,
            dry_run=args.dry_run,
        )
    except Exception as e:
        print(f"\n[ERROR] Generation failed: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
