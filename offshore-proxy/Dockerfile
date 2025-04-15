FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/offshore-proxy-1.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]