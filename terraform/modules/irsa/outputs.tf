output "bankapp_workload_role_arn" {
  description = "ARN of the banking application workload IAM role (IRSA)"
  value       = aws_iam_role.bankapp_workload.arn
}

output "alb_controller_role_arn" {
  description = "ARN of the AWS Load Balancer Controller IAM role"
  value       = aws_iam_role.alb_controller.arn
}

output "cluster_autoscaler_role_arn" {
  description = "ARN of the Cluster Autoscaler IAM role"
  value       = aws_iam_role.cluster_autoscaler.arn
}

output "external_dns_role_arn" {
  description = "ARN of the External DNS IAM role"
  value       = var.enable_external_dns ? aws_iam_role.external_dns[0].arn : ""
}
