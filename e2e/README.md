# Playwright regression suite

Automates `qa/manual-test-plan.csv`. `qa/coverage-matrix.csv` maps every plan row to its test or to the reason it stays manual; `scripts/check-coverage.mjs` fails CI if any row is missing, duplicated, or out of sync with the tests.

```bash
demo/start-demo.sh            # app on :8080 against MySQL 8
cd e2e && npm ci && npx playwright install chromium
npx playwright test           # BASE_URL overrides http://localhost:8080
node scripts/check-coverage.mjs
```

Each test registers its own unique customers, so tests run in parallel against a shared database. CI: `.github/workflows/e2e.yml` (MySQL 8 service container).
