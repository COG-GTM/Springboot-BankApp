#!/usr/bin/env bash
# Stop the e2e stack and drop its database volume.
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
"${COMPOSE[@]}" down -v --remove-orphans
