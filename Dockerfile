FROM eclipse-temurin:26
LABEL authors="gimen"
WORKDIR /app
COPY target/pbl1-redes-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]