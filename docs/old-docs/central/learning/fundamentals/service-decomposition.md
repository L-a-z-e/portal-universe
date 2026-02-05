# Service Decomposition (서비스 분리 전략)

## 학습 목표

- 마이크로서비스 분리 전략 이해
- Bounded Context와 도메인 경계 정의
- Portal Universe의 서비스 분리 분석

---

## 1. 서비스 분리의 중요성

### 왜 서비스 분리가 중요한가?

잘못된 서비스 분리는 다음과 같은 문제를 야기합니다:

| 문제 | 증상 |
|------|------|
| 과도한 결합 | 하나의 변경이 여러 서비스에 영향 |
| 분산 모놀리스 | MSA의 복잡성만 있고 장점은 없음 |
| 성능 저하 | 서비스 간 과도한 통신 |
| 데이터 불일관성 | 서비스 경계에서 데이터 정합성 문제 |

### 좋은 서비스 분리의 특성

- **높은 응집도 (High Cohesion)**: 관련 기능이 함께 존재
- **낮은 결합도 (Low Coupling)**: 서비스 간 최소 의존성
- **독립 배포 가능**: 다른 서비스 변경 없이 배포
- **명확한 책임**: 서비스 역할이 명확히 정의됨

---

## 2. 서비스 분리 전략

### 2.1 도메인 기반 분해 (Domain-Driven Decomposition)

**DDD(Domain-Driven Design)**의 Bounded Context를 활용합니다.

```
┌─────────────────────────────────────────────────────┐
│                    E-Commerce Domain                 │
├───────────────┬───────────────┬────────────────────┤
│   Catalog     │    Order      │     Payment        │
│   Context     │    Context    │     Context        │
├───────────────┼───────────────┼────────────────────┤
│ • Product     │ • Order       │ • Payment          │
│ • Category    │ • OrderItem   │ • Transaction      │
│ • Inventory   │ • Shipping    │ • Refund           │
└───────────────┴───────────────┴────────────────────┘
```

**Portal Universe 적용:**

```
┌─────────────────────────────────────────────────────┐
│                 Portal Universe                      │
├────────────┬────────────┬────────────┬─────────────┤
│  Identity  │   Blog     │  Shopping  │   Notify    │
│  Context   │  Context   │  Context   │   Context   │
├────────────┼────────────┼────────────┼─────────────┤
│ • User     │ • Post     │ • Product  │ • Notification│
│ • Role     │ • Comment  │ • Order    │ • Template   │
│ • Session  │ • Series   │ • Cart     │ • Channel    │
│            │ • Tag      │ • Coupon   │              │
└────────────┴────────────┴────────────┴─────────────┘
```

### 2.2 비즈니스 역량 기반 분해 (Business Capability)

조직의 비즈니스 기능 단위로 분리합니다.

```
비즈니스 역량                    서비스
─────────────                    ─────
사용자 관리          ──→        Auth Service
콘텐츠 생성/관리      ──→        Blog Service
상품 판매            ──→        Shopping Service
고객 알림            ──→        Notification Service
```

### 2.3 하위 도메인 분석 (Subdomain Analysis)

| 하위 도메인 유형 | 특성 | Portal Universe 예시 |
|-----------------|------|---------------------|
| Core Domain | 비즈니스 핵심, 경쟁력 원천 | Shopping (주문/결제) |
| Supporting Domain | Core를 지원 | Blog, Auth |
| Generic Domain | 일반적 기능, 구매/아웃소싱 가능 | Notification |

---

## 3. Bounded Context

### 3.1 Bounded Context란?

Bounded Context는 특정 도메인 모델이 적용되는 명시적인 경계입니다.

**같은 용어, 다른 의미:**

```
┌─────────────────────┐    ┌─────────────────────┐
│    Sales Context    │    │   Shipping Context  │
├─────────────────────┤    ├─────────────────────┤
│  Customer:          │    │  Customer:          │
│  - name             │    │  - name             │
│  - creditLimit      │    │  - address          │
│  - purchaseHistory  │    │  - deliveryPrefs    │
└─────────────────────┘    └─────────────────────┘
       │                           │
       └─────── 같은 'Customer' ───┘
           다른 속성과 의미!
```

### 3.2 Context Mapping

서비스 간 관계를 정의합니다.

**관계 유형:**

| 관계 | 설명 | Portal Universe 예시 |
|------|------|---------------------|
| Customer-Supplier | 상류(Upstream) 서비스가 하류(Downstream) 지원 | Auth → Shopping |
| Shared Kernel | 공유 모델 | common-library |
| Published Language | 공개 API 계약 | REST API, Kafka Events |
| Anti-Corruption Layer | 외부 모델 변환 레이어 | 외부 PG 연동 |

**Portal Universe Context Map:**

```
                 ┌──────────────┐
                 │   Gateway    │
                 └──────┬───────┘
                        │
    ┌───────────────────┼───────────────────┐
    │                   │                   │
    ▼                   ▼                   ▼
┌────────┐        ┌──────────┐       ┌──────────┐
│  Auth  │◀──────▶│   Blog   │       │ Shopping │
│Service │   U/D  │ Service  │       │ Service  │
└───┬────┘        └──────────┘       └─────┬────┘
    │                                      │
    │         ┌──────────────┐             │
    └────────▶│ Notification │◀────────────┘
       Event  │   Service    │    Event
              └──────────────┘
```

---

## 4. Portal Universe 서비스 분리 분석

### 4.1 Auth Service (Identity Context)

**책임:**
- 사용자 등록/인증
- OAuth2 토큰 발급
- 역할(Role) 관리

**도메인 엔티티:**

```java
// services/auth-service/.../user/domain/User.java
@Entity
public class User {
    @Id
    private Long id;
    private String email;
    private String password;
    private String nickname;

    @ManyToMany
    private Set<Role> roles;
}
```

**경계 정의 근거:**
- 인증은 독립적인 관심사
- 보안 관련 변경의 격리
- 다른 서비스와 느슨한 결합 (토큰 기반)

### 4.2 Blog Service (Content Context)

**책임:**
- 블로그 포스트 CRUD
- 댓글, 시리즈 관리
- 태그 시스템

**도메인 엔티티:**

```java
// services/blog-service/.../post/domain/Post.java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    private String title;
    private String content;
    private String authorId;  // Auth Service의 User ID 참조

    @Embedded
    private List<Comment> comments;
}
```

**경계 정의 근거:**
- 콘텐츠 관리는 별개의 도메인
- MongoDB 사용 (유연한 스키마)
- Shopping과 독립적인 비즈니스

### 4.3 Shopping Service (Commerce Context)

**책임:**
- 상품 카탈로그
- 장바구니, 주문
- 결제, 쿠폰

**하위 도메인:**

```
Shopping Service
├── Product (상품 관리)
├── Inventory (재고 관리)
├── Order (주문 처리)
├── Payment (결제)
├── Cart (장바구니)
├── Coupon (할인)
└── TimeDeal (타임딜)
```

**복잡한 도메인 처리:**

```java
// 하나의 서비스 내에서 모듈로 분리
services/shopping-service/
├── product/
│   ├── domain/
│   ├── service/
│   └── controller/
├── order/
│   ├── domain/
│   ├── service/
│   └── controller/
└── payment/
    ├── domain/
    ├── service/
    └── controller/
```

### 4.4 Notification Service (Messaging Context)

**책임:**
- 이벤트 구독 (Kafka Consumer)
- 알림 발송 (Email, Push, WebSocket)
- 알림 이력 관리

**경계 정의 근거:**
- 비동기 처리 (이벤트 기반)
- 다른 서비스의 이벤트를 구독
- 독립적인 확장 필요

---

## 5. 서비스 분리 실전 가이드

### 5.1 분리 결정 체크리스트

서비스 분리 여부를 결정할 때 다음을 확인하세요:

| 질문 | Yes → 분리 고려 |
|------|-----------------|
| 독립적으로 배포해야 하는가? | ✅ |
| 다른 확장 요구사항이 있는가? | ✅ |
| 다른 팀이 소유하는가? | ✅ |
| 다른 데이터 저장소가 필요한가? | ✅ |
| 장애를 격리해야 하는가? | ✅ |

### 5.2 분리하지 말아야 할 경우

```
❌ 단순히 코드를 나누고 싶어서
❌ 트랜잭션 경계가 자주 교차
❌ 초당 수백 번의 동기 호출 필요
❌ 팀 규모가 작음 (1-2명)
```

### 5.3 점진적 분리 전략

**Phase 1: 모듈화**
```
monolith/
├── auth-module/
├── blog-module/
└── shopping-module/
```

**Phase 2: 서비스 추출**
```
auth-service/     (독립 서비스)
blog-service/     (독립 서비스)
monolith/
└── shopping-module/
```

**Phase 3: 완전 분리**
```
auth-service/
blog-service/
shopping-service/
notification-service/
```

---

## 6. 서비스 간 데이터 공유

### 6.1 데이터 공유 패턴

**Pattern 1: API 호출**

```java
// Shopping Service가 Auth Service의 사용자 정보 조회
@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable Long userId);
}
```

**Pattern 2: 이벤트 기반 동기화**

```java
// Auth Service가 사용자 생성 이벤트 발행
@KafkaListener(topics = "user.created")
public void handleUserCreated(UserCreatedEvent event) {
    // Shopping Service에서 필요한 사용자 정보 캐시
    userCacheService.save(event.getUserId(), event.getNickname());
}
```

**Pattern 3: 데이터 복제**

```
Auth Service                    Shopping Service
   User                           Customer (복제본)
   ├── id                         ├── userId (FK)
   ├── email                      ├── nickname (복제)
   └── nickname                   └── createdAt
        │
        └──── Event ────────────▶ 동기화
```

### 6.2 Portal Universe의 데이터 공유

| 데이터 | 출처 | 소비자 | 방식 |
|--------|------|--------|------|
| userId | Auth | Blog, Shopping | JWT Claim |
| nickname | Auth | Blog, Shopping | API/Event |
| productId | Shopping | - | 내부 사용 |
| orderId | Shopping | Notification | Kafka Event |

---

## 7. 실습: 서비스 경계 분석

### 실습 1: 도메인 엔티티 경계 확인

```bash
# 각 서비스의 도메인 엔티티 확인
find services/*/src/main/java -name "*.java" -path "*/domain/*" \
  | head -20
```

### 실습 2: 서비스 간 참조 확인

```bash
# Feign Client 확인 (동기 통신)
grep -r "@FeignClient" services/*/src/main/java

# Kafka Listener 확인 (비동기 통신)
grep -r "@KafkaListener" services/*/src/main/java
```

### 실습 3: 공유 라이브러리 분석

```bash
# common-library 의존성 확인
cat services/shopping-service/build.gradle | grep common-library
```

---

## 8. 핵심 요약

### 서비스 분리 원칙

1. **Bounded Context 기반**: DDD의 도메인 경계 활용
2. **비즈니스 역량 중심**: 기술이 아닌 비즈니스 기준
3. **독립 배포 가능**: 다른 서비스 변경 없이 배포
4. **데이터 소유권 명확**: 서비스별 데이터 책임

### Portal Universe 핵심 결정

- **Auth**: 인증/인가 격리, 보안 책임
- **Blog**: 콘텐츠 도메인, MongoDB 선택
- **Shopping**: 핵심 비즈니스, 복잡 도메인 모듈화
- **Notification**: 이벤트 기반 분리

---

## 관련 문서

- [마이크로서비스 개요](./microservices-overview.md)
- [서비스 간 통신](./inter-service-communication.md)
- [분산 데이터 관리](./distributed-data-management.md)
- [Saga 패턴](../patterns/saga-pattern-deep-dive.md)
