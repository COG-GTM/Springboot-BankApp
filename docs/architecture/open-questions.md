# Open Questions — Springboot-BankApp AWS Migration

Everything below is something that **could not be determined from the repository**. Each item names
the role that owns the answer, states what the repo does show, and says why the answer changes the
design. Take this list into the architecture review.

Legend for blocking level:
**B1** = blocks target-state design sign-off · **B2** = blocks build start · **B3** = blocks prod cutover.

---

## A. Programme / scope

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| A1 | **What is the approved AWS service list?** The request contained the literal placeholder `[PASTE BLESSED SERVICE LIST]`. `target-state.md` was written against an assumed list and every off-list item is flagged — which of those flags are real? | Cloud Platform Architect | n/a | **B1** |
| A2 | Is this repository the system of record for the production banking application, or a reference/demo? Both Jenkinsfiles build a *different* upstream repo (`Jenkinsfile:26`, `GitOps/Jenkinsfile:21`) and the deployed image (`trainwithshubham/bankapp-eks:v2`, `bankapp-deployment.yml:20`) is not the image CI publishes. | Application Owner | Four divergent image references; pipeline points at `LondheShubham153/Springboot-BankApp` | **B1** |
| A3 | **Where does the code that is actually running in prod live?** If it is not this repo, the current-state analysis needs re-running against that source before any of it is trusted. | Application Owner / Release Manager | see A2 | **B1** |
| A4 | Is the intent lift-and-shift, or is the app being remediated during migration? Three defects (non-atomic transfers, disabled CSRF, single `"USER"` role) are correctness/compliance blockers that AWS cannot fix. | Application Owner + Head of Engineering | `AccountService.java:103-135`, `SecurityConfig.java:30`, `AccountService.java:99-101` | **B1** |
| A5 | Which of dev / test / UAT / prod actually exist today, and where? The repo contains a single `application.properties` with no profile variants. | Platform Engineering | no `application-*.properties` in `src/main/resources` | **B1** |

## B. Database (DBA)

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| B1 | **What is the current production data volume** — row counts for `account` and `transaction`, total DB size, and monthly growth? Sizing, migration window, and DMS instance class all depend on it. | DBA | Only arbitrary-looking storage requests: 10Gi (`persistent-volume-claim.yaml:11`) and 5Gi (`helm/bankapp/values.yaml:14`) | **B1** |
| B2 | **Is there a schema DDL artefact anywhere outside Hibernate?** `spring.jpa.hibernate.ddl-auto=update` (`application.properties:9`) means the live schema was created by the ORM and may have drifted from the entity classes. We need a `mysqldump --no-data` of prod to convert. | DBA | Only `CREATE DATABASE bankappdb;` (`src/main/resources/static/mysql/SQLScript.txt:1`) | **B2** |
| B3 | Are there **any objects in the live MySQL database not represented in the entity classes** — views, triggers, stored procedures, events, additional tables, or manually added indexes/constraints? The code has zero native SQL and zero proc calls, so anything of this kind is invisible to us. | DBA | 2 entities only (`Account.java`, `Transaction.java`); 0 `@Query`, 0 `JdbcTemplate` | **B2** |
| B4 | What is the **actual MySQL version and character set/collation** in prod? Manifests say `8.0` (`mysql-deployment.yml:20`) but Compose and Helm say `latest` (`docker-compose.yml:4`, `values.yaml:27`). | DBA | version drift across environments | **B2** |
| B5 | What **decimal precision** is required for `balance` and `amount`? Hibernate defaults an unannotated `BigDecimal` to `decimal(19,2)`; nothing in the code states the business requirement, and MySQL→Postgres `numeric` behaviour differs at the edges. | DBA + Application Owner | `Account.java:19`, `Transaction.java:13` — no `@Column(precision=…)` | **B2** |
| B6 | What are the current **backup, retention, and RPO/RTO commitments** for this database? The manifests define no backup job at all, and the data sits on node-local hostPath storage (`persistent-volume.yaml:14-16`) — so either there is an out-of-band backup we cannot see, or there is none. | DBA | no CronJob, no snapshot config anywhere | **B1** |
| B7 | **Is there any other consumer of this database** — reporting, ETL, BI, a downstream reconciliation feed, direct analyst access? If so, a Postgres cutover breaks them. | DBA + Data Platform | app-side: single JPA datasource, no other clients in repo | **B1** |
| B8 | What **downtime window** is acceptable for the MySQL→Postgres cutover? This determines DMS CDC vs. a simple dump-and-load. | DBA + Application Owner | n/a | **B1** |
| B9 | Are DB connections currently pooled/limited anywhere outside HikariCP defaults? Fargate task scaling multiplies connection count and RDS has a hard `max_connections`. | DBA | no pool config in `application.properties` | **B2** |

## C. Platform / infrastructure (Platform Engineering)

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| C1 | **Is there IaC for the current environment anywhere outside this repo** (Terraform, CloudFormation, Ansible)? The repo has none — infrastructure exists only as `eksctl` commands pasted into markdown (`README.md:62-82`). | Platform Engineering | no `.tf`, no template files | **B1** |
| C2 | Which **AWS account(s), region(s), and VPC/subnet/CIDR** are we targeting? The repo says us-west-1 (`README.md:63`), which is unlikely to be the bank's standard region. | Platform Engineering / Landing Zone team | `README.md:62-82` | **B1** |
| C3 | What is the real production **hostname**? The repo hardcodes `megaproject.trainwithshubham.com` (`bankapp-ingress.yml:15,18`), `bankapp.local` (`helm/.../ingress.yml:10`), and `bank.joakim.online` (`nginx.md:31`) — three different names, none plausibly the bank's. | Platform Engineering / Network | hardcoded hostnames | **B2** |
| C4 | Is the application **internet-facing or internal-only**? Design differs materially (WAF, CloudFront, public vs. private ALB). | Application Owner + Network Security | ingress with public Let's Encrypt cert implies public | **B1** |
| C5 | Who currently operates the **EKS cluster, Jenkins controller, SonarQube, and ArgoCD**? None of them are described as owned artefacts, and three of them are in the release path. | Platform Engineering | `Jenkinsfile:6`, `kubernetes/README.md:93-106` | **B1** |
| C6 | What is the expected **traffic profile** — peak concurrent users, RPS, and daily transaction count? The HPA targets 40% CPU on 500m limits (`bankapp-hpa.yml:19`, `bankapp-deployment.yml:60-62`), which tells us nothing about real load. | Application Owner + Platform Engineering | resource limits only | **B1** |
| C7 | Are there **egress restrictions / proxy requirements** for outbound HTTPS? Today the pipeline reaches Docker Hub, GitHub, NVD, and the Trivy DB, and cert-manager reaches Let's Encrypt. On AWS, ECR/Secrets Manager/CloudWatch access should be via VPC endpoints — is that mandated? | Network Security | integration list, `current-state.md` §4 | **B2** |
| C8 | Is **CloudFront permitted in front of an authenticated banking origin**, and is self-hosting the three CDN libraries (`dashboard.html:5,194-196`) acceptable/required? | Cloud Platform Architect + AppSec | public CDN references in templates | **B2** |
| C9 | Are there **hardcoded IPs or hostnames referenced by anything outside this repo** — firewall rules, allow-lists, monitoring checks, partner integrations — that will break when the endpoint moves? | Network / Platform Engineering | none in app code; only the hostnames in C3 | **B3** |

## D. Delivery / CI-CD (Release Engineering)

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| D1 | **Does the CD pipeline actually work today?** The `sed` step edits `kubernetes/bankapp-deployment.yaml`, but the file is `bankapp-deployment.yml` — the substitution is a silent no-op (`GitOps/Jenkinsfile:40`). So how does a new image tag actually reach the cluster — manually? | Release Manager | filename mismatch | **B1** |
| D2 | Which **Harness account/org/project** and which delivery pattern (blue/green vs. canary) are standard here? | Release Manager + Harness admin | n/a | **B2** |
| D3 | Is **Jenkins staying as CI**, or is Harness CI in scope too? The constraint names Harness for CD only. | Release Manager | `Jenkinsfile:1-87` | **B1** |
| D4 | What are the **mandatory release gates** (change ticket, CAB, four-eyes approval, evidence capture)? Today the SonarQube gate cannot fail a build (`vars/sonarqube_code_quality.groovy:3`, `abortPipeline: false`), Trivy has no `--exit-code` (`vars/trivy_scan.groovy:2`), and the image build skips tests (`Dockerfile:21`). | Release Manager + Internal Audit | non-blocking gates | **B3** |
| D5 | Where do the **Jenkins credentials** `docker`, `dockerhub`, and `Github-cred` come from, who owns them, and are they in scope to be migrated/revoked? | Release Manager + Security | `vars/docker_push.groovy:2`, `vars/pushImage.groovy:2`, `GitOps/Jenkinsfile:50` |**B3** |
| D6 | Is there a **rollback procedure** today, and what is the RTO for a bad release? Nothing in the repo describes one. | Release Manager | absence | **B1** |

## E. Security, secrets, and compliance (Security / AppSec)

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| E1 | **The DB password `Test@123` is committed in four places** (`application.properties:5`, `secrets.yaml:8-9`, `helm/bankapp/values.yaml:52-53`, `docker-compose.yml:7,26`). Is this the real production credential, and if so, has it been rotated / is rotation in scope now? | Security + DBA | committed plaintext and base64 | **B2** |
| E2 | Are there **credentials or config used in prod that are NOT in this repo** — injected by the platform, held in Jenkins, or set by hand on the cluster? We can only see what is committed. | Platform Engineering + Security | ConfigMap/Secret manifests only | **B1** |
| E3 | Which **secrets store** is approved — Secrets Manager, Parameter Store SecureString, or an existing enterprise vault — and what rotation period applies to RDS credentials? | Security / Cloud Platform | no secret manager in use today | **B1** |
| E4 | What **audit-logging and retention obligations** apply to financial events? There is currently **no audit log of deposits, withdrawals, or transfers anywhere** — only `spring.jpa.show-sql=true` (`application.properties:11`), which dumps SQL to stdout. | Compliance / Internal Audit | absence of logging | **B3** |
| E5 | Is **open self-service registration** (`/register` is `permitAll()`, `SecurityConfig.java:32`) intended in production, or is account creation supposed to be an onboarded/KYC flow? | Application Owner + Compliance | `SecurityConfig.java:31-34` | **B1** |
| E6 | Does any of this data fall under **GDPR/PII or PCI scope**? Determines encryption, log redaction, and DR-region eligibility. | Compliance / Data Protection Officer | `Account` holds username + password + balance | **B1** |
| E7 | Is **TLS required in transit to the database**? Every connection string today disables it (`useSSL=false` in `application.properties:3`, `configmap.yaml:8`, `docker-compose.yml:25`). RDS should enforce it. | Security + DBA | `useSSL=false` | **B2** |
| E8 | Who accepts the risk that **CSRF is disabled** (`SecurityConfig.java:30`) on money-moving POST endpoints (`BankController.java:50,58,82`) until the code is fixed? | AppSec + Application Owner | `SecurityConfig.java:30` | **B3** |

## F. Operations and external dependencies (Ops / App Owner)

| # | Question | Addressed to | What the repo shows | Blocks |
|---|---|---|---|---|
| F1 | **Are there any external cron jobs, batch runs, or ops scripts touching this application or its database** — end-of-day processing, statement generation, reconciliation, archival, cleanup? The application itself has **zero** scheduled work (no `@Scheduled` anywhere), so anything of this kind is external and invisible to us. | Application Owner + Ops | absence in `src/` | **B1** |
| F2 | Is there any **file-based interface** (SFTP drop, shared mount, report export)? The app performs no file I/O at all, so any such interface lives outside it. | Application Owner + Ops | no file I/O in `src/` | **B1** |
| F3 | Does the application send or receive **any email** in production? The only mail path found is a Jenkins build notification to a Gmail address (`GitOps/Jenkinsfile:73,90`) over port 465 (`README.md:189`). | Application Owner | absence of mail client in app | **B2** |
| F4 | **Who is `trainwithshubham@gmail.com`** (`letsencrypt-clusterissuer.yaml:8`, `GitOps/Jenkinsfile:73,90`) and why is an external personal address the ACME contact and the CD notification recipient? | Platform Engineering + Security | hardcoded external address | **B2** |
| F5 | What **monitoring, alerting, and on-call** exists today? The repo installs `kube-prometheus-stack` (`README.md:308`) but defines no alert rules, no SLOs, and no runbooks. | SRE / Ops | absence | **B1** |
| F6 | Has anyone observed the **session-affinity bug** in production — users logged out at random? With 2 replicas (`bankapp-deployment.yml:9`) and no session store, this should be happening today. Its absence would suggest prod is running a single instance. | Application Owner + SRE | no `spring-session` dependency | **B1** |
| F7 | Are `/actuator/*` endpoints expected to exist? Two deployment paths health-check `/actuator/health` (`docker-compose.yml:36`, `helm/bankapp/templates/deployment.yml:45,51`) while the actuator dependency is **absent from `pom.xml`** — so those probes cannot ever pass. | Application Owner + Platform | missing dependency | **B2** |
| F8 | What are the agreed **RTO/RPO for prod**, and does "multi-region" mean active/active, warm standby, or pilot light? The cost and complexity differ by an order of magnitude. | Application Owner + Resilience/BCM | n/a | **B1** |
