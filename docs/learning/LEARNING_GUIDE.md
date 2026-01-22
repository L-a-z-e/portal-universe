# Portal Universe 학습 가이드

## 개요

이 가이드는 Portal Universe 프로젝트의 **250개 학습 문서**를 체계적으로 학습하기 위한 로드맵입니다.

---

## 학습 방법론

### 1. 문서 구조 이해

각 문서는 다음 구조로 작성되어 있습니다:

```
1. 학습 목표          → 이 문서에서 무엇을 배울 수 있는지
2. 개념 설명          → 이론적 배경과 핵심 개념
3. Portal Universe 적용 → 실제 프로젝트 코드 분석
4. 실습 예제          → 직접 해볼 수 있는 코드
5. 핵심 요약          → 반드시 기억할 포인트
6. 관련 문서          → 추가 학습 경로
```

### 2. 권장 학습 방식

```
┌─────────────────────────────────────────────────────────────┐
│  1. 문서 읽기 → 2. 실제 코드 확인 → 3. 직접 수정해보기    │
│                                                              │
│  [이론 학습]    [코드 분석]        [실습]                   │
└─────────────────────────────────────────────────────────────┘
```

**팁**: 문서를 읽은 후, 반드시 해당 코드 파일을 직접 열어서 확인하세요!

---

## 역할별 학습 경로

### 🟦 백엔드 개발자 경로 (약 4-6주)

```
Week 1: 기초
├── fundamentals/microservices-overview.md
├── fundamentals/service-decomposition.md
├── fundamentals/inter-service-communication.md
└── fundamentals/12-factor-app.md

Week 2: 메시징 & 캐싱
├── kafka/ (12개 문서 전체)
└── redis/ (10개 문서 전체)

Week 3: 데이터베이스
├── mongodb/ (8개)
├── elasticsearch/ (8개)
└── Shopping Service - database/ (8개)

Week 4: 도메인 설계
├── Shopping Service - domain/ (12개)
├── Blog Service - domain/ (8개)
└── Auth Service - oauth2/ (10개)

Week 5: 비즈니스 로직
├── Shopping Service - business/ (10개)
├── Shopping Service - events/ (6개)
└── patterns/ (8개)

Week 6: 보안 & 인프라
├── security/ (7개)
├── infra/ (10개)
└── API Gateway - gateway/ (12개)
```

### 🟩 프론트엔드 개발자 경로 (약 3-4주)

```
Week 1: Vue 3 기초
├── portal-shell/vue/ (8개 문서)
├── portal-shell/mfe/ (7개)
└── design-system/patterns/ (3개)

Week 2: React 18 기초
├── shopping-frontend/react/ (9개)
├── shopping-frontend/mfe/ (4개)
└── shopping-frontend/shopping/ (6개)

Week 3: Design System & 통합
├── design-system/tokens/ (2개)
├── design-system/components/ (3개)
└── 백엔드 API 이해: fundamentals/api-gateway-pattern.md

Week 4: 심화
├── clean-code/principles/ (6개)
├── clean-code/testing/e2e-testing.md
└── patterns/portal-universe-patterns.md
```

### 🟨 풀스택 개발자 경로 (약 8-10주)

백엔드 경로 (6주) + 프론트엔드 경로 (4주) 순차 학습

---

## 주제별 학습 경로

### 🔴 Path 1: 마이크로서비스 입문 (1주)

**목표**: MSA의 핵심 개념과 Portal Universe 아키텍처 이해

```
Day 1: 마이크로서비스 기초
├── docs/learning/fundamentals/microservices-overview.md
└── docs/learning/fundamentals/service-decomposition.md

Day 2: 서비스 간 통신
├── docs/learning/fundamentals/inter-service-communication.md
└── docs/learning/fundamentals/api-gateway-pattern.md

Day 3: 분산 시스템
├── docs/learning/fundamentals/distributed-data-management.md
├── docs/learning/fundamentals/service-discovery.md
└── docs/learning/fundamentals/observability-basics.md

Day 4-5: 아키텍처 패턴
├── docs/learning/patterns/saga-pattern-deep-dive.md
├── docs/learning/patterns/state-machine-pattern.md
└── docs/learning/patterns/portal-universe-patterns.md
```

### 🟠 Path 2: Kafka 마스터 (1주)

**목표**: Kafka를 활용한 이벤트 기반 아키텍처 구현

```
Day 1: Kafka 기초
├── kafka/kafka-introduction.md
└── kafka/kafka-core-concepts.md

Day 2: Producer & Consumer
├── kafka/kafka-producers-deep-dive.md
└── kafka/kafka-consumers-deep-dive.md

Day 3: 고급 주제
├── kafka/kafka-partitioning-strategy.md
├── kafka/kafka-exactly-once.md
└── kafka/kafka-schema-evolution.md

Day 4: Spring 통합
├── kafka/kafka-spring-integration.md
└── kafka/kafka-error-handling.md

Day 5: 운영 & 트러블슈팅
├── kafka/kafka-monitoring.md
├── kafka/kafka-portal-universe.md
└── kafka/kafka-troubleshooting.md
```

### 🟡 Path 3: Shopping 도메인 심화 (2주)

**목표**: 쇼핑몰 도메인의 완전한 이해

```
Week 1: 도메인 & 데이터
├── domain/shopping-domain-overview.md
├── domain/product-domain.md
├── domain/order-domain.md
├── domain/payment-domain.md
├── domain/inventory-domain.md
├── domain/cart-domain.md
├── domain/coupon-domain.md
├── domain/timedeal-domain.md
└── database/shopping-erd.md ~ database/soft-delete-audit.md (8개)

Week 2: 비즈니스 로직 & 이벤트
├── business/order-flow.md
├── business/payment-integration.md
├── business/inventory-concurrency.md
├── business/coupon-issuance.md
├── business/timedeal-flash-sale.md
├── business/order-saga.md
├── events/ (6개 전체)
└── search/ (6개 전체)
```

### 🟢 Path 4: 인증/보안 (1주)

**목표**: OAuth2, JWT, Spring Security 완전 정복

```
Day 1-2: OAuth2 기초
├── docs/learning/security/oauth2-fundamentals.md
├── docs/learning/security/jwt-deep-dive.md
└── auth-service/oauth2/oauth2-server-setup.md

Day 3-4: Auth Service 심화
├── auth-service/oauth2/ (나머지 9개)
└── auth-service/user/ (6개)

Day 5: Gateway & 보안
├── docs/learning/security/spring-security-architecture.md
├── docs/learning/security/api-gateway-security.md
├── api-gateway/gateway/jwt-validation.md
└── api-gateway/gateway/rate-limiting.md
```

### 🔵 Path 5: Frontend 심화 (2주)

**목표**: Module Federation 기반 마이크로 프론트엔드 구현

```
Week 1: Host 앱 (Vue 3)
├── portal-shell/vue/ (8개)
└── portal-shell/mfe/ (7개)

Week 2: Remote 앱 (React 18)
├── shopping-frontend/react/ (9개)
├── shopping-frontend/mfe/ (4개)
├── shopping-frontend/shopping/ (6개)
└── design-system/ (8개)
```

### 🟣 Path 6: Clean Code & 테스트 (1주)

**목표**: 코드 품질 향상과 테스트 전략

```
Day 1-2: Clean Code 원칙
├── clean-code/principles/solid-principles.md
├── clean-code/principles/dry-kiss-yagni.md
├── clean-code/principles/clean-code-naming.md
├── clean-code/principles/clean-code-functions.md
└── clean-code/principles/error-handling-patterns.md

Day 3: 아키텍처
├── clean-code/architecture/layered-architecture.md
├── clean-code/architecture/hexagonal-architecture.md
└── clean-code/architecture/ddd-basics.md

Day 4: 테스트
├── clean-code/testing/unit-testing.md
├── clean-code/testing/integration-testing.md
└── clean-code/testing/e2e-testing.md

Day 5: 리팩토링
├── clean-code/refactoring/refactoring-techniques.md
└── clean-code/refactoring/code-review-checklist.md
```

---

## 난이도별 학습 순서

### ⭐ 입문 (30개)

```
1. fundamentals/ (8개) - MSA 기초 개념
2. kafka/kafka-introduction.md
3. redis/redis-introduction.md
4. mongodb/mongodb-introduction.md
5. elasticsearch/es-introduction.md
6. Shopping domain overview
7. Blog domain overview
8. clean-code/principles/dry-kiss-yagni.md
9. clean-code/principles/clean-code-naming.md
```

### ⭐⭐ 초급 (60개)

```
- kafka 나머지 기초
- redis 나머지 기초
- mongodb 나머지 기초
- Shopping domain 상세
- Vue 기초 (composition-api, pinia)
- React 기초 (hooks, zustand)
```

### ⭐⭐⭐ 중급 (100개)

```
- kafka 심화 (exactly-once, schema-evolution)
- redis 심화 (distributed-lock, rate-limiting)
- Shopping business 로직
- Auth Service 전체
- API Gateway 전체
- clean-code 전체
```

### ⭐⭐⭐⭐ 고급 (60개)

```
- patterns 전체
- infra 전체
- security 전체
- Notification Service 전체
- Module Federation 심화
- 성능 최적화, 트러블슈팅
```

---

## 실습 프로젝트

### 실습 1: 간단한 기능 추가

**주제**: 새로운 할인 타입 추가
**필수 학습 문서**:
- domain/coupon-domain.md
- business/price-calculation.md
- database/jpa-entity-mapping.md

### 실습 2: Kafka 이벤트 추가

**주제**: 새로운 도메인 이벤트 발행
**필수 학습 문서**:
- kafka/kafka-spring-integration.md
- events/event-producer.md
- patterns/outbox-pattern.md

### 실습 3: 새 페이지 추가 (Frontend)

**주제**: 위시리스트 페이지 구현
**필수 학습 문서**:
- react/hooks-deep-dive.md
- react/zustand-state.md
- shopping/product-list.md

---

## 학습 팁

### 1. 코드와 함께 읽기

문서에서 언급하는 파일 경로를 직접 열어서 확인하세요:

```bash
# VS Code에서 파일 열기
code services/shopping-service/src/main/java/.../order/domain/Order.java
```

### 2. 브랜치 만들어서 실험

```bash
git checkout -b learning/kafka-practice
# 코드 수정하며 학습
# 완료 후 삭제
git checkout dev && git branch -D learning/kafka-practice
```

### 3. 로컬 환경에서 실행

```bash
# Docker Compose로 전체 환경 실행
docker-compose up -d

# 특정 서비스만 실행
docker-compose up -d kafka redis mysql
```

### 4. 테스트 코드 실행

```bash
# 단위 테스트 실행
./gradlew :services:shopping-service:test

# 특정 테스트 클래스 실행
./gradlew :services:shopping-service:test --tests "*OrderTest"
```

---

## 학습 체크리스트

### Week 1 체크리스트
- [ ] MSA 기초 8개 문서 완료
- [ ] Portal Universe 아키텍처 다이어그램 이해
- [ ] 로컬 환경에서 전체 서비스 실행

### Week 2 체크리스트
- [ ] Kafka 12개 문서 완료
- [ ] OrderCreatedEvent 코드 분석
- [ ] Kafka 메시지 발행/구독 실습

### Week 3 체크리스트
- [ ] Redis 10개 문서 완료
- [ ] 분산 락 구현 코드 분석
- [ ] 캐시 적용 실습

### Week 4 체크리스트
- [ ] Shopping 도메인 12개 문서 완료
- [ ] Order, Payment 상태 머신 이해
- [ ] 새로운 도메인 이벤트 추가 실습

---

## 문서 위치 요약

| 영역 | 경로 | 문서 수 |
|------|------|---------|
| 전체 시스템 | `docs/learning/` | 99개 |
| Shopping Service | `services/shopping-service/docs/learning/` | 44개 |
| Blog Service | `services/blog-service/docs/learning/` | 24개 |
| Auth Service | `services/auth-service/docs/learning/` | 20개 |
| Notification Service | `services/notification-service/docs/learning/` | 16개 |
| API Gateway | `services/api-gateway/docs/learning/` | 12개 |
| Portal Shell | `frontend/portal-shell/docs/learning/` | 15개 |
| Shopping Frontend | `frontend/shopping-frontend/docs/learning/` | 20개 |
| **총계** | | **250개** |

---

## 다음 단계

학습을 완료한 후:

1. **실제 이슈 해결**: GitHub Issues에서 `good first issue` 라벨 확인
2. **코드 리뷰 참여**: PR 리뷰를 통해 코드 이해도 향상
3. **문서 기여**: 오타 수정, 내용 보완 등으로 기여
4. **새 기능 구현**: 학습한 패턴을 적용하여 기능 추가

---

## 관련 문서

- [README.md](./README.md) - 전체 문서 목록
- [ADR](../adr/) - 아키텍처 결정 기록
- [Scenarios](../scenarios/) - 업무 시나리오
