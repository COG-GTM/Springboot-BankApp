# GitOps Module — Continuous Delivery Pipeline

> Jenkins CD pipeline that implements GitOps by updating Kubernetes manifests with new Docker image tags and pushing changes to Git for ArgoCD sync.

## Overview

This module contains the **BankApp-CD** Jenkins pipeline. It is automatically triggered by the CI pipeline (`Jenkinsfile` at repo root) after a successful build and image push. The CD pipeline updates the Kubernetes deployment manifest with the new Docker image tag and pushes the change to Git, which ArgoCD then detects and syncs to the EKS cluster.

## Architecture

![GitOps Pipeline Flow](../docs/infrastructure/diagrams/gitops-pipeline-flow.png)

## Pipeline Stages

| # | Stage | Description |
|---|-------|-------------|
| 1 | Workspace cleanup | Clears the Jenkins workspace via `cleanWs()` |
| 2 | Git: Code Checkout | Clones the `DevOps` branch from the source repository |
| 3 | Verify: Docker Image Tags | Logs the received `DOCKER_TAG` parameter for audit trail |
| 4 | Update: Kubernetes manifest | Uses `sed` to replace the image tag in `kubernetes/bankapp-deployment.yaml` |
| 5 | Git: Code update and push | Commits and pushes the updated manifest back to the `DevOps` branch |

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `DOCKER_TAG` | String | `''` | Docker image tag built by the CI job (passed automatically) |

## Required Jenkins Credentials

| Credential ID | Type | Purpose |
|---------------|------|---------|
| `Github-cred` | Username/Password | Git push authentication (use PAT as password) |

## Email Notification

The pipeline sends an HTML email notification after every run (success or failure):

| Setting | Value |
|---------|-------|
| From | `trainwithshubham@gmail.com` |
| To | `trainwithshubham@gmail.com` |
| Content | Job name, build number, build URL |
| SMTP Port | 465 (SMTPS) |

## Integration Points

```
CI Pipeline (Jenkinsfile) ──triggers──▶ CD Pipeline (GitOps/Jenkinsfile)
                                              │
                                              ▼
                                     Git Push (updated manifest)
                                              │
                                              ▼
                                     ArgoCD detects change
                                              │
                                              ▼
                                     EKS cluster synced
```

## Prerequisites

- Jenkins with `Pipeline` and `Git` plugins
- Jenkins shared library `Shared` configured (see `vars/README.md`)
- GitHub credentials stored in Jenkins credential store
- ArgoCD configured to watch the repository's `kubernetes/` directory
- SMTP configured in Jenkins for email notifications
