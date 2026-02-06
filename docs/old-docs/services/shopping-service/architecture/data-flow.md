---
id: arch-data-flow
title: Shopping Service Data Flow
type: architecture
status: current
created: 2026-01-18
updated: 2026-01-18
author: Laze
tags: [architecture, shopping-service, data-flow, saga, events]
related:
  - arch-system-overview
  - api-order
  - api-payment
---

# Shopping Service Data Flow

## 📋 개요

Shopping Service의 주요 데이터 흐름과 이벤트 처리 과정을 설명합니다. 특히 주문 생성부터 결제, 재고 처리, 배송까지의 전체 흐름과 Saga 패턴을 통한 분산 트랜잭션 처리를 다룹니다.

---

## 🔄 주요 데이터 흐름

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
    EP->>K: Send to order-created topic

    OS-->>G: OrderResponse
    G-->>C: 201 Created
```

**설명**:
1. 클라이언트가 주문 생성 요청 (장바구니 ID + 배송지)
2. OrderService가 장바구니 항목으로 주문 생성 (상태: PENDING)
3. OrderSagaOrchestrator가 Saga 시작
4. **Step 1**: 재고 예약
   - Pessimistic Lock으로 재고 조회
   - availableQuantity → reservedQuantity 이동
   - StockMovement 기록 (타입: RESERVATION)
5. 재고 예약 성공 시 다음 단계로 진행
6. OrderCreatedEvent 발행 (Kafka)
7. 클라이언트에 주문 번호 반환

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
    participant OS as OrderService
    participant EP as EventPublisher
    participant K as Kafka
    participant DB as MySQL

    C->>G: POST /api/v1/shopping/payments
    Note over C,G: {orderNumber, paymentMethod}
    G->>PS: Process Payment
    PS->>DB: Get Order
    PS->>DB: Create Payment (PENDING)
    PS->>DB: Payment status = PROCESSING

    PS->>PG: processPayment(amount, method)
    alt Payment Success (90%)
        PG-->>PS: Success + transactionId
        PS->>DB: Payment status = COMPLETED
        PS->>DB: Payment.paidAt = now()

        Note over PS,SAGA: Continue Saga
        PS->>SAGA: completeSagaAfterPayment(orderNumber)
        SAGA->>DB: Get SagaState

        Note over SAGA,IS: Step 3: Deduct Inventory
        SAGA->>IS: deductStockBatch(quantities)
        IS->>DB: reserved -= quantity, total -= quantity
        IS->>DB: INSERT stock_movement (SALE)
        IS-->>SAGA: Success

        Note over SAGA,OS: Step 5: Confirm Order
        SAGA->>DB: Order status = PAID
        SAGA->>DB: SagaState status = COMPLETED

        PS->>EP: publishPaymentCompleted(payment)
        EP->>K: Send to payment-completed topic

        PS-->>G: PaymentResponse
        G-->>C: 200 OK

    else Payment Failed (10%)
        PG-->>PS: Failed + reason
        PS->>DB: Payment status = FAILED
        PS->>DB: Payment.failureReason = reason

        Note over PS,SAGA: Compensate Saga
        PS->>SAGA: compensate(sagaState, reason)
        SAGA->>IS: releaseStockBatch(quantities)
        IS->>DB: reserved -= quantity, available += quantity
        IS->>DB: INSERT stock_movement (RELEASE)
        SAGA->>DB: Order status = CANCELLED
        SAGA->>DB: SagaState status = FAILED

        PS->>EP: publishPaymentFailed(payment)
        EP->>K: Send to payment-failed topic

        PS-->>G: 400 Payment Failed
        G-->>C: 400 Bad Request
    end
```

**설명**:

#### 결제 성공 시:
1. Payment 생성 (상태: PENDING → PROCESSING)
2. MockPGClient 호출 (90% 성공률)
3. PG 성공 응답 → Payment.status = COMPLETED
4. **Saga 계속 진행**:
   - Step 3: 재고 차감 (reserved → 0, total 감소)
   - Step 4: 배송 생성 (별도 서비스, skip)
   - Step 5: 주문 확정 (Order.status = PAID)
   - SagaState.status = COMPLETED
5. PaymentCompletedEvent 발행

#### 결제 실패 시:
1. PG 실패 응답 → Payment.status = FAILED
2. **Saga 보상(Compensation)**:
   - 예약된 재고 해제 (reserved → available)
   - StockMovement 기록 (타입: RELEASE)
   - 주문 취소 (Order.status = CANCELLED)
   - SagaState.status = FAILED
3. PaymentFailedEvent 발행
4. 클라이언트에 실패 응답

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
        EP->>K: Send to order-cancelled topic
        OS-->>G: 200 OK

    else Order status = PAID
        OS->>PS: Refund Payment
        PS->>DB: Payment status = REFUNDED
        PS->>DB: Order status = REFUNDED
        OS->>IS: returnStock(quantities)
        IS->>DB: available += quantity, total += quantity
        OS->>EP: publishOrderCancelled(order)
        EP->>K: Send to order-cancelled topic
        OS-->>G: 200 OK

    else Order status = SHIPPING/DELIVERED
        OS-->>G: 400 Cannot Cancel
    end

    G-->>C: Response
```

**설명**:
1. 주문 상태에 따라 다른 처리:
   - **PENDING**: 재고 예약 해제
   - **PAID**: 결제 환불 + 재고 복원 (반품)
   - **SHIPPING/DELIVERED**: 취소 불가 (S203 에러)
2. OrderCancelledEvent 발행

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

**설명**:
1. 운송장 번호로 배송 상태 조회
2. 모든 배송 이력 함께 반환
3. 상태 업데이트 시 DeliveryHistory 자동 생성

---

## 📨 이벤트/메시지 흐름

### Kafka Topics

```mermaid
graph LR
    subgraph "Shopping Service (Publisher)"
        OS[OrderService]
        PS[PaymentService]
        IS[InventoryService]
        DS[DeliveryService]
    end

    subgraph "Kafka Topics"
        T1[order-created]
        T2[order-confirmed]
        T3[order-cancelled]
        T4[payment-completed]
        T5[payment-failed]
        T6[inventory-reserved]
        T7[delivery-shipped]
    end

    subgraph "Consumers"
        NS[NotificationService]
        AS[AnalyticsService]
        WH[WarehouseService]
    end

    OS --> T1
    OS --> T2
    OS --> T3
    PS --> T4
    PS --> T5
    IS --> T6
    DS --> T7

    T1 & T2 & T3 --> NS
    T1 & T2 & T3 & T4 & T5 --> AS
    T7 --> WH
```

### 이벤트 목록

| 이벤트 | Topic | 발행자 | 구독자 | Payload |
|--------|-------|--------|--------|---------|
| OrderCreatedEvent | order-created | OrderService | NotificationService | orderNumber, userId, totalAmount, items |
| OrderConfirmedEvent | order-confirmed | OrderService | NotificationService | orderNumber, userId |
| OrderCancelledEvent | order-cancelled | OrderService | NotificationService, InventoryService | orderNumber, userId, reason |
| PaymentCompletedEvent | payment-completed | PaymentService | NotificationService, OrderService | paymentNumber, orderNumber, amount |
| PaymentFailedEvent | payment-failed | PaymentService | NotificationService | paymentNumber, orderNumber, reason |
| InventoryReservedEvent | inventory-reserved | InventoryService | WarehouseService | orderNumber, productId, quantity |
| DeliveryShippedEvent | delivery-shipped | DeliveryService | NotificationService, TrackingService | trackingNumber, orderNumber |

---

## 🔄 Saga Pattern 상세

### Saga State Machine

```mermaid
stateDiagram-v2
    [*] --> STARTED: startSaga()

    STARTED --> RESERVE_INVENTORY: Step 1
    RESERVE_INVENTORY --> PROCESS_PAYMENT: reserve success
    RESERVE_INVENTORY --> COMPENSATING: reserve failed

    PROCESS_PAYMENT --> DEDUCT_INVENTORY: payment success
    PROCESS_PAYMENT --> COMPENSATING: payment failed

    DEDUCT_INVENTORY --> CREATE_DELIVERY: deduct success
    DEDUCT_INVENTORY --> COMPENSATING: deduct failed

    CREATE_DELIVERY --> CONFIRM_ORDER: delivery created
    CREATE_DELIVERY --> COMPENSATING: delivery failed

    CONFIRM_ORDER --> COMPLETED: confirm success
    CONFIRM_ORDER --> COMPENSATING: confirm failed

    COMPENSATING --> FAILED: compensation success
    COMPENSATING --> COMPENSATION_FAILED: compensation failed (3 attempts)

    COMPLETED --> [*]
    FAILED --> [*]
    COMPENSATION_FAILED --> [*]: Manual intervention required
```

### Saga Steps

| Step | 단계명 | 작업 | 보상(Compensation) |
|------|--------|------|-------------------|
| 1 | RESERVE_INVENTORY | 재고 예약 (available → reserved) | 재고 해제 (reserved → available) |
| 2 | PROCESS_PAYMENT | 결제 처리 (외부 호출) | 결제 취소/환불 |
| 3 | DEDUCT_INVENTORY | 재고 차감 (reserved 감소, total 감소) | 수동 복원 필요 (반품) |
| 4 | CREATE_DELIVERY | 배송 생성 | 배송 취소 |
| 5 | CONFIRM_ORDER | 주문 확정 (status = PAID) | 주문 취소 |

### Compensation 전략

```mermaid
flowchart TD
    A[Saga 실패 발생] --> B{어느 단계에서 실패?}

    B -->|RESERVE_INVENTORY| C[재고 예약 해제]
    B -->|PROCESS_PAYMENT| D[재고 예약 해제 + 결제 취소]
    B -->|DEDUCT_INVENTORY| E[수동 개입 필요]
    B -->|CREATE_DELIVERY| F[재고 복원 + 배송 취소]
    B -->|CONFIRM_ORDER| G[전체 롤백]

    C --> H[주문 취소]
    D --> H
    E --> I[관리자 알림]
    F --> H
    G --> H

    H --> J[SagaState = FAILED]
    I --> K[SagaState = COMPENSATION_FAILED]

    J --> L[완료]
    K --> M[수동 처리 대기]
```

**보상 실패 시**:
- 최대 3회 재시도
- 3회 실패 시 SagaState.status = COMPENSATION_FAILED
- 관리자에게 알림 발송
- 수동 개입 필요

---

## 🔐 동시성 제어

### 재고 관리 Pessimistic Lock

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

---

## 📊 성능 고려사항

### 주문 생성 병목

| 구간 | 예상 시간 | 병목 요인 |
|------|-----------|----------|
| 주문 생성 | ~100ms | DB Insert |
| 재고 예약 | ~50ms | Pessimistic Lock |
| 결제 처리 | ~1000ms | 외부 PG 호출 |
| 재고 차감 | ~50ms | DB Update |
| **전체** | **~1.2초** | PG 응답 시간 |

### 최적화 전략

1. **재고 조회 캐싱**: Redis로 가용 재고 캐시 (TTL: 10초)
2. **비동기 처리**: 결제 완료 후 Saga 나머지 단계는 비동기
3. **배치 재고 예약**: 여러 상품을 한 번에 Lock
4. **Connection Pool 조정**: 동시 주문 처리량 증가

---

## 🔗 관련 문서

- [System Overview](./system-overview.md)
- [Order API](../api/api-order.md)
- [Payment API](../api/api-payment.md)
- [Inventory API](../api/api-inventory.md)
- [Saga Pattern Troubleshooting](../troubleshooting/2026/01/TS-20260118-001-saga-compensation.md) (예정)

---

**최종 업데이트**: 2026-01-18
