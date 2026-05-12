# Terraform Module Plan: BankApp AWS Infrastructure

This document defines the Terraform module architecture for provisioning the Springboot-BankApp's AWS infrastructure. It replaces the current manual `eksctl`/`kubectl` provisioning workflow documented in `README.md` and `kubernetes/README.md` with a repeatable, version-controlled Infrastructure-as-Code (IaC) approach.

---

## Table of Contents

1. [Current State Analysis](#current-state-analysis)
2. [Module Architecture](#module-architecture)
3. [Directory Structure](#directory-structure)
4. [Module Specifications](#module-specifications)
   - [Root Module](#root-module)
   - [VPC Module](#vpc-module)
   - [EKS Module](#eks-module)
   - [RDS Module](#rds-module)
   - [IAM Module](#iam-module)
   - [Security Groups Module](#security-groups-module)
5. [Variable Definitions](#variable-definitions)
6. [Output Definitions](#output-definitions)
7. [State Management](#state-management)
8. [Migration Path](#migration-path)
9. [CI/CD Integration](#cicd-integration)
10. [Security Considerations](#security-considerations)

---

## Current State Analysis

The application is currently deployed using manual CLI commands:

| Resource | Current Provisioning Method | Key Parameters |
|---|---|---|
| **EKS Cluster** | `eksctl create cluster` | Name: `bankapp`, Region: `us-west-1`, K8s v1.30, no node group |
| **Node Group** | `eksctl create nodegroup` | `t2.medium`, 2 nodes, 29GB storage, SSH access |
| **IAM OIDC** | `eksctl utils associate-iam-oidc-provider` | Cluster-specific OIDC for service account IAM roles |
| **VPC/Subnets** | Auto-created by `eksctl` | Default CIDR, public/private subnets across AZs |
| **Security Groups** | Manually opened ports in AWS Console | 8080 (app), 9000 (SonarQube), 8081 (Jenkins), NodePort ranges |
| **MySQL** | In-cluster deployment (`mysql:8.0` pod) | `BankDB`, root auth, 10Gi hostPath PV |
| **Ingress** | NGINX Ingress Controller via Helm | Let's Encrypt TLS, domain: `megaproject.trainwithshubham.com` |
| **Monitoring** | `kube-prometheus-stack` Helm chart | Prometheus + Grafana, NodePort exposure |

### Current Gaps Addressed by Terraform

- No version control over infrastructure changes
- Manual security group edits are error-prone and unauditable
- MySQL runs as an in-cluster pod with hostPath storage (data loss risk on node failure)
- No disaster recovery or multi-environment support
- OIDC provider and IAM roles are created ad-hoc

---

## Module Architecture

```
                    +-------------------+
                    |   Root Module     |
                    | (environments/    |
                    |  dev/prod/staging)|
                    +--------+----------+
                             |
          +------------------+------------------+
          |         |         |        |        |
     +----v---+ +--v---+ +--v---+ +--v---+ +--v--------+
     |  VPC   | | EKS  | | RDS  | | IAM  | | Security  |
     | Module | |Module| |Module| |Module| | Groups    |
     +--------+ +------+ +------+ +------+ | Module    |
                                            +-----------+
```

All modules are composed via the root module. Cross-module dependencies are passed as outputs/inputs (e.g., VPC ID from VPC module flows into EKS and RDS modules).

---

## Directory Structure

```
terraform/
├── modules/
│   ├── vpc/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── README.md
│   ├── eks/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── node_groups.tf
│   │   ├── addons.tf
│   │   └── README.md
│   ├── rds/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── README.md
│   ├── iam/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── README.md
│   └── security-groups/
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── README.md
├── environments/
│   ├── dev/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── terraform.tfvars
│   │   ├── backend.tf
│   │   └── providers.tf
│   ├── staging/
│   │   └── ... (same structure)
│   └── prod/
│       └── ... (same structure)
├── TERRAFORM_MODULE_PLAN.md   # This document
└── README.md                  # Usage instructions
```

---

## Module Specifications

### Root Module

Each environment directory (`environments/dev/`, `environments/prod/`, etc.) acts as a root module that composes all child modules.

**`environments/dev/main.tf`** (example):

```hcl
module "vpc" {
  source = "../../modules/vpc"

  project_name     = var.project_name
  environment      = var.environment
  vpc_cidr         = var.vpc_cidr
  public_subnets   = var.public_subnets
  private_subnets  = var.private_subnets
  availability_zones = var.availability_zones
}

module "security_groups" {
  source = "../../modules/security-groups"

  project_name = var.project_name
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id

  eks_cluster_sg_ingress_rules = var.eks_cluster_sg_ingress_rules
  rds_allowed_cidr_blocks      = [var.vpc_cidr]
  app_port                     = var.app_port
}

module "iam" {
  source = "../../modules/iam"

  project_name           = var.project_name
  environment            = var.environment
  eks_cluster_name       = var.eks_cluster_name
  oidc_provider_arn      = module.eks.oidc_provider_arn
  oidc_provider_url      = module.eks.oidc_provider_url
  enable_ebs_csi_driver  = true
  enable_alb_controller  = true
}

module "eks" {
  source = "../../modules/eks"

  project_name       = var.project_name
  environment        = var.environment
  cluster_name       = var.eks_cluster_name
  cluster_version    = var.eks_cluster_version
  vpc_id             = module.vpc.vpc_id
  subnet_ids         = module.vpc.private_subnet_ids
  cluster_sg_id      = module.security_groups.eks_cluster_sg_id

  node_groups = var.node_groups

  cluster_role_arn   = module.iam.eks_cluster_role_arn
  node_role_arn      = module.iam.eks_node_role_arn
}

module "rds" {
  source = "../../modules/rds"

  project_name           = var.project_name
  environment            = var.environment
  db_name                = var.db_name
  db_username            = var.db_username
  db_instance_class      = var.db_instance_class
  db_allocated_storage   = var.db_allocated_storage
  db_engine_version      = var.db_engine_version
  vpc_id                 = module.vpc.vpc_id
  subnet_ids             = module.vpc.private_subnet_ids
  db_security_group_id   = module.security_groups.rds_sg_id
  multi_az               = var.rds_multi_az
}
```

---

### VPC Module

Provisions the network foundation: VPC, public/private subnets, NAT gateway, internet gateway, and route tables. Subnets are tagged for EKS auto-discovery.

**`modules/vpc/main.tf`** key resources:

```hcl
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.project_name}-${var.environment}-vpc"
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_subnet" "public" {
  count                   = length(var.public_subnets)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnets[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name                                          = "${var.project_name}-${var.environment}-public-${count.index + 1}"
    "kubernetes.io/role/elb"                       = "1"
    "kubernetes.io/cluster/${var.project_name}-${var.environment}" = "shared"
  }
}

resource "aws_subnet" "private" {
  count             = length(var.private_subnets)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnets[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name                                          = "${var.project_name}-${var.environment}-private-${count.index + 1}"
    "kubernetes.io/role/internal-elb"              = "1"
    "kubernetes.io/cluster/${var.project_name}-${var.environment}" = "shared"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}

resource "aws_eip" "nat" {
  domain = "vpc"
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }
}

resource "aws_route_table_association" "public" {
  count          = length(var.public_subnets)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = length(var.private_subnets)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
```

**`modules/vpc/variables.tf`**:

```hcl
variable "project_name" {
  description = "Project name used for resource naming and tagging"
  type        = string
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnets" {
  description = "List of public subnet CIDR blocks"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnets" {
  description = "List of private subnet CIDR blocks"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.20.0/24"]
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["us-west-1a", "us-west-1c"]
}
```

**`modules/vpc/outputs.tf`**:

```hcl
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "List of public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "List of private subnet IDs"
  value       = aws_subnet.private[*].id
}

output "nat_gateway_id" {
  description = "NAT Gateway ID"
  value       = aws_nat_gateway.main.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR block"
  value       = aws_vpc.main.cidr_block
}
```

---

### EKS Module

Provisions the EKS control plane, managed node groups, OIDC provider, and EKS add-ons (CoreDNS, kube-proxy, VPC-CNI). Matches current `eksctl` configuration.

**`modules/eks/main.tf`** key resources:

```hcl
resource "aws_eks_cluster" "main" {
  name     = var.cluster_name
  version  = var.cluster_version
  role_arn = var.cluster_role_arn

  vpc_config {
    subnet_ids              = var.subnet_ids
    security_group_ids      = [var.cluster_sg_id]
    endpoint_private_access = true
    endpoint_public_access  = true
  }

  tags = {
    Name        = var.cluster_name
    Environment = var.environment
    Project     = var.project_name
  }
}

data "tls_certificate" "eks" {
  url = aws_eks_cluster.main.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks" {
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = [data.tls_certificate.eks.certificates[0].sha1_fingerprint]
  url             = aws_eks_cluster.main.identity[0].oidc[0].issuer
}
```

**`modules/eks/node_groups.tf`**:

```hcl
resource "aws_eks_node_group" "main" {
  for_each = var.node_groups

  cluster_name    = aws_eks_cluster.main.name
  node_group_name = each.key
  node_role_arn   = var.node_role_arn
  subnet_ids      = var.subnet_ids

  instance_types = each.value.instance_types
  disk_size      = each.value.disk_size
  capacity_type  = lookup(each.value, "capacity_type", "ON_DEMAND")

  scaling_config {
    desired_size = each.value.desired_size
    min_size     = each.value.min_size
    max_size     = each.value.max_size
  }

  remote_access {
    ec2_ssh_key               = each.value.ssh_key_name
    source_security_group_ids = lookup(each.value, "ssh_security_group_ids", null)
  }

  labels = lookup(each.value, "labels", {})

  tags = {
    Name        = "${var.cluster_name}-${each.key}"
    Environment = var.environment
  }
}
```

**`modules/eks/addons.tf`**:

```hcl
resource "aws_eks_addon" "vpc_cni" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "vpc-cni"
}

resource "aws_eks_addon" "coredns" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "coredns"
  depends_on   = [aws_eks_node_group.main]
}

resource "aws_eks_addon" "kube_proxy" {
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "kube-proxy"
}

resource "aws_eks_addon" "ebs_csi" {
  count        = var.enable_ebs_csi_driver ? 1 : 0
  cluster_name = aws_eks_cluster.main.name
  addon_name   = "aws-ebs-csi-driver"
  service_account_role_arn = var.ebs_csi_role_arn
}
```

**`modules/eks/variables.tf`**:

```hcl
variable "project_name" {
  description = "Project name for tagging"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "bankapp"
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS cluster"
  type        = string
  default     = "1.30"
}

variable "vpc_id" {
  description = "VPC ID where the cluster will be created"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for the EKS cluster"
  type        = list(string)
}

variable "cluster_sg_id" {
  description = "Security group ID for the EKS cluster"
  type        = string
}

variable "cluster_role_arn" {
  description = "IAM role ARN for the EKS cluster"
  type        = string
}

variable "node_role_arn" {
  description = "IAM role ARN for the EKS node group"
  type        = string
}

variable "node_groups" {
  description = "Map of EKS managed node group configurations"
  type = map(object({
    instance_types       = list(string)
    disk_size            = number
    desired_size         = number
    min_size             = number
    max_size             = number
    ssh_key_name         = string
    ssh_security_group_ids = optional(list(string))
    capacity_type        = optional(string, "ON_DEMAND")
    labels               = optional(map(string), {})
  }))
  default = {
    bankapp = {
      instance_types = ["t2.medium"]
      disk_size      = 29
      desired_size   = 2
      min_size       = 2
      max_size       = 2
      ssh_key_name   = "eks-nodegroup-key"
    }
  }
}

variable "enable_ebs_csi_driver" {
  description = "Enable the EBS CSI driver add-on"
  type        = bool
  default     = true
}

variable "ebs_csi_role_arn" {
  description = "IAM role ARN for EBS CSI driver"
  type        = string
  default     = null
}
```

**`modules/eks/outputs.tf`**:

```hcl
output "cluster_id" {
  description = "EKS cluster ID"
  value       = aws_eks_cluster.main.id
}

output "cluster_name" {
  description = "EKS cluster name"
  value       = aws_eks_cluster.main.name
}

output "cluster_endpoint" {
  description = "EKS cluster API endpoint"
  value       = aws_eks_cluster.main.endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64-encoded CA certificate for cluster authentication"
  value       = aws_eks_cluster.main.certificate_authority[0].data
}

output "oidc_provider_arn" {
  description = "ARN of the OIDC provider"
  value       = aws_iam_openid_connect_provider.eks.arn
}

output "oidc_provider_url" {
  description = "URL of the OIDC provider"
  value       = aws_iam_openid_connect_provider.eks.url
}

output "cluster_security_group_id" {
  description = "Cluster-managed security group ID"
  value       = aws_eks_cluster.main.vpc_config[0].cluster_security_group_id
}
```

---

### RDS Module

Replaces the in-cluster MySQL pod with a managed AWS RDS MySQL instance. Provides automated backups, Multi-AZ failover, and encryption at rest.

**`modules/rds/main.tf`** key resources:

```hcl
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-${var.environment}-db-subnet"
  subnet_ids = var.subnet_ids

  tags = {
    Name        = "${var.project_name}-${var.environment}-db-subnet"
    Environment = var.environment
  }
}

resource "aws_db_instance" "mysql" {
  identifier              = "${var.project_name}-${var.environment}-mysql"
  engine                  = "mysql"
  engine_version          = var.db_engine_version
  instance_class          = var.db_instance_class
  allocated_storage       = var.db_allocated_storage
  max_allocated_storage   = var.db_max_allocated_storage
  storage_encrypted       = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  multi_az               = var.multi_az
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.db_security_group_id]

  backup_retention_period = var.backup_retention_period
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  skip_final_snapshot       = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${var.project_name}-${var.environment}-final" : null
  deletion_protection       = var.environment == "prod"

  parameter_group_name = aws_db_parameter_group.mysql.name

  tags = {
    Name        = "${var.project_name}-${var.environment}-mysql"
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_db_parameter_group" "mysql" {
  name   = "${var.project_name}-${var.environment}-mysql-params"
  family = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }
}
```

**`modules/rds/variables.tf`**:

```hcl
variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "db_name" {
  description = "Name of the MySQL database"
  type        = string
  default     = "BankDB"
}

variable "db_username" {
  description = "Master username for the database"
  type        = string
  default     = "bankapp_admin"
}

variable "db_password" {
  description = "Master password for the database (use secrets manager in production)"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.medium"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 20
}

variable "db_max_allocated_storage" {
  description = "Max allocated storage for autoscaling in GB"
  type        = number
  default     = 100
}

variable "db_engine_version" {
  description = "MySQL engine version"
  type        = string
  default     = "8.0"
}

variable "multi_az" {
  description = "Enable Multi-AZ deployment"
  type        = bool
  default     = false
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "db_security_group_id" {
  description = "Security group ID for the RDS instance"
  type        = string
}

variable "backup_retention_period" {
  description = "Number of days to retain automated backups"
  type        = number
  default     = 7
}
```

**`modules/rds/outputs.tf`**:

```hcl
output "db_instance_endpoint" {
  description = "RDS instance endpoint (hostname:port)"
  value       = aws_db_instance.mysql.endpoint
}

output "db_instance_address" {
  description = "RDS instance hostname"
  value       = aws_db_instance.mysql.address
}

output "db_instance_port" {
  description = "RDS instance port"
  value       = aws_db_instance.mysql.port
}

output "db_instance_id" {
  description = "RDS instance ID"
  value       = aws_db_instance.mysql.id
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.mysql.db_name
}

output "db_subnet_group_name" {
  description = "DB subnet group name"
  value       = aws_db_subnet_group.main.name
}
```

---

### IAM Module

Creates IAM roles and policies for the EKS cluster, node groups, and IRSA (IAM Roles for Service Accounts) for workloads.

**`modules/iam/main.tf`** key resources:

```hcl
# --- EKS Cluster Role ---

resource "aws_iam_role" "eks_cluster" {
  name = "${var.project_name}-${var.environment}-eks-cluster-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "eks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_cluster_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role       = aws_iam_role.eks_cluster.name
}

resource "aws_iam_role_policy_attachment" "eks_vpc_resource_controller" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSVPCResourceController"
  role       = aws_iam_role.eks_cluster.name
}

# --- EKS Node Group Role ---

resource "aws_iam_role" "eks_node" {
  name = "${var.project_name}-${var.environment}-eks-node-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "eks_worker_node_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node.name
}

resource "aws_iam_role_policy_attachment" "eks_cni_policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node.name
}

resource "aws_iam_role_policy_attachment" "ecr_read_only" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node.name
}

# --- EBS CSI Driver IRSA Role ---

resource "aws_iam_role" "ebs_csi_driver" {
  count = var.enable_ebs_csi_driver ? 1 : 0
  name  = "${var.project_name}-${var.environment}-ebs-csi-driver-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRoleWithWebIdentity"
      Effect = "Allow"
      Principal = {
        Federated = var.oidc_provider_arn
      }
      Condition = {
        StringEquals = {
          "${replace(var.oidc_provider_url, "https://", "")}:sub" = "system:serviceaccount:kube-system:ebs-csi-controller-sa"
          "${replace(var.oidc_provider_url, "https://", "")}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ebs_csi_driver" {
  count      = var.enable_ebs_csi_driver ? 1 : 0
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEBSCSIDriverPolicy"
  role       = aws_iam_role.ebs_csi_driver[0].name
}

# --- ALB Ingress Controller IRSA Role ---

resource "aws_iam_role" "alb_controller" {
  count = var.enable_alb_controller ? 1 : 0
  name  = "${var.project_name}-${var.environment}-alb-controller-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRoleWithWebIdentity"
      Effect = "Allow"
      Principal = {
        Federated = var.oidc_provider_arn
      }
      Condition = {
        StringEquals = {
          "${replace(var.oidc_provider_url, "https://", "")}:sub" = "system:serviceaccount:kube-system:aws-load-balancer-controller"
          "${replace(var.oidc_provider_url, "https://", "")}:aud" = "sts.amazonaws.com"
        }
      }
    }]
  })
}
```

**`modules/iam/variables.tf`**:

```hcl
variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "eks_cluster_name" {
  description = "EKS cluster name for role naming"
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider (for IRSA)"
  type        = string
  default     = null
}

variable "oidc_provider_url" {
  description = "URL of the EKS OIDC provider (for IRSA)"
  type        = string
  default     = null
}

variable "enable_ebs_csi_driver" {
  description = "Create IAM role for EBS CSI driver"
  type        = bool
  default     = true
}

variable "enable_alb_controller" {
  description = "Create IAM role for ALB Ingress Controller"
  type        = bool
  default     = true
}
```

**`modules/iam/outputs.tf`**:

```hcl
output "eks_cluster_role_arn" {
  description = "ARN of the EKS cluster IAM role"
  value       = aws_iam_role.eks_cluster.arn
}

output "eks_node_role_arn" {
  description = "ARN of the EKS node group IAM role"
  value       = aws_iam_role.eks_node.arn
}

output "ebs_csi_driver_role_arn" {
  description = "ARN of the EBS CSI driver IRSA role"
  value       = var.enable_ebs_csi_driver ? aws_iam_role.ebs_csi_driver[0].arn : null
}

output "alb_controller_role_arn" {
  description = "ARN of the ALB controller IRSA role"
  value       = var.enable_alb_controller ? aws_iam_role.alb_controller[0].arn : null
}
```

---

### Security Groups Module

Centralizes all security group definitions. Replaces the current manual port-opening workflow in the AWS Console.

**`modules/security-groups/main.tf`** key resources:

```hcl
# --- EKS Cluster Security Group (additional rules) ---

resource "aws_security_group" "eks_cluster" {
  name_prefix = "${var.project_name}-${var.environment}-eks-cluster-"
  vpc_id      = var.vpc_id
  description = "Security group for EKS cluster control plane"

  tags = {
    Name        = "${var.project_name}-${var.environment}-eks-cluster-sg"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "eks_cluster_ingress" {
  for_each = var.eks_cluster_sg_ingress_rules

  type              = "ingress"
  from_port         = each.value.from_port
  to_port           = each.value.to_port
  protocol          = each.value.protocol
  cidr_blocks       = each.value.cidr_blocks
  security_group_id = aws_security_group.eks_cluster.id
  description       = each.value.description
}

resource "aws_security_group_rule" "eks_cluster_egress_all" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.eks_cluster.id
  description       = "Allow all outbound traffic"
}

# --- EKS Node Security Group ---

resource "aws_security_group" "eks_nodes" {
  name_prefix = "${var.project_name}-${var.environment}-eks-nodes-"
  vpc_id      = var.vpc_id
  description = "Security group for EKS worker nodes"

  tags = {
    Name        = "${var.project_name}-${var.environment}-eks-nodes-sg"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "nodes_internal" {
  type                     = "ingress"
  from_port                = 0
  to_port                  = 65535
  protocol                 = "-1"
  source_security_group_id = aws_security_group.eks_nodes.id
  security_group_id        = aws_security_group.eks_nodes.id
  description              = "Allow nodes to communicate with each other"
}

resource "aws_security_group_rule" "nodes_cluster_inbound" {
  type                     = "ingress"
  from_port                = 1025
  to_port                  = 65535
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.eks_cluster.id
  security_group_id        = aws_security_group.eks_nodes.id
  description              = "Allow control plane to communicate with worker nodes"
}

resource "aws_security_group_rule" "nodes_egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.eks_nodes.id
  description       = "Allow all outbound traffic"
}

# --- RDS Security Group ---

resource "aws_security_group" "rds" {
  name_prefix = "${var.project_name}-${var.environment}-rds-"
  vpc_id      = var.vpc_id
  description = "Security group for RDS MySQL instance"

  tags = {
    Name        = "${var.project_name}-${var.environment}-rds-sg"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "rds_ingress_from_eks" {
  type                     = "ingress"
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.eks_nodes.id
  security_group_id        = aws_security_group.rds.id
  description              = "Allow MySQL access from EKS worker nodes"
}

resource "aws_security_group_rule" "rds_ingress_from_cidr" {
  for_each = toset(var.rds_allowed_cidr_blocks)

  type              = "ingress"
  from_port         = 3306
  to_port           = 3306
  protocol          = "tcp"
  cidr_blocks       = [each.value]
  security_group_id = aws_security_group.rds.id
  description       = "Allow MySQL access from VPC CIDR"
}

resource "aws_security_group_rule" "rds_egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.rds.id
  description       = "Allow all outbound traffic"
}

# --- Application Load Balancer Security Group ---

resource "aws_security_group" "alb" {
  name_prefix = "${var.project_name}-${var.environment}-alb-"
  vpc_id      = var.vpc_id
  description = "Security group for the Application Load Balancer"

  tags = {
    Name        = "${var.project_name}-${var.environment}-alb-sg"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "alb_http" {
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.alb.id
  description       = "Allow HTTP traffic"
}

resource "aws_security_group_rule" "alb_https" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.alb.id
  description       = "Allow HTTPS traffic"
}

resource "aws_security_group_rule" "alb_to_app" {
  type                     = "egress"
  from_port                = var.app_port
  to_port                  = var.app_port
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.eks_nodes.id
  security_group_id        = aws_security_group.alb.id
  description              = "Allow ALB to reach application on nodes"
}
```

**`modules/security-groups/variables.tf`**:

```hcl
variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "app_port" {
  description = "Application port"
  type        = number
  default     = 8080
}

variable "eks_cluster_sg_ingress_rules" {
  description = "Map of additional ingress rules for the EKS cluster security group"
  type = map(object({
    from_port   = number
    to_port     = number
    protocol    = string
    cidr_blocks = list(string)
    description = string
  }))
  default = {}
}

variable "rds_allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access RDS"
  type        = list(string)
  default     = []
}
```

**`modules/security-groups/outputs.tf`**:

```hcl
output "eks_cluster_sg_id" {
  description = "EKS cluster security group ID"
  value       = aws_security_group.eks_cluster.id
}

output "eks_nodes_sg_id" {
  description = "EKS worker nodes security group ID"
  value       = aws_security_group.eks_nodes.id
}

output "rds_sg_id" {
  description = "RDS security group ID"
  value       = aws_security_group.rds.id
}

output "alb_sg_id" {
  description = "ALB security group ID"
  value       = aws_security_group.alb.id
}
```

---

## Variable Definitions

### Environment-Level Variables (`environments/dev/terraform.tfvars`)

```hcl
# --- General ---
project_name = "bankapp"
environment  = "dev"
aws_region   = "us-west-1"

# --- VPC ---
vpc_cidr           = "10.0.0.0/16"
public_subnets     = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnets    = ["10.0.10.0/24", "10.0.20.0/24"]
availability_zones = ["us-west-1a", "us-west-1c"]

# --- EKS ---
eks_cluster_name    = "bankapp"
eks_cluster_version = "1.30"
node_groups = {
  bankapp = {
    instance_types = ["t2.medium"]
    disk_size      = 29
    desired_size   = 2
    min_size       = 2
    max_size       = 2
    ssh_key_name   = "eks-nodegroup-key"
  }
}

# --- RDS ---
db_name               = "BankDB"
db_username           = "bankapp_admin"
db_instance_class     = "db.t3.medium"
db_allocated_storage  = 20
db_engine_version     = "8.0"
rds_multi_az          = false

# --- Application ---
app_port = 8080

# --- Security Groups ---
eks_cluster_sg_ingress_rules = {
  jenkins = {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Jenkins CI server"
  }
  sonarqube = {
    from_port   = 9000
    to_port     = 9000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SonarQube code analysis"
  }
}
```

### Production Overrides (`environments/prod/terraform.tfvars`)

```hcl
project_name = "bankapp"
environment  = "prod"
aws_region   = "us-west-1"

# Larger node group for production
node_groups = {
  bankapp = {
    instance_types = ["t3.large"]
    disk_size      = 50
    desired_size   = 3
    min_size       = 2
    max_size       = 6
    ssh_key_name   = "bankapp-prod-key"
  }
}

# RDS production settings
db_instance_class     = "db.r6g.large"
db_allocated_storage  = 100
rds_multi_az          = true
```

---

## Output Definitions

### Root-Level Outputs (`environments/dev/outputs.tf`)

```hcl
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "eks_cluster_endpoint" {
  description = "EKS cluster API endpoint"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "rds_endpoint" {
  description = "RDS MySQL endpoint"
  value       = module.rds.db_instance_endpoint
}

output "rds_address" {
  description = "RDS MySQL hostname (for SPRING_DATASOURCE_URL)"
  value       = module.rds.db_instance_address
}

output "kubeconfig_command" {
  description = "Command to update local kubeconfig"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "spring_datasource_url" {
  description = "JDBC connection string for ConfigMap update"
  value       = "jdbc:mysql://${module.rds.db_instance_address}:${module.rds.db_instance_port}/${module.rds.db_name}?useSSL=true&requireSSL=true&serverTimezone=UTC"
}
```

---

## State Management

### Backend Configuration (`environments/dev/backend.tf`)

```hcl
terraform {
  required_version = ">= 1.5.0"

  backend "s3" {
    bucket         = "bankapp-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "us-west-1"
    dynamodb_table = "bankapp-terraform-locks"
    encrypt        = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}
```

### State Bootstrapping

Before first `terraform init`, create the S3 bucket and DynamoDB table for state locking:

```bash
# Create state bucket
aws s3api create-bucket \
  --bucket bankapp-terraform-state \
  --region us-west-1 \
  --create-bucket-configuration LocationConstraint=us-west-1

aws s3api put-bucket-versioning \
  --bucket bankapp-terraform-state \
  --versioning-configuration Status=Enabled

aws s3api put-bucket-encryption \
  --bucket bankapp-terraform-state \
  --server-side-encryption-configuration \
  '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"aws:kms"}}]}'

# Create lock table
aws dynamodb create-table \
  --table-name bankapp-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-west-1
```

---

## Migration Path

### Phase 1: Prepare (Week 1)

**Goal:** Set up Terraform tooling alongside existing infrastructure without disruption.

| Step | Action | Risk |
|---|---|---|
| 1.1 | Install Terraform >= 1.5.0 on CI/CD server | None |
| 1.2 | Create S3 state bucket and DynamoDB lock table | None |
| 1.3 | Scaffold module directory structure | None |
| 1.4 | Write and validate module code with `terraform validate` | None |
| 1.5 | Run `terraform plan` in dev to preview resource creation | None |

### Phase 2: Import Existing Resources (Week 2)

**Goal:** Bring existing manually-created AWS resources under Terraform management.

```bash
# Import existing VPC (get VPC ID from AWS Console/CLI)
terraform import module.vpc.aws_vpc.main vpc-0abc123def456

# Import existing EKS cluster
terraform import module.eks.aws_eks_cluster.main bankapp

# Import existing node group
terraform import 'module.eks.aws_eks_node_group.main["bankapp"]' bankapp:bankapp

# Import existing security groups
terraform import module.security_groups.aws_security_group.eks_cluster sg-0abc123

# Import IAM roles
terraform import module.iam.aws_iam_role.eks_cluster bankapp-dev-eks-cluster-role
terraform import module.iam.aws_iam_role.eks_node bankapp-dev-eks-node-role
```

After import, run `terraform plan` and resolve any drift (differences between actual state and Terraform config). Adjust variables and resource arguments until `plan` shows **no changes**.

### Phase 3: Provision RDS (Week 3)

**Goal:** Migrate from in-cluster MySQL to managed RDS.

| Step | Action | Details |
|---|---|---|
| 3.1 | Run `terraform apply` for the RDS module only | Creates RDS instance in private subnets |
| 3.2 | Export data from MySQL pod | `kubectl exec -n bankapp-namespace mysql-<pod> -- mysqldump -u root -p BankDB > bankdb_backup.sql` |
| 3.3 | Import data into RDS | `mysql -h <rds-endpoint> -u bankapp_admin -p BankDB < bankdb_backup.sql` |
| 3.4 | Update Kubernetes ConfigMap | Change `SPRING_DATASOURCE_URL` to point to RDS endpoint |
| 3.5 | Update Kubernetes Secret | Change credentials to match RDS master user |
| 3.6 | Restart application pods | `kubectl rollout restart deployment/bankapp-deploy -n bankapp-namespace` |
| 3.7 | Validate application connectivity | Confirm login, deposits, withdrawals, transfers work |
| 3.8 | Remove MySQL Kubernetes resources | Delete `mysql-deployment.yml`, `persistent-volume.yaml`, `persistent-volume-claim.yaml`, `mysql-service.yaml` |

### Phase 4: Full IaC Management (Week 4)

**Goal:** All infrastructure changes go through Terraform.

| Step | Action |
|---|---|
| 4.1 | Run `terraform plan` on full stack; confirm zero drift |
| 4.2 | Add Terraform plan/apply steps to Jenkins CI/CD pipeline |
| 4.3 | Remove `eksctl` commands from documentation |
| 4.4 | Enable branch protection on `terraform/` directory |
| 4.5 | Set up cost alerts and drift detection (e.g., `driftctl` or AWS Config) |

### Phase 5: Multi-Environment (Week 5+)

**Goal:** Use the same modules to provision staging and production environments.

```bash
# Create staging environment
cd terraform/environments/staging
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply

# Create production environment
cd terraform/environments/prod
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply
```

### Rollback Strategy

At any phase, rollback is possible:

- **Phases 1-2:** Delete Terraform state; existing resources are unaffected
- **Phase 3:** Re-deploy MySQL pod and restore from dump; revert ConfigMap/Secret
- **Phase 4+:** `terraform destroy` removes only Terraform-managed resources; use targeted destroy (`-target`) for selective rollback

---

## CI/CD Integration

### Jenkins Pipeline Stage for Terraform

Add these stages to `Jenkinsfile` after the existing security scans:

```groovy
stage("Terraform: Plan") {
    when {
        changeset "terraform/**"
    }
    steps {
        dir('terraform/environments/dev') {
            sh 'terraform init -no-color'
            sh 'terraform validate -no-color'
            sh 'terraform plan -no-color -out=tfplan'
        }
    }
}

stage("Terraform: Apply") {
    when {
        allOf {
            branch 'DevOps'
            changeset "terraform/**"
        }
    }
    input {
        message "Apply Terraform changes?"
        ok "Apply"
    }
    steps {
        dir('terraform/environments/dev') {
            sh 'terraform apply -no-color -auto-approve tfplan'
        }
    }
}
```

### ArgoCD Integration

After Terraform provisions the infrastructure, ArgoCD continues to manage the application layer (Kubernetes manifests in `kubernetes/`). The key integration point is the Kubernetes ConfigMap/Secret, which must reference the RDS endpoint output from Terraform:

```yaml
# kubernetes/configmap.yaml (updated for RDS)
data:
  SPRING_DATASOURCE_URL: jdbc:mysql://<rds-endpoint>:3306/BankDB?useSSL=true&requireSSL=true&serverTimezone=UTC
  SPRING_DATASOURCE_USERNAME: bankapp_admin
  MYSQL_DATABASE: BankDB
```

---

## Security Considerations

1. **Secrets Management:** Store the RDS password in AWS Secrets Manager or SSM Parameter Store. Reference it in Terraform using `data.aws_secretsmanager_secret_version`.
2. **State Encryption:** The S3 backend configuration enables KMS encryption for state files.
3. **State Access Control:** Restrict S3 bucket access to the CI/CD service account and infrastructure team via IAM policies.
4. **Least Privilege:** Each IAM role has only the minimum policies required (e.g., node role has no RDS access).
5. **Network Isolation:** RDS is placed in private subnets with security group rules allowing access only from EKS worker nodes (port 3306).
6. **TLS Enforcement:** RDS connections require SSL (`useSSL=true&requireSSL=true` in JDBC URL).
7. **Deletion Protection:** Production RDS has `deletion_protection = true` and creates a final snapshot before deletion.
8. **No Hardcoded Credentials:** Database passwords are marked `sensitive = true` and should never appear in `.tfvars` files committed to Git. Use environment variables (`TF_VAR_db_password`) or a secrets manager data source.
