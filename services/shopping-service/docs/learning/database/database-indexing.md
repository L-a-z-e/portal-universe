# Database Indexing

## 개요

인덱스는 데이터베이스 쿼리 성능을 향상시키는 핵심 요소입니다. shopping-service에서는 자주 조회되는 컬럼에 적절한 인덱스를 설계하여 쿼리 성능을 최적화합니다.

---

## 1. 인덱스 기본 개념

### 1.1 B-Tree 인덱스

MySQL InnoDB의 기본 인덱스 구조:

```
                    [M]
                   /   \
               [D,H]    [P,X]
              /  |  \   /  |  \
           [A-C][E-G][I-L][N-O][Q-W][Y-Z]
```

- 정렬된 상태 유지
- 범위 검색에 효율적
- `=`, `>`, `<`, `BETWEEN`, `LIKE 'prefix%'` 지원

### 1.2 Clustered vs Non-Clustered

| 구분 | Clustered Index | Non-Clustered Index |
|------|-----------------|---------------------|
| 정의 | 테이블 데이터 자체 정렬 | 별도 인덱스 구조 |
| 개수 | 테이블당 1개 | 여러 개 가능 |
| InnoDB | Primary Key | Secondary Index |
| 데이터 접근 | 직접 접근 | PK를 통해 접근 |

---

## 2. shopping-service 인덱스 설계

### 2.1 주문 테이블 (orders)

```sql
-- V4__Create_order_tables.sql

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- Clustered Index
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- ...
);

-- 인덱스
CREATE UNIQUE INDEX idx_order_number ON orders(order_number);
CREATE INDEX idx_order_user_id ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at);
```

JPA Entity에서:
```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "order_number", unique = true),
    @Index(name = "idx_order_user_id", columnList = "user_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created_at", columnList = "created_at")
})
public class Order { }
```

### 2.2 재고 이동 이력 (stock_movements)

```sql
-- V2__Create_inventory_tables.sql

CREATE TABLE stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- ...
);

-- 인덱스
CREATE INDEX idx_stock_movement_inventory_id ON stock_movements(inventory_id);
CREATE INDEX idx_stock_movement_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movement_reference ON stock_movements(reference_type, reference_id);  -- 복합
CREATE INDEX idx_stock_movement_created_at ON stock_movements(created_at);
```

JPA Entity에서:
```java
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_movement_inventory_id", columnList = "inventory_id"),
    @Index(name = "idx_stock_movement_product_id", columnList = "product_id"),
    @Index(name = "idx_stock_movement_reference", columnList = "reference_type, reference_id"),
    @Index(name = "idx_stock_movement_created_at", columnList = "created_at")
})
public class StockMovement { }
```

### 2.3 배송 테이블 (deliveries)

```java
@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_delivery_tracking_number", columnList = "tracking_number", unique = true),
    @Index(name = "idx_delivery_order_id", columnList = "order_id"),
    @Index(name = "idx_delivery_status", columnList = "status")
})
public class Delivery { }
```

### 2.4 장바구니 항목 (cart_items)

```java
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_item_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_item_product_id", columnList = "product_id")
})
public class CartItem { }
```

---

## 3. 인덱스 설계 전략

### 3.1 단일 컬럼 인덱스

**사용 케이스**: 단일 컬럼으로 자주 검색

```java
// Repository
Optional<Order> findByOrderNumber(String orderNumber);
Page<Order> findByUserId(String userId, Pageable pageable);

// 인덱스
CREATE UNIQUE INDEX idx_order_number ON orders(order_number);
CREATE INDEX idx_order_user_id ON orders(user_id);
```

### 3.2 복합 인덱스 (Composite Index)

**사용 케이스**: 여러 컬럼을 함께 검색

```java
// Repository - 복합 조건 검색
Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(
    String userId,
    OrderStatus status,
    Pageable pageable
);

// 최적의 복합 인덱스
CREATE INDEX idx_order_user_status_created
    ON orders(user_id, status, created_at DESC);
```

**컬럼 순서가 중요!**
```sql
-- 인덱스: (user_id, status, created_at)

-- 인덱스 사용 O
WHERE user_id = ?
WHERE user_id = ? AND status = ?
WHERE user_id = ? AND status = ? ORDER BY created_at

-- 인덱스 사용 X (첫 컬럼 생략)
WHERE status = ?
WHERE status = ? AND created_at = ?
```

### 3.3 커버링 인덱스 (Covering Index)

쿼리에 필요한 모든 컬럼을 인덱스에 포함:

```sql
-- 인덱스
CREATE INDEX idx_order_covering
    ON orders(user_id, status, order_number, total_amount, created_at);

-- 커버링 인덱스 쿼리 (테이블 접근 불필요)
SELECT order_number, total_amount, created_at
FROM orders
WHERE user_id = 'user123' AND status = 'PENDING';
```

### 3.4 장바구니 복합 인덱스

```java
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_cart_user_status", columnList = "user_id, status")
})
public class Cart { }
```

```java
// 활성 장바구니 조회에 최적화
Optional<Cart> findByUserIdAndStatus(String userId, CartStatus status);
```

---

## 4. 인덱스 분석

### 4.1 EXPLAIN 사용

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 'user123' AND status = 'PENDING';

-- 결과
| type  | possible_keys                    | key                   | rows | Extra       |
|-------|----------------------------------|-----------------------|------|-------------|
| ref   | idx_order_user_id,idx_user_status| idx_user_status       | 10   | Using where |
```

### 4.2 주요 지표

| 지표 | 설명 | 좋은 값 |
|------|------|--------|
| `type` | 접근 방식 | ref, eq_ref, const |
| `possible_keys` | 사용 가능 인덱스 | 존재해야 함 |
| `key` | 실제 사용 인덱스 | NULL이 아님 |
| `rows` | 스캔 행 수 | 적을수록 좋음 |
| `Extra` | 추가 정보 | Using index (커버링) |

### 4.3 type 우선순위 (좋음 → 나쁨)

1. `const` / `eq_ref` - PK/Unique 검색
2. `ref` - 인덱스 동등 검색
3. `range` - 인덱스 범위 검색
4. `index` - 인덱스 전체 스캔
5. `ALL` - 테이블 전체 스캔 (피해야 함)

---

## 5. 인덱스 사용 안 되는 경우

### 5.1 함수/연산 적용

```sql
-- 인덱스 사용 X
SELECT * FROM orders WHERE YEAR(created_at) = 2024;

-- 개선: 범위 조건으로 변경
SELECT * FROM orders
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01';
```

### 5.2 LIKE 와일드카드 시작

```sql
-- 인덱스 사용 X
SELECT * FROM products WHERE name LIKE '%phone';

-- 인덱스 사용 O
SELECT * FROM products WHERE name LIKE 'phone%';
```

### 5.3 OR 조건

```sql
-- 인덱스 비효율 (인덱스 머지 또는 풀스캔)
SELECT * FROM orders WHERE user_id = 'a' OR status = 'PENDING';

-- 개선: UNION 사용
SELECT * FROM orders WHERE user_id = 'a'
UNION
SELECT * FROM orders WHERE status = 'PENDING';
```

### 5.4 NOT 조건

```sql
-- 인덱스 사용 어려움
SELECT * FROM orders WHERE status != 'CANCELLED';

-- 개선: IN 조건 사용
SELECT * FROM orders WHERE status IN ('PENDING', 'CONFIRMED', 'PAID', 'DELIVERED');
```

---

## 6. 인덱스 관리

### 6.1 인덱스 조회

```sql
-- 테이블의 인덱스 확인
SHOW INDEX FROM orders;

-- 인덱스 사용 통계
SELECT * FROM sys.schema_index_statistics WHERE table_schema = 'shopping';
```

### 6.2 인덱스 생성/삭제

```sql
-- 인덱스 생성
CREATE INDEX idx_new_index ON orders(column_name);

-- 인덱스 삭제
DROP INDEX idx_old_index ON orders;

-- 인덱스 재생성 (통계 갱신)
ALTER TABLE orders DROP INDEX idx_order_user_id;
ALTER TABLE orders ADD INDEX idx_order_user_id (user_id);
```

### 6.3 통계 갱신

```sql
ANALYZE TABLE orders;
```

---

## 7. 인덱스와 락

### 7.1 Pessimistic Lock과 인덱스

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
Optional<Inventory> findByProductIdWithLock(@Param("productId") Long productId);
```

인덱스가 있으면:
- **Row Lock**: 해당 행만 잠금
- 인덱스를 통해 빠르게 행 찾음

인덱스가 없으면:
- **Table Lock** 또는 넓은 범위 잠금
- 동시성 급격히 저하

### 7.2 재고 인덱스 설계

```sql
-- 재고 조회에 최적화된 Unique 인덱스
CREATE UNIQUE INDEX idx_inventory_product_id ON inventory(product_id);
```

```java
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "product_id", unique = true)
})
public class Inventory { }
```

---

## 8. 인덱스 설계 체크리스트

### 8.1 인덱스 추가 고려

- [ ] WHERE 절에 자주 사용되는 컬럼
- [ ] JOIN 조건에 사용되는 컬럼
- [ ] ORDER BY에 사용되는 컬럼
- [ ] 외래키 컬럼
- [ ] 유니크 제약 필요 컬럼

### 8.2 인덱스 주의사항

- [ ] 과도한 인덱스는 INSERT/UPDATE 성능 저하
- [ ] 카디널리티(고유값 비율)가 낮은 컬럼은 비효율
- [ ] 복합 인덱스 컬럼 순서 확인
- [ ] 정기적인 통계 갱신

### 8.3 shopping-service 인덱스 요약

| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| orders | idx_order_number (unique) | 주문번호 조회 |
| orders | idx_order_user_id | 사용자별 주문 조회 |
| orders | idx_order_status | 상태별 필터링 |
| orders | idx_order_created_at | 정렬/범위 검색 |
| inventory | idx_inventory_product_id (unique) | 상품별 재고 조회 + 락 |
| stock_movements | idx_stock_movement_reference | 참조 타입+ID 복합 검색 |
| carts | idx_cart_user_status | 사용자+상태 복합 검색 |
| deliveries | idx_delivery_tracking_number (unique) | 운송장 조회 |

---

## 관련 문서

- [JPA Query Optimization](./jpa-query-optimization.md) - 쿼리 최적화
- [JPA Locking](./jpa-locking.md) - 락과 인덱스
- [Flyway Migration](./flyway-migration.md) - 인덱스 마이그레이션
