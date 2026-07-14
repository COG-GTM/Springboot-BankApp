variable "role_name" {
  description = "Name of the IRSA IAM role."
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the cluster IAM OIDC provider (from the EKS module)."
  type        = string
}

variable "oidc_provider_url" {
  description = "URL of the cluster IAM OIDC provider without the https:// scheme (from the EKS module)."
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace of the service account that may assume this role."
  type        = string
}

variable "service_account" {
  description = "Kubernetes service account name that may assume this role."
  type        = string
}

variable "secret_arns" {
  description = "Explicit Secrets Manager secret ARNs the workload may read. Wildcards are not permitted (enforced by Sentinel)."
  type        = list(string)
  default     = []
}

variable "kms_key_arns" {
  description = "Explicit KMS key ARNs the workload may use to decrypt the above secrets."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "Tags applied to every resource created by this module."
  type        = map(string)
  default     = {}
}
