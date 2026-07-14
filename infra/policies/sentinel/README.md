# Sentinel Policy Set — Springboot-BankApp

Governance policies for the Terraform in `infra/terraform`. Designed for a
Terraform Cloud/Enterprise policy set (see `sentinel.hcl`) and unit-tested
locally with the Sentinel CLI against mocks.

| Policy | Enforcement | What it blocks |
|---|---|---|
| `require-tags` | hard-mandatory | Taggable resources missing any of: Application, Environment, Owner, CostCenter, DataClassification, ManagedBy |
| `enforce-encryption-at-rest` | hard-mandatory | Unencrypted RDS, EKS secrets, node EBS, or CloudWatch log groups |
| `restrict-public-access` | hard-mandatory | Public RDS, public-IP subnets, sensitive ports (22/3306/3389/6443) open to 0.0.0.0/0 |
| `approved-regions` | hard-mandatory | AWS provider region outside the approved list |
| `approved-instance-types` | soft-mandatory | EKS node / RDS instance classes outside the approved list |
| `restrict-iam-wildcards` | hard-mandatory | IAM `Action:"*"`, `service:*`, or wildcard-resource on mutating actions |
| `require-logging` | hard-mandatory | Missing EKS control-plane logs, VPC flow logs, or RDS log exports |

`modules/resources.sentinel` holds shared helpers (`find_resources`, `planned`).

## Run the tests

```bash
cd infra/policies/sentinel
sentinel fmt -check *.sentinel modules/*.sentinel
sentinel test
```

Each policy has `test/<policy>/pass.hcl` (compliant plan → `main = true`) and
`test/<policy>/fail.hcl` (non-compliant plan → `main = false`), backed by
`test/mock-tfplan-pass.sentinel` and `test/mock-tfplan-fail.sentinel`.

## Enforce against a real plan (in CI or TFC)

```bash
cd infra/terraform/environments/prod
terraform plan -out=tfplan.bin
terraform show -json tfplan.bin > tfplan.json
# In TFC: attach this directory as a policy set to the workspace.
# Locally: generate a tfplan/v2 mock from tfplan.json and run `sentinel apply`.
```

Editing the approved lists: regions in `approved-regions.sentinel`; instance
classes in `approved-instance-types.sentinel`; mandatory tags in
`require-tags.sentinel`.
