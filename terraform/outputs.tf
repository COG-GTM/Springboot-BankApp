################################################################################
# VPC Outputs
################################################################################

output "vpc_id" {
  description = "ID of the VPC"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "CIDR block of the VPC"
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

output "database_subnet_ids" {
  description = "IDs of the database subnets"
  value       = module.vpc.database_subnet_ids
}

################################################################################
# EKS Outputs
################################################################################

output "cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "Endpoint for the EKS cluster API server"
  value       = module.eks.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data for the cluster"
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "cluster_version" {
  description = "Kubernetes version of the EKS cluster"
  value       = module.eks.cluster_version
}

output "oidc_provider_arn" {
  description = "ARN of the OIDC provider for IRSA"
  value       = module.eks.oidc_provider_arn
}

################################################################################
# RDS Outputs
################################################################################

output "rds_endpoint" {
  description = "Connection endpoint of the RDS instance"
  value       = module.rds.db_instance_endpoint
}

output "rds_address" {
  description = "Hostname of the RDS instance"
  value       = module.rds.db_instance_address
}

output "rds_port" {
  description = "Port of the RDS instance"
  value       = module.rds.db_instance_port
}

output "rds_database_name" {
  description = "Name of the database"
  value       = module.rds.db_name
}

################################################################################
# IAM Outputs
################################################################################

output "bankapp_workload_role_arn" {
  description = "ARN of the banking application workload IAM role (IRSA)"
  value       = module.iam.bankapp_workload_role_arn
}

output "alb_controller_role_arn" {
  description = "ARN of the AWS Load Balancer Controller IAM role"
  value       = module.iam.alb_controller_role_arn
}

output "cluster_autoscaler_role_arn" {
  description = "ARN of the Cluster Autoscaler IAM role"
  value       = module.iam.cluster_autoscaler_role_arn
}

################################################################################
# Security Group Outputs
################################################################################

output "eks_cluster_security_group_id" {
  description = "ID of the EKS cluster security group"
  value       = module.security_groups.eks_cluster_security_group_id
}

output "eks_nodes_security_group_id" {
  description = "ID of the EKS worker nodes security group"
  value       = module.security_groups.eks_nodes_security_group_id
}

output "rds_security_group_id" {
  description = "ID of the RDS security group"
  value       = module.security_groups.rds_security_group_id
}

output "alb_security_group_id" {
  description = "ID of the ALB security group"
  value       = module.security_groups.alb_security_group_id
}

################################################################################
# Connection Information
################################################################################

output "configure_kubectl" {
  description = "Command to configure kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "spring_datasource_url" {
  description = "JDBC connection URL for Spring Boot application.properties"
  value       = "jdbc:mysql://${module.rds.db_instance_address}:${module.rds.db_instance_port}/${module.rds.db_name}?useSSL=true&requireSSL=true&serverTimezone=UTC"
}
