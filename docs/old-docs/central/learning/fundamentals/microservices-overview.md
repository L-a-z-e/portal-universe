# Microservices Architecture Overview

## 학습 목표

- 마이크로서비스 아키텍처의 핵심 개념 이해
- 모놀리스 vs 마이크로서비스 비교
- Portal Universe 프로젝트에서의 MSA 적용 분석

---

## 1. 마이크로서비스란?

마이크로서비스 아키텍처(MSA)는 애플리케이션을 **독립적으로 배포 가능한 작은 서비스들의 집합**으로 구성하는 아키텍처 스타일입니다.

### 핵심 특성

| 특성 | 설명 |
|------|------|
| 독립 배포 | 각 서비스를 개별적으로 배포 가능 |
| 느슨한 결합 | 서비스 간 최소한의 의존성 |
| 비즈니스 역량 중심 | 기술보다 비즈니스 도메인 기준 분리 |
| 분산 데이터 관리 | 서비스별 독립적인 데이터 저장소 |
| 인프라 자동화 | CI/CD, 컨테이너 기반 배포 |

### Martin Fowler의 정의

> "마이크로서비스는 단일 애플리케이션을 작은 서비스의 모음으로 개발하는 방법으로,
> 각 서비스는 자체 프로세스에서 실행되며 HTTP API 같은 경량 메커니즘으로 통신합니다."

---

## 2. 모놀리스 vs 마이크로서비스

### 모놀리식 아키텍처

```
┌─────────────────────────────────────────┐
│              Monolith                    │
│  ┌─────────┬─────────┬─────────┐       │
│  │   UI    │ Business│  Data   │       │
│  │  Layer  │  Logic  │  Layer  │       │
│  └─────────┴─────────┴─────────┘       │
│              │                          │
│        ┌─────┴─────┐                   │
│        │ Database  │                   │
│        └───────────┘                   │
└─────────────────────────────────────────┘
```

**장점:**
- 개발 초기 단순성
- 단일 배포 단위
- 트랜잭션 관리 용이
- IDE 지원 우수

**단점:**
- 규모 증가에 따른 복잡도 증가
- 전체 재배포 필요
- 기술 스택 제한
- 팀 간 병목 현상

### 마이크로서비스 아키텍처

```
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  Auth    │  │  Blog    │  │ Shopping │  │ Notify   │
│ Service  │  │ Service  │  │ Service  │  │ Service  │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘
     │             │             │             │
┌────┴────┐  ┌────┴────┐  ┌────┴────┐  ┌────┴────┐
│ MySQL   │  │ MongoDB │  │  MySQL  │  │  Redis  │
└─────────┘  └─────────┘  └─────────┘  └─────────┘
```

**장점:**
- 독립적 배포 및 확장
- 기술 스택 다양성 (Polyglot)
- 팀 자율성
- 장애 격리

**단점:**
- 분산 시스템 복잡성
- 네트워크 통신 오버헤드
- 데이터 일관성 관리
- 운영 복잡도 증가

### 비교 테이블

| 관점 | 모놀리스 | 마이크로서비스 |
|------|----------|----------------|
| 배포 단위 | 전체 앱 | 개별 서비스 |
| 확장 방식 | 수직 확장 (Scale-up) | 수평 확장 (Scale-out) |
| 데이터 관리 | 단일 DB | 서비스별 DB |
| 팀 구조 | 기능별 팀 | 제품별 팀 |
| 기술 선택 | 단일 스택 | 다양한 스택 |
| 장애 영향 | 전체 시스템 | 해당 서비스 |

---

## 3. Portal Universe의 MSA 적용

### 서비스 구성

Portal Universe는 다음과 같은 마이크로서비스로 구성됩니다:

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                          │
│               (Spring Cloud Gateway)                    │
└──────────────────────┬──────────────────────────────────┘
                       │
       ┌───────────────┼───────────────┐
       │               │               │
┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
│Auth Service │ │Blog Service │ │Shopping Svc │
│  (Java)     │ │   (Java)    │ │   (Java)    │
│  MySQL      │ │  MongoDB    │ │   MySQL     │
└─────────────┘ └─────────────┘ └─────────────┘
       │               │               │
       └───────────────┼───────────────┘
                       │
              ┌────────┴────────┐
              │   Kafka        │
              │ (Event Bus)    │
              └────────┬───────┘
                       │
              ┌────────┴────────┐
              │  Notification   │
              │    Service      │
              └─────────────────┘
```

### 각 서비스 역할

| 서비스 | 역할 | 데이터베이스 | 통신 방식 |
|--------|------|-------------|-----------|
| Auth Service | 인증/인가, 사용자 관리 | MySQL | REST, Kafka |
| Blog Service | 블로그 포스트, 댓글 | MongoDB | REST, Kafka |
| Shopping Service | 상품, 주문, 결제 | MySQL + ES | REST, Kafka |
| Notification Service | 알림 처리 | Redis | Kafka Consumer |
| API Gateway | 라우팅, 인증 검증 | - | REST |

### Polyglot Persistence

Portal Universe는 각 서비스의 요구사항에 맞는 데이터베이스를 선택합니다:

- **MySQL**: 관계형 데이터가 필요한 Auth, Shopping
- **MongoDB**: 유연한 스키마의 Blog
- **Elasticsearch**: 상품 검색 최적화
- **Redis**: 캐싱, 세션, 분산 락

---

## 4. MSA 설계 원칙

### 4.1 Single Responsibility (단일 책임)

각 서비스는 하나의 비즈니스 역량에 집중합니다.

```java
// Good: Shopping Service는 쇼핑 관련 기능만 담당
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    // 주문 생성, 조회, 취소 등
}

// Bad: 모든 기능을 하나의 서비스에 집중
@RestController
public class MonolithController {
    // 주문, 결제, 알림, 사용자 관리 모두 포함
}
```

### 4.2 Database per Service

각 서비스는 자체 데이터베이스를 소유합니다.

```
Auth Service ──→ auth_db (MySQL)
Blog Service ──→ blog_db (MongoDB)
Shopping Service ──→ shopping_db (MySQL)
```

**주의사항:**
- 다른 서비스의 DB에 직접 접근 금지
- API 또는 이벤트를 통해서만 데이터 교환
- 데이터 중복은 허용 (eventual consistency)

### 4.3 API First Design

서비스 간 계약을 API로 먼저 정의합니다.

```yaml
# OpenAPI Specification
openapi: 3.0.0
info:
  title: Shopping Service API
  version: 1.0.0
paths:
  /api/v1/products:
    get:
      summary: 상품 목록 조회
      responses:
        '200':
          description: 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductList'
```

### 4.4 Smart Endpoints, Dumb Pipes

- **Smart Endpoints**: 비즈니스 로직은 서비스 내부에
- **Dumb Pipes**: 통신 채널은 단순하게 (REST, Kafka)

```java
// Smart Endpoint: 비즈니스 로직을 서비스에서 처리
@Service
public class OrderService {
    public Order createOrder(OrderRequest request) {
        validateStock();      // 재고 검증
        calculatePrice();     // 가격 계산
        applyDiscount();      // 할인 적용
        return saveOrder();   // 주문 저장
    }
}
```

---

## 5. MSA 도입 시 고려사항

### 언제 MSA가 적합한가?

| 상황 | MSA 권장도 |
|------|-----------|
| 대규모 팀 (여러 개발팀) | ⭐⭐⭐ 높음 |
| 빠른 배포 주기 필요 | ⭐⭐⭐ 높음 |
| 독립적 확장 필요 | ⭐⭐⭐ 높음 |
| 초기 스타트업 | ⭐ 낮음 |
| 소규모 팀 (1-5명) | ⭐ 낮음 |
| 빠른 MVP 개발 | ⭐ 낮음 |

### MSA 도입의 전제 조건

1. **DevOps 문화**: CI/CD 파이프라인, 자동화
2. **컨테이너 기술**: Docker, Kubernetes
3. **모니터링 인프라**: 중앙 로깅, 분산 추적
4. **팀 역량**: 분산 시스템 경험

### 점진적 마이그레이션 전략

모놀리스에서 MSA로 한 번에 전환하지 않고, 점진적으로 분리합니다:

```
Phase 1: Monolith ──→ Monolith + 1개 서비스
Phase 2: ──→ Monolith + 3개 서비스
Phase 3: ──→ 5개 서비스 (Monolith 제거)
```

**Strangler Fig Pattern** 적용:
1. 새로운 기능은 새 서비스로 개발
2. 기존 기능을 점진적으로 추출
3. 최종적으로 모놀리스 제거

---

## 6. 실습: Portal Universe 아키텍처 분석

### 질문 1: 서비스 경계 확인

Portal Universe의 각 서비스가 어떤 도메인을 담당하는지 확인하세요.

```bash
# 각 서비스의 도메인 패키지 구조 확인
tree services/auth-service/src/main/java -d -L 3
tree services/blog-service/src/main/java -d -L 3
tree services/shopping-service/src/main/java -d -L 3
```

### 질문 2: 서비스 간 통신 분석

서비스들이 어떻게 통신하는지 Kafka 토픽을 확인하세요.

```bash
# Kafka 토픽 확인
grep -r "topic" services/*/src/main/resources/application*.yml
```

### 질문 3: 데이터베이스 분리 확인

각 서비스의 데이터베이스 설정을 확인하세요.

```bash
# 데이터베이스 설정 확인
grep -r "datasource" services/*/src/main/resources/application*.yml
```

---

## 7. 핵심 요약

### MSA의 핵심 가치

1. **독립성**: 서비스별 독립 개발/배포/확장
2. **회복력**: 장애 격리, 전체 시스템 보호
3. **민첩성**: 빠른 변화 대응, 지속적 배포
4. **확장성**: 필요한 서비스만 선택적 확장

### Portal Universe에서 배울 점

- Polyglot Persistence (MySQL, MongoDB, ES, Redis)
- 이벤트 기반 비동기 통신 (Kafka)
- API Gateway를 통한 통합 진입점
- 컨테이너 기반 배포 (Docker, K8s)

---

## 관련 문서

- [서비스 분리 전략](./service-decomposition.md)
- [서비스 간 통신](./inter-service-communication.md)
- [API Gateway 패턴](./api-gateway-pattern.md)
- [분산 데이터 관리](./distributed-data-management.md)
