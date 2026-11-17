# Playbook — Findings & Reporting

## Overview
Turn workpaper exceptions into rated findings (5 Cs), assemble a reproducible evidence
package, and produce the audit report draft and audit-committee one-pager. Optionally
re-test a remediated control to validate closure. The auditor reviews and signs off — Devin drafts.

## What's Needed From User
- Completed workpapers from the ITGC and Application-Control playbooks
- `audit/racm.yaml` (for risk/regulation mapping and severity)
- Reporting templates in `audit/templates/`

<phase name="Finding Authoring" id="1">
## Phase 1 — Author Findings (5 Cs)

1. For each exception, draft a finding using `audit/templates/finding-5cs-template.md`: Condition, Criteria, Cause, Consequence, Recommendation.
2. Assign severity from the control's `expected_demo_result.severity` / auditor judgement, and map the finding to its `control_id`, `risk`, and `regulation`.
3. Attach evidence as citations (`file:line`, commit/PR, command output) — every assertion must be reproducible.

<verification>
- Every exception has a 5-Cs finding
- Each finding mapped to control_id + risk + regulation + severity
- Every finding's evidence is a citation a reviewer can re-check
</verification>
</phase>

<phase name="Reporting Roll-up" id="2">
## Phase 2 — Reporting Roll-up

1. Produce the audit-committee one-pager using `audit/templates/audit-committee-onepager.md`: overall RAG, counts by severity, themes, and key-control failures.
2. Roll findings up by risk and by regulation so the committee sees obligation-level exposure.
3. Produce the detailed report body: per-control results table + the findings.

<verification>
- One-pager produced with RAG, severity counts, themes
- Roll-up by risk and regulation present
- Detailed per-control results table complete
</verification>
</phase>

<phase name="Remediation Validation (optional)" id="3">
## Phase 3 — Remediation Validation

1. When a fix lands for a finding, re-run only that control's test procedure.
2. Update the finding/issue status (open → remediated/validated) with fresh evidence and date.
3. Note any residual risk.

<verification>
- Re-test performed for the specific remediated control
- Issue status updated with new evidence
- Residual risk recorded
</verification>
</phase>

## Specifications
- Reporting principle: traceability + reproducibility. No assertion without a citation.
- Devin drafts; the auditor concludes and signs off (independence preserved).

## Forbidden Actions
- Do not overstate severity or soften a finding without auditor instruction.
- Do not close a finding without a successful re-test.
