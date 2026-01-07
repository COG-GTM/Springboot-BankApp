# Java 22 Migration Test Plan

## Overview
This document outlines the testing strategy for validating the Java 22 migration of the Spring Boot BankApp application.

## Test Categories

### 1. Local Build and Run Tests
- Verify Maven build completes successfully with `mvn clean package`
- Confirm no compilation errors with Java 22
- Validate all unit tests pass
- Check for any deprecation warnings

### 2. Docker Image Build Tests
- Build Docker image: `docker build -t bankapp:java22 .`
- Verify image builds without errors
- Confirm image uses correct base images (maven:3.9.6-openjdk-22 and openjdk:22-alpine)
- Check image size is reasonable

### 3. Database Connectivity Tests
- Start MySQL 8.0 container
- Run application with docker-compose
- Verify JDBC connection to MySQL works
- Test database operations (CRUD)

### 4. Health Check Endpoint Tests
- Verify `/actuator/health` endpoint responds
- Check application startup time
- Validate all Spring Boot actuator endpoints

### 5. Security Scanning Tests
- Run Trivy filesystem scan: `trivy fs --severity HIGH,CRITICAL .`
- Run Trivy image scan: `trivy image bankapp:java22`
- Execute SonarQube analysis
- Review any new vulnerabilities introduced

### 6. Functional Tests
- Test user registration flow
- Test login/logout functionality
- Test banking operations
- Verify all REST endpoints

### 7. Performance Tests
- Compare startup time with Java 17 baseline
- Monitor memory usage
- Check for any performance regressions

## Test Execution Steps

1. **Pre-migration baseline**
   ```bash
   mvn clean package
   docker build -t bankapp:java17-baseline .
   ```

2. **Post-migration validation**
   ```bash
   mvn clean package
   docker build -t bankapp:java22 .
   docker-compose up -d
   curl http://localhost:8080/actuator/health
   ```

3. **Rollback verification**
   ```bash
   ./migration-scripts/validate-rollback.sh
   ```

## Success Criteria
- All unit tests pass
- Docker image builds successfully
- Application starts and connects to MySQL
- Health check endpoint returns healthy status
- No HIGH or CRITICAL vulnerabilities introduced
- Performance is comparable to Java 17 baseline

## Rollback Trigger Conditions
- Build failures
- Test failures
- Critical security vulnerabilities
- Performance degradation > 20%
- Database connectivity issues
