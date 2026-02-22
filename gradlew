#!/usr/bin/env sh
set -eu

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

echo "Error: Gradle is not installed and this repository cannot include binary wrapper artifacts in PRs." >&2
echo "Install Gradle 8.2+ and re-run this command." >&2
exit 1
