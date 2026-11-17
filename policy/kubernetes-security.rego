# Policy-as-code gate enforcing the Zero Trust controls applied to the bankapp
# Kubernetes manifests. Evaluated with Conftest (OPA) in CI so the controls
# cannot silently regress.
#
#   conftest test kubernetes/*.yml kubernetes/*.yaml --policy policy
#   conftest test --combine kubernetes/ --namespace netpol --policy policy
package main

import rego.v1

is_workload if input.kind == "Deployment"

workload_id := sprintf("%s/%s", [input.kind, input.metadata.name])

# --- Pod-level controls -----------------------------------------------------

deny contains msg if {
	is_workload
	not input.spec.template.spec.securityContext.runAsNonRoot == true
	msg := sprintf("%s: pod securityContext.runAsNonRoot must be true", [workload_id])
}

deny contains msg if {
	is_workload
	not input.spec.template.spec.securityContext.seccompProfile.type == "RuntimeDefault"
	msg := sprintf("%s: pod securityContext.seccompProfile.type must be RuntimeDefault", [workload_id])
}

# --- Container-level controls ----------------------------------------------

deny contains msg if {
	is_workload
	some c in input.spec.template.spec.containers
	not c.securityContext.allowPrivilegeEscalation == false
	msg := sprintf("%s container %s: securityContext.allowPrivilegeEscalation must be false", [workload_id, c.name])
}

deny contains msg if {
	is_workload
	some c in input.spec.template.spec.containers
	c.securityContext.privileged == true
	msg := sprintf("%s container %s: securityContext.privileged must not be true", [workload_id, c.name])
}

deny contains msg if {
	is_workload
	some c in input.spec.template.spec.containers
	not c.securityContext.readOnlyRootFilesystem == true
	msg := sprintf("%s container %s: securityContext.readOnlyRootFilesystem must be true", [workload_id, c.name])
}

deny contains msg if {
	is_workload
	some c in input.spec.template.spec.containers
	not drops_all_capabilities(c)
	msg := sprintf("%s container %s: securityContext.capabilities.drop must include ALL", [workload_id, c.name])
}

drops_all_capabilities(c) if {
	some cap in c.securityContext.capabilities.drop
	cap == "ALL"
}
