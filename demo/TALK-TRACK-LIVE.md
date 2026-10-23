# demo_qa live talk track

Situation: the bank has a 60-row manual regression plan run by hand every release. No automation exists. The presenter asks Devin, live, to automate it.

| Time | Beat | Live or pre-built |
|---|---|---|
| 0:00 | Show `qa/manual-test-plan.csv` on `demo/qa-live-before`. "60 rows, run by hand every release. There is no `e2e/` folder." | Live (GitHub) |
| 0:02 | Open a new Devin session, paste prompt 1 from `PROMPTS.md`. | Live |
| 0:04 | Devin reads the plan and the app, runs `demo/setup-demo.sh`, brings MySQL 8 and the app up. Point at the plan Devin writes. | Live |
| 0:05 to ~0:17 | While Devin works, open `qa/manual-test-plan.csv` and read MT-047 aloud: "System handles it appropriately." Ask the room what a tester would assert. Check back every few minutes. | Live session running |
| ~0:17 | Live PR lands (measured 16 min 32 s from paste) against `demo/qa-live-before`. Open the coverage matrix: every one of 60 rows has a status. | Live |
| ~0:18 | Wow moment: the `MT-047` row. "The plan only says the system handles a negative transfer appropriately. Devin tried it: a minus 50 transfer moves money the wrong way, the sender gains 50. Devin did not write a test that blesses that, and did not make up a new rule. Devin held the row and flagged the money bug for QA." | Live |
| ~0:20 to ~0:26 | Optional prompt 2 (about 5 min when CI is not flaky; CI itself takes about 2.5 min): planted regression on a stacked PR. CI goes red on exactly MT-040, the full-balance transfer, and the other 53 rows stay green. Hard case: Devin does not edit MT-040 to make CI pass. Devin asks whether to keep the new rule (and update MT-040) or drop the change. Answer "drop it". If time is short, show PR #329 (pre-built, red on MT-040 only). Live-run PRs like #332 and #336 close when the seed is reset. | Live or pre-built |

Hard case: `MT-047`. Devin must hold and escalate, not invent an expected result.

Fallback: if the live session stalls or the network misbehaves, `git checkout devin/fs-testing-1791848695-manual-to-automated && demo/setup-demo.sh` gives the finished state in about 5 minutes, and PRs #328 and #329 show the same outcome.

Honesty notes: the app is the open-source Springboot BankApp; the manual plan was written for this demo. Exact automated/not-automatable counts can differ by a row or two between live runs.
