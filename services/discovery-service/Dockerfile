FROM openjdk:17-jdk-slim

ARG JAR_FILE=build/libs/*.jar

COPY services/discovery-service/${JAR_FILE} app.jar

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java","-jar","/app.jar"]
