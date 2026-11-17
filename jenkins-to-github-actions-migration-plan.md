# Jenkins to GitHub Actions Migration Plan

## Springboot-BankApp CI/CD Pipelines

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Jenkins Architecture](#2-current-jenkins-architecture)
3. [Stage-by-Stage Mapping: CI Pipeline](#3-stage-by-stage-mapping-ci-pipeline)
4. [Stage-by-Stage Mapping: CD Pipeline](#4-stage-by-stage-mapping-cd-pipeline)
5. [Proposed GitHub Actions Workflow Files](#5-proposed-github-actions-workflow-files)
6. [Secrets & Credentials Configuration](#6-secrets--credentials-configuration)
7. [Shared Library Functions: Review & Disposition](#7-shared-library-functions-review--disposition)
8. [Migration Risks & Considerations](#8-migration-risks--considerations)
9. [Migration Checklist](#9-migration-checklist)

---

## 1. Executive Summary

The Springboot-BankApp project uses a **two-pipeline Jenkins architecture**:

| Pipeline | File | Purpose |
|----------|------|---------|
| **CI Pipeline** | `Jenkinsfile` | Build, security scanning (Trivy, OWASP, SonarQube), Docker image build/push to DockerHub |
| **CD Pipeline** | `GitOps/Jenkinsfile` | Update Kubernetes manifests with new image tags, push to Git for ArgoCD sync |

Both pipelines depend on a **Jenkins Shared Library** (`@Library('Shared')`) with 14 Groovy functions in `vars/`. The migration replaces these with a combination of marketplace GitHub Actions, inline shell steps, and (where needed) composite/reusable workflows.

**Key Complexity Drivers:**
- SonarQube integration (requires self-hosted SonarQube or SonarCloud migration decision)
- OWASP Dependency-Check (Jenkins plugin with `odcInstallation` reference)
- CI-to-CD pipeline triggering (Jenkins `build job:` call)
- GitOps manifest update + push (requires Git credentials in Actions)
- Email notification (Jenkins `emailext` plugin)

---

## 2. Current Jenkins Architecture

### 2.1 CI Pipeline Flow (`Jenkinsfile`)

```
Workspace Cleanup
    |
Git: Code Checkout (DevOps branch)
    |
Trivy: Filesystem Scan
    |
OWASP: Dependency Check
    |
SonarQube: Code Analysis
    |
SonarQube: Quality Gates
    |
Docker: Build Image (madhupdevops/bankapp:<tag>)
    |
Docker: Push to DockerHub
    |
[post-success] Archive XML artifacts + Trigger CD pipeline (BankApp-CD)
```

### 2.2 CD Pipeline Flow (`GitOps/Jenkinsfile`)

```
Workspace Cleanup
    |
Git: Code Checkout (DevOps branch)
    |
Verify: Docker Image Tag (echo)
    |
Update: Kubernetes manifest (sed bankapp-deployment.yaml)
    |
Git: Commit & Push updated manifests
    |
[post-always] Send email notification
```

### 2.3 Jenkins Shared Library (`vars/`)

| Function | File | Used By |
|----------|------|---------|
| `code_checkout(url, branch)` | `code_checkout.groovy` | CI + CD |
| `codeCheckout(branch, url, credId)` | `codeCheckout.groovy` | Not directly used (enhanced version) |
| `trivy_scan()` | `trivy_scan.groovy` | CI |
| `owasp_dependency()` | `owasp_dependency.groovy` | CI |
| `sonarqube_analysis(tool, name, key)` | `sonarqube_analysis.groovy` | CI |
| `sonarqube_code_quality()` | `sonarqube_code_quality.groovy` | CI |
| `docker_build(name, tag, user)` | `docker_build.groovy` | CI |
| `docker_push(name, tag, user)` | `docker_push.groovy` | CI |
| `buildImage(imageName)` | `buildImage.groovy` | Not directly used (alt version) |
| `pushImage(imageName)` | `pushImage.groovy` | Not directly used (alt version) |
| `deploy()` | `deploy.groovy` | Not directly used in current pipelines |
| `docker_compose()` | `docker_compose.groovy` | Not directly used in current pipelines |
| `docker_cleanup(name, tag, user)` | `docker_cleanup.groovy` | Not directly used in current pipelines |
| `greet(name)` | `greet.groovy` | Not directly used in current pipelines |

---

## 3. Stage-by-Stage Mapping: CI Pipeline

### 3.1 Workspace Cleanup

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `cleanWs()` | Not needed |
| **Why** | Jenkins agents persist state | GHA runners start fresh per job |
| **Action** | **Remove** -- no equivalent needed |

### 3.2 Git: Code Checkout

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `code_checkout("https://github.com/.../Springboot-BankApp.git", "DevOps")` | `actions/checkout@v4` |
| **Shared Lib** | `code_checkout.groovy` -- wraps `git url: ..., branch: ...` | Built-in action |
| **Action** | Replace with `actions/checkout@v4` with `ref: DevOps` (or trigger branch) |

```yaml
- uses: actions/checkout@v4
  with:
    ref: ${{ github.ref }}
    fetch-depth: 0  # Full clone for SonarQube analysis
```

### 3.3 Trivy: Filesystem Scan

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `trivy_scan()` -> `sh "trivy fs ."` | `aquasecurity/trivy-action@master` |
| **Shared Lib** | `trivy_scan.groovy` -- single shell command | Marketplace action |
| **Action** | Replace with Trivy GitHub Action |

```yaml
- name: Trivy Filesystem Scan
  uses: aquasecurity/trivy-action@master
  with:
    scan-type: 'fs'
    scan-ref: '.'
    format: 'table'
    exit-code: '1'
    severity: 'HIGH,CRITICAL'
```

### 3.4 OWASP: Dependency Check

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `owasp_dependency()` -> `dependencyCheck` + `dependencyCheckPublisher` | `dependency-check/Dependency-Check_Action@main` |
| **Shared Lib** | `owasp_dependency.groovy` -- uses Jenkins OWASP plugin with `odcInstallation: 'OWASP'` | Marketplace action or CLI install |
| **Action** | Replace with OWASP Dependency-Check GitHub Action |
| **Note** | Jenkins uses `odcInstallation` (pre-configured tool); GHA needs NVD API key for faster scans |

```yaml
- name: OWASP Dependency Check
  uses: dependency-check/Dependency-Check_Action@main
  with:
    project: 'bankapp'
    path: '.'
    format: 'XML,HTML'
    args: '--failOnCVSS 7'
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}

- name: Upload OWASP Report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: owasp-dependency-check-report
    path: reports/
```

### 3.5 SonarQube: Code Analysis

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `sonarqube_analysis("Sonar", "bankapp", "bankapp")` | `SonarSource/sonarqube-scan-action@v5` or `sonarcloud-github-action` |
| **Shared Lib** | `sonarqube_analysis.groovy` -- runs `sonar-scanner` CLI with `withSonarQubeEnv` | Marketplace action |
| **Action** | Replace with SonarQube/SonarCloud action |
| **Decision Required** | Self-hosted SonarQube vs. SonarCloud (see [Section 8](#8-migration-risks--considerations)) |

**Option A: Self-Hosted SonarQube** (requires network access from GHA runner to SonarQube server)
```yaml
- name: SonarQube Analysis
  uses: SonarSource/sonarqube-scan-action@v5
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
  with:
    args: >
      -Dsonar.projectKey=bankapp
      -Dsonar.projectName=bankapp
```

**Option B: SonarCloud** (hosted -- no self-managed server needed)
```yaml
- name: SonarCloud Analysis
  uses: SonarSource/sonarcloud-github-action@v5
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  with:
    args: >
      -Dsonar.projectKey=bankapp
      -Dsonar.organization=${{ secrets.SONAR_ORGANIZATION }}
```

### 3.6 SonarQube: Quality Gates

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `sonarqube_code_quality()` -> `waitForQualityGate abortPipeline: false` | `SonarSource/sonarqube-quality-gate-action@v1` |
| **Shared Lib** | `sonarqube_code_quality.groovy` -- 1-min timeout, waits for SonarQube webhook callback | Action polls SonarQube API |
| **Action** | Replace with quality gate action |
| **Note** | Jenkins uses webhook-based wait; GHA action polls the API directly |

```yaml
- name: SonarQube Quality Gate
  uses: SonarSource/sonarqube-quality-gate-action@v1
  timeout-minutes: 5
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
    SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

### 3.7 Docker: Build Image

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `docker_build("bankapp", "${params.DOCKER_TAG}", "madhupdevops")` | `docker/build-push-action@v6` |
| **Shared Lib** | `docker_build.groovy` -- `docker build -t user/project:tag .` | Marketplace action with BuildKit/cache support |
| **Action** | Replace with Docker build-push action (build only in this step, or combine with push) |

### 3.8 Docker: Push to DockerHub

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `docker_push("bankapp", "${params.DOCKER_TAG}", "madhupdevops")` | `docker/login-action@v3` + `docker/build-push-action@v6` |
| **Shared Lib** | `docker_push.groovy` -- uses `withCredentials([usernamePassword(credentialsId: 'docker', ...)])` then `docker login` + `docker push` | Action handles auth natively |
| **Action** | Combine build + push into single step with caching |

```yaml
- name: Login to DockerHub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}

- name: Build and Push Docker Image
  uses: docker/build-push-action@v6
  with:
    context: .
    push: true
    tags: |
      ${{ secrets.DOCKERHUB_USERNAME }}/bankapp:${{ github.sha }}
      ${{ secrets.DOCKERHUB_USERNAME }}/bankapp:latest
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### 3.9 Post-Success: Archive Artifacts + Trigger CD

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `archiveArtifacts artifacts: '*.xml'` + `build job: "BankApp-CD"` | `actions/upload-artifact@v4` + `workflow_dispatch` trigger or `repository_dispatch` |
| **Action** | Upload artifacts with action; trigger CD via `workflow_dispatch` API call or `workflow_run` event |

```yaml
- name: Upload Scan Reports
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: scan-reports
    path: '*.xml'

- name: Trigger CD Workflow
  if: success()
  uses: peter-evans/repository-dispatch@v3
  with:
    event-type: deploy
    client-payload: '{"docker_tag": "${{ github.sha }}"}'
```

---

## 4. Stage-by-Stage Mapping: CD Pipeline

### 4.1 Workspace Cleanup + Git Checkout

Same as CI -- handled automatically by GHA runners + `actions/checkout@v4`.

### 4.2 Verify Docker Image Tag

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `echo "DOCKER TAG RECEIVED: ${params.DOCKER_TAG}"` | Inline `echo` in `run:` step |
| **Action** | Simple logging step |

### 4.3 Update Kubernetes Manifest

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `sed -i` to update image tag in `bankapp-deployment.yaml` | Same `sed` command in `run:` step |
| **Action** | Direct replacement |

```yaml
- name: Update Kubernetes Manifest
  run: |
    cd kubernetes
    sed -i "s|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:${{ github.event.client_payload.docker_tag }}|g" bankapp-deployment.yaml
    cat bankapp-deployment.yaml | grep "image:"
```

### 4.4 Git: Commit & Push Updated Manifests

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `withCredentials([gitUsernamePassword(...)])` + `git add . && git commit && git push` | `stefanzweifel/git-auto-commit-action@v5` or manual git commands with `GITHUB_TOKEN` |
| **Shared Lib** | Uses `Github-cred` credential ID | GHA uses `GITHUB_TOKEN` or PAT |
| **Action** | Replace with auto-commit action or explicit git commands |

```yaml
- name: Commit and Push Manifest Changes
  run: |
    git config user.name "github-actions[bot]"
    git config user.email "github-actions[bot]@users.noreply.github.com"
    git add kubernetes/bankapp-deployment.yaml
    git commit -m "chore: update bankapp image to ${{ github.event.client_payload.docker_tag }}"
    git push
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

> **Note:** If the CD pipeline pushes to a *different* repository, a Personal Access Token (PAT) with `repo` scope is required instead of `GITHUB_TOKEN`.

### 4.5 Post: Email Notification

| Aspect | Jenkins | GitHub Actions |
|--------|---------|----------------|
| **What** | `emailext` plugin with HTML body | `dawidd6/action-send-mail@v3` or native GitHub notifications |
| **Action** | Replace with email action or consider Slack/Teams notification instead |

```yaml
- name: Send Email Notification
  if: always()
  uses: dawidd6/action-send-mail@v3
  with:
    server_address: smtp.gmail.com
    server_port: 587
    username: ${{ secrets.SMTP_USERNAME }}
    password: ${{ secrets.SMTP_PASSWORD }}
    subject: "BankApp Deployment - ${{ job.status }}"
    to: ${{ secrets.NOTIFICATION_EMAIL }}
    from: ${{ secrets.SMTP_USERNAME }}
    html_body: |
      <html><body>
        <p><b>Workflow:</b> ${{ github.workflow }}</p>
        <p><b>Run:</b> ${{ github.run_number }}</p>
        <p><b>Status:</b> ${{ job.status }}</p>
        <p><b>URL:</b> ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}</p>
      </body></html>
```

---

## 5. Proposed GitHub Actions Workflow Files

### 5.1 CI Workflow: `.github/workflows/ci.yml`

```yaml
name: CI Pipeline

on:
  push:
    branches: [DevOps, main]
  pull_request:
    branches: [DevOps, main]
  workflow_dispatch:
    inputs:
      docker_tag:
        description: 'Docker image tag override'
        required: false
        default: ''

env:
  DOCKER_TAG: ${{ github.event.inputs.docker_tag || github.sha }}

jobs:
  security-scan:
    name: Security Scanning
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Trivy Filesystem Scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
          format: 'table'
          exit-code: '1'
          severity: 'HIGH,CRITICAL'

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean install -DskipTests=true

      - name: OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'bankapp'
          path: '.'
          format: 'XML,HTML'
          args: '--failOnCVSS 7'
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}

      - name: Upload OWASP Report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: owasp-report
          path: reports/

  code-quality:
    name: Code Quality Analysis
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean install -DskipTests=true

      # Option A: Self-hosted SonarQube
      - name: SonarQube Analysis
        uses: SonarSource/sonarqube-scan-action@v5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        with:
          args: >
            -Dsonar.projectKey=bankapp
            -Dsonar.projectName=bankapp
            -Dsonar.java.binaries=target/classes

      - name: SonarQube Quality Gate
        uses: SonarSource/sonarqube-quality-gate-action@v1
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}

  build-and-push:
    name: Build & Push Docker Image
    runs-on: ubuntu-latest
    needs: [security-scan, code-quality]
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to DockerHub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKERHUB_USERNAME }}/bankapp:${{ env.DOCKER_TAG }}
            ${{ secrets.DOCKERHUB_USERNAME }}/bankapp:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Trigger CD Workflow
        if: success() && github.ref == 'refs/heads/DevOps'
        uses: peter-evans/repository-dispatch@v3
        with:
          event-type: deploy
          client-payload: '{"docker_tag": "${{ env.DOCKER_TAG }}"}'
```

### 5.2 CD Workflow: `.github/workflows/cd.yml`

```yaml
name: CD Pipeline

on:
  repository_dispatch:
    types: [deploy]
  workflow_dispatch:
    inputs:
      docker_tag:
        description: 'Docker image tag to deploy'
        required: true

env:
  DOCKER_TAG: ${{ github.event.client_payload.docker_tag || github.event.inputs.docker_tag }}

jobs:
  update-manifests:
    name: Update K8s Manifests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          ref: DevOps
          token: ${{ secrets.GIT_PAT }}  # PAT needed to push commits that trigger ArgoCD

      - name: Verify Docker Tag
        run: echo "Deploying image tag - ${{ env.DOCKER_TAG }}"

      - name: Update Kubernetes Deployment Manifest
        run: |
          cd kubernetes
          sed -i "s|trainwithshubham/bankapp-eks:.*|trainwithshubham/bankapp-eks:${{ env.DOCKER_TAG }}|g" bankapp-deployment.yml
          echo "Updated manifest:"
          grep "image:" bankapp-deployment.yml

      - name: Commit and Push Changes
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add kubernetes/bankapp-deployment.yml
          git diff --staged --quiet && echo "No changes to commit" && exit 0
          git commit -m "chore: update bankapp image to ${{ env.DOCKER_TAG }}"
          git push origin DevOps

      - name: Send Email Notification
        if: always()
        uses: dawidd6/action-send-mail@v3
        with:
          server_address: smtp.gmail.com
          server_port: 587
          username: ${{ secrets.SMTP_USERNAME }}
          password: ${{ secrets.SMTP_PASSWORD }}
          subject: "BankApp Deployment - ${{ job.status }}"
          to: ${{ secrets.NOTIFICATION_EMAIL }}
          from: ${{ secrets.SMTP_USERNAME }}
          html_body: |
            <html><body>
              <div style="background-color: #FFA07A; padding: 10px;">
                <p><b>Workflow:</b> ${{ github.workflow }}</p>
              </div>
              <div style="background-color: #90EE90; padding: 10px;">
                <p><b>Run:</b> #${{ github.run_number }}</p>
              </div>
              <div style="background-color: #87CEEB; padding: 10px;">
                <p><b>URL:</b> ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}</p>
              </div>
            </body></html>
```

---

## 6. Secrets & Credentials Configuration

All secrets below must be configured at **Settings > Secrets and variables > Actions** in the GitHub repository (or organization).

### 6.1 Required Secrets

| Secret Name | Source (Jenkins) | Purpose | How to Obtain |
|-------------|-----------------|---------|---------------|
| `DOCKERHUB_USERNAME` | `docker` credential (username) | DockerHub login username | DockerHub account settings |
| `DOCKERHUB_TOKEN` | `docker` credential (password) | DockerHub access token | DockerHub > Account Settings > Security > Access Tokens |
| `SONAR_TOKEN` | `withSonarQubeEnv("Sonar")` auto-injected | SonarQube/SonarCloud auth | SonarQube > My Account > Security > Generate Token |
| `SONAR_HOST_URL` | `withSonarQubeEnv("Sonar")` auto-injected | SonarQube server URL (e.g. `https://sonar.example.com`) | Self-hosted SonarQube URL (skip if using SonarCloud) |
| `GIT_PAT` | `Github-cred` credential (gitUsernamePassword) | Push manifest commits to repo | GitHub > Settings > Developer settings > PAT (fine-grained, `contents: write`) |

### 6.2 Recommended Secrets

| Secret Name | Purpose | Notes |
|-------------|---------|-------|
| `NVD_API_KEY` | Speed up OWASP Dependency-Check NVD downloads | Free at https://nvd.nist.gov/developers/request-an-api-key -- without it, scans are rate-limited |
| `SONAR_ORGANIZATION` | SonarCloud organization key | Only needed if migrating to SonarCloud |
| `SMTP_USERNAME` | Email notification sender | Gmail address (or SMTP service) |
| `SMTP_PASSWORD` | Email notification password/app password | Gmail App Password (requires 2FA) |
| `NOTIFICATION_EMAIL` | Email notification recipient | Destination email address |

### 6.3 Secrets That Can Be Removed Post-Migration

| Jenkins Credential | Type | Notes |
|-------------------|------|-------|
| `docker` | `usernamePassword` | Replaced by `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN` |
| `Github-cred` | `gitUsernamePassword` | Replaced by `GIT_PAT` or default `GITHUB_TOKEN` |
| `Sonar` (tool installation) | Jenkins tool config | Replaced by `SONAR_TOKEN` + `SONAR_HOST_URL` |
| `OWASP` (tool installation) | Jenkins tool config | Replaced by GitHub Action (self-contained) |

---

## 7. Shared Library Functions: Review & Disposition

### 7.1 Direct Replacements (No Manual Review Needed)

These functions map cleanly to GitHub Actions and can be retired:

| Function | File | GHA Replacement | Notes |
|----------|------|-----------------|-------|
| `code_checkout()` | `code_checkout.groovy` | `actions/checkout@v4` | 1:1 replacement |
| `trivy_scan()` | `trivy_scan.groovy` | `aquasecurity/trivy-action@master` | Direct replacement; action adds severity filtering and report formats |
| `docker_build()` | `docker_build.groovy` | `docker/build-push-action@v6` | Action adds BuildKit, layer caching, multi-platform support |
| `docker_push()` | `docker_push.groovy` | `docker/login-action@v3` + `docker/build-push-action@v6` | Auth handled by login action; no plaintext password exposure |
| `docker_cleanup()` | `docker_cleanup.groovy` | Not needed | GHA runners are ephemeral; images don't persist |
| `docker_compose()` | `docker_compose.groovy` | Inline `run:` step | Only if needed for integration tests |
| `greet()` | `greet.groovy` | Not needed | Utility function; no CI/CD purpose |

### 7.2 Requires Manual Review / Decision

These functions have Jenkins-specific behavior that needs careful migration:

| Function | File | Issue | Action Required |
|----------|------|-------|-----------------|
| `owasp_dependency()` | `owasp_dependency.groovy` | Uses Jenkins `dependencyCheck` plugin step with `odcInstallation: 'OWASP'` and `dependencyCheckPublisher` for report visualization | **Review:** Verify the OWASP Dependency-Check GitHub Action produces equivalent reports. The Jenkins plugin has built-in trend tracking and report publishing that GHA lacks natively. Consider adding `actions/upload-artifact` + a third-party report viewer. |
| `sonarqube_analysis()` | `sonarqube_analysis.groovy` | Uses `withSonarQubeEnv()` which auto-injects `SONAR_HOME`, auth tokens, and server URL from Jenkins global config. References `$SONAR_HOME/bin/sonar-scanner` binary from Jenkins tool installation. | **Review:** Determine whether the team will (a) continue with self-hosted SonarQube (requires network connectivity from GHA runners) or (b) migrate to SonarCloud. Self-hosted requires configuring `SONAR_HOST_URL` and ensuring firewall/VPN access. SonarCloud requires organization setup and project onboarding. |
| `sonarqube_code_quality()` | `sonarqube_code_quality.groovy` | Uses `waitForQualityGate` which depends on SonarQube sending a webhook callback to Jenkins. This mechanism doesn't exist in GHA. | **Review:** The GHA `sonarqube-quality-gate-action` polls the SonarQube API instead of waiting for webhooks. Ensure the SonarQube server's analysis is complete before the quality gate check runs (the action handles this, but timeouts may need tuning). |
| `codeCheckout()` | `codeCheckout.groovy` | Enhanced version with URL/branch validation regex, 5-minute timeout, credential passthrough, and structured error reporting. Not used in current pipelines but part of the shared library. | **Review:** If other Jenkins pipelines in the organization use this function, those pipelines also need migration. The validation logic is not needed in GHA since `actions/checkout` handles these concerns internally. |
| `deploy()` | `deploy.groovy` | Complex Docker Compose deployment with graceful shutdown, cleanup verification, health check polling (30 attempts x 10s), and structured error reporting. | **Review:** Not used in the current CI/CD pipelines but may be used by other projects sharing this library. If needed for integration testing in GHA, translate to a composite action or inline shell script. The health check polling logic needs to be replicated. |
| `buildImage()` | `buildImage.groovy` | Enhanced version with image name validation regex and Docker daemon accessibility check before build. | **Review:** Not used in current pipelines. The Docker daemon check is unnecessary in GHA (Docker is always available on `ubuntu-latest` runners). Image name validation could be added as a pre-step if desired. |
| `pushImage()` | `pushImage.groovy` | Alternative push function using `dockerhub` credential ID (different from `docker` used in `docker_push.groovy`). Tags image differently. | **Review:** Not used in current pipelines. If other pipelines use this function, note the different credential ID (`dockerhub` vs `docker`). |

### 7.3 Cross-Pipeline / Organizational Impact

> **Important:** The `@Library('Shared')` reference in both Jenkinsfiles points to a **shared library repository** (`https://github.com/DevMadhup/Jenkins_SharedLib.git`). Other Jenkins pipelines in the organization may also depend on these functions. Before removing the shared library:
>
> 1. Audit all Jenkins pipelines that reference `@Library('Shared')`
> 2. Migrate those pipelines to GitHub Actions first, or maintain the shared library until all consumers are migrated
> 3. Consider creating a GitHub Actions **reusable workflow** repository as the GHA equivalent of the shared library

---

## 8. Migration Risks & Considerations

### 8.1 SonarQube Connectivity

| Risk | Impact | Mitigation |
|------|--------|------------|
| Self-hosted SonarQube may not be accessible from GitHub-hosted runners | CI fails at code quality stage | Option A: Use a self-hosted GHA runner with network access. Option B: Migrate to SonarCloud. Option C: Set up a VPN/tunnel from GHA to SonarQube. |

### 8.2 CI-to-CD Pipeline Triggering

| Risk | Impact | Mitigation |
|------|--------|------------|
| Jenkins `build job: "BankApp-CD"` is synchronous and passes parameters directly | CD may not trigger reliably | Use `repository_dispatch` event (recommended) or `workflow_run` trigger. The `repository_dispatch` approach is more explicit and supports payload data. |

### 8.3 GitOps Commit Push

| Risk | Impact | Mitigation |
|------|--------|------------|
| `GITHUB_TOKEN` cannot trigger subsequent workflow runs (prevents infinite loops) | ArgoCD may not detect changes if it relies on webhook from push events | Use a Personal Access Token (`GIT_PAT`) instead of `GITHUB_TOKEN` for the manifest push step. |

### 8.4 OWASP Report Visualization

| Risk | Impact | Mitigation |
|------|--------|------------|
| Jenkins `dependencyCheckPublisher` provides in-UI trend graphs and report viewing | Loss of scan trend visibility | Upload reports as artifacts. Optionally integrate with GitHub Security tab via SARIF format (`--format SARIF`). |

### 8.5 Email Notifications

| Risk | Impact | Mitigation |
|------|--------|------------|
| Gmail may block SMTP from GHA runners (security policies) | Notifications fail silently | Use Gmail App Passwords, or switch to SendGrid/Mailgun. Alternatively, replace email with Slack/Teams notifications using `slackapi/slack-github-action@v2`. |

### 8.6 Branch Strategy

| Risk | Impact | Mitigation |
|------|--------|------------|
| Jenkins pipelines hardcode `DevOps` branch | May not align with GitHub branch protection or default branch | Update workflow triggers to match actual branching strategy. Consider whether `DevOps` branch should remain or be consolidated with `main`. |

### 8.7 Docker Image Tagging

| Risk | Impact | Mitigation |
|------|--------|------------|
| Jenkins uses `params.DOCKER_TAG` (manual input); GHA should use deterministic tags | Inconsistent image references | Use `github.sha` (commit SHA) as default tag for traceability. Support manual override via `workflow_dispatch` input. |

---

## 9. Migration Checklist

### Phase 1: Preparation
- [ ] Decide: Self-hosted SonarQube vs. SonarCloud
- [ ] Decide: Email notifications vs. Slack/Teams
- [ ] Audit other Jenkins pipelines using `@Library('Shared')`
- [ ] Register for NVD API key (https://nvd.nist.gov/developers/request-an-api-key)
- [ ] Create DockerHub access token (not password)
- [ ] Create GitHub PAT with `contents: write` scope (fine-grained, scoped to this repo)

### Phase 2: Configure GitHub Repository
- [ ] Add all secrets from [Section 6](#6-secrets--credentials-configuration) to repository settings
- [ ] Create `.github/workflows/ci.yml` (from [Section 5.1](#51-ci-workflow-githubworkflowsciyml))
- [ ] Create `.github/workflows/cd.yml` (from [Section 5.2](#52-cd-workflow-githubworkflowscdyml))
- [ ] If using SonarCloud: create `sonar-project.properties` in repo root
- [ ] Set up branch protection rules for `DevOps`/`main` requiring CI to pass

### Phase 3: Parallel Run & Validation
- [ ] Run GitHub Actions CI alongside Jenkins CI for 1-2 weeks
- [ ] Compare scan results (Trivy, OWASP, SonarQube) between Jenkins and GHA
- [ ] Verify Docker images are built and pushed correctly
- [ ] Verify CD pipeline updates Kubernetes manifests correctly
- [ ] Verify ArgoCD detects and syncs changes from GHA-committed manifests
- [ ] Verify email/Slack notifications are delivered

### Phase 4: Cutover
- [ ] Disable Jenkins CI pipeline
- [ ] Disable Jenkins CD pipeline
- [ ] Remove SonarQube webhook pointing to Jenkins (if migrated to polling)
- [ ] Update team documentation to reference GitHub Actions
- [ ] Archive `Jenkinsfile` and `GitOps/Jenkinsfile` (keep in repo for reference)
- [ ] Optionally remove `vars/` directory after confirming no other consumers

### Phase 5: Cleanup
- [ ] Remove Jenkins job configurations
- [ ] Revoke Jenkins-specific credentials (DockerHub password, GitHub credential, etc.)
- [ ] Update `cicd.md` documentation to reflect GitHub Actions workflows
- [ ] Consider converting shared library patterns to GitHub Actions reusable workflows for organizational reuse
