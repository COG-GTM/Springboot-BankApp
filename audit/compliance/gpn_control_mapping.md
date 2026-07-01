# Control-to-Regulation Mapping — BankApp (Springboot-BankApp)

> **Engagement:** Compliance-as-code demo for a payments customer (Global Payments).
> **Approach:** Map a [FINOS Common Cloud Controls](https://github.com/finos/common-cloud-controls)-style
> control catalogue (this repo's `audit/racm.yaml`) to the payments regulations that
> govern a card-processing / payments platform, then **re-perform each control against the
> actual code** and record a PASS / FAIL result with `file:line` evidence.
>
> **Auditee:** `COG-GTM/Springboot-BankApp` — a Spring Boot banking/payments application.
> **Control source of truth:** `audit/racm.yaml` (11 controls, Group Audit RACM ontology).
> **Result basis:** design + operating effectiveness, tested by inspection / re-performance.
> Every assertion cites `file:line`. Line numbers reference the code at the time of writing.

## Regulations in scope (mapped to)

| Tag | Regulation | Why it applies to a payments platform |
|---|---|---|
| **PCI DSS 4.0** | Payment Card Industry Data Security Standard v4.0 | Cardholder-data environment: secure coding (Req 6), access control (Req 7/8), secure config & stored-data protection (Req 2/3), logging (Req 10). |
| **SOX (ICFR)** | Sarbanes-Oxley §302/§404 — Internal Control over Financial Reporting | Money-movement code and the systems that change it are ICFR-relevant: completeness/accuracy of financial transactions, segregation of duties, change management. |
| **GDPR** | UK GDPR / DPA 2018 | Customer account data is personal data: Art. 32 (security of processing — confidentiality & integrity), Art. 5(1)(d)/(f) (accuracy, integrity & confidentiality), Art. 33 (breach detection). |

Nature legend: `preventive` / `detective` / `corrective` · `automated` / `manual` / `hybrid` · `key` = key control.

---

## Control-to-regulation matrix

| Control ID | Objective (short) | Nature | PCI DSS 4.0 | SOX (ICFR) | GDPR | Result | Sev | Evidence (`file:line`) |
|---|---|---|---|---|---|---|---|---|
| **APP-TXN-01** | Monetary amounts validated positive & in-bounds before posting | preventive · automated · key | Req 6.2.4 (secure coding — business-logic flaws) | §404 — completeness & accuracy of transactions | Art. 5(1)(d) accuracy; Art. 32(1)(b) integrity | **FAIL** | High | `service/AccountService.java:51-62` (deposit), `:64-78` (withdraw), `:103-135` (transfer); `controller/BankController.java:50-96` |
| **APP-TXN-02** | Transfers execute atomically (one transaction boundary) | preventive · automated · key | Req 6.2.4 (secure coding) | §404 — accuracy & reconcilability of balances | Art. 32(1)(b) integrity | **FAIL** | High | `service/AccountService.java:103-135` (4 writes, no `@Transactional`; debit `:113`, credit `:117`) |
| **APP-TXN-03** | Limits + maker-checker on high-value transfers | preventive · hybrid · key | Req 7.2 (least privilege / authorization) | §404 — authorization of transactions | Art. 32 | **FAIL** | High | `service/AccountService.java:103-135` (no limit / second-approver); `controller/BankController.java:82-96` |
| **APP-ACC-04** | Role-based access & segregation of duties | preventive · automated · key | Req 7.2 / 7.3 (RBAC, least privilege) | §404 — segregation of duties | Art. 32; Art. 5(1)(f) | **FAIL** | Medium | `service/AccountService.java:99-101` (single hardcoded `"USER"` authority); `config/SecurityConfig.java:31-34` (no role-restricted endpoints) |
| **APP-SEC-05** | State-changing endpoints protected (CSRF) | preventive · automated | Req 6.2.4 (secure coding — CSRF is a named attack) | §404 — integrity of financial transactions | Art. 32 | **FAIL** | Medium | `config/SecurityConfig.java:30` (`csrf.disable()`); endpoints `controller/BankController.java:50,58,82` |
| **ITGC-SEC-06** | No secrets/credentials committed to source/config | preventive · automated · key | Req 8.6.2 (no hard-coded passwords in config/property files); Req 2.2 (secure config); Req 3.x (protect stored auth data) | §404 — access to financial systems | Art. 32(1)(b) confidentiality | **FAIL** | High | `src/main/resources/application.properties:4-5` (DB user `root` / password `Test@123` in plaintext) |
| **ITGC-CM-07** | Changes independently reviewed & approved (author≠approver) | preventive · hybrid · key | Req 6.2.3 (code reviewed before release) | §404 — change management | Art. 32 (org measures) | **FAIL** | High | No `CODEOWNERS`; direct commits to default branch `DevOps` (e.g. commits `Update Jenkinsfile`, `Update bankapp-deployment.yml` in `git log`) |
| **ITGC-CM-08** | Build provenance: pipeline builds the audited repo | preventive · automated · key | Req 6.3 / 6.5 (change control & provenance) | §404 — change management provenance | Art. 32 | **FAIL** | Medium | `Jenkinsfile:26` checks out `LondheShubham153/Springboot-BankApp.git` (a different upstream repo) |
| **ITGC-SDLC-09** | SAST/SCA scans + automated test gate in pipeline | detective · automated · key | Req 6.3.1 (identify vulnerabilities); Req 6.2.4 | §404 — SDLC control | Art. 32 | **PARTIAL** | Medium | Strengths: `Jenkinsfile:31` (Trivy), `:39` (OWASP), `:47-61` (SonarQube gate). Gap: only test is `BankappApplicationTests.contextLoads()` (`src/test/java/com/example/bankapp/BankappApplicationTests.java:9-10`); no test-execution stage |
| **ITGC-DATA-10** | Controlled schema changes (no runtime auto-mutation) | preventive · automated | Req 6.3 / 6.5 (change control) | §404 — change management | Art. 32 | **FAIL** | Medium | `src/main/resources/application.properties:9` (`spring.jpa.hibernate.ddl-auto=update`) |
| **ITGC-LOG-11** | Financial/security events are audit-logged | detective · automated · key | Req 10.2 (audit logs); Req 10.3 (protect logs) | §404 — monitoring & audit trail | Art. 32; Art. 33 (breach detection) | **FAIL** | Medium | No audit logging in `service/AccountService.java:51-135`; only `spring.jpa.show-sql=true` (`application.properties:11`) which leaks SQL/data to stdout |

**Summary:** 11 controls tested — **10 FAIL, 1 PARTIAL, 0 PASS** at baseline.
High-severity FAILs: APP-TXN-01, APP-TXN-02, APP-TXN-03, ITGC-SEC-06, ITGC-CM-07.

---

## Re-performance detail per control

Each control below records the **test procedure**, the **re-performed observation** against
the actual code, and the **regulation rationale** for the mapping.

### APP-TXN-01 — Transaction amount validation — **FAIL (High)**
- **Procedure:** inspect deposit/withdraw/transfer; re-perform with `0`, negative, and very large amounts.
- **Observation:** `deposit` adds `amount` unconditionally (`service/AccountService.java:51-62`); `withdraw` (`:64-78`) and `transferAmount` (`:103-135`) check `balance >= amount` but never `amount > 0`. The controller passes `BigDecimal amount` straight through (`controller/BankController.java:50-96`). A **negative transfer** debits a negative (i.e. *credits* the sender) and credits a negative to the recipient — fund misappropriation.
- **Mapping rationale:** PCI DSS 4.0 **Req 6.2.4** requires software to be engineered to prevent business-logic / input-tampering attacks. SOX **ICFR** requires transaction completeness & accuracy. GDPR **Art. 5(1)(d)** (accuracy) & **Art. 32(1)(b)** (integrity).

### APP-TXN-02 — Transfer atomicity — **FAIL (High)**
- **Procedure:** inspect `transferAmount` for a transaction boundary; reason about partial-failure between debit and credit.
- **Observation:** `transferAmount` performs four separate repository writes — sender save (`:113`), recipient save (`:117`), two transaction-record saves (`:126`, `:134`) — with **no `@Transactional`**. A failure after `:113` but before `:117` destroys money and leaves balances non-reconcilable.
- **Mapping rationale:** PCI DSS **Req 6.2.4** (secure coding). SOX **ICFR** (accuracy/reconcilability). GDPR **Art. 32(1)(b)** (integrity of processing).

### APP-TXN-03 — Authorisation & dual-control on value movement — **FAIL (High)**
- **Procedure:** check for per-transaction / daily limits and a second-approver step on transfers.
- **Observation:** `transferAmount` (`service/AccountService.java:103-135`) enforces no limit and has no maker-checker. Any authenticated user moves any amount up to balance in one unreviewed action (`controller/BankController.java:82-96`).
- **Mapping rationale:** PCI DSS **Req 7.2** (least privilege / authorized access). SOX **ICFR** (authorization of transactions). GDPR **Art. 32**.

### APP-ACC-04 — Role-based access & least privilege — **FAIL (Medium)**
- **Procedure:** inspect granted-authorities and endpoint authorization rules.
- **Observation:** `authorities()` hardcodes a single `"USER"` authority for every account (`service/AccountService.java:99-101`); `SecurityConfig` only distinguishes `permitAll` on `/register` vs `authenticated` for everything else (`config/SecurityConfig.java:31-34`) — no admin/operator segregation.
- **Mapping rationale:** PCI DSS **Req 7.2 / 7.3** (RBAC, least privilege). SOX **ICFR** (segregation of duties). GDPR **Art. 32**, **Art. 5(1)(f)**.

### APP-SEC-05 — CSRF protection on state-changing endpoints — **FAIL (Medium)**
- **Procedure:** inspect security config for CSRF on POST endpoints.
- **Observation:** `SecurityConfig` calls `csrf.disable()` (`config/SecurityConfig.java:30`); `/deposit`, `/withdraw`, `/transfer` POSTs (`controller/BankController.java:50,58,82`) are exposed to cross-site request forgery.
- **Mapping rationale:** PCI DSS **Req 6.2.4** explicitly names CSRF among attacks secure coding must mitigate. SOX **ICFR** (integrity of financial transactions). GDPR **Art. 32**.

### ITGC-SEC-06 — Credential & secrets management — **FAIL (High)**  ← *remediated in this PR*
- **Procedure:** scan committed configuration for embedded credentials.
- **Observation (baseline):** `application.properties:4-5` commits DB username `root` and password `Test@123` in plaintext. Anyone with repo read access obtains DB credentials.
- **Mapping rationale:** PCI DSS 4.0 **Req 8.6.2** — "passwords/passphrases for application and system accounts are not hard-coded in scripts, configuration/property files, or bespoke and custom source code"; also **Req 2.2** (secure configuration) and **Req 3.x** (protect stored authentication data). SOX **ICFR** (controlled access to financial systems). GDPR **Art. 32(1)(b)** (confidentiality of processing).
- **Remediation (this PR):** credentials externalised to environment variables with no committed default for the password (`application.properties:4-5`); enforced by `CredentialManagementControlTest` and the `compliance-gate` CI workflow. See `## Remediation` below.

### ITGC-CM-07 — Change management: independent review & approval — **FAIL (High)**
- **Procedure:** inspect branch protection & CODEOWNERS; sample changes to the protected branch.
- **Observation:** no `CODEOWNERS` file; `git log` on `DevOps` shows direct commits (e.g. `Update Jenkinsfile`, `Update bankapp-deployment.yml`) with no associated reviewed PR — production pipeline & k8s manifests changed without independent approval.
- **Mapping rationale:** PCI DSS **Req 6.2.3** (code reviewed prior to release). SOX **ICFR** (change management). GDPR **Art. 32** (organizational measures).

### ITGC-CM-08 — Pipeline integrity & source traceability — **FAIL (Medium)**
- **Procedure:** trace the CI checkout source against the audited repo.
- **Observation:** `Jenkinsfile:26` checks out `https://github.com/LondheShubham153/Springboot-BankApp.git` (branch `DevOps`) — a different upstream repo than the one under audit. Deployed-artifact provenance is not tied to reviewed source.
- **Mapping rationale:** PCI DSS **Req 6.3 / 6.5** (change control & provenance). SOX **ICFR** (change management provenance). GDPR **Art. 32**.

### ITGC-SDLC-09 — Secure SDLC: scanning & test gating — **PARTIAL (Medium)**
- **Procedure:** inspect pipeline for SAST/SCA/filesystem scanning and a test-execution gate.
- **Observation:** Trivy (`Jenkinsfile:31`), OWASP dependency-check (`:39`) and SonarQube quality gate (`:47-61`) are present. **Gap:** the only test is `contextLoads()` (`src/test/java/com/example/bankapp/BankappApplicationTests.java:9-10`) — no business-logic coverage and no test-execution stage in the pipeline.
- **Mapping rationale:** PCI DSS **Req 6.3.1** (identify vulnerabilities) & **6.2.4**. SOX **ICFR** (SDLC control). GDPR **Art. 32**.
- **Note:** this PR adds the first business-logic-adjacent automated test and a CI test gate (see Remediation), improving this control.

### ITGC-DATA-10 — Production schema/data change control — **FAIL (Medium)**
- **Procedure:** inspect ORM/schema config for uncontrolled runtime auto-migration.
- **Observation:** `application.properties:9` sets `spring.jpa.hibernate.ddl-auto=update` — Hibernate mutates the schema at runtime in every environment with no review or migration trail.
- **Mapping rationale:** PCI DSS **Req 6.3 / 6.5** (change control). SOX **ICFR** (change management). GDPR **Art. 32**.

### ITGC-LOG-11 — Logging & monitoring of financial events — **FAIL (Medium)**
- **Procedure:** inspect transaction paths for audit logging.
- **Observation:** no audit logging around deposit/withdraw/transfer (`service/AccountService.java:51-135`). The only logging signal is `spring.jpa.show-sql=true` (`application.properties:11`), which leaks SQL to stdout and is not a protected audit trail.
- **Mapping rationale:** PCI DSS **Req 10.2** (audit logs for events) & **Req 10.3** (protect logs). SOX **ICFR** (monitoring & audit trail). GDPR **Art. 32** & **Art. 33** (breach detection). The `show-sql` leak is itself a GDPR **Art. 5(1)(f)** concern.

---

## Remediation delivered in this PR (ITGC-SEC-06)

| Field | Value |
|---|---|
| **Control** | ITGC-SEC-06 — Credential & secrets management |
| **Regulations** | PCI DSS 4.0 Req 8.6.2, Req 2.2, Req 3.x · SOX ICFR (§404) · GDPR Art. 32 |
| **Before** | `application.properties:4-5` committed `spring.datasource.password=Test@123` (plaintext DB credentials in source control). |
| **After** | Credentials read from environment variables; **no committed default for the password** — `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}`. |
| **Proving test** | `CredentialManagementControlTest` re-performs the control: it parses `application.properties` and fails if any credential value is a hard-coded literal rather than a `${ENV}` placeholder. |
| **CI gate** | `.github/workflows/compliance-gate.yml` runs the control test on every push/PR, failing the build if the control regresses (a secret is re-committed). |

The control now **PASSES** (proven by the test) and is **wired into CI as a gate**, mirroring
the continuous-controls-monitoring model in `audit/ccm/continuous-controls-monitoring.md`.
