package main

import rego.v1

hardened_deployment := {
	"kind": "Deployment",
	"metadata": {"name": "bankapp-deploy"},
	"spec": {"template": {"spec": {
		"securityContext": {
			"runAsNonRoot": true,
			"seccompProfile": {"type": "RuntimeDefault"},
		},
		"containers": [{
			"name": "bankapp",
			"securityContext": {
				"allowPrivilegeEscalation": false,
				"readOnlyRootFilesystem": true,
				"capabilities": {"drop": ["ALL"]},
			},
		}],
	}}},
}

insecure_deployment := {
	"kind": "Deployment",
	"metadata": {"name": "legacy"},
	"spec": {"template": {"spec": {"containers": [{"name": "legacy"}]}}},
}

test_hardened_deployment_passes if {
	count(deny) == 0 with input as hardened_deployment
}

test_insecure_deployment_is_denied if {
	count(deny) > 0 with input as insecure_deployment
}

test_missing_drop_all_is_denied if {
	d := json.patch(hardened_deployment, [{
		"op": "replace",
		"path": "/spec/template/spec/containers/0/securityContext/capabilities/drop",
		"value": ["NET_RAW"],
	}])
	count(deny) > 0 with input as d
}

test_writable_root_filesystem_is_denied if {
	d := json.patch(hardened_deployment, [{
		"op": "replace",
		"path": "/spec/template/spec/containers/0/securityContext/readOnlyRootFilesystem",
		"value": false,
	}])
	count(deny) > 0 with input as d
}
