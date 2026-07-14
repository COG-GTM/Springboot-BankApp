variable "cluster_name" {
  description = "Name of the EKS cluster."
  type        = string
}

variable "kubernetes_version" {
  description = "Kubernetes control-plane version."
  type        = string
  default     = "1.31"
}

variable "subnet_ids" {
  description = "Private subnet IDs the control plane ENIs and worker nodes are placed in."
  type        = list(string)
}

variable "kms_key_arn" {
  description = "KMS key ARN used for EKS secrets envelope encryption and node EBS volume encryption."
  type        = string
}

variable "endpoint_public_access" {
  description = "Whether the EKS API server endpoint is reachable from outside the VPC. Keep false for production; if enabled, restrict public_access_cidrs."
  type        = bool
  default     = false
}

variable "public_access_cidrs" {
  description = "CIDR blocks allowed to reach the public API endpoint. Only used when endpoint_public_access is true. Never leave as 0.0.0.0/0 in production."
  type        = list(string)
  default     = []
}

variable "enabled_cluster_log_types" {
  description = "EKS control-plane log types to ship to CloudWatch."
  type        = list(string)
  default     = ["api", "audit", "authenticator", "controllerManager", "scheduler"]
}

variable "log_retention_days" {
  description = "Retention period (days) for the EKS control-plane CloudWatch log group."
  type        = number
  default     = 365
}

variable "node_instance_types" {
  description = "Instance types for the managed node group. Restricted to approved, current-generation classes by governance policy."
  type        = list(string)
  default     = ["m6i.large"]
}

variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
  default     = 2
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 4
}

variable "node_disk_size" {
  description = "EBS root volume size (GiB) for each worker node."
  type        = number
  default     = 30
}

variable "tags" {
  description = "Tags applied to every resource created by this module."
  type        = map(string)
  default     = {}
}
