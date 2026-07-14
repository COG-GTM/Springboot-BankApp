# Springboot-BankApp — Infrastructure & Governance (IaC)

Production-oriented, **generation-only** infrastructure-as-code and
policy-as-code for Springboot-BankApp. Nothing here has been applied.

```
infra/
├── terraform/
│   ├── modules/            # reusable modules
│   │   ├── vpc/            # VPC, subnets, NAT, flow logs (KMS)
│   │   ├── eks/            # EKS cluster, IRSA OIDC, private managed nodes
│   │   ├── rds/            # encrypted Multi-AZ MySQL, managed master secret
│   │   └── iam/            # IRSA role, least-privilege secret/KMS access
│   └── environments/
│       └── prod/           # composition + concrete demo values (no backend)
├── policies/sentinel/      # governance policies + CLI mock tests
├── argocd/                 # Argo CD Application for the GitOps target
└── docs/                   # inventory, gap analysis, RCA, scan results
```

Start with `docs/infrastructure-inventory.md` (inventory + gap analysis +
assumptions), then `docs/validation-and-scans.md` and `docs/ARGOCD-RCA.md`.

## Modules

| Module | Purpose | Key safeguards |
|---|---|---|
| `vpc` | Networking across ≥2 AZs | Private/public/database tiers, NAT, KMS-encrypted flow logs, locked default SG |
| `eks` | Managed cluster + nodes | Private API endpoint, control-plane logging, secrets envelope encryption, private nodes, IMDSv2 + hop-limit 1, encrypted EBS |
| `rds` | MySQL 8.0 | `storage_encrypted`, Multi-AZ, backups, deletion protection, TLS-required param group, IAM auth, log exports, RDS-managed master secret |
| `iam` | Workload access (IRSA) | Trust scoped to `namespace:serviceaccount`; only `GetSecretValue`/`DescribeSecret` on the exact secret ARN + `Decrypt`/`DescribeKey` on the exact KMS ARN; no wildcards |

All modules are consumed by `environments/prod`, which also provisions three
rotated KMS keys (logs / EKS / RDS) for blast-radius isolation.

## Prerequisites before this can be applied (by the platform team)

1. **Remote state + locking** — create an encrypted S3 bucket (versioned) and a
   DynamoDB lock table, then uncomment and fill the `backend "s3"` block in
   `environments/prod/versions.tf`. State contains sensitive values; keep it
   encrypted and access-controlled.
2. **Credentials** — assume a role with least privilege for plan/apply; never
   long-lived keys in CI.
3. **Policy enforcement** — attach `policies/sentinel` as a Terraform
   Cloud/Enterprise policy set (or run in CI via the Sentinel CLI with a mock
   generated from the plan).

## Recommended workflow (plan / review / apply separation)

```bash
cd infra/terraform/environments/prod
terraform init                      # with backend configured
terraform plan -out=tfplan.bin      # review; feeds Sentinel
terraform show -json tfplan.bin > tfplan.json
# Sentinel gates here (TFC policy set or CLI). Human approval required.
terraform apply tfplan.bin          # only after review + policy pass
```

Local checks used in this repo (no cloud calls):

```bash
terraform fmt -recursive && terraform validate
tfsec infra/terraform
checkov -d infra/terraform
cd infra/policies/sentinel && sentinel test
```

## Migrating from in-cluster MySQL to RDS

1. Apply the Terraform (VPC/EKS/RDS/IAM) in a non-prod account first.
2. Take a logical dump of the in-cluster MySQL (`mysqldump BankDB`).
3. Restore into RDS over TLS; validate row counts.
4. Repoint the app: set `SPRING_DATASOURCE_URL` to the RDS endpoint and read the
   password from the RDS-managed secret via the IRSA role (`iam` module output),
   removing the hardcoded values in ConfigMap/Secret/Helm values.
5. Cut over during a maintenance window; keep the in-cluster MySQL until RDS is
   validated, then remove the MySQL manifests/PV/PVC.

## Rollback

- **Terraform**: infra changes roll back by reverting the module/env commit and
  re-planning; destructive changes are gated by `deletion_protection` (RDS) and
  `prevent_destroy`-style review. Never `terraform apply` an unreviewed plan.
- **Database**: RDS automated backups + final snapshot enable point-in-time
  restore; cut back to the retained in-cluster MySQL during migration if needed.
- **App / GitOps**: Argo CD keeps deployment history — roll back with
  `argocd app rollback bankapp <revision>` (or revert the image-tag commit).
  See `docs/ARGOCD-RCA.md` for the CD pipeline defect to fix first.

## No-apply boundary

This deliverable is **generation only**. The S3 backend is intentionally
commented out, and no `terraform apply` (or cloud-touching `plan`) was run. All
validation was static (`fmt`/`validate`/`tfsec`/`checkov`) or mock-based
(Sentinel). Applying is a deliberate, reviewed action for the platform team.
