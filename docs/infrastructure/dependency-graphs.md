# Module Dependency Graphs

> Visual dependency maps for every infrastructure module in the BankApp project. All diagrams are rendered as PNG images.

## Full System Architecture

Shows how the CI/CD pipeline, GitOps flow, and Kubernetes runtime connect end-to-end.

![Full System Architecture](diagrams/full-system-architecture.png)

---

## Kubernetes Module Dependencies

Shows the resource dependency order within `kubernetes/` — which resources must exist before others can be created.

![Kubernetes Module Dependencies](diagrams/kubernetes-module-deps.png)

**Key dependency chains:**

1. **Namespace** → all resources (everything lives in `bankapp-namespace`)
2. **Secret + ConfigMap** → MySQL Deployment, BankApp Deployment (environment variables)
3. **PV → PVC** → MySQL Deployment (volume mount)
4. **MySQL Deployment → MySQL Service** → BankApp Deployment (JDBC connection)
5. **BankApp Deployment → BankApp Service** → Ingress (traffic routing)
6. **ClusterIssuer** → Ingress (TLS certificate provisioning)
7. **BankApp Deployment** → HPA (scaling target)

---

## Helm Chart Dependencies

Shows template dependencies within the `helm/bankapp/` chart, including how `values.yaml` feeds into each template.

![Helm Chart Dependencies](diagrams/helm-chart-deps.png)

**Differences from raw manifests:**
- MySQL uses **StatefulSet** instead of Deployment
- MySQL Service is **headless** (`clusterIP: None`)
- Init container in BankApp Deployment waits for MySQL readiness
- Includes **VPA** (Vertical Pod Autoscaler) in addition to HPA
- Secrets are auto base64-encoded via `b64enc` template function

---

## CI/CD Pipeline Flow

Shows the Jenkins CI pipeline stages and their dependencies on external tools and credentials.

![CI/CD Pipeline Flow](diagrams/cicd-pipeline-flow.png)

**Pipeline stages:** Workspace Cleanup → Git Checkout → Trivy FS Scan → OWASP Dependency Check → SonarQube Analysis → SonarQube Quality Gate → Docker Build → Docker Push → *(triggers CD pipeline)*

---

## GitOps Pipeline Flow

Shows the CD pipeline that updates Kubernetes manifests and triggers ArgoCD sync.

![GitOps Pipeline Flow](diagrams/gitops-pipeline-flow.png)

**Pipeline stages:** Workspace Cleanup → Git Checkout → Verify Tag → Update K8s Manifest → Git Push → *(ArgoCD auto-sync)*

---

## Shared Library Dependencies

Shows which shared library functions are used by the CI and CD pipelines.

![Shared Library Dependencies](diagrams/shared-library-deps.png)

**CI Pipeline (`Jenkinsfile`)** uses: `code_checkout`, `trivy_scan`, `owasp_dependency`, `sonarqube_analysis`, `sonarqube_code_quality`, `docker_build`, `docker_push`

**CD Pipeline (`GitOps/Jenkinsfile`)** uses: `code_checkout`
