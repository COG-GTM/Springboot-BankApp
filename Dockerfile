#----------------------------------
# Stage 1: Build
#----------------------------------

FROM maven:3.9-eclipse-temurin-25 AS builder

LABEL app=bankapp

WORKDIR /src

COPY . /src

RUN mvn clean install -DskipTests=true

#--------------------------------------
# Stage 2: Runtime
#--------------------------------------

FROM eclipse-temurin:25-jre-alpine AS deployer

COPY --from=builder /src/target/*.jar /src/target/bankapp.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/src/target/bankapp.jar"]
