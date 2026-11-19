#!/usr/bin/env bash
# Load e2e/seed/seed.sql into the running MySQL container. Idempotent.
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
# MYSQL_PWD is read from the container's own environment, so the password is
# neither on this host's command line nor in the container's process list.
"${COMPOSE[@]}" exec -T mysql \
  sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql --user=root "$0"' "$DB_NAME" < "$E2E_DIR/seed/seed.sql"
echo "Seed data loaded into $DB_NAME"
