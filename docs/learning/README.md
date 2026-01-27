# Portal Universe 학습 자료

Portal Universe 프로젝트의 기술 스택과 아키텍처를 이해하기 위한 포괄적인 학습 자료입니다.

---

## 권장 학습 순서

```
1. 마이크로서비스 기초 → Kafka → Redis → MongoDB → Elasticsearch
2. Shopping 도메인 → Blog 도메인 → Auth 도메인 → Notification 도메인
3. Module Federation → Vue/React 패턴 → Design System
4. API Gateway → 보안
5. 아키텍처 패턴 → Clean Code & 트레이드오프
6. AWS 로컬 개발 → LocalStack → Kubernetes → AWS 배포
```

---

## PART 0: Microservices Fundamentals

위치: `docs/learning/fundamentals/`

### 🎯 마이크로서비스 기초

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [microservices-overview.md](./fundamentals/microservices-overview.md) | MSA 개요, Monolith vs MSA | ⭐ |
| [service-decomposition.md](./fundamentals/service-decomposition.md) | 서비스 분해 전략, Bounded Context | ⭐⭐ |
| [inter-service-communication.md](./fundamentals/inter-service-communication.md) | 동기/비동기 통신, Feign vs Kafka | ⭐⭐ |
| [api-gateway-pattern.md](./fundamentals/api-gateway-pattern.md) | API Gateway 패턴, Routing, Filter | ⭐⭐ |
| [distributed-data-management.md](./fundamentals/distributed-data-management.md) | 분산 데이터 관리, Database per Service | ⭐⭐⭐ |
| [service-discovery.md](./fundamentals/service-discovery.md) | Service Discovery, Eureka, Kubernetes DNS | ⭐⭐ |
| [observability-basics.md](./fundamentals/observability-basics.md) | 관찰 가능성, 메트릭/로깅/추적 | ⭐⭐ |
| [12-factor-app.md](./fundamentals/12-factor-app.md) | 12-Factor App 방법론 | ⭐⭐ |

---

## PART 1: 전체 시스템 학습 자료

### 📚 Apache Kafka

위치: `docs/learning/kafka/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [kafka-introduction.md](./kafka/kafka-introduction.md) | Kafka 아키텍처, Topic, Partition, Consumer Group | ⭐⭐ |
| [kafka-core-concepts.md](./kafka/kafka-core-concepts.md) | Broker, ZooKeeper, Replication, ISR | ⭐⭐ |
| [kafka-producers-deep-dive.md](./kafka/kafka-producers-deep-dive.md) | Producer 설정, Ack, Idempotence | ⭐⭐⭐ |
| [kafka-consumers-deep-dive.md](./kafka/kafka-consumers-deep-dive.md) | Consumer Group, Offset, Rebalancing | ⭐⭐⭐ |
| [kafka-partitioning-strategy.md](./kafka/kafka-partitioning-strategy.md) | Partitioning 전략, Key 설계 | ⭐⭐⭐ |
| [kafka-exactly-once.md](./kafka/kafka-exactly-once.md) | Exactly-Once 시맨틱, Transactional Producer | ⭐⭐⭐⭐ |
| [kafka-schema-evolution.md](./kafka/kafka-schema-evolution.md) | Schema Registry, Avro, 스키마 진화 | ⭐⭐⭐ |
| [kafka-spring-integration.md](./kafka/kafka-spring-integration.md) | @KafkaListener, KafkaTemplate, DLQ, 에러 처리 | ⭐⭐⭐ |
| [kafka-error-handling.md](./kafka/kafka-error-handling.md) | 에러 처리 전략, Retry, DLT | ⭐⭐⭐ |
| [kafka-monitoring.md](./kafka/kafka-monitoring.md) | JMX 메트릭, Lag 모니터링 | ⭐⭐⭐ |
| [kafka-portal-universe.md](./kafka/kafka-portal-universe.md) | Portal Universe Kafka 구조 | ⭐⭐⭐ |
| [kafka-troubleshooting.md](./kafka/kafka-troubleshooting.md) | 자주 발생하는 문제 및 해결책 | ⭐⭐⭐ |

### 📚 Redis

위치: `docs/learning/redis/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [redis-introduction.md](./redis/redis-introduction.md) | 데이터 구조, TTL, 캐싱 패턴 | ⭐⭐ |
| [redis-data-structures.md](./redis/redis-data-structures.md) | String, Hash, List, Set, Sorted Set | ⭐⭐ |
| [redis-caching-patterns.md](./redis/redis-caching-patterns.md) | Cache-Aside, Read-Through, Write-Through | ⭐⭐⭐ |
| [redis-distributed-lock.md](./redis/redis-distributed-lock.md) | Redisson, @DistributedLock, Lua Script | ⭐⭐⭐⭐ |
| [redis-rate-limiting.md](./redis/redis-rate-limiting.md) | Rate Limiting 구현, Sliding Window | ⭐⭐⭐ |
| [redis-pub-sub.md](./redis/redis-pub-sub.md) | Pub/Sub 패턴, 실시간 알림 | ⭐⭐ |
| [redis-persistence.md](./redis/redis-persistence.md) | RDB, AOF, 영속성 전략 | ⭐⭐⭐ |
| [redis-spring-integration.md](./redis/redis-spring-integration.md) | Spring Data Redis, RedisTemplate | ⭐⭐⭐ |
| [redis-best-practices.md](./redis/redis-best-practices.md) | 모범 사례, 성능 최적화 | ⭐⭐⭐ |
| [redis-troubleshooting.md](./redis/redis-troubleshooting.md) | 자주 발생하는 문제 및 해결책 | ⭐⭐⭐ |

### 📚 MongoDB

위치: `docs/learning/mongodb/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [mongodb-introduction.md](./mongodb/mongodb-introduction.md) | Document 모델, Embedding vs Reference | ⭐⭐ |
| [mongodb-data-modeling.md](./mongodb/mongodb-data-modeling.md) | 데이터 모델링 패턴, 1:N 관계 | ⭐⭐⭐ |
| [mongodb-crud-operations.md](./mongodb/mongodb-crud-operations.md) | CRUD 연산, Query 최적화 | ⭐⭐ |
| [mongodb-aggregation.md](./mongodb/mongodb-aggregation.md) | Aggregation Pipeline, $match, $group | ⭐⭐⭐ |
| [mongodb-indexing.md](./mongodb/mongodb-indexing.md) | 인덱스 전략, Compound Index | ⭐⭐⭐ |
| [mongodb-transactions.md](./mongodb/mongodb-transactions.md) | 트랜잭션, ACID 보장 | ⭐⭐⭐ |
| [mongodb-spring-integration.md](./mongodb/mongodb-spring-integration.md) | @Document, Repository, Aggregation | ⭐⭐⭐ |
| [mongodb-best-practices.md](./mongodb/mongodb-best-practices.md) | 모범 사례, 성능 최적화 | ⭐⭐⭐ |

### 📚 PostgreSQL

위치: `docs/learning/postgresql/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [postgresql-introduction.md](./postgresql/postgresql-introduction.md) | PostgreSQL 소개, 특징, Docker 설정 | ⭐ |
| [postgresql-sql-fundamentals.md](./postgresql/postgresql-sql-fundamentals.md) | SQL 기초, DDL/DML/DCL | ⭐⭐ |
| [mysql-vs-postgresql.md](./postgresql/mysql-vs-postgresql.md) | MySQL vs PostgreSQL 비교, 선택 기준 | ⭐⭐⭐ |
| [postgresql-data-types.md](./postgresql/postgresql-data-types.md) | JSONB, Array, UUID | ⭐⭐⭐ |
| [postgresql-indexing.md](./postgresql/postgresql-indexing.md) | B-Tree, GIN, GiST | ⭐⭐⭐ |
| [postgresql-transactions.md](./postgresql/postgresql-transactions.md) | ACID, MVCC, Deadlock | ⭐⭐⭐ |
| [postgresql-spring-integration.md](./postgresql/postgresql-spring-integration.md) | Spring Boot + JPA | ⭐⭐⭐ |
| [postgresql-jsonb.md](./postgresql/postgresql-jsonb.md) | JSONB 연산자, 인덱싱 | ⭐⭐⭐ |
| [postgresql-advanced-features.md](./postgresql/postgresql-advanced-features.md) | CTE, Window Function, Full-Text | ⭐⭐⭐⭐ |
| [postgresql-performance-tuning.md](./postgresql/postgresql-performance-tuning.md) | EXPLAIN, 쿼리 최적화 | ⭐⭐⭐⭐ |
| [postgresql-migration.md](./postgresql/postgresql-migration.md) | MySQL → PostgreSQL 마이그레이션 | ⭐⭐⭐⭐ |
| [postgresql-best-practices.md](./postgresql/postgresql-best-practices.md) | 모범 사례 | ⭐⭐⭐ |

### 📚 Elasticsearch

위치: `docs/learning/elasticsearch/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [es-introduction.md](./elasticsearch/es-introduction.md) | 역인덱스, Nori 분석기, Query DSL | ⭐⭐⭐ |
| [es-core-concepts.md](./elasticsearch/es-core-concepts.md) | Index, Document, Mapping, Shard | ⭐⭐⭐ |
| [es-query-dsl.md](./elasticsearch/es-query-dsl.md) | Match, Term, Bool Query | ⭐⭐⭐ |
| [es-aggregations.md](./elasticsearch/es-aggregations.md) | Bucket, Metric Aggregations | ⭐⭐⭐ |
| [es-nori-analyzer.md](./elasticsearch/es-nori-analyzer.md) | Nori 형태소 분석기, 한국어 검색 | ⭐⭐⭐ |
| [es-spring-integration.md](./elasticsearch/es-spring-integration.md) | Spring Data Elasticsearch | ⭐⭐⭐ |
| [es-performance-tuning.md](./elasticsearch/es-performance-tuning.md) | 성능 최적화, Refresh Interval | ⭐⭐⭐⭐ |
| [es-portal-universe.md](./elasticsearch/es-portal-universe.md) | 상품 검색 구현, 자동완성, Faceted Search | ⭐⭐⭐⭐ |

---

## PART 2: Shopping Service

위치: `services/shopping-service/docs/learning/`

### 📦 도메인 설계

| 문서 | 주제 |
|------|------|
| [shopping-domain-overview.md](../../services/shopping-service/docs/learning/domain/shopping-domain-overview.md) | 전체 도메인 맵, Aggregate Root |
| [product-domain.md](../../services/shopping-service/docs/learning/domain/product-domain.md) | 상품 도메인, SKU, Option |
| [order-domain.md](../../services/shopping-service/docs/learning/domain/order-domain.md) | Order 상태 머신, 스냅샷 패턴 |
| [payment-domain.md](../../services/shopping-service/docs/learning/domain/payment-domain.md) | Payment 도메인, PG 연동 |
| [inventory-domain.md](../../services/shopping-service/docs/learning/domain/inventory-domain.md) | 3단계 재고 모델, StockMovement |
| [cart-domain.md](../../services/shopping-service/docs/learning/domain/cart-domain.md) | 장바구니 도메인, CartItem |
| [coupon-domain.md](../../services/shopping-service/docs/learning/domain/coupon-domain.md) | 쿠폰 도메인, 발급/사용 정책 |
| [timedeal-domain.md](../../services/shopping-service/docs/learning/domain/timedeal-domain.md) | 타임딜 도메인, Flash Sale |
| [category-domain.md](../../services/shopping-service/docs/learning/domain/category-domain.md) | 카테고리 도메인, 계층 구조 |
| [brand-domain.md](../../services/shopping-service/docs/learning/domain/brand-domain.md) | 브랜드 도메인 |
| [review-domain.md](../../services/shopping-service/docs/learning/domain/review-domain.md) | 리뷰 도메인, 평점 계산 |
| [shipping-domain.md](../../services/shopping-service/docs/learning/domain/shipping-domain.md) | 배송 도메인, 배송 정책 |

### 🗄️ 데이터베이스

| 문서 | 주제 |
|------|------|
| [shopping-erd.md](../../services/shopping-service/docs/learning/database/shopping-erd.md) | ERD, 테이블 관계, 인덱스 전략 |
| [jpa-entity-mapping.md](../../services/shopping-service/docs/learning/database/jpa-entity-mapping.md) | JPA Entity 매핑, 연관관계 |
| [optimistic-locking.md](../../services/shopping-service/docs/learning/database/optimistic-locking.md) | Optimistic Lock, @Version |
| [pessimistic-locking.md](../../services/shopping-service/docs/learning/database/pessimistic-locking.md) | Pessimistic Lock, SELECT FOR UPDATE |
| [connection-pooling.md](../../services/shopping-service/docs/learning/database/connection-pooling.md) | HikariCP, Connection Pool 최적화 |
| [query-optimization.md](../../services/shopping-service/docs/learning/database/query-optimization.md) | N+1 문제, Fetch Join |
| [transaction-isolation.md](../../services/shopping-service/docs/learning/database/transaction-isolation.md) | 트랜잭션 격리 수준 |
| [soft-delete-audit.md](../../services/shopping-service/docs/learning/database/soft-delete-audit.md) | Soft Delete, 감사 추적 |

### 💼 비즈니스 로직

| 문서 | 주제 |
|------|------|
| [order-flow.md](../../services/shopping-service/docs/learning/business/order-flow.md) | 주문 전체 흐름, Saga 단계 |
| [payment-integration.md](../../services/shopping-service/docs/learning/business/payment-integration.md) | PG 연동, Webhook 처리 |
| [inventory-concurrency.md](../../services/shopping-service/docs/learning/business/inventory-concurrency.md) | 재고 동시성 제어, 분산 락 |
| [coupon-issuance.md](../../services/shopping-service/docs/learning/business/coupon-issuance.md) | 쿠폰 발급 로직, 선착순 처리 |
| [timedeal-flash-sale.md](../../services/shopping-service/docs/learning/business/timedeal-flash-sale.md) | 플래시 세일 구현 |
| [price-calculation.md](../../services/shopping-service/docs/learning/business/price-calculation.md) | 가격 계산, 할인 적용 |
| [order-saga.md](../../services/shopping-service/docs/learning/business/order-saga.md) | Order Saga 패턴, 보상 트랜잭션 |
| [notification-integration.md](../../services/shopping-service/docs/learning/business/notification-integration.md) | 알림 서비스 연동 |
| [shipping-calculation.md](../../services/shopping-service/docs/learning/business/shipping-calculation.md) | 배송비 계산 |
| [cart-management.md](../../services/shopping-service/docs/learning/business/cart-management.md) | 장바구니 관리 로직 |

### 📡 이벤트

| 문서 | 주제 |
|------|------|
| [event-producer.md](../../services/shopping-service/docs/learning/events/event-producer.md) | Event Producer, KafkaTemplate |
| [event-consumer.md](../../services/shopping-service/docs/learning/events/event-consumer.md) | Event Consumer, @KafkaListener |
| [outbox-pattern.md](../../services/shopping-service/docs/learning/events/outbox-pattern.md) | Outbox Pattern, 트랜잭션 보장 |
| [event-driven-architecture.md](../../services/shopping-service/docs/learning/events/event-driven-architecture.md) | 이벤트 기반 아키텍처 |
| [domain-events.md](../../services/shopping-service/docs/learning/events/domain-events.md) | Domain Event 설계 |
| [event-versioning.md](../../services/shopping-service/docs/learning/events/event-versioning.md) | Event 버전 관리 |

### 🔎 검색

| 문서 | 주제 |
|------|------|
| [product-search.md](../../services/shopping-service/docs/learning/search/product-search.md) | 상품 검색 구현 |
| [search-indexing.md](../../services/shopping-service/docs/learning/search/search-indexing.md) | Elasticsearch 인덱싱 |
| [search-autocomplete.md](../../services/shopping-service/docs/learning/search/search-autocomplete.md) | 자동완성 구현 |
| [faceted-search.md](../../services/shopping-service/docs/learning/search/faceted-search.md) | Faceted Search, 필터링 |
| [search-ranking.md](../../services/shopping-service/docs/learning/search/search-ranking.md) | 검색 랭킹 알고리즘 |
| [search-performance.md](../../services/shopping-service/docs/learning/search/search-performance.md) | 검색 성능 최적화 |

### 🔗 API

| 문서 | 주제 |
|------|------|
| [rest-api-design.md](../../services/shopping-service/docs/learning/api/rest-api-design.md) | REST API 설계 원칙 |
| [dto-validation.md](../../services/shopping-service/docs/learning/api/dto-validation.md) | DTO Validation, @Valid |
| [error-response.md](../../services/shopping-service/docs/learning/api/error-response.md) | 에러 응답 설계 |
| [pagination.md](../../services/shopping-service/docs/learning/api/pagination.md) | 페이징 처리 |
| [api-versioning.md](../../services/shopping-service/docs/learning/api/api-versioning.md) | API 버전 관리 |
| [openapi-swagger.md](../../services/shopping-service/docs/learning/api/openapi-swagger.md) | OpenAPI, Swagger 문서화 |

---

## PART 2.5: Auth Service

위치: `services/auth-service/docs/learning/`

### 🔐 OAuth2

| 문서 | 주제 |
|------|------|
| [oauth2-server-setup.md](../../services/auth-service/docs/learning/oauth2/oauth2-server-setup.md) | OAuth2 Authorization Server 설정 |
| [oauth2-grant-types.md](../../services/auth-service/docs/learning/oauth2/oauth2-grant-types.md) | Grant Types (Authorization Code, Client Credentials) |
| [oauth2-token-management.md](../../services/auth-service/docs/learning/oauth2/oauth2-token-management.md) | Access Token, Refresh Token |
| [oauth2-scope.md](../../services/auth-service/docs/learning/oauth2/oauth2-scope.md) | Scope 설계, 권한 관리 |
| [oauth2-pkce.md](../../services/auth-service/docs/learning/oauth2/oauth2-pkce.md) | PKCE (Proof Key for Code Exchange) |
| [oauth2-client-registration.md](../../services/auth-service/docs/learning/oauth2/oauth2-client-registration.md) | Client 등록, 관리 |
| [oauth2-security.md](../../services/auth-service/docs/learning/oauth2/oauth2-security.md) | OAuth2 보안 모범 사례 |
| [oauth2-testing.md](../../services/auth-service/docs/learning/oauth2/oauth2-testing.md) | OAuth2 테스트 |
| [oauth2-migration.md](../../services/auth-service/docs/learning/oauth2/oauth2-migration.md) | 기존 시스템에서 OAuth2로 마이그레이션 |
| [oauth2-troubleshooting.md](../../services/auth-service/docs/learning/oauth2/oauth2-troubleshooting.md) | 자주 발생하는 문제 및 해결책 |

### 👤 사용자 관리

| 문서 | 주제 |
|------|------|
| [user-registration.md](../../services/auth-service/docs/learning/user/user-registration.md) | 사용자 회원가입 |
| [user-authentication.md](../../services/auth-service/docs/learning/user/user-authentication.md) | 인증 처리 |
| [user-profile.md](../../services/auth-service/docs/learning/user/user-profile.md) | 사용자 프로필 관리 |
| [password-management.md](../../services/auth-service/docs/learning/user/password-management.md) | 비밀번호 관리, BCrypt |
| [email-verification.md](../../services/auth-service/docs/learning/user/email-verification.md) | 이메일 인증 |
| [social-login.md](../../services/auth-service/docs/learning/user/social-login.md) | 소셜 로그인 (Google, Kakao) |

### 🛡️ 보안

| 문서 | 주제 |
|------|------|
| [spring-security-config.md](../../services/auth-service/docs/learning/security/spring-security-config.md) | Spring Security 설정 |
| [jwt-implementation.md](../../services/auth-service/docs/learning/security/jwt-implementation.md) | JWT 구현 |
| [role-based-access.md](../../services/auth-service/docs/learning/security/role-based-access.md) | RBAC (Role-Based Access Control) |
| [security-best-practices.md](../../services/auth-service/docs/learning/security/security-best-practices.md) | 보안 모범 사례 |

---

## PART 3: Blog Service

위치: `services/blog-service/docs/learning/`

### 📦 도메인 설계

| 문서 | 주제 |
|------|------|
| [blog-domain-overview.md](../../services/blog-service/docs/learning/domain/blog-domain-overview.md) | Document 구조, 역정규화 전략 |
| [post-domain.md](../../services/blog-service/docs/learning/domain/post-domain.md) | 게시글 도메인 |
| [comment-domain.md](../../services/blog-service/docs/learning/domain/comment-domain.md) | 댓글 도메인, 계층 구조 |
| [tag-domain.md](../../services/blog-service/docs/learning/domain/tag-domain.md) | 태그 도메인 |
| [category-domain.md](../../services/blog-service/docs/learning/domain/category-domain.md) | 카테고리 도메인 |
| [like-domain.md](../../services/blog-service/docs/learning/domain/like-domain.md) | 좋아요 도메인 |
| [view-count-domain.md](../../services/blog-service/docs/learning/domain/view-count-domain.md) | 조회수 도메인 |
| [user-follow-domain.md](../../services/blog-service/docs/learning/domain/user-follow-domain.md) | 팔로우 도메인 |

### 🗄️ MongoDB

| 문서 | 주제 |
|------|------|
| [blog-mongodb-schema.md](../../services/blog-service/docs/learning/mongodb/blog-mongodb-schema.md) | MongoDB 스키마 설계 |
| [embedded-vs-reference.md](../../services/blog-service/docs/learning/mongodb/embedded-vs-reference.md) | Embedding vs Referencing |
| [mongodb-aggregation-blog.md](../../services/blog-service/docs/learning/mongodb/mongodb-aggregation-blog.md) | Aggregation 활용 |
| [mongodb-performance.md](../../services/blog-service/docs/learning/mongodb/mongodb-performance.md) | 성능 최적화 |
| [mongodb-indexing-strategy.md](../../services/blog-service/docs/learning/mongodb/mongodb-indexing-strategy.md) | 인덱스 전략 |
| [mongodb-change-streams.md](../../services/blog-service/docs/learning/mongodb/mongodb-change-streams.md) | Change Streams, 실시간 업데이트 |
| [mongodb-backup-restore.md](../../services/blog-service/docs/learning/mongodb/mongodb-backup-restore.md) | 백업 및 복구 |
| [mongodb-sharding.md](../../services/blog-service/docs/learning/mongodb/mongodb-sharding.md) | Sharding, 수평 확장 |

### 🎨 기능 구현

| 문서 | 주제 |
|------|------|
| [post-crud.md](../../services/blog-service/docs/learning/features/post-crud.md) | 게시글 CRUD |
| [comment-hierarchy.md](../../services/blog-service/docs/learning/features/comment-hierarchy.md) | 댓글 계층 구조 구현 |
| [tag-search.md](../../services/blog-service/docs/learning/features/tag-search.md) | 태그 검색 |
| [feed-generation.md](../../services/blog-service/docs/learning/features/feed-generation.md) | 피드 생성 알고리즘 |
| [content-moderation.md](../../services/blog-service/docs/learning/features/content-moderation.md) | 콘텐츠 검열 |
| [markdown-processing.md](../../services/blog-service/docs/learning/features/markdown-processing.md) | Markdown 처리 |
| [image-upload.md](../../services/blog-service/docs/learning/features/image-upload.md) | 이미지 업로드 (S3) |
| [seo-optimization.md](../../services/blog-service/docs/learning/features/seo-optimization.md) | SEO 최적화 |

---

## PART 3.5: Notification Service

위치: `services/notification-service/docs/learning/`

### 📡 Kafka 통합

| 문서 | 주제 |
|------|------|
| [notification-kafka-consumer.md](../../services/notification-service/docs/learning/kafka/notification-kafka-consumer.md) | Kafka Consumer 구현 |
| [event-routing.md](../../services/notification-service/docs/learning/kafka/event-routing.md) | Event Routing 전략 |
| [retry-strategy.md](../../services/notification-service/docs/learning/kafka/retry-strategy.md) | Retry 전략 |
| [dead-letter-queue.md](../../services/notification-service/docs/learning/kafka/dead-letter-queue.md) | Dead Letter Queue |
| [consumer-scaling.md](../../services/notification-service/docs/learning/kafka/consumer-scaling.md) | Consumer 스케일링 |
| [idempotency.md](../../services/notification-service/docs/learning/kafka/idempotency.md) | 멱등성 보장 |

### 🌐 WebSocket

| 문서 | 주제 |
|------|------|
| [websocket-setup.md](../../services/notification-service/docs/learning/websocket/websocket-setup.md) | WebSocket 설정 |
| [stomp-protocol.md](../../services/notification-service/docs/learning/websocket/stomp-protocol.md) | STOMP 프로토콜 |
| [user-subscriptions.md](../../services/notification-service/docs/learning/websocket/user-subscriptions.md) | 사용자별 구독 관리 |
| [push-notifications.md](../../services/notification-service/docs/learning/websocket/push-notifications.md) | Push 알림 |
| [connection-management.md](../../services/notification-service/docs/learning/websocket/connection-management.md) | 연결 관리, 재연결 |
| [websocket-security.md](../../services/notification-service/docs/learning/websocket/websocket-security.md) | WebSocket 보안 |

### 🔔 알림 처리

| 문서 | 주제 |
|------|------|
| [notification-types.md](../../services/notification-service/docs/learning/notification/notification-types.md) | 알림 타입 (이메일, SMS, Push) |
| [notification-template.md](../../services/notification-service/docs/learning/notification/notification-template.md) | 알림 템플릿 |
| [notification-preferences.md](../../services/notification-service/docs/learning/notification/notification-preferences.md) | 사용자 알림 설정 |
| [notification-history.md](../../services/notification-service/docs/learning/notification/notification-history.md) | 알림 이력 관리 |

---

## PART 4: API Gateway

위치: `services/api-gateway/docs/learning/`

### 🌐 Gateway & 보안

| 문서 | 주제 |
|------|------|
| [spring-cloud-gateway.md](../../services/api-gateway/docs/learning/gateway/spring-cloud-gateway.md) | Route, Predicate, Filter, StripPrefix |
| [circuit-breaker.md](../../services/api-gateway/docs/learning/gateway/circuit-breaker.md) | Resilience4j, 상태 전이, Fallback |
| [jwt-validation.md](../../services/api-gateway/docs/learning/gateway/jwt-validation.md) | JWT 검증, 접근 제어, CORS |
| [rate-limiting.md](../../services/api-gateway/docs/learning/gateway/rate-limiting.md) | Rate Limiting, Redis 기반 |
| [request-logging.md](../../services/api-gateway/docs/learning/gateway/request-logging.md) | 요청/응답 로깅 |
| [request-transformation.md](../../services/api-gateway/docs/learning/gateway/request-transformation.md) | 요청 변환 |
| [response-caching.md](../../services/api-gateway/docs/learning/gateway/response-caching.md) | 응답 캐싱 |
| [load-balancing.md](../../services/api-gateway/docs/learning/gateway/load-balancing.md) | 로드 밸런싱 |
| [retry-timeout.md](../../services/api-gateway/docs/learning/gateway/retry-timeout.md) | Retry, Timeout 설정 |
| [gateway-monitoring.md](../../services/api-gateway/docs/learning/gateway/gateway-monitoring.md) | Gateway 모니터링 |
| [gateway-security.md](../../services/api-gateway/docs/learning/gateway/gateway-security.md) | Gateway 보안 |
| [gateway-performance.md](../../services/api-gateway/docs/learning/gateway/gateway-performance.md) | Gateway 성능 최적화 |

---

## PART 5: Frontend

### 🎨 Module Federation

| 문서 | 주제 | 위치 |
|------|------|------|
| [module-federation-host.md](../../frontend/portal-shell/docs/learning/mfe/module-federation-host.md) | Host 설정, Remote 로딩, 공유 리소스 | portal-shell |
| [module-federation-remote.md](../../frontend/shopping-frontend/docs/learning/mfe/module-federation-remote.md) | Bootstrap 패턴, Keep-Alive, 스타일 격리 | shopping-frontend |
| [mfe-communication.md](../../frontend/portal-shell/docs/learning/mfe/mfe-communication.md) | MFE 간 통신 패턴 | portal-shell |
| [shared-dependencies.md](../../frontend/portal-shell/docs/learning/mfe/shared-dependencies.md) | 공유 의존성 관리 | portal-shell |
| [dynamic-remote-loading.md](../../frontend/portal-shell/docs/learning/mfe/dynamic-remote-loading.md) | 동적 Remote 로딩 | portal-shell |
| [mfe-routing.md](../../frontend/portal-shell/docs/learning/mfe/mfe-routing.md) | MFE 라우팅 전략 | portal-shell |
| [mfe-error-handling.md](../../frontend/portal-shell/docs/learning/mfe/mfe-error-handling.md) | MFE 에러 처리 | portal-shell |

### 🟦 Vue 3 (Portal Shell)

위치: `frontend/portal-shell/docs/learning/vue/`

| 문서 | 주제 |
|------|------|
| [composition-api.md](../../frontend/portal-shell/docs/learning/vue/composition-api.md) | Composition API, setup |
| [pinia-state.md](../../frontend/portal-shell/docs/learning/vue/pinia-state.md) | Pinia Store |
| [vue-router.md](../../frontend/portal-shell/docs/learning/vue/vue-router.md) | Vue Router 4 |
| [composables.md](../../frontend/portal-shell/docs/learning/vue/composables.md) | Composables 패턴 |
| [lifecycle-hooks.md](../../frontend/portal-shell/docs/learning/vue/lifecycle-hooks.md) | Lifecycle Hooks |
| [reactivity-system.md](../../frontend/portal-shell/docs/learning/vue/reactivity-system.md) | Reactivity System |
| [component-design.md](../../frontend/portal-shell/docs/learning/vue/component-design.md) | Component 설계 |
| [vue-performance.md](../../frontend/portal-shell/docs/learning/vue/vue-performance.md) | Vue 성능 최적화 |

### ⚛️ React 18 (Shopping Frontend)

#### 기초 학습 가이드

위치: `frontend/shopping-frontend/docs/learning/`

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [README.md](../../frontend/shopping-frontend/docs/learning/README.md) | React 학습 가이드 마스터, 4주 로드맵, 개발 환경 설정 | ⭐ |
| [01-project-structure.md](../../frontend/shopping-frontend/docs/learning/01-project-structure.md) | 프로젝트 구조, 디렉토리 역할, 진입점 차이 (main.tsx vs bootstrap.tsx) | ⭐ |
| [02-react-basics.md](../../frontend/shopping-frontend/docs/learning/02-react-basics.md) | JSX 문법, 함수형 컴포넌트, Props, 조건부 렌더링, 리스트, 이벤트 | ⭐⭐ |
| [03-hooks.md](../../frontend/shopping-frontend/docs/learning/03-hooks.md) | useState, useEffect, useCallback, useMemo, Custom Hooks | ⭐⭐⭐ |
| [04-state-management.md](../../frontend/shopping-frontend/docs/learning/04-state-management.md) | Zustand Store 생성, 선택적 구독, Persist 미들웨어, 비동기 액션 | ⭐⭐⭐ |
| [05-routing.md](../../frontend/shopping-frontend/docs/learning/05-routing.md) | React Router, 중첩 라우트, Protected Routes, URL 파라미터, Query String | ⭐⭐ |
| [06-styling.md](../../frontend/shopping-frontend/docs/learning/06-styling.md) | Tailwind CSS Utility 클래스, 반응형 디자인, 다크모드 | ⭐⭐ |
| [07-module-federation.md](../../frontend/shopping-frontend/docs/learning/07-module-federation.md) | Micro Frontend, Host/Remote 구조, 의존성 공유, 컨텍스트 주입 | ⭐⭐⭐⭐ |

#### 심화 학습 (계획)

위치: `frontend/shopping-frontend/docs/learning/react/`

| 문서 | 주제 |
|------|------|
| [hooks-deep-dive.md](../../frontend/shopping-frontend/docs/learning/react/hooks-deep-dive.md) | useState, useEffect, useCallback, useMemo 심화 |
| [zustand-state.md](../../frontend/shopping-frontend/docs/learning/react/zustand-state.md) | Zustand Store, 미들웨어, 최적화 |
| [react-router.md](../../frontend/shopping-frontend/docs/learning/react/react-router.md) | React Router 6 심화 |
| [custom-hooks.md](../../frontend/shopping-frontend/docs/learning/react/custom-hooks.md) | Custom Hooks 패턴 |
| [context-api.md](../../frontend/shopping-frontend/docs/learning/react/context-api.md) | Context API |
| [error-boundaries.md](../../frontend/shopping-frontend/docs/learning/react/error-boundaries.md) | Error Boundaries |
| [code-splitting.md](../../frontend/shopping-frontend/docs/learning/react/code-splitting.md) | Code Splitting, Lazy Loading |
| [react-performance.md](../../frontend/shopping-frontend/docs/learning/react/react-performance.md) | React 성능 최적화 |
| [testing-react.md](../../frontend/shopping-frontend/docs/learning/react/testing-react.md) | React Testing Library |

### 🛍️ Shopping Frontend 기능

위치: `frontend/shopping-frontend/docs/learning/shopping/`

| 문서 | 주제 |
|------|------|
| [product-list.md](../../frontend/shopping-frontend/docs/learning/shopping/product-list.md) | 상품 목록 구현 |
| [product-detail.md](../../frontend/shopping-frontend/docs/learning/shopping/product-detail.md) | 상품 상세 구현 |
| [cart-management.md](../../frontend/shopping-frontend/docs/learning/shopping/cart-management.md) | 장바구니 관리 |
| [checkout-flow.md](../../frontend/shopping-frontend/docs/learning/shopping/checkout-flow.md) | 결제 플로우 |
| [order-tracking.md](../../frontend/shopping-frontend/docs/learning/shopping/order-tracking.md) | 주문 추적 |
| [search-ui.md](../../frontend/shopping-frontend/docs/learning/shopping/search-ui.md) | 검색 UI |

---

## PART 6: 아키텍처 패턴

위치: `docs/learning/patterns/`

### 🏗️ 핵심 패턴

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [saga-pattern-deep-dive.md](./patterns/saga-pattern-deep-dive.md) | Orchestration vs Choreography, 보상 로직 | ⭐⭐⭐⭐ |
| [state-machine-pattern.md](./patterns/state-machine-pattern.md) | Order/Payment 상태 전이, Guard 조건 | ⭐⭐⭐ |
| [event-sourcing.md](./patterns/event-sourcing.md) | Event Sourcing 패턴 | ⭐⭐⭐⭐ |
| [cqrs-pattern.md](./patterns/cqrs-pattern.md) | CQRS (Command Query Responsibility Segregation) | ⭐⭐⭐⭐ |
| [outbox-pattern-deep.md](./patterns/outbox-pattern-deep.md) | Outbox Pattern 심화 | ⭐⭐⭐⭐ |
| [strangler-fig-pattern.md](./patterns/strangler-fig-pattern.md) | Strangler Fig, 점진적 마이그레이션 | ⭐⭐⭐ |
| [bulkhead-pattern.md](./patterns/bulkhead-pattern.md) | Bulkhead Pattern, 격리 | ⭐⭐⭐ |
| [portal-universe-patterns.md](./patterns/portal-universe-patterns.md) | 프로젝트 전체 패턴 총정리 | ⭐⭐⭐⭐ |

---

## PART 7: Clean Code & 아키텍처

위치: `docs/learning/clean-code/`

### 📐 설계 원칙

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [solid-principles.md](./clean-code/principles/solid-principles.md) | SOLID 5원칙 (SRP, OCP, LSP, ISP, DIP) | ⭐⭐⭐ |
| [dry-kiss-yagni.md](./clean-code/principles/dry-kiss-yagni.md) | DRY, KISS, YAGNI 실용적 설계 원칙 | ⭐⭐ |
| [clean-code-naming.md](./clean-code/principles/clean-code-naming.md) | 의미 있는 이름 짓기 | ⭐⭐ |
| [clean-code-functions.md](./clean-code/principles/clean-code-functions.md) | 함수 설계 원칙 (크기, 인자, CQS) | ⭐⭐⭐ |
| [clean-code-comments.md](./clean-code/principles/clean-code-comments.md) | 주석 작성 가이드, JavaDoc | ⭐⭐ |
| [error-handling-patterns.md](./clean-code/principles/error-handling-patterns.md) | 에러 처리 패턴, ErrorCode Enum | ⭐⭐⭐ |

### 🏛️ 아키텍처

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [layered-architecture.md](./clean-code/architecture/layered-architecture.md) | Layered Architecture | ⭐⭐ |
| [hexagonal-architecture.md](./clean-code/architecture/hexagonal-architecture.md) | Hexagonal Architecture (Ports & Adapters) | ⭐⭐⭐ |
| [ddd-basics.md](./clean-code/architecture/ddd-basics.md) | DDD 기초 (Aggregate, Entity, Value Object) | ⭐⭐⭐ |

### 🧪 테스트

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [unit-testing.md](./clean-code/testing/unit-testing.md) | 단위 테스트, JUnit 5, Mockito | ⭐⭐ |
| [integration-testing.md](./clean-code/testing/integration-testing.md) | 통합 테스트, Testcontainers | ⭐⭐⭐ |
| [e2e-testing.md](./clean-code/testing/e2e-testing.md) | E2E 테스트, Playwright | ⭐⭐⭐ |

### 🔧 리팩토링

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [refactoring-techniques.md](./clean-code/refactoring/refactoring-techniques.md) | 5가지 핵심 리팩토링 기법 (Extract Method, Rename, Magic Number, Parameter Object, Polymorphism) | ⭐⭐⭐ |
| [code-review-checklist.md](./clean-code/refactoring/code-review-checklist.md) | 코드 리뷰 체크리스트 (가독성, 성능, 보안, 테스트, 아키텍처) | ⭐⭐⭐ |

### ⚖️ 트레이드오프

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [trade-offs.md](./clean-code/trade-offs.md) | 아키텍처 트레이드오프 분석 | ⭐⭐⭐⭐ |

---

## PART 8: AWS 로컬 개발

위치: `docs/learning/aws/`

AWS 클라우드 서비스와 LocalStack을 활용한 로컬 개발 환경 가이드입니다.

### ☁️ AWS 기초

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [aws-overview.md](./aws/fundamentals/aws-overview.md) | AWS 개요, 핵심 서비스 카테고리 | ⭐ |
| [region-az.md](./aws/fundamentals/region-az.md) | 리전, 가용영역, 서울 리전 | ⭐ |
| [aws-cli-setup.md](./aws/fundamentals/aws-cli-setup.md) | AWS CLI 설치, 프로필, awslocal | ⭐⭐ |

### 🔐 IAM (Identity & Access Management)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [iam-introduction.md](./aws/iam/iam-introduction.md) | User, Role, Policy 개념 | ⭐⭐ |
| [iam-policies.md](./aws/iam/iam-policies.md) | Policy 문법, 최소 권한 원칙 | ⭐⭐⭐ |
| [iam-best-practices.md](./aws/iam/iam-best-practices.md) | Credentials 관리, MFA | ⭐⭐ |

### 📦 S3 (Simple Storage Service)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [s3-introduction.md](./aws/s3/s3-introduction.md) | 객체 스토리지, 버킷 구조 | ⭐⭐ |
| [s3-operations.md](./aws/s3/s3-operations.md) | CRUD, Pre-signed URL, Multipart | ⭐⭐ |
| [s3-sdk-integration.md](./aws/s3/s3-sdk-integration.md) | AWS SDK v2, Spring Boot 통합 | ⭐⭐⭐ |
| [s3-permissions.md](./aws/s3/s3-permissions.md) | 버킷 정책, ACL, CORS | ⭐⭐⭐ |
| [s3-best-practices.md](./aws/s3/s3-best-practices.md) | 키 네이밍, 스토리지 클래스 | ⭐⭐ |

### 💻 EC2 (Elastic Compute Cloud)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [ec2-introduction.md](./aws/ec2/ec2-introduction.md) | 가상 서버, 인스턴스 타입 | ⭐⭐ |
| [ec2-vs-kubernetes.md](./aws/ec2/ec2-vs-kubernetes.md) | EC2 vs ECS vs EKS 비교 | ⭐⭐⭐ |

### 🧪 LocalStack (핵심)

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [localstack-setup.md](./aws/localstack/localstack-setup.md) | Docker Compose 설정, 환경 변수 | ⭐⭐ |
| [localstack-persistence.md](./aws/localstack/localstack-persistence.md) | **데이터 영속성 문제 해결** ⭐핵심⭐ | ⭐⭐⭐ |
| [localstack-services.md](./aws/localstack/localstack-services.md) | S3, SQS, SNS, DynamoDB 설정 | ⭐⭐ |
| [localstack-troubleshooting.md](./aws/localstack/localstack-troubleshooting.md) | 자주 발생하는 문제 및 해결책 | ⭐⭐ |

### 🚀 배포 파이프라인

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [environment-profiles.md](./aws/deployment/environment-profiles.md) | local/docker/k8s 환경별 설정 | ⭐⭐⭐ |
| [local-to-kubernetes.md](./aws/deployment/local-to-kubernetes.md) | 로컬 → Docker → K8s 전환 | ⭐⭐⭐ |
| [kubernetes-to-aws.md](./aws/deployment/kubernetes-to-aws.md) | K8s → AWS 마이그레이션 | ⭐⭐⭐⭐ |

### ✅ 모범 사례

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [aws-migration-checklist.md](./aws/best-practices/aws-migration-checklist.md) | LocalStack → AWS 체크리스트 | ⭐⭐⭐ |

---

## PART 9: Monitoring & Observability

위치: `docs/learning/infra/`

### 🐳 Docker & Kubernetes

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [docker-fundamentals.md](./infra/docker-fundamentals.md) | Docker 기초, 이미지, 컨테이너 | ⭐⭐ |
| [docker-compose.md](./infra/docker-compose.md) | Docker Compose, 멀티 컨테이너 | ⭐⭐ |
| [kubernetes-fundamentals.md](./infra/kubernetes-fundamentals.md) | Kubernetes 기초, Pod, Service | ⭐⭐⭐ |
| [kubernetes-deployment.md](./infra/kubernetes-deployment.md) | Deployment, ReplicaSet | ⭐⭐⭐ |
| [kubernetes-config.md](./infra/kubernetes-config.md) | ConfigMap, Secret | ⭐⭐⭐ |

### 📊 메트릭 & 시각화

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [prometheus-grafana.md](./infra/prometheus-grafana.md) | Prometheus 메트릭 수집, PromQL, Grafana 대시보드, Alerting | ⭐⭐⭐ |
| [actuator-metrics.md](./infra/actuator-metrics.md) | Spring Boot Actuator, Micrometer | ⭐⭐ |

### 📋 로깅

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [loki-logging.md](./infra/loki-logging.md) | Loki 아키텍처, Promtail, LogQL, Spring Boot JSON 로그 | ⭐⭐⭐ |

### 🔗 분산 추적

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [zipkin-tracing.md](./infra/zipkin-tracing.md) | Zipkin 분산 추적, Trace 연동 | ⭐⭐⭐ |

---

## PART 10: Security & Authentication

위치: `docs/learning/security/`

### 🔐 보안 기초

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [oauth2-fundamentals.md](./security/oauth2-fundamentals.md) | OAuth2 기초, Grant Types | ⭐⭐ |
| [jwt-deep-dive.md](./security/jwt-deep-dive.md) | JWT 구조, Claims, 서명 검증 | ⭐⭐⭐ |
| [spring-security-architecture.md](./security/spring-security-architecture.md) | Spring Security 아키텍처 | ⭐⭐⭐ |
| [api-gateway-security.md](./security/api-gateway-security.md) | API Gateway 보안 | ⭐⭐⭐ |
| [cors-csrf.md](./security/cors-csrf.md) | CORS, CSRF 방어 | ⭐⭐ |
| [encryption-hashing.md](./security/encryption-hashing.md) | 암호화, 해싱 (AES, RSA, BCrypt) | ⭐⭐⭐ |
| [security-best-practices.md](./security/security-best-practices.md) | 보안 모범 사례 | ⭐⭐⭐ |

---

## PART 11: Design System

위치: `docs/learning/design-system/`

### 🎨 Design Tokens

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [design-tokens.md](./design-system/design-tokens.md) | Design Token 개념, 3-tier 구조 | ⭐⭐ |
| [token-implementation.md](./design-system/design-tokens/token-implementation.md) | Token 구현 (CSS Variables) | ⭐⭐ |

### 🧩 Components

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [component-library.md](./design-system/components/component-library.md) | Component Library 구조 | ⭐⭐ |
| [button-component.md](./design-system/components/button-component.md) | Button Component 설계 | ⭐⭐ |
| [form-components.md](./design-system/components/form-components.md) | Form Components | ⭐⭐⭐ |

### 🎭 Patterns

| 문서 | 주제 | 난이도 |
|------|------|--------|
| [design-patterns.md](./design-system/patterns/design-patterns.md) | UI 패턴 | ⭐⭐ |
| [responsive-design.md](./design-system/patterns/responsive-design.md) | 반응형 디자인 | ⭐⭐ |
| [accessibility.md](./design-system/patterns/accessibility.md) | 접근성 (a11y) | ⭐⭐⭐ |

---

## 기존 학습 노트

### 구현 패턴

| 파일명 | 주제 | 관련 서비스 |
|--------|------|-------------|
| [admin-implementation-patterns.md](./admin-implementation-patterns.md) | Admin 기능 구현 패턴 | shopping-service |

### 학습 노트

| 번호 | 파일명 | 주제 | 핵심 기술 |
|----|--------|------|-----------|
| 01 | [01-domain-model.md](./notes/01-domain-model.md) | 도메인 모델 설계 | DDD, Entity 설계 |
| 02 | [02-saga-pattern.md](./notes/02-saga-pattern.md) | Saga 패턴 | Orchestration, 분산 트랜잭션 |
| 03 | [03-concurrency-control.md](./notes/03-concurrency-control.md) | 동시성 제어 | Pessimistic/Optimistic Lock |
| 04 | [04-snapshot-pattern.md](./notes/04-snapshot-pattern.md) | 스냅샷 패턴 | 가격 스냅샷, 이력 관리 |
| 05 | [05-react-fundamentals.md](./notes/05-react-fundamentals.md) | React 기초 | Hooks, Context API |
| 06 | [06-shopping-frontend-implementation.md](./notes/06-shopping-frontend-implementation.md) | Shopping Frontend 구현 | Module Federation, React Router |
| 07 | [07-security-cryptography.md](./notes/07-security-cryptography.md) | 암호화 개념 | AES, RSA, BCrypt, JWT, PKCE, OAuth2 |
| 08 | [08-redis-lua-script-atomicity.md](./notes/08-redis-lua-script-atomicity.md) | Redis Lua 원자성 | Lua Script, 동시성 제어, 선착순 처리 |

---

## 주제별 인덱스

### Fundamentals
- **Microservices**: [개요](./fundamentals/microservices-overview.md) | [서비스 분해](./fundamentals/service-decomposition.md) | [통신](./fundamentals/inter-service-communication.md) | [API Gateway](./fundamentals/api-gateway-pattern.md) | [데이터 관리](./fundamentals/distributed-data-management.md) | [Service Discovery](./fundamentals/service-discovery.md) | [Observability](./fundamentals/observability-basics.md) | [12-Factor](./fundamentals/12-factor-app.md)

### Backend Infrastructure
- **Kafka**: [소개](./kafka/kafka-introduction.md) | [핵심 개념](./kafka/kafka-core-concepts.md) | [Producer](./kafka/kafka-producers-deep-dive.md) | [Consumer](./kafka/kafka-consumers-deep-dive.md) | [Partitioning](./kafka/kafka-partitioning-strategy.md) | [Exactly-Once](./kafka/kafka-exactly-once.md) | [Schema](./kafka/kafka-schema-evolution.md) | [Spring 통합](./kafka/kafka-spring-integration.md) | [에러 처리](./kafka/kafka-error-handling.md) | [모니터링](./kafka/kafka-monitoring.md) | [적용 사례](./kafka/kafka-portal-universe.md) | [트러블슈팅](./kafka/kafka-troubleshooting.md)
- **Redis**: [소개](./redis/redis-introduction.md) | [데이터 구조](./redis/redis-data-structures.md) | [캐싱 패턴](./redis/redis-caching-patterns.md) | [분산 락](./redis/redis-distributed-lock.md) | [Rate Limiting](./redis/redis-rate-limiting.md) | [Pub/Sub](./redis/redis-pub-sub.md) | [영속성](./redis/redis-persistence.md) | [Spring 통합](./redis/redis-spring-integration.md) | [모범 사례](./redis/redis-best-practices.md) | [트러블슈팅](./redis/redis-troubleshooting.md)
- **MongoDB**: [소개](./mongodb/mongodb-introduction.md) | [데이터 모델링](./mongodb/mongodb-data-modeling.md) | [CRUD](./mongodb/mongodb-crud-operations.md) | [Aggregation](./mongodb/mongodb-aggregation.md) | [인덱싱](./mongodb/mongodb-indexing.md) | [트랜잭션](./mongodb/mongodb-transactions.md) | [Spring 통합](./mongodb/mongodb-spring-integration.md) | [모범 사례](./mongodb/mongodb-best-practices.md)
- **PostgreSQL**: [소개](./postgresql/postgresql-introduction.md) | [SQL 기초](./postgresql/postgresql-sql-fundamentals.md) | [MySQL 비교](./postgresql/mysql-vs-postgresql.md) | [데이터 타입](./postgresql/postgresql-data-types.md) | [인덱싱](./postgresql/postgresql-indexing.md) | [트랜잭션](./postgresql/postgresql-transactions.md) | [Spring 통합](./postgresql/postgresql-spring-integration.md) | [JSONB](./postgresql/postgresql-jsonb.md) | [고급 기능](./postgresql/postgresql-advanced-features.md) | [성능 튜닝](./postgresql/postgresql-performance-tuning.md) | [마이그레이션](./postgresql/postgresql-migration.md) | [모범 사례](./postgresql/postgresql-best-practices.md)
- **Elasticsearch**: [소개](./elasticsearch/es-introduction.md) | [핵심 개념](./elasticsearch/es-core-concepts.md) | [Query DSL](./elasticsearch/es-query-dsl.md) | [Aggregations](./elasticsearch/es-aggregations.md) | [Nori](./elasticsearch/es-nori-analyzer.md) | [Spring 통합](./elasticsearch/es-spring-integration.md) | [성능 튜닝](./elasticsearch/es-performance-tuning.md) | [상품 검색](./elasticsearch/es-portal-universe.md)

### Domain Design
- **Shopping**: [개요](../../services/shopping-service/docs/learning/domain/shopping-domain-overview.md) | [Product](../../services/shopping-service/docs/learning/domain/product-domain.md) | [Order](../../services/shopping-service/docs/learning/domain/order-domain.md) | [Payment](../../services/shopping-service/docs/learning/domain/payment-domain.md) | [Inventory](../../services/shopping-service/docs/learning/domain/inventory-domain.md) | [Cart](../../services/shopping-service/docs/learning/domain/cart-domain.md) | [Coupon](../../services/shopping-service/docs/learning/domain/coupon-domain.md) | [Timedeal](../../services/shopping-service/docs/learning/domain/timedeal-domain.md) | [Category](../../services/shopping-service/docs/learning/domain/category-domain.md) | [Brand](../../services/shopping-service/docs/learning/domain/brand-domain.md) | [Review](../../services/shopping-service/docs/learning/domain/review-domain.md) | [Shipping](../../services/shopping-service/docs/learning/domain/shipping-domain.md)
- **Blog**: [개요](../../services/blog-service/docs/learning/domain/blog-domain-overview.md) | [Post](../../services/blog-service/docs/learning/domain/post-domain.md) | [Comment](../../services/blog-service/docs/learning/domain/comment-domain.md) | [Tag](../../services/blog-service/docs/learning/domain/tag-domain.md) | [Category](../../services/blog-service/docs/learning/domain/category-domain.md) | [Like](../../services/blog-service/docs/learning/domain/like-domain.md) | [View Count](../../services/blog-service/docs/learning/domain/view-count-domain.md) | [Follow](../../services/blog-service/docs/learning/domain/user-follow-domain.md)

### API Gateway & Security
- **Gateway**: [Spring Cloud Gateway](../../services/api-gateway/docs/learning/gateway/spring-cloud-gateway.md) | [Circuit Breaker](../../services/api-gateway/docs/learning/gateway/circuit-breaker.md) | [JWT 검증](../../services/api-gateway/docs/learning/gateway/jwt-validation.md) | [Rate Limiting](../../services/api-gateway/docs/learning/gateway/rate-limiting.md) | [로깅](../../services/api-gateway/docs/learning/gateway/request-logging.md) | [변환](../../services/api-gateway/docs/learning/gateway/request-transformation.md) | [캐싱](../../services/api-gateway/docs/learning/gateway/response-caching.md) | [로드 밸런싱](../../services/api-gateway/docs/learning/gateway/load-balancing.md) | [Retry/Timeout](../../services/api-gateway/docs/learning/gateway/retry-timeout.md) | [모니터링](../../services/api-gateway/docs/learning/gateway/gateway-monitoring.md) | [보안](../../services/api-gateway/docs/learning/gateway/gateway-security.md) | [성능](../../services/api-gateway/docs/learning/gateway/gateway-performance.md)
- **Security**: [OAuth2 기초](./security/oauth2-fundamentals.md) | [JWT](./security/jwt-deep-dive.md) | [Spring Security](./security/spring-security-architecture.md) | [Gateway 보안](./security/api-gateway-security.md) | [CORS/CSRF](./security/cors-csrf.md) | [암호화](./security/encryption-hashing.md) | [모범 사례](./security/security-best-practices.md)

### Frontend
- **MFE**: [Host (Vue)](../../frontend/portal-shell/docs/learning/mfe/module-federation-host.md) | [Remote (React)](../../frontend/shopping-frontend/docs/learning/mfe/module-federation-remote.md) | [통신](../../frontend/portal-shell/docs/learning/mfe/mfe-communication.md) | [공유 의존성](../../frontend/portal-shell/docs/learning/mfe/shared-dependencies.md) | [동적 로딩](../../frontend/portal-shell/docs/learning/mfe/dynamic-remote-loading.md) | [라우팅](../../frontend/portal-shell/docs/learning/mfe/mfe-routing.md) | [에러 처리](../../frontend/portal-shell/docs/learning/mfe/mfe-error-handling.md)
- **Vue**: [Composition API](../../frontend/portal-shell/docs/learning/vue/composition-api.md) | [Pinia](../../frontend/portal-shell/docs/learning/vue/pinia-state.md) | [Router](../../frontend/portal-shell/docs/learning/vue/vue-router.md) | [Composables](../../frontend/portal-shell/docs/learning/vue/composables.md) | [Lifecycle](../../frontend/portal-shell/docs/learning/vue/lifecycle-hooks.md) | [Reactivity](../../frontend/portal-shell/docs/learning/vue/reactivity-system.md) | [Component](../../frontend/portal-shell/docs/learning/vue/component-design.md) | [성능](../../frontend/portal-shell/docs/learning/vue/vue-performance.md)
- **React 기초**: [학습 가이드](../../frontend/shopping-frontend/docs/learning/README.md) | [프로젝트 구조](../../frontend/shopping-frontend/docs/learning/01-project-structure.md) | [React 기초](../../frontend/shopping-frontend/docs/learning/02-react-basics.md) | [Hooks](../../frontend/shopping-frontend/docs/learning/03-hooks.md) | [Zustand](../../frontend/shopping-frontend/docs/learning/04-state-management.md) | [Router](../../frontend/shopping-frontend/docs/learning/05-routing.md) | [Tailwind CSS](../../frontend/shopping-frontend/docs/learning/06-styling.md) | [Module Federation](../../frontend/shopping-frontend/docs/learning/07-module-federation.md)
- **React 심화**: [Hooks 심화](../../frontend/shopping-frontend/docs/learning/react/hooks-deep-dive.md) | [Zustand 심화](../../frontend/shopping-frontend/docs/learning/react/zustand-state.md) | [Router 심화](../../frontend/shopping-frontend/docs/learning/react/react-router.md) | [Custom Hooks](../../frontend/shopping-frontend/docs/learning/react/custom-hooks.md) | [Context](../../frontend/shopping-frontend/docs/learning/react/context-api.md) | [Error Boundaries](../../frontend/shopping-frontend/docs/learning/react/error-boundaries.md) | [Code Splitting](../../frontend/shopping-frontend/docs/learning/react/code-splitting.md) | [성능](../../frontend/shopping-frontend/docs/learning/react/react-performance.md) | [테스트](../../frontend/shopping-frontend/docs/learning/react/testing-react.md)
- **Design System**: [Design Tokens](./design-system/design-tokens.md) | [Token 구현](./design-system/design-tokens/token-implementation.md) | [Component Library](./design-system/components/component-library.md) | [Button](./design-system/components/button-component.md) | [Form](./design-system/components/form-components.md) | [패턴](./design-system/patterns/design-patterns.md) | [반응형](./design-system/patterns/responsive-design.md) | [접근성](./design-system/patterns/accessibility.md)

### Architecture Patterns
- **Patterns**: [Saga 심화](./patterns/saga-pattern-deep-dive.md) | [State Machine](./patterns/state-machine-pattern.md) | [Event Sourcing](./patterns/event-sourcing.md) | [CQRS](./patterns/cqrs-pattern.md) | [Outbox](./patterns/outbox-pattern-deep.md) | [Strangler Fig](./patterns/strangler-fig-pattern.md) | [Bulkhead](./patterns/bulkhead-pattern.md) | [전체 정리](./patterns/portal-universe-patterns.md)
- **Clean Code**: [SOLID](./clean-code/principles/solid-principles.md) | [DRY/KISS/YAGNI](./clean-code/principles/dry-kiss-yagni.md) | [네이밍](./clean-code/principles/clean-code-naming.md) | [함수](./clean-code/principles/clean-code-functions.md) | [주석](./clean-code/principles/clean-code-comments.md) | [에러 처리](./clean-code/principles/error-handling-patterns.md) | [Layered](./clean-code/architecture/layered-architecture.md) | [Hexagonal](./clean-code/architecture/hexagonal-architecture.md) | [DDD](./clean-code/architecture/ddd-basics.md) | [Unit Test](./clean-code/testing/unit-testing.md) | [Integration Test](./clean-code/testing/integration-testing.md) | [E2E Test](./clean-code/testing/e2e-testing.md) | [리팩토링](./clean-code/refactoring/refactoring-techniques.md) | [코드 리뷰](./clean-code/refactoring/code-review-checklist.md) | [트레이드오프](./clean-code/trade-offs.md)

### AWS & LocalStack
- **AWS 기초**: [개요](./aws/fundamentals/aws-overview.md) | [리전/AZ](./aws/fundamentals/region-az.md) | [CLI 설정](./aws/fundamentals/aws-cli-setup.md)
- **IAM**: [소개](./aws/iam/iam-introduction.md) | [정책](./aws/iam/iam-policies.md) | [모범 사례](./aws/iam/iam-best-practices.md)
- **S3**: [소개](./aws/s3/s3-introduction.md) | [연산](./aws/s3/s3-operations.md) | [SDK 통합](./aws/s3/s3-sdk-integration.md) | [권한](./aws/s3/s3-permissions.md) | [모범 사례](./aws/s3/s3-best-practices.md)
- **EC2**: [소개](./aws/ec2/ec2-introduction.md) | [EC2 vs K8s](./aws/ec2/ec2-vs-kubernetes.md)
- **LocalStack**: [설정](./aws/localstack/localstack-setup.md) | [영속성 ⭐](./aws/localstack/localstack-persistence.md) | [서비스별](./aws/localstack/localstack-services.md) | [트러블슈팅](./aws/localstack/localstack-troubleshooting.md)
- **배포**: [환경 프로필](./aws/deployment/environment-profiles.md) | [로컬→K8s](./aws/deployment/local-to-kubernetes.md) | [K8s→AWS](./aws/deployment/kubernetes-to-aws.md) | [마이그레이션 체크리스트](./aws/best-practices/aws-migration-checklist.md)

### Monitoring & Observability
- **Infra**: [Docker 기초](./infra/docker-fundamentals.md) | [Docker Compose](./infra/docker-compose.md) | [K8s 기초](./infra/kubernetes-fundamentals.md) | [Deployment](./infra/kubernetes-deployment.md) | [Config](./infra/kubernetes-config.md)
- **메트릭**: [Prometheus & Grafana](./infra/prometheus-grafana.md) | [Actuator](./infra/actuator-metrics.md)
- **로깅**: [Loki Logging](./infra/loki-logging.md)
- **추적**: [Zipkin Tracing](./infra/zipkin-tracing.md)

---

## 문서 통계

| 카테고리 | 문서 수 |
|----------|---------|
| Fundamentals | 8개 |
| 인프라 (Kafka, Redis, MongoDB, PostgreSQL, ES) | 50개 |
| Security | 7개 |
| Patterns | 8개 |
| Shopping Service | 47개 |
| Blog Service | 24개 |
| Auth Service | 20개 |
| Notification Service | 16개 |
| API Gateway | 12개 |
| Frontend (Portal Shell, Shopping) | 42개 |
| Design System | 8개 |
| Clean Code & 리팩토링 | 15개 |
| AWS & LocalStack | 25개 |
| Monitoring & Infra | 10개 |
| 학습 노트 | 8개 |
| 기타 | 3개 |
| **총계** | **300개** |

---

## 관련 문서

- [Scenarios 목록](../scenarios/README.md) - 업무 시나리오 문서
- [ADR 목록](../adr/README.md) - 아키텍처 결정 기록
- [Architecture](../architecture/) - 아키텍처 설계 문서
- [PRD](../prd/) - 제품 요구사항 문서
