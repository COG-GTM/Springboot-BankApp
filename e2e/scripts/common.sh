# Shared settings for the e2e helper scripts. Source, do not execute.
E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$E2E_DIR/.." && pwd)"

# The e2e stack runs as its own Compose project with project-scoped container
# names and its own host port so it can coexist with (and never tear down) a regular
# `docker compose up` of the root docker-compose.yml.
E2E_PROJECT="${E2E_COMPOSE_PROJECT:-bankapp-e2e}"
export E2E_APP_PORT="${E2E_APP_PORT:-8081}"

COMPOSE=(docker compose -p "$E2E_PROJECT" -f "$REPO_ROOT/docker-compose.yml" -f "$E2E_DIR/docker-compose.e2e.yml")
BASE_URL="${CYPRESS_BASE_URL:-http://localhost:$E2E_APP_PORT}"
DB_NAME="${E2E_DB_NAME:-BankDB}"

# The root docker-compose.yml takes no default database passwords. Unless they
# are already set, generate random ones once per stack and keep them in a
# git-ignored file so every helper script (up/seed/logs/down) uses the same.
E2E_DB_ENV_FILE="$E2E_DIR/.e2e-db.env"
if [ -z "${MYSQL_ROOT_PASSWORD:-}" ] || [ -z "${BANKAPP_DB_PASSWORD:-}" ]; then
  if [ ! -f "$E2E_DB_ENV_FILE" ]; then
    (
      umask 077
      printf 'MYSQL_ROOT_PASSWORD=%s\nBANKAPP_DB_PASSWORD=%s\n' \
        "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > "$E2E_DB_ENV_FILE"
    )
  fi
  set -a
  source "$E2E_DB_ENV_FILE"
  set +a
fi
export MYSQL_ROOT_PASSWORD BANKAPP_DB_PASSWORD
