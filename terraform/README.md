# BankApp AWS Infrastructure — Terraform

Production-ready Terraform modules for deploying the Spring Boot banking application on AWS EKS.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                              VPC                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │ Public Sub-a │  │ Public Sub-b │  │ Public Sub-c │  ← ALB/IGW   │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │Private Sub-a │  │Private Sub-b │  │Private Sub-c │  ← EKS Nodes │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │  DB Sub-a    │  │  DB Sub-b    │  │  DB Sub-c    │  ← RDS MySQL │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
```

## Modules

| Module | Description |
|--------|-------------|
| `vpc` | VPC with public, private, and database subnets, NAT gateways, flow logs |
| `eks` | EKS cluster with managed node groups, OIDC provider, KMS encryption, add-ons |
| `rds` | RDS MySQL 8.0 with parameter groups, encryption, Performance Insights, alarms |
| `iam` | Base IAM roles for EKS cluster and node groups (no EKS dependency) |
| `irsa` | OIDC-based IRSA workload roles: bankapp, ALB controller, autoscaler, external DNS (depends on EKS) |
| `security-groups` | Security groups for EKS cluster, nodes, RDS, and ALB |

## Prerequisites

1. AWS CLI configured with appropriate credentials
2. Terraform >= 1.5.0
3. S3 bucket and DynamoDB table for remote state (per environment)

### Create Remote State Resources

```bash
# Replace <env> with dev, staging, or prod
aws s3api create-bucket \
  --bucket bankapp-terraform-state-<env> \
  --region us-east-1

aws s3api put-bucket-versioning \
  --bucket bankapp-terraform-state-<env> \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket bankapp-terraform-state-<env> \
  --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"aws:kms"}}]}'

aws dynamodb create-table \
  --table-name bankapp-terraform-locks-<env> \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

## Usage

### Initialize (per environment)

```bash
cd terraform

# Dev
terraform init -backend-config=environments/dev/backend.tfvars

# Staging
terraform init -backend-config=environments/staging/backend.tfvars

# Production
terraform init -backend-config=environments/prod/backend.tfvars
```

### Plan

```bash
terraform plan \
  -var-file=environments/dev/terraform.tfvars \
  -var="database_master_password=<SECURE_PASSWORD>"
```

### Apply

```bash
terraform apply \
  -var-file=environments/dev/terraform.tfvars \
  -var="database_master_password=<SECURE_PASSWORD>"
```

### Configure kubectl

After applying, configure kubectl using the output:

```bash
aws eks update-kubeconfig \
  --region us-east-1 \
  --name $(terraform output -raw cluster_name)
```

## Environment Differences

| Feature | Dev | Staging | Prod |
|---------|-----|---------|------|
| AZs | 2 | 3 | 3 |
| NAT Gateway | Single | Single | One per AZ |
| EKS Nodes | t3.medium (1-4) | t3.large (2-6) | m5.xlarge (3-10) + spot |
| RDS Instance | db.t3.medium | db.t3.large | db.r5.xlarge |
| RDS Multi-AZ | No | Yes | Yes |
| RDS Backups | 3 days | 7 days | 30 days |
| Deletion Protection | No | Yes | Yes |
| Performance Insights | No | Yes | Yes |
| CloudWatch Alarms | No | Yes | Yes |
| API Endpoint | Public | Public | Private only |

## Security Considerations

- All data at rest is encrypted via KMS (EKS secrets, RDS storage)
- RDS is deployed in isolated database subnets with no internet access
- EKS nodes run in private subnets behind NAT gateways
- IRSA (IAM Roles for Service Accounts) for least-privilege pod access
- VPC flow logs enabled for network monitoring
- Security groups follow least-privilege with source-based rules
- Production EKS API endpoint is private-only
