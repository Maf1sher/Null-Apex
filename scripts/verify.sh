#!/usr/bin/env bash

set -uo pipefail

if (($# > 0)); then
    printf 'Usage: %s\n' "$0" >&2
    exit 2
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd -- "$SCRIPT_DIR/.." && pwd)"
LOG_FILE="$PROJECT_DIR/build/verification.log"

cd "$PROJECT_DIR"
mkdir -p "$(dirname "$LOG_FILE")"

printf 'Running Gradle build and unit tests (the game client will not be launched)...\n'

if ./gradlew build --console=plain >"$LOG_FILE" 2>&1; then
    printf '\nVerification passed: Gradle build succeeded.\n'

    shopt -s nullglob
    reports=("$PROJECT_DIR"/build/test-results/test/TEST-*.xml)
    if ((${#reports[@]} == 0)); then
        printf 'Unit tests: no JUnit reports found.\n'
    else
        test_count=0
        skipped_count=0
        failure_count=0
        error_count=0

        for report in "${reports[@]}"; do
            while IFS= read -r line; do
                [[ "$line" == *"<testsuite "* ]] || continue
                regex='tests="([0-9]+)"'
                if [[ "$line" =~ $regex ]]; then
                    test_count=$((test_count + BASH_REMATCH[1]))
                fi
                regex='skipped="([0-9]+)"'
                if [[ "$line" =~ $regex ]]; then
                    skipped_count=$((skipped_count + BASH_REMATCH[1]))
                fi
                regex='failures="([0-9]+)"'
                if [[ "$line" =~ $regex ]]; then
                    failure_count=$((failure_count + BASH_REMATCH[1]))
                fi
                regex='errors="([0-9]+)"'
                if [[ "$line" =~ $regex ]]; then
                    error_count=$((error_count + BASH_REMATCH[1]))
                fi
            done < "$report"
        done

        passed_count=$((test_count - skipped_count - failure_count - error_count))
        printf 'Unit tests: %d passed, %d skipped, %d failed.\n' \
            "$passed_count" "$skipped_count" "$((failure_count + error_count))"
    fi

    printf 'Full log: %s\n' "${LOG_FILE#"$PROJECT_DIR/"}"
    exit 0
else
    gradle_status=$?
fi

printf '\nVerification failed (Gradle exit code %d).\n' "$gradle_status" >&2
printf 'Relevant diagnostics:\n' >&2
grep -nE '(^> Task .* FAILED|What went wrong:|Execution failed for task|error:|There were failing tests|FAILURE: Build failed)' \
    "$LOG_FILE" | tail -n 25 >&2 || true
printf '\nLast 40 log lines:\n' >&2
tail -n 40 "$LOG_FILE" >&2
printf '\nFull log: %s\n' "${LOG_FILE#"$PROJECT_DIR/"}" >&2
exit "$gradle_status"
