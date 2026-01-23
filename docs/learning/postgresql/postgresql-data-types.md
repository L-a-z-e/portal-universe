# PostgreSQL 데이터 타입

## 학습 목표
- PostgreSQL 고유 데이터 타입 이해 (JSONB, Array, UUID)
- MySQL과의 타입 차이점 파악
- Portal Universe Shopping Service 적용 전략
- 타입별 인덱싱 및 쿼리 최적화

---

## 1. 기본 데이터 타입

### 1.1 숫자 타입

| 타입 | 크기 | 범위 | 사용 사례 |
|------|------|------|----------|
| **SMALLINT** | 2 bytes | -32,768 ~ 32,767 | 작은 정수 |
| **INTEGER** | 4 bytes | -2,147,483,648 ~ 2,147,483,647 | 일반 정수 |
| **BIGINT** | 8 bytes | -9,223,372,036,854,775,808 ~ 9,223,372,036,854,775,807 | 큰 정수 |
| **SERIAL** | 4 bytes | 1 ~ 2,147,483,647 | 자동 증가 |
| **BIGSERIAL** | 8 bytes | 1 ~ 9,223,372,036,854,775,807 | 큰 자동 증가 |
| **NUMERIC(p,s)** | 가변 | 임의 정밀도 | 금액, 정확한 소수 |
| **DECIMAL(p,s)** | 가변 | NUMERIC과 동일 | 금액 |
| **REAL** | 4 bytes | 6자리 정밀도 | 부동소수점 |
| **DOUBLE PRECISION** | 8 bytes | 15자리 정밀도 | 부동소수점 |

**예시:**

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    stock INTEGER NOT NULL DEFAULT 0,
    price NUMERIC(12,2) NOT NULL,  -- 금액 (정확한 소수)
    weight REAL                    -- 무게 (근사값)
);
```

### 1.2 문자열 타입

| 타입 | 크기 | 설명 |
|------|------|------|
| **VARCHAR(n)** | 가변 | 최대 n 문자 |
| **CHAR(n)** | 고정 | 정확히 n 문자 (공백 채움) |
| **TEXT** | 무제한 | 무제한 길이 (권장) |

**MySQL과의 차이:**
- MySQL `TEXT`: 최대 65,535 bytes
- PostgreSQL `TEXT`: 무제한 (1GB까지)

```sql
CREATE TABLE products (
    name VARCHAR(255) NOT NULL,
    description TEXT,           -- 무제한 (권장)
    sku CHAR(10) UNIQUE        -- 고정 길이
);
```

### 1.3 날짜/시간 타입

| 타입 | 크기 | 범위 | 설명 |
|------|------|------|------|
| **DATE** | 4 bytes | 4713 BC ~ 5874897 AD | 날짜 |
| **TIME** | 8 bytes | 00:00:00 ~ 24:00:00 | 시간 |
| **TIMESTAMP** | 8 bytes | 4713 BC ~ 294276 AD | 날짜+시간 (timezone 없음) |
| **TIMESTAMPTZ** | 8 bytes | 4713 BC ~ 294276 AD | 날짜+시간 (timezone 포함, 권장) |
| **INTERVAL** | 16 bytes | -178,000,000 years ~ 178,000,000 years | 시간 간격 |

```sql
CREATE TABLE orders (
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMPTZ,  -- timezone 포함 (권장)
    processing_time INTERVAL   -- '2 days 3 hours'
);

-- 날짜 연산
SELECT NOW() + INTERVAL '7 days';
SELECT '2024-01-15'::DATE - '2024-01-01'::DATE;  -- 14 (일)
```

### 1.4 Boolean 타입

```sql
CREATE TABLE products (
    is_active BOOLEAN DEFAULT true,
    is_featured BOOLEAN
);

-- 조회
SELECT * FROM products WHERE is_active;  -- true
SELECT * FROM products WHERE NOT is_featured;  -- false

-- 값
-- true: TRUE, 't', 'true', 'y', 'yes', 'on', '1'
-- false: FALSE, 'f', 'false', 'n', 'no', 'off', '0'
```

---

## 2. UUID 타입

### 2.1 UUID란?

**UUID (Universally Unique Identifier)**: 128-bit 고유 식별자

```
550e8400-e29b-41d4-a716-446655440000
└──────┘ └──┘ └──┘ └──┘ └──────────┘
  32비트   16  16   16      48비트
```

### 2.2 MySQL vs PostgreSQL

| 항목 | MySQL | PostgreSQL |
|------|-------|-----------|
| **타입** | `CHAR(36)` | `UUID` (16 bytes) |
| **크기** | 36 bytes | 16 bytes |
| **성능** | 느림 (문자열 비교) | 빠름 (숫자 비교) |
| **생성** | `UUID()` 함수 | Extension 필요 |

### 2.3 PostgreSQL UUID 사용

```sql
-- Extension 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- UUID 타입 사용
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INSERT
INSERT INTO orders (user_id, order_number)
VALUES (uuid_generate_v4(), 'ORD-2024-0001');

-- 조회
SELECT * FROM orders WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

### 2.4 UUID vs BIGSERIAL

| 항목 | BIGSERIAL | UUID |
|------|-----------|------|
| **크기** | 8 bytes | 16 bytes |
| **예측 가능** | ✅ 순차 | ❌ 무작위 |
| **분산 생성** | ❌ 중앙 관리 | ✅ 가능 |
| **노출 위험** | ⚠️ 높음 (순차) | ✅ 낮음 |
| **인덱스 성능** | 우수 (순차) | 보통 (무작위) |
| **사용 사례** | 내부 시스템 | API 노출, 분산 시스템 |

**Portal Universe 권장:**
- **내부 PK**: `BIGSERIAL` (성능 우수)
- **외부 노출 ID**: UUID 추가 컬럼
  ```sql
  CREATE TABLE orders (
      id BIGSERIAL PRIMARY KEY,         -- 내부
      public_id UUID UNIQUE DEFAULT uuid_generate_v4(),  -- 외부 노출
      order_number VARCHAR(30) NOT NULL UNIQUE
  );
  ```

---

## 3. JSONB 타입

### 3.1 JSON vs JSONB

| 항목 | JSON | JSONB |
|------|------|-------|
| **저장 형식** | Text (원본 유지) | Binary (파싱 후 저장) |
| **쓰기 속도** | 빠름 | 느림 (파싱 오버헤드) |
| **읽기 속도** | 느림 (매번 파싱) | 빠름 (Binary) |
| **인덱싱** | ❌ | ✅ GIN 인덱스 |
| **중복 키** | 유지 | 제거 (마지막 값) |
| **공백/순서** | 유지 | 제거/정렬 |
| **연산자** | 기본 | 풍부 |
| **권장** | ❌ | ✅ **대부분 JSONB 사용** |

### 3.2 JSONB 생성

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    metadata JSONB  -- 유연한 메타데이터
);

-- INSERT
INSERT INTO products (name, price, metadata)
VALUES (
    'MacBook Pro 16"',
    2500000,
    '{
        "color": "Space Gray",
        "memory": "16GB",
        "storage": "512GB",
        "processor": "M3 Pro",
        "ports": ["USB-C", "HDMI", "SD Card"],
        "warranty": {
            "period": 12,
            "type": "manufacturer"
        }
    }'
);
```

### 3.3 JSONB 연산자

| 연산자 | 설명 | 반환 타입 | 예시 |
|-------|------|----------|------|
| `->` | 필드 추출 (JSON) | JSONB | `metadata->'color'` → `"Space Gray"` |
| `->>` | 필드 추출 (TEXT) | TEXT | `metadata->>'color'` → `Space Gray` |
| `#>` | 경로 추출 (JSON) | JSONB | `metadata#>'{warranty,period}'` → `12` |
| `#>>` | 경로 추출 (TEXT) | TEXT | `metadata#>>'{warranty,period}'` → `12` |
| `@>` | 포함 여부 | BOOLEAN | `metadata @> '{"color":"Space Gray"}'` |
| `<@` | 포함됨 여부 | BOOLEAN | `'{"color":"Space Gray"}' <@ metadata` |
| `?` | 키 존재 여부 | BOOLEAN | `metadata ? 'warranty'` |
| `?|` | 키 중 하나라도 존재 | BOOLEAN | `metadata ?| array['color','size']` |
| `?&` | 모든 키 존재 | BOOLEAN | `metadata ?& array['color','memory']` |
| `||` | 병합 | JSONB | `metadata || '{"on_sale":true}'` |
| `-` | 키 삭제 | JSONB | `metadata - 'warranty'` |
| `#-` | 경로 삭제 | JSONB | `metadata #- '{warranty,period}'` |

### 3.4 JSONB 쿼리 예시

```sql
-- 1. 단일 필드 조회 (TEXT)
SELECT name, metadata->>'color' AS color
FROM products;

-- 2. 중첩 필드 조회
SELECT name, metadata#>>'{warranty,period}' AS warranty_months
FROM products;

-- 3. WHERE 조건 (특정 값)
SELECT * FROM products
WHERE metadata->>'color' = 'Space Gray';

-- 4. 포함 여부 (@>)
SELECT * FROM products
WHERE metadata @> '{"color": "Space Gray", "memory": "16GB"}';

-- 5. 키 존재 여부
SELECT * FROM products
WHERE metadata ? 'warranty';

-- 6. 배열 요소 확인
SELECT * FROM products
WHERE metadata->'ports' @> '"USB-C"';

-- 7. JSONB 업데이트 (병합)
UPDATE products
SET metadata = metadata || '{"on_sale": true, "discount": 10}'
WHERE price > 2000000;

-- 8. 키 삭제
UPDATE products
SET metadata = metadata - 'on_sale'
WHERE id = 1;

-- 9. 중첩 객체 수정
UPDATE products
SET metadata = jsonb_set(metadata, '{warranty,period}', '24')
WHERE id = 1;

-- 10. 배열 추가
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{ports}',
    (metadata->'ports') || '"Thunderbolt"'::jsonb
)
WHERE id = 1;
```

### 3.5 JSONB 인덱싱

```sql
-- 1. 전체 JSONB 컬럼 인덱스 (GIN)
CREATE INDEX idx_products_metadata ON products USING gin(metadata);

-- 사용 사례: 포함 여부, 키 존재 여부
SELECT * FROM products WHERE metadata @> '{"color": "black"}';
SELECT * FROM products WHERE metadata ? 'warranty';

-- 2. 특정 필드 인덱스
CREATE INDEX idx_products_color ON products ((metadata->>'color'));

-- 사용 사례: 특정 필드 조건
SELECT * FROM products WHERE metadata->>'color' = 'Space Gray';

-- 3. 표현식 인덱스
CREATE INDEX idx_products_price_range ON products ((metadata->>'storage'));

-- 4. Partial 인덱스
CREATE INDEX idx_products_on_sale ON products USING gin(metadata)
WHERE metadata->>'on_sale' = 'true';
```

### 3.6 Portal Universe 활용 예시

```sql
-- products 테이블 (상품 메타데이터)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    stock INTEGER NOT NULL,
    metadata JSONB,  -- 다양한 속성
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 샘플 데이터
INSERT INTO products (name, price, stock, metadata) VALUES
('MacBook Pro 16"', 2500000, 10, '{
    "color": "Space Gray",
    "memory": "16GB",
    "storage": "512GB",
    "processor": "M3 Pro",
    "features": {
        "retina_display": true,
        "touch_id": true,
        "webcam": "1080p"
    }
}'),
('iPhone 15 Pro', 1350000, 50, '{
    "color": "Natural Titanium",
    "storage": "256GB",
    "features": {
        "action_button": true,
        "dynamic_island": true,
        "usb_c": true
    }
}');

-- 인덱스
CREATE INDEX idx_products_metadata ON products USING gin(metadata);
CREATE INDEX idx_products_color ON products ((metadata->>'color'));

-- 쿼리 예시
-- 1. Space Gray 제품
SELECT name, price FROM products
WHERE metadata->>'color' = 'Space Gray';

-- 2. 512GB 이상 제품
SELECT name, metadata->>'storage' AS storage
FROM products
WHERE (metadata->>'storage')::TEXT >= '512GB';

-- 3. Retina Display 지원 제품
SELECT name FROM products
WHERE metadata->'features'->>'retina_display' = 'true';

-- 4. 특정 기능 포함 제품
SELECT name FROM products
WHERE metadata->'features' ? 'action_button';
```

---

## 4. Array 타입

### 4.1 Array 선언

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tags TEXT[],           -- 문자열 배열
    prices INTEGER[],      -- 정수 배열
    dimensions REAL[][]    -- 2차원 배열
);
```

### 4.2 Array 삽입

```sql
-- ARRAY 키워드
INSERT INTO products (name, tags)
VALUES ('iPhone 15', ARRAY['smartphone', 'apple', 'new']);

-- 리터럴 표기
INSERT INTO products (name, tags)
VALUES ('MacBook Pro', '{"laptop", "apple", "bestseller"}');

-- 빈 배열
INSERT INTO products (name, tags)
VALUES ('iPad', '{}');

-- NULL
INSERT INTO products (name, tags)
VALUES ('AirPods', NULL);
```

### 4.3 Array 조회

```sql
-- 1. 전체 배열
SELECT name, tags FROM products;

-- 2. 특정 요소 (1-based index)
SELECT name, tags[1] AS first_tag FROM products;

-- 3. 배열 슬라이싱
SELECT name, tags[1:2] AS first_two_tags FROM products;

-- 4. 배열 길이
SELECT name, array_length(tags, 1) AS tag_count FROM products;

-- 5. 배열 → 행으로 변환 (UNNEST)
SELECT name, unnest(tags) AS tag
FROM products;

-- 6. 배열 포함 여부 (ANY)
SELECT name FROM products
WHERE 'apple' = ANY(tags);

-- 7. 배열 포함 여부 (@>)
SELECT name FROM products
WHERE tags @> ARRAY['apple', 'new'];

-- 8. 교집합 (&&)
SELECT name FROM products
WHERE tags && ARRAY['sale', 'bestseller'];

-- 9. 배열 정렬
SELECT name, array_sort(tags) FROM products;

-- 10. 배열 집계
SELECT array_agg(name) AS product_names
FROM products
WHERE tags @> ARRAY['apple'];
```

### 4.4 Array 수정

```sql
-- 1. 요소 추가 (array_append)
UPDATE products
SET tags = array_append(tags, 'premium')
WHERE price > 2000000;

-- 2. 요소 앞에 추가 (array_prepend)
UPDATE products
SET tags = array_prepend('featured', tags)
WHERE id = 1;

-- 3. 배열 병합 (||)
UPDATE products
SET tags = tags || ARRAY['sale', 'limited']
WHERE id = 1;

-- 4. 요소 제거 (array_remove)
UPDATE products
SET tags = array_remove(tags, 'old')
WHERE 'old' = ANY(tags);

-- 5. 특정 인덱스 교체
UPDATE products
SET tags[1] = 'smartphone'
WHERE id = 1;

-- 6. 배열 교체
UPDATE products
SET tags = ARRAY['new', 'arrival']
WHERE id = 1;
```

### 4.5 Array 인덱싱

```sql
-- GIN 인덱스 (권장)
CREATE INDEX idx_products_tags ON products USING gin(tags);

-- 사용 사례
SELECT * FROM products WHERE tags @> ARRAY['apple'];
SELECT * FROM products WHERE tags && ARRAY['sale'];
SELECT * FROM products WHERE 'bestseller' = ANY(tags);
```

### 4.6 Portal Universe 활용 예시

```sql
-- products 테이블
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    tags TEXT[] DEFAULT '{}',      -- 태그 배열
    category_path TEXT[],          -- ['Electronics', 'Computers', 'Laptops']
    image_urls TEXT[]              -- 이미지 URL 배열
);

-- 샘플 데이터
INSERT INTO products (name, price, tags, category_path, image_urls) VALUES
('MacBook Pro 16"', 2500000,
 ARRAY['laptop', 'apple', 'bestseller', 'premium'],
 ARRAY['Electronics', 'Computers', 'Laptops'],
 ARRAY['https://cdn.com/mbp1.jpg', 'https://cdn.com/mbp2.jpg']),
('iPhone 15 Pro', 1350000,
 ARRAY['smartphone', 'apple', 'new', '5g'],
 ARRAY['Electronics', 'Mobile', 'Smartphones'],
 ARRAY['https://cdn.com/iphone1.jpg']);

-- 인덱스
CREATE INDEX idx_products_tags ON products USING gin(tags);
CREATE INDEX idx_products_category_path ON products USING gin(category_path);

-- 쿼리 예시
-- 1. Apple 제품
SELECT name, tags FROM products
WHERE 'apple' = ANY(tags);

-- 2. Sale 또는 Bestseller
SELECT name FROM products
WHERE tags && ARRAY['sale', 'bestseller'];

-- 3. Premium이면서 Apple
SELECT name FROM products
WHERE tags @> ARRAY['premium', 'apple'];

-- 4. 카테고리 경로 검색
SELECT name FROM products
WHERE category_path @> ARRAY['Electronics', 'Computers'];

-- 5. 태그 개수
SELECT name, array_length(tags, 1) AS tag_count
FROM products
ORDER BY tag_count DESC;

-- 6. 태그별 제품 수
SELECT unnest(tags) AS tag, COUNT(*) AS product_count
FROM products
GROUP BY tag
ORDER BY product_count DESC;
```

---

## 5. ENUM 타입

### 5.1 ENUM 생성

```sql
-- ENUM 타입 정의
CREATE TYPE order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- 테이블에 사용
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    status order_status NOT NULL DEFAULT 'PENDING'
);
```

### 5.2 ENUM 사용

```sql
-- INSERT
INSERT INTO orders (order_number, status)
VALUES ('ORD-2024-0001', 'CONFIRMED');

-- 조회
SELECT * FROM orders WHERE status = 'PENDING';

-- 순서 비교
SELECT * FROM orders WHERE status < 'SHIPPED';
-- PENDING < CONFIRMED < PROCESSING < SHIPPED
```

### 5.3 ENUM vs VARCHAR

| 항목 | ENUM | VARCHAR |
|------|------|---------|
| **크기** | 4 bytes | 가변 |
| **성능** | 우수 (정수 비교) | 보통 (문자열 비교) |
| **유효성 검증** | 자동 | CHECK 제약 필요 |
| **수정** | 복잡 (ALTER TYPE) | 간단 |
| **유연성** | 낮음 | 높음 |

**Portal Universe 권장:**
- **고정된 상태**: ENUM (order_status)
- **자주 변경되는 값**: VARCHAR + CHECK 제약

---

## 6. 기타 유용한 타입

### 6.1 INET (IP 주소)

```sql
CREATE TABLE access_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    ip_address INET,  -- IPv4, IPv6
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INSERT
INSERT INTO access_logs (user_id, ip_address)
VALUES (123, '192.168.1.100');

-- 조회
SELECT * FROM access_logs
WHERE ip_address << '192.168.1.0/24';  -- 서브넷
```

### 6.2 CIDR (네트워크)

```sql
CREATE TABLE ip_blocks (
    id BIGSERIAL PRIMARY KEY,
    network CIDR
);

INSERT INTO ip_blocks (network)
VALUES ('192.168.0.0/24');
```

### 6.3 MONEY

```sql
-- 권장하지 않음 (NUMERIC 사용 권장)
CREATE TABLE products (
    price MONEY  -- $1,234.56
);
```

---

## 7. 타입 캐스팅

```sql
-- :: 연산자 (PostgreSQL)
SELECT '123'::INTEGER;
SELECT '2024-01-15'::DATE;
SELECT '[1,2,3]'::JSONB;

-- CAST 함수 (표준 SQL)
SELECT CAST('123' AS INTEGER);
SELECT CAST('2024-01-15' AS DATE);
```

---

## 8. 핵심 요약

- [ ] PostgreSQL은 **풍부한 데이터 타입** 지원 (JSONB, Array, UUID 등)
- [ ] **JSONB**는 JSON보다 우수 (Binary, 인덱싱)
- [ ] **Array** 타입으로 1:Many 관계 간소화
- [ ] **UUID**는 외부 노출 ID에 적합 (내부 PK는 BIGSERIAL)
- [ ] **NUMERIC**은 금액 저장에 적합 (정확한 소수)
- [ ] **TIMESTAMPTZ** 권장 (timezone 포함)
- [ ] **GIN 인덱스**로 JSONB, Array 최적화

---

## 관련 문서

- 이전: [MySQL vs PostgreSQL](./mysql-vs-postgresql.md)
- 다음: [PostgreSQL 인덱싱](./postgresql-indexing.md)
- 심화: [PostgreSQL JSONB](./postgresql-jsonb.md)

---

## 참고 자료

- [PostgreSQL Data Types](https://www.postgresql.org/docs/current/datatype.html)
- [JSONB Functions](https://www.postgresql.org/docs/current/functions-json.html)
- [Array Functions](https://www.postgresql.org/docs/current/functions-array.html)
