#----------------------------------
# Stage 1
#----------------------------------

# Import docker image with maven installed (Java 11 for migration)
FROM maven:3.8.8-eclipse-temurin-11 as builder

# Add maintainer, so that new user will understand who had written this Dockerfile
MAINTAINER Madhup Pandey<madhuppandey2908@gmail.com>

# Add labels to the image to filter out if we have multiple application running
LABEL app=bankapp

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build application and skip test cases
RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2
#--------------------------------------

# Import small size java image (Java 11 for migration)
FROM eclipse-temurin:11-jre-alpine as deployer

# Copy build from stage 1 (builder)
COPY --from=builder /src/target/*.jar /src/target/bankapp.jar

# Expose application port 
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/src/target/bankapp.jar"]
