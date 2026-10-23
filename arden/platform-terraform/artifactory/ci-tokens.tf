# Per-repo CI tokens for ci-shared-workflows v3 callers (every estate repo still pins @v3 and reads ARTIFACTORY_TOKEN).
# CCC.Core.CN13: tokens expire after var.ci_token_ttl_seconds. Terraform re-issues each token local.apply_interval_days
# before it expires and writes the new value to the repo's ARTIFACTORY_TOKEN secret so v3 builds keep working.
# Rotation runs on apply, so the scheduled Atlantis apply must run at least every local.apply_interval_days days.
# Tokens issued before this change never expire and are revoked separately (CVE-2026-82329 token sweep).
locals {
  apply_interval_days = 5
}

resource "time_rotating" "ci_token" {
  rotation_days = floor(var.ci_token_ttl_seconds / 86400) - local.apply_interval_days
}

resource "artifactory_scoped_token" "ci" {
  for_each    = toset(var.ci_repos)
  username    = "svc-${each.key}"
  scopes      = ["applied-permissions/user"]
  expires_in  = var.ci_token_ttl_seconds
  refreshable = false
  description = "CI token for ${each.key}"

  lifecycle {
    create_before_destroy = true
    replace_triggered_by  = [time_rotating.ci_token]
  }
}

resource "github_actions_secret" "artifactory_token" {
  for_each        = artifactory_scoped_token.ci
  repository      = each.key
  secret_name     = "ARTIFACTORY_TOKEN"
  plaintext_value = each.value.access_token
}

variable "ci_token_ttl_seconds" {
  type        = number
  default     = 2592000 # 30 days
  description = "Lifetime of per-repo CI tokens (CCC.Core.CN13)."
  validation {
    condition     = var.ci_token_ttl_seconds >= 864000 && var.ci_token_ttl_seconds <= 2592000
    error_message = "CI tokens must expire within 30 days, and last at least 10 days so rotation leaves the apply interval."
  }
}

variable "ci_repos" {
  type = list(string)
  default = [
    "allocation-service", "confirmation-service", "settlement-instruction-service", "custody-gateway",
    "fx-funding-service", "stock-loan-recall-service", "corporate-actions-service",
    "risk-lib", "recon-job", "eod-pricing-batch", "client-portal",
  ]
}
