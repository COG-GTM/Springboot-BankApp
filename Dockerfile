#----------------------------------
# Stage 1
#----------------------------------

# Import docker image with maven installed
FROM maven:3.8.3-openjdk-17 as builder 

# Add labels to the image to filter out if we have multiple application running
LABEL app=bankapp
LABEL maintainer="Madhup Pandey <madhuppandey2908@gmail.com>"

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build application and skip test cases
RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2
#--------------------------------------

# Import small, maintained JRE image (openjdk:17-alpine is deprecated/unmaintained)
FROM eclipse-temurin:17-jre-alpine as deployer

# Create an unprivileged user so the container does not run as root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy build from stage 1 (builder)
COPY --from=builder /src/target/*.jar /src/target/bankapp.jar

# Drop root privileges
USER appuser

# Expose application port 
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/src/target/bankapp.jar"]
