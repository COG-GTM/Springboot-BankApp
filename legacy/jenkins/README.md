# Legacy Jenkins Configuration

This directory contains the original Jenkins pipeline configuration files that were used before migrating to GitHub Actions.

## Files

- `Jenkinsfile`: Original CI pipeline with 8 stages including security scanning, code quality, and Docker operations
- `Jenkinsfile-CD`: Original CD pipeline for GitOps deployment and Kubernetes manifest updates
- `vars/`: Shared library functions used by the Jenkins pipelines

## Migration Notes

These files have been preserved for reference and historical purposes. The equivalent functionality is now implemented in GitHub Actions workflows:

- `.github/workflows/ci.yml`: Replaces the Jenkins CI pipeline
- `.github/workflows/cd.yml`: Replaces the Jenkins CD pipeline

## Shared Library Functions

The Jenkins shared library functions have been replaced with equivalent GitHub Actions steps:

| Jenkins Function | GitHub Actions Equivalent |
|------------------|---------------------------|
| `code_checkout()` | `actions/checkout@v4` |
| `trivy_scan()` | Trivy CLI installation and execution |
| `owasp_dependency()` | `dependency-check/Dependency-Check_Action@main` |
| `sonarqube_analysis()` | SonarQube Scanner CLI |
| `sonarqube_code_quality()` | `sonarqube-quality-gate-action@master` |
| `docker_build()` | Docker CLI commands |
| `docker_push()` | `docker/login-action@v3` + Docker CLI |

## Credentials Migration

Jenkins credentials have been migrated to GitHub Actions secrets:

- Jenkins `docker` credential → `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN`
- Jenkins `Github-cred` credential → Built-in `GITHUB_TOKEN`
- Jenkins SonarQube configuration → `SONAR_TOKEN` + `SONAR_HOST_URL`
- Jenkins email configuration → Email secrets in GitHub Actions
