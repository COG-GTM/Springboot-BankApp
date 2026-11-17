# Infrastructure Inventory & Gap Analysis — Springboot-BankApp

This document inventories the application and its **existing** deployment
artifacts (Helm, raw Kubernetes manifests, Jenkins/GitOps pipeline, eksctl
docs), then maps the app's real requirements to a governed, production-oriented
AWS design and lists the gaps the new Terraform + Sentinel layer closes.

> There was **no Terraform and no policy-as-code in this repo before this
> change**. Everything under `infra/` was generated from scratch by reading the
> app and its deployment artifacts. Nothing here has been applied — this is
> generation only.

---

## 1. Application

| Property | Value | Source |
|---|---|---|
| Framework | Spring Boot 3.3.3 (Java 17) | `pom.xml` |
| Persistence | Spring Data JPA → MySQL 8.x | `pom.xml`, `application.properties` |
| Web/UI | Thymeleaf + Spring Security | `pom.xml` |
| Listen port | 8080 | `Dockerfile`, manifests |
| DB name | `BankDB` (k8s/docker), `bankappdb` (local) | manifests, `application.properties` |
| Schema mgmt | `spring.jpa.hibernate.ddl-auto=update` | `application.properties` |

**App-level observations feeding the infra design**
- Needs one MySQL database reachable on 3306 with TLS; today it uses an
  in-cluster MySQL with node-local storage.
- Needs its DB credentials injected as a secret; today they are hardcoded
  (`Test@123`) in `application.properties`, Helm values, and a base64
  "secret".
- Stateless web tier (good fit for HPA + rolling deploys).
- Manifests reference `/actuator/health` probes, but `pom.xml` does not include
  `spring-boot-starter-actuator` — probes would 404. Flagged, not fixed (app
  code out of scope).

---

## 2. Existing deployment inventory

### 2.1 Helm chart (`helm/bankapp/`)
- Namespace `bankapp-namespace`; app image `trainwithshubham/springboot-bankapp:latest`.
- MySQL as a **StatefulSet** + headless service, 5Gi PVC, `mysql:latest`.
- NodePort 30080; HPA 1–5.
- Hardcoded `MYSQL_ROOT_PASSWORD: Test@123` / `SPRING_DATASOURCE_PASSWORD: Test@123`.

### 2.2 Raw Kubernetes (`kubernetes/`)
- App **Deployment** `trainwithshubham/bankapp-eks:v2`, 2 replicas, ConfigMap + Secret.
- MySQL as a **Deployment** (not StatefulSet) + normal service, `mysql:8.0`.
- PersistentVolume is **hostPath** `/mnt/data/mysql`, `storageClassName: standard`.
- `secrets.yaml` holds base64 of `Test@123` (encoding, not encryption).
- NGINX ingress + cert-manager for `megaproject.trainwithshubham.com`.

> Helm and raw manifests describe the **same app two different ways**
> (StatefulSet vs Deployment for MySQL, different images/labels). This drift is
> a deployment risk in its own right.

### 2.3 CI/CD
- `Jenkinsfile` (CI): Trivy, OWASP Dependency-Check, SonarQube, Docker build/push;
  checks out an **external** repo `LondheShubham153/Springboot-BankApp`.
- `GitOps/Jenkinsfile` (CD): `sed` image bump + git push to `DevOps`; Argo CD
  reconciles. **The sed targets a non-existent filename — see
  `ARGOCD-RCA.md`.**

### 2.4 eksctl documentation
- `README.md`: region `us-west-1`, k8s `1.30`, node `t2.medium` ×2, SSH enabled.
- `kubernetes/README.md`: region `ap-south-1`, k8s `1.31`, node `t2.medium`.
- NodePort/ArgoCD/Prometheus/Grafana exposed manually.

---

## 3. Requirements → generated infrastructure

| App requirement | Generated implementation |
|---|---|
| Run EKS workload on private nodes | `modules/vpc` (private subnets + NAT), `modules/eks` (nodes in private subnets, private API endpoint) |
| MySQL persistence, durable + encrypted | `modules/rds` (MySQL 8.0, `storage_encrypted`, Multi-AZ, backups) |
| App reads DB secret without static creds | `modules/rds` `manage_master_user_password` (Secrets Manager) + `modules/iam` IRSA role scoped to that secret |
| Ingress for the web tier | Public subnets tagged `kubernetes.io/role/elb` for AWS LB controller |
| Observability / audit | VPC Flow Logs, EKS control-plane logs, RDS log exports — all KMS-encrypted |
| Autoscaling compatibility | Private subnets tagged for cluster-autoscaler; ASG-backed managed node group |
| Encryption everywhere | Dedicated rotated KMS keys for logs / EKS secrets+EBS / RDS |

---

## 4. Gap analysis (current → governed)

| # | Gap in existing setup | Risk | Closed by |
|---|---|---|---|
| G1 | **No encryption at rest** (hostPath/EBS/DB unencrypted) | Data exposure | RDS `storage_encrypted`, EKS secrets envelope encryption, encrypted EBS, KMS log encryption + `enforce-encryption-at-rest.sentinel` |
| G2 | **Hardcoded credentials** in properties/values/secrets | Credential leak | RDS-managed master secret + IRSA least-privilege read; no secrets in code |
| G3 | **No/'standard' storage on EKS**, hostPath | Data loss on node churn | Managed RDS replaces in-cluster MySQL |
| G4 | **No mandatory tagging** | No cost/ownership/audit attribution | Provider `default_tags` + `require-tags.sentinel` |
| G5 | **No logging/audit** (no flow logs, no EKS/RDS logs) | Blind to incidents | Flow logs, EKS control-plane logs, RDS exports + `require-logging.sentinel` |
| G6 | **Public/over-broad access** (public nodes, SSH, NodePorts) | Attack surface | Private nodes, private API endpoint, `restrict-public-access.sentinel` |
| G7 | **No least-privilege IAM** | Lateral movement | Scoped IRSA (explicit secret+KMS ARNs), IMDSv2, hop-limit 1 + `restrict-iam-wildcards.sentinel` |
| G8 | **Region drift** (us-west-1 vs ap-south-1) | Ungoverned placement | `approved-regions.sentinel` |
| G9 | **Previous-gen instance types** (`t2.medium`) | Perf/cost/EOL | `approved-instance-types.sentinel` (m6i/… , db.t3+) |
| G10 | **Broken GitOps image bump** (`.yaml` vs `.yml`) | Stale deploys | `ARGOCD-RCA.md` (fix documented; pipeline owned elsewhere) |
| G11 | **Manifest drift** (Helm vs raw; missing actuator) | Unreliable deploys/probes | Documented; Argo CD `Application` pins one source + ignores HPA replica drift |

---

## 5. Assumptions

1. **Region**: standardized on `us-west-1` (matches `README.md`); `ap-south-1`
   in `kubernetes/README.md` treated as drift. Approved set is `us-west-1`,
   `us-east-1` — adjust in `approved-regions.sentinel` + `terraform.tfvars`.
2. **No apply**: backend is intentionally commented out and no
   `terraform apply`/`plan` against real AWS was run. State/backend must be
   configured by the platform team before use (see `README.md` in `infra/`).
3. **Managed RDS replaces in-cluster MySQL**. The Helm/raw MySQL manifests are
   left untouched (app repo out of scope); migration is described in the infra
   `README.md`.
4. **Credentials** come from RDS-managed Secrets Manager; the app should read
   via IRSA (role ARN is a module output). Rewiring `SPRING_DATASOURCE_*` to the
   RDS endpoint/secret is an app/manifest change left to the owners.
5. **Instance sizing** (`m6i.large`, `db.t3.medium`) are sane demo defaults, not
   a capacity-planning outcome.
6. **CIDR layout** (`10.0.0.0/16` with /20 public+private and /24 database
   subnets across 2 AZs) is a standard starting point; widen AZs for true HA.
7. **Sentinel** is authored for Terraform Cloud/Enterprise; validated locally
   with the Sentinel CLI via mocks (`infra/policies/sentinel/test`).
