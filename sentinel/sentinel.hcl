# sentinel.hcl
# Sentinel policy configuration for Terraform Enterprise
# Maps policies to enforcement levels for financial services compliance
#
# Enforcement levels:
#   hard-mandatory  — Cannot be overridden; blocks apply on failure (security policies)
#   soft-mandatory  — Can be overridden by authorized users (cost/tagging policies)
#   advisory        — Logged but never blocks (informational)

policy "mandatory-tags" {
  source            = "./policies/mandatory-tags.sentinel"
  enforcement_level = "soft-mandatory"

  params = {}
}

policy "restrict-instance-types" {
  source            = "./policies/restrict-instance-types.sentinel"
  enforcement_level = "soft-mandatory"

  params = {}
}

policy "enforce-encryption" {
  source            = "./policies/enforce-encryption.sentinel"
  enforcement_level = "hard-mandatory"

  params = {}
}

policy "restrict-public-access" {
  source            = "./policies/restrict-public-access.sentinel"
  enforcement_level = "hard-mandatory"

  params = {}
}

policy "enforce-multi-az" {
  source            = "./policies/enforce-multi-az.sentinel"
  enforcement_level = "soft-mandatory"

  params = {}
}

policy "restrict-regions" {
  source            = "./policies/restrict-regions.sentinel"
  enforcement_level = "hard-mandatory"

  params = {}
}
