# Workpaper — APP-TXN-01: Transaction amount validation

> WORKED EXAMPLE — illustrates the output Devin produces from the Application-Control
> Re-performance playbook. Line numbers reference the repo at the time of writing.

| Field | Value |
|---|---|
| **Control ID** | APP-TXN-01 |
| **Auditable entity** | AE-APP — Core banking application logic |
| **Control objective** | All monetary transactions are validated to be positive and within bounds before posting. |
| **Risk mitigated** | R-01 — Monetary value is misappropriated or fabricated through transaction processing |
| **Regulation(s)** | ICFR; Consumer Duty |
| **Control type** | preventive · automated · key |
| **Control owner** | Head of Payments Engineering |
| **Period** | FY in-scope period |
| **Tester** | Devin (drafted) · {Auditor} (reviewed) |
| **Date** | 2026-06-19 |

## Test method
Inspection + re-performance. The control is automated and code-resident, so the
strongest test is to read the implementing code and re-perform it with boundary inputs.

## Population & sample
- **Population:** all monetary mutation methods in `AccountService` (deposit, withdraw, transfer).
- **Sample:** full population (3 methods) + crafted boundary inputs.

## Procedure performed
1. Located the three monetary mutation methods and read each fully.
2. Identified the validations present on each path.
3. Re-performed each with inputs: `0`, `-100`, and a value greater than balance.

## Evidence
| # | Evidence | Reference |
|---|---|---|
| 1 | `deposit` adds `amount` with no sign/bounds check | `src/main/java/com/example/bankapp/service/AccountService.java:51-62` |
| 2 | `withdraw` checks balance ≥ amount but not amount > 0 | `src/main/java/com/example/bankapp/service/AccountService.java:64-78` |
| 3 | `transferAmount` checks balance ≥ amount but not amount > 0 | `src/main/java/com/example/bankapp/service/AccountService.java:103-135` |
| 4 | Controller passes `BigDecimal amount` straight through, no validation | `src/main/java/com/example/bankapp/controller/BankController.java:50-96` |

## Re-performance
| Method | Input | Expected (policy) | Observed | Result |
|---|---|---|---|---|
| deposit | `-100` | rejected | balance increases by… wait, decreases (credits a negative) | FAIL |
| withdraw | `-100` | rejected | balance **increases** by 100 (subtracting a negative) | FAIL |
| transfer | `-100` to victim | rejected | sender **gains** 100, recipient **loses** 100 | FAIL |
| transfer | `0` | rejected | accepted (no-op transaction record created) | FAIL |

## Result
- **Conclusion:** FAIL
- **Pass criteria:** Non-positive and out-of-bounds amounts are rejected on every path.
- **Exceptions:** No positive-amount validation on any monetary method; negative amounts invert the intended money movement.

## Notes / residual risk
Highest-impact gap in the audit: a negative transfer is a direct fund-misappropriation
vector exploitable by any authenticated user. Raised as Finding FND-01.
