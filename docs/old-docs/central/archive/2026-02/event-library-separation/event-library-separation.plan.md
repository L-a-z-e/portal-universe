# Feature Plan: event-library-separation

> **Feature**: 이벤트 라이브러리 분리
> **Author**: Claude
> **Created**: 2026-02-03
> **Status**: Draft

---

## 1. 개요

### 1.1 배경

현재 모든 도메인 이벤트가 `common-library`에 위치하여 불필요한 의존성 결합이 발생합니다:

```
현재 구조:
common-library/
└── event/
    ├── shopping/   ← blog-service도 의존하게 됨 (불필요)
    ├── blog/       ← shopping-service도 의존하게 됨 (불필요)
    └── prism/      ← 모든 서비스가 의존하게 됨 (불필요)
```

**문제점:**
- blog-service가 shopping 이벤트를 알 필요 없음
- shopping 이벤트 변경 시 blog-service도 재빌드 필요
- 단일 책임 원칙(SRP) 위반
- 불필요한 클래스패스 오염

### 1.2 목표

도메인별 이벤트 라이브러리를 분리하여 필요한 서비스만 해당 이벤트에 의존하도록 개선합니다.

```
목표 구조:
common-library/         → 진짜 공통 (예외, 응답, 유틸)
shopping-events/        → shopping-service, notification-service만 의존
blog-events/            → blog-service, notification-service만 의존
prism-events/           → prism-service, notification-service만 의존
```

---

## 2. 요구사항

### 2.1 기능 요구사항

| ID | 요구사항 | 우선순위 |
|----|---------|:--------:|
| FR-01 | shopping-events 라이브러리 생성 | High |
| FR-02 | blog-events 라이브러리 생성 | High |
| FR-03 | prism-events 라이브러리 생성 | High |
| FR-04 | common-library에서 이벤트 제거 | High |
| FR-05 | 각 서비스의 의존성 업데이트 | High |
| FR-06 | 기존 기능 정상 동작 보장 | Critical |

### 2.2 비기능 요구사항

| ID | 요구사항 | 측정 기준 |
|----|---------|----------|
| NFR-01 | 빌드 시간 단축 | 개별 서비스 빌드 시 불필요한 재컴파일 없음 |
| NFR-02 | 하위 호환성 | 기존 Kafka 메시지와 호환 |
| NFR-03 | 개발 편의성 | 이벤트 추가 시 해당 라이브러리만 수정 |

---

## 3. 범위

### 3.1 포함 (In Scope)

- [ ] shopping-events 모듈 생성 및 이벤트 이동
- [ ] blog-events 모듈 생성 및 이벤트 이동
- [ ] prism-events 모듈 생성 및 이벤트 이동
- [ ] 각 서비스 build.gradle 의존성 수정
- [ ] common-library에서 이벤트 패키지 제거
- [ ] 컴파일 및 통합 테스트

### 3.2 제외 (Out of Scope)

- Schema Registry 도입 (향후 별도 기능)
- 이벤트 버전 관리 시스템
- 이벤트 호환성 검증 자동화

---

## 4. 구현 전략

### 4.1 라이브러리 구조

```
services/
├── shopping-events/
│   ├── build.gradle
│   └── src/main/java/com/portal/universe/event/shopping/
│       ├── OrderCreatedEvent.java
│       ├── OrderCancelledEvent.java
│       ├── PaymentCompletedEvent.java
│       ├── PaymentFailedEvent.java
│       ├── DeliveryShippedEvent.java
│       ├── CouponIssuedEvent.java
│       └── TimeDealStartedEvent.java
│
├── blog-events/
│   ├── build.gradle
│   └── src/main/java/com/portal/universe/event/blog/
│       ├── PostLikedEvent.java
│       ├── CommentCreatedEvent.java
│       ├── CommentRepliedEvent.java
│       └── UserFollowedEvent.java
│
├── prism-events/
│   ├── build.gradle
│   └── src/main/java/com/portal/universe/event/prism/
│       ├── PrismTaskCompletedEvent.java
│       └── PrismTaskFailedEvent.java
│
└── common-library/
    └── src/main/java/com/portal/universe/common/
        ├── exception/     ← 유지
        ├── response/      ← 유지
        └── event/         ← 삭제 (UserSignedUpEvent만 남기거나 auth-events로 이동)
```

### 4.2 의존성 관계

```
shopping-service     → shopping-events, common-library
blog-service         → blog-events, common-library
prism-service        → (NestJS - 별도 타입 정의)
notification-service → shopping-events, blog-events, prism-events, common-library
auth-service         → common-library (UserSignedUpEvent 포함 시)
```

### 4.3 패키지 변경

| 현재 | 변경 후 |
|------|--------|
| `com.portal.universe.common.event.shopping.*` | `com.portal.universe.event.shopping.*` |
| `com.portal.universe.common.event.blog.*` | `com.portal.universe.event.blog.*` |
| `com.portal.universe.common.event.prism.*` | `com.portal.universe.event.prism.*` |

---

## 5. 위험 요소

| 위험 | 영향 | 완화 전략 |
|------|-----|----------|
| Import 경로 변경으로 컴파일 오류 | High | IDE 일괄 변경 기능 활용 |
| Kafka 역직렬화 실패 | Critical | 클래스 FQCN 동일하게 유지 또는 TypeId 헤더 활용 |
| 누락된 의존성 | Medium | 단계별 빌드 검증 |

---

## 6. 구현 단계

### Phase 1: 이벤트 라이브러리 생성 (Day 1)

```
□ 1.1 shopping-events 모듈 생성
□ 1.2 blog-events 모듈 생성
□ 1.3 prism-events 모듈 생성
□ 1.4 settings.gradle에 모듈 등록
```

### Phase 2: 이벤트 이동 (Day 1)

```
□ 2.1 common-library에서 shopping 이벤트 → shopping-events로 이동
□ 2.2 common-library에서 blog 이벤트 → blog-events로 이동
□ 2.3 common-library에서 prism 이벤트 → prism-events로 이동
□ 2.4 패키지 경로 업데이트
```

### Phase 3: 서비스 의존성 수정 (Day 1-2)

```
□ 3.1 shopping-service build.gradle 수정
□ 3.2 blog-service build.gradle 수정
□ 3.3 notification-service build.gradle 수정
□ 3.4 각 서비스 import 문 수정
```

### Phase 4: 정리 및 검증 (Day 2)

```
□ 4.1 common-library에서 이벤트 패키지 삭제
□ 4.2 전체 빌드 검증
□ 4.3 통합 테스트 (Kafka 이벤트 발행/수신)
```

---

## 7. 예상 일정

| Phase | 작업 | 예상 소요 |
|-------|------|----------|
| Phase 1 | 모듈 생성 | 1시간 |
| Phase 2 | 이벤트 이동 | 1시간 |
| Phase 3 | 의존성 수정 | 2시간 |
| Phase 4 | 검증 | 1시간 |
| **Total** | | **~5시간** |

---

## 8. 성공 기준

- [ ] 모든 서비스 빌드 성공
- [ ] 기존 Kafka 이벤트 정상 발행/수신
- [ ] shopping-service가 blog-events에 의존하지 않음
- [ ] blog-service가 shopping-events에 의존하지 않음

---

## Changelog

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-02-03 | 최초 작성 |
