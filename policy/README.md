# Kubernetes security policy-as-code

Conftest/OPA policies that enforce the Zero Trust controls applied to the
manifests in `kubernetes/`. They run in CI via `.github/workflows/iac-security.yml`
and can be run locally:

```bash
# Rego unit tests
conftest verify --policy policy

# Enforce hardened pod/container security context on the workloads
conftest test kubernetes/bankapp-deployment.yml kubernetes/mysql-deployment.yml --policy policy

# Enforce a default-deny ingress NetworkPolicy exists in the namespace
conftest test --combine kubernetes/ --namespace netpol --policy policy
```

## Controls enforced

| Control | Threat mitigated |
| --- | --- |
| `runAsNonRoot` + `seccompProfile: RuntimeDefault` (pod) | Container breakout / host compromise via root + unrestricted syscalls |
| `allowPrivilegeEscalation: false`, `privileged: false` | Privilege escalation inside the container |
| `readOnlyRootFilesystem: true` | Malware persistence / tampering with the container filesystem |
| `capabilities.drop: [ALL]` | Abuse of Linux capabilities (e.g. `NET_RAW` spoofing) |
| Default-deny ingress NetworkPolicy | East-west lateral movement after a pod compromise |
