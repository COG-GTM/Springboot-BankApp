# Combined-mode policy: asserts the namespace ships a default-deny ingress
# NetworkPolicy. Run with:
#
#   conftest test --combine kubernetes/ --namespace netpol --policy policy
package netpol

import rego.v1

default_deny_exists if {
	some f in input
	np := f.contents
	np.kind == "NetworkPolicy"
	np.spec.podSelector == {}
	"Ingress" in np.spec.policyTypes
}

deny contains msg if {
	not default_deny_exists
	msg := "no default-deny ingress NetworkPolicy found in the bankapp namespace manifests"
}
