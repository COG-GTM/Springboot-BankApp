# Open Questions

Every question below is a fact the repository cannot settle. Each names the role that owns the
answer and the design decision it unblocks. IDs are referenced from `target-state.md` and
`migration-plan.md`.

## Programme

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q1** | No approved ("blessed") AWS service list was supplied. The list in `target-state.md` §1 is an assumption made by us so design could start. **Is that list correct, and if not, what is the authoritative list?** Everything in the target design is contingent on the answer. | Platform Engineering | WS0 entry; all of `target-state.md` §3 |
| **Q2** | No platform migration guardrails were supplied. `target-state.md` §2 uses a default set, labelled as assumed. **Do you have your own guardrails, and do they replace or extend these ten?** | Platform Engineering | WS0 entry; the guardrail-compliance table |
| **Q3** | Is the organisation's standard managed container platform ECS Fargate or EKS? The design assumes ECS; if it is EKS, rows 1, 11 and 13 of the target table change (and much of the existing Kubernetes material becomes reusable). | Platform Engineering | WS4 |
| **Q4** | Is this repository actually what runs in production? Both Jenkins jobs check out `LondheShubham153/Springboot-BankApp` (`Jenkinsfile:26`, `GitOps/Jenkinsfile:21`), and the Kubernetes Deployment runs `trainwithshubham/bankapp-eks:v2` (`kubernetes/bankapp-deployment.yml:20`), which this pipeline never produces. **We may be migrating an artefact nobody deploys.** | Application Owner | Everything. This is the first question to answer |
| **Q5** | Is there an existing AWS landing zone / Control Tower estate, or is WS0 building one from nothing? | Platform Engineering | WS0 sizing |
| **Q6** | Who is the named application owner, cost centre and data classification for the mandatory tag set? | Application Owner | G10, WS5 |
| **Q7** | What is the target production domain name and which zone hosts it? The repo uses `megaproject.trainwithshubham.com` (`kubernetes/bankapp-ingress.yml:15`), a third-party demo domain. | Application Owner + Platform Engineering | WS9 |

## Database

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q8** | What is the production data volume (row counts for `account` and `transaction`, total size on disk) and peak transaction rate? The repo shows the schema shape only — two Hibernate-generated tables (`model/Account.java`, `model/Transaction.java`) — with no volumetrics. | DBA | WS3 approach (DMS full-load vs full-load + CDC) and cutover window |
| **Q9** | What are the RTO and RPO for this workload? Aurora Global Database, backup retention and whether a warm standby is sufficient all follow from the numbers. | Application Owner + DBA | WS9, G7 |
| **Q10** | The schema is currently generated at runtime by `spring.jpa.hibernate.ddl-auto=update` (`application.properties:9`). **Can we take a definitive DDL snapshot of the live production schema?** Hibernate's `update` mode never drops anything, so the live schema may contain columns and indexes no longer present in the entity classes. | DBA | WS3 baseline migration script |
| **Q11** | Is there any consumer of the MySQL database other than this application (reporting, ETL, direct queries)? Nothing in the repo indicates one, but the repo cannot prove a negative about external consumers. | DBA + Application Owner | WS3, WS10 cutover scope |
| **Q12** | Are `BigDecimal` balances currently stored as `DECIMAL` in MySQL, and with what precision/scale? Hibernate's defaults for an unannotated `BigDecimal` (`model/Account.java:19`) can differ from what production actually holds; a precision change during the PostgreSQL migration would silently alter monetary values. | DBA | WS3 validation criteria |

## Platform

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q13** | Sessions are held in each JVM's memory and the Ingress sets no session affinity (`kubernetes/bankapp-ingress.yml:6-10`). Is ALB application-based stickiness acceptable as the interim, or must we move to stateless authentication before production? An in-list session store does not exist on the assumed service list. | Platform Engineering + Application Owner | WS4 |
| **Q14** | Is Route 53 failover (DNS-TTL-bounded) acceptable for cross-region failover, or is a sub-minute failover requirement going to force Global Accelerator onto the approved list? | Platform Engineering | WS9, G7 |
| **Q15** | Which approved base container image should replace `openjdk:17-alpine` (`Dockerfile:28`), and where is it published? | Platform Engineering | WS2 |
| **Q16** | Which internal Maven repository must the build resolve dependencies from? The build currently resolves from Maven Central via the wrapper (`.mvn/wrapper/maven-wrapper.properties`). | Platform Engineering | WS2, G9 |

## Delivery

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q17** | Which pipeline product is the approved delivery toolchain, and who owns onboarding this repository onto it? | Release Manager | WS2 |
| **Q18** | Is the Jenkins estate (`Jenkinsfile`, `GitOps/Jenkinsfile`, the `Shared` library in `vars/`) shared with other applications, or can it be decommissioned with this workload? | Release Manager | WS2 exit |
| **Q19** | ArgoCD is documented (`kubernetes/README.md:116-133`) but no `Application` manifest is committed. **Is ArgoCD actually in use for this workload today?** If so, its configuration lives outside this repository and we need it. | Release Manager + Platform Engineering | WS2 current-state completeness |
| **Q20** | What release cadence and change-approval process applies? The CD job commits directly to a branch with no review (`GitOps/Jenkinsfile:50-63`). | Release Manager | WS2, WS10 |

## Security

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q21** | Database credentials are committed in four locations (`current-state.md` §7 S1). **Have these values ever been used against a production database, and if so, when will they be rotated and revoked?** Values are deliberately not reproduced here or anywhere in this package. | Security | WS1; WS10 entry criterion |
| **Q22** | CSRF is disabled (`config/SecurityConfig.java:30`) on a cookie-session application with state-changing POSTs. Is there a compensating control today (WAF rule, gateway), or is this an unmitigated exposure that must be fixed before cutover? | Security | WS6 |
| **Q23** | Is there a required authorisation/segregation-of-duties model? Today every user receives the single authority `"USER"` (`service/AccountService.java:99-101`). | Security + Application Owner | WS6 scope |
| **Q24** | What are the regulatory audit-logging requirements for financial events? The application logs no financial events today (`current-state.md` §7 S7). | Security + Application Owner | WS5, WS6 |
| **Q25** | Is TLS to the database mandatory in all environments? Every environment definition currently sets `useSSL=false` (`application.properties:3`, `kubernetes/configmap.yaml:8`, `docker-compose.yml:25`). | Security | WS4, G8 |
| **Q26** | Which internal registry and scanning policy governs container images, and what severity threshold blocks a release? Today Trivy runs with no threshold (`vars/trivy_scan.groovy:2`) and the SonarQube gate is explicitly non-blocking (`vars/sonarqube_code_quality.groovy:3`). | Security + Release Manager | WS2 |

## Operations

| ID | Question | Owner | Blocks |
|---|---|---|---|
| **Q27** | Which central observability platform must receive logs and metrics, and what are the mandatory tag keys? | Platform Engineering | WS5, G10 |
| **Q28** | What is the acceptable cutover window (planned downtime) for the database switch, and who signs off on it? | Application Owner + Release Manager | WS10 |
| **Q29** | Who operates this workload after cutover, and what on-call/runbook standard must be met before production entry? | Application Owner + Platform Engineering | WS10 |
| **Q30** | What functional acceptance criteria will be used to validate the migrated system? Automated coverage today is a single `contextLoads()` test (`src/test/java/com/example/bankapp/BankappApplicationTests.java:9-11`), so acceptance will be manual unless a test suite is built (WS6). | Application Owner + QA | WS8, WS10 |
