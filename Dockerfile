#----------------------------------
# Stage 1: Build the Quarkus application
#----------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL app=bankapp

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build the Quarkus application (fast-jar) and skip test cases
RUN mvn clean package -DskipTests

#--------------------------------------
# Stage 2: Runtime image
#--------------------------------------
FROM eclipse-temurin:17-jre-alpine AS deployer

WORKDIR /deployments

# Copy the Quarkus fast-jar layout from the builder stage
COPY --from=builder /src/target/quarkus-app/ /deployments/

# Expose application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
