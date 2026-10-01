# Long-lived identity tokens for CI. Created once per repo in 2024; rotated manually.
resource "artifactory_scoped_token" "ci" {
  for_each    = toset(var.legacy_ci_repos) # shrinks to [] as repos move to @v4
  username    = "svc-${each.key}"
  scopes      = ["applied-permissions/user"]
  expires_in  = 0 # TODO remove: replaced by OIDC exchange in ci-shared-workflows v4
  refreshable = true
  description = "CI token for ${each.key}"
}

variable "legacy_ci_repos" {
  type = list(string)
  default = [
    "allocation-service", "confirmation-service", "settlement-instruction-service", "custody-gateway",
    "fx-funding-service", "stock-loan-recall-service", "corporate-actions-service",
    "risk-lib", "recon-job", "eod-pricing-batch", "client-portal",
  ]
}
