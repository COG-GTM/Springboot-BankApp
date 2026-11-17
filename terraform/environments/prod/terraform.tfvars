################################################################################
# Production Environment Configuration
################################################################################

# General
aws_region   = "us-east-1"
project_name = "bankapp"
environment  = "prod"

# VPC
vpc_cidr              = "10.2.0.0/16"
availability_zones    = ["us-east-1a", "us-east-1b", "us-east-1c"]
public_subnet_cidrs   = ["10.2.1.0/24", "10.2.2.0/24", "10.2.3.0/24"]
private_subnet_cidrs  = ["10.2.11.0/24", "10.2.12.0/24", "10.2.13.0/24"]
database_subnet_cidrs = ["10.2.21.0/24", "10.2.22.0/24", "10.2.23.0/24"]
enable_nat_gateway    = true
single_nat_gateway    = false # HA: one NAT per AZ for production
enable_flow_logs      = true

# EKS
cluster_version                 = "1.29"
cluster_endpoint_private_access = true
cluster_endpoint_public_access  = false # Private-only in production

node_groups = {
  general = {
    instance_types = ["m5.xlarge"]
    desired_size   = 3
    min_size       = 3
    max_size       = 10
    capacity_type  = "ON_DEMAND"
    disk_size      = 100
    labels = {
      role        = "general"
      environment = "prod"
    }
  }
  spot = {
    instance_types = ["m5.xlarge", "m5a.xlarge", "m5n.xlarge"]
    desired_size   = 2
    min_size       = 0
    max_size       = 8
    capacity_type  = "SPOT"
    labels = {
      role        = "spot-workers"
      environment = "prod"
    }
    taints = [{
      key    = "spot"
      value  = "true"
      effect = "NO_SCHEDULE"
    }]
  }
}

# RDS
rds_engine_version               = "8.0"
rds_instance_class               = "db.r5.xlarge"
rds_allocated_storage            = 100
rds_max_allocated_storage        = 500
rds_storage_type                 = "gp3"
rds_multi_az                     = true
rds_backup_retention_period      = 30
rds_deletion_protection          = true
rds_skip_final_snapshot          = false
rds_performance_insights_enabled = true
database_name                    = "BankDB"

# Security - restrict to known IPs in production
alb_ingress_cidrs = ["0.0.0.0/0"]

# IAM
enable_secrets_manager_access = true
enable_external_dns           = true

# Monitoring
create_cloudwatch_alarms = true
