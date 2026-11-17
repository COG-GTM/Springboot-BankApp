# Playbook — ITGC Testing

## Overview
Test the IT General Controls in the RACM (change management, logical access, secure
SDLC, secrets/config, data/schema control, logging) by **inspection over the full
population** of code, config, pipeline and repo history. Produce one workpaper per
control with cited evidence and a pass/fail/partial result. Read-only.

## What's Needed From User
- Auditee repository access (code + full git history; un-shallow if needed)
- `audit/racm.yaml`
- The in-scope period and protected branch

<phase name="Change Management & Pipeline Integrity" id="1">
## Phase 1 — Change Management & Pipeline (ITGC-CM-07, ITGC-CM-08)

1. Check for `CODEOWNERS` and branch-protection evidence on the protected branch (design test).
2. For the in-scope population of changes to the protected branch, build a per-change evidence table: commit/PR, author, approver (must differ from author), linked change record, CI status. Note any direct-to-branch pushes.
3. Trace the CI/CD checkout source (`Jenkinsfile`) against the audited repository to test build provenance (ITGC-CM-08).

<verification>
- Per-change evidence table produced for the population/sample
- Author≠approver tested for each sampled change
- Branch-protection / CODEOWNERS design state recorded
- Pipeline source provenance traced and concluded
</verification>
</phase>

<phase name="Logical Access & Secrets" id="2">
## Phase 2 — Logical Access & Secrets (APP-ACC-04, APP-SEC-05, ITGC-SEC-06)

1. Inspect the granted-authorities/role model and endpoint authorisation rules; assess least privilege and SoD (APP-ACC-04).
2. Inspect security configuration for CSRF and protections on state-changing endpoints (APP-SEC-05).
3. Scan all committed config and history for embedded secrets/credentials (ITGC-SEC-06). Cite exact `file:line`.

<verification>
- Role/privilege model assessed with citations
- CSRF / state-changing-endpoint protection concluded
- Secret scan run over source + history with results cited
</verification>
</phase>

<phase name="SDLC, Data & Logging" id="3">
## Phase 3 — Secure SDLC, Data & Logging (ITGC-SDLC-09, ITGC-DATA-10, ITGC-LOG-11)

1. Inspect the pipeline for SAST/SCA/filesystem scanning and a test-execution gate; inventory the test suite and assess business-logic coverage (ITGC-SDLC-09).
2. Inspect persistence config for uncontrolled runtime schema mutation (ITGC-DATA-10).
3. Inspect transaction code paths for audit logging of financial/security events (ITGC-LOG-11).

<verification>
- Pipeline scanning + test gating concluded; test coverage inventoried
- Schema/data change control concluded with citation
- Financial-event logging concluded with citation
</verification>
</phase>

<phase name="Workpapers" id="4">
## Phase 4 — Workpapers

1. For each ITGC tested, produce a workpaper using `audit/templates/workpaper-template.md`.
2. Record result (pass/partial/fail), every piece of evidence as a citation, and any exceptions.
3. Hand exceptions to the Findings & Reporting playbook.

<verification>
- One workpaper per ITGC control tested
- Every result backed by a citation (file:line / commit / command output)
- Exceptions listed and ready for findings
</verification>
</phase>

## Specifications
- Test the full population wherever feasible (e.g. all merges, all config).
- Every conclusion must be reproducible from the cited evidence.

## Forbidden Actions
- Do not modify the auditee codebase or its history.
- Do not conclude a pass without inspecting actual evidence (no inquiry-only passes).
- Do not print secret values — cite the location only.
