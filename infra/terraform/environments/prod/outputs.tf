output "vpc_id" {
  description = "VPC ID."
  value       = module.vpc.vpc_id
}

output "private_subnet_ids" {
  description = "Private subnet IDs hosting the EKS nodes."
  value       = module.vpc.private_subnet_ids
}

output "cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS API server endpoint."
  value       = module.eks.cluster_endpoint
}

output "oidc_provider_arn" {
  description = "IAM OIDC provider ARN for IRSA."
  value       = module.eks.oidc_provider_arn
}

output "rds_endpoint" {
  description = "RDS MySQL endpoint the application connects to."
  value       = module.rds.db_endpoint
}

output "rds_master_secret_arn" {
  description = "Secrets Manager ARN holding the RDS master credentials."
  value       = module.rds.master_user_secret_arn
}

output "bankapp_irsa_role_arn" {
  description = "IRSA role ARN to annotate on the bankapp Kubernetes service account."
  value       = module.bankapp_irsa.role_arn
}
