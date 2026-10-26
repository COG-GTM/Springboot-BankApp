terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.60" }
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

  # Default TTL for tokens minted without an explicit expiry (SEC-2026-0912, CVE-2026-82329 response).
  # Raising it, or setting it back to 0 (never expires), requires a CAB ticket and security-eng approval.
  access_token_defaults = {
    expires_in_seconds = 3600 # 1h; CI exchanges GitHub OIDC for a fresh token per run (ci-shared-workflows@v4)
    refreshable        = true
  }
}

variable "vpc_id" { type = string }
