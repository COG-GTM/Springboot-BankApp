# Validation & Security Scan Results

Tooling versions: Terraform `v1.12.1`, tfsec `v1.28.14`, checkov `3.3.8`,
Sentinel `v0.40.0`. All commands are read-only — **no `terraform apply` and no
`terraform plan` against real AWS was executed.**

## Terraform fmt + validate

```
$ terraform fmt -recursive        # no diffs
$ terraform -chdir=environments/prod init -backend=false
$ terraform -chdir=environments/prod validate
Success! The configuration is valid.
```

## tfsec

```
$ tfsec infra/terraform
  passed               76
No problems detected!
```

Two findings were addressed by design and two by justified inline ignores:
- RDS IAM database authentication enabled (removed `aws-rds-enable-iam-auth`).
- EKS node egress to internet — required for image pulls via NAT; ignored with
  justification (`aws-ec2-no-public-egress-sgr`).
- VPC flow-log IAM `logs:CreateLogStream` on `<group-arn>:*` — tightest scoping
  AWS allows; ignored with justification (`aws-iam-no-policy-wildcards`).

## checkov

```
$ checkov -d infra/terraform
Passed checks: 235, Failed checks: 0, Skipped checks: 10
```

Skips are all justified inline:
- `CKV_AWS_109 / CKV_AWS_111 / CKV_AWS_356` on the three KMS key policies — the
  root "AccountAdmin" (`kms:*`) statement is the AWS-recommended baseline for a
  KMS key policy (Resource `*` = the key itself); omitting it risks an
  unmanageable key.
- `CKV_AWS_382` on the EKS node egress rule — worker nodes require outbound
  internet via NAT.

Fixes applied to clear checkov findings (rather than skip):
- `CKV_AWS_161` RDS IAM auth → `iam_database_authentication_enabled = true`.
- `CKV_AWS_341` IMDS hop limit → `http_put_response_hop_limit = 1` (pods use IRSA).
- `CKV_AWS_339` EKS version → `1.31`.
- `CKV2_AWS_64` KMS key policies defined for the EKS and RDS keys.
- `CKV2_AWS_69` RDS TLS in transit → parameter group `require_secure_transport = 1`.

## Sentinel policy tests

Policies are unit-tested with the Sentinel CLI against compliant/non-compliant
mocks (`infra/policies/sentinel/test`).

```
$ cd infra/policies/sentinel && sentinel test
PASS - restrict-public-access.sentinel
PASS - enforce-encryption-at-rest.sentinel
PASS - approved-regions.sentinel
PASS - approved-instance-types.sentinel
PASS - restrict-iam-wildcards.sentinel
PASS - require-tags.sentinel
PASS - require-logging.sentinel
7 tests completed
```

Each policy has a `pass` case (main = true against a compliant plan) and a
`fail` case (main = false against a plan that mirrors the current
untagged/unencrypted/public/wildcard/no-logging setup).
