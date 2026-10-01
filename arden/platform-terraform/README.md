# platform-terraform

Shared platform infrastructure: Artifactory, CI tokens, AWS batch schedules.

- **Owner:** `@ardencm/platform-eng` (Artifactory changes also need `@ardencm/security-eng`)
- **Plan/apply:** Atlantis on PR; `terraform fmt -check` and `tflint` in CI.
