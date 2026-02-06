# PostgreSQL 고급 기능

**난이도:** ⭐⭐⭐⭐

## 학습 목표
- CTE (Common Table Expressions) 및 Recursive CTE 마스터
- Window Functions를 활용한 복잡한 집계 및 순위 계산
- Full-Text Search (tsvector, tsquery, GIN) 구현
- Materialized View를 통한 성능 최적화
- Portal Universe에서의 고급 쿼리 시나리오 구현

---

## 1. CTE (Common Table Expressions)

### 1.1 CTE 기본

CTE는 **임시 결과 집합**을 정의하여 쿼리를 더 읽기 쉽고 유지보수하기 쉽게 만듭니다.

```sql
-- 기본 CTE 구문
WITH cte_name AS (
    SELECT ...
    FROM ...
    WHERE ...
)
SELECT * FROM cte_name;
```

**예시: 카테고리별 평균 가격**

```sql
-- CTE 없이
SELECT
    c.name AS category_name,
    AVG(p.price) AS avg_price
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE p.is_deleted = FALSE
GROUP BY c.name
HAVING AVG(p.price) > (
    SELECT AVG(price) FROM products WHERE is_deleted = FALSE
);

-- CTE 사용
WITH avg_prices AS (
    SELECT
        category_id,
        AVG(price) AS avg_price
    FROM products
    WHERE is_deleted = FALSE
    GROUP BY category_id
),
overall_avg AS (
    SELECT AVG(price) AS overall_avg
    FROM products
    WHERE is_deleted = FALSE
)
SELECT
    c.name AS category_name,
    ap.avg_price,
    oa.overall_avg
FROM avg_prices ap
JOIN categories c ON ap.category_id = c.id
CROSS JOIN overall_avg oa
WHERE ap.avg_price > oa.overall_avg;
```

### 1.2 다중 CTE

```sql
-- Portal Universe: 월별 매출 분석
WITH monthly_orders AS (
    SELECT
        DATE_TRUNC('month', created_at) AS month,
        COUNT(*) AS order_count,
        SUM(total_amount) AS revenue
    FROM orders
    WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED')
    GROUP BY DATE_TRUNC('month', created_at)
),
monthly_growth AS (
    SELECT
        month,
        order_count,
        revenue,
        LAG(revenue) OVER (ORDER BY month) AS prev_revenue,
        revenue - LAG(revenue) OVER (ORDER BY month) AS revenue_growth
    FROM monthly_orders
)
SELECT
    month,
    order_count,
    revenue,
    prev_revenue,
    revenue_growth,
    CASE
        WHEN prev_revenue IS NULL THEN NULL
        ELSE ROUND((revenue_growth / prev_revenue * 100)::NUMERIC, 2)
    END AS growth_percentage
FROM monthly_growth
ORDER BY month DESC;
```

### 1.3 Recursive CTE

**재귀 CTE**는 **계층적 데이터**를 처리할 때 사용합니다.

**구문:**

```sql
WITH RECURSIVE cte_name AS (
    -- 초기 쿼리 (Base case)
    SELECT ...
    FROM ...
    WHERE ...

    UNION ALL

    -- 재귀 쿼리 (Recursive case)
    SELECT ...
    FROM cte_name
    WHERE ...  -- 종료 조건
)
SELECT * FROM cte_name;
```

**예시 1: 카테고리 트리**

```sql
-- 카테고리 테이블 (계층 구조)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    parent_id INTEGER REFERENCES categories(id)
);

INSERT INTO categories (id, name, parent_id) VALUES
(1, 'Electronics', NULL),
(2, 'Computers', 1),
(3, 'Laptops', 2),
(4, 'Desktops', 2),
(5, 'Mobile', 1),
(6, 'Smartphones', 5),
(7, 'Tablets', 5);

-- 특정 카테고리의 모든 하위 카테고리 조회
WITH RECURSIVE category_tree AS (
    -- 초기: 루트 카테고리
    SELECT
        id,
        name,
        parent_id,
        1 AS level,
        name::TEXT AS path
    FROM categories
    WHERE id = 1  -- Electronics

    UNION ALL

    -- 재귀: 하위 카테고리
    SELECT
        c.id,
        c.name,
        c.parent_id,
        ct.level + 1,
        ct.path || ' > ' || c.name
    FROM categories c
    JOIN category_tree ct ON c.parent_id = ct.id
)
SELECT
    id,
    REPEAT('  ', level - 1) || name AS category_name,
    level,
    path
FROM category_tree
ORDER BY path;

-- 결과:
-- | id | category_name      | level | path                        |
-- |----|-------------------|-------|----------------------------|
-- | 1  | Electronics        | 1     | Electronics                 |
-- | 2  |   Computers        | 2     | Electronics > Computers     |
-- | 3  |     Laptops        | 3     | Electronics > Computers > Laptops |
-- | 4  |     Desktops       | 3     | Electronics > Computers > Desktops |
-- | 5  |   Mobile           | 2     | Electronics > Mobile        |
-- | 6  |     Smartphones    | 3     | Electronics > Mobile > Smartphones |
-- | 7  |     Tablets        | 3     | Electronics > Mobile > Tablets |
```

**예시 2: 조직도**

```sql
-- 조직 테이블
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    manager_id INTEGER REFERENCES employees(id),
    position VARCHAR(100)
);

-- CEO의 모든 부하 직원 조회
WITH RECURSIVE org_chart AS (
    -- CEO
    SELECT
        id,
        name,
        manager_id,
        position,
        1 AS level
    FROM employees
    WHERE manager_id IS NULL

    UNION ALL

    -- 부하 직원
    SELECT
        e.id,
        e.name,
        e.manager_id,
        e.position,
        oc.level + 1
    FROM employees e
    JOIN org_chart oc ON e.manager_id = oc.id
)
SELECT
    REPEAT('  ', level - 1) || name AS employee,
    position,
    level
FROM org_chart
ORDER BY level, name;
```

**예시 3: 숫자 시퀀스 생성**

```sql
-- 1부터 10까지 생성
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM numbers
    WHERE n < 10
)
SELECT * FROM numbers;

-- 날짜 범위 생성
WITH RECURSIVE date_series AS (
    SELECT '2024-01-01'::DATE AS date
    UNION ALL
    SELECT date + INTERVAL '1 day'
    FROM date_series
    WHERE date < '2024-01-31'::DATE
)
SELECT date FROM date_series;
```

---

## 2. Window Functions

### 2.1 Window Functions 개요

Window Functions는 **현재 행과 관련된 행들의 집합**에 대해 계산을 수행합니다.

**구문:**

```sql
SELECT
    column,
    window_function() OVER (
        [PARTITION BY partition_expression]
        [ORDER BY sort_expression]
        [frame_clause]
    )
FROM table;
```

### 2.2 순위 함수

| 함수 | 설명 | 동점 처리 |
|------|------|----------|
| `ROW_NUMBER()` | 고유 순번 (1, 2, 3, ...) | 연속 |
| `RANK()` | 순위 (1, 2, 2, 4, ...) | 건너뜀 |
| `DENSE_RANK()` | 밀집 순위 (1, 2, 2, 3, ...) | 건너뛰지 않음 |
| `NTILE(n)` | n개 그룹으로 분할 | - |

**예시: 제품 가격 순위**

```sql
SELECT
    name,
    category_id,
    price,
    ROW_NUMBER() OVER (PARTITION BY category_id ORDER BY price DESC) AS row_num,
    RANK() OVER (PARTITION BY category_id ORDER BY price DESC) AS rank,
    DENSE_RANK() OVER (PARTITION BY category_id ORDER BY price DESC) AS dense_rank
FROM products
WHERE is_deleted = FALSE;

-- 결과:
-- | name      | category_id | price  | row_num | rank | dense_rank |
-- |-----------|-------------|--------|---------|------|------------|
-- | Product A | 1           | 150000 | 1       | 1    | 1          |
-- | Product B | 1           | 150000 | 2       | 1    | 1          |
-- | Product C | 1           | 120000 | 3       | 3    | 2          |
-- | Product D | 2           | 200000 | 1       | 1    | 1          |
```

**Portal Universe: 카테고리별 Top 3 제품**

```sql
WITH ranked_products AS (
    SELECT
        p.id,
        p.name,
        p.price,
        c.name AS category_name,
        RANK() OVER (PARTITION BY p.category_id ORDER BY p.price DESC) AS price_rank
    FROM products p
    JOIN categories c ON p.category_id = c.id
    WHERE p.is_deleted = FALSE
)
SELECT
    category_name,
    name,
    price,
    price_rank
FROM ranked_products
WHERE price_rank <= 3
ORDER BY category_name, price_rank;
```

### 2.3 집계 함수 (Window 버전)

```sql
SELECT
    order_id,
    created_at,
    total_amount,
    -- 누적 합계
    SUM(total_amount) OVER (ORDER BY created_at) AS cumulative_revenue,
    -- 이동 평균 (최근 7일)
    AVG(total_amount) OVER (
        ORDER BY created_at
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS moving_avg_7days,
    -- 카테고리별 평균과 비교
    AVG(total_amount) OVER (PARTITION BY user_id) AS user_avg_order
FROM orders
WHERE status = 'PAID'
ORDER BY created_at;
```

### 2.4 LAG / LEAD 함수

**이전/다음 행 값 참조**

```sql
-- Portal Universe: 월별 매출 증감
SELECT
    DATE_TRUNC('month', created_at) AS month,
    SUM(total_amount) AS revenue,
    LAG(SUM(total_amount)) OVER (ORDER BY DATE_TRUNC('month', created_at)) AS prev_revenue,
    SUM(total_amount) - LAG(SUM(total_amount)) OVER (ORDER BY DATE_TRUNC('month', created_at)) AS revenue_change,
    LEAD(SUM(total_amount)) OVER (ORDER BY DATE_TRUNC('month', created_at)) AS next_revenue
FROM orders
WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED')
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month;
```

### 2.5 FIRST_VALUE / LAST_VALUE

```sql
-- 각 사용자의 첫 주문과 마지막 주문
SELECT
    user_id,
    order_id,
    created_at,
    total_amount,
    FIRST_VALUE(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY created_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS first_order_amount,
    LAST_VALUE(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY created_at
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS last_order_amount
FROM orders
ORDER BY user_id, created_at;
```

### 2.6 Frame Clause

| Frame Type | 설명 |
|------------|------|
| `ROWS BETWEEN` | 물리적 행 기준 |
| `RANGE BETWEEN` | 논리적 값 기준 |

**범위 지정:**

| 범위 | 설명 |
|------|------|
| `UNBOUNDED PRECEDING` | 파티션 시작 |
| `n PRECEDING` | 현재 행에서 n행 이전 |
| `CURRENT ROW` | 현재 행 |
| `n FOLLOWING` | 현재 행에서 n행 이후 |
| `UNBOUNDED FOLLOWING` | 파티션 끝 |

```sql
-- 최근 3개 주문 평균 금액
SELECT
    order_id,
    created_at,
    total_amount,
    AVG(total_amount) OVER (
        ORDER BY created_at
        ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
    ) AS avg_last_3_orders
FROM orders
WHERE user_id = 123
ORDER BY created_at;
```

---

## 3. Full-Text Search (전문 검색)

### 3.1 tsvector & tsquery

**tsvector**: 검색 가능한 문서 표현
**tsquery**: 검색 쿼리

```sql
-- 기본 사용법
SELECT to_tsvector('english', 'The quick brown fox jumps over the lazy dog');
-- 결과: 'brown':3 'dog':9 'fox':4 'jump':5 'lazi':8 'quick':2

SELECT to_tsquery('english', 'quick & fox');
-- 결과: 'quick' & 'fox'

-- 검색
SELECT to_tsvector('english', 'The quick brown fox') @@
       to_tsquery('english', 'quick & fox');
-- 결과: true
```

### 3.2 제품 전문 검색 구현

**1단계: search_vector 컬럼 추가**

```sql
-- 컬럼 추가
ALTER TABLE products
ADD COLUMN search_vector tsvector;

-- 데이터 생성
UPDATE products
SET search_vector = to_tsvector('english',
    COALESCE(name, '') || ' ' ||
    COALESCE(description, '') || ' ' ||
    COALESCE(metadata ->> 'brand', '')
);

-- GIN 인덱스 생성
CREATE INDEX idx_products_search_vector
ON products USING GIN(search_vector);
```

**2단계: 트리거로 자동 업데이트**

```sql
CREATE OR REPLACE FUNCTION products_search_vector_update() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english',
        COALESCE(NEW.name, '') || ' ' ||
        COALESCE(NEW.description, '') || ' ' ||
        COALESCE(NEW.metadata ->> 'brand', '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_search_vector_trigger
BEFORE INSERT OR UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION products_search_vector_update();
```

**3단계: 검색 쿼리**

```sql
-- 단순 검색
SELECT
    id,
    name,
    price,
    ts_rank(search_vector, query) AS rank
FROM products,
     to_tsquery('english', 'laptop & wireless') AS query
WHERE search_vector @@ query
  AND is_deleted = FALSE
ORDER BY rank DESC
LIMIT 20;

-- 구문 검색 (phrase search)
SELECT * FROM products
WHERE search_vector @@ phraseto_tsquery('english', 'gaming laptop');

-- 접두사 검색
SELECT * FROM products
WHERE search_vector @@ to_tsquery('english', 'lapt:*');
-- lapt로 시작하는 단어 (laptop, laptops 등)
```

### 3.3 가중치 설정

```sql
-- 제목에 더 높은 가중치
UPDATE products
SET search_vector =
    setweight(to_tsvector('english', COALESCE(name, '')), 'A') ||
    setweight(to_tsvector('english', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('english', COALESCE(metadata ->> 'brand', '')), 'C');

-- 검색 (가중치 반영)
SELECT
    id,
    name,
    ts_rank('{0.1, 0.2, 0.4, 1.0}', search_vector, query) AS rank
FROM products,
     to_tsquery('english', 'laptop') AS query
WHERE search_vector @@ query
ORDER BY rank DESC;
-- 가중치: D=0.1, C=0.2, B=0.4, A=1.0
```

### 3.4 한글 전문 검색

PostgreSQL 기본 텍스트 검색은 영어에 최적화되어 있습니다. 한글은 **pg_trgm 확장**을 사용합니다.

```sql
-- pg_trgm 확장 설치
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 트라이그램 인덱스 생성
CREATE INDEX idx_products_name_trgm
ON products USING GIN(name gin_trgm_ops);

CREATE INDEX idx_products_description_trgm
ON products USING GIN(description gin_trgm_ops);

-- 유사도 검색
SELECT
    name,
    similarity(name, '아이폰') AS sim
FROM products
WHERE name % '아이폰'  -- % 연산자: 유사도 검색
ORDER BY sim DESC
LIMIT 10;

-- LIKE 검색 (인덱스 사용)
SELECT * FROM products
WHERE name ILIKE '%아이폰%';  -- GIN 인덱스 사용
```

---

## 4. Materialized View

### 4.1 Materialized View란?

**Materialized View**는 쿼리 결과를 **물리적으로 저장**하여 복잡한 집계 쿼리 성능을 향상시킵니다.

| 특성 | View | Materialized View |
|------|------|------------------|
| 저장 | 쿼리만 저장 | 결과 저장 |
| 성능 | 매번 재실행 | 빠름 (캐시) |
| 최신성 | 항상 최신 | 수동 갱신 필요 |
| 인덱스 | ❌ | ✅ |
| 사용 시기 | 간단한 조인 | 복잡한 집계 |

### 4.2 생성 및 사용

```sql
-- Portal Universe: 월별 매출 대시보드
CREATE MATERIALIZED VIEW mv_monthly_sales AS
SELECT
    DATE_TRUNC('month', o.created_at) AS month,
    COUNT(DISTINCT o.id) AS order_count,
    COUNT(DISTINCT o.user_id) AS unique_customers,
    SUM(o.total_amount) AS total_revenue,
    AVG(o.total_amount) AS avg_order_value,
    SUM(oi.quantity) AS total_items_sold
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
GROUP BY DATE_TRUNC('month', o.created_at);

-- 인덱스 생성
CREATE UNIQUE INDEX idx_mv_monthly_sales_month
ON mv_monthly_sales(month);

-- 조회 (매우 빠름)
SELECT * FROM mv_monthly_sales
ORDER BY month DESC;

-- 갱신
REFRESH MATERIALIZED VIEW mv_monthly_sales;

-- 동시성 유지 갱신 (UNIQUE INDEX 필요)
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_sales;
```

### 4.3 실시간 갱신 (트리거)

```sql
-- 트리거로 자동 갱신 (간단한 경우)
CREATE OR REPLACE FUNCTION refresh_monthly_sales()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_monthly_sales;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_refresh_monthly_sales
AFTER INSERT OR UPDATE OR DELETE ON orders
FOR EACH STATEMENT
EXECUTE FUNCTION refresh_monthly_sales();
```

### 4.4 Portal Universe 활용 예시

**제품 랭킹 대시보드**

```sql
CREATE MATERIALIZED VIEW mv_product_rankings AS
WITH sales_stats AS (
    SELECT
        p.id,
        p.name,
        p.price,
        c.name AS category_name,
        COUNT(oi.id) AS order_count,
        SUM(oi.quantity) AS total_sold,
        SUM(oi.quantity * oi.unit_price) AS total_revenue
    FROM products p
    JOIN categories c ON p.category_id = c.id
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id
    WHERE p.is_deleted = FALSE
      AND (o.status IN ('PAID', 'SHIPPED', 'DELIVERED') OR o.status IS NULL)
    GROUP BY p.id, p.name, p.price, c.name
)
SELECT
    id,
    name,
    price,
    category_name,
    order_count,
    total_sold,
    total_revenue,
    RANK() OVER (ORDER BY total_revenue DESC) AS revenue_rank,
    RANK() OVER (PARTITION BY category_name ORDER BY total_sold DESC) AS category_rank
FROM sales_stats;

CREATE INDEX idx_mv_product_rankings_revenue_rank
ON mv_product_rankings(revenue_rank);

CREATE INDEX idx_mv_product_rankings_category
ON mv_product_rankings(category_name, category_rank);

-- 조회
SELECT * FROM mv_product_rankings
WHERE revenue_rank <= 10
ORDER BY revenue_rank;
```

---

## 5. Portal Universe 고급 쿼리 시나리오

### 5.1 고객 세그먼테이션

```sql
-- RFM 분석 (Recency, Frequency, Monetary)
WITH customer_stats AS (
    SELECT
        user_id,
        MAX(created_at) AS last_order_date,
        COUNT(*) AS order_count,
        SUM(total_amount) AS total_spent
    FROM orders
    WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED')
    GROUP BY user_id
),
rfm_scores AS (
    SELECT
        user_id,
        EXTRACT(DAY FROM NOW() - last_order_date) AS recency_days,
        order_count AS frequency,
        total_spent AS monetary,
        NTILE(5) OVER (ORDER BY last_order_date DESC) AS recency_score,
        NTILE(5) OVER (ORDER BY order_count) AS frequency_score,
        NTILE(5) OVER (ORDER BY total_spent) AS monetary_score
    FROM customer_stats
)
SELECT
    user_id,
    recency_days,
    frequency,
    monetary,
    recency_score,
    frequency_score,
    monetary_score,
    CASE
        WHEN recency_score >= 4 AND frequency_score >= 4 AND monetary_score >= 4
            THEN 'VIP'
        WHEN recency_score >= 3 AND frequency_score >= 3
            THEN 'Loyal'
        WHEN recency_score >= 4 AND frequency_score <= 2
            THEN 'New'
        WHEN recency_score <= 2
            THEN 'At Risk'
        ELSE 'Regular'
    END AS customer_segment
FROM rfm_scores
ORDER BY monetary DESC;
```

### 5.2 장바구니 분석

```sql
-- 함께 구매되는 제품 조합
WITH product_pairs AS (
    SELECT
        oi1.product_id AS product_a,
        oi2.product_id AS product_b,
        COUNT(DISTINCT oi1.order_id) AS co_purchase_count
    FROM order_items oi1
    JOIN order_items oi2
        ON oi1.order_id = oi2.order_id
        AND oi1.product_id < oi2.product_id  -- 중복 제거
    GROUP BY oi1.product_id, oi2.product_id
)
SELECT
    p1.name AS product_a_name,
    p2.name AS product_b_name,
    pp.co_purchase_count,
    RANK() OVER (PARTITION BY pp.product_a ORDER BY pp.co_purchase_count DESC) AS recommendation_rank
FROM product_pairs pp
JOIN products p1 ON pp.product_a = p1.id
JOIN products p2 ON pp.product_b = p2.id
WHERE pp.co_purchase_count >= 5
ORDER BY pp.co_purchase_count DESC
LIMIT 50;
```

### 5.3 재고 회전율 분석

```sql
WITH inventory_metrics AS (
    SELECT
        p.id,
        p.name,
        p.stock_quantity,
        COALESCE(SUM(oi.quantity), 0) AS total_sold_30days
    FROM products p
    LEFT JOIN order_items oi ON p.id = oi.product_id
    LEFT JOIN orders o ON oi.order_id = o.id
        AND o.created_at >= NOW() - INTERVAL '30 days'
        AND o.status IN ('PAID', 'SHIPPED', 'DELIVERED')
    WHERE p.is_deleted = FALSE
    GROUP BY p.id, p.name, p.stock_quantity
)
SELECT
    id,
    name,
    stock_quantity,
    total_sold_30days,
    CASE
        WHEN stock_quantity = 0 THEN 0
        ELSE ROUND((total_sold_30days::NUMERIC / stock_quantity), 2)
    END AS turnover_ratio,
    CASE
        WHEN total_sold_30days = 0 THEN NULL
        ELSE ROUND((stock_quantity::NUMERIC / total_sold_30days * 30), 0)
    END AS days_of_inventory,
    CASE
        WHEN stock_quantity < total_sold_30days * 0.3 THEN 'Low Stock'
        WHEN stock_quantity > total_sold_30days * 3 THEN 'Overstocked'
        ELSE 'Normal'
    END AS stock_status
FROM inventory_metrics
ORDER BY turnover_ratio DESC;
```

---

## 6. 핵심 정리

| 기능 | 사용 시기 | 주요 함수 |
|------|----------|----------|
| **CTE** | 복잡한 쿼리 구조화 | `WITH ... AS` |
| **Recursive CTE** | 계층 데이터 | `WITH RECURSIVE` |
| **Window Functions** | 순위, 누적, 비교 | `ROW_NUMBER(), RANK(), LAG(), LEAD()` |
| **Full-Text Search** | 텍스트 검색 | `to_tsvector(), to_tsquery(), @@` |
| **Materialized View** | 복잡한 집계 캐싱 | `CREATE MATERIALIZED VIEW` |

---

## 7. 실습 예제

### 실습 1: Recursive CTE로 카테고리 트리 탐색

```sql
-- 샘플 데이터 생성
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    parent_id INTEGER REFERENCES categories(id)
);

INSERT INTO categories (name, parent_id) VALUES
('All', NULL),
('Electronics', 1),
('Fashion', 1),
('Computers', 2),
('Mobile', 2),
('Laptops', 4),
('Smartphones', 5);

-- 전체 트리 조회
WITH RECURSIVE tree AS (
    SELECT id, name, parent_id, 1 AS level, name::TEXT AS path
    FROM categories WHERE parent_id IS NULL
    UNION ALL
    SELECT c.id, c.name, c.parent_id, t.level + 1, t.path || ' > ' || c.name
    FROM categories c JOIN tree t ON c.parent_id = t.id
)
SELECT REPEAT('  ', level - 1) || name AS category, level, path
FROM tree ORDER BY path;
```

### 실습 2: Window Functions로 순위 계산

```sql
-- 카테고리별 가격 순위
SELECT
    name,
    category_id,
    price,
    RANK() OVER (PARTITION BY category_id ORDER BY price DESC) AS price_rank,
    PERCENT_RANK() OVER (PARTITION BY category_id ORDER BY price DESC) AS percentile
FROM products
WHERE is_deleted = FALSE;
```

---

## 8. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL 트랜잭션](./postgresql-transactions.md)
- [PostgreSQL 성능 튜닝](./postgresql-performance-tuning.md)

---

## 9. 참고 자료

- [PostgreSQL CTE Documentation](https://www.postgresql.org/docs/current/queries-with.html)
- [PostgreSQL Window Functions](https://www.postgresql.org/docs/current/functions-window.html)
- [PostgreSQL Full-Text Search](https://www.postgresql.org/docs/current/textsearch.html)
- [PostgreSQL Materialized Views](https://www.postgresql.org/docs/current/rules-materializedviews.html)
