################################################################################
# Dev Environment Configuration
################################################################################

# General
aws_region   = "us-east-1"
project_name = "bankapp"
environment  = "dev"

# VPC
vpc_cidr              = "10.0.0.0/16"
availability_zones    = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs   = ["10.0.1.0/24", "10.0.2.0/24"]
private_subnet_cidrs  = ["10.0.11.0/24", "10.0.12.0/24"]
database_subnet_cidrs = ["10.0.21.0/24", "10.0.22.0/24"]
enable_nat_gateway    = true
single_nat_gateway    = true # Cost saving: single NAT for dev
enable_flow_logs      = true

# EKS
cluster_version                 = "1.29"
cluster_endpoint_private_access = true
cluster_endpoint_public_access  = true

node_groups = {
  general = {
    instance_types = ["t3.medium"]
    desired_size   = 2
    min_size       = 1
    max_size       = 4
    capacity_type  = "ON_DEMAND"
    labels = {
      role        = "general"
      environment = "dev"
    }
  }
}

# RDS
rds_engine_version               = "8.0"
rds_instance_class               = "db.t3.medium"
rds_allocated_storage            = 20
rds_max_allocated_storage        = 50
rds_multi_az                     = false # Single-AZ for dev
rds_backup_retention_period      = 3
rds_deletion_protection          = false
rds_skip_final_snapshot          = true
rds_performance_insights_enabled = false
database_name                    = "BankDB"

# Security
alb_ingress_cidrs = ["0.0.0.0/0"]

# Monitoring
create_cloudwatch_alarms = false
