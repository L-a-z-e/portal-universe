# =================================================================
# Stage 1: Build Stage
# 역할: Java 소스 코드를 컴파일하고 실행 가능한 JAR 파일을 생성합니다.
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# build.gradle, settings.gradle을 먼저 복사하여 Gradle 종속성을 별도 레이어에 캐싱합니다.
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# 전체 소스 코드 복사
COPY . .

# Gradle 빌드 실행. 테스트는 제외하여 빌드 속도를 높입니다.
RUN ./gradlew :services:discovery-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# 역할: 빌드된 JAR 파일을 최소한의 환경에서 실행합니다.
# =================================================================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Health check 등에 사용될 수 있는 curl을 설치합니다.
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Build Stage에서 생성된 JAR 파일을 Runtime Stage로 복사합니다.
COPY --from=builder /app/services/discovery-service/build/libs/discovery-service-0.0.1-SNAPSHOT.jar app.jar

# JAR 파일에 실행 권한을 부여합니다.
RUN chmod +x app.jar

# 컨테이너 시작 시 JAR 파일을 직접 실행합니다.
# Discovery Service는 다른 서비스들이 의존하는 최상위 서비스이므로, 다른 서비스의 시작을 기다릴 필요가 없습니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
