# Java 22 Migration Documentation

## Overview

This document describes the migration of the Spring Boot BankApp application from Java 17 to Java 22. The migration includes updates to the build configuration, Docker images, and CI/CD pipeline compatibility.

## Migration Summary

### Changes Made

#### 1. pom.xml Updates
- Updated `java.version` property from `17` to `22`
- Updated `maven-compiler-plugin` version from `3.8.0` to `3.11.0`
- Updated compiler source and target from `1.8` to `22`

#### 2. Dockerfile Updates
- Stage 1 (Build): Changed from `maven:3.8.3-openjdk-17` to `maven:3.9.6-openjdk-22`
- Stage 2 (Runtime): Changed from `openjdk:17-alpine` to `openjdk:22-alpine`

### Dependency Compatibility

The following dependencies have been verified for Java 22 compatibility:

| Dependency | Version | Java 22 Compatible |
|------------|---------|-------------------|
| Spring Boot | 3.3.3 | Yes |
| MySQL Connector | 8.0.33 | Yes |
| Spring Security | (managed by Spring Boot) | Yes |
| Thymeleaf | (managed by Spring Boot) | Yes |

### CI/CD Pipeline Compatibility

#### Jenkins Configuration
- Jenkins continues to run on Java 17 (as required by `openjdk-17-jre`)
- The pipeline uses Docker-based builds, which now use Java 22 images
- No changes required to `Jenkinsfile` or `GitOps/Jenkinsfile`
- Both pipelines will automatically use the updated Docker images with Java 22

#### Docker Compose
- The `docker-compose.yml` configuration remains unchanged
- Health check endpoints (`/actuator/health`) work with Java 22
- JDBC URL configuration is compatible with Java 22

#### Kubernetes Deployment
- The `kubernetes/bankapp-deployment.yml` does not require immediate changes
- Memory limits (1Gi) may need adjustment after Java 22 testing
- Monitor application performance after deployment

## Rollback Procedure

If issues occur during migration, follow these steps:

### Quick Rollback

1. Run the rollback script:
   ```bash
   ./migration-scripts/rollback-java17.sh
   ```

2. Rebuild the application:
   ```bash
   mvn clean package
   ```

3. Rebuild Docker image:
   ```bash
   docker build -t bankapp:java17 .
   ```

4. Redeploy to Kubernetes using Java 17 images

5. Update `migration-scripts/java-versions.properties` to reflect rollback

### Automatic Rollback

Use the auto-rollback script for automated rollback on build failure:
```bash
./migration-scripts/auto-rollback.sh
```

This script will:
- Attempt to build with Java 22
- Automatically rollback to Java 17 if the build fails
- Rebuild with Java 17 configuration

### Validate Rollback Capability

Before deploying to production, validate that rollback works:
```bash
./migration-scripts/validate-rollback.sh
```

## Testing Instructions

### Local Build and Run

1. Build the application:
   ```bash
   mvn clean package
   ```

2. Build Docker image:
   ```bash
   docker build -t bankapp:java22 .
   ```

3. Run with docker-compose:
   ```bash
   docker-compose up
   ```

4. Verify application starts and connects to MySQL 8.0

5. Test all endpoints function correctly:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Security Scanning

1. Run Trivy filesystem scan:
   ```bash
   trivy fs --severity HIGH,CRITICAL .
   ```

2. Run Trivy image scan:
   ```bash
   trivy image bankapp:java22
   ```

3. Execute SonarQube analysis through the Jenkins pipeline

## Migration Scripts

The following scripts are available in the `migration-scripts/` directory:

| Script | Purpose |
|--------|---------|
| `rollback-java17.sh` | Reverts pom.xml and Dockerfile to Java 17 |
| `auto-rollback.sh` | Automatic rollback on build failure |
| `validate-rollback.sh` | Validates rollback capability |
| `java-versions.properties` | Tracks version configurations |
| `backup-pom.xml` | Backup of original pom.xml |
| `backup-Dockerfile` | Backup of original Dockerfile |
| `test-plan.md` | Detailed test plan for migration |

## Version History

| Date | Version | Description |
|------|---------|-------------|
| 2026-01-07 | 1.0.0 | Initial Java 22 migration |

## Notes

- Jenkins remains on Java 17 - it uses Docker images with Java 22 for builds
- Kubernetes deployment files do not require changes for this migration
- Monitor application performance after deployment to production
- Review memory usage and adjust Kubernetes resource limits if needed

## Contact

For questions about this migration, refer to the project documentation or contact the DevOps team.
