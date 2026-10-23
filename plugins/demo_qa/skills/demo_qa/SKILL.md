---
name: demo_qa
description: Set up the live demo_qa situation (Springboot-BankApp with a 60-row manual test plan and no automated tests) and hand the presenter the exact prompts.
---

# demo_qa: live situation setup

Use when a presenter asks to set up, reset, or prepare demo_qa (manual regression plan to Playwright, FS Golden Demo Bank).

The presenter, not this setup, runs Devin live. Never pre-run the automation, never open a test PR against the seed branch, never merge.

## Steps
1. Clone `COG-GTM/Springboot-BankApp` and check out the kit branch `devin/1792723253-demo-qa-live`.
2. Run `demo/seed-situation.sh` and wait for `READY`. It force-resets ONLY `demo/qa-live-before` to the BEFORE state (the app, `qa/manual-test-plan.csv` with 60 rows, no `e2e/`, no CI test workflow) and closes PRs that targeted it. If the script prints anything other than `READY` at the end, stop and report the log.
3. Optional machine check: on `demo/qa-live-before`, run `demo/setup-demo.sh` and wait for `READY` (MySQL 8 plus the app). The live Devin session runs the same script itself.
4. Print for the presenter, verbatim from `demo/PROMPTS.md` on the kit branch: the seed branch URL `https://github.com/COG-GTM/Springboot-BankApp/tree/demo/qa-live-before`, prompt 1, optional prompt 2, the expected outcome, and the fallback. Point to `demo/TALK-TRACK-LIVE.md` for beats.

## What the presenter should see live (measured 16 min 32 s)
- A PR against `demo/qa-live-before`: about 54 rows automated as Playwright tests, green in CI against MySQL 8, and a coverage matrix accounting for all 60 rows.
- About 6 rows held manual, each with a reason (brand signoff, physical printer, JAWS, moderated session, missing notification feature).
- Hard case: MT-047 ("System handles it appropriately"). Devin does not invent an expected result; Devin shows that a negative transfer moves money the wrong way and hands the row to QA.
- Optional prompt 2: a one-character regression; CI flags exactly MT-040 (full-balance transfer).

## Fallback
`git checkout devin/fs-testing-1791848695-manual-to-automated && demo/setup-demo.sh`, then PRs #328 and #329.
