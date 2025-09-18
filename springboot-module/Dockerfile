FROM openjdk:17-jdk-slim

WORKDIR /app

COPY springboot-module/target/springboot-module-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
