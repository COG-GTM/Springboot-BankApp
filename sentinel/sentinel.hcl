# =============================================================================
# Sentinel Policy Set Configuration for Terraform Enterprise
# =============================================================================
# This policy set enforces governance rules across AWS infrastructure for
# the Springboot-BankApp project deployed on AWS EKS.
#
# Enforcement Levels:
#   - hard-mandatory:  Cannot be overridden. Blocks apply on violation.
#   - soft-mandatory:  Can be overridden by authorized users.
#   - advisory:        Logged but does not block apply.
# =============================================================================

# ---------------------------------------------------------------------------
# Encryption Policies
# ---------------------------------------------------------------------------

policy "require-s3-encryption" {
  source            = "./policies/encryption/require-s3-encryption.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-rds-encryption" {
  source            = "./policies/encryption/require-rds-encryption.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-ebs-encryption" {
  source            = "./policies/encryption/require-ebs-encryption.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-eks-encryption" {
  source            = "./policies/encryption/require-eks-encryption.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-sqs-sns-encryption" {
  source            = "./policies/encryption/require-sqs-sns-encryption.sentinel"
  enforcement_level = "soft-mandatory"
}

policy "require-elasticache-encryption" {
  source            = "./policies/encryption/require-elasticache-encryption.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-cloudwatch-log-encryption" {
  source            = "./policies/encryption/require-cloudwatch-log-encryption.sentinel"
  enforcement_level = "soft-mandatory"
}

# ---------------------------------------------------------------------------
# Access Control Policies
# ---------------------------------------------------------------------------

policy "restrict-iam-policies" {
  source            = "./policies/access-control/restrict-iam-policies.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "restrict-security-group-ingress" {
  source            = "./policies/access-control/restrict-security-group-ingress.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-vpc-flow-logs" {
  source            = "./policies/access-control/require-vpc-flow-logs.sentinel"
  enforcement_level = "soft-mandatory"
}

policy "restrict-public-access" {
  source            = "./policies/access-control/restrict-public-access.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-imdsv2" {
  source            = "./policies/access-control/require-imdsv2.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "restrict-rds-public-access" {
  source            = "./policies/access-control/restrict-rds-public-access.sentinel"
  enforcement_level = "hard-mandatory"
}

# ---------------------------------------------------------------------------
# Tagging Policies
# ---------------------------------------------------------------------------

policy "enforce-mandatory-tags" {
  source            = "./policies/tagging/enforce-mandatory-tags.sentinel"
  enforcement_level = "soft-mandatory"
}

# ---------------------------------------------------------------------------
# Region Restriction Policies
# ---------------------------------------------------------------------------

policy "restrict-aws-regions" {
  source            = "./policies/region/restrict-aws-regions.sentinel"
  enforcement_level = "hard-mandatory"
}
