# Finding FND-01 — Monetary transactions accept non-positive amounts

> WORKED EXAMPLE — output of the Findings & Reporting playbook for control APP-TXN-01.

| Field | Value |
|---|---|
| **Finding ID** | FND-01 |
| **Related control** | APP-TXN-01 (and APP-TXN-02 atomicity) |
| **Risk** | R-01 — Monetary value is misappropriated or fabricated |
| **Regulation(s)** | ICFR; Consumer Duty |
| **Severity** | High |
| **Status** | Open |
| **Owner (1st line)** | Head of Payments Engineering |
| **Target date** | {YYYY-MM-DD} |

## Condition
`AccountService.deposit` (L51-62), `withdraw` (L64-78) and `transferAmount` (L103-135)
post monetary movements without validating that the amount is positive. Re-performance
confirmed a `-100` transfer credits the sender and debits the recipient, and a `-100`
withdrawal increases the balance.

## Criteria
Monetary transactions must validate inputs (positive, within limits) before posting, and
account balances must remain complete and accurate (ICFR; FCA Consumer Duty — avoidance
of foreseeable customer harm).

## Cause
Input validation was never implemented in the service layer; the controller passes the
`BigDecimal amount` straight through, and the only balance check (`balance >= amount`)
is trivially satisfied by negative values.

## Consequence
Any authenticated user can misappropriate funds from another account via a negative
transfer, or inflate their own balance via a negative withdrawal. Direct financial loss,
mis-stated balances, and customer harm. Combined with the absence of a transaction
boundary (APP-TXN-02), partial failures can also destroy money.

## Recommendation
1. Reject non-positive amounts on `deposit`, `withdraw` and `transferAmount` (fail closed).
2. Enforce per-transaction and daily limits; add maker-checker above a threshold (APP-TXN-03).
3. Wrap `transferAmount` in a single `@Transactional` unit of work (APP-TXN-02).
4. Add re-performance unit tests covering zero/negative/over-limit inputs (closes the ITGC-SDLC-09 test-coverage gap for this logic).

## Evidence
| # | Evidence | Reference |
|---|---|---|
| 1 | No positive-amount check on any monetary method | `src/main/java/com/example/bankapp/service/AccountService.java:51-135` |
| 2 | Controller forwards raw amount | `src/main/java/com/example/bankapp/controller/BankController.java:50-96` |
| 3 | Re-performance table | `audit/samples/example-workpaper-APP-TXN-01.md` |

## Management response
> {to be completed by the first line}
