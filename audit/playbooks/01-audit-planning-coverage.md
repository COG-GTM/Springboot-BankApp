# Playbook — Audit Planning & Coverage Mapping

## Overview
Ingest the audit ontology (`audit/racm.yaml`) and the auditee codebase, then produce
(a) a **coverage map** of where each control is implemented (or should be) in the code
and (b) a **fieldwork test plan** with a procedure, population and sample per control.
This is a read-only scoping activity — no findings are concluded here.

## What's Needed From User
- Repository access for the auditee (Springboot-BankApp)
- The RACM at `audit/racm.yaml` (loaded as Knowledge or read from the repo)
- The in-scope audit period (for change-management population)

<phase name="Ontology & Scope Intake" id="1">
## Phase 1 — Ontology & Scope Intake

1. Read `audit/racm.yaml`. Enumerate every control with its `control_id`, `auditable_entity`, `risk`, `regulation`, `test_procedure`, `population`, and `pass_criteria`.
2. Read `audit/ontology.md` to confirm the entity/risk/control/test/evidence/finding model.
3. Confirm the in-scope period and the protected branch for change-management population.

<verification>
- Every control in `racm.yaml` is enumerated with its key attributes
- Each control is mapped to its auditable entity and the regulation(s) it supports
- The audit period and protected branch are recorded
</verification>
</phase>

<phase name="Coverage Mapping" id="2">
## Phase 2 — Coverage Mapping

1. For each control, locate where it is (or should be) implemented in the codebase and record exact `file:line` references.
2. Classify each control as: `implemented`, `partially implemented`, `not implemented`, or `not applicable` — with a one-line rationale and citation. Do NOT yet conclude pass/fail.
3. Flag any control whose `auditable_entity.primary_source` does not exist or cannot be located.

<verification>
- Every control has at least one code citation or an explicit "no implementation found" note
- Coverage classification recorded per control with rationale
- Gaps in locating sources are flagged
</verification>
</phase>

<phase name="Test Plan" id="3">
## Phase 3 — Fieldwork Test Plan

1. For each control, restate the test procedure, the population, the sampling approach (full population vs sample size), and the pass criteria.
2. Choose the strongest feasible test method (prefer inspection / re-performance over inquiry).
3. Produce the plan as a table ordered by `key: true` first, then by severity, and save it as a workpaper-style artifact.

<verification>
- Test plan covers 100% of controls in the RACM
- Each control's population and sample are explicit and justified
- Key controls are prioritised first
- Output saved as a planning workpaper
</verification>
</phase>

## Specifications
- Output: a coverage map + a fieldwork test plan, both fully cited.
- Read-only. No code changes. No pass/fail conclusions in this playbook.

## Forbidden Actions
- Do not modify the auditee codebase.
- Do not conclude findings — that happens in the testing playbooks.
- Do not rely on inquiry alone where inspection/re-performance is possible.
