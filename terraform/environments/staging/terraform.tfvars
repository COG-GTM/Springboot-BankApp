################################################################################
# Staging Environment Configuration
################################################################################

# General
aws_region   = "us-east-1"
project_name = "bankapp"
environment  = "staging"

# VPC
vpc_cidr              = "10.1.0.0/16"
availability_zones    = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs   = ["10.1.1.0/24", "10.1.2.0/24", "10.1.3.0/24"]
private_subnet_cidrs  = ["10.1.11.0/24", "10.1.12.0/24", "10.1.13.0/24"]
database_subnet_cidrs = ["10.1.21.0/24", "10.1.22.0/24", "10.1.23.0/24"]
enable_nat_gateway    = true
single_nat_gateway    = true # Single NAT for staging cost optimization
enable_flow_logs      = true

# EKS
cluster_version                 = "1.29"
cluster_endpoint_private_access = true
cluster_endpoint_public_access  = true

node_groups = {
  general = {
    instance_types = ["t3.large"]
    desired_size   = 2
    min_size       = 2
    max_size       = 6
    capacity_type  = "ON_DEMAND"
    labels = {
      role        = "general"
      environment = "staging"
    }
  }
}

# RDS
rds_engine_version               = "8.0"
rds_instance_class               = "db.t3.large"
rds_allocated_storage            = 50
rds_max_allocated_storage        = 100
rds_multi_az                     = true
rds_backup_retention_period      = 7
rds_deletion_protection          = true
rds_skip_final_snapshot          = false
rds_performance_insights_enabled = true
database_name                    = "BankDB"

# Security
alb_ingress_cidrs = ["0.0.0.0/0"]

# Monitoring
create_cloudwatch_alarms = true
