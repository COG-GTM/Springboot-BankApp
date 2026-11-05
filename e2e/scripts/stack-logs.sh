#!/usr/bin/env bash
# Print the logs of the e2e stack (used by CI to archive them on failure).
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
"${COMPOSE[@]}" logs --no-color "$@"
