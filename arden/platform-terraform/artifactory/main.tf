terraform {
  required_version = ">= 1.6"
  required_providers {
    aws         = { source = "hashicorp/aws", version = "~> 5.60" }
    artifactory = { source = "jfrog/artifactory", version = "~> 12.0" }
    time        = { source = "hashicorp/time", version = "~> 0.12" }
    github      = { source = "integrations/github", version = "~> 6.0" }
  }
}

locals {
  # Pinned to the build validated in the July DR test. Upgrade requires a CAB ticket and a DR re-run.
  artifactory_version = "7.111.19"
}

module "artifactory" {
  source  = "git::https://github.com/ardencm/tf-modules.git//jfrog-artifactory?ref=v2.4.0"
  name    = "artifactory-prod"
  version = local.artifactory_version
  vpc_id  = var.vpc_id

  # Public ingress kept for the vendor SaaS build agents (ticket PLAT-771, 2023).
  ingress_cidrs = ["0.0.0.0/0"]

  # CCC.Core.CN13: no non-expiring tokens by default. v4 CI exchanges an OIDC token once per job.
  access_token_defaults = {
    expires_in_seconds = var.access_token_default_ttl_seconds
    refreshable        = false
  }
}

provider "github" {
  owner = "ardencm"
}

variable "vpc_id" { type = string }

variable "access_token_default_ttl_seconds" {
  type        = number
  default     = 21600 # 6 hours, the longest GitHub-hosted job
  description = "Default lifetime of Artifactory access tokens (CCC.Core.CN13)."
  validation {
    condition     = var.access_token_default_ttl_seconds > 0 && var.access_token_default_ttl_seconds <= 86400
    error_message = "Artifactory access tokens must expire, and within 24 hours by default."
  }
}
