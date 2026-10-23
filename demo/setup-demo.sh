#!/usr/bin/env bash
set -eEuo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STARTED_AT="$(date +%s)"
CURRENT_STEP="initializing"
TMP_DIR=""
MATRIX_BACKUP=""

on_error() {
  local status=$?
  printf 'SETUP FAILED: %s (exit %s)\n' "$CURRENT_STEP" "$status" >&2
  exit "$status"
}

on_exit() {
  local status=$?
  if [[ -n "$MATRIX_BACKUP" && -f "$MATRIX_BACKUP" ]]; then
    cp -p "$MATRIX_BACKUP" "$REPO_ROOT/qa/coverage-matrix.md" || status=$?
  fi
  if [[ -n "$TMP_DIR" && -d "$TMP_DIR" ]]; then
    rm -rf "$TMP_DIR" || status=$?
  fi
  exit "$status"
}

trap on_error ERR
trap on_exit EXIT

cd "$REPO_ROOT"

CURRENT_STEP="JDK 17"
printf '==> %s\n' "$CURRENT_STEP"
java_major=0
if command -v java >/dev/null 2>&1; then
  java_major="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
  java_major="${java_major:-0}"
fi
if (( java_major < 17 )); then
  sudo apt-get install -y openjdk-17-jdk-headless
fi
java_major="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
if (( java_major < 17 )); then
  CURRENT_STEP="JDK 17 verification"
  false
fi

CURRENT_STEP="Node 20"
printf '==> %s\n' "$CURRENT_STEP"
node_major=0
if command -v node >/dev/null 2>&1; then
  node_major="$(node -p 'process.versions.node.split(".")[0]')"
fi
if [[ "$node_major" != "20" ]]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi
node_major="$(node -p 'process.versions.node.split(".")[0]')"
if [[ "$node_major" != "20" ]]; then
  CURRENT_STEP="Node 20 verification"
  false
fi

CURRENT_STEP="Docker and MySQL image"
printf '==> %s\n' "$CURRENT_STEP"
if ! command -v docker >/dev/null 2>&1; then
  sudo apt-get install -y docker.io
  sudo systemctl start docker || sudo service docker start
fi
if ! docker info >/dev/null 2>&1; then
  CURRENT_STEP="Docker daemon unavailable"
  false
fi
if ! docker pull mysql:8.0; then
  if ! docker image inspect mysql:8.0 >/dev/null 2>&1; then
    CURRENT_STEP="MySQL image pull"
    false
  fi
  printf 'MySQL image pull failed, using the cached mysql:8.0 image.\n'
fi

CURRENT_STEP="Maven mirror"
printf '==> %s\n' "$CURRENT_STEP"
mkdir -p "$HOME/.m2"
settings="$HOME/.m2/settings.xml"
if [[ ! -f "$settings" ]]; then
  printf 'Maven Central rate-limits cloud VMs, configuring the GCS mirror.\n'
  cat > "$settings" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>gcs-central</id>
      <name>Google Cloud Storage Maven Central mirror</name>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
elif ! grep -q '<mirror>' "$settings"; then
  printf 'Maven Central rate-limits cloud VMs, backing up settings and configuring the GCS mirror.\n'
  cp "$settings" "$settings.bak"
  cat > "$settings" <<'EOF'
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>gcs-central</id>
      <name>Google Cloud Storage Maven Central mirror</name>
      <url>https://maven-central.storage-download.googleapis.com/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
fi
export MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2

CURRENT_STEP="Maven dependency pre-warm"
printf '==> %s\n' "$CURRENT_STEP"
./mvnw -B -q dependency:go-offline

CURRENT_STEP="Maven package build"
printf '==> %s\n' "$CURRENT_STEP"
./mvnw -B -q -DskipTests package
if [[ ! -f target/bankapp-0.0.1-SNAPSHOT.jar ]]; then
  CURRENT_STEP="Maven package verification"
  false
fi

CURRENT_STEP="Maven offline package build"
printf '==> %s\n' "$CURRENT_STEP"
./mvnw -B -q -o -DskipTests package

CURRENT_STEP="Playwright dependencies"
printf '==> %s\n' "$CURRENT_STEP"
(
  cd "$REPO_ROOT/e2e"
  npm ci
  npx playwright install --with-deps chromium
)

CURRENT_STEP="Initial demo reset"
printf '==> %s\n' "$CURRENT_STEP"
"$REPO_ROOT/demo/start-demo.sh"

CURRENT_STEP="Playwright smoke suite"
printf '==> %s\n' "$CURRENT_STEP"
(
  cd "$REPO_ROOT/e2e"
  PLAYWRIGHT_JSON_OUTPUT_NAME=results.json npx playwright test --reporter=dot,html,json
)

CURRENT_STEP="Playwright result count"
printf '==> %s\n' "$CURRENT_STEP"
node -e 'const stats = require("./e2e/results.json").stats; if (stats.expected !== 54 || stats.unexpected !== 0) { console.error("Expected 54 passed and 0 failed, got " + stats.expected + " passed and " + stats.unexpected + " failed"); process.exit(1); } console.log("Playwright results: " + stats.expected + " passed, " + stats.unexpected + " failed");'

CURRENT_STEP="Coverage matrix generation"
printf '==> %s\n' "$CURRENT_STEP"
TMP_DIR="$(mktemp -d)"
MATRIX_BACKUP="$TMP_DIR/coverage-matrix.md"
cp -p "$REPO_ROOT/qa/coverage-matrix.md" "$MATRIX_BACKUP"
(
  cd "$REPO_ROOT/e2e"
  npm run matrix
)
grep -Fxq -- '- Unmapped: 0' "$REPO_ROOT/qa/coverage-matrix.md"
grep -Fxq -- '- Manual cases: 60' "$REPO_ROOT/qa/coverage-matrix.md"
if ! git -C "$REPO_ROOT" diff --quiet HEAD -- qa/coverage-matrix.md; then
  CURRENT_STEP="Coverage matrix comparison"
  false
fi

CURRENT_STEP="Final demo reset"
printf '==> %s\n' "$CURRENT_STEP"
"$REPO_ROOT/demo/start-demo.sh"

elapsed="$(( $(date +%s) - STARTED_AT ))"
printf 'Setup completed in %ss.\n' "$elapsed"
printf '%s\n' \
  '==================================================' \
  'READY: Manual QA plan to automated Playwright suite' \
  'First command:  cd e2e && npx playwright test' \
  'First line:     "This is the bank'\''s 60-row manual regression plan. Devin turned every row into a tracked outcome."' \
  '=================================================='
