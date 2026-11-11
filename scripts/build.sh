#!/usr/bin/env bash
# Compiles the project and builds the runnable jar: target/campus-cms.jar
#
# Usage:  scripts/build.sh [--with-tests]
#   (tests are skipped by default to keep builds fast; use scripts/test.sh or
#    --with-tests to run them)
#
# Needs JDK 17+ and Maven (mvn) on your PATH.

set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: 'java' not found. Install JDK 17 or newer and add it to PATH." >&2
  exit 1
fi

# Check the Java major version (first number inside the quotes of 'java -version').
JAVA_MAJOR="$(java -version 2>&1 | head -n 1 | sed -E 's/^[^"]*"([0-9]+).*$/\1/')"
if [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] && [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "ERROR: Java $JAVA_MAJOR found, but Java 17 or newer is required." >&2
  exit 1
fi

if command -v mvn >/dev/null 2>&1; then
  MVN=mvn
elif command -v mvn.cmd >/dev/null 2>&1; then
  MVN=mvn.cmd
else
  echo "ERROR: Maven ('mvn') not found. Install Maven and add its bin folder to PATH." >&2
  exit 1
fi

if [ "${1:-}" = "--with-tests" ]; then
  GOALS=(clean package)
else
  GOALS=(clean package -DskipTests)
fi

echo "Building with Maven ..."
if ! "$MVN" -B -ntp "${GOALS[@]}"; then
  echo "" >&2
  echo "BUILD FAILED. Read the first ERROR line above." >&2
  echo "If it says it cannot delete target/, close the running app and try again." >&2
  exit 1
fi

echo "Built: target/campus-cms.jar"
