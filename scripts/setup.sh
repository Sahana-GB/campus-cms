#!/usr/bin/env bash
# Creates the SQLite database from db/schema.sql and db/seed.sql.
#
# Usage:  scripts/setup.sh [db_path]        (default: data/campus.db)
#
# Safe to re-run: it deletes the old database first, so the demo data goes back
# to its starting state. Close the app before running it (Windows locks the file).
# Works with the sqlite3 command if you have it, otherwise falls back to Python 3.

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

DB="${1:-data/campus.db}"
SCHEMA="db/schema.sql"
SEED="db/seed.sql"
TABLES="users students faculty courses enrollments assignments grades"

for f in "$SCHEMA" "$SEED"; do
  if [ ! -f "$f" ]; then
    echo "ERROR: $f not found." >&2
    exit 1
  fi
done

mkdir -p "$(dirname "$DB")"

# Finds a working Python 3 (skips the fake Microsoft Store 'python3' on Windows).
find_python() {
  local cmd
  for cmd in python3 python py; do
    if command -v "$cmd" >/dev/null 2>&1 && "$cmd" -c "import sqlite3" >/dev/null 2>&1; then
      echo "$cmd"
      return 0
    fi
  done
  return 1
}

# Decide how to load the SQL BEFORE deleting anything, so a missing tool
# never leaves you without a database.
if command -v sqlite3 >/dev/null 2>&1; then
  MODE=sqlite3
elif PY="$(find_python)"; then
  MODE=python
else
  echo "ERROR: need either the 'sqlite3' command or Python 3 on your PATH." >&2
  exit 1
fi

# Remove the old database. If the app is still open, Windows refuses to delete it.
rm -f "$DB" "$DB-journal" "$DB-wal" "$DB-shm" 2>/dev/null || true
if [ -e "$DB" ]; then
  echo "ERROR: could not delete $DB. Close the webpage and try again." >&2
  exit 1
fi

echo "Creating $DB ..."
if [ "$MODE" = "sqlite3" ]; then
  echo "(using the sqlite3 command)"
  sqlite3 "$DB" < "$SCHEMA"
  sqlite3 "$DB" < "$SEED"
  for t in $TABLES; do
    echo "  $t: $(sqlite3 "$DB" "SELECT COUNT(*) FROM $t") rows"
  done
else
  echo "(using Python: $PY)"
  "$PY" - "$DB" "$SCHEMA" "$SEED" $TABLES << 'PYEOF'
import sqlite3, sys

db, schema, seed, *tables = sys.argv[1:]
con = sqlite3.connect(db)
con.execute("PRAGMA foreign_keys = ON")
for path in (schema, seed):
    with open(path, encoding="utf-8-sig") as f:   # utf-8-sig ignores a BOM
        con.executescript(f.read())
con.commit()
for t in tables:
    n = con.execute("SELECT COUNT(*) FROM " + t).fetchone()[0]
    print("  %s: %d rows" % (t, n))
con.close()
PYEOF
fi

echo "Database ready: $DB"
