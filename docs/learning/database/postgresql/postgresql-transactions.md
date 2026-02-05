# PostgreSQL 트랜잭션

**난이도:** ⭐⭐⭐

## 학습 목표
- ACID 속성 이해 및 PostgreSQL 구현 방식 학습
- 트랜잭션 격리 수준(Isolation Level) 파악
- MVCC(Multi-Version Concurrency Control) 메커니즘 이해
- Deadlock 방지 전략 수립
- Portal Universe 주문 처리에서의 안전한 트랜잭션 구현

---

## 1. 트랜잭션 기초

### 1.1 트랜잭션이란?

트랜잭션은 **논리적 작업 단위**로, 모두 성공하거나 모두 실패해야 하는 일련의 데이터베이스 연산입니다.

```sql
BEGIN;
    UPDATE accounts SET balance = balance - 1000 WHERE user_id = 1;
    UPDATE accounts SET balance = balance + 1000 WHERE user_id = 2;
COMMIT;
```

### 1.2 ACID 속성

| 속성 | 의미 | PostgreSQL 구현 |
|------|------|----------------|
| **Atomicity** (원자성) | 모두 성공 또는 모두 실패 | WAL(Write-Ahead Logging) |
| **Consistency** (일관성) | 제약 조건 항상 유지 | CHECK, FK, Trigger |
| **Isolation** (격리성) | 동시 실행 트랜잭션 간섭 방지 | MVCC |
| **Durability** (지속성) | 완료된 트랜잭션 영구 보존 | fsync, WAL |

---

## 2. 트랜잭션 제어

### 2.1 기본 명령어

```sql
-- 트랜잭션 시작
BEGIN;
-- 또는
START TRANSACTION;

-- 트랜잭션 커밋 (영구 저장)
COMMIT;

-- 트랜잭션 롤백 (취소)
ROLLBACK;

-- 세이브포인트 생성
SAVEPOINT sp1;

-- 세이브포인트로 롤백
ROLLBACK TO SAVEPOINT sp1;

-- 세이브포인트 해제
RELEASE SAVEPOINT sp1;
```

### 2.2 세이브포인트 활용

```sql
BEGIN;
    -- 주문 생성
    INSERT INTO orders (user_id, total_amount, status)
    VALUES (123, 50000, 'PENDING');

    SAVEPOINT after_order;

    -- 재고 차감 시도
    UPDATE products SET stock_quantity = stock_quantity - 1
    WHERE id = 456;

    -- 재고 부족 시 주문만 유지하고 재고 업데이트 취소
    SELECT stock_quantity FROM products WHERE id = 456;
    -- stock_quantity가 음수라면:
    ROLLBACK TO SAVEPOINT after_order;

    -- 주문 상태 변경
    UPDATE orders SET status = 'OUT_OF_STOCK' WHERE id = LASTVAL();

COMMIT;
```

### 2.3 MySQL vs PostgreSQL 트랜잭션

| 특성 | MySQL (InnoDB) | PostgreSQL |
|------|----------------|------------|
| 기본 격리 수준 | REPEATABLE READ | READ COMMITTED |
| MVCC | ✅ (Undo Log) | ✅ (Tuple Version) |
| Serializable 구현 | Gap Lock | SSI (Predicate Lock) |
| Deadlock 감지 | ✅ | ✅ |
| 자동 커밋 | ✅ | ✅ |
| Nested Transaction | ❌ | ✅ (Savepoint) |

---

## 3. 격리 수준 (Isolation Level)

### 3.1 격리 수준 개요

PostgreSQL은 SQL 표준의 4가지 격리 수준을 지원합니다.

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read | 성능 |
|-----------|-----------|---------------------|--------------|------|
| **READ UNCOMMITTED** | 가능 (PostgreSQL에서는 불가능) | 가능 | 가능 | ⚡⚡⚡⚡ |
| **READ COMMITTED** | 불가능 | 가능 | 가능 | ⚡⚡⚡ |
| **REPEATABLE READ** | 불가능 | 불가능 | 불가능* | ⚡⚡ |
| **SERIALIZABLE** | 불가능 | 불가능 | 불가능 | ⚡ |

*PostgreSQL의 REPEATABLE READ는 Phantom Read도 방지합니다.

### 3.2 격리 수준 설정

```sql
-- 세션 레벨 설정
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 트랜잭션별 설정
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
    -- 트랜잭션 작업
COMMIT;

-- 현재 격리 수준 확인
SHOW TRANSACTION ISOLATION LEVEL;
```

### 3.3 READ COMMITTED (기본값)

**특성:**
- 커밋된 데이터만 읽음
- 같은 쿼리를 반복해도 다른 결과 가능 (Non-Repeatable Read)

**예시:**

```sql
-- Session A
BEGIN;
SELECT balance FROM accounts WHERE user_id = 1;
-- 결과: 10000

-- Session B
BEGIN;
UPDATE accounts SET balance = 15000 WHERE user_id = 1;
COMMIT;

-- Session A (계속)
SELECT balance FROM accounts WHERE user_id = 1;
-- 결과: 15000 (변경됨!)
COMMIT;
```

**사용 시나리오:**
- 일반적인 OLTP 애플리케이션
- 높은 동시성 필요
- 데이터 일관성보다 성능 우선

### 3.4 REPEATABLE READ

**특성:**
- 트랜잭션 시작 시점의 스냅샷 읽기
- 같은 쿼리는 항상 같은 결과 반환
- PostgreSQL은 Phantom Read도 방지

**예시:**

```sql
-- Session A
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT * FROM products WHERE price > 1000;
-- 결과: 100개

-- Session B
BEGIN;
INSERT INTO products (name, price) VALUES ('New Product', 1500);
COMMIT;

-- Session A (계속)
SELECT * FROM products WHERE price > 1000;
-- 결과: 여전히 100개 (Session B의 INSERT 보이지 않음)
COMMIT;
```

**사용 시나리오:**
- 보고서 생성
- 배치 작업
- 일관된 데이터 뷰 필요

### 3.5 SERIALIZABLE

**특성:**
- 가장 높은 격리 수준
- 직렬 실행과 동일한 결과 보장
- Serialization Anomaly 방지

**예시:**

```sql
-- Session A: 재고 확인 후 주문
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT stock_quantity FROM products WHERE id = 456;
-- 결과: 10

-- Session B: 동시 주문
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT stock_quantity FROM products WHERE id = 456;
-- 결과: 10

-- Session A (계속)
UPDATE products SET stock_quantity = stock_quantity - 5 WHERE id = 456;
INSERT INTO orders (product_id, quantity) VALUES (456, 5);
COMMIT;
-- 성공

-- Session B (계속)
UPDATE products SET stock_quantity = stock_quantity - 8 WHERE id = 456;
INSERT INTO orders (product_id, quantity) VALUES (456, 8);
COMMIT;
-- ERROR: could not serialize access due to read/write dependencies
```

**사용 시나리오:**
- 금융 거래
- 재고 관리 (재고 부족 방지)
- 데이터 일관성이 최우선

---

## 4. MVCC (Multi-Version Concurrency Control)

### 4.1 MVCC란?

PostgreSQL은 **다중 버전 동시성 제어**를 통해 읽기와 쓰기가 서로 차단하지 않도록 합니다.

**핵심 원리:**
- 각 행의 여러 버전 유지
- 트랜잭션마다 적절한 버전 선택
- 읽기는 쓰기를 차단하지 않음
- 쓰기는 읽기를 차단하지 않음

### 4.2 튜플 버전 관리

각 행(튜플)은 메타데이터를 포함합니다:

```
┌─────────────┬──────────┬──────────┬───────────────┐
│ xmin        │ xmax     │ cmin     │ cmax          │
├─────────────┼──────────┼──────────┼───────────────┤
│ 생성 트랜잭션 │ 삭제 트랜잭션 │ 생성 명령 │ 삭제 명령      │
└─────────────┴──────────┴──────────┴───────────────┘
```

**예시:**

```sql
-- 초기 데이터
INSERT INTO products (id, name, price) VALUES (1, 'iPhone', 1000);
-- xmin=100, xmax=NULL

-- 업데이트
UPDATE products SET price = 1200 WHERE id = 1;
-- 기존 튜플: xmin=100, xmax=101 (삭제 표시)
-- 새 튜플: xmin=101, xmax=NULL (새 버전)

-- 트랜잭션 A (xid=102, 시작 시점 100 이전)
SELECT price FROM products WHERE id = 1;
-- 결과: 1000 (xmin=100인 버전)

-- 트랜잭션 B (xid=103, 시작 시점 101 이후)
SELECT price FROM products WHERE id = 1;
-- 결과: 1200 (xmin=101인 버전)
```

### 4.3 Vacuum

**문제:**
- 오래된 튜플 버전이 계속 쌓임
- 디스크 공간 낭비
- 성능 저하

**해결: VACUUM**

```sql
-- 수동 Vacuum
VACUUM products;

-- Full Vacuum (테이블 재구성)
VACUUM FULL products;

-- Analyze 포함 (통계 갱신)
VACUUM ANALYZE products;

-- Auto Vacuum 설정 확인
SELECT schemaname, tablename, last_vacuum, last_autovacuum
FROM pg_stat_user_tables
WHERE tablename = 'products';
```

**Auto Vacuum 설정:**

```sql
-- postgresql.conf
autovacuum = on
autovacuum_naptime = 1min
autovacuum_vacuum_threshold = 50
autovacuum_vacuum_scale_factor = 0.2
```

---

## 5. 락(Lock) 메커니즘

### 5.1 락 타입

| 락 모드 | 설명 | 사용 시점 |
|---------|------|----------|
| **ACCESS SHARE** | SELECT | 읽기 |
| **ROW SHARE** | SELECT FOR UPDATE | 행 단위 잠금 |
| **ROW EXCLUSIVE** | INSERT, UPDATE, DELETE | 행 수정 |
| **SHARE UPDATE EXCLUSIVE** | VACUUM, CREATE INDEX | DDL |
| **SHARE** | CREATE INDEX (non-concurrent) | 공유 잠금 |
| **ACCESS EXCLUSIVE** | DROP TABLE, TRUNCATE | 배타적 잠금 |

### 5.2 명시적 락

```sql
-- 행 단위 잠금 (SELECT FOR UPDATE)
BEGIN;
SELECT * FROM products WHERE id = 456 FOR UPDATE;
-- 다른 트랜잭션이 이 행을 UPDATE/DELETE 불가

UPDATE products SET stock_quantity = stock_quantity - 1 WHERE id = 456;
COMMIT;

-- 공유 잠금 (SELECT FOR SHARE)
BEGIN;
SELECT * FROM products WHERE id = 456 FOR SHARE;
-- 다른 트랜잭션이 읽기 가능, 수정 불가
COMMIT;

-- 테이블 잠금
BEGIN;
LOCK TABLE products IN SHARE MODE;
-- 다른 트랜잭션이 읽기만 가능
COMMIT;
```

### 5.3 락 확인

```sql
-- 현재 락 조회
SELECT
    locktype,
    relation::regclass AS table_name,
    mode,
    transactionid,
    pid,
    granted
FROM pg_locks
WHERE NOT granted
ORDER BY pid;

-- 대기 중인 락 조회
SELECT
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_statement,
    blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

---

## 6. Deadlock (교착 상태)

### 6.1 Deadlock이란?

두 개 이상의 트랜잭션이 **서로의 락을 기다리며 무한 대기**하는 상태입니다.

**예시:**

```sql
-- Session A
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE user_id = 1;
-- (잠시 대기)

-- Session B
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE user_id = 2;
-- (잠시 대기)

-- Session A (계속)
UPDATE accounts SET balance = balance + 100 WHERE user_id = 2;
-- user_id=2가 Session B에 의해 락됨, 대기...

-- Session B (계속)
UPDATE accounts SET balance = balance + 100 WHERE user_id = 1;
-- user_id=1이 Session A에 의해 락됨, 대기...
-- ERROR: deadlock detected
```

### 6.2 Deadlock 방지 전략

**1. 일관된 락 순서**

```sql
-- ❌ 나쁜 예
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE user_id = ? ;  -- 랜덤 순서
UPDATE accounts SET balance = balance + 100 WHERE user_id = ? ;
COMMIT;

-- ✅ 좋은 예
BEGIN;
-- 항상 작은 user_id부터 락 획득
UPDATE accounts SET balance = balance - 100 WHERE user_id = LEAST(from_user, to_user);
UPDATE accounts SET balance = balance + 100 WHERE user_id = GREATEST(from_user, to_user);
COMMIT;
```

**2. 짧은 트랜잭션**

```sql
-- ❌ 나쁜 예: 긴 트랜잭션
BEGIN;
UPDATE products SET stock_quantity = stock_quantity - 1 WHERE id = 456;
-- 외부 API 호출 (5초 대기)
-- 이메일 발송 (3초 대기)
COMMIT;

-- ✅ 좋은 예: 짧은 트랜잭션
BEGIN;
UPDATE products SET stock_quantity = stock_quantity - 1 WHERE id = 456;
COMMIT;
-- 외부 작업은 트랜잭션 밖에서
```

**3. 재시도 로직**

```java
// Spring
@Transactional
public void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {
    int maxRetries = 3;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        try {
            // 락 순서 보장
            Long firstId = Math.min(fromUserId, toUserId);
            Long secondId = Math.max(fromUserId, toUserId);

            Account first = accountRepository.findByIdForUpdate(firstId);
            Account second = accountRepository.findByIdForUpdate(secondId);

            if (fromUserId.equals(firstId)) {
                first.withdraw(amount);
                second.deposit(amount);
            } else {
                second.withdraw(amount);
                first.deposit(amount);
            }

            return;
        } catch (DeadlockLoserDataAccessException e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw new BusinessException("Transfer failed after retries", e);
            }
            // 짧은 대기 후 재시도
            Thread.sleep(100 * retryCount);
        }
    }
}
```

**4. Deadlock Timeout 설정**

```sql
-- postgresql.conf
deadlock_timeout = 1s  -- 1초 후 deadlock 감지
```

---

## 7. Portal Universe: 주문 처리 트랜잭션

### 7.1 주문 생성 프로세스

```sql
-- 1단계: 재고 확인 및 차감 (비관적 락)
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 상품 재고 확인 (FOR UPDATE로 락 획득)
SELECT id, name, price, stock_quantity
FROM products
WHERE id IN (456, 789)
FOR UPDATE;

-- 재고 부족 체크
SELECT id FROM products
WHERE id IN (456, 789) AND stock_quantity < requested_quantity;
-- 재고 부족 시 ROLLBACK;

-- 재고 차감
UPDATE products
SET stock_quantity = stock_quantity - 1
WHERE id = 456;

UPDATE products
SET stock_quantity = stock_quantity - 2
WHERE id = 789;

-- 2단계: 주문 생성
INSERT INTO orders (
    order_number,
    user_id,
    total_amount,
    status,
    shipping_address,
    created_at
) VALUES (
    'ORD-2024-000123',
    12345,
    150000,
    'PENDING',
    '{"city": "Seoul", "address": "Gangnam-gu"}'::JSONB,
    NOW()
) RETURNING id;

-- 3단계: 주문 아이템 생성
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
VALUES
    (LASTVAL(), 456, 1, 50000),
    (LASTVAL(), 789, 2, 50000);

COMMIT;
```

### 7.2 동시성 제어 시나리오

**시나리오 1: 재고 부족 방지**

```sql
-- 두 사용자가 마지막 1개 상품 동시 주문

-- User A
BEGIN;
SELECT stock_quantity FROM products WHERE id = 456 FOR UPDATE;
-- stock_quantity = 1

-- User B
BEGIN;
SELECT stock_quantity FROM products WHERE id = 456 FOR UPDATE;
-- 대기... (User A의 락 해제 대기)

-- User A (계속)
UPDATE products SET stock_quantity = 0 WHERE id = 456;
INSERT INTO orders (user_id, total_amount) VALUES (123, 50000);
COMMIT;
-- User A 주문 성공

-- User B (계속, 락 획득)
SELECT stock_quantity FROM products WHERE id = 456;
-- stock_quantity = 0
ROLLBACK;
-- User B 주문 실패 (재고 부족)
```

**시나리오 2: 결제 처리 (SERIALIZABLE)**

```sql
-- 이중 결제 방지
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 주문 상태 확인
SELECT status FROM orders WHERE id = 12345 FOR UPDATE;
-- status = 'PENDING'

-- 결제 처리
UPDATE orders SET status = 'PAID', paid_at = NOW() WHERE id = 12345;

-- 결제 내역 생성
INSERT INTO payments (order_id, amount, method, status)
VALUES (12345, 150000, 'CARD', 'COMPLETED');

COMMIT;
```

### 7.3 Spring Boot 트랜잭션 구현

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * 주문 생성 (READ COMMITTED)
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 재고 확인 및 차감 (비관적 락)
        List<Product> products = productRepository
            .findAllByIdInForUpdate(request.getProductIds());

        // 재고 검증
        for (OrderItemRequest item : request.getItems()) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new InsufficientStockException(product.getName());
            }

            // 재고 차감
            product.decreaseStock(item.getQuantity());
        }

        // 2. 주문 생성
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(request.getUserId())
            .totalAmount(calculateTotalAmount(request.getItems()))
            .status(OrderStatus.PENDING)
            .shippingAddress(request.getShippingAddress())
            .build();

        order = orderRepository.save(order);

        // 3. 주문 아이템 생성
        List<OrderItem> orderItems = request.getItems().stream()
            .map(item -> OrderItem.builder()
                .order(order)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build())
            .collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        return OrderResponse.from(order, orderItems);
    }

    /**
     * 결제 처리 (SERIALIZABLE)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public PaymentResponse processPayment(Long orderId, PaymentRequest request) {
        // 1. 주문 조회 및 락
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 결제 가능 상태 확인
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStatusException(
                "Order already processed: " + order.getStatus());
        }

        // 3. 결제 처리 (외부 API 호출은 트랜잭션 밖에서)
        // ...

        // 4. 주문 상태 업데이트
        order.markAsPaid();

        // 5. 결제 내역 생성
        Payment payment = Payment.builder()
            .orderId(orderId)
            .amount(order.getTotalAmount())
            .method(request.getPaymentMethod())
            .status(PaymentStatus.COMPLETED)
            .build();

        paymentRepository.save(payment);

        return PaymentResponse.from(payment);
    }

    /**
     * 주문 취소 (재고 복구)
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 1. 주문 조회
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. 취소 가능 상태 확인
        if (!order.isCancellable()) {
            throw new OrderNotCancellableException(orderId);
        }

        // 3. 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<Long> productIds = orderItems.stream()
            .map(OrderItem::getProductId)
            .collect(Collectors.toList());

        List<Product> products = productRepository.findAllByIdInForUpdate(productIds);

        for (OrderItem item : orderItems) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow();

            product.increaseStock(item.getQuantity());
        }

        // 4. 주문 상태 업데이트
        order.cancel();
    }
}
```

### 7.4 Repository 구현

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 비관적 락으로 상품 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    /**
     * 낙관적 락으로 상품 조회
     */
    @Lock(LockModeType.OPTIMISTIC)
    Optional<Product> findById(Long id);
}

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 비관적 락으로 주문 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
```

### 7.5 낙관적 락 vs 비관적 락

| 특성 | 낙관적 락 | 비관적 락 |
|------|----------|----------|
| **구현** | @Version 컬럼 | FOR UPDATE |
| **충돌 처리** | 커밋 시점 예외 발생 | 대기 후 획득 |
| **성능** | 높음 (락 없음) | 낮음 (대기 발생) |
| **적합 시나리오** | 충돌 드문 경우 | 충돌 빈번한 경우 |
| **Portal Universe** | 게시물 수정 | 재고 차감, 결제 |

**낙관적 락 예시:**

```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer stockQuantity;

    @Version  // 낙관적 락
    private Long version;

    public void decreaseStock(Integer quantity) {
        if (this.stockQuantity < quantity) {
            throw new InsufficientStockException();
        }
        this.stockQuantity -= quantity;
    }
}

// 사용
try {
    product.decreaseStock(5);
    productRepository.save(product);
} catch (OptimisticLockingFailureException e) {
    // 다른 트랜잭션이 먼저 수정함, 재시도 필요
    throw new ConcurrentUpdateException("Product updated by another transaction");
}
```

---

## 8. 트랜잭션 모니터링

### 8.1 활성 트랜잭션 조회

```sql
SELECT
    pid,
    usename,
    application_name,
    state,
    query_start,
    state_change,
    NOW() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state != 'idle'
    AND xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

### 8.2 긴 트랜잭션 찾기

```sql
SELECT
    pid,
    usename,
    NOW() - xact_start AS duration,
    state,
    query
FROM pg_stat_activity
WHERE xact_start < NOW() - INTERVAL '5 minutes'
ORDER BY xact_start;
```

### 8.3 트랜잭션 종료

```sql
-- 특정 프로세스 종료
SELECT pg_cancel_backend(12345);  -- 실행 중인 쿼리 취소
SELECT pg_terminate_backend(12345);  -- 강제 종료
```

---

## 9. 핵심 정리

| 개념 | 설명 | 사용 시기 |
|------|------|----------|
| **READ COMMITTED** | 커밋된 데이터만 읽기 | 일반 OLTP |
| **REPEATABLE READ** | 일관된 스냅샷 읽기 | 보고서, 배치 |
| **SERIALIZABLE** | 직렬 실행 보장 | 금융, 재고 관리 |
| **MVCC** | 다중 버전 동시성 제어 | 자동 (PostgreSQL 내부) |
| **FOR UPDATE** | 비관적 락 | 재고 차감, 결제 |
| **@Version** | 낙관적 락 | 게시물 수정 |
| **Vacuum** | 오래된 튜플 제거 | 자동 (Auto Vacuum) |

---

## 10. 실습 예제

### 실습 1: 격리 수준 비교

```sql
-- Terminal 1
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT * FROM products WHERE id = 1;
-- price = 1000

-- Terminal 2
BEGIN;
UPDATE products SET price = 1500 WHERE id = 1;
COMMIT;

-- Terminal 1 (계속)
SELECT * FROM products WHERE id = 1;
-- price = 1500 (변경됨)
COMMIT;

-- 다시 REPEATABLE READ로 테스트
-- Terminal 1
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT * FROM products WHERE id = 1;
-- price = 1500

-- Terminal 2
BEGIN;
UPDATE products SET price = 2000 WHERE id = 1;
COMMIT;

-- Terminal 1 (계속)
SELECT * FROM products WHERE id = 1;
-- price = 1500 (여전히 동일)
COMMIT;
```

### 실습 2: Deadlock 재현

```sql
-- Terminal 1
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE user_id = 1;

-- Terminal 2
BEGIN;
UPDATE accounts SET balance = balance - 100 WHERE user_id = 2;

-- Terminal 1
UPDATE accounts SET balance = balance + 100 WHERE user_id = 2;
-- 대기...

-- Terminal 2
UPDATE accounts SET balance = balance + 100 WHERE user_id = 1;
-- ERROR: deadlock detected
```

---

## 11. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL 성능 튜닝](./postgresql-performance-tuning.md)
- [PostgreSQL Spring 통합](./postgresql-spring-integration.md)

---

## 12. 참고 자료

- [PostgreSQL Transaction Documentation](https://www.postgresql.org/docs/current/tutorial-transactions.html)
- [PostgreSQL MVCC](https://www.postgresql.org/docs/current/mvcc.html)
- [PostgreSQL Isolation Levels](https://www.postgresql.org/docs/current/transaction-iso.html)
- [Understanding Deadlocks](https://www.postgresql.org/docs/current/explicit-locking.html)
