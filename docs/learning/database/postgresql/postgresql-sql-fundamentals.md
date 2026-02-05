# PostgreSQL SQL 기초

## 학습 목표
- PostgreSQL DDL/DML/DCL 문법 이해
- MySQL과의 문법 차이점 파악
- JOIN, Subquery, Aggregation 활용
- Portal Universe Shopping Service 스키마 작성

---

## 1. DDL (Data Definition Language)

### 1.1 데이터베이스 생성

```sql
-- 데이터베이스 생성
CREATE DATABASE shopping_db
    ENCODING 'UTF8'
    LC_COLLATE 'en_US.UTF-8'
    LC_CTYPE 'en_US.UTF-8';

-- 데이터베이스 삭제
DROP DATABASE IF EXISTS shopping_db;

-- 데이터베이스 목록
\l
SELECT datname FROM pg_database;
```

### 1.2 테이블 생성

#### PostgreSQL vs MySQL 문법 차이

| 항목 | PostgreSQL | MySQL |
|------|-----------|-------|
| **자동 증가** | `SERIAL`, `BIGSERIAL` | `AUTO_INCREMENT` |
| **Boolean** | `BOOLEAN` | `TINYINT(1)` |
| **Text** | `TEXT` (무제한) | `TEXT` (65,535) |
| **JSON** | `JSONB` | `JSON` |
| **Array** | `TEXT[]` | 미지원 |
| **UUID** | `UUID` | `CHAR(36)` |
| **현재 시각** | `CURRENT_TIMESTAMP` | `CURRENT_TIMESTAMP` |

#### PostgreSQL 테이블 생성 예시

```sql
-- products 테이블
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    metadata JSONB,
    tags TEXT[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- orders 테이블
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    shipping_address JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    payment_method VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- order_items 테이블
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    CONSTRAINT fk_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT
);
```

### 1.3 MySQL 문법과의 차이

#### AUTO_INCREMENT → SERIAL

**MySQL:**
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

**PostgreSQL:**
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,  -- BIGINT + SEQUENCE 자동 생성
    name VARCHAR(255) NOT NULL
);
```

#### ON UPDATE CURRENT_TIMESTAMP 대안

**MySQL:**
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**PostgreSQL (트리거 필요):**
```sql
-- 트리거 함수 생성
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 적용
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 1.4 테이블 수정 (ALTER)

```sql
-- 컬럼 추가
ALTER TABLE products
ADD COLUMN category VARCHAR(50);

-- 컬럼 타입 변경
ALTER TABLE products
ALTER COLUMN price TYPE NUMERIC(15,2);

-- 컬럼 이름 변경
ALTER TABLE products
RENAME COLUMN description TO product_description;

-- 컬럼 삭제
ALTER TABLE products
DROP COLUMN category;

-- 제약 조건 추가
ALTER TABLE products
ADD CONSTRAINT check_price_positive CHECK (price >= 0);

-- 제약 조건 삭제
ALTER TABLE products
DROP CONSTRAINT check_price_positive;
```

### 1.5 테이블 삭제

```sql
-- 테이블 삭제 (외래 키 있으면 실패)
DROP TABLE products;

-- 외래 키 무시하고 삭제
DROP TABLE products CASCADE;

-- 테이블 존재 시에만 삭제
DROP TABLE IF EXISTS products;

-- 데이터만 삭제 (구조 유지)
TRUNCATE TABLE products;

-- CASCADE로 참조 테이블도 비우기
TRUNCATE TABLE orders CASCADE;
```

---

## 2. DML (Data Manipulation Language)

### 2.1 INSERT

```sql
-- 단일 행 삽입
INSERT INTO products (name, price, stock, tags)
VALUES ('MacBook Pro', 2500000, 10, ARRAY['laptop', 'apple', 'bestseller']);

-- 다중 행 삽입
INSERT INTO products (name, price, stock, metadata)
VALUES
    ('iPhone 15', 1200000, 50, '{"color": "black", "storage": "256GB"}'),
    ('AirPods Pro', 350000, 100, '{"color": "white", "noise_cancel": true}'),
    ('iPad Air', 850000, 30, '{"color": "silver", "storage": "128GB"}');

-- RETURNING 절 (PostgreSQL 고유 기능)
INSERT INTO products (name, price, stock)
VALUES ('Galaxy S24', 1100000, 40)
RETURNING id, name, created_at;

-- 서브쿼리로 삽입
INSERT INTO products_backup
SELECT * FROM products WHERE is_active = false;
```

### 2.2 SELECT

```sql
-- 기본 조회
SELECT * FROM products;

-- 특정 컬럼 조회
SELECT id, name, price FROM products;

-- WHERE 절
SELECT * FROM products
WHERE price > 1000000 AND is_active = true;

-- JSONB 조회
SELECT name, metadata->>'color' AS color
FROM products
WHERE metadata->>'storage' = '256GB';

-- Array 조회
SELECT name FROM products
WHERE 'bestseller' = ANY(tags);

-- LIKE 검색
SELECT * FROM products
WHERE name LIKE '%MacBook%';

-- ILIKE (대소문자 무시)
SELECT * FROM products
WHERE name ILIKE '%macbook%';

-- IN 절
SELECT * FROM products
WHERE id IN (1, 2, 3);

-- BETWEEN
SELECT * FROM products
WHERE price BETWEEN 500000 AND 1500000;

-- ORDER BY
SELECT * FROM products
ORDER BY price DESC, created_at ASC;

-- LIMIT & OFFSET
SELECT * FROM products
ORDER BY created_at DESC
LIMIT 10 OFFSET 20;
```

### 2.3 UPDATE

```sql
-- 단일 행 수정
UPDATE products
SET price = 2400000, stock = 15
WHERE id = 1;

-- 다중 행 수정
UPDATE products
SET is_active = false
WHERE stock = 0;

-- JSONB 수정
UPDATE products
SET metadata = metadata || '{"on_sale": true}'
WHERE price > 2000000;

-- Array 추가
UPDATE products
SET tags = array_append(tags, 'premium')
WHERE price > 2000000;

-- RETURNING 절
UPDATE products
SET stock = stock - 1
WHERE id = 1
RETURNING id, name, stock;

-- FROM 절 (다른 테이블 참조)
UPDATE products p
SET is_active = false
FROM order_items oi
WHERE p.id = oi.product_id
  AND oi.quantity = 0;
```

### 2.4 DELETE

```sql
-- 조건부 삭제
DELETE FROM products
WHERE is_active = false;

-- 모든 행 삭제 (주의!)
DELETE FROM products;

-- RETURNING 절
DELETE FROM products
WHERE id = 1
RETURNING *;

-- USING 절 (다른 테이블 참조)
DELETE FROM order_items oi
USING orders o
WHERE oi.order_id = o.id
  AND o.status = 'CANCELLED';
```

---

## 3. JOIN

### 3.1 INNER JOIN

```sql
-- 주문과 주문 아이템 조인
SELECT
    o.order_number,
    o.total_amount,
    oi.quantity,
    p.name AS product_name,
    oi.unit_price
FROM orders o
INNER JOIN order_items oi ON o.id = oi.order_id
INNER JOIN products p ON oi.product_id = p.id;
```

### 3.2 LEFT JOIN

```sql
-- 모든 상품과 주문 아이템 (주문 없는 상품도 포함)
SELECT
    p.name,
    p.stock,
    COALESCE(SUM(oi.quantity), 0) AS total_sold
FROM products p
LEFT JOIN order_items oi ON p.id = oi.product_id
GROUP BY p.id, p.name, p.stock;
```

### 3.3 RIGHT JOIN

```sql
-- 모든 주문과 주문 아이템
SELECT
    o.order_number,
    oi.quantity,
    p.name
FROM order_items oi
RIGHT JOIN orders o ON oi.order_id = o.id
LEFT JOIN products p ON oi.product_id = p.id;
```

### 3.4 FULL OUTER JOIN

```sql
-- 모든 상품과 모든 주문 아이템
SELECT
    p.name AS product_name,
    oi.quantity
FROM products p
FULL OUTER JOIN order_items oi ON p.id = oi.product_id;
```

---

## 4. Aggregation (집계)

### 4.1 기본 집계 함수

```sql
-- COUNT
SELECT COUNT(*) FROM products;
SELECT COUNT(DISTINCT category) FROM products;

-- SUM
SELECT SUM(total_amount) FROM orders;

-- AVG
SELECT AVG(price) FROM products;

-- MIN/MAX
SELECT MIN(price), MAX(price) FROM products;

-- 여러 집계 동시
SELECT
    COUNT(*) AS total_products,
    AVG(price) AS avg_price,
    MIN(price) AS min_price,
    MAX(price) AS max_price,
    SUM(stock) AS total_stock
FROM products;
```

### 4.2 GROUP BY

```sql
-- 상태별 주문 수
SELECT
    status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_revenue
FROM orders
GROUP BY status
ORDER BY total_revenue DESC;

-- 월별 매출
SELECT
    DATE_TRUNC('month', created_at) AS month,
    COUNT(*) AS order_count,
    SUM(total_amount) AS revenue
FROM orders
WHERE status = 'COMPLETED'
GROUP BY DATE_TRUNC('month', created_at)
ORDER BY month DESC;

-- HAVING 절
SELECT
    status,
    COUNT(*) AS order_count
FROM orders
GROUP BY status
HAVING COUNT(*) > 10;
```

---

## 5. Subquery (서브쿼리)

### 5.1 WHERE 절 서브쿼리

```sql
-- 평균 가격보다 비싼 상품
SELECT name, price
FROM products
WHERE price > (SELECT AVG(price) FROM products);

-- IN 서브쿼리
SELECT name
FROM products
WHERE id IN (
    SELECT DISTINCT product_id
    FROM order_items
);

-- EXISTS
SELECT name
FROM products p
WHERE EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.product_id = p.id
);
```

### 5.2 FROM 절 서브쿼리

```sql
-- 상품별 판매량
SELECT
    p.name,
    s.total_sold
FROM products p
INNER JOIN (
    SELECT
        product_id,
        SUM(quantity) AS total_sold
    FROM order_items
    GROUP BY product_id
) s ON p.id = s.product_id
ORDER BY s.total_sold DESC;
```

### 5.3 SELECT 절 서브쿼리

```sql
-- 상품과 판매량
SELECT
    name,
    price,
    (
        SELECT COALESCE(SUM(quantity), 0)
        FROM order_items
        WHERE product_id = products.id
    ) AS total_sold
FROM products;
```

---

## 6. DCL (Data Control Language)

### 6.1 사용자(Role) 생성

```sql
-- 사용자 생성
CREATE USER shopping_user WITH PASSWORD 'secure_password';

-- Role 생성
CREATE ROLE readonly;
CREATE ROLE readwrite;
```

### 6.2 권한 부여 (GRANT)

```sql
-- 데이터베이스 연결 권한
GRANT CONNECT ON DATABASE shopping_db TO shopping_user;

-- 스키마 사용 권한
GRANT USAGE ON SCHEMA public TO shopping_user;

-- 테이블 SELECT 권한
GRANT SELECT ON products TO shopping_user;

-- 모든 테이블 SELECT 권한
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly;

-- INSERT, UPDATE, DELETE 권한
GRANT INSERT, UPDATE, DELETE ON products TO readwrite;

-- 시퀀스 사용 권한
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO readwrite;

-- Role에 사용자 추가
GRANT readonly TO shopping_user;
```

### 6.3 권한 회수 (REVOKE)

```sql
-- SELECT 권한 회수
REVOKE SELECT ON products FROM shopping_user;

-- 모든 권한 회수
REVOKE ALL PRIVILEGES ON products FROM shopping_user;
```

---

## 7. Portal Universe Shopping Service 스키마

### 7.1 전체 스키마

```sql
-- products 테이블
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    category VARCHAR(50),
    metadata JSONB,  -- {"color": "black", "size": ["S","M"], "features": {...}}
    tags TEXT[] DEFAULT '{}',  -- ['sale', 'new', 'bestseller']
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- orders 테이블
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    shipping_address JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    payment_method VARCHAR(20),
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- order_items 테이블
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    CONSTRAINT fk_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE RESTRICT
);

-- updated_at 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- products 트리거
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- orders 트리거
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 인덱스
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_tags ON products USING gin(tags);
CREATE INDEX idx_products_metadata ON products USING gin(metadata);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
```

### 7.2 샘플 데이터

```sql
-- 상품 샘플
INSERT INTO products (name, description, price, stock, category, metadata, tags)
VALUES
    ('MacBook Pro 16"', 'Apple M3 Pro Chip', 2500000, 10, 'laptop',
     '{"color": "Space Gray", "memory": "16GB", "storage": "512GB"}',
     ARRAY['apple', 'laptop', 'bestseller']),
    ('iPhone 15 Pro', 'Titanium Design', 1350000, 50, 'smartphone',
     '{"color": "Natural Titanium", "storage": "256GB"}',
     ARRAY['apple', 'smartphone', 'new']),
    ('AirPods Pro 2', 'Active Noise Cancellation', 350000, 100, 'audio',
     '{"color": "White", "noise_cancel": true}',
     ARRAY['apple', 'audio', 'sale']);

-- 주문 샘플
INSERT INTO orders (order_number, user_id, shipping_address, status, total_amount)
VALUES
    ('ORD-2024-0001', 'user123', '{"name": "홍길동", "address": "서울시 강남구", "phone": "010-1234-5678"}', 'COMPLETED', 2850000),
    ('ORD-2024-0002', 'user456', '{"name": "김철수", "address": "서울시 서초구", "phone": "010-9876-5432"}', 'PENDING', 1350000);

-- 주문 아이템 샘플
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
VALUES
    (1, 1, 1, 2500000, 2500000),
    (1, 3, 1, 350000, 350000),
    (2, 2, 1, 1350000, 1350000);
```

---

## 8. 핵심 요약

- [ ] PostgreSQL은 `SERIAL`/`BIGSERIAL`로 자동 증가 (`AUTO_INCREMENT` 대체)
- [ ] `BOOLEAN`, `TEXT`, `JSONB`, `TEXT[]` 등 다양한 데이터 타입 지원
- [ ] `RETURNING` 절로 INSERT/UPDATE/DELETE 결과 즉시 반환
- [ ] `ILIKE`로 대소문자 무시 검색
- [ ] `ON UPDATE` 트리거 필요 (`updated_at` 자동 갱신)
- [ ] `LIMIT` & `OFFSET`으로 페이지네이션
- [ ] `GRANT`/`REVOKE`로 세밀한 권한 관리

---

## 관련 문서

- 이전: [PostgreSQL 소개](./postgresql-introduction.md)
- 다음: [MySQL vs PostgreSQL 비교](./mysql-vs-postgresql.md)
- 심화: [PostgreSQL 데이터 타입](./postgresql-data-types.md)

---

## 참고 자료

- [PostgreSQL SQL Syntax](https://www.postgresql.org/docs/current/sql.html)
- [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html)
- [PostgreSQL vs MySQL Syntax](https://wiki.postgresql.org/wiki/Things_to_find_out_about_when_moving_from_MySQL_to_PostgreSQL)
