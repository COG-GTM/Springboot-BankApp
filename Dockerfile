#----------------------------------
# Stage 1
#----------------------------------

# Import docker image with maven installed (maintained Temurin-based image)
FROM maven:3.9.9-eclipse-temurin-17 AS builder

# Add labels to the image to filter out if we have multiple application running
LABEL app=bankapp
LABEL maintainer="madhuppandey2908@gmail.com"

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build application and skip test cases
RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2
#--------------------------------------

# Import small, maintained and patched JRE image
FROM eclipse-temurin:17-jre-alpine AS deployer

# Create a dedicated non-root user/group to run the application
RUN addgroup -S bankapp && adduser -S -G bankapp bankapp

WORKDIR /app

# Copy build from stage 1 (builder)
COPY --from=builder /src/target/*.jar /app/bankapp.jar

# Run as the non-root user
USER bankapp

# Expose application port 
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/app/bankapp.jar"]
