# =================================================================
# Stage 1: Build Stage
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:blog-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/blog-service/build/libs/blog-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
