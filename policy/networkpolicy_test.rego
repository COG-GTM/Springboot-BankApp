package netpol

import rego.v1

default_deny := {"contents": {
	"kind": "NetworkPolicy",
	"metadata": {"name": "default-deny-ingress"},
	"spec": {"podSelector": {}, "policyTypes": ["Ingress"]},
}}

some_deployment := {"contents": {"kind": "Deployment", "metadata": {"name": "bankapp-deploy"}}}

test_default_deny_present_passes if {
	count(deny) == 0 with input as [default_deny, some_deployment]
}

test_default_deny_absent_is_denied if {
	count(deny) > 0 with input as [some_deployment]
}
