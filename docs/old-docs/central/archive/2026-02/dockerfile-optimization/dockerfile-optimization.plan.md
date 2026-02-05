# Dockerfile Optimization Planning Document

> **Summary**: Gradle 서비스 Dockerfile의 Layer Cache 최적화로 빌드 시간 단축
>
> **Project**: portal-universe
> **Version**: N/A
> **Author**: Claude
> **Date**: 2026-02-04
> **Status**: Draft

---

## 1. Overview

### 1.1 Purpose

현재 Gradle 기반 백엔드 서비스들의 Dockerfile이 Layer Cache를 효율적으로 활용하지 못하고 있어, 소스 코드 한 줄 변경에도 전체 빌드가 재실행되는 문제를 해결한다.

### 1.2 Background

**현재 상태 분석 결과:**

| 캐시 유형 | 현재 상태 | 문제점 |
|----------|----------|--------|
| Layer Cache | ❌ 미활용 | `COPY . .` 이후 바로 빌드하여 코드 변경 시 무조건 재빌드 |
| Mount Cache | ✅ 활용 중 | `--mount=type=cache`로 Gradle 캐시 재사용 |

**대상 서비스 (5개):**
- `services/api-gateway/Dockerfile`
- `services/auth-service/Dockerfile`
- `services/blog-service/Dockerfile`
- `services/shopping-service/Dockerfile`
- `services/notification-service/Dockerfile`

**문제 구조:**
```dockerfile
# 현재 (비효율적)
COPY . .                    # ← 소스 변경 시 여기서 캐시 무효화
RUN gradlew build           # ← 매번 재실행 (mount cache로 다운로드만 스킵)
```

**모노레포 이슈:**
- Build context가 프로젝트 루트 (`.`)
- auth-service 코드 수정 시 blog-service 빌드도 캐시 무효화
- `.dockerignore`가 frontend는 제외하지만 다른 서비스는 포함

### 1.3 Related Documents

- 분석 대화: 현재 세션
- Docker BuildKit 문서: https://docs.docker.com/build/cache/

---

## 2. Scope

### 2.1 In Scope

- [x] 5개 Gradle 서비스 Dockerfile 최적화
- [x] Layer Cache 활용을 위한 의존성 파일 분리
- [x] 기존 Mount Cache 유지
- [ ] 빌드 시간 측정 및 비교

### 2.2 Out of Scope

- Python/Node.js 서비스 Dockerfile (별도 구조)
- Frontend Dockerfile 최적화
- CI/CD 파이프라인 변경
- 서비스별 Build context 분리 (큰 구조 변경 필요)

---

## 3. Requirements

### 3.1 Functional Requirements

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| FR-01 | 의존성 파일(build.gradle, settings.gradle)을 먼저 복사 | High | Pending |
| FR-02 | dependencies 다운로드 단계 분리 | High | Pending |
| FR-03 | 소스 코드 복사와 빌드 단계 분리 | High | Pending |
| FR-04 | 기존 Mount Cache 설정 유지 | High | Pending |
| FR-05 | common-library 의존성 처리 | Medium | Pending |

### 3.2 Non-Functional Requirements

| Category | Criteria | Measurement Method |
|----------|----------|-------------------|
| Performance | 의존성 변경 없을 때 빌드 시간 50% 이상 단축 | 빌드 전후 시간 측정 |
| Compatibility | 기존 docker-compose.yml 변경 없이 작동 | 빌드 및 실행 테스트 |
| Maintainability | 모든 서비스 동일한 패턴 적용 | 코드 리뷰 |

---

## 4. Success Criteria

### 4.1 Definition of Done

- [x] 모든 5개 서비스 Dockerfile 수정 완료
- [ ] docker-compose build 성공
- [ ] 각 서비스 컨테이너 정상 실행
- [ ] 빌드 캐시 효율성 검증 완료

### 4.2 Quality Criteria

- [ ] 의존성만 변경 시 dependencies 단계까지만 재빌드
- [ ] 소스만 변경 시 빌드 단계만 재빌드
- [ ] 다른 서비스 변경이 영향을 최소화

---

## 5. Risks and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| 모노레포 구조로 인한 복잡성 | Medium | High | common-library 의존성 파일도 함께 복사 |
| gradle dependencies 실패 가능성 | Medium | Low | 기존 mount cache 유지로 fallback |
| 기존 빌드 동작 변경 | High | Low | 단계별 테스트 진행 |

---

## 6. Architecture Considerations

### 6.1 Project Level Selection

| Level | Characteristics | Recommended For | Selected |
|-------|-----------------|-----------------|:--------:|
| **Starter** | Simple structure | Static sites | ☐ |
| **Dynamic** | Feature-based modules | Web apps with backend | ☐ |
| **Enterprise** | Strict layer separation, microservices | High-traffic systems | ☑ |

### 6.2 Key Architectural Decisions

| Decision | Options | Selected | Rationale |
|----------|---------|----------|-----------|
| 캐시 전략 | Layer만 / Mount만 / 둘 다 | 둘 다 | 최대 효율 |
| 의존성 분리 | 2단계 / 3단계 | 2단계 | 복잡도 vs 효과 균형 |
| settings.gradle 위치 | 루트만 / 각 서비스 | 루트 | 모노레포 구조 유지 |

### 6.3 개선된 Dockerfile 구조

```dockerfile
# Stage 1: Build Stage
FROM gradle:8.9-jdk17 AS builder

WORKDIR /app

# 1단계: 의존성 파일만 먼저 복사 (Layer Cache 활용)
COPY build.gradle settings.gradle ./
COPY services/common-library/build.gradle services/common-library/
COPY services/{service-name}/build.gradle services/{service-name}/
COPY gradle/ gradle/
COPY gradlew ./

# 2단계: 의존성 다운로드 (의존성 변경 없으면 캐시)
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:{service-name}:dependencies --no-daemon || true

# 3단계: 소스 코드 복사
COPY services/common-library/ services/common-library/
COPY services/{service-name}/ services/{service-name}/

# 4단계: 빌드 (소스만 변경 시 여기서만 재실행)
RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    --mount=type=cache,target=/home/gradle/.gradle/wrapper \
    ./gradlew :services:{service-name}:build --no-daemon -x test

# Stage 2: Runtime Stage (기존과 동일)
...
```

---

## 7. Convention Prerequisites

### 7.1 Existing Project Conventions

- [x] `.dockerignore`가 frontend 제외
- [x] Multi-stage build 사용 중
- [x] Mount cache 사용 중
- [x] 각 서비스별 Dockerfile 존재

### 7.2 Conventions to Define/Verify

| Category | Current State | To Define | Priority |
|----------|---------------|-----------|:--------:|
| **Dockerfile 패턴** | 서비스마다 동일 | Layer cache 표준화 | High |
| **Build context** | 루트 (`.`) | 유지 | - |
| **Mount paths** | `/home/gradle/.gradle/caches` | 유지 | - |

### 7.3 Environment Variables Needed

변경 없음 - 기존 환경변수 그대로 사용

---

## 8. Implementation Plan

### 8.1 단계별 작업

| 단계 | 작업 | 대상 |
|------|------|------|
| 1 | api-gateway Dockerfile 수정 및 테스트 | pilot |
| 2 | 나머지 4개 서비스 동일 패턴 적용 | batch |
| 3 | 전체 docker-compose build 테스트 | validation |
| 4 | 캐시 효율성 측정 | metrics |

### 8.2 예상 효과

```
Before (현재):
┌─────────────────────────────────────────────────────────────┐
│ 소스 1줄 변경 → COPY . . 무효화 → gradlew build 재실행     │
│ 예상 시간: 2-5분 (mount cache로 다운로드 스킵)             │
└─────────────────────────────────────────────────────────────┘

After (개선):
┌─────────────────────────────────────────────────────────────┐
│ 소스 1줄 변경 → dependencies 캐시 히트 → build만 재실행    │
│ 예상 시간: 30초-1분 (컴파일만 수행)                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Next Steps

1. [ ] Design 문서 작성 (`dockerfile-optimization.design.md`)
2. [ ] 팀 리뷰 및 승인
3. [ ] api-gateway 파일럿 구현
4. [ ] 나머지 서비스 적용

---

## Version History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 0.1 | 2026-02-04 | Initial draft | Claude |
