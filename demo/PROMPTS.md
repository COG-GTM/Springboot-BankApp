# demo_qa prompts (live Devin)

Presenter sheet with goal, links, beats and which PR to use: `demo/PRESENTER.md`.

Before the meeting: run `demo/seed-situation.sh` from this kit branch and wait for `READY`.
In the meeting: open a NEW Devin session on `COG-GTM/Springboot-BankApp` and paste prompt 1 exactly.

## Prompt 1: manual plan to automated suite (live, about 17 min wall time measured)

```
Repo: COG-GTM/Springboot-BankApp, branch demo/qa-live-before. Branch off it and open your PR against demo/qa-live-before.

Here is our 60-row manual regression plan (qa/manual-test-plan.csv). Automate every row you responsibly can as Playwright tests that run in CI against MySQL 8, publish a coverage matrix that accounts for all 60 rows, and tell me which rows you could not automate and why. If a row's expected result is not testable, do not invent one.
```

Expected back:
- A PR against `demo/qa-live-before` with `e2e/` (Playwright, TypeScript), a GitHub Actions workflow running against a MySQL 8 service, and a coverage matrix accounting for all 60 rows.
- Roughly 54 rows automated, about 5 not automatable with a reason each (for example print, email, cross-browser or timing rows), and 1 row held.
- The hard case: `MT-047` (negative transfer). The plan's expected result is only "System handles it appropriately", and the app actually accepts `-50` and moves the sender from `$100` to `$150`. Devin reports that as a money-movement defect and holds the row for QA instead of writing a test that blesses the bug or inventing an expected result.

Exact counts can vary by a row or two between runs; the invariants are: all 60 rows accounted for, `MT-047` held and explained, no invented expected results.

## Prompt 2: planted regression (optional, run in the same session after prompt 1 finishes)

```
On a new branch off your PR branch, change AccountService so a customer cannot transfer their full balance (< 0 to <= 0), and open a PR against your PR branch. Tell me which plan rows CI flags.
```

Expected back: CI on that PR fails on exactly the full-balance transfer case (`MT-040` in the plan), and Devin says so.

## If the live run stalls

Switch to the pre-built fallback: `git checkout devin/fs-testing-1791848695-manual-to-automated && demo/setup-demo.sh`, then show PR #328 (suite) and PR #329 (planted regression).
