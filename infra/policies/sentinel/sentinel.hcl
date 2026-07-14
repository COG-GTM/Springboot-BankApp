# Sentinel policy set for Springboot-BankApp AWS infrastructure.
#
# Enforcement levels:
#   hard-mandatory - the plan cannot be applied until the violation is fixed.
#   soft-mandatory - can be overridden by an authorized approver.
#   advisory       - informational only.
#
# Attach this policy set to a Terraform Cloud/Enterprise workspace, or evaluate
# locally with the Sentinel CLI against a mock generated from a plan:
#   terraform plan -out=tfplan.bin
#   terraform show -json tfplan.bin > tfplan.json
#   sentinel apply -global "..."   # or use a generated mock (see test/README).

import "module" "resources" {
  source = "./modules/resources.sentinel"
}

policy "require-tags" {
  source            = "./require-tags.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "enforce-encryption-at-rest" {
  source            = "./enforce-encryption-at-rest.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "restrict-public-access" {
  source            = "./restrict-public-access.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "approved-regions" {
  source            = "./approved-regions.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "approved-instance-types" {
  source            = "./approved-instance-types.sentinel"
  enforcement_level = "soft-mandatory"
}

policy "restrict-iam-wildcards" {
  source            = "./restrict-iam-wildcards.sentinel"
  enforcement_level = "hard-mandatory"
}

policy "require-logging" {
  source            = "./require-logging.sentinel"
  enforcement_level = "hard-mandatory"
}
