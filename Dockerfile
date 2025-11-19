#----------------------------------
# Stage 1
#----------------------------------

# Import docker image with Java 22 JDK
FROM eclipse-temurin:22-jdk-alpine as builder

# Add maintainer, so that new user will understand who had written this Dockerfile
MAINTAINER Madhup Pandey<madhuppandey2908@gmail.com>

# Add labels to the image to filter out if we have multiple application running
LABEL app=bankapp

# Install Maven
ARG MAVEN_VERSION=3.9.11
ARG USER_HOME_DIR="/root"
ARG BASE_URL=https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN apk add --no-cache curl tar bash \
    && mkdir -p /usr/share/maven /usr/share/maven/ref \
    && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
    && rm -f /tmp/apache-maven.tar.gz \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

# Set working directory
WORKDIR /src

# Copy source code from local to container
COPY . /src

# Build application and skip test cases
RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2
#--------------------------------------

# Import small size java image
FROM eclipse-temurin:22-jre-alpine as deployer

# Copy build from stage 1 (builder)
COPY --from=builder /src/target/*.jar /src/target/bankapp.jar

# Expose application port 
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "/src/target/bankapp.jar"]
