#!/usr/bin/env bash
# Person C + A: measure BEFORE and AFTER adding indexes. Run from anywhere:  ./scripts/profile_c.sh
# Needs data/campus.db already filled with the big dataset (Person A's generate_data.py).
# Safe to re-run: it removes the indexes first so "before" is always without them.
set -euo pipefail
cd "$(dirname "$0")/.."

DB="${1:-data/campus.db}"
[ -f "$DB" ] || { echo "Database not found: $DB (run ./scripts/setup.sh and generate the data first)"; exit 1; }
mkdir -p profiling

echo "== BEFORE (no indexes) =="
python3 python/profile_queries.py --db "$DB" --drop-indexes
python3 python/profile_queries.py --db "$DB" | tee profiling/before.txt

echo
echo "== Applying db/indexes.sql =="
python3 python/profile_queries.py --db "$DB" --apply-sql db/indexes.sql

echo
echo "== AFTER (with indexes) =="
python3 python/profile_queries.py --db "$DB" | tee profiling/after.txt

echo
echo "Saved profiling/before.txt and profiling/after.txt. Now fill in profiling/notes.md."
