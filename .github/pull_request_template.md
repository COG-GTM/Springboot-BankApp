# Change summary

<!-- What changes, and why. Link the ticket / change record. -->

Change record / ticket:

## Change classification

- [ ] Standard (pre-approved, low risk)
- [ ] Normal (requires review + approval before merge)
- [ ] Emergency (retrospective approval; state the incident reference)

## Control attestations

- [ ] **ITGC-CM-07** — I am not the approver of this change; an independent code owner will review it.
- [ ] **ITGC-SEC-06** — No credentials, tokens or keys are committed; configuration is read from the environment. The gitleaks job is green.
- [ ] **ITGC-DATA-10** — Any schema change is an append-only Flyway migration under `src/main/resources/db/migration/`; no existing migration was edited and `spring.jpa.hibernate.ddl-auto` remains `validate`.
- [ ] **ITGC-SDLC-09** — Tests and security scans pass in CI; scanner findings above threshold are fixed, not waived.
- [ ] **ITGC-LOG-11** — Financial or security-relevant behaviour changes emit audit events, and no secrets or full PII are written to logs.
- [ ] **ITGC-CM-08** — Build provenance unchanged: the pipeline still builds this repository.

## Money movement impact

- [ ] This change affects deposit, withdraw or transfer behaviour. If checked, describe the balance / limit / authorisation impact and the tests covering it:

## Rollback plan

<!-- How this change is reverted, including whether the migration is backward compatible. -->

## Evidence

<!-- Test output, audit log samples, scan reports, screenshots. -->
