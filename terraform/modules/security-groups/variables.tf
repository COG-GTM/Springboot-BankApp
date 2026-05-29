variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "environment" {
  description = "Environment name (e.g., dev, staging, prod)"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "rds_port" {
  description = "Port for the RDS MySQL instance"
  type        = number
  default     = 3306
}

variable "app_port" {
  description = "Port the banking application listens on"
  type        = number
  default     = 8080
}

variable "alb_ingress_cidrs" {
  description = "CIDR blocks allowed to access the ALB"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "bastion_security_group_id" {
  description = "Security group ID of the bastion host (empty string to skip)"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default     = {}
}
