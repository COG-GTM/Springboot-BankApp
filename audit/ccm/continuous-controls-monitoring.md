# Continuous Controls Monitoring (CCM)

Point-in-time, sample-based testing is the traditional audit model. CCM uses a **scheduled
Devin session** to re-run the automated control tests on a cadence, diff against the last
run, and escalate **only on change** — moving toward continuous, full-population assurance.

## Scope (automatable controls only)
The controls suitable for unattended re-testing each cycle:

| Control | What is re-checked each run |
|---|---|
| APP-TXN-01 | positive-amount validation still present on all monetary methods |
| APP-TXN-02 | transfer still executes within a transaction boundary |
| APP-ACC-04 | role/privilege model unchanged or improved |
| APP-SEC-05 | CSRF protection state on state-changing endpoints |
| ITGC-SEC-06 | no new secrets committed (secret scan over diff) |
| ITGC-CM-07 | every new change to protected branch had an independent approval |
| ITGC-SDLC-09 | scans + tests still gate; test coverage not regressed |
| ITGC-DATA-10 | no uncontrolled runtime schema mutation reintroduced |
| ITGC-LOG-11 | financial-event logging present |

## Schedule spec
- **Cadence:** nightly (or per-merge trigger on the protected branch).
- **Trigger:** scheduled Devin session (see the `managing-schedules` capability) or a CI/webhook trigger.
- **Prompt (pinned):**
  > Using `audit/racm.yaml`, re-run the automated control tests for the controls flagged
  > CCM-eligible. Compare results to the previous run. Escalate ONLY controls whose status
  > changed (improved or regressed). Produce a short delta report and update open findings.
- **Escalation:** post a delta report to the audit channel; open/refresh a finding for any
  regression; auto-close (pending auditor sign-off) any finding whose control now passes.

## Output each cycle
- A **delta report**: controls changed since last run (regressions first), with citations.
- Updated finding statuses (open / remediated / validated), each with fresh evidence.
- The session transcript, retained as the audit trail for that monitoring cycle.

## Guardrails
- Read-only against the auditee.
- No-change runs produce a one-line "no exceptions; no change" entry — no noise.
- Auditor signs off any status transition to "validated".
