#----------------------------------
# Stage 1: Build the Quarkus application
#----------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL app=bankapp

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build the Quarkus application (skip tests)
RUN mvn clean package -DskipTests

#--------------------------------------
# Stage 2: Run the Quarkus application
#--------------------------------------
FROM eclipse-temurin:17-jre-alpine AS deployer

WORKDIR /app

# Copy the Quarkus fast-jar layout from the builder stage
COPY --from=builder /src/target/quarkus-app/ /app/

# Expose application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
