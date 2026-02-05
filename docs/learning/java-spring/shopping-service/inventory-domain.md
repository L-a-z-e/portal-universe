# Inventory 도메인 심화

## 학습 목표
- 3단계 재고 모델 (Total, Available, Reserved) 이해
- 재고 변동 메서드의 책임 분리 학습
- StockMovement를 통한 감사 추적 (Audit Trail) 이해

---

## 1. Inventory Aggregate 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                    INVENTORY AGGREGATE                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │                  Inventory (Root)                         │  │
│   ├──────────────────────────────────────────────────────────┤  │
│   │  id                  Long (PK)                            │  │
│   │  productId           Long (unique, FK to Product)         │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  availableQuantity   Integer (판매 가능)                   │  │
│   │  reservedQuantity    Integer (예약됨)                      │  │
│   │  totalQuantity       Integer (전체 = available + reserved) │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  version             Long (@Version - 낙관적 락)           │  │
│   │  createdAt           LocalDateTime                        │  │
│   │  updatedAt           LocalDateTime                        │  │
│   └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │               StockMovement (감사 이력)                    │  │
│   ├──────────────────────────────────────────────────────────┤  │
│   │  id                  Long (PK)                            │  │
│   │  inventoryId         Long (FK)                            │  │
│   │  productId           Long (비정규화)                       │  │
│   │  movementType        MovementType (ENUM)                  │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  quantity            Integer (변동량)                      │  │
│   │  previousAvailable   Integer (변경 전)                     │  │
│   │  afterAvailable      Integer (변경 후)                     │  │
│   │  previousReserved    Integer (변경 전)                     │  │
│   │  afterReserved       Integer (변경 후)                     │  │
│   │  ─────────────────────────────────────────────────────    │  │
│   │  referenceType       String (ORDER, PAYMENT 등)           │  │
│   │  referenceId         String (주문번호 등)                  │  │
│   │  reason              String (변동 사유)                    │  │
│   │  performedBy         String (수행자)                       │  │
│   │  createdAt           LocalDateTime                        │  │
│   └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 3단계 재고 모델

### 2.1 재고 상태 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                     TOTAL QUANTITY (100)                         │
│  ┌────────────────────────────────┬────────────────────────────┐│
│  │    AVAILABLE QUANTITY (70)     │   RESERVED QUANTITY (30)   ││
│  │         (판매 가능)             │        (예약됨)            ││
│  │                                │                            ││
│  │  • 즉시 구매 가능               │  • 주문 생성됨              ││
│  │  • 상품 목록에 표시             │  • 결제 대기 중             ││
│  │                                │  • 아직 출고 안됨           ││
│  └────────────────────────────────┴────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 수량 관계

```java
// 항등식 (Invariant)
totalQuantity == availableQuantity + reservedQuantity
```

| 수량 유형 | 의미 | 표시 대상 |
|----------|------|----------|
| `totalQuantity` | 물리적 재고 | 관리자 화면 |
| `availableQuantity` | 판매 가능 재고 | 고객 화면 |
| `reservedQuantity` | 예약된 재고 | 시스템 내부 |

---

## 3. 재고 변동 시나리오

### 3.1 재고 흐름 다이어그램

```
                     INBOUND (+)
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AVAILABLE QUANTITY                            │
│                                                                  │
│    ◄──── RELEASE (+)                    RESERVE (−) ────►       │
│           주문 취소                        주문 생성              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    RESERVED QUANTITY                             │
│                                                                  │
│    ◄──── RELEASE (+)                    DEDUCT (−) ────►        │
│           결제 실패                        결제 완료              │
│                                           (total도 감소)         │
└─────────────────────────────────────────────────────────────────┘
                                              │
                         RETURN (+)           ▼
                            │            ┌─────────┐
                            └───────────►│  출고   │
                              반품 처리   └─────────┘
```

### 3.2 시나리오별 수량 변화

| 시나리오 | Available | Reserved | Total | 메서드 |
|----------|-----------|----------|-------|--------|
| **입고** | +N | - | +N | `addStock(N)` |
| **주문 생성** | -N | +N | - | `reserve(N)` |
| **주문 취소** | +N | -N | - | `release(N)` |
| **결제 완료** | - | -N | -N | `deduct(N)` |
| **반품 처리** | +N | - | +N | `returnStock(N)` |
| **관리자 조정** | ±N | ±N | ±N | `adjust(a, r)` |

---

## 4. 재고 변동 메서드

### 4.1 reserve() - 재고 예약

```java
/**
 * 재고를 예약합니다 (주문 생성 시).
 * Available → Reserved 이동
 */
public void reserve(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    if (this.availableQuantity < quantity) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INSUFFICIENT_STOCK);
    }
    this.availableQuantity -= quantity;
    this.reservedQuantity += quantity;
    // totalQuantity 변화 없음
}
```

**실행 전후:**
```
Before: available=100, reserved=0, total=100
reserve(10)
After:  available=90,  reserved=10, total=100
```

### 4.2 release() - 예약 해제

```java
/**
 * 예약된 재고를 해제합니다 (주문 취소, 결제 실패 시).
 * Reserved → Available 복원
 */
public void release(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    if (this.reservedQuantity < quantity) {
        throw new CustomBusinessException(
            ShoppingErrorCode.STOCK_RELEASE_FAILED);
    }
    this.reservedQuantity -= quantity;
    this.availableQuantity += quantity;
    // totalQuantity 변화 없음
}
```

**실행 전후:**
```
Before: available=90,  reserved=10, total=100
release(10)
After:  available=100, reserved=0,  total=100
```

### 4.3 deduct() - 재고 차감

```java
/**
 * 예약된 재고를 실제로 차감합니다 (결제 완료 시).
 * Reserved와 Total 모두 감소
 */
public void deduct(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    if (this.reservedQuantity < quantity) {
        throw new CustomBusinessException(
            ShoppingErrorCode.STOCK_DEDUCTION_FAILED);
    }
    this.reservedQuantity -= quantity;
    this.totalQuantity -= quantity;
    // availableQuantity 변화 없음
}
```

**실행 전후:**
```
Before: available=90, reserved=10, total=100
deduct(10)
After:  available=90, reserved=0,  total=90
```

### 4.4 addStock() - 재고 추가

```java
/**
 * 재고를 추가합니다 (입고 시).
 * Available과 Total 모두 증가
 */
public void addStock(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    this.availableQuantity += quantity;
    this.totalQuantity += quantity;
    // reservedQuantity 변화 없음
}
```

### 4.5 returnStock() - 반품 복원

```java
/**
 * 반품으로 인한 재고 복원입니다.
 * Available과 Total 모두 증가 (addStock과 동일)
 */
public void returnStock(int quantity) {
    if (quantity <= 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    this.availableQuantity += quantity;
    this.totalQuantity += quantity;
}
```

### 4.6 adjust() - 관리자 조정

```java
/**
 * 관리자에 의한 재고 조정입니다.
 * 직접 수량을 지정하여 전체 재설정
 */
public void adjust(int newAvailable, int newReserved) {
    if (newAvailable < 0 || newReserved < 0) {
        throw new CustomBusinessException(
            ShoppingErrorCode.INVALID_STOCK_QUANTITY);
    }
    this.availableQuantity = newAvailable;
    this.reservedQuantity = newReserved;
    this.totalQuantity = newAvailable + newReserved;
}
```

---

## 5. 낙관적 잠금 (Optimistic Locking)

### 5.1 @Version 필드

```java
@Entity
public class Inventory {
    @Version
    private Long version;
    // ...
}
```

### 5.2 동작 원리

```sql
-- JPA가 생성하는 UPDATE 쿼리
UPDATE inventory
SET available_quantity = ?,
    reserved_quantity = ?,
    total_quantity = ?,
    version = version + 1,
    updated_at = ?
WHERE id = ?
  AND version = ?   -- 버전 검증

-- 다른 트랜잭션이 먼저 업데이트했다면:
-- 결과: 0 rows affected
-- → OptimisticLockException 발생
```

### 5.3 재시도 전략

```java
@Service
public class InventoryService {

    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository
            .findByProductId(productId)
            .orElseThrow();
        inventory.reserve(quantity);
        // 저장 시 버전 충돌 발생 가능
    }
}
```

---

## 6. MovementType Enum

```java
public enum MovementType {
    INBOUND("입고"),      // 재고 추가
    RESERVE("예약"),      // 주문에 의한 재고 예약
    RELEASE("해제"),      // 예약된 재고 해제
    DEDUCT("차감"),       // 결제 완료 후 실제 차감
    RETURN("반품"),       // 반품에 의한 복원
    ADJUSTMENT("조정"),   // 관리자에 의한 수동 조정
    INITIAL("초기화");    // 최초 재고 설정
}
```

---

## 7. StockMovement - 감사 추적

### 7.1 Entity 구조

```java
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_inventory_id",
           columnList = "inventory_id"),
    @Index(name = "idx_stock_movement_product_id",
           columnList = "product_id"),
    @Index(name = "idx_stock_movement_reference",
           columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_movement_created_at",
           columnList = "created_at")
})
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long inventoryId;
    private Long productId;           // 비정규화 (빠른 조회)

    @Enumerated(EnumType.STRING)
    private MovementType movementType;

    private Integer quantity;          // 변동량

    // 변경 전/후 스냅샷
    private Integer previousAvailable;
    private Integer afterAvailable;
    private Integer previousReserved;
    private Integer afterReserved;

    // 참조 정보
    private String referenceType;      // ORDER, PAYMENT, ADMIN
    private String referenceId;        // 주문번호, 결제번호 등
    private String reason;             // 변동 사유
    private String performedBy;        // 수행자

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 7.2 StockMovement 생성 예시

```java
@Service
public class InventoryService {

    @Transactional
    public void reserveWithTracking(Long productId, int quantity,
                                    String orderNumber) {
        Inventory inventory = inventoryRepository
            .findByProductId(productId)
            .orElseThrow();

        // 변경 전 상태 저장
        int prevAvailable = inventory.getAvailableQuantity();
        int prevReserved = inventory.getReservedQuantity();

        // 재고 예약
        inventory.reserve(quantity);

        // 이력 기록
        StockMovement movement = StockMovement.builder()
            .inventoryId(inventory.getId())
            .productId(productId)
            .movementType(MovementType.RESERVE)
            .quantity(quantity)
            .previousAvailable(prevAvailable)
            .afterAvailable(inventory.getAvailableQuantity())
            .previousReserved(prevReserved)
            .afterReserved(inventory.getReservedQuantity())
            .referenceType("ORDER")
            .referenceId(orderNumber)
            .reason("주문 재고 예약")
            .performedBy("SYSTEM")
            .build();

        stockMovementRepository.save(movement);
    }
}
```

### 7.3 감사 추적 조회

```sql
-- 특정 상품의 재고 변동 이력
SELECT *
FROM stock_movements
WHERE product_id = 123
ORDER BY created_at DESC;

-- 특정 주문의 재고 변동 추적
SELECT *
FROM stock_movements
WHERE reference_type = 'ORDER'
  AND reference_id = 'ORD-20250122-A1B2C3D4';
```

---

## 8. 주문 Saga에서의 재고 흐름

```
┌───────────────────────────────────────────────────────────────────────────┐
│                           ORDER SAGA                                       │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  Step 1: RESERVE_INVENTORY                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │  실행: inventory.reserve(quantity)                                   │ │
│  │        available -= N, reserved += N                                 │ │
│  │  기록: StockMovement(RESERVE)                                        │ │
│  │  ─────────────────────────────────────────────────────────────────  │ │
│  │  보상: inventory.release(quantity)                                   │ │
│  │        reserved -= N, available += N                                 │ │
│  │  기록: StockMovement(RELEASE)                                        │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  Step 3: DEDUCT_INVENTORY                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │  실행: inventory.deduct(quantity)                                    │ │
│  │        reserved -= N, total -= N                                     │ │
│  │  기록: StockMovement(DEDUCT)                                         │ │
│  │  ─────────────────────────────────────────────────────────────────  │ │
│  │  보상: inventory.addStock(quantity)                                  │ │
│  │        available += N, total += N                                    │ │
│  │  기록: StockMovement(RETURN)                                         │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 인덱스 전략

```java
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id",
           columnList = "product_id",
           unique = true)  // 1:1 관계
})
```

| 인덱스 | 용도 |
|--------|------|
| `product_id` (unique) | 상품별 재고 조회, 1:1 보장 |

---

## 10. 핵심 정리

| 개념 | 설명 |
|------|------|
| **3단계 재고** | Total = Available + Reserved |
| **reserve()** | Available → Reserved (주문 생성) |
| **release()** | Reserved → Available (주문 취소) |
| **deduct()** | Reserved, Total 감소 (결제 완료) |
| **addStock()** | Available, Total 증가 (입고) |
| **낙관적 잠금** | @Version으로 동시성 제어 |
| **감사 추적** | StockMovement로 모든 변동 기록 |
| **참조 추적** | referenceType, referenceId로 원인 추적 |

---

## 다음 학습

- [재고 동시성 제어](../business/inventory-concurrency.md)
- [Redis 분산 락](../../docs/learning/redis/redis-distributed-lock.md)
- [주문 Saga 흐름](../business/order-saga.md)
