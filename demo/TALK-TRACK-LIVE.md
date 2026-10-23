# demo_qa live talk track

Situation: the bank has a 60-row manual regression plan run by hand every release. No automation exists. The presenter asks Devin, live, to automate it.

| Time | Beat | Live or pre-built |
|---|---|---|
| 0:00 | Show `qa/manual-test-plan.csv` on `demo/qa-live-before`. "60 rows, run by hand every release. There is no `e2e/` folder." | Live (GitHub) |
| 0:02 | Open a new Devin session, paste prompt 1 from `PROMPTS.md`. | Live |
| 0:04 | Devin reads the plan and the app, runs `demo/setup-demo.sh`, brings MySQL 8 and the app up. Point at the plan Devin writes. | Live |
| 0:05 to ~0:45 | While Devin works, walk the pre-built PR #328 so the room sees the destination. Check back on the live session every 10 minutes. | Pre-built fallback, live session running |
| ~0:45 | Live PR lands against `demo/qa-live-before`. Open the coverage matrix: every one of 60 rows has a status. | Live |
| ~0:47 | Wow moment: the `MT-047` row. "The plan says a negative transfer of 50 turns 100 into 150. Devin did not write a test that blesses that, and did not make up a new rule. Devin held the row and flagged the money bug for QA." | Live |
| ~0:50 | Optional prompt 2: planted regression. CI goes red on exactly the full-balance case. If time is short, show PR #329 instead. | Live or pre-built |

Hard case: `MT-047`. Devin must hold and escalate, not invent an expected result.

Fallback: if the live session stalls or the network misbehaves, `git checkout devin/fs-testing-1791848695-manual-to-automated && demo/setup-demo.sh` gives the finished state in about 5 minutes, and PRs #328 and #329 show the same outcome.

Honesty notes: the app is the open-source Springboot BankApp; the manual plan was written for this demo. Exact automated/not-automatable counts can differ by a row or two between live runs.
