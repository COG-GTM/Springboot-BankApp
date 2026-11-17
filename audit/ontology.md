# Audit Ontology

The ontology is the backbone of "Devin for Group Audit". It is the machine-readable
vocabulary that lets Devin turn *"go audit this system"* into concrete, repeatable,
attributable tests — and lets every result roll up to a risk and a regulation.

## The model

```
Auditable Entity ──has──> Risk ──mitigated by──> Control ──tested by──> Test Procedure
       │                   │                        │                       │
  (system/process)   (what goes wrong)     (preventive/detective/      (inquiry / observation /
                                            corrective; manual/auto;     inspection / re-performance;
                                            key/non-key; owner)          sample; pass/fail criteria)
                                                 │
                                         mapped to Regulation
                                   (ICFR, PRA/FCA, SMCR, Consumer Duty,
                                    Op-Resilience/DORA, GDPR)
                                                 │
  Test Procedure ──produces──> Evidence ──substantiates──> Result ──exception──> Finding
                                                                                    │
                                                                            tracked to Action
                                                                          (remediation + re-test)
```

## Entities in the ontology

| Concept | Meaning | Where it lives |
|---|---|---|
| **Auditable Entity** | A system or process in scope (the slice of the audit universe) | `racm.yaml: auditable_entities` |
| **Risk** | What could go wrong if the control fails | `racm.yaml: risks` |
| **Control** | The mechanism that mitigates the risk; typed by nature, automation, and whether it is *key* | `racm.yaml: controls` |
| **Regulation** | The obligation the control supports | `racm.yaml: regulations` |
| **Test Procedure** | How the control is tested (the four classic methods below) | `controls[].test_procedure` |
| **Evidence** | Artifacts that substantiate a test result (must be a citation or re-runnable) | `controls[].evidence_required` |
| **Finding / Issue** | An exception where the control failed, rated by severity | produced by Devin → `templates/finding-5cs-template.md` |
| **Action** | Remediation, validated by a re-test | tracked via issue + re-test |

## Control attributes (the typing that drives testing)

- **Nature:** `preventive` (stops it happening) · `detective` (spots it after) · `corrective` (fixes it)
- **Automation:** `automated` · `manual` · `hybrid`
- **Key:** `true` if failure alone could lead to a material misstatement / significant risk
- **Owner:** the accountable first-line owner (links to SMCR accountability)

## The four test methods (auditing standard)

1. **Inquiry** — ask the owner / read documentation (weakest alone).
2. **Observation** — watch the control operate.
3. **Inspection** — examine evidence (config, code, logs, PR metadata).
4. **Re-performance** — independently execute the control and compare the result (strongest).

Devin's leverage is greatest on **inspection** and **re-performance**, executed over the
**full population** rather than a sample — e.g. inspecting every merge for approval, or
re-performing a transfer calculation against crafted inputs.

## Why this matters

Because every control row carries a `control_id`, a `risk`, a `regulation`, a
`test_procedure`, `pass_criteria`, and `evidence_required`, Devin's output is:

- **Traceable** — each conclusion cites `file:line`, a PR/commit, or command output.
- **Reproducible** — a reviewer or regulator can re-run the test.
- **Roll-up-able** — results aggregate by risk, by regulation, and by key/non-key for the audit-committee view.
- **Independent** — Devin tests and evidences; the auditor concludes and signs off.
