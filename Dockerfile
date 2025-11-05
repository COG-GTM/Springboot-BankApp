#----------------------------------
# Stage 1
#----------------------------------

# Import docker image with maven installed
FROM maven:3.8-eclipse-temurin-18 as builder

# Add maintainer, so that new user will understand who had written this Dockerfile
MAINTAINER Madhup Pandey<madhuppandey2908@gmail.com>

# Add labels to the image to filter out if we have multiple application running
LABEL app=bankapp

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build application with tests
RUN mvn clean install

#--------------------------------------
# Stage 2
#--------------------------------------

# Import small size java image
FROM eclipse-temurin:18-jre-alpine as deployer

# Copy build from stage 1 (builder)
COPY --from=builder /src/target/*.jar /src/target/bankapp.jar

# Expose application port 
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/src/target/bankapp.jar"]
