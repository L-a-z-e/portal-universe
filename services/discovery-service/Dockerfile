FROM openjdk:17-jdk-slim
ARG JAR_FILE=build/libs/*.jar
COPY services/discovery-service/${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
