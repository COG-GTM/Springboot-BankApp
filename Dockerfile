#----------------------------------
# Stage 1: Build
#----------------------------------

# Security: pin base image to SHA digest to prevent supply-chain attacks via tag mutation
FROM maven:3.8.3-openjdk-17@sha256:8a66581a077762c8752a9f64f73cdd8c59e9c4446eb810417119e0436b075931 AS builder

LABEL app=bankapp

WORKDIR /src

COPY . /src

RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2: Runtime
#--------------------------------------

# Security: replaced deprecated openjdk:17-alpine with actively maintained eclipse-temurin;
#           pinned to SHA digest for reproducible builds
FROM eclipse-temurin:17-jre-alpine@sha256:02320dd4ce20e243dfb915c686089cf9315c763084fafbb12d5c9993aee18b57

# Security: remove unnecessary OS packages and caches from final image
RUN apk --no-cache upgrade \
 && rm -rf /var/cache/apk/*

# Security: create a non-root user to run the application (CIS Docker Benchmark 4.1)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder --chown=appuser:appgroup /src/target/*.jar /app/bankapp.jar

EXPOSE 8080

# Security: run as non-root user to limit blast radius of container compromise
USER appuser

ENTRYPOINT ["java", "-jar", "/app/bankapp.jar"]
