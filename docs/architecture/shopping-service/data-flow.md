---
id: arch-data-flow
title: Shopping Service Data Flow
type: architecture
status: current
created: 2026-01-18
updated: 2026-02-06
author: Laze
tags: [architecture, shopping-service, data-flow, saga, events, redis]
related:
  - arch-system-overview
  - arch-saga-pattern
  - arch-coupon-system
  - arch-timedeal-system
  - arch-queue-system
  - arch-search-system
---

# Shopping Service Data Flow

## 개요

Shopping Service의 주요 데이터 흐름과 이벤트 처리 과정을 설명합니다. 10개 도메인의 핵심 플로우를 다루며, Saga 패턴 분산 트랜잭션, Redis Lua Script 동시성 제어, SSE 실시간 스트리밍을 포함합니다.

Saga 패턴 상세는 [saga-pattern.md](./saga-pattern.md)를 참조하세요.

---

## 주요 데이터 흐름

### 1. 상품 조회

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant PS as ProductService
    participant DB as MySQL

    C->>G: GET /api/v1/shopping/products
    G->>PS: Forward Request
    PS->>DB: SELECT * FROM products
    DB-->>PS: Product List
    PS-->>G: ProductResponse[]
    G-->>C: 200 OK
```

**설명**:
1. 클라이언트가 상품 목록 조회 요청
2. API Gateway가 JWT 검증 후 ProductService로 라우팅
3. ProductService가 DB에서 활성 상품 조회
4. 상품 정보를 DTO로 변환하여 반환

---

### 2. 장바구니 추가

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant CS as CartService
    participant PS as ProductService
    participant DB as MySQL

    C->>G: POST /api/v1/shopping/cart/items
    Note over C,G: {productId, quantity}
    G->>CS: Forward with userId (from JWT)
    CS->>PS: Get Product Info
    PS-->>CS: Product
    CS->>DB: BEGIN TRANSACTION
    CS->>DB: SELECT cart WHERE userId AND status=ACTIVE
    alt Cart exists
        CS->>DB: INSERT cart_item
    else Cart not exists
        CS->>DB: INSERT cart
        CS->>DB: INSERT cart_item
    end
    CS->>DB: COMMIT
    DB-->>CS: Success
    CS-->>G: CartResponse
    G-->>C: 200 OK
```

**설명**:
1. 클라이언트가 장바구니에 상품 추가 요청
2. JWT에서 userId 추출
3. 사용자의 활성 장바구니 조회 (없으면 생성)
4. 상품 정보 조회 (가격 스냅샷 저장)
5. CartItem 추가 및 저장
6. 이미 같은 상품이 있으면 S106 에러 발생

---

### 3. 주문 생성 (Saga Pattern)

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant OS as OrderService
    participant SAGA as OrderSagaOrchestrator
    participant IS as InventoryService
    participant EP as EventPublisher
    participant K as Kafka
    participant DB as MySQL

    C->>G: POST /api/v1/shopping/orders
    Note over C,G: {cartId, shippingAddress}
    G->>OS: Create Order
    OS->>DB: BEGIN TRANSACTION
    OS->>DB: Get Cart with Items
    OS->>DB: Create Order with Items
    OS->>DB: Cart status = CHECKED_OUT
    OS->>DB: COMMIT

    OS->>SAGA: startSaga(order)
    SAGA->>DB: Create SagaState (STARTED)

    Note over SAGA,IS: Step 1: Reserve Inventory
    SAGA->>IS: reserveStockBatch(quantities)
    IS->>DB: SELECT inventory FOR UPDATE
    IS->>DB: available -= quantity, reserved += quantity
    IS->>DB: INSERT stock_movement (RESERVATION)
    IS-->>SAGA: Success

    SAGA->>DB: SagaState.currentStep = PROCESS_PAYMENT
    SAGA-->>OS: Saga Started

    OS->>EP: publishOrderCreated(order)
    EP->>K: Send to shopping.order.created
    OS-->>G: OrderResponse
    G-->>C: 201 Created
```

**설명**: [Saga Pattern 상세](./saga-pattern.md) 참조
1. 장바구니 항목으로 주문 생성 (PENDING)
2. OrderSagaOrchestrator가 Saga 시작
3. **Step 1**: Pessimistic Lock으로 재고 예약 (available -> reserved)
4. OrderCreatedEvent 발행

---

### 4. 결제 처리 및 Saga 완료

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant PS as PaymentService
    participant PG as MockPGClient
    participant SAGA as OrderSagaOrchestrator
    participant IS as InventoryService
    participant DS as DeliveryService
    participant EP as EventPublisher
    participant K as Kafka
    participant DB as MySQL

    C->>G: POST /api/v1/shopping/payments
    Note over C,G: {orderNumber, paymentMethod}
    G->>PS: Process Payment
    PS->>DB: Get Order
    PS->>DB: Create Payment (PENDING -> PROCESSING)

    PS->>PG: processPayment(amount, method)
    alt Payment Success (90%)
        PG-->>PS: Success + transactionId
        PS->>DB: Payment status = COMPLETED

        PS->>SAGA: completeSagaAfterPayment(orderNumber)

        Note over SAGA,IS: Step 3: Deduct Inventory
        SAGA->>IS: deductStockBatch(quantities)
        IS->>DB: reserved -= quantity, total -= quantity
        IS->>DB: INSERT stock_movement (SALE)

        Note over SAGA,DS: Step 4: Create Delivery
        SAGA->>DS: createDelivery(order)
        DS->>DB: INSERT delivery (PREPARING)

        Note over SAGA: Step 5: Confirm Order
        SAGA->>DB: Order status = PAID
        SAGA->>DB: SagaState status = COMPLETED

        PS->>EP: publishPaymentCompleted(payment)
        EP->>K: Send to shopping.payment.completed
        PS-->>G: PaymentResponse
        G-->>C: 200 OK

    else Payment Failed (10%)
        PG-->>PS: Failed + reason
        PS->>DB: Payment status = FAILED

        PS->>SAGA: compensate(sagaState, reason)
        SAGA->>IS: releaseStockBatch(quantities)
        IS->>DB: reserved -= quantity, available += quantity
        SAGA->>DB: Order status = CANCELLED
        SAGA->>DB: SagaState status = FAILED

        PS->>EP: publishPaymentFailed(payment)
        EP->>K: Send to shopping.payment.failed
        PS-->>G: 400 Payment Failed
        G-->>C: 400 Bad Request
    end
```

---

### 5. 주문 취소

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant OS as OrderService
    participant PS as PaymentService
    participant IS as InventoryService
    participant EP as EventPublisher
    participant K as Kafka
    participant DB as MySQL

    C->>G: PUT /api/v1/shopping/orders/{orderNumber}/cancel
    Note over C,G: {reason}
    G->>OS: Cancel Order
    OS->>DB: Get Order

    alt Order status = PENDING
        OS->>DB: Order status = CANCELLED
        OS->>IS: releaseStockBatch(quantities)
        IS->>DB: reserved -= quantity, available += quantity
        OS->>EP: publishOrderCancelled(order)
        EP->>K: Send to shopping.order.cancelled
        OS-->>G: 200 OK

    else Order status = PAID
        OS->>PS: Refund Payment
        PS->>DB: Payment status = REFUNDED
        PS->>DB: Order status = REFUNDED
        OS->>IS: returnStock(quantities)
        IS->>DB: available += quantity, total += quantity
        OS->>EP: publishOrderCancelled(order)
        OS-->>G: 200 OK

    else Order status = SHIPPING/DELIVERED
        OS-->>G: 400 Cannot Cancel (S203)
    end

    G-->>C: Response
```

---

### 6. 배송 추적

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant DS as DeliveryService
    participant DB as MySQL

    C->>G: GET /api/v1/shopping/deliveries/{trackingNumber}
    G->>DS: Get Delivery Status
    DS->>DB: SELECT delivery WITH histories
    DB-->>DS: Delivery + DeliveryHistory[]
    DS-->>G: DeliveryResponse
    G-->>C: 200 OK

    Note over C,DS: Update Status
    C->>G: PUT /api/v1/shopping/deliveries/{trackingNumber}/status
    Note over C,G: {status, location, description}
    G->>DS: Update Status
    DS->>DB: Update delivery.status
    DS->>DB: INSERT delivery_history
    DS-->>G: 200 OK
    G-->>C: 200 OK
```

---

### 7. 쿠폰 선착순 발급

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant CS as CouponService
    participant RD as CouponRedisService
    participant Redis as Redis
    participant DB as MySQL
    participant K as Kafka

    C->>G: POST /api/v1/shopping/coupons/{couponId}/issue
    G->>CS: issueCoupon(couponId, userId)

    CS->>RD: issueCoupon(couponId, userId, maxQuantity)
    RD->>Redis: EVAL coupon_issue.lua
    Note over Redis: 1. SISMEMBER issued → 이미 발급?
    Note over Redis: 2. GET stock → 재고 확인
    Note over Redis: 3. DECR stock → 재고 감소
    Note over Redis: 4. SADD issued → 사용자 기록

    alt Lua return = 1 (성공)
        Redis-->>RD: 1
        RD-->>CS: SUCCESS
        CS->>DB: BEGIN TRANSACTION
        CS->>DB: Coupon.issuedQuantity += 1
        CS->>DB: INSERT user_coupons (AVAILABLE)
        CS->>DB: COMMIT
        CS->>K: CouponIssuedEvent
        CS-->>G: UserCouponResponse
        G-->>C: 201 Created

    else Lua return = 0 (재고 소진)
        Redis-->>RD: 0
        RD-->>CS: EXHAUSTED
        CS-->>G: Error S602
        G-->>C: 409 Conflict

    else Lua return = -1 (이미 발급)
        Redis-->>RD: -1
        RD-->>CS: ALREADY_ISSUED
        CS-->>G: Error S604
        G-->>C: 409 Conflict
    end
```

**핵심**: Lua Script로 "중복 확인 + 재고 감소 + 사용자 기록"을 원자적으로 수행. 상세는 [coupon-system.md](./coupon-system.md) 참조.

---

### 8. 타임딜 구매

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant TS as TimeDealService
    participant RD as TimeDealRedisService
    participant Redis as Redis
    participant DB as MySQL

    C->>G: POST /api/v1/shopping/time-deals/purchase
    Note over C,G: {timeDealProductId, quantity}
    G->>TS: purchaseTimeDeal(userId, request)

    TS->>RD: purchaseProduct(dealId, productId, userId, qty, maxPerUser)
    RD->>Redis: EVAL timedeal_purchase.lua
    Note over Redis: 1. GET purchased → 사용자 구매 수량
    Note over Redis: 2. current + qty > max? → 제한 확인
    Note over Redis: 3. GET stock → 재고 확인
    Note over Redis: 4. DECRBY stock → 재고 감소
    Note over Redis: 5. INCRBY purchased → 구매 수량 증가

    alt Lua return > 0 (성공, 남은 재고)
        Redis-->>RD: remainingStock
        RD-->>TS: SUCCESS
        TS->>DB: BEGIN TRANSACTION
        TS->>DB: TimeDealProduct.soldQuantity += qty
        TS->>DB: INSERT time_deal_purchases
        TS->>DB: COMMIT
        TS-->>G: TimeDealPurchaseResponse
        G-->>C: 201 Created

    else Lua return = 0 (재고 소진)
        Redis-->>RD: 0
        TS-->>G: Error S704 TIMEDEAL_SOLD_OUT
        G-->>C: 409 Conflict

    else Lua return = -1 (구매 제한 초과)
        Redis-->>RD: -1
        TS-->>G: Error S705 TIMEDEAL_PURCHASE_LIMIT_EXCEEDED
        G-->>C: 409 Conflict
    end
```

**핵심**: Lua Script로 "구매 한도 확인 + 재고 감소 + 구매 수량 기록"을 원자적으로 수행. 상세는 [timedeal-system.md](./timedeal-system.md) 참조.

---

### 9. 대기열 입장 및 실시간 알림

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant QS as QueueService
    participant SSE as QueueStreamController
    participant Redis as Redis
    participant DB as MySQL

    Note over C,DB: 1. 대기열 입장
    C->>G: POST /api/v1/shopping/queue/{eventType}/{eventId}/enter
    G->>QS: enterQueue(eventType, eventId, userId)
    QS->>DB: INSERT queue_entries (WAITING, entryToken=UUID)
    QS->>Redis: ZADD queue:waiting:{type}:{id} {timestamp} {userId}
    QS->>Redis: ZRANK queue:waiting:{type}:{id} {userId}
    Redis-->>QS: position
    QS-->>G: QueueStatusResponse (position, estimatedWait, token)
    G-->>C: 200 OK

    Note over C,SSE: 2. SSE 구독
    C->>SSE: GET /queue/{type}/{id}/subscribe/{token}
    SSE-->>C: SSE Connection (5min timeout)

    loop 3초마다
        SSE->>Redis: ZRANK (순번 조회)
        Redis-->>SSE: position
        SSE-->>C: event: queue-status {position, estimatedWait}
    end

    Note over SSE,Redis: 3. 입장 처리 (스케줄러/수동)
    QS->>Redis: SCARD entered → 현재 입장 인원
    QS->>Redis: ZPOPMIN waiting → 상위 N명
    QS->>DB: QueueEntry status = ENTERED
    QS->>Redis: SADD entered → 입장 기록

    SSE->>Redis: ZRANK → null (대기열에 없음)
    SSE-->>C: event: queue-entered
    SSE-->>C: Connection Close
```

**핵심**: Redis Sorted Set으로 순번 관리, SSE로 실시간 상태 전달. 상세는 [queue-system.md](./queue-system.md) 참조.

---

### 10. 상품 검색

```mermaid
sequenceDiagram
    participant C as Client
    participant G as API Gateway
    participant SS as ProductSearchService
    participant SG as SuggestService
    participant ES as Elasticsearch
    participant Redis as Redis

    Note over C,Redis: 자동완성
    C->>G: GET /api/v1/shopping/search/suggest?keyword=아이
    G->>SG: suggest("아이", 5)
    SG->>ES: Completion Suggester (name.suggest, fuzzy)
    ES-->>SG: suggestions[]
    SG-->>G: ["아이폰", "아이패드", "아이맥"]
    G-->>C: 200 OK

    Note over C,Redis: 상품 검색
    C->>G: GET /api/v1/shopping/search?keyword=아이폰&minPrice=500000
    G->>SS: search(request)
    SS->>ES: Multi-match (name^3, description) + price range filter
    ES-->>SS: SearchHits with highlights
    SS-->>G: SearchResponse (products, highlights)
    G-->>C: 200 OK

    par 검색 기록 저장
        SS->>Redis: ZINCRBY search:popular "아이폰" 1
        SS->>Redis: LPUSH search:recent:{userId} "아이폰"
        SS->>Redis: LTRIM search:recent:{userId} 0 19
    end
```

**핵심**: Elasticsearch full-text 검색 + Completion Suggester 자동완성 + Redis 인기/최근 검색어. 상세는 [search-system.md](./search-system.md) 참조.

---

## 이벤트/메시지 흐름

### Kafka Topics

```mermaid
graph LR
    subgraph "Shopping Service (Publisher)"
        OS[OrderService]
        PS[PaymentService]
        IS[InventoryService]
        DS[DeliveryService]
        CS[CouponService]
        TS[TimeDealScheduler]
    end

    subgraph "Kafka Topics"
        T1[shopping.order.created]
        T2[shopping.order.confirmed]
        T3[shopping.order.cancelled]
        T4[shopping.payment.completed]
        T5[shopping.payment.failed]
        T6[shopping.inventory.reserved]
        T7[shopping.delivery.shipped]
        T8[shopping.coupon.issued]
        T9[shopping.timedeal.started]
    end

    subgraph "Consumers"
        NS[NotificationService]
        AS[AnalyticsService]
    end

    OS --> T1 & T2 & T3
    PS --> T4 & T5
    IS --> T6
    DS --> T7
    CS --> T8
    TS --> T9

    T1 & T2 & T3 & T4 & T5 --> NS
    T1 & T2 & T3 & T4 & T5 & T8 & T9 --> AS
```

### 이벤트 목록

| 이벤트 | Topic | 발행자 | Payload |
|--------|-------|--------|---------|
| OrderCreatedEvent | `shopping.order.created` | OrderService | orderNumber, userId, totalAmount, items |
| OrderConfirmedEvent | `shopping.order.confirmed` | OrderService | orderNumber, userId |
| OrderCancelledEvent | `shopping.order.cancelled` | OrderService | orderNumber, userId, reason |
| PaymentCompletedEvent | `shopping.payment.completed` | PaymentService | paymentNumber, orderNumber, amount |
| PaymentFailedEvent | `shopping.payment.failed` | PaymentService | paymentNumber, orderNumber, reason |
| InventoryReservedEvent | `shopping.inventory.reserved` | InventoryService | orderNumber, productId, quantity |
| DeliveryShippedEvent | `shopping.delivery.shipped` | DeliveryService | trackingNumber, orderNumber |
| CouponIssuedEvent | `shopping.coupon.issued` | CouponService | couponId, userId, couponCode |
| TimeDealStartedEvent | `shopping.timedeal.started` | TimeDealScheduler | timeDealId, name, startsAt |

---

## 동시성 제어

### 1. Pessimistic Lock (재고 관리)

```java
// InventoryRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
```

**시나리오**: 두 사용자가 동시에 같은 상품 주문

```mermaid
sequenceDiagram
    participant U1 as User 1
    participant U2 as User 2
    participant IS as InventoryService
    participant DB as MySQL

    Note over DB: availableQuantity = 1

    U1->>IS: Reserve 1
    U2->>IS: Reserve 1

    IS->>DB: SELECT FOR UPDATE (User 1)
    Note over DB: Lock acquired by User 1

    IS->>DB: available = 0, reserved = 1
    IS-->>U1: Success
    Note over DB: Lock released

    IS->>DB: SELECT FOR UPDATE (User 2)
    Note over DB: availableQuantity = 0
    IS-->>U2: Error: S402 INSUFFICIENT_STOCK
```

### 2. Redis Lua Script (쿠폰, 타임딜)

Redis Lua Script는 단일 스레드에서 원자적으로 실행되어 Race Condition을 방지합니다.

**coupon_issue.lua 흐름**:
```
SISMEMBER (이미 발급?) → GET stock (재고 확인) → DECR stock → SADD user
```
- Return: 1 (성공), 0 (재고 소진), -1 (이미 발급)

**timedeal_purchase.lua 흐름**:
```
GET purchased (구매 수량) → 한도 확인 → GET stock → DECRBY stock → INCRBY purchased
```
- Return: >0 (성공, 남은 재고), 0 (재고 소진), -1 (한도 초과)

### 3. Redisson 분산 락 (@DistributedLock AOP)

```java
@DistributedLock(key = "'scheduler:timedeal:status'", waitTime = 0, leaseTime = 55)
public void updateTimeDealStatus() { ... }
```

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| key | - | SpEL 표현식 (메서드 파라미터 바인딩 가능) |
| waitTime | 3s | 락 획득 대기 시간 |
| leaseTime | 5s | 락 유지 시간 |
| timeUnit | SECONDS | 시간 단위 |

**Key prefix**: `lock:{parsedKey}`
**획득 실패**: `CustomBusinessException(S408 CONCURRENT_STOCK_MODIFICATION)`

---

## 초기화 흐름

서비스 시작 시 실행되는 초기화 로직:

```mermaid
graph TD
    START[Application Start] --> A[CouponRedisBootstrap<br/>ApplicationRunner]
    START --> B[TimeDealRedisInitializer<br/>ApplicationReadyEvent]
    START --> C[IndexInitializationService<br/>@PostConstruct]

    A --> A1[ACTIVE 쿠폰 조회]
    A1 --> A2[Redis에 재고 복원<br/>coupon:stock:{id}]
    A2 --> A3[발급 사용자 목록 복원<br/>coupon:issued:{id}]

    B --> B1[ACTIVE 타임딜 조회]
    B1 --> B2[각 product 남은 재고 계산<br/>dealQuantity - soldQuantity]
    B2 --> B3[Redis에 재고 복원<br/>timedeal:stock:{dealId}:{productId}]

    C --> C1[products 인덱스 존재 확인]
    C1 --> C2{인덱스 있음?}
    C2 -->|No| C3[products-mapping.json으로<br/>인덱스 생성]
    C2 -->|Yes| C4[Skip]
```

---

## 성능 고려사항

### 주문 생성 병목

| 구간 | 예상 시간 | 병목 요인 |
|------|-----------|----------|
| 주문 생성 | ~100ms | DB Insert |
| 재고 예약 | ~50ms | Pessimistic Lock |
| 결제 처리 | ~1000ms | 외부 PG 호출 |
| 재고 차감 | ~50ms | DB Update |
| **전체** | **~1.2초** | PG 응답 시간 |

### 쿠폰/타임딜 성능

| 구간 | 예상 시간 | 기술 |
|------|-----------|------|
| Lua Script 실행 | ~1ms | Redis single-thread atomic |
| MySQL 동기화 | ~30ms | DB Insert |
| **전체** | **~35ms** | Redis 우선, DB 후속 |

### 최적화 전략

1. **Redis Lua Script**: 고동시성 구간(쿠폰, 타임딜)을 Redis에서 원자적 처리
2. **Pessimistic Lock**: 일반 재고 관리는 DB 레벨 Lock 유지
3. **SSE**: 대기열 상태를 폴링 대신 서버 푸시로 전달
4. **비동기 처리**: Kafka 이벤트로 후속 처리 분리
5. **배치 재고 예약**: 여러 상품을 한 번에 Lock (`reserveStockBatch`)
6. **Elasticsearch**: 상품 검색을 DB에서 분리

---

## 관련 문서

- [System Overview](./system-overview.md)
- [Saga Pattern](./saga-pattern.md) - 5단계 분산 트랜잭션 상세
- [Coupon System](./coupon-system.md) - Redis Lua 선착순 발급
- [TimeDeal System](./timedeal-system.md) - Scheduler 라이프사이클
- [Queue System](./queue-system.md) - Redis Sorted Set + SSE
- [Search System](./search-system.md) - Elasticsearch 검색

---

**최종 업데이트**: 2026-02-06
