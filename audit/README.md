# Devin for Group Audit — Demo Kit

A self-contained demonstration of how Devin supports an **internal audit (third line of
defence)** function: an **audit ontology** (Risk & Control Matrix), **control testing**
(ITGC + application controls + code review as a continuous control), and **audit
reporting** — all grounded in this banking application as the auditee.

> **Thesis.** Devin is the *execution & evidence layer* of internal audit. Under the
> auditor's direction and sign-off, it plans testing from a machine-readable control
> catalogue, executes the tests, re-performs key calculations, reviews code changes as a
> continuous control, and drafts workpapers & findings with full traceability. It does
> **not** replace the auditor's judgement, independence, or sign-off.

## What's in this kit

| Path | What it is |
|---|---|
| `racm.yaml` | **The audit ontology** — Risk & Control Matrix: entities, risks, 11 controls, regulations, test procedures, expected findings |
| `ontology.md` | The ontology model explained (entity → risk → control → test → evidence → finding → action) |
| `playbooks/01-audit-planning-coverage.md` | Coverage mapping + fieldwork test plan (read-only scoping) |
| `playbooks/02-itgc-testing.md` | ITGC testing over the full population (change mgmt, access, SDLC, secrets, data, logging) |
| `playbooks/03-application-control-reperformance.md` | Re-perform transaction/transfer/authorisation controls |
| `playbooks/04-findings-and-reporting.md` | 5-Cs findings, roll-up reporting, remediation validation |
| `templates/workpaper-template.md` | Per-control workpaper |
| `templates/finding-5cs-template.md` | Finding (Condition / Criteria / Cause / Consequence / Recommendation) |
| `templates/audit-committee-onepager.md` | Audit-committee roll-up |
| `samples/example-workpaper-APP-TXN-01.md` | Worked example workpaper (what Devin produces) |
| `samples/example-finding-FND-01.md` | Worked example finding |
| `ccm/continuous-controls-monitoring.md` | Scheduled continuous-controls-monitoring spec |

## The demo flow (Ask Devin → Ask Devin → Devin session)

Follows the progressive narrative: two lightweight **Ask Devin** discovery/scoping prompts,
then one **Devin session** that executes and reports.

**1. Ask Devin (discover):**
> Here is our Cards platform repo and our Risk & Control Matrix at `audit/racm.yaml`. Map
> each control to where it's implemented (or should be) in this codebase and tell me what's auditable.

**2. Ask Devin (scope the fieldwork):**
> Using `@playbook:01-audit-planning-coverage`, produce the fieldwork test plan: for each
> control, the test procedure, the population, and the sample. Prioritise the key controls.

**3. Devin session (execute + report):**
> Run the audit fieldwork. Execute `@playbook:02-itgc-testing` and
> `@playbook:03-application-control-reperformance`, then `@playbook:04-findings-and-reporting`.
> Produce a workpaper per control, 5-Cs findings, and the audit-committee one-pager — every
> result cited to `file:line`, commit/PR, or command output.

*(Replace the `@playbook:` names with the `@playbook:playbook-<id>` references once the
Devin Playbooks are created in the workspace.)*

## Expected findings (so the demo is repeatable)

These exist naturally in the auditee code — Devin discovers real gaps, nothing is faked:

| Control | Result | The gap |
|---|---|---|
| APP-TXN-01 | **FAIL** | No positive-amount validation → negative transfer steals from recipient (`AccountService.java:51-135`) |
| APP-TXN-02 | **FAIL** | `transferAmount` not `@Transactional` → partial failure destroys money (`AccountService.java:103-135`) |
| APP-TXN-03 | **FAIL** | No limits, no maker-checker on transfers |
| APP-ACC-04 | **FAIL** | Single hardcoded `"USER"` role, no SoD (`AccountService.java:99-101`) |
| APP-SEC-05 | **FAIL** | CSRF disabled on state-changing endpoints (`SecurityConfig.java:30`) |
| ITGC-SEC-06 | **FAIL** | DB credentials committed in `application.properties:4-5` |
| ITGC-CM-07 | **FAIL** | No CODEOWNERS/branch protection; direct commits to `DevOps` |
| ITGC-CM-08 | **FAIL** | Pipeline builds from a different upstream repo (`Jenkinsfile:26`) |
| ITGC-SDLC-09 | **PARTIAL** | Trivy/OWASP/SonarQube present, but only test is `contextLoads()` |
| ITGC-DATA-10 | **FAIL** | `ddl-auto=update` auto-mutates schema (`application.properties:9`) |
| ITGC-LOG-11 | **FAIL** | No audit logging of financial events |

## Optional closing beats

- **Code review as a continuous control:** open a PR that subtly weakens a control (e.g.
  remove the insufficient-funds check in `withdraw`) and let Devin Review flag the control
  breach *before* merge. See "the weakening PR" in the demo notes below.
- **Continuous Controls Monitoring:** schedule the CCM session (`ccm/`) to re-test nightly
  and escalate only on change.
- **Remediation validation:** fix APP-TXN-01, then have Devin re-test that one control and
  close FND-01 with fresh evidence.

### The "weakening PR" (seed for the code-review beat)
In `AccountService.withdraw`, delete the guard:
```java
if (account.getBalance().compareTo(amount) < 0) {
    throw new RuntimeException("Insufficient funds");
}
```
Open it as a PR. Devin's review should flag that it removes a key transaction-integrity
control (APP-TXN-01 family) and would allow overdrawn balances — caught pre-merge.

## Guardrails (why this is audit-appropriate)
- **Independence / human-in-the-loop:** Devin proposes & drafts; the auditor concludes and signs off.
- **Read-only assurance:** in audit mode Devin never changes the audited system.
- **The auditor is auditable:** every session is logged, reproducible, and exportable.
- **Least privilege & synthetic data:** scoped, read-only access; no real customer data.
