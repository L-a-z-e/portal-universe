# Shopping Service 도메인 개요

## 학습 목표
- Shopping Service의 전체 도메인 구조 이해
- Aggregate Root와 관계 파악
- 핵심 비즈니스 흐름 학습

---

## 1. 도메인 맵

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SHOPPING SERVICE DOMAIN                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐               │
│   │   CATALOG    │     │   ORDERING   │     │  PROMOTION   │               │
│   ├──────────────┤     ├──────────────┤     ├──────────────┤               │
│   │ • Product    │     │ • Order      │     │ • Coupon     │               │
│   │ • Inventory  │     │ • OrderItem  │     │ • UserCoupon │               │
│   │ • Stock      │     │ • Cart       │     │ • TimeDeal   │               │
│   │   Movement   │     │ • CartItem   │     │              │               │
│   └──────────────┘     └──────────────┘     └──────────────┘               │
│          │                    │                    │                        │
│          │                    ▼                    │                        │
│          │           ┌──────────────┐              │                        │
│          │           │   PAYMENT    │              │                        │
│          │           ├──────────────┤              │                        │
│          │           │ • Payment    │              │                        │
│          │           └──────────────┘              │                        │
│          │                    │                    │                        │
│          │                    ▼                    │                        │
│          │           ┌──────────────┐              │                        │
│          │           │   DELIVERY   │              │                        │
│          │           ├──────────────┤              │                        │
│          │           │ • Delivery   │              │                        │
│          │           │ • History    │              │                        │
│          │           └──────────────┘              │                        │
│          │                                                                   │
│          └─────────────────────────────────────────┘                        │
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │                         SAGA ORCHESTRATION                            │  │
│   ├──────────────────────────────────────────────────────────────────────┤  │
│   │  • SagaState     • OrderSagaOrchestrator                             │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Aggregate Root 식별

### 2.1 Aggregate 정의

| Aggregate Root | 하위 Entity | Value Object |
|----------------|-------------|--------------|
| **Order** | OrderItem | Address |
| **Cart** | CartItem | - |
| **Payment** | - | - |
| **Delivery** | DeliveryHistory | Address |
| **Inventory** | StockMovement | - |
| **Coupon** | - | - |
| **UserCoupon** | - | - |
| **TimeDeal** | TimeDealProduct | - |

### 2.2 Aggregate 경계

```
┌─ Order Aggregate ───────────────────┐
│                                      │
│  Order (Root)                        │
│    │                                 │
│    ├── OrderItem [1..*]              │
│    │                                 │
│    └── Address (Embedded)            │
│                                      │
└──────────────────────────────────────┘

┌─ Inventory Aggregate ───────────────┐
│                                      │
│  Inventory (Root)                    │
│    │                                 │
│    └── StockMovement [0..*]          │
│         (감사 추적)                   │
│                                      │
└──────────────────────────────────────┘

┌─ TimeDeal Aggregate ────────────────┐
│                                      │
│  TimeDeal (Root)                     │
│    │                                 │
│    ├── TimeDealProduct [1..*]        │
│    │                                 │
│    └── TimeDealPurchase [0..*]       │
│                                      │
└──────────────────────────────────────┘
```

---

## 3. 도메인 엔티티 상세

### 3.1 Order (주문)

주문의 라이프사이클을 관리하는 핵심 Aggregate입니다.

**상태 전이:**
```
           ┌───────────────────────────────────────────┐
           │                                           │
           ▼                                           │
PENDING ──────► CONFIRMED ──────► PAID ──────► SHIPPING ──────► DELIVERED
   │               │                 │              │
   │               │                 │              │
   └──────►────────┴────────►────────┴──────►───────┘
              │                            │
              ▼                            ▼
          CANCELLED                    REFUNDED
```

**핵심 필드:**
- `orderNumber`: UUID 기반 (ORD-YYYYMMDD-XXXXXXXX)
- `totalAmount`: 원가 합계
- `discountAmount`: 쿠폰 할인
- `finalAmount`: 실제 결제 금액

**주요 메서드:**
```java
public void confirm()      // PENDING → CONFIRMED
public void markAsPaid()   // CONFIRMED → PAID
public void ship()         // PAID → SHIPPING
public void deliver()      // SHIPPING → DELIVERED
public void cancel(reason) // → CANCELLED
public void applyCoupon(userCouponId, discountAmount)
```

### 3.2 Inventory (재고)

재고 상태를 세밀하게 추적합니다.

**재고 상태 구조:**
```
┌─────────────────────────────────────────────┐
│              Total Quantity                  │
│  ┌─────────────────────┬───────────────────┐│
│  │ Available Quantity  │ Reserved Quantity ││
│  │   (판매 가능)        │   (예약됨)         ││
│  └─────────────────────┴───────────────────┘│
└─────────────────────────────────────────────┘
```

**재고 변동 흐름:**
```
주문 생성 → reserve()
  • availableQty -= N
  • reservedQty += N

결제 완료 → deduct()
  • reservedQty -= N
  • totalQty -= N

주문 취소 → release()
  • reservedQty -= N
  • availableQty += N
```

### 3.3 Payment (결제)

결제 처리 및 PG 연동을 담당합니다.

**상태 전이:**
```
PENDING ──► PROCESSING ──► COMPLETED
    │            │             │
    │            │             ▼
    │            └──► FAILED   REFUNDED
    │
    └─────────────────► CANCELLED
```

**PG 연동 필드:**
- `pgTransactionId`: PG사 거래 ID
- `pgResponse`: PG 응답 JSON
- `paymentMethod`: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE, POINTS

### 3.4 Coupon & UserCoupon (쿠폰)

쿠폰 정책과 사용자 보유 쿠폰을 분리 관리합니다.

**할인 계산:**
```java
// FIXED: 고정 할인
discountAmount = Math.min(discountValue, orderAmount)

// PERCENTAGE: 퍼센트 할인
discountAmount = orderAmount × (discountValue / 100)
discountAmount = Math.min(discountAmount, maximumDiscountAmount)
```

### 3.5 TimeDeal (타임딜)

시간 한정 특가 판매를 관리합니다.

**구조:**
```
TimeDeal (캠페인)
    │
    └── TimeDealProduct (상품별 특가)
            │
            └── TimeDealPurchase (구매 기록)
```

**제약 조건:**
- `maxPerUser`: 1인당 최대 구매 수량
- `dealQuantity`: 한정 판매 수량
- 시간 범위: `startsAt` ~ `endsAt`

---

## 4. 핵심 비즈니스 흐름

### 4.1 주문 생성 흐름

```
1. 장바구니 체크아웃
   Cart.checkout() → CHECKED_OUT

2. 주문 생성
   Order.create() → PENDING

3. 재고 예약 (Saga Step 1)
   Inventory.reserve(qty) → StockMovement 기록

4. 결제 처리 (Saga Step 2)
   Payment.startProcessing() → PROCESSING
   Payment.complete(pgId) → COMPLETED

5. 재고 확정 (Saga Step 3)
   Inventory.deduct(qty) → StockMovement 기록

6. 배송 생성 (Saga Step 4)
   Delivery.create() → PREPARING

7. 주문 확정 (Saga Step 5)
   Order.confirm() → CONFIRMED
```

### 4.2 쿠폰 적용 흐름

```
1. 쿠폰 유효성 검증
   Coupon.isAvailable()
   UserCoupon.isUsable()

2. 할인 금액 계산
   discount = Coupon.calculateDiscount(orderAmount)

3. 주문에 적용
   Order.applyCoupon(userCouponId, discount)
   finalAmount = totalAmount - discountAmount

4. 쿠폰 사용 처리
   UserCoupon.use(orderId)
```

### 4.3 타임딜 구매 흐름

```
1. 타임딜 활성화 확인
   TimeDeal.isActive()

2. 재고 확인
   TimeDealProduct.getRemainingQuantity() > 0

3. 1인당 구매 제한 확인
   TimeDealPurchase 조회 → 기존 구매량 + 요청량 ≤ maxPerUser

4. 구매 처리
   TimeDealProduct.soldQuantity += qty
   TimeDealPurchase 생성

5. 특가 적용 주문 생성
   OrderItem.price = dealPrice
```

---

## 5. 도메인 이벤트

### 5.1 발행 이벤트 목록

| 이벤트 | 발행 시점 | Consumer |
|--------|----------|----------|
| `OrderCreatedEvent` | 주문 생성 | Notification Service |
| `OrderConfirmedEvent` | 주문 확정 | Notification Service |
| `OrderCancelledEvent` | 주문 취소 | Notification Service |
| `PaymentCompletedEvent` | 결제 완료 | Notification Service |
| `PaymentFailedEvent` | 결제 실패 | Notification Service |
| `InventoryReservedEvent` | 재고 예약 | (내부) |
| `DeliveryShippedEvent` | 배송 시작 | Notification Service |

### 5.2 이벤트 페이로드 예시

```java
public record OrderCreatedEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt
) {}
```

---

## 6. Saga 패턴 적용

### 6.1 OrderSaga 단계

```
┌───────────────────────────────────────────────────────────────────────┐
│                        ORDER SAGA STEPS                               │
├───────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Step 1: RESERVE_INVENTORY                                            │
│    ├─ 실행: Inventory.reserve()                                       │
│    └─ 보상: Inventory.release()                                       │
│                                                                       │
│  Step 2: PROCESS_PAYMENT                                              │
│    ├─ 실행: Payment.process()                                         │
│    └─ 보상: Payment.refund() (외부 PG)                                │
│                                                                       │
│  Step 3: DEDUCT_INVENTORY                                             │
│    ├─ 실행: Inventory.deduct()                                        │
│    └─ 보상: Inventory.addStock()                                      │
│                                                                       │
│  Step 4: CREATE_DELIVERY                                              │
│    ├─ 실행: Delivery.create()                                         │
│    └─ 보상: Delivery.cancel()                                         │
│                                                                       │
│  Step 5: CONFIRM_ORDER                                                │
│    ├─ 실행: Order.confirm()                                           │
│    └─ 보상: Order.cancel()                                            │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

### 6.2 Saga 상태 추적

```java
public class SagaState {
    private String sagaId;           // SAGA-XXXXXXXX
    private Long orderId;
    private String currentStep;
    private SagaStatus status;       // STARTED, COMPLETED, COMPENSATING, FAILED
    private String completedSteps;   // CSV: "STEP1,STEP2,..."
    private int compensationAttempts;
    private String lastErrorMessage;
}
```

---

## 7. 동시성 제어 전략

### 7.1 재고 관리

| 방식 | 사용 위치 | 목적 |
|------|----------|------|
| **Optimistic Lock** | Inventory.version | 일반 재고 차감 |
| **Distributed Lock** | Redis/Redisson | 선착순 쿠폰/타임딜 |
| **Lua Script** | Redis | 원자적 재고 확인 + 차감 |

### 7.2 낙관적 잠금 (Inventory)

```java
@Entity
public class Inventory {
    @Version
    private Long version;

    public void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new InsufficientStockException();
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }
}
```

---

## 8. 감사 추적 (Audit Trail)

### 8.1 StockMovement

모든 재고 변동을 기록합니다.

```java
@Entity
public class StockMovement {
    private MovementType movementType;  // RESERVE, RELEASE, DEDUCT, ...
    private int quantity;
    private int previousAvailable;
    private int afterAvailable;
    private int previousReserved;
    private int afterReserved;
    private String referenceType;       // ORDER, PAYMENT
    private String referenceId;
    private String reason;
    private String performedBy;
}
```

### 8.2 DeliveryHistory

배송 상태 변이를 추적합니다.

```java
@Entity
public class DeliveryHistory {
    private DeliveryStatus status;
    private String location;
    private String description;
    private LocalDateTime createdAt;
}
```

---

## 9. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Aggregate Root** | Order, Inventory, Payment, Delivery, Coupon, TimeDeal |
| **Value Object** | Address (배송 주소) |
| **Domain Event** | Kafka를 통한 서비스 간 통신 |
| **Saga Pattern** | 분산 트랜잭션 관리 |
| **Optimistic Lock** | 재고 동시성 제어 |
| **Audit Trail** | StockMovement, DeliveryHistory |

---

## 다음 학습

- [Order 도메인 심화](./order-domain.md)
- [Inventory 도메인 심화](./inventory-domain.md)
- [Shopping ERD](../database/shopping-erd.md)
