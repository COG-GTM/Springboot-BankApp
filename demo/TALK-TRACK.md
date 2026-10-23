# Talk track: Manual QA plan to automated Playwright suite

Audience: engineering and QA leadership. Running time: about 8 minutes.
Every number below was measured on this repo. If a number on screen differs, say the on-screen number.

## Before the meeting (once per machine)

```bash
demo/setup-demo.sh      # fresh machine to presenter-ready, ends with "READY: ..."
```

Windows setup: `.\demo\setup-demo.ps1`. Reset with `.\demo\start-demo.ps1`.

Setup installs JDK 17, Node 20, Docker and the `mysql:8.0` image, configures a Maven mirror, pre-builds the app, installs Playwright Chromium, runs the full suite once as a smoke check and leaves an empty database.
Open these tabs in advance, so CI is never triggered live:

- PR #328 checks (green): https://github.com/COG-GTM/Springboot-BankApp/pull/328/checks
- PR #329 checks (red, intended): https://github.com/COG-GTM/Springboot-BankApp/pull/329/checks

## Reset (before every run, about 15 s)

```bash
demo/start-demo.sh
```

This recreates the MySQL container, starts the app detached (log in `demo/logs/app.log`), waits for the login page and seeds `demo_sender` and `demo_payee` (password `Passw0rd!`) for the hard case. It prints `RESET OK` or fails loudly.

## Beat 1: the starting point (1 min)

Show: `qa/manual-test-plan.csv` in the editor.
Say: "This is the bank's 60-row manual regression plan. Devin turned every row into a tracked outcome. Today a tester clicks through these by hand before every release."

## Beat 2: the wow moment (2 min)

Run:

```bash
cd e2e && npx playwright test
npm run matrix
npx playwright show-report
```

Show: the terminal ending in `54 passed` (about 18 s locally), then `qa/coverage-matrix.md`, then the HTML report with one transfer test opened.
Say: "60 rows in, 60 rows accounted for. 54 are now browser tests that pass in under 20 seconds. 5 can't be automated: brand review, a live notification integration, a physical printer, a JAWS screen reader with a human listener, and a usability study. Each one has a written reason. 1 is held for QA, and I'll come back to it. Nothing is unmapped. Every test title starts with its plan ID, so a failure points straight back at the row a tester wrote."

## Beat 3: the suite runs on every pull request (1 min)

Show: the PR #328 checks tab (green, already run). Do not re-run CI.
Say: "The same suite runs in GitHub Actions against MySQL 8 on every pull request. This is Devin's PR: 54 passed, and the coverage matrix is published in the job summary."

## Beat 4: the suite catches a planted bug (2 min)

Show: PR #329 "Files changed": a single character in `AccountService.java`, `< 0` became `<= 0`. Then the PR #329 checks tab (red, already run).
Say: "We planted a realistic one-character bug: a customer can no longer transfer their full balance. The suite flags exactly one test, MT-040, 'customer can transfer the full balance'. The other 53 still pass. The reviewer sees which manual row broke without reading any test code."

Optional local proof (about 1 min, needs the regression branch checked out):

```bash
git checkout devin/fs-testing-1791848695-planted-regression
demo/start-demo.sh --build && (cd e2e && npx playwright test)   # 1 failed (MT-040), 53 passed
git checkout devin/fs-testing-1791848695-manual-to-automated && demo/start-demo.sh --build
```

## Beat 5: the hard case, MT-047 (1.5 min)

Show: the browser at http://localhost:8080. Log in as `demo_sender` / `Passw0rd!`, deposit 100, then transfer `-50` to `demo_payee`.
On screen: the balance goes from $100.00 to $150.00 with no error.
Say: "Row MT-047 says 'enter a negative amount; the system handles it appropriately'. That isn't a testable expected result. While automating it, Devin found that the app accepts a negative transfer and the sender's balance goes up. That is a real money bug. Devin didn't invent an expected result and didn't write a test that locks in either behavior. The case is held for QA with the question written down: reject with a message, block it in the field, or both. We count it as a finding, not as coverage."

## If asked "what else would the suite catch?"

"Any change that breaks one of the 54 automated plan rows: 13 authentication, 7 deposit, 7 withdraw, 11 transfer, 6 transaction history, 7 dashboard and 3 cross-cutting. Each failure names the plan row it guards."

## Expected numbers

| Item | Value |
| --- | --- |
| Manual plan rows | 60 |
| Automated, passing | 54 / 54 |
| Not automatable (with reasons) | 5 |
| Held for QA | 1 (MT-047) |
| Unmapped | 0 |
| Planted regression | exactly MT-040 fails, 53 pass |
| Reset | about 15 s |
| Local suite run | about 18 s |
| CI suite run | about 36 s, about 2.5 min per job |
