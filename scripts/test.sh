#!/usr/bin/env bash
# Run the complete JUnit 5 test suite.
# Usage: scripts/test.sh [Maven arguments...]
# Requires JDK 17+ and Maven on PATH.

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: Java was not found. Install JDK 17 or newer and add it to PATH." >&2
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | head -n 1 | sed -E 's/^[^"]*"([0-9]+).*$/\1/')"
if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "ERROR: Java $JAVA_MAJOR found; JDK 17 or newer is required." >&2
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "ERROR: Maven (mvn) was not found. Install Maven and add it to PATH." >&2
  exit 1
fi

echo "Running Campus CMS tests..."
exec mvn -B -ntp test "$@"
