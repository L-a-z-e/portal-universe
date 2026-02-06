# =================================================================
# Stage 1: Build Stage
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# ---------------------------------------------
# Layer 1: 의존성 파일만 복사 (Layer Cache 활용)
# ---------------------------------------------
COPY build.gradle settings.gradle ./
COPY gradle/ gradle/
COPY gradlew ./
COPY services/common-library/build.gradle services/common-library/
COPY services/shopping-events/build.gradle services/shopping-events/
COPY services/shopping-service/build.gradle services/shopping-service/

# ---------------------------------------------
# Layer 2: 의존성 다운로드 (의존성 변경 없으면 캐시)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:shopping-service:dependencies --no-daemon || true

# ---------------------------------------------
# Layer 3: 소스 코드 복사 (common-library, shopping-events 포함)
# ---------------------------------------------
COPY services/common-library/ services/common-library/
COPY services/shopping-events/ services/shopping-events/
COPY services/shopping-service/ services/shopping-service/

# ---------------------------------------------
# Layer 4: 빌드 (소스만 변경 시 여기서만 재실행)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:shopping-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/shopping-service/build/libs/shopping-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
