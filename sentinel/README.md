# Sentinel Policy Set for Terraform Enterprise

Governance-as-code policies for AWS infrastructure used by the Springboot-BankApp,
deployed on AWS EKS with Jenkins CI, ArgoCD CD, and a MySQL backend.

---

## Policy Structure

```
sentinel/
├── sentinel.hcl                                          # Policy set configuration
├── common/
│   └── tfplan-functions.sentinel                         # Shared helper functions
├── policies/
│   ├── encryption/
│   │   ├── require-s3-encryption.sentinel                # S3 SSE + public access block
│   │   ├── require-rds-encryption.sentinel               # RDS storage encryption + KMS
│   │   ├── require-ebs-encryption.sentinel               # EBS / launch template / EC2
│   │   ├── require-eks-encryption.sentinel               # EKS secrets envelope encryption
│   │   ├── require-sqs-sns-encryption.sentinel           # SQS / SNS at-rest encryption
│   │   ├── require-elasticache-encryption.sentinel       # ElastiCache at-rest + in-transit
│   │   └── require-cloudwatch-log-encryption.sentinel    # CloudWatch Log Group KMS
│   ├── access-control/
│   │   ├── restrict-iam-policies.sentinel                # Block wildcard / admin policies
│   │   ├── restrict-security-group-ingress.sentinel      # Block open ingress on sensitive ports
│   │   ├── require-vpc-flow-logs.sentinel                # VPC flow log existence + config
│   │   ├── restrict-public-access.sentinel               # RDS, ES, Redshift, ECR, Lambda
│   │   ├── require-imdsv2.sentinel                       # EC2 / launch template IMDSv2
│   │   └── restrict-rds-public-access.sentinel           # RDS VPC placement + multi-AZ
│   ├── tagging/
│   │   └── enforce-mandatory-tags.sentinel               # Mandatory tag keys + allowed values
│   └── region/
│       └── restrict-aws-regions.sentinel                 # Restrict to approved regions
└── test/                                                 # Test fixture directories
```

---

## Enforcement Levels

| Level | Meaning | Override? |
|-------|---------|-----------|
| **hard-mandatory** | Blocks `terraform apply` on violation. No override possible. | No |
| **soft-mandatory** | Blocks by default, but authorized users can override with justification. | Yes |
| **advisory** | Logged in run output but does not block. | N/A |

---

## AWS Resource Type → Governance Rule Mapping

### Compute

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_instance` | EBS root volume encryption (hard) | IMDSv2 required (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_launch_template` | EBS block device encryption (hard) | IMDSv2 required (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_lambda_function` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_lambda_function_url` | — | IAM auth required (hard) | — | — |

### Containers & Kubernetes

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_eks_cluster` | Secrets envelope encryption with KMS (hard) | Private API endpoint, audit logging (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_eks_node_group` | Via launch template (hard) | Launch template required for IMDSv2 (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_ecr_repository` | — | Scan-on-push, tag immutability (hard) | Mandatory tags (soft) | Region-restricted (hard) |

### Databases

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_db_instance` | Storage encryption + KMS key (hard) | Not publicly accessible, DB subnet group, multi-AZ, deletion protection, 7-day backup retention (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_rds_cluster` | Storage encryption (hard) | DB subnet group, deletion protection (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_elasticache_replication_group` | At-rest + in-transit encryption (hard) | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_elasticache_cluster` | Engine-level checks (hard) | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_redshift_cluster` | — | Not publicly accessible (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_elasticsearch_domain` | — | VPC placement required (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_opensearch_domain` | — | VPC placement required (hard) | Mandatory tags (soft) | Region-restricted (hard) |

### Storage

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_s3_bucket` | SSE-S3 or SSE-KMS (hard) | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_s3_bucket_server_side_encryption_configuration` | AES256 / aws:kms / aws:kms:dsse (hard) | — | — | — |
| `aws_s3_bucket_public_access_block` | — | Block all public access (hard) | — | — |
| `aws_ebs_volume` | Encrypted (hard) | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_efs_file_system` | — | — | Mandatory tags (soft) | Region-restricted (hard) |

### Networking

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_vpc` | — | Flow logs required (soft) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_subnet` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_security_group` | — | No unrestricted ingress on sensitive ports (hard) | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_security_group_rule` | — | No unrestricted ingress on sensitive ports (hard) | — | — |
| `aws_vpc_security_group_ingress_rule` | — | No unrestricted ingress on sensitive ports (hard) | — | — |
| `aws_lb` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_lb_target_group` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_nat_gateway` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_eip` | — | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_flow_log` | — | ALL traffic type, CW Logs or S3 destination (soft) | Mandatory tags (soft) | — |

### IAM & Security

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_iam_role` | — | Max 4h session, no admin policy attachment (hard) | Mandatory tags (soft) | Global service |
| `aws_iam_policy` | — | No wildcard actions/resources (hard) | — | Global service |
| `aws_iam_role_policy_attachment` | — | Block AdministratorAccess / IAMFullAccess / PowerUserAccess (hard) | — | Global service |
| `aws_iam_user` | — | Minimize creation (prefer roles) (hard) | — | Global service |
| `aws_iam_instance_profile` | — | — | — | Global service |
| `aws_iam_openid_connect_provider` | — | — | — | Global service |
| `aws_kms_key` | — | — | Mandatory tags (soft) | Region-restricted (hard) |

### Messaging

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_sqs_queue` | KMS or SQS-managed SSE (soft) | — | Mandatory tags (soft) | Region-restricted (hard) |
| `aws_sns_topic` | KMS encryption (soft) | — | Mandatory tags (soft) | Region-restricted (hard) |

### Observability

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_cloudwatch_log_group` | KMS encryption (soft), retention ≤ 365 days | — | Mandatory tags (soft) | Region-restricted (hard) |

### CDN & DNS (Global Services)

| AWS Resource Type | Encryption | Access Control | Tagging | Region |
|---|---|---|---|---|
| `aws_cloudfront_distribution` | — | — | — | Global service |
| `aws_route53_zone` | — | — | — | Global service |
| `aws_route53_record` | — | — | — | Global service |
| `aws_waf_web_acl` / `aws_wafv2_web_acl` | — | — | — | Global service |

---

## Enforcement Level Recommendations

### Hard-Mandatory (Cannot Override)

These rules protect against data breaches, credential theft, and unauthorized access.
They should **never** be bypassed in a banking application.

| Policy | Rationale |
|--------|-----------|
| `require-s3-encryption` | Prevents unencrypted storage of artifacts, backups, and logs |
| `require-rds-encryption` | Banking data at rest must be encrypted (PCI DSS, SOC 2) |
| `require-ebs-encryption` | Worker node disks may contain cached application data |
| `require-eks-encryption` | Kubernetes secrets (DB creds, TLS certs) must be envelope-encrypted |
| `require-elasticache-encryption` | Session tokens and cached data must be encrypted at rest and in transit |
| `restrict-iam-policies` | Prevents privilege escalation via overly permissive policies |
| `restrict-security-group-ingress` | Blocks internet exposure of databases, admin panels, and SSH |
| `restrict-public-access` | Ensures data stores and registries are not publicly accessible |
| `require-imdsv2` | Mitigates SSRF-based credential theft on EC2/EKS nodes |
| `restrict-rds-public-access` | Database must be VPC-internal with multi-AZ for availability |
| `restrict-aws-regions` | Data residency and compliance; prevents shadow deployments |

### Soft-Mandatory (Override with Justification)

These rules enforce best practices but may have legitimate exceptions during
development or migration.

| Policy | Rationale | When to Override |
|--------|-----------|------------------|
| `require-sqs-sns-encryption` | Messaging encryption is best practice | Dev/test queues with no sensitive data |
| `require-cloudwatch-log-encryption` | Log encryption protects stack traces | Short-lived debugging log groups |
| `require-vpc-flow-logs` | Network forensics and audit trail | Temporary sandbox VPCs |
| `enforce-mandatory-tags` | Cost allocation and ownership | Automated/ephemeral resources with short lifespans |

---

## Mandatory Tags

All taggable resources must include:

| Tag Key | Description | Example Values |
|---------|-------------|----------------|
| `Environment` | Deployment environment | `production`, `staging`, `development`, `testing`, `dr` |
| `Application` | Application identifier | `bankapp`, `bankapp-mysql`, `bankapp-monitoring` |
| `Owner` | Team or individual responsible | `platform-team`, `devops@company.com` |
| `CostCenter` | Financial cost allocation code | `CC-1234`, `engineering-infra` |

---

## Restricted Ports (Security Group Ingress)

The following ports are blocked from `0.0.0.0/0` and `::/0` ingress:

| Port | Service | Reason |
|------|---------|--------|
| 22 | SSH | Remote access must use VPN or bastion |
| 3306 | MySQL | Database access is VPC-internal only |
| 3389 | RDP | Windows remote desktop must not be public |
| 5432 | PostgreSQL | Database access is VPC-internal only |
| 6379 | Redis | Cache access is VPC-internal only |
| 9000 | SonarQube | Code quality tool is internal only |
| 27017 | MongoDB | Database access is VPC-internal only |
| 9200 | Elasticsearch | Search engine must not be public |
| 8081 | Jenkins | CI server must not be public |

---

## Approved Regions

| Region | Purpose |
|--------|---------|
| `us-west-1` | Primary (N. California) — EKS cluster, RDS, application |
| `us-west-2` | DR / failover (Oregon) |
| `us-east-1` | Global services (CloudFront, ACM, IAM) |

---

## Usage with Terraform Enterprise

### 1. Register the Policy Set

In TFE, navigate to **Settings → Policy Sets → Connect a new policy set** and
point to the `sentinel/` directory in this repository.

### 2. Scope to Workspaces

Scope the policy set to workspaces managing the BankApp infrastructure:

- `bankapp-eks-cluster`
- `bankapp-rds`
- `bankapp-networking`
- `bankapp-monitoring`

### 3. Policy Check Workflow

```
terraform plan → Sentinel evaluates → Pass/Fail → terraform apply (if passed)
```

- **hard-mandatory** failures block the run entirely
- **soft-mandatory** failures require an authorized override
- **advisory** failures are logged but do not block

### 4. Testing Policies Locally

Use the [Sentinel CLI](https://developer.hashicorp.com/sentinel/docs/commands) to
test policies before pushing:

```bash
sentinel test -run=require-s3-encryption
sentinel test -run=enforce-mandatory-tags
```
