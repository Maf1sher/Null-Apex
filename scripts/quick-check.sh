#!/usr/bin/env bash

set -uo pipefail

if (($# > 0)); then
    printf 'Usage: %s\n' "$0" >&2
    exit 2
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_DIR/build/quick-check.log"

cd "$PROJECT_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

printf 'Running quick Java compilation (tests and game client will not be launched)...\n'

if ./gradlew compileJava --console=plain >"$LOG_FILE" 2>&1; then
    printf '\nQuick check passed: Java compilation succeeded.\n'
    printf 'Full log: %s\n' "${LOG_FILE#"$PROJECT_DIR/"}"
    exit 0
else
    gradle_status=$?
fi

printf '\nQuick check failed (Gradle exit code %d).\n' "$gradle_status" >&2
printf 'Relevant diagnostics:\n' >&2
grep -nE '(^> Task .* FAILED|What went wrong:|Execution failed for task|error:|FAILURE: Build failed)' \
    "$LOG_FILE" | tail -n 25 >&2 || true
printf '\nLast 30 log lines:\n' >&2
tail -n 30 "$LOG_FILE" >&2
printf '\nFull log: %s\n' "${LOG_FILE#"$PROJECT_DIR/"}" >&2
exit "$gradle_status"
