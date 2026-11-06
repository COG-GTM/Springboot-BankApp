#!/usr/bin/env bash
# Load e2e/seed/seed.sql into the running MySQL container. Idempotent.
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
"${COMPOSE[@]}" exec -T mysql \
  mysql --user=root --password="$DB_PASSWORD" "$DB_NAME" < "$E2E_DIR/seed/seed.sql"
echo "Seed data loaded into $DB_NAME"
