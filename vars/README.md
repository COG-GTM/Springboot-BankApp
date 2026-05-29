# Jenkins Shared Library Module

> Reusable Groovy functions for Jenkins pipelines, encapsulating CI/CD operations including code checkout, security scanning, Docker operations, and deployment.

## Overview

This directory implements a [Jenkins Shared Library](https://www.jenkins.io/doc/book/pipeline/shared-libraries/) under the `vars/` convention. Each `.groovy` file defines a single callable function that can be invoked from any Jenkins pipeline using `@Library('Shared') _`.

## Architecture

![Shared Library Dependency Graph](../docs/infrastructure/diagrams/shared-library-deps.png)

## Function Catalog

### Source Control

| Function | File | Parameters | Description |
|----------|------|------------|-------------|
| `code_checkout` | `code_checkout.groovy` | `GitUrl`, `GitBranch` | Simple git checkout by URL and branch |
| `codeCheckout` | `codeCheckout.groovy` | `branch`, `url`, `credId` | Enhanced checkout with input validation, timeout (5 min), and error handling |

### Security Scanning

| Function | File | Parameters | Description |
|----------|------|------------|-------------|
| `trivy_scan` | `trivy_scan.groovy` | *(none)* | Runs Trivy filesystem scan on the workspace |
| `owasp_dependency` | `owasp_dependency.groovy` | *(none)* | Runs OWASP Dependency-Check and publishes XML report |
| `sonarqube_analysis` | `sonarqube_analysis.groovy` | `SonarQubeAPI`, `Projectname`, `ProjectKey` | Runs SonarQube Scanner with the given project configuration |
| `sonarqube_code_quality` | `sonarqube_code_quality.groovy` | *(none)* | Waits for SonarQube Quality Gate result (1 min timeout) |

### Docker Operations

| Function | File | Parameters | Description |
|----------|------|------------|-------------|
| `docker_build` | `docker_build.groovy` | `ProjectName`, `ImageTag`, `DockerHubUser` | Builds Docker image as `user/project:tag` |
| `docker_push` | `docker_push.groovy` | `Project`, `ImageTag`, `dockerhubuser` | Authenticates to DockerHub and pushes image |
| `docker_cleanup` | `docker_cleanup.groovy` | `Project`, `ImageTag`, `DockerHubUser` | Removes local Docker image |
| `buildImage` | `buildImage.groovy` | `imageName` | Validates image name and builds with Docker daemon check |
| `pushImage` | `pushImage.groovy` | `imageName` | Tags and pushes image using `dockerhub` credential ID |

### Deployment

| Function | File | Parameters | Description |
|----------|------|------------|-------------|
| `deploy` | `deploy.groovy` | *(none)* | Full Docker Compose deployment with health check polling (30 attempts, 10s interval) |
| `docker_compose` | `docker_compose.groovy` | *(none)* | Simple `docker-compose down && up -d` |

### Utility

| Function | File | Parameters | Description |
|----------|------|------------|-------------|
| `greet` | `greet.groovy` | `name` | Sanitized greeting message (strips special characters) |

## Required Jenkins Credentials

| Credential ID | Type | Used By |
|---------------|------|---------|
| `docker` | Username/Password | `docker_push` |
| `dockerhub` | Username/Password | `pushImage` |

## Required Jenkins Tools

| Tool Name | Type | Used By |
|-----------|------|---------|
| `Sonar` | SonarQube Scanner | `sonarqube_analysis` |
| `OWASP` | OWASP Dependency-Check | `owasp_dependency` |

## Setup

1. Navigate to **Manage Jenkins → System → Global Trusted Pipeline Libraries**
2. Add a new library:
   - **Name:** `Shared`
   - **Default version:** branch name (e.g., `DevOps`)
   - **Retrieval method:** Modern SCM → Git
   - **Project repository:** repository URL containing this `vars/` directory
3. Use in any pipeline:

```groovy
@Library('Shared') _
pipeline {
    stages {
        stage('Checkout') {
            steps {
                script {
                    code_checkout("https://github.com/org/repo.git", "main")
                }
            }
        }
    }
}
```
