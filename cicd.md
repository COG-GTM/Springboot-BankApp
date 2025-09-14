#### CICD Workflow (GitHub Actions)
- Cloning the Project code from GitHub using actions/checkout@v4
- Security scanning with Trivy and OWASP dependency check
- Code quality analysis with SonarQube
- Build docker image and push it to docker hub
- GitOps deployment by updating Kubernetes manifests
- Email notifications for deployment status

    ![Login diagram](images/flow.png)

## Migration from Jenkins to GitHub Actions

This repository has been migrated from Jenkins-based CI/CD to GitHub Actions workflows. The legacy Jenkins configuration files are preserved in the `legacy/jenkins/` directory for reference.

### Jenkins to GitHub Actions Mapping

| Jenkins Function | GitHub Actions Equivalent |
|------------------|---------------------------|
| `code_checkout()` | `actions/checkout@v4` |
| `trivy_scan()` | Trivy CLI installation and execution |
| `owasp_dependency()` | `dependency-check/Dependency-Check_Action@main` |
| `sonarqube_analysis()` | SonarQube Scanner CLI |
| `sonarqube_code_quality()` | `sonarqube-quality-gate-action@master` |
| `docker_build()` | Docker CLI commands |
| `docker_push()` | `docker/login-action@v3` + Docker CLI |

#### Creating CICD pipeline (GitHub Actions)

 1. #### Configure GitHub Actions (Replaces Jenkins)
    - No server installation required
    - Configure repository secrets in GitHub repository settings
    - Access workflows at: `https://github.com/COG-GTM/Springboot-BankApp/actions`
    - Workflows automatically trigger on code changes

2. #### GitHub Actions Configuration.
    - **Runners**: GitHub-hosted runners (ubuntu-latest) provide isolated execution environment
    - **Security**: Built-in Docker support with secure credential management
    - **Secrets Management**: 
        - Configure repository secrets in Settings > Secrets and variables > Actions
        - Required secrets documented in `.github/SECRETS.md`
        - Includes Docker Hub, SonarQube, and email notification credentials
    - **Workflow Triggers**:
        - Automatic triggers on push/PR to DevOps/main branches
        - Manual triggers via workflow dispatch
        - Repository dispatch for CI/CD pipeline coordination

3. #### GitHub Actions Workflows and Triggers
 
    - **CI Workflow** (`.github/workflows/ci.yml`):
        - Triggers on push/PR to DevOps/main branches
        - Manual trigger via workflow dispatch with custom Docker tags
        - Includes all security scanning, code quality, and Docker operations
        
    - **CD Workflow** (`.github/workflows/cd.yml`):
        - Triggers via repository dispatch from CI workflow
        - Manual trigger for specific Docker tag deployments
        - Updates Kubernetes manifests and sends notifications
        
    - **Automatic Triggers**:
        - No webhook configuration needed - GitHub Actions triggers automatically
        - Built-in integration with GitHub repository events
        - Secure and immediate execution without polling delays
        
    - **Manual Execution**:
        ```bash
        # Trigger CI workflow manually
        gh workflow run ci.yml --ref DevOps -f docker_tag=manual-v1.0
        
        # Trigger CD workflow manually  
        gh workflow run cd.yml --ref DevOps -f docker_tag=manual-v1.0
        ```
        
    - **Monitoring**:
        - View workflow runs at: `https://github.com/COG-GTM/Springboot-BankApp/actions`
        - Real-time logs and status updates
        - Email notifications for deployment status












#### Nginx and HTTPS [guide](nginx.md)
