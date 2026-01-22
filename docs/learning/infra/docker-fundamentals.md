# Docker Fundamentals

컨테이너 기술의 기초와 Docker의 핵심 개념을 학습합니다.

---

## 1. Container vs Virtual Machine

### Virtual Machine (VM)
```
┌─────────────────────────────────────────────┐
│              Application                     │
├─────────────────────────────────────────────┤
│           Guest OS (Full OS)                 │
├─────────────────────────────────────────────┤
│              Hypervisor                      │
├─────────────────────────────────────────────┤
│              Host OS                         │
├─────────────────────────────────────────────┤
│              Hardware                        │
└─────────────────────────────────────────────┘
```

### Container
```
┌─────────────────────────────────────────────┐
│              Application                     │
├─────────────────────────────────────────────┤
│         Container Runtime (Docker)           │
├─────────────────────────────────────────────┤
│              Host OS                         │
├─────────────────────────────────────────────┤
│              Hardware                        │
└─────────────────────────────────────────────┘
```

| 특성 | Virtual Machine | Container |
|------|----------------|-----------|
| 시작 시간 | 분 단위 | 초 단위 |
| 리소스 사용량 | 높음 (GB) | 낮음 (MB) |
| 격리 수준 | 완전 격리 | 프로세스 격리 |
| 이식성 | 낮음 | 높음 |
| OS | 독립적인 Full OS | Host Kernel 공유 |

---

## 2. Docker Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Docker Client                          │
│                    (docker build, run, pull)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │ REST API
┌─────────────────────────▼───────────────────────────────────┐
│                       Docker Daemon (dockerd)                │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │   Images     │  Containers  │     Networks/Volumes     │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     Container Runtime                        │
│                      (containerd, runc)                      │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 구성 요소

| 구성 요소 | 역할 |
|----------|------|
| Docker Client | 사용자 명령을 Docker Daemon에 전달 |
| Docker Daemon | 이미지, 컨테이너, 네트워크 관리 |
| Docker Registry | 이미지 저장소 (Docker Hub, ECR 등) |
| Container Runtime | 실제 컨테이너 실행 담당 |

---

## 3. Docker Image

### Image Layer 구조

```
┌─────────────────────────────────────────┐
│  Layer 4: COPY app.jar (Writable)       │  ← 애플리케이션 코드
├─────────────────────────────────────────┤
│  Layer 3: RUN ./gradlew build           │  ← 빌드 결과물
├─────────────────────────────────────────┤
│  Layer 2: COPY build.gradle             │  ← 의존성 정의
├─────────────────────────────────────────┤
│  Layer 1: FROM eclipse-temurin:17       │  ← Base Image
└─────────────────────────────────────────┘
```

### Layer Caching 원리

```dockerfile
# Bad: 코드 변경 시 모든 레이어 재빌드
COPY . .
RUN ./gradlew build

# Good: 의존성 캐싱 활용
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies
COPY . .
RUN ./gradlew build
```

**캐싱 규칙:**
- 이전 레이어가 변경되면 이후 모든 레이어 무효화
- `ADD`/`COPY`는 파일 내용 변경 시 캐시 무효화
- `RUN` 명령어는 명령어 문자열이 동일하면 캐시 사용

---

## 4. Dockerfile 작성

### Portal Universe auth-service Dockerfile 분석

```dockerfile
# =================================================================
# Stage 1: Build Stage
# 역할: Java 소스 코드를 컴파일하고 실행 가능한 JAR 파일을 생성
# =================================================================
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# build.gradle, settings.gradle을 먼저 복사하여
# Gradle 종속성을 별도 레이어에 캐싱
COPY build.gradle settings.gradle ./
COPY gradlew ./gradlew
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# 전체 소스 코드 복사
COPY . .

# Gradle 빌드 실행 (테스트 제외)
RUN ./gradlew :services:auth-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage
# 역할: 빌드된 JAR 파일을 최소한의 환경에서 실행
# =================================================================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Build Stage에서 생성된 JAR 파일만 복사
COPY --from=builder /app/services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

# 컨테이너 시작 시 JAR 파일 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Multi-Stage Build의 장점

| 단계 | 이미지 크기 | 포함 내용 |
|------|-----------|----------|
| Build Stage | ~1.5GB | JDK, Gradle, 소스코드, 빌드 도구 |
| Runtime Stage | ~300MB | JRE, JAR 파일만 |

---

## 5. Dockerfile Instructions

### 주요 명령어

| 명령어 | 용도 | 예시 |
|--------|------|------|
| `FROM` | Base Image 지정 | `FROM eclipse-temurin:17-jdk` |
| `WORKDIR` | 작업 디렉토리 설정 | `WORKDIR /app` |
| `COPY` | 파일/디렉토리 복사 | `COPY . .` |
| `RUN` | 빌드 시 명령 실행 | `RUN apt-get update` |
| `ENV` | 환경 변수 설정 | `ENV JAVA_OPTS="-Xmx512m"` |
| `EXPOSE` | 포트 문서화 | `EXPOSE 8080` |
| `ENTRYPOINT` | 컨테이너 시작 명령 | `ENTRYPOINT ["java", "-jar"]` |
| `CMD` | 기본 실행 인자 | `CMD ["app.jar"]` |

### ENTRYPOINT vs CMD

```dockerfile
# ENTRYPOINT: 항상 실행되는 명령
# CMD: 기본 인자 (docker run 시 덮어쓰기 가능)

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=local"]

# docker run myapp                     → java -jar app.jar --spring.profiles.active=local
# docker run myapp --spring.profiles.active=docker → java -jar app.jar --spring.profiles.active=docker
```

---

## 6. Image Build & Management

### 빌드 명령어

```bash
# 기본 빌드
docker build -t myapp:v1.0 .

# 특정 Dockerfile 지정
docker build -f services/auth-service/Dockerfile -t auth-service:v1.0 .

# 빌드 인자 전달
docker build --build-arg BUILD_MODE=docker -t myapp:v1.0 .

# 캐시 없이 빌드
docker build --no-cache -t myapp:v1.0 .

# 특정 플랫폼용 빌드 (M1 Mac → Linux)
docker build --platform linux/amd64 -t myapp:v1.0 .
```

### 이미지 관리

```bash
# 이미지 목록 확인
docker images

# 이미지 상세 정보
docker inspect myapp:v1.0

# 이미지 히스토리 (레이어 확인)
docker history myapp:v1.0

# 이미지 태깅
docker tag myapp:v1.0 registry.example.com/myapp:v1.0

# 이미지 푸시
docker push registry.example.com/myapp:v1.0

# 불필요한 이미지 정리
docker image prune -a
```

---

## 7. Container Lifecycle

```
┌──────────┐   create   ┌──────────┐   start   ┌──────────┐
│  Image   │ ─────────► │ Created  │ ─────────► │ Running  │
└──────────┘            └──────────┘            └────┬─────┘
                                                     │
                          ┌──────────────────────────┤
                          │                          │
                          ▼ pause                    ▼ stop
                    ┌──────────┐              ┌──────────┐
                    │  Paused  │              │ Stopped  │
                    └──────────┘              └────┬─────┘
                                                   │
                                                   ▼ rm
                                             ┌──────────┐
                                             │ Removed  │
                                             └──────────┘
```

### 컨테이너 명령어

```bash
# 컨테이너 실행
docker run -d --name auth-service -p 8081:8081 auth-service:v1.0

# 컨테이너 목록
docker ps        # 실행 중인 컨테이너
docker ps -a     # 모든 컨테이너

# 컨테이너 로그
docker logs -f auth-service

# 컨테이너 접속
docker exec -it auth-service /bin/bash

# 컨테이너 중지/시작/재시작
docker stop auth-service
docker start auth-service
docker restart auth-service

# 컨테이너 삭제
docker rm auth-service
docker rm -f auth-service  # 강제 삭제
```

---

## 8. Best Practices

### Dockerfile 최적화

```dockerfile
# 1. 공식 이미지 사용
FROM eclipse-temurin:17-jre-alpine  # alpine 기반으로 크기 최소화

# 2. 불필요한 패키지 제거
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 3. 레이어 최소화
RUN apt-get update \
    && apt-get install -y package1 package2 \
    && apt-get clean

# 4. 비루트 사용자 실행
RUN addgroup --system app && adduser --system --ingroup app app
USER app

# 5. .dockerignore 활용
# .dockerignore 파일:
# .git
# node_modules
# target
# *.log
```

### 보안 고려사항

```dockerfile
# 1. 최신 보안 패치 적용
RUN apt-get update && apt-get upgrade -y

# 2. 비밀 정보를 이미지에 포함하지 않음
# Bad
ENV DATABASE_PASSWORD=secret123

# Good: 런타임에 환경변수로 전달
docker run -e DATABASE_PASSWORD=secret123 myapp

# 3. 읽기 전용 파일시스템
docker run --read-only myapp

# 4. 리소스 제한
docker run --memory=512m --cpus=0.5 myapp
```

---

## 9. 실습 예제

### Spring Boot 애플리케이션 컨테이너화

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 빌드 및 실행
docker build -t spring-app:v1.0 .
docker run -d -p 8080:8080 --name spring-app spring-app:v1.0

# 로그 확인
docker logs -f spring-app

# 헬스 체크
curl http://localhost:8080/actuator/health
```

---

## 10. 관련 문서

- [Docker Compose](./docker-compose.md) - 멀티 컨테이너 구성
- [Kubernetes Fundamentals](./kubernetes-fundamentals.md) - 컨테이너 오케스트레이션
- [Portal Universe Infra Guide](./portal-universe-infra-guide.md) - 프로젝트 인프라 가이드
