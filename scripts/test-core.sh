#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
test_dir="$(mktemp -d -t punchy-core-XXXXXXXX)"
trap 'rm -rf -- "$test_dir"' EXIT
javac --release 17 -d "$test_dir" \
    "$project_dir/app/src/main/java/dev/jaycee/punchy/ProgressTracker.java" \
    "$project_dir/tests/ProgressTrackerTest.java"
java -cp "$test_dir" dev.jaycee.punchy.ProgressTrackerTest
