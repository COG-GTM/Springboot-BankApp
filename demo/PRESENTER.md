# demo_qa presenter sheet: manual test plan to Playwright

**Goal:** turn the bank's 60-row manual regression plan into a Playwright suite that runs in CI on MySQL 8, live, in about 17 minutes. Every row is accounted for. Devin does not invent an expected result for MT-047 (negative transfer); it flags the money bug and holds the row for QA.

## Links
| What | Link |
|---|---|
| **Live session (already started with prompt 1)** | the session link your setup session sent you (the setup session starts it with prompt 1) |
| The 60-row plan | https://github.com/COG-GTM/Springboot-BankApp/blob/demo/qa-live-before/qa/manual-test-plan.csv |
| MT-047, the hard case (line 48) | https://github.com/COG-GTM/Springboot-BankApp/blob/demo/qa-live-before/qa/manual-test-plan.csv#L48 |
| BEFORE and AFTER videos (kit PR #335 description, does not close on reseed) | https://github.com/COG-GTM/Springboot-BankApp/pull/335 |
| Pre-built suite: 54 automated, CI green | https://github.com/COG-GTM/Springboot-BankApp/pull/328 |
| Pre-built coverage matrix (MT-047 row) | https://github.com/COG-GTM/Springboot-BankApp/blob/devin/fs-testing-1791848695-manual-to-automated/qa/coverage-matrix.md |
| Pre-built planted regression: CI red on MT-040 only, 53 pass | https://github.com/COG-GTM/Springboot-BankApp/pull/329 |

## Beats
| Time | Beat | Use |
|---|---|---|
| 0:00 | "60 rows, run by hand every release. There is no `e2e/` folder." Open the plan. | The 60-row plan |
| 0:01 | Optional: show the app working before any automation, and the AFTER clip of the 54-test suite running. | BEFORE / AFTER videos on #335 |
| 0:02 | Open the live session. Prompt 1 is already in it as the first message; show it to the room. | Live session |
| 0:04 | Devin reads the plan and the app, brings up MySQL 8 and the app. Point at its plan. | Live session |
| 0:05 to ~0:17 | Read MT-047 aloud: "System handles it appropriately." Ask the room what a tester would assert. Check back every few minutes. | MT-047 link |
| ~0:17 | Live PR lands against `demo/qa-live-before`. Open the coverage matrix: all 60 rows have a status. | Live PR (from the session). Running late: #328 |
| ~0:18 | **Wow moment:** the MT-047 row. "A minus 50 transfer moves money the wrong way, the sender gains 50. Devin did not write a test that blesses that, and did not make up a rule. It held the row and flagged the bug for QA." | Live PR matrix. Pre-built: coverage matrix link |
| ~0:20 to ~0:26 | Optional prompt 2 in the same session. CI goes red on exactly MT-040, the other 53 stay green. Devin asks keep the rule or drop the change; answer "drop it". | Live stacked PR. Short on time: #329 |

## Prompt 1 (already sent to the live session as its first message)
```
Repo: COG-GTM/Springboot-BankApp, branch demo/qa-live-before. Branch off it and open your PR against demo/qa-live-before.

Here is our 60-row manual regression plan (qa/manual-test-plan.csv). Automate every row you responsibly can as Playwright tests that run in CI against MySQL 8, publish a coverage matrix that accounts for all 60 rows, and tell me which rows you could not automate and why. If a row's expected result is not testable, do not invent one.
```
Expect: about 54 automated, about 5 not automatable with a reason each, MT-047 held. Counts can move by a row or two; all 60 accounted for and MT-047 held must always be true.

## Prompt 2 (optional, same session, after prompt 1's PR is up)
```
On a new branch off your PR branch, change AccountService so a customer cannot transfer their full balance (< 0 to <= 0), and open a PR against your PR branch. Tell me which plan rows CI flags.
```
Expect: CI red about 2.5 min after the PR opens, on MT-040 only.

## If the live run stalls
Show #328 (suite, green) and #329 (planted regression, red on MT-040 only). To run the finished state locally: `git checkout devin/fs-testing-1791848695-manual-to-automated && demo/setup-demo.sh`.

Honesty notes: the app is the open-source Springboot BankApp; the plan was written for this demo.
