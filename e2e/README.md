# Playwright regression suite

Automates [`qa/manual-test-plan.csv`](../qa/manual-test-plan.csv). Each test title starts with its plan ID (`MT-0xx`), one test per row.
Rows that cannot be automated are listed with a reason in [`qa/not-automated.json`](../qa/not-automated.json).
[`qa/coverage-matrix.md`](../qa/coverage-matrix.md) accounts for all 60 rows and is regenerated on every CI run
(job summary + `e2e-report` artifact).

## Run locally

```bash
demo/start-demo.sh            # MySQL 8 in Docker + app on :8080
cd e2e
npm ci
npx playwright install chromium
npx playwright test           # BASE_URL defaults to http://localhost:8080
npm run matrix                # rewrite ../qa/coverage-matrix.md with these results
```

`npm run matrix:check` asks Playwright for the test list (`--list`) and fails if a plan row is neither automated nor listed in `qa/not-automated.json`, is in both, has two tests, or its test is `test.skip` / `test.fixme` / `test.fail`. With results (`npm run matrix`) it also fails on tests skipped at runtime.

Tests register their own unique users, so they run in parallel and do not need a clean database.
The Deposit/Withdraw/Transfer panels use Bootstrap from public CDNs, so the browser needs internet access.

## Adding or changing a row

1. Add the row to `qa/manual-test-plan.csv`.
2. Either add `test('MT-0xx <title>', ...)` to the matching spec, or add an entry with a reason to `qa/not-automated.json`.
   Do not make up an expected result the plan does not state.
3. `npm run matrix:check`.
