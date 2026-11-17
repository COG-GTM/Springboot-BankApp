variable "identifier" {
  description = "RDS instance identifier."
  type        = string
}

variable "engine_version" {
  description = "MySQL engine version. Matches the 8.x line the application (mysql-connector 8.0.33, MySQL8Dialect) is built against."
  type        = string
  default     = "8.0.39"
}

variable "instance_class" {
  description = "RDS instance class. Restricted to approved classes by governance policy."
  type        = string
  default     = "db.t3.medium"
}

variable "allocated_storage" {
  description = "Initial allocated storage in GiB."
  type        = number
  default     = 20
}

variable "max_allocated_storage" {
  description = "Upper bound for storage autoscaling in GiB."
  type        = number
  default     = 100
}

variable "db_name" {
  description = "Initial database name to create. The application expects BankDB (docker-compose/k8s) / bankappdb (local)."
  type        = string
  default     = "BankDB"
}

variable "master_username" {
  description = "Master username. The application connects as this user."
  type        = string
  default     = "bankapp_admin"
}

variable "subnet_ids" {
  description = "Isolated database subnet IDs for the DB subnet group."
  type        = list(string)
}

variable "vpc_id" {
  description = "VPC ID the database security group is created in."
  type        = string
}

variable "allowed_security_group_ids" {
  description = "Security group IDs allowed to reach MySQL on 3306 (e.g. the EKS node/cluster security group). No CIDR-based ingress is permitted."
  type        = list(string)
}

variable "kms_key_arn" {
  description = "KMS key ARN used to encrypt storage, performance insights, and the managed master-user secret."
  type        = string
}

variable "multi_az" {
  description = "Deploy a standby replica in a second AZ for high availability."
  type        = bool
  default     = true
}

variable "backup_retention_period" {
  description = "Number of days to retain automated backups."
  type        = number
  default     = 14
}

variable "deletion_protection" {
  description = "Prevent accidental deletion of the database."
  type        = bool
  default     = true
}

variable "monitoring_interval" {
  description = "Enhanced monitoring granularity in seconds (0 disables)."
  type        = number
  default     = 60
}

variable "tags" {
  description = "Tags applied to every resource created by this module."
  type        = map(string)
  default     = {}
}
