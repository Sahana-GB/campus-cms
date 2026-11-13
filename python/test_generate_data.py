#!/usr/bin/env python3
"""Comprehensive test suite for python/generate_data.py.

Uses temporary SQLite databases to verify:
- 100, 1,000, and 10,000 records generation
- Unique usernames, roll numbers, and emails
- Foreign key integrity and PRAGMA checks
- Password hash format & Java PBKDF2-HMAC-SHA256 compatibility
- Transaction rollback on insertion error
- Dry-run mode
- Idempotency / repeated execution
- Invalid inputs handling

Run:
  python -m unittest python/test_generate_data.py
"""

import os
import shutil
import sqlite3
import sys
import tempfile
import time
import unittest
from pathlib import Path

# Ensure python directory is on sys.path
sys.path.insert(0, str(Path(__file__).resolve().parent))

from generate_data import (
    DEFAULT_TEST_PASSWORD,
    HASH_LENGTH,
    ITERATIONS,
    SALT_LENGTH,
    SYNTHETIC_ROLL_PREFIX,
    SYNTHETIC_USER_PREFIX,
    generate_records,
    hash_password,
    insert_synthetic_records,
    run_generator,
    validate_database,
    verify_password,
)

SCHEMA_PATH = Path(__file__).resolve().parent.parent / "db" / "schema.sql"


class TestSyntheticDataGenerator(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.db_path = Path(self.temp_dir) / "test_campus.db"

        # Initialize test database from db/schema.sql
        con = sqlite3.connect(str(self.db_path))
        try:
            with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
                schema_sql = f.read()
            con.executescript(schema_sql)
            con.commit()
        finally:
            con.close()

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_password_hash_format_and_java_compatibility(self):
        """Verifies PBKDF2-HMAC-SHA256 hash matches Java PasswordUtil specifications."""
        password = "test_password_123"
        stored_hash = hash_password(password)

        parts = stored_hash.split(":")
        self.assertEqual(len(parts), 2, "Hash must follow salt_hex:hash_hex format")

        salt_hex, hash_hex = parts
        self.assertEqual(len(salt_hex), SALT_LENGTH * 2, "Salt must be 16 bytes (32 hex characters)")
        self.assertEqual(len(hash_hex), HASH_LENGTH * 2, "Hash must be 32 bytes (64 hex characters)")

        # Verify positive and negative password verification
        self.assertTrue(verify_password(password, stored_hash))
        self.assertFalse(verify_password("wrong_password", stored_hash))
        self.assertFalse(verify_password("", stored_hash))
        self.assertFalse(verify_password(password, "invalid:hash"))

        # Test against known PBKDF2 vector from db/seed.sql (student1)
        # student1 in seed.sql:
        # salt = 9b6305a4c0bb1f84e2dd188b403f24b5
        # password = pass123
        # hash = fa156d835a1e289c3cc0fb1ed74800d9fba917c06737614f113fc2af05375501
        seed_salt = bytes.fromhex("9b6305a4c0bb1f84e2dd188b403f24b5")
        seed_hash = hash_password("pass123", salt=seed_salt)
        self.assertEqual(
            seed_hash,
            "9b6305a4c0bb1f84e2dd188b403f24b5:fa156d835a1e289c3cc0fb1ed74800d9fba917c06737614f113fc2af05375501",
            "Derived hash must match seed.sql and Java PasswordUtil PBKDF2 output byte-for-byte",
        )

    def test_dry_run_does_not_modify_database(self):
        """Dry-run must generate and validate data without writing to SQLite."""
        result = run_generator(count=100, db_path=self.db_path, dry_run=True)
        self.assertEqual(result["count"], 100)

        con = sqlite3.connect(str(self.db_path))
        try:
            cur = con.cursor()
            cur.execute("SELECT COUNT(*) FROM users;")
            self.assertEqual(cur.fetchone()[0], 0, "Users table must remain empty after dry run")
            cur.execute("SELECT COUNT(*) FROM students;")
            self.assertEqual(cur.fetchone()[0], 0, "Students table must remain empty after dry run")
        finally:
            con.close()

    def test_generate_100_records(self):
        """Generates 100 synthetic student records and checks schema integrity."""
        result = run_generator(count=100, db_path=self.db_path, seed=42)
        self.assertEqual(result["newly_created"], 100)

        stats = validate_database(self.db_path)
        self.assertEqual(stats["total_users"], 100)
        self.assertEqual(stats["total_students"], 100)
        self.assertEqual(stats["synth_users"], 100)
        self.assertEqual(stats["synth_students"], 100)
        self.assertEqual(stats["active_students"], 100)
        self.assertEqual(stats["fk_violations"], 0)
        self.assertEqual(stats["integrity_status"], "ok")
        self.assertEqual(stats["duplicate_usernames"], 0)
        self.assertEqual(stats["duplicate_rolls"], 0)

        # Check all branches and years are represented
        self.assertEqual(len(stats["branch_counts"]), 4)
        self.assertEqual(len(stats["year_counts"]), 4)

    def test_generate_1000_records(self):
        """Generates 1,000 synthetic student records and checks distribution."""
        result = run_generator(count=1000, db_path=self.db_path, seed=123)
        self.assertEqual(result["newly_created"], 1000)

        stats = validate_database(self.db_path)
        self.assertEqual(stats["total_users"], 1000)
        self.assertEqual(stats["total_students"], 1000)
        self.assertEqual(stats["fk_violations"], 0)
        self.assertEqual(stats["duplicate_usernames"], 0)
        self.assertEqual(stats["duplicate_rolls"], 0)

    def test_generate_10000_records(self):
        """Generates 10,000 synthetic student records and benchmarks execution."""
        t0 = time.time()
        result = run_generator(count=10000, db_path=self.db_path, seed=999)
        elapsed = time.time() - t0

        self.assertEqual(result["newly_created"], 10000)
        stats = validate_database(self.db_path)
        self.assertEqual(stats["total_users"], 10000)
        self.assertEqual(stats["total_students"], 10000)
        self.assertEqual(stats["fk_violations"], 0)
        self.assertEqual(stats["duplicate_usernames"], 0)
        self.assertEqual(stats["duplicate_rolls"], 0)
        print(f"\n[BENCHMARK] 10,000 records generated & inserted in: {elapsed:.2f} s")

    def test_unique_usernames_and_roll_numbers(self):
        """Verifies no collisions occur in username or roll number sequences."""
        records = generate_records(500, set(), seed=1)
        usernames = [r["username"] for r in records]
        rolls = [r["roll_number"] for r in records]
        emails = [r["email"] for r in records]

        self.assertEqual(len(usernames), len(set(usernames)), "All usernames must be strictly unique")
        self.assertEqual(len(rolls), len(set(rolls)), "All roll numbers must be strictly unique")
        self.assertEqual(len(emails), len(set(emails)), "All emails must be strictly unique")

    def test_valid_foreign_key_relationships(self):
        """Verifies every student row references a valid user row with role STUDENT."""
        run_generator(count=50, db_path=self.db_path)

        con = sqlite3.connect(str(self.db_path))
        try:
            cur = con.cursor()
            cur.execute("""
                SELECT s.student_id, s.user_id, u.username, u.role, u.is_active
                FROM students s
                JOIN users u ON s.user_id = u.user_id
            """)
            rows = cur.fetchall()
            self.assertEqual(len(rows), 50)
            for r in rows:
                self.assertEqual(r[3], "STUDENT", "Linked user role must be STUDENT")
                self.assertEqual(r[4], 1, "Linked user must be active")
        finally:
            con.close()

    def test_transaction_rollback_on_insertion_failure(self):
        """Verifies atomic rollback: if students insert fails, users are rolled back too."""
        # Create a trigger that aborts insertion into students
        con = sqlite3.connect(str(self.db_path))
        try:
            con.execute("CREATE TRIGGER fail_student_insert BEFORE INSERT ON students BEGIN SELECT RAISE(ABORT, 'Simulated failure'); END;")
            con.commit()
        finally:
            con.close()

        records = generate_records(10, set(), seed=42)
        with self.assertRaises(sqlite3.Error):
            insert_synthetic_records(self.db_path, records)

        # Verify no orphan rows in users
        con = sqlite3.connect(str(self.db_path))
        try:
            cur = con.cursor()
            cur.execute("SELECT COUNT(*) FROM users;")
            self.assertEqual(cur.fetchone()[0], 0, "Users table must be rolled back on failure")
            cur.execute("SELECT COUNT(*) FROM students;")
            self.assertEqual(cur.fetchone()[0], 0, "Students table must be rolled back on failure")
        finally:
            con.close()

    def test_repeated_execution_is_idempotent(self):
        """Repeated runs should not insert duplicates or crash."""
        # Run 1: generate 50 records
        r1 = run_generator(count=50, db_path=self.db_path, seed=10)
        self.assertEqual(r1["newly_created"], 50)

        # Run 2: request 50 records again (should create 0 new records)
        r2 = run_generator(count=50, db_path=self.db_path, seed=10)
        self.assertEqual(r2["newly_created"], 0)
        self.assertEqual(r2["already_present"], 50)

        # Run 3: request 100 records (should create 50 additional records)
        r3 = run_generator(count=100, db_path=self.db_path, seed=10)
        self.assertEqual(r3["newly_created"], 50)

        stats = validate_database(self.db_path)
        self.assertEqual(stats["total_students"], 100)
        self.assertEqual(stats["duplicate_usernames"], 0)
        self.assertEqual(stats["duplicate_rolls"], 0)

    def test_deterministic_seed_generation(self):
        """Same seed must produce identical data records."""
        rec1 = generate_records(20, set(), seed=777)
        rec2 = generate_records(20, set(), seed=777)

        self.assertEqual(rec1, rec2, "Identical seed must yield identical records")

    def test_invalid_arguments(self):
        """Invalid count or nonexistent db paths must raise descriptive errors."""
        with self.assertRaises(ValueError):
            run_generator(count=0, db_path=self.db_path)

        with self.assertRaises(ValueError):
            run_generator(count=-10, db_path=self.db_path)

        nonexistent_db = Path(self.temp_dir) / "missing_dir" / "nonexistent.db"
        with self.assertRaises(FileNotFoundError):
            run_generator(count=10, db_path=nonexistent_db)


if __name__ == "__main__":
    unittest.main()
