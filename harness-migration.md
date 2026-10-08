# Jenkins to Harness migration

The two Jenkins jobs are replicated as two Harness pipelines. The Jenkinsfiles stay in place so both systems can run in parallel until the Harness pipelines are validated.

| Jenkins | Harness |
| --- | --- |
| `Jenkinsfile` (CI job) | [`.harness/bankapp-ci.yaml`](.harness/bankapp-ci.yaml) (`bankapp_ci`) |
| `GitOps/Jenkinsfile` (`BankApp-CD` job) | [`.harness/bankapp-cd.yaml`](.harness/bankapp-cd.yaml) (`bankapp_cd`) |
| GitHub webhook / Poll SCM (`cicd.md`) | [`.harness/triggers/bankapp-ci-push-devops.yaml`](.harness/triggers/bankapp-ci-push-devops.yaml) |
| Credential `Github-cred` | [`.harness/connectors/github_bankapp.yaml`](.harness/connectors/github_bankapp.yaml) |
| Credential `docker` | [`.harness/connectors/dockerhub_bankapp.yaml`](.harness/connectors/dockerhub_bankapp.yaml) |
| `post.success { build job: "BankApp-CD" }` | `trigger_bankapp_cd` pipeline-chaining stage at the end of `bankapp_ci` |
| `post.always { emailext ... }` | `notify` stage (`when: pipelineStatus: All`) with an Email step in `bankapp_cd` |

All YAML uses `orgIdentifier: default` and `projectIdentifier: Springboot_BankApp`. Change both if your Harness project uses other identifiers. The pipeline and trigger YAML validates against the published [Harness v0 schema](https://github.com/harness/harness-schema).

## Stage mapping

### CI (`bankapp_ci`)

| Jenkins stage | Harness step | Notes |
| --- | --- | --- |
| Workspace cleanup | (implicit) | Each CI stage starts with an empty workspace. |
| Git: Code Checkout | codebase clone | `properties.ci.codebase` with `github_bankapp`. The trigger sets the branch to `<+trigger.branch>` (`DevOps`). Manual runs ask for the branch. |
| Trivy: Filesystem scan | `trivy_fs_scan` (Run) | `trivy fs .`, report only. Same as Jenkins. |
| OWASP: Dependency check | `owasp_dependency_check` (Run) | `dependency-check.sh --scan ./ --project bankapp --format XML --out .`. Writes `dependency-check-report.xml`. |
| SonarQube: Code Analysis | `sonarqube_analysis` (Run) | `sonar-scanner -Dsonar.projectName=bankapp -Dsonar.projectKey=bankapp -Dsonar.java.binaries=. -X` |
| SonarQube: Code Quality Gates | `sonarqube_quality_gate` (Run) | Polls the CE task from `.scannerwork/report-task.txt`. **Fails the pipeline** unless the gate is `OK`. |
| Docker: Build Images + Push to DockerHub | `docker_build_and_push` (BuildAndPushDockerRegistry) | Root `Dockerfile`, image `madhupdevops/bankapp:<+pipeline.variables.DOCKER_TAG>`. |
| `build job: "BankApp-CD"` | `trigger_bankapp_cd` (Pipeline stage) | Passes `DOCKER_TAG`. Runs only if the CI stage succeeds. |

`DOCKER_TAG` is a runtime input that defaults to `<+codebase.commitSha>`. The trigger also sets it to `<+codebase.commitSha>`.

### CD (`bankapp_cd`)

| Jenkins stage | Harness step | Notes |
| --- | --- | --- |
| Workspace cleanup / Git: Code Checkout | codebase clone of `DevOps` | Fixed branch, same as Jenkins. |
| Verify: Docker Image Tags | `verify_docker_tag` | Echoes the tag, and also fails if the tag is empty. |
| Update: Kubernetes manifest | `update_k8s_manifest` | `sed` on **`kubernetes/bankapp-deployment.yml`**. Jenkins points at `bankapp-deployment.yaml`, which does not exist in this repo. |
| Git: Code update and push to GitHub | `git_commit_and_push` | Same commit message. Pushes `HEAD:DevOps` to `<+codebase.repoUrl>` using the `github_pat` secret (the same token the connector uses). |
| `post.always` emailext | `notify` / `email_notification` | Subject `BankApp Application has been updated and deployed - '<status of gitops_update>'`. The HTML body carries over, with `JOB_NAME`/`BUILD_NUMBER`/`BUILD_URL` mapped to `<+pipeline.name>`/`<+pipeline.sequenceId>`/`<+pipeline.executionUrl>`. |

ArgoCD still watches `kubernetes/` on `DevOps`, so nothing changes downstream.

## Harness resources to create

### Connectors (project scope)

| Identifier | Type | Replaces | Used by |
| --- | --- | --- | --- |
| `github_bankapp` | GitHub (account URL `https://github.com/COG-GTM`, username + token, API access enabled) | `Github-cred` | Codebase clone (CI and CD), the push webhook trigger, and Git Experience import |
| `dockerhub_bankapp` | Docker Registry (DockerHub, user `madhupdevops`) | `docker` | `docker_build_and_push`, and pulling every Run step image |

The SonarQube server needs no connector. The analysis steps run `sonar-scanner` against `SONAR_HOST_URL` with a token, just as `withSonarQubeEnv("Sonar")` did. If you have STO, you can swap in the native `Sonarqube` step instead (see "Optional STO steps").

### Secrets (Harness Secret Manager or your external secret manager)

| Identifier | Content | Used by |
| --- | --- | --- |
| `github_pat` | GitHub PAT with `repo` scope (contents write, webhooks admin) for COG-GTM/Springboot-BankApp | `github_bankapp` connector, `git_commit_and_push` |
| `dockerhub_pat` | DockerHub access token for `madhupdevops` (read/write) | `dockerhub_bankapp` connector |
| `sonarqube_token` | SonarQube analysis token with Execute Analysis on `bankapp` and Browse access (needed to read the quality gate) | `sonarqube_analysis`, `sonarqube_quality_gate` |
| `nvd_api_key` | NVD API key ([request one](https://nvd.nist.gov/developers/request-an-api-key)). Without it, dependency-check 10.x takes hours to download the NVD feed. | `owasp_dependency_check` |

### Variables and settings

- Project variable `sonar_host_url`: the base URL of the SonarQube server (the Jenkins `Sonar` installation). The CI pipeline reads it as `<+variable.sonar_host_url>`.
- SMTP configuration (Account Settings, then SMTP): the Email step needs it. The sender address comes from this SMTP config, so set it to `trainwithshubham@gmail.com` to match `emailext from:`.
- Webhook: once `github_bankapp` has API access, saving the trigger registers the GitHub webhook automatically. Otherwise, copy the trigger's webhook URL into the repo's Settings, then Webhooks (push events).

### Delegates and build infrastructure

- Both CI stages run on **Harness Cloud** (`runtime: Cloud`), and the connectors use `executeOnDelegate: false`. That works when GitHub, DockerHub and SonarQube are all reachable from the internet.
- If SonarQube (or any other dependency) is private, which mirrors the self-hosted Jenkins agent setup:
  1. Install a Kubernetes delegate (for example with Helm in the EKS cluster) and tag it, for example `bankapp-delegate`.
  2. Create a `K8sCluster` connector, for example `bankapp_k8s`, with `credential.type: InheritFromDelegate` and `delegateSelectors: [bankapp-delegate]`.
  3. In both CI stages, replace `platform`/`runtime` with `infrastructure: {type: KubernetesDirect, spec: {connectorRef: bankapp_k8s, namespace: harness-builds, os: Linux}}`.
  4. Set `executeOnDelegate: true` on both connectors.
  5. The OWASP step image runs as a non-root user. On Kubernetes infrastructure, add `runAsUser: "0"` to that step if it cannot write to `/harness`.
- The `notify` Custom stage runs only the Email step. It needs no build infrastructure or delegate.

### Step templates (recommended, for reuse across services)

These steps are currently inline. Promote them to org/account Step templates (Template Library, New Template, Step) so other services get the same controls, just as other jobs reuse the Jenkins `Shared` library:

| Template | Source step | Inputs to expose |
| --- | --- | --- |
| `trivy_fs_scan` | `bankapp-ci.yaml` / `trivy_fs_scan` | image tag, scan path |
| `owasp_dependency_check` | `bankapp-ci.yaml` / `owasp_dependency_check` | project name, NVD secret |
| `sonarqube_analysis` | `bankapp-ci.yaml` / `sonarqube_analysis` | project key/name, host URL, token secret |
| `sonarqube_quality_gate` | `bankapp-ci.yaml` / `sonarqube_quality_gate` | token secret, timeout |
| `gitops_image_bump` | `bankapp-cd.yaml` / `update_k8s_manifest` + `git_commit_and_push` (as a Step Group template) | image name, manifest path, branch, token secret |

`BuildAndPushDockerRegistry` and `Email` are native Harness steps and need no template.

### Importing

1. Create the secrets, connectors and project variable above.
2. Import the pipelines from Git: Pipelines, Create, Import From Git, connector `github_bankapp`, repo `Springboot-BankApp`, branch `DevOps`. Import `.harness/bankapp-cd.yaml` **first**, because the CI pipeline's chained stage references `bankapp_cd`. Then import `.harness/bankapp-ci.yaml`.
3. Create the trigger on `bankapp_ci` from `.harness/triggers/bankapp-ci-push-devops.yaml` (Triggers, New Trigger, YAML view).

## Jenkins shared library functions to port

`@Library('Shared')` loads the external global library. According to [`vars/README.md`](vars/README.md), that is [`DevMadhup/Jenkins_SharedLib`](https://github.com/DevMadhup/Jenkins_SharedLib). This repo's `vars/` directory holds copies, which are identical except for `sonarqube_analysis` (the external version adds `-Dsonar.java.binaries=.`). The Harness port follows the external library.

| Function | Jenkins implementation (external `vars/*.groovy`) | Harness port | Status |
| --- | --- | --- | --- |
| `code_checkout(url, branch)` | `git url: url, branch: branch` | Codebase clone via `github_bankapp` | Ported (native) |
| `trivy_scan()` | `sh "trivy fs ."` | Run step `aquasec/trivy:0.56.2`, `trivy fs .` | Ported |
| `owasp_dependency()` | `dependencyCheck additionalArguments: '--scan ./', odcInstallation: 'OWASP'`; `dependencyCheckPublisher pattern: '**/dependency-check-report.xml'` | Run step `owasp/dependency-check:10.0.4`, `--scan ./ --project bankapp --format XML --out .` plus `--nvdApiKey` | Scan ported. The publisher (trend graphs, build status) has no equivalent; see the gaps below. |
| `sonarqube_analysis(api, name, key)` | `withSonarQubeEnv(api) { $SONAR_HOST/bin/sonar-scanner -Dsonar.projectName=name -Dsonar.projectKey=key -Dsonar.java.binaries=. -X }` | Run step `sonarsource/sonar-scanner-cli:11.1` with the same properties. Host and token come from the `sonar_host_url` variable and the `sonarqube_token` secret. | Ported |
| `sonarqube_code_quality()` | `timeout(1 min) { waitForQualityGate abortPipeline: false }` | Run step polling `api/ce/task` and `api/qualitygates/project_status?analysisId=` with a 5m timeout | Ported. Now blocking (see below). |
| `docker_build(project, tag, user)` | `docker build -t user/project:tag .` | `BuildAndPushDockerRegistry` (`repo: madhupdevops/bankapp`, `dockerfile: Dockerfile`, `context: .`) | Ported (native) |
| `docker_push(project, tag, user)` | `withCredentials(usernamePassword('docker')) { docker login }`; `docker push user/project:tag` | Same `BuildAndPushDockerRegistry` step, authenticated by `dockerhub_bankapp` | Ported (native) |

`docker_cleanup`, `docker_compose`, `buildImage`, `pushImage`, `deploy` and `greet` are not called by either Jenkinsfile, so they were not ported.

## Intentional differences and known gaps

- **The quality gate now blocks.** Jenkins used `waitForQualityGate abortPipeline: false` with a 1-minute timeout, so a failing gate never stopped the build. The Harness step fails the pipeline on any status other than `OK` and allows 5 minutes for the background task.
- **No `dependencyCheckPublisher` or `archiveArtifacts '*.xml'`.** `dependency-check-report.xml` is generated in the stage workspace but not kept. To keep it, add an `Upload Artifacts to S3/GCS/JFrog` step, or use the STO `Owasp` step in ingestion mode.
- **Email:** `attachLog: true` is not supported by the Harness Email step. The body links to the execution instead.
- **Default `DOCKER_TAG`:** Jenkins defaulted to an empty string, so webhook-triggered builds produced `madhupdevops/bankapp:` (invalid). Harness defaults to the commit SHA.
- **Repository:** the Jenkinsfiles clone and push `LondheShubham153/Springboot-BankApp`. Harness uses this repo (`COG-GTM/Springboot-BankApp`) through the connector.
- **The `Dockerfile` runtime base image no longer exists (affects Jenkins too):** `openjdk:17-alpine` has been removed from Docker Hub, so `docker build` fails at `FROM openjdk:17-alpine`. Until the base image is changed (for example to `eclipse-temurin:17-jre-alpine`), `docker_build_and_push` will fail the same way the Jenkins `docker_build` stage does.
- **Image name mismatch (carried over as-is):** CI pushes `madhupdevops/bankapp:<tag>`, but CD writes `trainwithshubham/bankapp-eks:<tag>` into the manifest. Align the two before relying on the GitOps deploy.
- **CI loop guard:** CD pushes to `DevOps`, which would fire the push trigger again. The trigger's `jexlCondition` skips a push only when the head commit is authored by `harness-bankapp-cd@users.noreply.github.com` *and* its message contains `Updated K8s Deployment Docker Image Version`. Git author fields are not authenticated, so if you need a hard guarantee, branch-protect `DevOps` so only the CD token can push directly.
- **Out-of-order deploys:** `autoAbortPreviousExecutions: true` on the trigger aborts an in-flight CI/CD run when a newer `DevOps` push arrives, so an older build cannot bump the manifest after a newer one. Jenkins had no such guard.
- **`git add`:** the CD step stages only `kubernetes/bankapp-deployment.yml` instead of `git add .`.

## Optional STO steps

If the account has Harness STO, the three scanner Run steps can be replaced with native steps that also feed STO dashboards and exemptions:
- `AquaTrivy` (orchestration, target type `repository`) for `trivy_fs_scan`
- `Owasp` (orchestration or ingestion of `dependency-check-report.xml`) for `owasp_dependency_check`
- `Sonarqube` (orchestration, product config `default`, project key `bankapp`) for `sonarqube_analysis`. Keep `sonarqube_quality_gate` or use STO fail-on-severity.

## Parallel-run validation

Both systems push to `DevOps` and react to pushes. While running them side by side:
- Keep the Jenkins webhook and the Harness trigger both enabled to compare scan results and images. Use different tags (Harness uses the commit SHA).
- Let only one CD pipeline push manifest changes at a time. For example, disable the `trigger_bankapp_cd` stage, or point the Harness CD push at a scratch branch until cut-over, so ArgoCD doesn't receive competing image bumps.
- At cut-over, disable the Jenkins jobs and the GitHub webhook to Jenkins, then remove the Jenkinsfiles in a follow-up change.
