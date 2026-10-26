# E2E regression suite

Playwright automation of `qa/manual-test-plan.csv`. Each test title starts with its plan ID (`MT-001 ...`).
Rows that cannot be automated are listed with a reason in `qa/not-automated.csv`; `qa/coverage-matrix.md` is generated from both.

```bash
demo/start-demo.sh            # MySQL 8 + app on :8080
cd e2e && npm ci
npx playwright install chromium
npx playwright test           # BASE_URL overrides http://localhost:8080
npm run coverage              # regenerate qa/coverage-matrix.md
npm run coverage:check        # fail if any plan row is unaccounted for or the matrix is stale
```

CI: `.github/workflows/e2e.yml` runs the suite against a MySQL 8 service and posts the matrix with per-row results to the job summary.
Each test creates its own uniquely named customers, so the suite needs no DB reset and runs in parallel.
