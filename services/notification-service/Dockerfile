# =================================================================
# Stage 1: Build Stage
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:notification-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/notification-service/build/libs/notification-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
