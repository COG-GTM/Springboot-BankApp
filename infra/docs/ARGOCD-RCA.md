# Argo CD Deploy Failure — Root Cause Analysis

This RCA documents a **reproducible deploy failure** in the existing GitOps
pipeline that feeds Argo CD, plus two secondary sync-health issues that the
Argo CD `Application` (`infra/argocd/bankapp-application.yaml`) will surface on
an EKS target. All findings are derived from the manifests/pipeline in this
repository — no live cluster was required for the primary finding.

---

## Primary finding — CD image bump silently targets the wrong filename

**Severity:** High — production deploys never receive new image tags.

### Symptom
The `BankApp-CD` GitOps job (`GitOps/Jenkinsfile`) is supposed to bump the
running image tag in the Kubernetes manifest and push the change so Argo CD
reconciles the new version. In practice the image tag is never updated, and the
pipeline step fails.

### Root cause
`GitOps/Jenkinsfile` line 40 edits a file that does not exist:

```groovy
sh """
    sed -i -e 's|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:${params.DOCKER_TAG}|g' bankapp-deployment.yaml
"""
```

The deployment manifest in the repo is `kubernetes/bankapp-deployment.yml`
(`.yml`), **not** `bankapp-deployment.yaml` (`.yaml`).

### Evidence (reproduced against the repo tree)
```
$ ls kubernetes/bankapp-deployment*
kubernetes/bankapp-deployment.yml

$ (cd kubernetes && sed -i -e 's|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:v9|g' bankapp-deployment.yaml)
sed: can't read bankapp-deployment.yaml: No such file or directory
# exit code: 2
```

`sed` exits non-zero, which fails the `sh` step and the CD stage. Because the
commit-and-push stage never runs (or runs with no diff), Argo CD keeps
reconciling the previously committed image (`trainwithshubham/bankapp-eks:v2`).
The result is a green-looking Argo CD app that is silently pinned to a stale
image.

### Proposed fix
Point the `sed` at the real filename (and make the substitution robust):

```diff
- sed -i -e 's|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:${params.DOCKER_TAG}|g' bankapp-deployment.yaml
+ sed -i -e 's|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:${params.DOCKER_TAG}|g' bankapp-deployment.yml
```

Hardening recommendations:
- Fail fast if the target file is missing: `test -f bankapp-deployment.yml`.
- Guard the push against empty commits: `git diff --cached --quiet || git commit ...`.
- Prefer a declarative image updater (Argo CD Image Updater or Kustomize
  `images:`) over in-place `sed`.

> This RCA does not change application/runtime code; the fix above is documented
> for the pipeline owners and intentionally left out of this infrastructure PR.

---

## Secondary finding A — in-cluster MySQL storage will not schedule on EKS

**Severity:** Medium — DB pod stuck `Pending`, app `initContainer` blocks, app
never becomes Ready.

`kubernetes/persistent-volume.yaml` defines a **hostPath** PersistentVolume
(`/mnt/data/mysql`) with `storageClassName: standard`, and
`kubernetes/persistent-volume-claim.yaml` requests the same. On EKS:
- The default StorageClass is `gp2`/`gp3` (EBS CSI), not `standard`.
- `hostPath` data is node-local and lost on node replacement (nodes are
  cattle), so even if it binds, MySQL data is not durable.

Consequently the MySQL pod can sit `Pending`, the app's
`wait-for-mysql` init container loops, and Argo CD reports the Application
`Progressing`/`Degraded` rather than `Healthy`.

The PersistentVolume also sets `metadata.namespace` on a **cluster-scoped**
resource; Argo CD/kubectl ignore it, but it is invalid and should be removed.

**Fix / direction:** this is exactly what the generated Terraform `rds` module
addresses — replace the in-cluster MySQL (Deployment/StatefulSet + PV/PVC) with
managed, encrypted, Multi-AZ RDS, and point `SPRING_DATASOURCE_URL` at the RDS
endpoint output.

## Secondary finding B — HPA vs. Argo CD replica drift

**Severity:** Low — cosmetic `OutOfSync`, but causes reconcile churn.

`kubernetes/bankapp-deployment.yml` hardcodes `replicas: 2` while
`kubernetes/bankapp-hpa.yml` manages replicas (1–5). With automated self-heal,
Argo CD and the HPA fight over `spec.replicas`. The generated
`Application` manifest sets `ignoreDifferences` on
`/spec/replicas` to break this loop.
