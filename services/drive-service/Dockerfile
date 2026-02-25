# =================================================================
# Stage 1: Build Stage
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# Layer 1: dependency files only (layer cache)
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gradlew ./
COPY services/common-library/build.gradle services/common-library/
COPY services/event-contracts/build.gradle services/event-contracts/
COPY services/drive-service/build.gradle services/drive-service/

# Layer 2: download dependencies (cached if unchanged)
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:drive-service:dependencies --no-daemon || true

# Layer 3: source code
COPY services/common-library/ services/common-library/
COPY services/event-contracts/ services/event-contracts/
COPY services/drive-service/ services/drive-service/

# Layer 4: build
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:drive-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/drive-service/build/libs/drive-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
