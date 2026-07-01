#!/usr/bin/env bash
# =============================================================================
# Compliance CI gate for control ITGC-SEC-06 (Credential & secrets management).
#   Regulations: PCI DSS 4.0 Req 8.6.2 / Req 2.2, SOX ICFR, GDPR Art. 32.
#
# Fails the build if committed configuration contains a hard-coded credential
# instead of an environment-injected ${ENV} placeholder. Lightweight: pure bash,
# no JVM/DB required, so it can run first as a fast pre-check in CI.
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CONFIG="$REPO_ROOT/src/main/resources/application.properties"

fail() { echo "FAIL [ITGC-SEC-06] $*" >&2; exit 1; }

[ -f "$CONFIG" ] || fail "config not found: $CONFIG"

# 1) The previously-committed DB password must never reappear.
if grep -q 'Test@123' "$CONFIG"; then
  fail "hard-coded DB password 'Test@123' found in application.properties"
fi

# 2) Any secret-bearing property must be a bare \${ENV} reference (no literal, no inline default).
#    Matches keys containing password/passwd/secret/token/apikey.
while IFS= read -r line; do
  key="${line%%=*}"
  val="${line#*=}"
  # trim whitespace
  val="$(echo "$val" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  if [[ ! "$val" =~ ^\$\{[A-Za-z0-9_.]+\}$ ]]; then
    fail "secret-bearing property '$key' is not a bare \${ENV} reference: '$val'"
  fi
done < <(grep -iE '^[[:space:]]*[^#][^=]*(password|passwd|secret|token|apikey|api[-_.]?key)[^=]*=' "$CONFIG" || true)

echo "PASS [ITGC-SEC-06] no committed credentials in application.properties"
