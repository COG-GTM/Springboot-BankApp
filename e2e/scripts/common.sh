# Shared settings for the e2e helper scripts. Source, do not execute.
E2E_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$E2E_DIR/.." && pwd)"
COMPOSE=(docker compose -f "$REPO_ROOT/docker-compose.yml" -f "$E2E_DIR/docker-compose.e2e.yml")
BASE_URL="${CYPRESS_BASE_URL:-http://localhost:8080}"
DB_NAME="${E2E_DB_NAME:-BankDB}"
DB_PASSWORD="${E2E_DB_ROOT_PASSWORD:-Test@123}"
