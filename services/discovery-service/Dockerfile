# ===============================================================
# 1단계: Builder
# ===============================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle

RUN ./gradlew dependencies --no-daemon

COPY . .

RUN ./gradlew :services:discovery-service:build --no-daemon -x test
# ===============================================================
# 2단계: Runner
# ===============================================================
FROM openjdk:17-jdk-slim

WORKDIR /app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/services/discovery-service/build/libs/discovery-service-0.0.1-SNAPSHOT.jar app.jar

RUN chmod +x app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]