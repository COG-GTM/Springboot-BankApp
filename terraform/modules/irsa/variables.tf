variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider"
  type        = string
}

variable "oidc_provider_url" {
  description = "URL of the EKS OIDC provider (without https://)"
  type        = string
}

variable "bankapp_namespace" {
  description = "Kubernetes namespace for the banking application"
  type        = string
  default     = "bankapp-namespace"
}

variable "bankapp_service_account_name" {
  description = "Kubernetes service account name for the banking application"
  type        = string
  default     = "bankapp-sa"
}

variable "enable_secrets_manager_access" {
  description = "Enable Secrets Manager access for the bankapp workload"
  type        = bool
  default     = true
}

variable "secrets_manager_arns" {
  description = "List of Secrets Manager ARNs the bankapp workload can access"
  type        = list(string)
  default     = ["arn:aws:secretsmanager:*:*:secret:bankapp/*"]
}

variable "enable_s3_access" {
  description = "Enable S3 access for the bankapp workload"
  type        = bool
  default     = false
}

variable "s3_bucket_arns" {
  description = "List of S3 bucket ARNs the bankapp workload can access"
  type        = list(string)
  default     = []
}

variable "enable_external_dns" {
  description = "Enable External DNS IAM role"
  type        = bool
  default     = false
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
