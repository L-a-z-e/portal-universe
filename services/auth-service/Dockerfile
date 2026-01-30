# =================================================================
# Stage 1: Build Stage
# 역할: Java 소스 코드를 컴파일하고 실행 가능한 JAR 파일을 생성합니다.
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

COPY . .

# Gradle 캐시 마운트로 의존성 재다운로드 방지
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
--mount=type=cache,target=/home/gradle/.gradle/wrapper \
./gradlew :services:auth-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# 역할: 빌드된 JAR 파일을 최소한의 환경에서 실행합니다.
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 비루트 사용자 생성 및 전환
RUN addgroup --system appgroup && adduser --system -ingroup appgroup appuser

# Build Stage에서 생성된 JAR 파일을 Runtime Stage로 복사합니다.
COPY --from=builder /app/services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

# 비루트 사용자로 전환
USER appuser

# 컨테이너 시작 시 JAR 파일을 직접 실행합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]