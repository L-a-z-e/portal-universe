# PostgreSQL 성능 튜닝

**난이도:** ⭐⭐⭐⭐

## 학습 목표
- EXPLAIN ANALYZE 출력 심층 분석
- 쿼리 최적화 기법 (JOIN, Subquery, Index)
- 파티셔닝 전략 (Range, List, Hash)
- Vacuum 및 ANALYZE 관리
- Connection Pooling 최적화
- Portal Universe Shopping Service 성능 튜닝 실전

---

## 1. EXPLAIN ANALYZE 심층 분석

### 1.1 기본 사용법

```sql
-- 실행 계획만 확인 (실제 실행 안 함)
EXPLAIN
SELECT * FROM products WHERE category_id = 5;

-- 실제 실행 + 성능 측정
EXPLAIN ANALYZE
SELECT * FROM products WHERE category_id = 5;

-- 상세 정보 포함
EXPLAIN (ANALYZE, VERBOSE, BUFFERS)
SELECT * FROM products WHERE category_id = 5;
```

### 1.2 출력 해석

```sql
EXPLAIN ANALYZE
SELECT p.name, c.name AS category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.price > 100000
ORDER BY p.price DESC
LIMIT 10;
```

**출력 예시:**

```
Limit  (cost=1234.56..1456.78 rows=10 width=64) (actual time=5.234..6.789 rows=10 loops=1)
  ->  Sort  (cost=1234.56..1345.67 rows=4444 width=64) (actual time=5.232..5.456 rows=10 loops=1)
        Sort Key: p.price DESC
        Sort Method: top-N heapsort  Memory: 25kB
        ->  Hash Join  (cost=123.45..890.12 rows=4444 width=64) (actual time=1.234..4.567 rows=4500 loops=1)
              Hash Cond: (p.category_id = c.id)
              ->  Seq Scan on products p  (cost=0.00..678.90 rows=4444 width=48) (actual time=0.012..2.345 rows=4500 loops=1)
                    Filter: (price > 100000::numeric)
                    Rows Removed by Filter: 5500
              ->  Hash  (cost=100.00..100.00 rows=1876 width=24) (actual time=1.123..1.124 rows=20 loops=1)
                    Buckets: 2048  Batches: 1  Memory Usage: 18kB
                    ->  Seq Scan on categories c  (cost=0.00..100.00 rows=1876 width=24) (actual time=0.005..0.567 rows=20 loops=1)
Planning Time: 0.456 ms
Execution Time: 6.890 ms
```

### 1.3 주요 메트릭

| 메트릭 | 설명 | 해석 |
|--------|------|------|
| **cost** | `시작 비용..총 비용` | 추정치 (낮을수록 좋음) |
| **rows** | 추정 반환 행 수 | 실제와 비교 |
| **width** | 평균 행 크기 (bytes) | - |
| **actual time** | `첫 행..마지막 행` (ms) | 실제 시간 |
| **rows** (actual) | 실제 반환 행 수 | 추정치와 비교 |
| **loops** | 반복 횟수 | 1에 가까울수록 좋음 |
| **Planning Time** | 계획 수립 시간 | - |
| **Execution Time** | 실제 실행 시간 | **핵심 지표** |

### 1.4 스캔 타입별 성능

| 스캔 타입 | 속도 | 사용 시점 | 비고 |
|----------|------|----------|------|
| **Index-Only Scan** | ⚡⚡⚡⚡⚡ | Covering Index | 테이블 접근 없음 |
| **Index Scan** | ⚡⚡⚡⚡ | 선택도 높음 (< 5%) | 인덱스 + 테이블 |
| **Bitmap Index Scan** | ⚡⚡⚡ | 중간 선택도 (5~20%) | 여러 인덱스 병합 |
| **Seq Scan** | ⚡ | 선택도 낮음 (> 20%) | 전체 테이블 스캔 |

**예시:**

```sql
-- ❌ Seq Scan (느림)
EXPLAIN ANALYZE
SELECT * FROM products WHERE price > 10000;
/*
Seq Scan on products  (cost=0.00..1234.56 rows=50000 width=128)
  Filter: (price > 10000)
  Rows Removed by Filter: 50000
Execution Time: 25.678 ms
*/

-- ✅ Index Scan (빠름)
CREATE INDEX idx_products_price ON products(price);

EXPLAIN ANALYZE
SELECT * FROM products WHERE price > 100000;
/*
Index Scan using idx_products_price on products  (cost=0.29..234.56 rows=500 width=128)
  Index Cond: (price > 100000)
Execution Time: 2.345 ms
*/

-- ✅ Index-Only Scan (매우 빠름)
CREATE INDEX idx_products_price_covering
ON products(price) INCLUDE (id, name, category_id);

EXPLAIN ANALYZE
SELECT id, name, price, category_id FROM products WHERE price > 100000;
/*
Index-Only Scan using idx_products_price_covering on products  (cost=0.29..123.45 rows=500 width=24)
  Index Cond: (price > 100000)
  Heap Fetches: 0
Execution Time: 1.234 ms
*/
```

---

## 2. 쿼리 최적화 기법

### 2.1 JOIN 최적화

**Nested Loop vs Hash Join vs Merge Join**

| JOIN 타입 | 조건 | 성능 | 사용 시점 |
|----------|------|------|----------|
| **Nested Loop** | 작은 테이블 × 큰 테이블 | ⚡⚡ | 인덱스 사용 가능 |
| **Hash Join** | 중간 크기 테이블 | ⚡⚡⚡⚡ | 동등 조인 |
| **Merge Join** | 정렬된 테이블 | ⚡⚡⚡ | 범위 조인 |

**예시:**

```sql
-- ❌ 나쁜 예: 카르테시안 곱
SELECT p.name, c.name
FROM products p, categories c
WHERE p.price > 100000;  -- JOIN 조건 누락!
-- 결과: 100만 × 20 = 2000만 행

-- ✅ 좋은 예: 명시적 JOIN
SELECT p.name, c.name
FROM products p
INNER JOIN categories c ON p.category_id = c.id
WHERE p.price > 100000;
```

**JOIN 순서 최적화**

```sql
-- ❌ 나쁜 예: 큰 테이블 먼저 조인
SELECT *
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE p.category_id = 5;
-- 1. orders (100만) × order_items (300만) = 대량 중간 결과
-- 2. 중간 결과 × products 필터링

-- ✅ 좋은 예: 작은 테이블/필터링 먼저
SELECT *
FROM products p
JOIN order_items oi ON p.id = oi.product_id
JOIN orders o ON oi.order_id = o.id
WHERE p.category_id = 5;
-- 1. products 필터링 (category_id = 5) → 100개
-- 2. order_items 조인 (100개)
-- 3. orders 조인

-- 더 나은 방법: CTE 사용
WITH filtered_products AS (
    SELECT id FROM products WHERE category_id = 5
)
SELECT *
FROM filtered_products fp
JOIN order_items oi ON fp.id = oi.product_id
JOIN orders o ON oi.order_id = o.id;
```

### 2.2 Subquery 최적화

**Correlated Subquery → JOIN 변환**

```sql
-- ❌ 나쁜 예: Correlated Subquery (각 행마다 실행)
SELECT p.name, p.price
FROM products p
WHERE p.price > (
    SELECT AVG(price)
    FROM products p2
    WHERE p2.category_id = p.category_id  -- 상관 서브쿼리
);
-- Execution Time: 5000ms (100만 행 × 서브쿼리)

-- ✅ 좋은 예: JOIN으로 변환
WITH category_avg AS (
    SELECT category_id, AVG(price) AS avg_price
    FROM products
    GROUP BY category_id
)
SELECT p.name, p.price
FROM products p
JOIN category_avg ca ON p.category_id = ca.category_id
WHERE p.price > ca.avg_price;
-- Execution Time: 50ms
```

**IN vs EXISTS**

```sql
-- ✅ IN: 서브쿼리 결과가 작을 때
SELECT * FROM products
WHERE category_id IN (
    SELECT id FROM categories WHERE name LIKE 'Electronics%'
);
-- categories 테이블 작음 (20개) → Hash 생성 후 빠른 검색

-- ✅ EXISTS: 서브쿼리 결과가 클 때
SELECT * FROM products p
WHERE EXISTS (
    SELECT 1 FROM order_items oi
    WHERE oi.product_id = p.id
);
-- order_items 테이블 큼 (300만) → 존재만 확인 (Short-circuit)

-- ❌ NOT IN: NULL 문제
SELECT * FROM products
WHERE category_id NOT IN (SELECT id FROM archived_categories);
-- NULL이 있으면 결과 없음!

-- ✅ NOT EXISTS: 안전
SELECT * FROM products p
WHERE NOT EXISTS (
    SELECT 1 FROM archived_categories ac WHERE ac.id = p.category_id
);
```

### 2.3 인덱스 최적화

**Covering Index**

```sql
-- 자주 실행되는 쿼리
SELECT id, name, price FROM products
WHERE category_id = 5
ORDER BY price DESC
LIMIT 10;

-- ✅ Covering Index (모든 컬럼 포함)
CREATE INDEX idx_products_category_price_covering
ON products(category_id, price DESC)
INCLUDE (id, name)
WHERE is_deleted = FALSE;

EXPLAIN ANALYZE
SELECT id, name, price FROM products
WHERE category_id = 5 AND is_deleted = FALSE
ORDER BY price DESC
LIMIT 10;
/*
Index-Only Scan using idx_products_category_price_covering
  Heap Fetches: 0  ← 테이블 접근 없음!
Execution Time: 0.234 ms
*/
```

**Partial Index**

```sql
-- 활성 제품만 검색 (80%)
SELECT * FROM products
WHERE is_deleted = FALSE AND category_id = 5;

-- ❌ 전체 인덱스
CREATE INDEX idx_products_category ON products(category_id);
-- 크기: 50MB

-- ✅ 부분 인덱스
CREATE INDEX idx_products_category_active
ON products(category_id)
WHERE is_deleted = FALSE;
-- 크기: 40MB (20% 감소)
-- 성능: 더 빠름 (인덱스 작음)
```

---

## 3. 파티셔닝 (Partitioning)

### 3.1 파티셔닝 개요

**대용량 테이블을 논리적으로 분할**하여 쿼리 성능 및 관리 효율성 향상.

**장점:**
- 쿼리 성능 향상 (파티션 프루닝)
- 데이터 관리 용이 (오래된 파티션 삭제)
- 인덱스 크기 감소
- Vacuum 시간 단축

**단점:**
- 복잡성 증가
- 파티션 키 선택 중요

### 3.2 Range Partitioning (범위 파티셔닝)

**시계열 데이터에 적합**

```sql
-- 주문 테이블 (월별 파티셔닝)
CREATE TABLE orders (
    id BIGSERIAL,
    order_number VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 파티션 생성
CREATE TABLE orders_2024_01 PARTITION OF orders
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE orders_2024_02 PARTITION OF orders
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE orders_2024_03 PARTITION OF orders
FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

-- 기본 파티션 (범위 밖)
CREATE TABLE orders_default PARTITION OF orders DEFAULT;

-- 인덱스 (각 파티션에 자동 생성)
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at);
CREATE INDEX idx_orders_status ON orders(status);

-- 쿼리 (파티션 프루닝)
EXPLAIN ANALYZE
SELECT * FROM orders
WHERE created_at BETWEEN '2024-02-01' AND '2024-02-29';
/*
Seq Scan on orders_2024_02  ← 하나의 파티션만 스캔!
Execution Time: 5.234 ms
*/
```

**자동 파티션 생성 (pg_partman 확장)**

```sql
CREATE EXTENSION pg_partman;

SELECT partman.create_parent(
    p_parent_table := 'public.orders',
    p_control := 'created_at',
    p_type := 'native',
    p_interval := 'monthly',
    p_premake := 3  -- 3개월 미리 생성
);
```

### 3.3 List Partitioning (목록 파티셔닝)

**카테고리별 분할**

```sql
-- 제품 테이블 (카테고리별 파티셔닝)
CREATE TABLE products (
    id BIGSERIAL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (id, category_id)
) PARTITION BY LIST (category_id);

-- 파티션 생성
CREATE TABLE products_electronics PARTITION OF products
FOR VALUES IN (1, 2, 3);  -- Electronics 관련 카테고리

CREATE TABLE products_fashion PARTITION OF products
FOR VALUES IN (4, 5, 6);  -- Fashion 관련 카테고리

CREATE TABLE products_home PARTITION OF products
FOR VALUES IN (7, 8, 9);  -- Home 관련 카테고리

CREATE TABLE products_other PARTITION OF products DEFAULT;
```

### 3.4 Hash Partitioning (해시 파티셔닝)

**균등 분산**

```sql
-- 사용자 테이블 (해시 파티셔닝)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255)
) PARTITION BY HASH (id);

-- 4개 파티션 생성
CREATE TABLE users_part0 PARTITION OF users
FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE users_part1 PARTITION OF users
FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE users_part2 PARTITION OF users
FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE users_part3 PARTITION OF users
FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

### 3.5 Portal Universe: 주문 파티셔닝 전략

```sql
-- 2024년 1월부터 월별 파티션 생성
DO $$
DECLARE
    start_date DATE := '2024-01-01';
    end_date DATE := '2025-01-01';
    partition_date DATE := start_date;
    partition_name TEXT;
BEGIN
    WHILE partition_date < end_date LOOP
        partition_name := 'orders_' || TO_CHAR(partition_date, 'YYYY_MM');

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF orders
             FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            partition_date,
            partition_date + INTERVAL '1 month'
        );

        partition_date := partition_date + INTERVAL '1 month';
    END LOOP;
END $$;

-- 파티션 확인
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS size
FROM pg_tables
WHERE tablename LIKE 'orders_%'
ORDER BY tablename;
```

---

## 4. Vacuum 및 ANALYZE

### 4.1 Vacuum이란?

**Vacuum**은 삭제/업데이트된 행의 **빈 공간을 회수**하고 **통계를 갱신**합니다.

**문제:**
- UPDATE/DELETE는 기존 행을 삭제 표시만 함 (MVCC)
- Dead Tuple이 쌓여 디스크 낭비 및 성능 저하

**해결: Vacuum**

```sql
-- 일반 Vacuum (빈 공간 재사용 가능하게 표시)
VACUUM products;

-- Full Vacuum (테이블 재구성, 디스크 공간 반환)
VACUUM FULL products;  -- 배타 락 필요, 시간 오래 걸림

-- Analyze 포함 (통계 갱신)
VACUUM ANALYZE products;

-- Verbose (진행 상황 출력)
VACUUM VERBOSE products;
```

### 4.2 Auto Vacuum 설정

```sql
-- postgresql.conf
autovacuum = on
autovacuum_max_workers = 3
autovacuum_naptime = 1min
autovacuum_vacuum_threshold = 50
autovacuum_vacuum_scale_factor = 0.2
autovacuum_analyze_threshold = 50
autovacuum_analyze_scale_factor = 0.1

-- 테이블별 설정
ALTER TABLE orders SET (
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);
```

**Auto Vacuum 트리거 조건:**

```
vacuum_threshold = autovacuum_vacuum_threshold + autovacuum_vacuum_scale_factor * tuples

예시:
- 테이블 행 수: 1,000,000
- threshold: 50 + 0.2 * 1,000,000 = 200,050
- 200,050개 이상 변경 시 Auto Vacuum 실행
```

### 4.3 Bloat 확인

```sql
-- 테이블 Bloat 확인
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname || '.' || tablename)) AS table_size,
    n_dead_tup,
    n_live_tup,
    ROUND(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_tuple_percent
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;

-- Vacuum 필요 여부
SELECT
    schemaname,
    tablename,
    last_vacuum,
    last_autovacuum,
    n_dead_tup,
    CASE
        WHEN last_autovacuum IS NULL THEN 'Never vacuumed'
        WHEN n_dead_tup > 10000 THEN 'Vacuum recommended'
        ELSE 'OK'
    END AS status
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

### 4.4 ANALYZE

```sql
-- 통계 갱신 (쿼리 플래너 최적화)
ANALYZE products;

-- 전체 데이터베이스
ANALYZE;

-- 통계 확인
SELECT
    schemaname,
    tablename,
    last_analyze,
    last_autoanalyze,
    n_mod_since_analyze
FROM pg_stat_user_tables
WHERE n_mod_since_analyze > 10000
ORDER BY n_mod_since_analyze DESC;
```

---

## 5. Connection Pooling 최적화

### 5.1 HikariCP 설정 (재확인)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

### 5.2 PostgreSQL 서버 설정

```sql
-- postgresql.conf

# 최대 연결 수
max_connections = 100

# 연결당 메모리
work_mem = 16MB
maintenance_work_mem = 256MB

# 타임아웃
statement_timeout = 30000  # 30초
idle_in_transaction_session_timeout = 60000  # 60초
```

### 5.3 연결 모니터링

```sql
-- 활성 연결 수
SELECT
    COUNT(*),
    state
FROM pg_stat_activity
GROUP BY state;

-- 긴 트랜잭션 찾기
SELECT
    pid,
    usename,
    application_name,
    state,
    NOW() - xact_start AS duration,
    query
FROM pg_stat_activity
WHERE state != 'idle'
  AND xact_start < NOW() - INTERVAL '5 minutes'
ORDER BY duration DESC;

-- 연결 수 확인
SELECT
    COUNT(*) AS total_connections,
    (SELECT setting::INT FROM pg_settings WHERE name = 'max_connections') AS max_connections
FROM pg_stat_activity;
```

---

## 6. Portal Universe 성능 튜닝 실전

### 6.1 시나리오 1: 느린 제품 검색

**문제:**

```sql
-- 실행 시간: 500ms
SELECT p.*, c.name AS category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.price BETWEEN 50000 AND 100000
  AND p.is_deleted = FALSE
ORDER BY p.created_at DESC
LIMIT 20;
```

**분석:**

```sql
EXPLAIN ANALYZE ...
/*
Seq Scan on products p  ← 전체 테이블 스캔!
  Filter: (price >= 50000 AND price <= 100000 AND NOT is_deleted)
  Rows Removed by Filter: 95000
Execution Time: 523.456 ms
*/
```

**해결:**

```sql
-- 1. 복합 인덱스 생성
CREATE INDEX idx_products_active_price_created
ON products(is_deleted, price, created_at DESC)
WHERE is_deleted = FALSE;

-- 2. Covering Index
CREATE INDEX idx_products_active_price_created_covering
ON products(is_deleted, price, created_at DESC)
INCLUDE (id, name, category_id)
WHERE is_deleted = FALSE;

-- 결과: 실행 시간 5ms (100배 개선)
```

### 6.2 시나리오 2: 주문 통계 쿼리

**문제:**

```sql
-- 실행 시간: 10초
SELECT
    DATE_TRUNC('day', created_at) AS date,
    COUNT(*) AS order_count,
    SUM(total_amount) AS revenue
FROM orders
WHERE created_at >= '2024-01-01'
  AND status IN ('PAID', 'SHIPPED', 'DELIVERED')
GROUP BY DATE_TRUNC('day', created_at)
ORDER BY date;
```

**해결:**

```sql
-- Materialized View 생성
CREATE MATERIALIZED VIEW mv_daily_sales AS
SELECT
    DATE_TRUNC('day', created_at) AS date,
    COUNT(*) AS order_count,
    SUM(total_amount) AS revenue
FROM orders
WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED')
GROUP BY DATE_TRUNC('day', created_at);

CREATE UNIQUE INDEX idx_mv_daily_sales_date ON mv_daily_sales(date);

-- 매일 자정 자동 갱신
CREATE OR REPLACE FUNCTION refresh_daily_sales()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_sales;
END;
$$ LANGUAGE plpgsql;

-- 크론 작업 (pg_cron 확장)
SELECT cron.schedule('refresh-daily-sales', '0 0 * * *', 'SELECT refresh_daily_sales()');

-- 조회: 실행 시간 1ms (10000배 개선)
SELECT * FROM mv_daily_sales WHERE date >= '2024-01-01';
```

### 6.3 시나리오 3: N+1 문제

**문제:**

```java
// 100개 주문 조회 → 100번 쿼리 실행
List<Order> orders = orderRepository.findByUserId(userId);
for (Order order : orders) {
    List<OrderItem> items = order.getOrderItems();  // 각 주문마다 쿼리!
    // ...
}
```

**해결:**

```java
// 방법 1: Fetch Join
@Query("SELECT o FROM Order o " +
       "LEFT JOIN FETCH o.orderItems " +
       "WHERE o.userId = :userId")
List<Order> findByUserIdWithItems(@Param("userId") Long userId);

// 방법 2: Entity Graph
@EntityGraph(attributePaths = {"orderItems"})
List<Order> findByUserId(Long userId);

// 결과: 1번 쿼리로 모든 데이터 조회
```

---

## 7. 핵심 정리

| 기법 | 사용 시기 | 효과 |
|------|----------|------|
| **Covering Index** | 자주 조회되는 컬럼 | 테이블 접근 제거 |
| **Partial Index** | 일부 데이터만 검색 | 인덱스 크기 감소 |
| **파티셔닝** | 대용량 테이블 (> 10GB) | 쿼리 성능 향상 |
| **Materialized View** | 복잡한 집계 쿼리 | 100~1000배 개선 |
| **Vacuum** | 정기적 유지보수 | Bloat 제거 |
| **Connection Pooling** | 모든 애플리케이션 | 연결 오버헤드 감소 |

---

## 8. 실습 예제

### 실습 1: 인덱스 전/후 비교

```sql
-- 테스트 데이터 생성
CREATE TABLE test_products AS
SELECT
    id,
    'Product ' || id AS name,
    (random() * 1000000)::DECIMAL(10,2) AS price,
    (random() * 100)::INTEGER AS category_id,
    random() > 0.2 AS is_deleted
FROM generate_series(1, 1000000) AS id;

-- 인덱스 없이 검색
EXPLAIN ANALYZE
SELECT * FROM test_products
WHERE category_id = 50 AND price > 500000 AND is_deleted = FALSE;
-- Execution Time: ~300ms

-- 인덱스 생성
CREATE INDEX idx_test_products
ON test_products(category_id, price)
WHERE is_deleted = FALSE;

-- 인덱스로 검색
EXPLAIN ANALYZE
SELECT * FROM test_products
WHERE category_id = 50 AND price > 500000 AND is_deleted = FALSE;
-- Execution Time: ~5ms (60배 개선)
```

### 실습 2: 파티셔닝 효과

```sql
-- 파티셔닝 전
CREATE TABLE orders_non_partitioned (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    total_amount DECIMAL(10, 2)
);

INSERT INTO orders_non_partitioned (created_at, total_amount)
SELECT
    '2024-01-01'::TIMESTAMP + (random() * 365)::INTEGER * INTERVAL '1 day',
    (random() * 100000)::DECIMAL(10,2)
FROM generate_series(1, 10000000);

EXPLAIN ANALYZE
SELECT COUNT(*) FROM orders_non_partitioned
WHERE created_at BETWEEN '2024-06-01' AND '2024-06-30';
-- Execution Time: ~2000ms

-- 파티셔닝 후 (월별)
-- (파티션 생성 코드 생략)

EXPLAIN ANALYZE
SELECT COUNT(*) FROM orders_partitioned
WHERE created_at BETWEEN '2024-06-01' AND '2024-06-30';
-- Execution Time: ~200ms (10배 개선, 1개 파티션만 스캔)
```

---

## 9. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL 트랜잭션](./postgresql-transactions.md)
- [PostgreSQL 고급 기능](./postgresql-advanced-features.md)

---

## 10. 참고 자료

- [PostgreSQL Performance Optimization](https://www.postgresql.org/docs/current/performance-tips.html)
- [PostgreSQL Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [PostgreSQL Vacuum](https://www.postgresql.org/docs/current/routine-vacuuming.html)
- [Use The Index, Luke](https://use-the-index-luke.com/)
