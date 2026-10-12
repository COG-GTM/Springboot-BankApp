terraform {
  required_version = ">= 1.6"
  required_providers {
    aws    = { source = "hashicorp/aws", version = "~> 5.60" }
    time   = { source = "hashicorp/time", version = "~> 0.12" }
    github = { source = "integrations/github", version = "~> 6.0" }
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

  # CCC.Core.CN13: no non-expiring tokens. v4 CI exchanges OIDC once per job, so the default
  # covers the longest GitHub-hosted job (6h) and nothing more.
  access_token_defaults = {
    expires_in_seconds = 21600
    refreshable        = false
  }
}

variable "vpc_id" { type = string }
