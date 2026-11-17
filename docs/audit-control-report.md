# Audit Control Report — Springboot-BankApp

- **Auditee**: COG-GTM/Springboot-BankApp (Spring Boot 3.3.3 / Java 17, branch `DevOps`)
- **Baseline commit tested (before)**: `305826d`
- **Scope**: application-level money-movement controls, access control, CSRF, committed credentials, audit logging
- **Method**: code inspection with `file:line` evidence, plus re-performance through automated tests added in this change

## How to re-verify

A real MySQL is required (no H2 profile in this repo):

```bash
docker run -d --name bankapp-mysql \
  -e MYSQL_ROOT_PASSWORD=Test@123 -e MYSQL_DATABASE=bankappdb -p 3306:3306 \
  mysql:8.0 --default-authentication-plugin=mysql_native_password

cd Springboot-BankApp && ./mvnw clean test
```

Result on this change: `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` / `BUILD SUCCESS`.

## Control results

| ID | Assertion | Before | Evidence (before, at `305826d`) | Change made | After | Re-verify command |
|---|---|---|---|---|---|---|
| APP-TXN-01 | Amount is positive and bounded on deposit / withdraw / transfer | **FAIL** | No amount check anywhere on the money-movement path: `src/main/java/com/example/bankapp/service/AccountService.java:51-62` (deposit), `:64-78` (withdraw), `:103-135` (transfer). A negative transfer inverts the legs and debits the recipient (`:112-117`). No bound in the UI either: `src/main/resources/templates/dashboard.html:147,163,183` | `validateAmount` rejects null, ≤ 0, > 2 decimal places and amounts above `MAX_TRANSACTION_AMOUNT` (1,000,000.00), called first in all three operations; self-transfer rejected; overdraft check retained for withdraw and transfer; `min`/`max`/`step` added to the amount inputs | **PASS** | `AccountService.java:61-77, 79-100, 102-126, 151-195`; tests `src/test/java/com/example/bankapp/service/AccountServiceValidationTest.java:70-134` — `./mvnw test -Dtest=AccountServiceValidationTest` |
| APP-TXN-02 | Transfer executes atomically in a single transaction | **FAIL** | `transferAmount` had no `@Transactional`; four separate repository writes auto-commit individually, so a failure after the debit destroys money: `AccountService.java:103-135` (saves at `:113`, `:117`, `:126`, `:134`) | `@Transactional` on `deposit`, `withdraw` and `transferAmount` | **PASS** | `AccountService.java:79, 102, 151`; re-performed by failure injection on the credit leg in `src/test/java/com/example/bankapp/service/TransferAtomicityIntegrationTest.java:58-75` (balances and both ledger rows roll back) — `./mvnw test -Dtest=TransferAtomicityIntegrationTest` |
| APP-TXN-03 | Per-transaction limits or maker-checker on money movement | **FAIL** | No limit and no second-approver step on any path: `AccountService.java:51-62, 64-78, 103-135`; `BankController.java:50-56, 58-72, 82-96` posts straight through | Partially remediated: hard per-transaction cap of 1,000,000.00 (`MAX_TRANSACTION_AMOUNT`) enforced server-side. Daily/cumulative limits and maker-checker are **not** implemented | **PARTIAL** (residual R-1) | `AccountService.java:27, 74-76`; test `AccountServiceValidationTest.java:87-95` |
| APP-ACC-04 | Roles/authorities supporting segregation of duties | **FAIL** | Every authenticated principal gets a single hardcoded authority: `AccountService.java:99-101`; authorization is `anyRequest().authenticated()` with no role checks: `SecurityConfig.java:31-34` | Not changed — a real role model (`Account.role`) requires a schema change, which is out of scope for this PR | **FAIL** (residual R-2) | `AccountService.java:147-149`, `SecurityConfig.java:30-33` |
| APP-SEC-05 | CSRF protection enabled on state-changing endpoints | **FAIL** | `.csrf(csrf -> csrf.disable())` at `SecurityConfig.java:30`; no token in any form: `dashboard.html:144,160,176`, `login.html:108`, `register.html:108`; logout was a `GET` link: `dashboard.html:120`, `transactions.html:99` | Removed the `disable()` call (Spring Security default `CsrfFilter` now active), added `_csrf` hidden inputs to every POST form, and converted logout to a POST form matched with `AntPathRequestMatcher("/logout", "POST")` | **PASS** | `SecurityConfig.java:28-33, 43`; `dashboard.html:122,150,167,184`, `transactions.html:101`, `login.html:109`, `register.html:109`; tests `src/test/java/com/example/bankapp/controller/BankControllerCsrfTest.java:46-63` (403 without token, redirect with token) — `./mvnw test -Dtest=BankControllerCsrfTest` |
| ITGC-SEC-06 | No credentials committed to the repository | **FAIL** | Plaintext DB root password in `src/main/resources/application.properties:4-5`; same value in `docker-compose.yml:7,26` and base64 in `kubernetes/secrets.yaml:8-9` | Not changed — removing them requires rotating the credential and provisioning external secret storage; deleting them here would break the documented local/EKS flow without fixing exposure (the value is already in git history) | **FAIL** (residual R-3) | `application.properties:4-5`, `docker-compose.yml:7,26`, `kubernetes/secrets.yaml:8-9` |
| ITGC-LOG-11 | Audit log written for every financial event | **FAIL** | No logging of any kind on the money-movement path: `AccountService.java:51-62, 64-78, 103-135`; the `Transaction` rows are business records, not an audit trail — they carry no actor, outcome or reason: `model/Transaction.java:25-30` | New `AuditLogger` writes a structured `AUDIT` line — `timestamp`, `actor`, `action`, `amount`, `fromAccountId`, `toAccountId`, `outcome`, `reason` — on both the success and failure path of deposit, withdraw and transfer | **PASS** | `src/main/java/com/example/bankapp/audit/AuditLogger.java:18-30`; call sites `AccountService.java:84, 99, 110, 125, 165, 194`; test `AccountServiceValidationTest.java:136-142`. Observed during the test run: `event=financial_transaction timestamp=... actor=csrf-test-... action=DEPOSIT amount=10.00 fromAccountId=7 toAccountId=null outcome=SUCCESS reason=""` |

Line numbers in the "After" column refer to this change; "Before" line numbers refer to commit `305826d`.

## Residual findings (deliberately not fixed here)

| Ref | Control | Finding | Reason not fixed in this PR | Owner |
|---|---|---|---|---|
| R-1 | APP-TXN-03 | No daily/cumulative limits and no maker-checker approval on high-value transfers; only a single per-transaction cap exists | Maker-checker needs a pending-approval entity plus approver UI and a schema change, which the scope of this PR excludes | Application owner (Retail Banking Engineering) with Product Risk |
| R-2 | APP-ACC-04 | Single hardcoded `USER` authority; no roles, no segregation of duties between initiator and approver | A role model requires an `Account.role` column and a data migration; the PR is constrained not to change the DB schema strategy | Application owner with IAM |
| R-3 | ITGC-SEC-06 | DB root credential `Test@123` committed in `application.properties`, `docker-compose.yml` and `kubernetes/secrets.yaml`, and present in git history | Requires credential rotation and an external secret store (AWS Secrets Manager / K8s External Secrets); removing the literals alone leaves the history exposed and gives false assurance | Platform / DevSecOps |
| R-4 | ITGC-CM-07 | No `CODEOWNERS` and no branch protection on `DevOps`; commits land directly (e.g. `66c677d`, `4e4274c` pushed straight to the branch) | Repository administration, not a code change; must be applied in GitHub settings | Repository admin / Engineering management |
| R-5 | ITGC-CM-08 | CI builds a different upstream repository: `code_checkout("https://github.com/LondheShubham153/Springboot-BankApp.git","DevOps")` — `Jenkinsfile:26` — so pipeline results are not evidence about this codebase | Pipeline configuration change with a blast radius beyond this PR; needs a separate CI change and re-baselining of the scans | DevSecOps / Build engineering |
| R-6 | ITGC-DATA-10 | `spring.jpa.hibernate.ddl-auto=update` (`application.properties:9`) lets the runtime mutate the schema uncontrolled | The PR is explicitly constrained not to change the DB schema strategy; needs a migration tool (Flyway/Liquibase) and a cutover plan | Application owner with DBA |
