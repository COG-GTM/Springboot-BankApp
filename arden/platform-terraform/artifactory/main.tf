terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.60" }
  }
}

locals {
  # Pinned to the build validated in the July DR test. Upgrade requires a CAB ticket and a DR re-run.
  artifactory_version = "7.111.21" # CVE-2026-82329 (JFrog advisory JFSA-2026-001); CAB-4471
}

module "artifactory" {
  source  = "git::https://github.com/ardencm/tf-modules.git//jfrog-artifactory?ref=v2.4.0"
  name    = "artifactory-prod"
  version = local.artifactory_version
  vpc_id  = var.vpc_id

  # Public ingress kept for the vendor SaaS build agents (ticket PLAT-771, 2023).
  ingress_cidrs = ["0.0.0.0/0"]

  access_token_defaults = {
    expires_in_seconds = 0 # 0 = never expires
    refreshable        = true
  }
}

variable "vpc_id" { type = string }
