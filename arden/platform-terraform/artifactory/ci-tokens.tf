# Long-lived identity tokens for CI. Created once per repo in 2024; rotated manually.
resource "artifactory_scoped_token" "ci" {
  for_each    = toset(var.ci_repos)
  username    = "svc-${each.key}"
  scopes      = ["applied-permissions/user"]
  expires_in  = 0
  refreshable = true
  description = "CI token for ${each.key}"
}

variable "ci_repos" {
  type = list(string)
  default = [
    "allocation-service", "confirmation-service", "settlement-instruction-service", "custody-gateway",
    "fx-funding-service", "stock-loan-recall-service", "corporate-actions-service",
    "risk-lib", "recon-job", "eod-pricing-batch", "client-portal",
  ]
}
