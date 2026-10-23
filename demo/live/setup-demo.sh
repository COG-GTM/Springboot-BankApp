#!/usr/bin/env bash
# demo_qa BEFORE-state setup. Installed into the seed branch as demo/setup-demo.sh.
# Prepares a machine so Devin can build, run and Playwright-test the app with no network surprises.
set -eEuo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STARTED_AT="$(date +%s)"
CURRENT_STEP="initializing"
PLAYWRIGHT_VERSION="1.52.0"

trap 'status=$?; printf "SETUP FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
cd "$REPO_ROOT"

step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

ensure_mysql_image() {
  if docker pull mysql:8.0; then return 0; fi
  printf 'Docker Hub pull failed, trying mirror.gcr.io/library/mysql:8.0\n'
  if docker pull mirror.gcr.io/library/mysql:8.0; then docker tag mirror.gcr.io/library/mysql:8.0 mysql:8.0; return 0; fi
  docker image inspect mysql:8.0 >/dev/null 2>&1 && { printf 'Using cached mysql:8.0 image.\n'; return 0; }
  printf 'FAILED: could not get mysql:8.0 from Docker Hub or mirror.gcr.io\n' >&2
  return 1
}

jdk_major() {
  local java_v javac_v
  command -v java >/dev/null 2>&1 && command -v javac >/dev/null 2>&1 || { echo 0; return; }
  java_v="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
  javac_v="$(javac -version 2>&1 | sed -n 's/^javac \([0-9]*\).*/\1/p')"
  java_v="${java_v:-0}"; javac_v="${javac_v:-0}"
  echo $(( java_v < javac_v ? java_v : javac_v ))
}

step "Plan present, automation absent"
[[ -f qa/manual-test-plan.csv ]] || fail "qa/manual-test-plan.csv missing"
rows="$(tail -n +2 qa/manual-test-plan.csv | grep -c '^MT-')"
[[ "$rows" == "60" ]] || fail "expected 60 manual rows, found $rows"

step "JDK 17"
if (( $(jdk_major) < 17 )); then sudo apt-get install -y openjdk-17-jdk-headless; fi
(( $(jdk_major) >= 17 )) || fail "JDK 17 verification (need java and javac)"

step "Node 20"
node_major=0
command -v node >/dev/null 2>&1 && node_major="$(node -p 'process.versions.node.split(".")[0]')"
if [[ "$node_major" != "20" ]]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi
[[ "$(node -p 'process.versions.node.split(".")[0]')" == "20" ]] || fail "Node 20 verification"

step "Docker and MySQL 8 image"
if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get install -y docker.io
  sudo systemctl start docker || sudo service docker start
fi
docker info >/dev/null 2>&1 || fail "Docker daemon unavailable"
ensure_mysql_image

step "Maven dependencies (GCS mirror, Maven Central rate-limits cloud VMs)"
export MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2
./mvnw -B -q -s demo/maven-settings.xml dependency:go-offline
./mvnw -B -q -s demo/maven-settings.xml -DskipTests package
[[ -f target/bankapp-0.0.1-SNAPSHOT.jar ]] || fail "Maven package verification"

step "Playwright ${PLAYWRIGHT_VERSION} Chromium and OS deps (warm cache)"
npm cache add "@playwright/test@${PLAYWRIGHT_VERSION}" typescript@5 @types/node@20 >/dev/null
npx -y "playwright@${PLAYWRIGHT_VERSION}" install --with-deps chromium

step "App reset (empty bankappdb, app on :8080)"
demo/start-demo.sh
curl -sf -o /dev/null http://localhost:8080/login || fail "app not answering on :8080"

step "Seed is still the BEFORE state"
[[ ! -e e2e ]] || fail "e2e/ exists; this is not the BEFORE state, rerun demo/seed-situation.sh"

branch="$(git rev-parse --abbrev-ref HEAD)"
printf 'Setup completed in %ss.\n' "$(( $(date +%s) - STARTED_AT ))"
printf '%s\n' \
  '==================================================' \
  'READY: demo_qa (manual QA plan, no automation yet)' \
  "Branch:   ${branch}" \
  'App:      http://localhost:8080 (reset any time: demo/start-demo.sh)' \
  'Plan:     qa/manual-test-plan.csv (60 rows)' \
  '=================================================='
