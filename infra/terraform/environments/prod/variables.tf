variable "region" {
  description = "AWS region. Must be on the governance-approved list (see Sentinel approved-regions policy)."
  type        = string
  default     = "us-west-1"
}

variable "name_prefix" {
  description = "Prefix for all named resources."
  type        = string
  default     = "bankapp-prod"
}

variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
  default     = "bankapp"
}

variable "kubernetes_version" {
  description = "EKS control-plane version."
  type        = string
  default     = "1.31"
}

variable "vpc_cidr" {
  description = "VPC CIDR block."
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "Availability Zones to deploy across."
  type        = list(string)
  default     = ["us-west-1a", "us-west-1c"]
}

variable "endpoint_public_access" {
  description = "Expose the EKS API server publicly. Default false (private-only)."
  type        = bool
  default     = false
}

variable "public_access_cidrs" {
  description = "CIDRs allowed to reach the public EKS endpoint when endpoint_public_access is true (e.g. corporate egress ranges)."
  type        = list(string)
  default     = []
}

variable "node_instance_types" {
  description = "Approved worker-node instance types."
  type        = list(string)
  default     = ["m6i.large"]
}

variable "db_instance_class" {
  description = "Approved RDS instance class."
  type        = string
  default     = "db.t3.medium"
}

variable "cost_center" {
  description = "Cost center tag value (required tag)."
  type        = string
  default     = "public-cloud-platform"
}

variable "data_classification" {
  description = "Data classification tag value (required tag)."
  type        = string
  default     = "confidential"
}

variable "owner" {
  description = "Owning team tag value (required tag)."
  type        = string
  default     = "platform-engineering"
}
