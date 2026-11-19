#!/usr/bin/env bash
# Stop the e2e stack, drop its database volume and its generated passwords.
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
"${COMPOSE[@]}" down -v --remove-orphans
rm -f "$E2E_DB_ENV_FILE"
