# Jenkins Pipeline Stage Inventory (Springboot-BankApp)

This note captures every stage in the existing Jenkins DevSecOps pipeline so the
Harness pipeline/templates in this directory can reproduce it 1:1. The Jenkins
files remain the source of truth until teams cut over; this is an **additive**
standardization, nothing is deleted.

## Source files
- `Jenkinsfile` — CI job (`BankApp-CI`), uses `@Library('Shared')` from `vars/`.
- `GitOps/Jenkinsfile` — CD job (`BankApp-CD`), triggered by CI on success.
- `vars/*.groovy` — shared library step implementations (see mapping below).

## CI pipeline (`Jenkinsfile`)

| # | Jenkins stage | Shared-lib call (`vars/`) | Underlying command | Harness equivalent |
|---|---------------|---------------------------|--------------------|--------------------|
| 1 | Workspace cleanup | `cleanWs()` | Jenkins built-in | Implicit — Harness runs each stage on a fresh pod/workspace |
| 2 | Git: Code Checkout | `code_checkout(url, "DevOps")` | `git url:… branch:DevOps` | `cloneCodebase: true` + Harness code connector |
| 3 | Trivy: Filesystem scan | `trivy_scan()` | `trivy fs .` | `Run` step — Trivy fs (in security step group) |
| 4 | OWASP: Dependency check | `owasp_dependency()` | `dependencyCheck --scan ./` + publish `dependency-check-report.xml` | `Run` step — OWASP Dependency-Check (in security step group) |
| 5 | SonarQube: Code Analysis | `sonarqube_analysis("Sonar","bankapp","bankapp")` | `sonar-scanner -Dsonar.projectName=… -Dsonar.projectKey=…` | Harness `SonarqubeScanner` (STO) / `Run` step (in security step group) |
| 6 | SonarQube: Quality Gate | `sonarqube_code_quality()` | `waitForQualityGate` (1 min timeout) | `Run` step polling the Sonar quality gate API |
| 7 | Docker: Build Images | `docker_build("bankapp",TAG,"madhupdevops")` | `docker build -t madhupdevops/bankapp:TAG .` | `BuildAndPushDockerRegistry` (or Kaniko/`Run` buildx) |
| 8 | Docker: Push to DockerHub | `docker_push("bankapp",TAG,"madhupdevops")` | `docker login` + `docker push` (creds id `docker`) | Combined into `BuildAndPushDockerRegistry` with a Docker connector |
| — | `post { success }` | `archiveArtifacts '*.xml'` + trigger `BankApp-CD` | — | Harness stage artifact + next CD stage in same pipeline |

Note: the Jenkins CI job never runs `mvn test` directly — the Maven build happens
inside the multi-stage `Dockerfile` (`mvn clean install -DskipTests=true`). The
Harness pipeline adds an **explicit Build & Test** step (`./mvnw clean test`) up
front so unit tests run and fail fast before image build, which is the intended
target state for the converged platform.

## CD pipeline (`GitOps/Jenkinsfile`)

| # | Jenkins stage | Command | Harness equivalent |
|---|---------------|---------|--------------------|
| 1 | Workspace cleanup | `cleanWs()` | Implicit |
| 2 | Git: Code Checkout | `code_checkout(url,"DevOps")` | `cloneCodebase` |
| 3 | Verify: Docker Image Tags | `echo DOCKER_TAG` | Pipeline variable `<+pipeline.variables.docker_tag>` |
| 4 | Update: Kubernetes manifest | `sed -i 's|…bankapp-eks:.*|…bankapp-eks:TAG|' kubernetes/bankapp-deployment.yaml` | GitOps manifest update `Run` step (or Harness GitOps ApplicationSet sync) |
| 5 | Git: update & push | commit + `git push … DevOps` (creds `Github-cred`) | `Run` step committing to the GitOps repo (or native GitOps sync) |
| — | `post { always }` | `emailext` build notification | Harness pipeline notification rule (Email/Slack) |

## Deploy target facts (from `kubernetes/` + `helm/`)
- Namespace: `bankapp-namespace` (`kubernetes/bankapp-namespace.yaml`).
- Deployment: `bankapp-deploy`, image `trainwithshubham/bankapp-eks:<tag>`
  (`kubernetes/bankapp-deployment.yml`).
- Service: `bankapp-service` on port 8080 (`kubernetes/bankapp-service.yaml`).
- Delivery model: **GitOps** — CD job rewrites the image tag in the manifest and
  pushes; ArgoCD reconciles the cluster to match Git.

## Connectors / secrets used (map these in Harness, never hard-code)
| Jenkins credential | Purpose | Harness connector/secret placeholder |
|--------------------|---------|--------------------------------------|
| `Github-cred` | Clone + GitOps push | `<+configuration>` Git/GitHub connector `account.Github` |
| `docker` | DockerHub login/push | Docker connector `account.DockerHub` |
| `Sonar` (`SONAR_HOME`, `withSonarQubeEnv`) | SonarQube server + token | SonarQube connector `account.SonarQube` + secret `sonar_token` |
| Kubeconfig (ArgoCD/EKS) | Deploy | K8s connector `account.EKS` / GitOps agent |
