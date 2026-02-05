# PostgreSQL 인덱싱

**난이도:** ⭐⭐⭐

## 학습 목표
- PostgreSQL의 다양한 인덱스 타입 이해 (B-Tree, Hash, GIN, GiST)
- 인덱스 설계 전략 및 최적화 방법 습득
- EXPLAIN ANALYZE를 통한 쿼리 성능 분석
- Portal Universe Shopping Service의 효과적인 인덱스 전략 수립

---

## 1. PostgreSQL 인덱스 개요

### 1.1 인덱스란?

인덱스는 **데이터 검색 속도를 향상**시키기 위한 데이터 구조입니다. 책의 색인과 같이 특정 값을 빠르게 찾을 수 있도록 도와줍니다.

### 1.2 인덱스 타입 비교

| 인덱스 타입 | 사용 사례 | 연산자 | 성능 특성 |
|------------|----------|--------|----------|
| **B-Tree** | 일반 검색, 범위 쿼리 | `=, <, >, <=, >=, BETWEEN` | 가장 범용적, 정렬 지원 |
| **Hash** | 정확한 일치 검색 | `=` | 동등 비교만 지원, 메모리 효율적 |
| **GIN** | 배열, JSONB, 전문 검색 | `@>, ?`, tsquery | 다중 값 검색 최적화 |
| **GiST** | 공간 데이터, 범위 타입 | `&&, @>`, geometry | 복잡한 데이터 타입 |
| **BRIN** | 대용량 시계열 데이터 | 범위 검색 | 작은 인덱스 크기 |
| **SP-GiST** | 비균형 데이터 구조 | 전화번호, IP 주소 | 특수 목적 |

### 1.3 MySQL vs PostgreSQL 인덱스

| 특성 | MySQL (InnoDB) | PostgreSQL |
|------|----------------|------------|
| 기본 인덱스 | B+Tree | B-Tree |
| 전문 검색 | Full-Text Index | GIN + tsvector |
| JSON 인덱스 | JSON 경로 인덱스 | GIN (JSONB) |
| 부분 인덱스 | ❌ | ✅ |
| 표현식 인덱스 | ❌ (Generated Column) | ✅ |
| Covering Index | ✅ (자동) | ✅ (INCLUDE) |

---

## 2. B-Tree 인덱스

### 2.1 기본 사용법

PostgreSQL의 **기본 인덱스 타입**입니다.

```sql
-- 단일 컬럼 인덱스
CREATE INDEX idx_products_name ON products(name);

-- 복합 인덱스 (Composite Index)
CREATE INDEX idx_products_category_price
ON products(category, price DESC);

-- 고유 인덱스
CREATE UNIQUE INDEX idx_users_email ON users(email);
```

### 2.2 복합 인덱스 설계 원칙

**왼쪽 우선 규칙 (Left-prefix Rule)**

```sql
CREATE INDEX idx_orders_user_status_date
ON orders(user_id, status, created_at);

-- ✅ 인덱스 사용 가능
SELECT * FROM orders WHERE user_id = 123;
SELECT * FROM orders WHERE user_id = 123 AND status = 'PENDING';
SELECT * FROM orders WHERE user_id = 123 AND status = 'PENDING'
  AND created_at > '2024-01-01';

-- ❌ 인덱스 사용 불가
SELECT * FROM orders WHERE status = 'PENDING';
SELECT * FROM orders WHERE created_at > '2024-01-01';
```

**카디널리티 기준 정렬**

```sql
-- 높은 카디널리티 → 낮은 카디널리티 순서
CREATE INDEX idx_orders_optimal
ON orders(
    user_id,      -- 카디널리티 높음 (수천~수만)
    status,       -- 카디널리티 낮음 (5~10개)
    created_at    -- 카디널리티 높음 (시간별)
);
```

### 2.3 정렬 방향 최적화

```sql
-- 최신 주문 조회 쿼리
SELECT * FROM orders
WHERE user_id = 123
ORDER BY created_at DESC
LIMIT 10;

-- 최적화된 인덱스 (정렬 방향 일치)
CREATE INDEX idx_orders_user_created_desc
ON orders(user_id, created_at DESC);
```

---

## 3. 특수 인덱스 타입

### 3.1 Hash 인덱스

```sql
-- Hash 인덱스 생성
CREATE INDEX idx_sessions_token_hash
ON sessions USING HASH(session_token);

-- 동등 비교만 지원
SELECT * FROM sessions WHERE session_token = 'abc123';  -- ✅

-- 범위 검색 불가
SELECT * FROM sessions WHERE session_token > 'abc';     -- ❌ 인덱스 미사용
```

**사용 시나리오:**
- 긴 문자열의 정확한 일치 검색
- UUID, 해시값, 토큰 검색
- 범위 검색이 필요 없는 경우

### 3.2 GIN 인덱스 (Generalized Inverted Index)

**배열 컬럼 검색**

```sql
-- 제품 태그 배열
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    tags TEXT[]
);

-- GIN 인덱스 생성
CREATE INDEX idx_products_tags_gin ON products USING GIN(tags);

-- 배열 연산자 사용
SELECT * FROM products WHERE tags @> ARRAY['organic'];          -- 포함
SELECT * FROM products WHERE tags && ARRAY['vegan', 'gluten-free']; -- 교집합
SELECT * FROM products WHERE 'organic' = ANY(tags);              -- 요소 존재
```

**JSONB 검색**

```sql
-- 제품 메타데이터
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    metadata JSONB
);

-- JSONB GIN 인덱스
CREATE INDEX idx_products_metadata_gin ON products USING GIN(metadata);

-- JSONB 연산자 사용
SELECT * FROM products WHERE metadata @> '{"brand": "Samsung"}';
SELECT * FROM products WHERE metadata ? 'warranty';
SELECT * FROM products WHERE metadata -> 'specs' @> '{"ram": "16GB"}';
```

**전문 검색 (Full-Text Search)**

```sql
-- 전문 검색 컬럼 추가
ALTER TABLE products ADD COLUMN search_vector tsvector;

-- 검색 벡터 업데이트
UPDATE products SET search_vector =
    to_tsvector('english', name || ' ' || COALESCE(description, ''));

-- GIN 인덱스 생성
CREATE INDEX idx_products_search_gin
ON products USING GIN(search_vector);

-- 전문 검색 쿼리
SELECT * FROM products
WHERE search_vector @@ to_tsquery('english', 'laptop & wireless');
```

### 3.3 GiST 인덱스 (Generalized Search Tree)

```sql
-- 지리 공간 데이터 (PostGIS 확장 필요)
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE stores (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    location GEOMETRY(POINT, 4326)
);

-- GiST 인덱스 생성
CREATE INDEX idx_stores_location_gist
ON stores USING GIST(location);

-- 반경 검색
SELECT * FROM stores
WHERE ST_DWithin(
    location,
    ST_MakePoint(127.0276, 37.4979)::geography,
    5000  -- 5km
);
```

---

## 4. 고급 인덱스 기법

### 4.1 부분 인덱스 (Partial Index)

특정 조건을 만족하는 행만 인덱싱하여 **인덱스 크기 감소** 및 **성능 향상**.

```sql
-- 활성 제품만 인덱싱
CREATE INDEX idx_products_active_name
ON products(name)
WHERE is_deleted = FALSE AND status = 'ACTIVE';

-- 최근 주문만 인덱싱
CREATE INDEX idx_orders_recent
ON orders(user_id, created_at)
WHERE created_at > '2024-01-01';

-- NULL이 아닌 값만 인덱싱
CREATE INDEX idx_products_discount_price
ON products(price)
WHERE discount_percentage IS NOT NULL;
```

**효과:**

```sql
-- Before: 1,000,000 rows 인덱스
CREATE INDEX idx_products_name ON products(name);

-- After: 800,000 rows 인덱스 (20% 감소)
CREATE INDEX idx_products_active_name
ON products(name)
WHERE is_deleted = FALSE;
```

### 4.2 표현식 인덱스 (Expression Index)

```sql
-- 대소문자 무시 검색
CREATE INDEX idx_users_email_lower
ON users(LOWER(email));

SELECT * FROM users WHERE LOWER(email) = 'user@example.com';

-- 계산된 값 인덱싱
CREATE INDEX idx_products_final_price
ON products((price * (1 - discount_percentage / 100)));

SELECT * FROM products
WHERE (price * (1 - discount_percentage / 100)) < 100;

-- 날짜 추출
CREATE INDEX idx_orders_year_month
ON orders(EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at));

SELECT * FROM orders
WHERE EXTRACT(YEAR FROM created_at) = 2024
  AND EXTRACT(MONTH FROM created_at) = 6;
```

### 4.3 Covering Index (INCLUDE)

쿼리에 필요한 모든 컬럼을 인덱스에 포함하여 **테이블 접근 없이** 데이터 반환.

```sql
-- 기본 인덱스 (Index Scan + Table Lookup)
CREATE INDEX idx_orders_user ON orders(user_id);

SELECT user_id, total_amount, created_at
FROM orders WHERE user_id = 123;
-- → Index Scan + Heap Fetch (테이블 접근 필요)

-- Covering Index (Index-Only Scan)
CREATE INDEX idx_orders_user_covering
ON orders(user_id)
INCLUDE (total_amount, created_at);

SELECT user_id, total_amount, created_at
FROM orders WHERE user_id = 123;
-- → Index-Only Scan (테이블 접근 불필요)
```

---

## 5. EXPLAIN ANALYZE

### 5.1 실행 계획 분석

```sql
EXPLAIN ANALYZE
SELECT p.name, p.price, c.name AS category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.price > 1000 AND p.is_deleted = FALSE
ORDER BY p.created_at DESC
LIMIT 10;
```

**출력 예시:**

```
Limit  (cost=0.42..123.45 rows=10 width=64) (actual time=1.234..2.567 rows=10 loops=1)
  ->  Nested Loop  (cost=0.42..12345.67 rows=1000 width=64) (actual time=1.232..2.564 rows=10 loops=1)
        ->  Index Scan Backward using idx_products_created_desc on products p
            (cost=0.29..8901.23 rows=1000 width=48) (actual time=0.123..1.456 rows=15 loops=1)
              Filter: (price > 1000::numeric AND NOT is_deleted)
              Rows Removed by Filter: 5
        ->  Index Scan using categories_pkey on categories c
            (cost=0.13..3.15 rows=1 width=24) (actual time=0.067..0.068 rows=1 loops=15)
              Index Cond: (id = p.category_id)
Planning Time: 0.567 ms
Execution Time: 2.789 ms
```

### 5.2 주요 메트릭 해석

| 메트릭 | 설명 | 좋은 값 |
|--------|------|---------|
| **cost** | 추정 비용 | 낮을수록 좋음 |
| **rows** | 추정 행 수 | 실제와 유사할수록 좋음 |
| **actual time** | 실제 실행 시간 (ms) | 낮을수록 좋음 |
| **loops** | 반복 횟수 | 1에 가까울수록 좋음 |

### 5.3 스캔 타입

| 스캔 타입 | 설명 | 성능 |
|----------|------|------|
| **Seq Scan** | 전체 테이블 스캔 | ⚠️ 느림 (작은 테이블 제외) |
| **Index Scan** | 인덱스 + 테이블 접근 | ✅ 빠름 |
| **Index-Only Scan** | 인덱스만 사용 | ✅✅ 매우 빠름 |
| **Bitmap Index Scan** | 여러 인덱스 병합 | ✅ 중간 |

### 5.4 성능 문제 식별

```sql
-- ❌ 나쁜 예: Seq Scan
EXPLAIN ANALYZE SELECT * FROM products WHERE name = 'iPhone 15';
/*
Seq Scan on products  (cost=0.00..1234.56 rows=1 width=128)
  Filter: (name = 'iPhone 15')
  Rows Removed by Filter: 99999
*/

-- ✅ 개선: 인덱스 생성
CREATE INDEX idx_products_name ON products(name);

EXPLAIN ANALYZE SELECT * FROM products WHERE name = 'iPhone 15';
/*
Index Scan using idx_products_name on products  (cost=0.29..8.31 rows=1 width=128)
  Index Cond: (name = 'iPhone 15')
*/
```

---

## 6. Portal Universe Shopping Service 인덱스 전략

### 6.1 Products 테이블

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    tags TEXT[],
    metadata JSONB,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 전략

-- 1. 카테고리별 가격 범위 검색 (복합 인덱스)
CREATE INDEX idx_products_category_price
ON products(category_id, price DESC)
WHERE is_deleted = FALSE;

-- 2. 브랜드별 최신 제품 조회
CREATE INDEX idx_products_brand_created
ON products(brand_id, created_at DESC)
WHERE is_deleted = FALSE;

-- 3. 전문 검색 (GIN)
CREATE INDEX idx_products_name_gin
ON products USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- 4. 태그 검색 (GIN)
CREATE INDEX idx_products_tags_gin
ON products USING GIN(tags)
WHERE is_deleted = FALSE;

-- 5. JSONB 메타데이터 검색 (GIN)
CREATE INDEX idx_products_metadata_gin
ON products USING GIN(metadata)
WHERE metadata IS NOT NULL;

-- 6. 재고 부족 알림 (부분 인덱스)
CREATE INDEX idx_products_low_stock
ON products(stock_quantity)
WHERE stock_quantity < 10 AND is_deleted = FALSE;

-- 7. Covering Index (자주 조회되는 컬럼)
CREATE INDEX idx_products_category_covering
ON products(category_id)
INCLUDE (name, price, stock_quantity, created_at)
WHERE is_deleted = FALSE;
```

### 6.2 Orders 테이블

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    shipping_address JSONB NOT NULL,
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 전략

-- 1. 사용자별 주문 목록 (최신순)
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);

-- 2. 주문 상태별 조회 (관리자용)
CREATE INDEX idx_orders_status_created
ON orders(status, created_at DESC);

-- 3. 주문 번호 고속 검색 (Hash)
CREATE INDEX idx_orders_number_hash
ON orders USING HASH(order_number);

-- 4. 처리 대기 주문 (부분 인덱스)
CREATE INDEX idx_orders_pending
ON orders(created_at)
WHERE status IN ('PENDING', 'PAID');

-- 5. 고액 주문 분석 (부분 인덱스)
CREATE INDEX idx_orders_high_value
ON orders(user_id, total_amount DESC, created_at DESC)
WHERE total_amount > 100000;

-- 6. 배송 주소 검색 (GIN)
CREATE INDEX idx_orders_shipping_address_gin
ON orders USING GIN(shipping_address);

-- 7. 월별 매출 집계 (표현식 인덱스)
CREATE INDEX idx_orders_year_month
ON orders(
    EXTRACT(YEAR FROM created_at),
    EXTRACT(MONTH FROM created_at)
);
```

### 6.3 Order Items 테이블

```sql
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 전략

-- 1. 주문별 상품 조회
CREATE INDEX idx_order_items_order
ON order_items(order_id);

-- 2. 상품별 판매 분석
CREATE INDEX idx_order_items_product_created
ON order_items(product_id, created_at DESC);

-- 3. Covering Index (주문 상세 조회)
CREATE INDEX idx_order_items_order_covering
ON order_items(order_id)
INCLUDE (product_id, quantity, unit_price);
```

### 6.4 실제 쿼리 최적화 예시

**시나리오 1: 카테고리별 가격 범위 검색**

```sql
-- 쿼리
SELECT p.id, p.name, p.price, b.name AS brand_name
FROM products p
JOIN brands b ON p.brand_id = b.id
WHERE p.category_id = 5
  AND p.price BETWEEN 50000 AND 100000
  AND p.is_deleted = FALSE
ORDER BY p.price DESC
LIMIT 20;

-- 사용된 인덱스
-- idx_products_category_price (category_id, price DESC) WHERE is_deleted = FALSE

EXPLAIN ANALYZE 결과:
/*
Limit  (cost=0.42..50.12 rows=20 width=64) (actual time=0.234..1.567 rows=20 loops=1)
  ->  Nested Loop  (cost=0.42..1234.56 rows=500 width=64) (actual time=0.232..1.564 rows=20 loops=1)
        ->  Index Scan using idx_products_category_price on products p
              (cost=0.29..890.12 rows=500 width=48) (actual time=0.123..0.987 rows=25 loops=1)
              Index Cond: (category_id = 5 AND price >= 50000 AND price <= 100000)
              Filter: NOT is_deleted
        ->  Index Scan using brands_pkey on brands b
              (cost=0.13..0.68 rows=1 width=24) (actual time=0.012..0.013 rows=1 loops=25)
              Index Cond: (id = p.brand_id)
Execution Time: 1.789 ms
*/
```

**시나리오 2: 사용자 주문 내역 조회**

```sql
-- 쿼리
SELECT
    o.id,
    o.order_number,
    o.total_amount,
    o.status,
    o.created_at,
    COUNT(oi.id) AS item_count
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.user_id = 12345
GROUP BY o.id
ORDER BY o.created_at DESC
LIMIT 10;

-- 사용된 인덱스
-- idx_orders_user_created (user_id, created_at DESC)
-- idx_order_items_order (order_id)

EXPLAIN ANALYZE 결과:
/*
Limit  (cost=45.67..123.45 rows=10 width=80) (actual time=0.456..1.234 rows=10 loops=1)
  ->  GroupAggregate  (cost=45.67..890.12 rows=100 width=80) (actual time=0.454..1.231 rows=10 loops=1)
        ->  Nested Loop Left Join  (cost=0.56..850.34 rows=500 width=72)
              ->  Index Scan using idx_orders_user_created on orders o
                    (cost=0.29..123.45 rows=100 width=64) (actual time=0.067..0.234 rows=15 loops=1)
                    Index Cond: (user_id = 12345)
              ->  Index Scan using idx_order_items_order on order_items oi
                    (cost=0.27..7.15 rows=5 width=8) (actual time=0.034..0.056 rows=3 loops=15)
                    Index Cond: (order_id = o.id)
Execution Time: 1.456 ms
*/
```

---

## 7. 인덱스 관리 및 모니터링

### 7.1 인덱스 조회

```sql
-- 테이블의 모든 인덱스 조회
SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'products'
ORDER BY indexname;

-- 인덱스 크기 조회
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE tablename = 'products'
ORDER BY pg_relation_size(indexrelid) DESC;

-- 사용되지 않는 인덱스 찾기
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0 AND indexrelid IS NOT NULL
ORDER BY pg_relation_size(indexrelid) DESC;
```

### 7.2 인덱스 재구성

```sql
-- 인덱스 재생성 (블로트 제거)
REINDEX INDEX idx_products_category_price;

-- 테이블의 모든 인덱스 재생성
REINDEX TABLE products;

-- 동시 인덱스 재생성 (락 최소화)
CREATE INDEX CONCURRENTLY idx_products_name_new ON products(name);
DROP INDEX CONCURRENTLY idx_products_name;
ALTER INDEX idx_products_name_new RENAME TO idx_products_name;
```

### 7.3 인덱스 삭제

```sql
-- 인덱스 삭제
DROP INDEX idx_products_old;

-- 동시 삭제 (락 최소화)
DROP INDEX CONCURRENTLY idx_products_old;
```

---

## 8. 인덱스 설계 Best Practices

### 8.1 인덱스 생성 기준

| 상황 | 인덱스 생성 | 이유 |
|------|------------|------|
| WHERE 절에 자주 사용 | ✅ | 검색 성능 향상 |
| JOIN 조건 컬럼 | ✅ | 조인 성능 향상 |
| ORDER BY 컬럼 | ✅ | 정렬 성능 향상 |
| GROUP BY 컬럼 | ✅ | 집계 성능 향상 |
| 카디널리티 낮음 (< 5%) | ❌ | 인덱스 효율 낮음 |
| 자주 업데이트되는 컬럼 | ⚠️ | 쓰기 성능 저하 고려 |
| 작은 테이블 (< 1000 rows) | ❌ | Seq Scan이 더 빠름 |

### 8.2 인덱스 개수 제한

```
권장 인덱스 개수:
- 읽기 중심 테이블: 5~10개
- 쓰기 중심 테이블: 2~5개
- 초대용량 테이블: 필수 인덱스만 (3개 이하)
```

**이유:**
- 각 INSERT/UPDATE/DELETE마다 모든 인덱스 갱신 필요
- 인덱스가 많을수록 쓰기 성능 저하

### 8.3 복합 인덱스 vs 단일 인덱스

```sql
-- ❌ 나쁜 예: 단일 인덱스 남발
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);

-- ✅ 좋은 예: 복합 인덱스
CREATE INDEX idx_products_category_price
ON products(category_id, price DESC);
-- 두 쿼리 모두 지원:
-- WHERE category_id = 5
-- WHERE category_id = 5 AND price > 1000
```

---

## 9. 핵심 정리

| 개념 | 설명 | 사용 시기 |
|------|------|----------|
| **B-Tree** | 범용 인덱스 | 일반 검색, 정렬 |
| **Hash** | 동등 비교 전용 | UUID, 토큰 검색 |
| **GIN** | 다중 값 인덱스 | 배열, JSONB, 전문 검색 |
| **GiST** | 공간 인덱스 | 지리 데이터, 범위 타입 |
| **Partial Index** | 조건부 인덱스 | 활성 데이터만 인덱싱 |
| **Expression Index** | 함수 인덱스 | LOWER(), 계산 결과 |
| **Covering Index** | 포함 인덱스 | 테이블 접근 제거 |

---

## 10. 실습 예제

### 실습 1: 인덱스 성능 비교

```sql
-- 테스트 데이터 생성
CREATE TABLE test_products AS
SELECT
    id,
    'Product ' || id AS name,
    (random() * 1000)::INTEGER AS price,
    (random() * 10)::INTEGER + 1 AS category_id
FROM generate_series(1, 1000000) AS id;

-- 인덱스 없이 검색
EXPLAIN ANALYZE
SELECT * FROM test_products WHERE category_id = 5 AND price > 500;
-- Execution Time: ~300ms (Seq Scan)

-- 인덱스 생성
CREATE INDEX idx_test_category_price
ON test_products(category_id, price);

-- 인덱스로 검색
EXPLAIN ANALYZE
SELECT * FROM test_products WHERE category_id = 5 AND price > 500;
-- Execution Time: ~10ms (Index Scan)
```

### 실습 2: 부분 인덱스 효과

```sql
-- 전체 인덱스
CREATE INDEX idx_products_all ON products(status);
-- 크기: 50MB

-- 부분 인덱스 (활성 제품만)
CREATE INDEX idx_products_active
ON products(status)
WHERE is_deleted = FALSE;
-- 크기: 40MB (20% 감소)

-- 쿼리 속도 비교
EXPLAIN ANALYZE
SELECT * FROM products WHERE status = 'ACTIVE' AND is_deleted = FALSE;
-- 부분 인덱스가 더 빠름 (인덱스 크기 작음)
```

---

## 11. 관련 문서

- [PostgreSQL 트랜잭션](./postgresql-transactions.md)
- [PostgreSQL 성능 튜닝](./postgresql-performance-tuning.md)
- [PostgreSQL JSONB](./postgresql-jsonb.md)
- [PostgreSQL 고급 기능](./postgresql-advanced-features.md)

---

## 12. 참고 자료

- [PostgreSQL Index Documentation](https://www.postgresql.org/docs/current/indexes.html)
- [Use The Index, Luke](https://use-the-index-luke.com/)
- [PostgreSQL Index Types](https://www.postgresql.org/docs/current/indexes-types.html)
- [EXPLAIN Documentation](https://www.postgresql.org/docs/current/using-explain.html)
