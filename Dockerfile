#----------------------------------
# Stage 1: build
#----------------------------------

# Chainguard JDK: minimal, continuously rebuilt, low-CVE base image
FROM cgr.dev/chainguard/jdk:latest AS builder

LABEL app=bankapp

# Artifact repository used for the build. Override with an internal
# Artifactory/Nexus mirror where builds must not egress to the public internet.
ARG MAVEN_REPO_URL=https://repo.maven.apache.org/maven2
ENV MVNW_REPOURL=${MAVEN_REPO_URL}
ENV MAVEN_USER_HOME=/build/.m2

WORKDIR /build

RUN mkdir -p "${MAVEN_USER_HOME}" && \
    printf '<settings><mirrors><mirror><id>central-mirror</id><name>central-mirror</name><url>%s</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' \
        "${MAVEN_REPO_URL}" > "${MAVEN_USER_HOME}/settings.xml"

COPY --chown=65532:65532 .mvn/ .mvn/
COPY --chown=65532:65532 mvnw pom.xml ./

# Resolve dependencies first so they stay cached across source-only changes
RUN sh ./mvnw -B -ntp -s "${MAVEN_USER_HOME}/settings.xml" dependency:go-offline

COPY --chown=65532:65532 src/ src/

RUN sh ./mvnw -B -ntp -s "${MAVEN_USER_HOME}/settings.xml" clean package -DskipTests=true && \
    cp target/bankapp-*.jar target/bankapp.jar

#--------------------------------------
# Stage 2: runtime
#--------------------------------------

# Chainguard JRE: distroless runtime with no shell or package manager, non-root by default
FROM cgr.dev/chainguard/jre:latest AS runtime

LABEL app=bankapp

WORKDIR /app

COPY --from=builder --chown=65532:65532 /build/target/bankapp.jar /app/bankapp.jar

# Non-root user shipped with the Chainguard image
USER 65532:65532

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/bankapp.jar"]
