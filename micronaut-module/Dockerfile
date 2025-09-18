FROM openjdk:17-jdk-slim

WORKDIR /app

COPY micronaut-module/target/micronaut-module-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
