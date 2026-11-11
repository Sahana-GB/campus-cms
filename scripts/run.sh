#!/usr/bin/env bash
# Starts the Campus CMS app. Creates the database and the jar first if missing.
#
# Usage:  scripts/run.sh [--reset] [--rebuild]
#   --reset    rebuild the database (demo data goes back to its starting state)
#   --rebuild  recompile the code before running

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

RESET=0
REBUILD=0
for arg in "$@"; do
  case "$arg" in
    --reset)   RESET=1 ;;
    --rebuild) REBUILD=1 ;;
    -h|--help) sed -n '2,7p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) echo "Unknown option: $arg  (use --reset, --rebuild or --help)" >&2; exit 1 ;;
  esac
done

if [ "$RESET" -eq 1 ] || [ ! -f data/campus.db ]; then
  bash scripts/setup.sh
fi

if [ "$REBUILD" -eq 1 ] || [ ! -f target/campus-cms.jar ]; then
  bash scripts/build.sh
fi

# Friendly message while Main.java has not been written yet.
if [ -d target/classes ] && [ ! -f target/classes/com/campus/cms/Main.class ]; then
  echo "ERROR: Main.java does not exist yet" >&2
  exit 1
fi

echo "Starting Campus CMS ..."
exec java -jar target/campus-cms.jar
