#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
exec mvn -B -Pmysql-performance verify \
  -Dperf.accounts="${PERF_ACCOUNTS:-100000}" \
  -Dperf.transactions="${PERF_TRANSACTIONS:-1000000}" \
  -Dperf.samples="${PERF_SAMPLES:-21}" \
  -Dperf.warmup="${PERF_WARMUP:-5}" "$@"
