# Required GitHub Actions Secrets

The following secrets must be configured in the repository settings:

## Docker Hub
- `DOCKERHUB_USERNAME`: Docker Hub username (madhupdevops)
- `DOCKERHUB_TOKEN`: Docker Hub access token

## SonarQube
- `SONAR_TOKEN`: SonarQube authentication token
- `SONAR_HOST_URL`: SonarQube server URL

## Email Notifications
- `EMAIL_USERNAME`: SMTP username for notifications
- `EMAIL_PASSWORD`: SMTP password for notifications  
- `EMAIL_TO`: Recipient email address
- `EMAIL_FROM`: Sender email address

## Setup Instructions
1. Go to repository Settings > Secrets and variables > Actions
2. Add each secret with the corresponding value
3. Ensure the GitHub token has write permissions for repository dispatch

## Migration Notes
These secrets replace the Jenkins credentials that were previously used:
- Jenkins `docker` credential → `DOCKERHUB_USERNAME` + `DOCKERHUB_TOKEN`
- Jenkins `Github-cred` credential → Built-in `GITHUB_TOKEN`
- Jenkins SonarQube configuration → `SONAR_TOKEN` + `SONAR_HOST_URL`
- Jenkins email configuration → Email secrets above
