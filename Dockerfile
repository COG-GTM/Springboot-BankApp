#----------------------------------
# Stage 1: Build
#----------------------------------

# Maintained, pinned Alpine-based Maven + Eclipse Temurin JDK 17 builder.
# Replaces the deprecated, EOL `maven:3.8.3-openjdk-17` (Oracle Linux 8.5)
# base image which carried 500+ known OS-package CVEs.
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

LABEL app=bankapp
LABEL org.opencontainers.image.source="https://github.com/COG-GTM/Springboot-BankApp"

WORKDIR /src

# Resolve dependencies first so they are cached independently of source changes.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Copy source code and build the application (tests run in CI separately).
COPY . /src
RUN mvn -B clean install -DskipTests=true

#--------------------------------------
# Stage 2: Runtime
#--------------------------------------

# Minimal, maintained JRE-only runtime. Replaces the deprecated/unpullable
# `openjdk:17-alpine` base image (the `openjdk` Docker Hub repo is EOL).
FROM eclipse-temurin:17-jre-alpine AS deployer

# Create a dedicated non-root user/group (high UID to avoid host UID clashes).
RUN addgroup -g 10001 -S appgroup \
 && adduser -u 10001 -S appuser -G appgroup

WORKDIR /app

# Copy the built artifact and hand ownership to the non-root user.
COPY --from=builder --chown=appuser:appgroup /src/target/*.jar /app/bankapp.jar

# Drop privileges: run as the non-root user.
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/bankapp.jar"]
