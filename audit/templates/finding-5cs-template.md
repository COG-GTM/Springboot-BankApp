# Finding {FINDING_ID} — {short title}

| Field | Value |
|---|---|
| **Finding ID** | {FINDING_ID} |
| **Related control** | {CONTROL_ID} |
| **Risk** | {R-XX — title} |
| **Regulation(s)** | {…} |
| **Severity** | {Critical / High / Medium / Low} |
| **Status** | {Open / Remediated / Validated} |
| **Owner (1st line)** | {accountable owner} |
| **Target date** | {YYYY-MM-DD} |

## Condition
*What we found* — the actual state observed, with citations.
> {e.g. `AccountService.transferAmount` (L103-135) performs no positive-amount check and is not `@Transactional`.}

## Criteria
*What should be true* — the policy/standard/regulation the condition is measured against.
> {e.g. Monetary transactions must validate inputs and execute atomically (ICFR; Consumer Duty — avoidance of foreseeable harm).}

## Cause
*Why it happened* — root cause, not just the symptom.
> {e.g. Validation and transaction-boundary management were never implemented in the service layer.}

## Consequence
*Impact* — what the risk could lead to (quantify where possible).
> {e.g. A negative transfer misappropriates funds; a partial failure mid-transfer destroys money and breaks reconciliation.}

## Recommendation
*What to do* — specific, actionable, testable.
> {e.g. Reject non-positive amounts on all monetary methods; wrap transfer in a single `@Transactional` unit; add limits + maker-checker above threshold; add a re-performance test.}

## Evidence
| # | Evidence | Reference |
|---|---|---|
| 1 | {…} | `{file:line}` / {PR/commit} / {command output} |

## Management response
> {to be completed by the first line}
