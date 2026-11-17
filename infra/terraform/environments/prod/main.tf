data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

locals {
  common_tags = {
    Application        = "springboot-bankapp"
    Environment        = "prod"
    Owner              = var.owner
    CostCenter         = var.cost_center
    DataClassification = var.data_classification
    ManagedBy          = "terraform"
  }

  account_id = data.aws_caller_identity.current.account_id
  partition  = data.aws_partition.current.partition
}

# ---------------------------------------------------------------------------
# KMS keys (rotation enabled) - one per data domain for blast-radius isolation.
# ---------------------------------------------------------------------------
resource "aws_kms_key" "logs" {
  description             = "${var.name_prefix} CloudWatch Logs encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  policy                  = data.aws_iam_policy_document.logs_key.json

  tags = local.common_tags
}

resource "aws_kms_alias" "logs" {
  name          = "alias/${var.name_prefix}-logs"
  target_key_id = aws_kms_key.logs.key_id
}

# The "AccountAdmin" statement (kms:* on the key, granted to the account root)
# is the AWS-recommended baseline for every KMS key policy - without it the key
# can become unmanageable. The static analyzers flag it as a wildcard policy;
# the skips below record that this is intentional and scoped to a single key.
data "aws_iam_policy_document" "logs_key" {
  # checkov:skip=CKV_AWS_109: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_111: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_356: In a KMS key policy, Resource "*" refers to the key itself.
  statement {
    sid       = "AccountAdmin"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]

    principals {
      type        = "AWS"
      identifiers = ["arn:${local.partition}:iam::${local.account_id}:root"]
    }
  }

  statement {
    sid       = "AllowCloudWatchLogs"
    effect    = "Allow"
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:ReEncrypt*", "kms:GenerateDataKey*", "kms:Describe*"]
    resources = ["*"]

    principals {
      type        = "Service"
      identifiers = ["logs.${var.region}.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "eks_key" {
  # checkov:skip=CKV_AWS_109: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_111: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_356: In a KMS key policy, Resource "*" refers to the key itself.
  statement {
    sid       = "AccountAdmin"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]

    principals {
      type        = "AWS"
      identifiers = ["arn:${local.partition}:iam::${local.account_id}:root"]
    }
  }
}

data "aws_iam_policy_document" "rds_key" {
  # checkov:skip=CKV_AWS_109: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_111: Root key administration is required on a KMS key policy.
  # checkov:skip=CKV_AWS_356: In a KMS key policy, Resource "*" refers to the key itself.
  statement {
    sid       = "AccountAdmin"
    effect    = "Allow"
    actions   = ["kms:*"]
    resources = ["*"]

    principals {
      type        = "AWS"
      identifiers = ["arn:${local.partition}:iam::${local.account_id}:root"]
    }
  }
}

resource "aws_kms_key" "eks" {
  description             = "${var.name_prefix} EKS secrets + node EBS encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  policy                  = data.aws_iam_policy_document.eks_key.json

  tags = local.common_tags
}

resource "aws_kms_alias" "eks" {
  name          = "alias/${var.name_prefix}-eks"
  target_key_id = aws_kms_key.eks.key_id
}

resource "aws_kms_key" "rds" {
  description             = "${var.name_prefix} RDS storage + master-secret encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
  policy                  = data.aws_iam_policy_document.rds_key.json

  tags = local.common_tags
}

resource "aws_kms_alias" "rds" {
  name          = "alias/${var.name_prefix}-rds"
  target_key_id = aws_kms_key.rds.key_id
}

# ---------------------------------------------------------------------------
# Networking
# ---------------------------------------------------------------------------
module "vpc" {
  source = "../../modules/vpc"

  name         = var.name_prefix
  cidr_block   = var.vpc_cidr
  azs          = var.azs
  cluster_name = var.cluster_name
  kms_key_arn  = aws_kms_key.logs.arn

  public_subnet_cidrs   = ["10.0.0.0/20", "10.0.16.0/20"]
  private_subnet_cidrs  = ["10.0.64.0/20", "10.0.80.0/20"]
  database_subnet_cidrs = ["10.0.128.0/24", "10.0.129.0/24"]

  single_nat_gateway = false

  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# EKS
# ---------------------------------------------------------------------------
module "eks" {
  source = "../../modules/eks"

  cluster_name       = var.cluster_name
  kubernetes_version = var.kubernetes_version
  subnet_ids         = module.vpc.private_subnet_ids
  kms_key_arn        = aws_kms_key.eks.arn

  endpoint_public_access = var.endpoint_public_access
  public_access_cidrs    = var.public_access_cidrs

  node_instance_types = var.node_instance_types
  node_desired_size   = 2
  node_min_size       = 2
  node_max_size       = 4
  node_disk_size      = 30

  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# RDS MySQL (replaces the in-cluster mysql StatefulSet/Deployment)
# ---------------------------------------------------------------------------
module "rds" {
  source = "../../modules/rds"

  identifier                 = "${var.name_prefix}-mysql"
  instance_class             = var.db_instance_class
  db_name                    = "BankDB"
  subnet_ids                 = module.vpc.database_subnet_ids
  vpc_id                     = module.vpc.vpc_id
  allowed_security_group_ids = [module.eks.cluster_security_group_id]
  kms_key_arn                = aws_kms_key.rds.arn

  multi_az                = true
  backup_retention_period = 14
  deletion_protection     = true

  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# IRSA role: bankapp service account -> read RDS master secret only
# ---------------------------------------------------------------------------
module "bankapp_irsa" {
  source = "../../modules/iam"

  role_name         = "${var.name_prefix}-bankapp-irsa"
  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url
  namespace         = "bankapp-namespace"
  service_account   = "bankapp"

  secret_arns  = [module.rds.master_user_secret_arn]
  kms_key_arns = [aws_kms_key.rds.arn]

  tags = local.common_tags
}
