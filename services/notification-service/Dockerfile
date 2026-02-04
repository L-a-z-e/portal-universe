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
COPY services/auth-events/build.gradle services/auth-events/
COPY services/shopping-events/build.gradle services/shopping-events/
COPY services/blog-events/build.gradle services/blog-events/
COPY services/prism-events/build.gradle services/prism-events/
COPY services/notification-service/build.gradle services/notification-service/

# ---------------------------------------------
# Layer 2: 의존성 다운로드 (의존성 변경 없으면 캐시)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:notification-service:dependencies --no-daemon || true

# ---------------------------------------------
# Layer 3: 소스 코드 복사 (모든 이벤트 모듈 포함)
# ---------------------------------------------
COPY services/common-library/ services/common-library/
COPY services/auth-events/ services/auth-events/
COPY services/shopping-events/ services/shopping-events/
COPY services/blog-events/ services/blog-events/
COPY services/prism-events/ services/prism-events/
COPY services/notification-service/ services/notification-service/

# ---------------------------------------------
# Layer 4: 빌드 (소스만 변경 시 여기서만 재실행)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:notification-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/notification-service/build/libs/notification-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
