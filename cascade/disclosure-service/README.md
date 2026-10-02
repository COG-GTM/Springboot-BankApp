# cascade/disclosure-service — Reg DD / Reg Z disclosure values from DNA core outputs

Cascade Community Bank estate (Banking & Lending demo). A small Spring Boot 3.3 / Java 17 service that renders the
disclosure figures a deposit/loan operation owes under **Regulation DD (12 CFR Part 1030)** and **Regulation Z
(12 CFR Part 1026)** from what the core produces — and compares the recorded legacy core with the migrated
PostgreSQL core so a change in the core shows up where Compliance looks.

| endpoint | what |
|---|---|
| `GET /` | HTML: parity status, disclosed APY per product, APR per loan product, APY earned per account |
| `GET /api/disclosures/products` | Appendix A Part I APY per deposit product vs the disclosed APY, ±0.05 pp (§1030.3(f)(2)) |
| `GET /api/disclosures/loans` | actuarial APR (§1026.22(a)(1)) per loan product vs disclosed, ±⅛ pp (§1026.22(a)(2)) |
| `GET /api/disclosures/accounts/{id}/apy-earned?source=legacy|migrated` | Appendix A Part II APY earned over the loaded window (§1030.6(a)(1)) |
| `GET /api/disclosures/parity` | legacy vs migrated APY earned per account; differences are reported, never rounded away |

## Inputs (classpath `core-outputs/`)
* `legacy/` — the Cascade golden set from `fiserv-dna-migration-demo/cascade/finserv-estate/demo/bk/golden/` (50 accounts × 90 days).
  **Produced by a reference model of the Oracle behaviour derived from the PL/SQL source — not Oracle.** Oracle is not available in the sandbox.
* `migrated/daily_accrual.csv` — optional; the PostgreSQL run's export (`demo/out/bk/migrated_daily_accrual.csv`). The demo's
  pre-run PR adds it together with a parity test. Without it the parity endpoint says "not loaded" rather than "no differences".

## Assumption to confirm
APY earned is computed from the sum of the core's **rounded daily accrual** (`accrual_rec.rounded_accrual()`, the
statement "interest earned today" line) — **[compliance to confirm]** that Cascade's statements use that figure and not
the 6dp accrual. The 1-cent escalations in the core parity only reach a disclosure through this line.

## Run
```bash
./mvnw -q test                 # from the Springboot-BankApp root: cd cascade/disclosure-service && mvn test
mvn spring-boot:run            # http://localhost:8089/
```
Java 17, Maven; no database, no credentials. Engineering evidence for Compliance — not a regulatory determination.
