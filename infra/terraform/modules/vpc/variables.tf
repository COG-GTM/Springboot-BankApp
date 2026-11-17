variable "name" {
  description = "Name prefix applied to all VPC resources."
  type        = string
}

variable "cidr_block" {
  description = "Primary IPv4 CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "Availability Zones to spread subnets across. Provide at least two for HA."
  type        = list(string)

  validation {
    condition     = length(var.azs) >= 2
    error_message = "At least two Availability Zones are required for a highly available deployment."
  }
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (one per AZ). Host the NAT gateways and public load balancers."
  type        = list(string)
  default     = ["10.0.0.0/20", "10.0.16.0/20", "10.0.32.0/20"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (one per AZ). Host the EKS worker nodes."
  type        = list(string)
  default     = ["10.0.64.0/20", "10.0.80.0/20", "10.0.96.0/20"]
}

variable "database_subnet_cidrs" {
  description = "CIDR blocks for isolated database subnets (one per AZ). Host RDS with no route to the internet."
  type        = list(string)
  default     = ["10.0.128.0/24", "10.0.129.0/24", "10.0.130.0/24"]
}

variable "cluster_name" {
  description = "EKS cluster name, used to apply the subnet discovery tags the AWS Load Balancer Controller and EKS require."
  type        = string
}

variable "single_nat_gateway" {
  description = "When true, provision a single NAT gateway shared by all private subnets (cheaper, lower availability). When false, one NAT gateway per AZ."
  type        = bool
  default     = false
}

variable "flow_log_retention_days" {
  description = "Retention period (days) for the VPC Flow Logs CloudWatch log group."
  type        = number
  default     = 365
}

variable "kms_key_arn" {
  description = "KMS key ARN used to encrypt the VPC Flow Logs CloudWatch log group. Required so log data is encrypted at rest."
  type        = string
}

variable "tags" {
  description = "Tags applied to every resource created by this module."
  type        = map(string)
  default     = {}
}
