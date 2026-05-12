################################################################################
# Terraform Configuration
################################################################################

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  backend "s3" {
    # Backend configuration is provided via backend config files per environment:
    #   terraform init -backend-config=environments/<env>/backend.tfvars
    #
    # Required keys: bucket, key, region, dynamodb_table, encrypt
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
      Repository  = "COG-GTM/Springboot-BankApp"
    }
  }
}

################################################################################
# Local Values
################################################################################

locals {
  cluster_name = "${var.project_name}-${var.environment}-eks"

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

################################################################################
# VPC
################################################################################

module "vpc" {
  source = "./modules/vpc"

  project_name          = var.project_name
  environment           = var.environment
  vpc_cidr              = var.vpc_cidr
  availability_zones    = var.availability_zones
  public_subnet_cidrs   = var.public_subnet_cidrs
  private_subnet_cidrs  = var.private_subnet_cidrs
  database_subnet_cidrs = var.database_subnet_cidrs
  cluster_name          = local.cluster_name
  enable_nat_gateway    = var.enable_nat_gateway
  single_nat_gateway    = var.single_nat_gateway
  enable_flow_logs      = var.enable_flow_logs
  tags                  = local.common_tags
}

################################################################################
# Security Groups
################################################################################

module "security_groups" {
  source = "./modules/security-groups"

  project_name              = var.project_name
  environment               = var.environment
  vpc_id                    = module.vpc.vpc_id
  cluster_name              = local.cluster_name
  rds_port                  = var.rds_port
  app_port                  = var.app_port
  alb_ingress_cidrs         = var.alb_ingress_cidrs
  bastion_security_group_id = var.bastion_security_group_id
  tags                      = local.common_tags
}

################################################################################
# IAM
################################################################################

module "iam" {
  source = "./modules/iam"

  project_name                  = var.project_name
  environment                   = var.environment
  cluster_name                  = local.cluster_name
  oidc_provider_arn             = module.eks.oidc_provider_arn
  oidc_provider_url             = module.eks.oidc_provider_url
  bankapp_namespace             = var.bankapp_namespace
  bankapp_service_account_name  = var.bankapp_service_account_name
  enable_secrets_manager_access = var.enable_secrets_manager_access
  secrets_manager_arns          = var.secrets_manager_arns
  enable_s3_access              = var.enable_s3_access
  s3_bucket_arns                = var.s3_bucket_arns
  enable_external_dns           = var.enable_external_dns
  tags                          = local.common_tags
}

################################################################################
# EKS
################################################################################

module "eks" {
  source = "./modules/eks"

  cluster_name                         = local.cluster_name
  cluster_version                      = var.cluster_version
  cluster_role_arn                     = module.iam.eks_cluster_role_arn
  node_role_arn                        = module.iam.eks_node_role_arn
  private_subnet_ids                   = module.vpc.private_subnet_ids
  public_subnet_ids                    = module.vpc.public_subnet_ids
  cluster_security_group_id            = module.security_groups.eks_cluster_security_group_id
  cluster_endpoint_private_access      = var.cluster_endpoint_private_access
  cluster_endpoint_public_access       = var.cluster_endpoint_public_access
  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs
  cluster_enabled_log_types            = var.cluster_enabled_log_types
  create_kms_key                       = var.create_kms_key
  node_groups                          = var.node_groups
  cluster_addons                       = var.cluster_addons
  tags                                 = local.common_tags
}

################################################################################
# RDS MySQL
################################################################################

module "rds" {
  source = "./modules/rds"

  project_name                 = var.project_name
  environment                  = var.environment
  engine_version               = var.rds_engine_version
  instance_class               = var.rds_instance_class
  allocated_storage            = var.rds_allocated_storage
  max_allocated_storage        = var.rds_max_allocated_storage
  storage_type                 = var.rds_storage_type
  database_name                = var.database_name
  master_username              = var.database_master_username
  master_password              = var.database_master_password
  port                         = var.rds_port
  multi_az                     = var.rds_multi_az
  db_subnet_group_name         = module.vpc.db_subnet_group_name
  security_group_ids           = [module.security_groups.rds_security_group_id]
  kms_key_arn                  = module.eks.kms_key_arn
  backup_retention_period      = var.rds_backup_retention_period
  deletion_protection          = var.rds_deletion_protection
  skip_final_snapshot          = var.rds_skip_final_snapshot
  performance_insights_enabled = var.rds_performance_insights_enabled
  create_cloudwatch_alarms     = var.create_cloudwatch_alarms
  alarm_sns_topic_arns         = var.alarm_sns_topic_arns
  tags                         = local.common_tags
}
