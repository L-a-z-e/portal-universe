FROM openjdk:17-jdk-slim
ARG JAR_FILE=build/libs/*.jar
COPY services/blog-service/${JAR_FILE} app.jar
RUN apt-get update && apt-get install -y netcat-traditional && rm -rf /var/lib/apt/lists/*
COPY scripts/docker-entrypoint-wait.sh /usr/local/bin/docker-entrypoint-wait.sh
RUN chmod +x /usr/local/bin/docker-entrypoint-wait.sh
ENTRYPOINT ["docker-entrypoint-wait.sh", "discovery-service", "8761", "30", "java", "-jar", "/app.jar"]
