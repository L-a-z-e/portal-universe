# Dockerfile Optimization Design Document

> **Summary**: Gradle 서비스 Dockerfile의 Layer Cache 최적화 상세 설계
>
> **Project**: portal-universe
> **Version**: N/A
> **Author**: Claude
> **Date**: 2026-02-04
> **Status**: Draft
> **Planning Doc**: [dockerfile-optimization.plan.md](../01-plan/features/dockerfile-optimization.plan.md)

---

## 1. Overview

### 1.1 Design Goals

- 의존성 파일과 소스 코드를 분리하여 Docker Layer Cache 최대 활용
- 기존 Mount Cache 유지로 Gradle 캐시 재사용
- 모노레포 구조에서 서비스별 독립적 캐시 레이어 구성

### 1.2 Design Principles

- **최소 변경**: 기존 Runtime Stage 유지, Build Stage만 수정
- **일관성**: 모든 서비스에 동일한 패턴 적용 (common-library 유무에 따라 2가지 템플릿)
- **점진적 적용**: Pilot(api-gateway) 먼저, 이후 나머지 서비스 적용

---

## 2. Architecture

### 2.1 서비스 의존성 분석

```
┌─────────────────────────────────────────────────────────────┐
│                     common-library                          │
│                   (공통 예외, 응답 형식)                      │
└─────────────────────────────────────────────────────────────┘
         ▲           ▲           ▲           ▲
         │           │           │           │
    ┌────┴────┐ ┌────┴────┐ ┌────┴────┐ ┌────┴────┐
    │  auth   │ │  blog   │ │shopping │ │notifica │
    │ service │ │ service │ │ service │ │  tion   │
    └─────────┘ └─────────┘ └─────────┘ └─────────┘

    ┌─────────┐
    │   api   │ ← common-library 미사용 (독립적)
    │ gateway │
    └─────────┘
```

### 2.2 Dockerfile 템플릿 유형

| 유형 | 대상 서비스 | common-library |
|------|------------|----------------|
| **Type A** | api-gateway | 미사용 |
| **Type B** | auth, blog, shopping, notification | 사용 |

### 2.3 Layer Cache 전략

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: 의존성 파일 복사                                     │
│   COPY build.gradle settings.gradle ./                      │
│   COPY gradle/ gradlew ./                                   │
│   COPY services/{service}/build.gradle                      │
│   (Type B: COPY services/common-library/build.gradle)       │
├─────────────────────────────────────────────────────────────┤
│ Layer 2: 의존성 다운로드 (변경 없으면 캐시)                    │
│   RUN gradlew dependencies                                  │
├─────────────────────────────────────────────────────────────┤
│ Layer 3: 소스 코드 복사                                      │
│   COPY services/{service}/ services/{service}/              │
│   (Type B: COPY services/common-library/ ...)              │
├─────────────────────────────────────────────────────────────┤
│ Layer 4: 빌드 (소스 변경 시만 재실행)                         │
│   RUN gradlew build                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Design

### 3.1 Type A: api-gateway (common-library 미사용)

```dockerfile
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
COPY services/api-gateway/build.gradle services/api-gateway/

# ---------------------------------------------
# Layer 2: 의존성 다운로드 (의존성 변경 없으면 캐시)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:api-gateway:dependencies --no-daemon || true

# ---------------------------------------------
# Layer 3: 소스 코드 복사
# ---------------------------------------------
COPY services/api-gateway/ services/api-gateway/

# ---------------------------------------------
# Layer 4: 빌드 (소스만 변경 시 여기서만 재실행)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:api-gateway:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage (기존과 동일)
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/api-gateway/build/libs/api-gateway-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 Type B: common-library 의존 서비스

**auth-service 예시** (blog, shopping, notification 동일 패턴):

```dockerfile
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
COPY services/auth-service/build.gradle services/auth-service/

# ---------------------------------------------
# Layer 2: 의존성 다운로드 (의존성 변경 없으면 캐시)
# common-library도 함께 resolve
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:auth-service:dependencies --no-daemon || true

# ---------------------------------------------
# Layer 3: 소스 코드 복사 (common-library 포함)
# ---------------------------------------------
COPY services/common-library/ services/common-library/
COPY services/auth-service/ services/auth-service/

# ---------------------------------------------
# Layer 4: 빌드 (소스만 변경 시 여기서만 재실행)
# ---------------------------------------------
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:auth-service:build --no-daemon -x test

# =================================================================
# Stage 2: Runtime Stage (기존과 동일)
# =================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN addgroup --system appgroup && adduser --system -ingroup appgroup appuser

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/logs && chown appuser:appgroup /app/logs

COPY --from=builder /app/services/auth-service/build/libs/auth-service-0.0.1-SNAPSHOT.jar app.jar

USER appuser

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 4. Service-Specific Configuration

### 4.1 대상 서비스 목록

| Service | Type | Port | JAR Name |
|---------|------|------|----------|
| api-gateway | A | 8080 | api-gateway-0.0.1-SNAPSHOT.jar |
| auth-service | B | 8081 | auth-service-0.0.1-SNAPSHOT.jar |
| blog-service | B | 8082 | blog-service-0.0.1-SNAPSHOT.jar |
| shopping-service | B | 8083 | shopping-service-0.0.1-SNAPSHOT.jar |
| notification-service | B | 8084 | notification-service-0.0.1-SNAPSHOT.jar |

### 4.2 서비스별 변경 요약

| Service | 변경할 파일 |
|---------|-----------|
| api-gateway | `services/api-gateway/Dockerfile` |
| auth-service | `services/auth-service/Dockerfile` |
| blog-service | `services/blog-service/Dockerfile` |
| shopping-service | `services/shopping-service/Dockerfile` |
| notification-service | `services/notification-service/Dockerfile` |

---

## 5. Cache Efficiency Analysis

### 5.1 시나리오별 캐시 히트

| 변경 사항 | Layer 1 | Layer 2 | Layer 3 | Layer 4 |
|----------|---------|---------|---------|---------|
| 의존성 없음, 소스 없음 | ✅ HIT | ✅ HIT | ✅ HIT | ✅ HIT |
| 소스만 변경 | ✅ HIT | ✅ HIT | ❌ MISS | ❌ MISS |
| 의존성만 변경 | ❌ MISS | ❌ MISS | ❌ MISS | ❌ MISS |
| common-library 변경 (Type B) | ✅ HIT | ✅ HIT | ❌ MISS | ❌ MISS |

### 5.2 예상 빌드 시간

| 시나리오 | 현재 | 개선 후 | 단축률 |
|----------|------|---------|--------|
| Full rebuild | ~5분 | ~5분 | 0% |
| 소스만 변경 | ~3분 | ~1분 | 66% |
| 의존성 + 소스 변경 | ~5분 | ~4분 | 20% |
| 캐시 완전 히트 | ~3분 | ~10초 | 95% |

---

## 6. Test Plan

### 6.1 검증 시나리오

| # | 테스트 | 예상 결과 |
|---|--------|----------|
| 1 | `docker compose build api-gateway` (초기) | 빌드 성공 |
| 2 | 코드 변경 없이 재빌드 | 모든 레이어 캐시 히트 |
| 3 | Java 소스 파일 1줄 변경 후 재빌드 | Layer 1-2 캐시, Layer 3-4 재실행 |
| 4 | build.gradle 의존성 추가 후 재빌드 | 모든 레이어 재실행 |
| 5 | `docker compose up` 정상 실행 | 컨테이너 헬스체크 통과 |

### 6.2 검증 명령어

```bash
# 1. 개별 서비스 빌드 테스트
docker compose build api-gateway --progress=plain

# 2. 캐시 확인 (CACHED 문구 확인)
docker compose build api-gateway 2>&1 | grep -i cached

# 3. 전체 서비스 빌드
docker compose build api-gateway auth-service blog-service shopping-service notification-service

# 4. 컨테이너 실행 및 헬스체크
docker compose up -d api-gateway
docker compose ps  # 헬스체크 상태 확인
```

---

## 7. Rollback Plan

문제 발생 시 롤백 방법:

```bash
# Git에서 이전 Dockerfile 복원
git checkout HEAD~1 -- services/api-gateway/Dockerfile

# 또는 수동으로 기존 패턴 복원
# COPY . .
# RUN gradlew build
```

---

## 8. Implementation Order

### 8.1 단계별 구현

| 순서 | 작업 | 대상 |
|------|------|------|
| 1 | api-gateway Dockerfile 수정 | Pilot |
| 2 | api-gateway 빌드 테스트 | 검증 |
| 3 | auth-service Dockerfile 수정 | Type B 첫 번째 |
| 4 | blog-service Dockerfile 수정 | |
| 5 | shopping-service Dockerfile 수정 | |
| 6 | notification-service Dockerfile 수정 | |
| 7 | 전체 docker-compose build 테스트 | 최종 검증 |

### 8.2 Checklist

- [ ] api-gateway Dockerfile 수정
- [ ] api-gateway 빌드 및 실행 테스트
- [ ] auth-service Dockerfile 수정
- [ ] blog-service Dockerfile 수정
- [ ] shopping-service Dockerfile 수정
- [ ] notification-service Dockerfile 수정
- [ ] 전체 빌드 테스트 (`docker compose build`)
- [ ] 캐시 효율성 검증

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-02-04 | Initial draft | Claude |
