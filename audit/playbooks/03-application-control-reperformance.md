# Playbook — Application-Control Re-performance

## Overview
Independently **re-perform** the key application/business-process controls in the RACM
(transaction validation, transfer atomicity, authorisation/dual-control) against crafted
test data, then confirm whether the code enforces what the RACM claims. Re-performance is
the strongest test method. Read-only against the auditee; any test harness runs in isolation.

## What's Needed From User
- Auditee repository access
- `audit/racm.yaml`
- A test database / fixture (synthetic data only)

<phase name="Control Path Tracing" id="1">
## Phase 1 — Trace the Control Paths

1. For APP-TXN-01, APP-TXN-02, APP-TXN-03, locate the implementing code and trace the full execution path with `file:line` citations.
2. State, for each control, the behaviour the RACM claims and the behaviour the code actually implements.
3. Identify the boundary/abuse inputs to exercise (zero, negative, very large, self-transfer, partial-failure).

<verification>
- Each application control's code path traced and cited
- Claimed vs actual behaviour stated per control
- Abuse/boundary input set defined
</verification>
</phase>

<phase name="Re-performance" id="2">
## Phase 2 — Re-perform Against Test Data

1. Drive each control with the crafted inputs (via existing tests, a scratch harness, or careful reasoning where execution is infeasible) using synthetic data only.
2. Record the observed outcome vs the expected (policy-correct) outcome in a re-performance table.
3. Confirm transaction-boundary behaviour for multi-write operations (does a mid-operation failure roll back?).

<verification>
- Re-performance table: input → expected → observed → pass/fail
- Negative/zero amount behaviour confirmed for deposit/withdraw/transfer
- Atomicity / partial-failure behaviour confirmed for transfer
- High-value transfer authorisation/limit behaviour confirmed
</verification>
</phase>

<phase name="Workpapers" id="3">
## Phase 3 — Workpapers

1. Produce a workpaper per application control using `audit/templates/workpaper-template.md`.
2. Attach the re-performance table and code citations as evidence.
3. Hand exceptions to the Findings & Reporting playbook.

<verification>
- One workpaper per application control re-performed
- Re-performance evidence attached and reproducible
- Exceptions listed for findings
</verification>
</phase>

## Specifications
- Synthetic/anonymised data only. The harness must not touch production-like data.
- Prefer executable re-performance; where infeasible, document the reasoning explicitly and mark the method.

## Forbidden Actions
- Do not modify auditee business logic to make a control "pass".
- Do not use real customer data.
