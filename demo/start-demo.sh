#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STARTED_AT="$(date +%s)"
JAR="$REPO_ROOT/target/bankapp-0.0.1-SNAPSHOT.jar"
APP_LOG="$REPO_ROOT/demo/logs/app.log"
MYSQL_TIMEOUT=90

ensure_mysql_image() {
  local primary_image="${DEMO_MYSQL_PRIMARY_IMAGE:-mysql:8.0}"
  local mirror_image="${DEMO_MYSQL_MIRROR_IMAGE:-mirror.gcr.io/library/mysql:8.0}"

  if docker pull "$primary_image"; then
    if [[ "$primary_image" != "mysql:8.0" ]]; then
      docker tag "$primary_image" mysql:8.0
    fi
    return 0
  fi

  printf 'Docker Hub pull failed, trying mirror.gcr.io/library/mysql:8.0\n'
  if docker pull "$mirror_image"; then
    docker tag "$mirror_image" mysql:8.0
    return 0
  fi

  if docker image inspect mysql:8.0 >/dev/null 2>&1; then
    printf 'Docker Hub and mirror pulls failed, using the cached mysql:8.0 image.\n'
    return 0
  fi

  printf 'FAILED: could not get mysql:8.0 from Docker Hub or mirror.gcr.io\n' >&2
  return 1
}

if [[ $# -gt 0 ]]; then
  if [[ $# -ne 1 || "$1" != "--build" ]]; then
    printf 'Usage: %s [--build]\n' "$0" >&2
    exit 2
  fi
  "$REPO_ROOT/mvnw" -B -q -s "$REPO_ROOT/demo/maven-settings.xml" -o -DskipTests package
elif [[ ! -f "$JAR" ]]; then
  printf 'jar missing, run demo/setup-demo.sh first\n' >&2
  exit 1
fi

pkill -f 'bankapp-0.0.1-SNAPSHOT.jar' || true
port_free=0
for _ in {1..15}; do
  if ! (echo > /dev/tcp/127.0.0.1/8080) >/dev/null 2>&1; then
    port_free=1
    break
  fi
  sleep 1
done
if [[ "$port_free" -ne 1 ]]; then
  printf 'FAILED: port 8080 did not become free within 15 seconds\n' >&2
  if [[ -f "$APP_LOG" ]]; then
    tail -n 40 "$APP_LOG" >&2
  fi
  exit 1
fi

if ! docker image inspect mysql:8.0 >/dev/null 2>&1; then
  ensure_mysql_image
fi

docker rm -f bankapp-mysql >/dev/null 2>&1 || true
docker run -d --name bankapp-mysql \
  -e MYSQL_ROOT_PASSWORD=Test@123 \
  -e MYSQL_DATABASE=bankappdb \
  -p 127.0.0.1:3306:3306 \
  mysql:8.0 \
  --default-authentication-plugin=mysql_native_password >/dev/null

mysql_ready=0
for _ in $(seq 1 "$MYSQL_TIMEOUT"); do
  if docker exec bankapp-mysql mysql -uroot -pTest@123 -e 'select 1' bankappdb >/dev/null 2>&1; then
    mysql_ready=1
    break
  fi
  sleep 1
done
if [[ "$mysql_ready" -ne 1 ]]; then
  printf 'FAILED: MySQL did not become ready within %s seconds\n' "$MYSQL_TIMEOUT" >&2
  docker logs --tail 40 bankapp-mysql >&2 || true
  exit 1
fi

mkdir -p "$REPO_ROOT/demo/logs"
(
  cd "$REPO_ROOT"
  SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/bankappdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
    setsid nohup java -jar "$JAR" > "$APP_LOG" 2>&1 < /dev/null &
  printf '%s\n' "$!" > "$REPO_ROOT/demo/logs/app.pid"
)

app_ready=0
for _ in $(seq 1 120); do
  if curl -sf -o /dev/null http://localhost:8080/login; then
    app_ready=1
    break
  fi
  sleep 1
done
if [[ "$app_ready" -ne 1 ]]; then
  printf 'FAILED: app did not serve /login within 120 seconds\n' >&2
  if [[ -f "$APP_LOG" ]]; then
    tail -n 40 "$APP_LOG" >&2
  fi
  exit 1
fi

register_demo_user() {
  local username="$1"
  local status
  status="$(curl -sS -o /dev/null -w '%{http_code}' -X POST \
    --data-urlencode "username=$username" \
    --data-urlencode 'password=Passw0rd!' \
    http://localhost:8080/register)"
  if [[ "$status" != "302" ]]; then
    printf 'FAILED: registration for %s returned HTTP %s, expected 302\n' "$username" "$status" >&2
    return 1
  fi
}

register_demo_user demo_sender
register_demo_user demo_payee

branch="$(git -C "$REPO_ROOT" branch --show-current)"
short_sha="$(git -C "$REPO_ROOT" rev-parse --short HEAD)"
elapsed="$(( $(date +%s) - STARTED_AT ))"
printf 'RESET OK: empty bankappdb, app up at http://localhost:8080 from %s@%s (%ss)\n' \
  "$branch" "$short_sha" "$elapsed"
