# Database ERD Overview

**Last Updated**: 2026-02-06

Portal Universe는 마이크로서비스 아키텍처로 각 서비스가 독립적인 데이터베이스를 운영합니다.

## Service Database Summary

| Service | Database | Purpose | ERD 링크 |
|---------|----------|---------|----------|
| **Shopping Service** | MySQL 8.0 | 쇼핑몰 (상품, 주문, 결제, 배송, 재고) | [shopping-service-schema.md](./shopping-service-schema.md) |
| **Auth Service** | MySQL 8.0 | 인증/인가 (사용자, 역할, 권한, 멤버십) | [auth-service-schema.md](./auth-service-schema.md) |
| **Blog Service** | MongoDB 7.0 | 블로그 (게시물, 댓글, 시리즈, 태그) | [blog-service-schema.md](./blog-service-schema.md) |
| **Notification Service** | MySQL 8.0 | 알림 (실시간 푸시, WebSocket) | [notification-service-schema.md](./notification-service-schema.md) |
| **API Gateway** | - | 라우팅 전용 (DB 없음) | - |
| **Prism Service** | PostgreSQL | AI 기반 서비스 (향후) | - |
| **Chatbot Service** | PostgreSQL | 챗봇 대화 이력 (향후) | - |

## Architecture Overview

```mermaid
graph TB
    subgraph "Auth Service"
        AuthDB[(MySQL<br/>Auth)]
    end

    subgraph "Shopping Service"
        ShoppingDB[(MySQL<br/>Shopping)]
    end

    subgraph "Blog Service"
        BlogDB[(MongoDB<br/>Blog)]
    end

    subgraph "Notification Service"
        NotificationDB[(MySQL<br/>Notification)]
    end

    subgraph "Prism Service"
        PrismDB[(PostgreSQL<br/>Prism)]
    end

    Kafka[Kafka Event Bus]

    AuthDB --> Kafka
    ShoppingDB --> Kafka
    BlogDB --> Kafka
    Kafka --> NotificationDB

    style Kafka fill:#f9f,stroke:#333,stroke-width:4px
```

## Database 선택 근거

### MySQL (Shopping, Auth, Notification)
- **ACID 보장**: 주문/결제/인증 트랜잭션 중요
- **관계형 모델**: 복잡한 조인 쿼리 필요
- **성숙한 생태계**: Spring Data JPA 완벽 지원

### MongoDB (Blog)
- **유연한 스키마**: 게시물 구조 변경 용이
- **Document 모델**: 게시물 + 태그 + 이미지 등 계층 구조
- **전문 검색**: Text Index로 한글 검색 최적화
- **빠른 읽기**: 조회 중심 워크로드

### PostgreSQL (Prism, Chatbot)
- **JSON 지원**: 비정규 데이터 저장 필요
- **고급 기능**: Full-Text Search, Array, JSONB
- **확장성**: AI 기능 확장 대비

## 서비스 간 데이터 참조

### 원칙
- **No Direct DB Access**: 서비스 간 직접 DB 접근 금지
- **API/Event 통신**: REST API 또는 Kafka 이벤트로 통신
- **ID 참조만 저장**: 타 서비스 엔티티의 ID만 저장 (Foreign Key 없음)

### 예시

#### Shopping Service → Auth Service
```java
// Shopping Service: Order 엔티티
@Column(name = "user_id")
private String userId;  // Auth Service의 User UUID 참조

// 사용자 정보 조회: Feign Client 사용
@FeignClient(name = "auth-service")
public interface AuthClient {
    @GetMapping("/api/v1/users/{userId}")
    UserResponse getUser(@PathVariable String userId);
}
```

#### Blog Service → Auth Service
```javascript
// Blog Service: Post 도큐먼트
{
  authorId: "user-uuid-123",  // Auth Service 참조
  authorName: "홍길동"         // 비정규화 (캐시)
}
```

#### Event-Driven Communication
```yaml
# Order 생성 → Notification 발송
Shopping Service:
  - Order 생성
  - Kafka "order-events" 토픽에 이벤트 발행

Notification Service:
  - Kafka "order-events" 구독
  - Notification 엔티티 생성
  - WebSocket으로 실시간 알림 전송
```

## 주요 Entity 개수

### Shopping Service (MySQL)
- **16개 테이블**:
  - 주문: `orders`, `order_items`, `saga_states`
  - 결제: `payments`
  - 배송: `deliveries`, `delivery_histories`
  - 장바구니: `carts`, `cart_items`
  - 재고: `inventory`, `stock_movements`
  - 상품: `products`
  - 쿠폰: `coupons`, `user_coupons`
  - 타임딜: `time_deals`, `time_deal_purchases`
  - 대기열: `queue_entries`

### Auth Service (MySQL)
- **14개 테이블**:
  - 사용자: `users`, `user_profiles`, `social_accounts`, `follows`, `password_histories`
  - RBAC: `roles`, `permissions`, `user_roles`, `role_permissions`
  - 멤버십: `membership_tiers`, `user_memberships`, `membership_tier_permissions`
  - 감사: `auth_audit_logs`
  - 판매자: `seller_applications`

### Blog Service (MongoDB)
- **5개 컬렉션**:
  - `posts`, `comments`, `series`, `tags`, `likes`

### Notification Service (MySQL)
- **1개 테이블**:
  - `notifications`

## 데이터 일관성 전략

### 1. 이벤트 기반 비동기 동기화
```
Shopping: Order 생성
  → Kafka: "order-events"
    → Notification: 알림 생성
    → Blog: 구매 후기 작성 유도
```

### 2. Saga Pattern (분산 트랜잭션)
```
Order 생성 Saga:
1. 재고 예약 (Inventory.reserve)
2. 결제 처리 (Payment.process)
3. 재고 차감 (Inventory.deduct)
4. 배송 생성 (Delivery.create)

실패 시 보상 트랜잭션:
- 재고 해제 (Inventory.release)
- 결제 취소 (Payment.cancel)
```

### 3. 비정규화 (캐싱)
```javascript
// Post에 authorName 캐싱
{
  authorId: "user-uuid-123",
  authorName: "홍길동"  // Auth Service에서 가져와 저장
}

// 장점: 매번 Auth Service 호출 불필요
// 단점: User 이름 변경 시 수동 동기화 필요
```

## 인덱싱 전략

### 복합 인덱스
```sql
-- Shopping Service
CREATE INDEX idx_order_user_status ON orders(user_id, status);
CREATE INDEX idx_cart_user_status ON carts(user_id, status);

-- Auth Service
CREATE INDEX idx_follow_follower_id ON follows(follower_id);
CREATE INDEX idx_follow_following_id ON follows(following_id);

-- Notification Service
CREATE INDEX idx_notification_user_status ON notifications(user_id, status);
```

### Unique 제약
```sql
-- 중복 방지
CREATE UNIQUE INDEX uk_coupon_code ON coupons(code);
CREATE UNIQUE INDEX uk_inventory_product ON inventory(product_id);
CREATE UNIQUE INDEX uk_follow_relationship ON follows(follower_id, following_id);
```

## 백업 및 복구

### RDB (MySQL/PostgreSQL)
- **일일 풀 백업**: 매일 02:00 AM
- **증분 백업**: 4시간마다
- **Binary Log**: 7일 보관
- **Point-in-Time Recovery**: 가능

### MongoDB
- **Replica Set**: 3노드 구성
- **Oplog**: 24시간 보관
- **mongodump**: 매일 백업
- **Atlas Backup**: 프로덕션 환경

## 모니터링

### Metrics
- **Connection Pool**: HikariCP 메트릭
- **Query Performance**: Slow Query Log
- **Replication Lag**: MongoDB Replica Set 지연
- **Disk Usage**: 임계값 알림

### Tools
- **Prometheus + Grafana**: 메트릭 시각화
- **Spring Actuator**: JPA 통계
- **MongoDB Compass**: 쿼리 분석

## 마이그레이션 전략

### Flyway (RDB)
```
services/shopping-service/src/main/resources/db/migration/
  V1__init_schema.sql
  V2__add_saga_state.sql
  V3__add_time_deal.sql
```

### Mongock (MongoDB)
```java
@ChangeSet(order = "001", id = "init-collections", author = "admin")
public void initCollections(MongoDatabase db) {
    db.createCollection("posts");
    db.createCollection("comments");
}
```

## 참고 자료

- [Spring Data JPA Best Practices](https://spring.io/guides/gs/accessing-data-jpa/)
- [MongoDB Schema Design](https://www.mongodb.com/docs/manual/core/data-model-design/)
- [Microservices Data Patterns](https://microservices.io/patterns/data/)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

## 다음 단계

- [ ] Prism Service ERD 작성 (PostgreSQL)
- [ ] Chatbot Service ERD 작성 (PostgreSQL)
- [ ] 서비스별 샘플 데이터 생성 스크립트
- [ ] 데이터베이스 성능 테스트
- [ ] 백업 자동화 스크립트
