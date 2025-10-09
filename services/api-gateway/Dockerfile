FROM openjdk:17-jdk-slim

ARG JAR_FILE=build/libs/*.jar

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY services/api-gateway/${JAR_FILE} app.jar

COPY scripts/docker-entrypoint-wait.sh /usr/local/bin/docker-entrypoint-wait.sh

RUN chmod +x /usr/local/bin/docker-entrypoint-wait.sh

ENTRYPOINT ["docker-entrypoint-wait.sh"]

CMD ["discovery-service", "8761", "/actuator/health", "30", "--", "java", "-jar", "/app.jar"]