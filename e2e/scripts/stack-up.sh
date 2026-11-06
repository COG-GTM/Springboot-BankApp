#!/usr/bin/env bash
# Build the application image from the working tree and start app + MySQL.
# Blocks until both containers report healthy, then waits for /login to answer.
# Set E2E_SKIP_BUILD=1 to start an already built bankapp:e2e image instead.
set -euo pipefail
source "$(dirname "$0")/common.sh"

cd "$REPO_ROOT"
UP_ARGS=(up -d --wait --wait-timeout 300)
if [ "${E2E_SKIP_BUILD:-0}" != "1" ]; then
  UP_ARGS+=(--build)
fi
"${COMPOSE[@]}" "${UP_ARGS[@]}"

for i in $(seq 1 60); do
  if curl -fsS -o /dev/null "$BASE_URL/login"; then
    echo "App is up at $BASE_URL"
    exit 0
  fi
  sleep 2
done

echo "App did not answer on $BASE_URL/login" >&2
"${COMPOSE[@]}" logs --tail=100 mainapp >&2
exit 1
