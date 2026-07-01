# Harness CI/CD — Standardized, Templated Pipelines

This directory converges Springboot-BankApp's Jenkins DevSecOps pipeline onto
**Harness**, using **reusable templates** so other teams can onboard by supplying
a handful of variables instead of copy-pasting a `Jenkinsfile`.

> This is **additive**. The existing `Jenkinsfile`, `GitOps/Jenkinsfile`, and
> `vars/*.groovy` shared library are left intact. Teams cut over when ready.

## Contents

| File | Purpose |
|------|---------|
| `pipeline.yaml` | Full CI+CD pipeline for **this** service (bankapp), built from the templates below. |
| `templates/stepgroup-security-scans.yaml` | **Step Group Template** — Trivy fs scan, OWASP Dependency-Check, SonarQube analysis + quality gate. |
| `templates/stage-ci-build-scan.yaml` | **CI Stage Template** — Build & Test (Maven) → security scans (above) → Docker build & push. |
| `templates/stage-cd-gitops-deploy.yaml` | **CD Stage Template** — verify tag → bump image in the K8s manifest → push (ArgoCD reconciles). |
| `templates/pipeline-template.yaml` | **Pipeline Template** — wires the CI + CD stage templates end-to-end; a new team creates a pipeline from this. |
| `JENKINS_INVENTORY.md` | Stage-by-stage inventory of the current Jenkins pipeline and its Harness equivalents. |

## Reuse model (why templates)

```
Pipeline Template  (java_service_cicd)
   ├── CI Stage Template   (build_scan_and_push_maven)
   │      └── Step Group Template (devsecops_security_scans)  ← Trivy/OWASP/Sonar
   └── CD Stage Template   (gitops_deploy)
```

The Jenkins shared library (`vars/*.groovy`) already expressed "reusable steps"
(`trivy_scan`, `owasp_dependency`, `sonarqube_analysis`, `docker_build`…). Harness
Templates are the platform-native equivalent: versioned (`versionLabel: v1`),
parameterized with runtime inputs (`<+input>`), and shareable across projects or
the whole account.

## Jenkins → Harness stage mapping

| Jenkins (shared-lib call) | Harness step | Where |
|---------------------------|--------------|-------|
| `code_checkout(url,"DevOps")` | `cloneCodebase: true` + Git connector | pipeline `properties.ci.codebase` |
| `trivy_scan()` | `Run` (aquasec/trivy → `trivy fs`) | step group |
| `owasp_dependency()` | `Run` (owasp/dependency-check) | step group |
| `sonarqube_analysis(...)` | `Run` (maven `sonar:sonar`) | step group |
| `sonarqube_code_quality()` | `Run` (poll Sonar quality-gate API) | step group |
| `docker_build()` + `docker_push()` | `BuildAndPushDockerRegistry` | CI stage |
| CD `sed` manifest bump + `git push` | `Run` (sed + git push, ArgoCD sync) | CD stage |
| `post{ emailext }` | `notificationRules` (Email/Slack) | pipeline |

An explicit **Build & Test** step (`./mvnw clean test`) is added at the front of
the CI stage — the Jenkins job only compiled inside the Dockerfile with
`-DskipTests=true`, so tests never ran in CI. Running them here is the intended
converged behavior.

## How another team onboards (Jenkins → Harness in ~10 minutes)

1. **Import the templates.** Add the four YAMLs in `templates/` to your Harness
   project (Templates page → *New Template → Import from Git*), or promote them to
   **account-level** templates so every team shares one copy. Adjust
   `orgIdentifier` / `projectIdentifier` to yours.
2. **Create connectors & secrets** (see next section). This is the equivalent of
   Jenkins *Manage Credentials* + *Global Trusted Pipeline Libraries*.
3. **Create a pipeline from `pipeline-template.yaml`** (*Use Template*). Harness
   prompts for the runtime inputs — supply your values:

   | Variable | Example | Jenkins analogue |
   |----------|---------|------------------|
   | `service_name` | `payments-api` | Sonar project name |
   | `image_repo` | `myorg/payments-api` | `docker_build(project, …, user)` |
   | `docker_tag` | `<+input>` at run time | `params.DOCKER_TAG` |
   | `sonar_host_url` | `https://sonarqube.myorg.com` | SonarQube server "Sonar" |
   | `sonar_project_key` | `payments-api` | `sonarqube_analysis` key |
   | `image_repo` (CD) | `myorg/payments-api-eks` | image in K8s manifest |
   | `gitops_repo` | `github.com/myorg/payments-api.git` | GitOps push target |
   | codebase `connectorRef` / `repoName` | your Git connector / repo | `code_checkout(url,…)` |

4. **Run it.** CI builds/tests/scans and pushes the image; CD bumps the manifest
   tag and pushes so ArgoCD deploys — the same two-job flow as Jenkins, now in one
   Harness pipeline.

## Connectors & secrets (placeholders — never commit real secrets)

Create these in Harness and reference them by identifier. The YAML uses
placeholder refs; swap them for yours.

| Harness entity | Referenced as | Replaces Jenkins |
|----------------|---------------|------------------|
| Git/GitHub connector | `account.Github` | `Github-cred` |
| Docker registry connector | `account.DockerHub` | `docker` credential |
| Kubernetes connector | `account.EKS` | kubeconfig / ArgoCD |
| SonarQube token secret | `<+secrets.getValue("sonar_token")>` | `withSonarQubeEnv("Sonar")` |
| GitOps push token secret | `<+secrets.getValue("gitops_token")>` | `Github-cred` for push |

Secrets live in the Harness Secret Manager (or your vault via a Secret Manager
connector). Never place real tokens in these YAML files.

## Validation

All YAML in this directory is well-formed and parses cleanly. Validate locally:

```bash
python -c "import glob,yaml; [yaml.safe_load(open(f)) for f in glob.glob('.harness/**/*.yaml', recursive=True)]"
```

In Harness, the built-in schema validator runs when you save each pipeline/template.
