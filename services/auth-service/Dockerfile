# =================================================================
# Stage 1: Build Stage
# 역할: Java 소스 코드를 컴파일하고 실행 가능한 JAR 파일을 생성합니다.
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# build.gradle, settings.gradle을 먼저 복사하여 Gradle 종속성을 별도 레이어에 캐싱합니다.
# 소스 코드가 변경되어도 종속성은 다시 다운로드하지 않도록 최적화합니다.
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# 전체 소스 코드 복사
COPY . .

# Gradle 빌드 실행. 테스트는 제외하여 빌드 속도를 높입니다.
RUN ./gradlew :services:auth-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# 역할: 빌드된 JAR 파일을 최소한의 환경에서 실행합니다.
# =================================================================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# docker-entrypoint-wait.sh 스크립트가 의존하는 'curl'을 설치합니다.
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 다른 서비스가 준비될 때까지 대기하는 스크립트를 복사하고 실행 권한을 부여합니다.
COPY scripts/docker-entrypoint-wait.sh /usr/local/bin/docker-entrypoint-wait.sh
RUN chmod +x /usr/local/bin/docker-entrypoint-wait.sh

# Build Stage에서 생성된 JAR 파일을 Runtime Stage로 복사합니다.
COPY --from=builder /app/services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

# JAR 파일에 실행 권한을 부여합니다.
RUN chmod +x app.jar

# 컨테이너 시작 시 대기 스크립트를 실행합니다.
ENTRYPOINT ["docker-entrypoint-wait.sh"]

# 대기 스크립트에 전달될 인자들:
# 1. 대기할 서비스 호스트: config-service
# 2. 대기할 서비스 포트: 8888
# 3. 헬스체크 경로: /actuator/health
# 4. 타임아웃(초): 30
# 5. (--) 이후: 실제 실행할 명령어 (java -jar app.jar)
CMD ["config-service", "8888", "/actuator/health", "30", "--", "java", "-jar", "app.jar"]
