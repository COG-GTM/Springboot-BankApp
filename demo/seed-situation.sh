#!/usr/bin/env bash
# demo_qa: (re)create the BEFORE branch a presenter hands to a live Devin session.
# BEFORE = upstream app + the 60-row manual plan, with no e2e/, no CI workflow, no coverage matrix.
# Safe to rerun: force-resets ONLY the seed branch and closes PRs that targeted it.
set -eEuo pipefail

REPO="${DEMO_QA_REPO:-COG-GTM/Springboot-BankApp}"
SEED_BRANCH="${DEMO_QA_SEED_BRANCH:-demo/qa-live-before}"
BASE_SHA=305826d755587d70eb24a7076eaf0797743548b3   # upstream app, before any automation
PLAN_SHA=75d6e9b82f61a28e7b4c8939bcd0df075790dea4   # commit that introduced qa/manual-test-plan.csv
FALLBACK_BRANCH=devin/fs-testing-1791848695-manual-to-automated
KIT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CURRENT_STEP="initializing"
WORKTREE=""

trap 'status=$?; printf "SEED FAILED: %s (exit %s)\n" "$CURRENT_STEP" "$status" >&2; exit "$status"' ERR
trap '[[ -n "$WORKTREE" ]] && git -C "$KIT_ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1; true' EXIT
step() { CURRENT_STEP="$1"; printf '==> %s\n' "$1"; }
fail() { CURRENT_STEP="$1"; false; }

case "$SEED_BRANCH" in main|master|devin/fs-testing-*) fail "refusing to reset protected branch $SEED_BRANCH";; esac

step "Tools"
for t in git gh; do command -v "$t" >/dev/null || fail "$t not installed"; done
gh auth status >/dev/null 2>&1 || fail "gh not authenticated"

step "Fetch $REPO"
git -C "$KIT_ROOT" fetch -q origin
git -C "$KIT_ROOT" cat-file -e "$BASE_SHA^{commit}" || fail "base commit $BASE_SHA not found"
git -C "$KIT_ROOT" cat-file -e "$PLAN_SHA^{commit}" || fail "plan commit $PLAN_SHA not found"

step "Close PRs and delete branches from earlier live runs against $SEED_BRANCH"
while read -r num head; do
  [[ -z "$num" ]] && continue
  gh pr close "$num" -R "$REPO" --comment "Closed by demo/seed-situation.sh: demo_qa seed branch was reset." >/dev/null
  printf '    closed PR #%s (%s)\n' "$num" "$head"
  case "$head" in devin/*) git -C "$KIT_ROOT" push -q origin --delete "$head" 2>/dev/null && printf '    deleted branch %s\n' "$head" || true;; esac
done < <(gh pr list -R "$REPO" --base "$SEED_BRANCH" --state open --json number,headRefName --jq '.[] | "\(.number) \(.headRefName)"')

step "Build BEFORE tree from $BASE_SHA"
WORKTREE="$(mktemp -d)/seed"
git -C "$KIT_ROOT" worktree add -q --detach "$WORKTREE" "$BASE_SHA"
cd "$WORKTREE"
mkdir -p qa demo
git show "$PLAN_SHA:qa/manual-test-plan.csv" > qa/manual-test-plan.csv
cp "$KIT_ROOT/demo/live/setup-demo.sh" demo/setup-demo.sh
cp "$KIT_ROOT/demo/start-demo.sh" "$KIT_ROOT/demo/maven-settings.xml" demo/
cp "$KIT_ROOT/demo/live/AGENTS.md" AGENTS.md
cp "$KIT_ROOT/mvnw" "$KIT_ROOT/mvnw.cmd" .
cp "$KIT_ROOT/.gitignore" .gitignore
chmod +x demo/setup-demo.sh demo/start-demo.sh mvnw
git add -A
git -c commit.gpgsign=false commit -q -m "chore: seed demo_qa BEFORE state (60-row manual plan, no automation)"

step "Verify BEFORE state"
rows="$(tail -n +2 qa/manual-test-plan.csv | grep -c '^MT-')"
[[ "$rows" == "60" ]] || fail "expected 60 manual rows, found $rows"
grep -q '^MT-047,' qa/manual-test-plan.csv || fail "hard case MT-047 missing from plan"
for p in e2e .github/workflows/e2e.yml qa/coverage-matrix.md qa/not-automated.json; do
  [[ ! -e "$p" ]] || fail "AFTER artifact present in seed: $p"
done

step "Push $SEED_BRANCH"
git push -q --force origin "HEAD:refs/heads/$SEED_BRANCH"
remote_sha="$(git ls-remote origin "refs/heads/$SEED_BRANCH" | cut -f1)"
[[ "$remote_sha" == "$(git rev-parse HEAD)" ]] || fail "remote $SEED_BRANCH does not match seed commit"

printf '%s\n' \
  '==================================================================' \
  'READY: demo_qa seeded (BEFORE state, Devin has not run yet)' \
  "Seed branch:  https://github.com/$REPO/tree/$SEED_BRANCH (${remote_sha:0:7})" \
  "Prompt:       demo/PROMPTS.md, prompt 1 (open a NEW Devin session on $REPO)" \
  "Fallback:     git checkout $FALLBACK_BRANCH && demo/setup-demo.sh" \
  "              pre-built PRs: https://github.com/$REPO/pull/328 (suite), /pull/329 (planted regression)" \
  '=================================================================='
