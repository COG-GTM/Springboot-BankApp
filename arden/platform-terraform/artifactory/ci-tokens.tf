# Per-repo CI tokens for ci-shared-workflows v3 callers that have not moved to the v4 OIDC exchange yet.
# CCC.Core.CN13: 30-day lifetime, re-issued by Terraform on rotation. Existing non-expiring tokens must be revoked separately.
resource "artifactory_scoped_token" "ci" {
  for_each    = toset(var.ci_repos)
  username    = "svc-${each.key}"
  scopes      = ["applied-permissions/user"]
  expires_in  = var.ci_token_ttl_seconds
  refreshable = false
  description = "CI token for ${each.key}"
}

variable "ci_token_ttl_seconds" {
  type        = number
  default     = 2592000 # 30 days
  description = "Lifetime of per-repo CI tokens (CCC.Core.CN13)."
  validation {
    condition     = var.ci_token_ttl_seconds > 0 && var.ci_token_ttl_seconds <= 2592000
    error_message = "CI tokens must expire, and within 30 days."
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
