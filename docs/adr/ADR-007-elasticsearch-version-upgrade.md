---
id: ADR-007
title: Elasticsearch 8.18.5 버전 업그레이드
type: adr
status: accepted
created: 2026-01-19
updated: 2026-01-19
author: Laze
decision_date: 2026-01-19
reviewers: []
tags: [elasticsearch, infrastructure, compatibility]
related:
  - ADR-005
---

# ADR-007: Elasticsearch 8.18.5 버전 업그레이드

## 메타데이터

| 항목 | 내용 |
|------|------|
| **상태** | Accepted |
| **결정일** | 2026-01-19 |
| **작성자** | Laze |

---

## Context (배경)

### 문제 상황

Spring Boot 3.5.5가 관리하는 Elasticsearch Java 클라이언트(8.18.5)와 로컬 ES 서버(8.11.0) 버전 불일치로 인해 런타임 오류가 발생했습니다.

**발생 오류**:
- `NoSuchMethodError: activeShardsPercentAsNumber()` - Health Check API 호환성 문제
- `MissingRequiredPropertyException: HealthResponse.unassignedPrimaryShards` - 응답 필드 누락

**원인 분석**:
- Spring Boot 3.5.5는 내부적으로 elasticsearch-java 8.18.5를 관리
- 로컬 ES 서버는 8.11.0으로 구동
- build.gradle에서 `elasticsearch-java:8.11.0`을 명시적으로 지정하여 버전 충돌 발생
- 클라이언트가 8.11.0 API를 사용하지만, Actuator Health Check는 8.18.x API를 기대

### 기술적 제약

1. **버전 일치 필요성**: ES 클라이언트와 서버 버전이 일치해야 안정적으로 동작
2. **API 변경**: ES 8.18.x에서 RangeQuery API가 변경됨 (generic → typed: `number()`, `date()` 등)
3. **Kubernetes 환경**: K8s 배포 환경에서도 동일 버전 사용 필요

---

## Decision Drivers (결정 요인)

1. **호환성**: Spring Boot가 관리하는 버전과 일치하여 Actuator Health Check 정상 작동
2. **유지보수성**: 명시적 버전 고정 대신 Spring Boot BOM 관리 방식 활용
3. **일관성**: Local, Docker, Kubernetes 모든 환경에서 동일 버전 사용
4. **안정성**: 검증된 최신 버전 사용으로 보안 및 버그 수정 적용

---

## Considered Options (검토한 대안)

### Option 1: ES 서버를 8.18.5로 업그레이드 (선택됨)

**변경 사항**:
- Docker Compose ES/Kibana 이미지: 8.11.0 → 8.18.5
- build.gradle: `elasticsearch-java:8.11.0` 명시적 의존성 제거
- application.yml: ES Health Check 비활성화 설정 제거
- ProductSearchService.java: RangeQuery API를 8.18.x 버전으로 수정

**장점**:
- Spring Boot 관리 버전 사용으로 일관성 확보
- Actuator Health Check 정상 작동
- 최신 기능 및 보안 패치 적용

**단점**:
- ES 데이터 마이그레이션 필요 가능성 (개발 환경은 데이터 재생성으로 해결)
- RangeQuery API 코드 수정 필요

---

### Option 2: ES 클라이언트를 8.11.0으로 고정 유지

**변경 사항**:
- 기존 설정 유지
- Actuator ES Health Check 비활성화 유지

**장점**:
- 코드 변경 최소화
- 기존 ES 데이터 보존

**단점**:
- Spring Boot 관리 방식과 충돌
- Actuator Health Check 비활성화로 모니터링 불완전
- 향후 Spring Boot 업그레이드 시 지속적 호환성 문제 예상

**평가**: **부적합** - 근본적 해결이 아닌 우회책

---

### Option 3: Spring Data Elasticsearch만 사용 (elasticsearch-java 제거)

**변경 사항**:
- Low-level ES 클라이언트 대신 Spring Data Elasticsearch Repository만 사용

**장점**:
- 추상화된 API로 버전 의존성 감소
- 코드 간소화

**단점**:
- 복잡한 쿼리 작성 어려움 (Fuzzy search, Highlight 등)
- 기존 ProductSearchService 로직 전면 재작성 필요

**평가**: **과도한 변경** - 현재 구현된 검색 기능 손실

---

## Option 비교표

| 항목 | ES 업그레이드 | 클라이언트 고정 | Spring Data만 사용 |
|------|--------------|----------------|-------------------|
| **호환성** | 완전 호환 | 부분 호환 | 완전 호환 |
| **코드 변경** | 최소 (RangeQuery) | 없음 | 대규모 |
| **Health Check** | 정상 | 비활성화 | 정상 |
| **유지보수** | 용이 | 지속적 관리 필요 | 용이 |
| **기능 손실** | 없음 | 없음 | 일부 검색 기능 |

---

## Decision (최종 결정)

**Option 1: ES 서버를 8.18.5로 업그레이드합니다.**

### 수정 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `docker-compose.yml` | ES/Kibana 이미지 8.11.0 → 8.18.5 |
| `integration-tests/docker-compose.test.yml` | ES-test 이미지 8.11.0 → 8.18.5 |
| `services/shopping-service/build.gradle` | `elasticsearch-java:8.11.0` 의존성 제거 |
| `services/shopping-service/src/main/resources/application.yml` | ES Health Check 비활성화 설정 제거 |
| `services/shopping-service/.../ProductSearchService.java` | RangeQuery API 수정 |
| `k8s/infrastructure/configmap.yaml` | ELASTICSEARCH_URIS, REDIS 환경변수 추가 |
| `k8s/infrastructure/elasticsearch.yaml` | **신규 생성** - K8s ES StatefulSet |

### API 변경 사항

**변경 전 (8.11.0 API)**:
```java
.range(r -> {
    r.field("price");
    if (request.getMinPrice() != null) {
        r.gte(JsonData.of(request.getMinPrice()));
    }
    return r;
})
```

**변경 후 (8.18.x API)**:
```java
.range(r -> r
    .number(n -> {
        n.field("price");
        if (request.getMinPrice() != null) {
            n.gte(request.getMinPrice().doubleValue());
        }
        if (request.getMaxPrice() != null) {
            n.lte(request.getMaxPrice().doubleValue());
        }
        return n;
    })
)
```

---

## Consequences (영향)

### 긍정적 영향

1. **버전 일관성**
   - Spring Boot BOM 관리 버전과 완전 일치
   - Local, Docker, K8s 모든 환경에서 동일 버전

2. **모니터링 개선**
   - Actuator ES Health Check 정상 작동
   - `/actuator/health`에서 ES 상태 확인 가능

3. **유지보수성 향상**
   - 명시적 버전 고정 제거로 Spring Boot 업그레이드 용이
   - 최신 보안 패치 및 버그 수정 자동 적용

4. **Kubernetes 지원**
   - K8s 환경용 ES StatefulSet 배포 파일 추가
   - 프로덕션 환경 준비 완료

### 부정적 영향 (트레이드오프)

1. **데이터 마이그레이션**
   - 개발 환경 ES 데이터 재생성 필요 (볼륨 삭제 후 재시작)
   - 프로덕션에서는 Snapshot/Restore 절차 필요

2. **코드 변경**
   - RangeQuery API 변경으로 ProductSearchService 수정 필요

---

## 검증 방법

### 1. ES 서버 버전 확인
```bash
curl http://localhost:9200
# "version.number": "8.18.5" 확인
```

### 2. Actuator Health Check
```bash
curl http://localhost:8083/actuator/health
# elasticsearch: UP 확인
```

### 3. 검색 기능 테스트
- Shopping 페이지에서 상품 목록 로딩 확인
- 가격 범위 필터 검색 테스트

### 4. 빌드 테스트
```bash
./gradlew :services:shopping-service:build
```

---

## 롤백 계획

문제 발생 시:
1. `docker-compose.yml`에서 ES/Kibana 버전을 8.11.0으로 복원
2. `build.gradle`에 `elasticsearch-java:8.11.0` 다시 추가
3. `application.yml`에 ES Health Check 비활성화 다시 추가
4. `ProductSearchService.java` RangeQuery API 복원

---

## 참고 자료

- [Elasticsearch Java Client Changelog](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/release-notes.html)
- [Spring Boot Dependency Versions](https://docs.spring.io/spring-boot/docs/3.5.5/reference/html/dependency-versions.html)
- [Elasticsearch Docker Image](https://www.docker.elastic.co/r/elasticsearch)

---

**최종 업데이트**: 2026-01-19
**작성자**: Laze
