# MySQL vs PostgreSQL 비교

## 학습 목표
- MySQL과 PostgreSQL의 핵심 차이점 이해
- 문법 및 기능 비교를 통한 마이그레이션 준비
- 프로젝트 특성에 맞는 DB 선택 기준 파악
- Portal Universe 서비스별 최적 DB 전략 수립

---

## 1. 핵심 비교표

### 1.1 전반적 특징

| 항목 | MySQL | PostgreSQL | 승자 |
|------|-------|-----------|------|
| **라이선스** | GPL / Commercial | PostgreSQL License (MIT 유사) | 🟦 PostgreSQL |
| **개발 시작** | 1995 | 1986 (30+ 년) | - |
| **철학** | 속도 & 사용 편의성 | 표준 준수 & 확장성 | - |
| **ACID** | ✅ InnoDB | ✅ 완전 지원 | 🟦 PostgreSQL |
| **MVCC** | ✅ InnoDB | ✅ 네이티브 | 🟦 PostgreSQL |
| **확장성** | Plugin 제한적 | Extension 풍부 | 🟦 PostgreSQL |
| **커뮤니티** | 대규모 | 대규모 (학술적) | 🟢 동점 |
| **인기도** | 1위 (점유율) | 4위 (급성장) | 🟢 MySQL |
| **학습 곡선** | 낮음 | 중간~높음 | 🟢 MySQL |

### 1.2 성능 비교

| 항목 | MySQL | PostgreSQL | 승자 |
|------|-------|-----------|------|
| **읽기 성능** | 매우 우수 (단순 쿼리) | 우수 | 🟢 MySQL |
| **쓰기 성능** | 우수 | 우수 | 🟢 동점 |
| **복잡한 쿼리** | 보통 | 매우 우수 | 🟦 PostgreSQL |
| **동시성** | 보통 (Lock 경쟁) | 매우 우수 (MVCC) | 🟦 PostgreSQL |
| **대용량 JOIN** | 보통 | 우수 | 🟦 PostgreSQL |
| **인덱스 성능** | 우수 (B-Tree) | 우수 (다양한 타입) | 🟦 PostgreSQL |
| **전문 검색** | 보통 | 우수 (내장) | 🟦 PostgreSQL |

### 1.3 기능 비교

| 기능 | MySQL | PostgreSQL | 승자 |
|------|-------|-----------|------|
| **JSONB** | ❌ (JSON만, text) | ✅ Binary, 인덱싱 | 🟦 PostgreSQL |
| **Array** | ❌ | ✅ 네이티브 지원 | 🟦 PostgreSQL |
| **UUID** | ❌ (CHAR로 저장) | ✅ 네이티브 | 🟦 PostgreSQL |
| **Window Functions** | ✅ 8.0+ | ✅ 완전 지원 | 🟢 동점 |
| **CTE (WITH)** | ✅ 8.0+ | ✅ + Recursive | 🟦 PostgreSQL |
| **Full-Text Search** | ✅ 제한적 | ✅ 강력함 | 🟦 PostgreSQL |
| **GIS** | ❌ (Extension 필요) | ✅ PostGIS | 🟦 PostgreSQL |
| **트리거** | ✅ 제한적 | ✅ 강력함 | 🟦 PostgreSQL |
| **뷰** | ✅ | ✅ + Materialized | 🟦 PostgreSQL |
| **Partitioning** | ✅ 제한적 | ✅ 선언적 파티셔닝 | 🟦 PostgreSQL |

### 1.4 운영 및 관리

| 항목 | MySQL | PostgreSQL | 승자 |
|------|-------|-----------|------|
| **설정 난이도** | 낮음 | 중간 | 🟢 MySQL |
| **백업/복구** | 간단 (mysqldump) | 복잡 (pg_dump, WAL) | 🟢 MySQL |
| **복제** | 간단 (Master-Slave) | 복잡 (Streaming) | 🟢 MySQL |
| **모니터링** | 우수 (도구 많음) | 우수 | 🟢 동점 |
| **클라우드 지원** | 매우 우수 | 우수 (급성장) | 🟢 MySQL |
| **Vacuum 필요** | ❌ | ✅ (관리 필요) | 🟢 MySQL |

---

## 2. 문법 차이점

### 2.1 자동 증가 (Auto Increment)

**MySQL:**
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);
```

**PostgreSQL:**
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,  -- BIGINT + SEQUENCE
    name VARCHAR(255)
);

-- 또는 명시적
CREATE SEQUENCE products_id_seq;
CREATE TABLE products (
    id BIGINT DEFAULT nextval('products_id_seq') PRIMARY KEY,
    name VARCHAR(255)
);
```

### 2.2 자동 타임스탬프

**MySQL:**
```sql
CREATE TABLE products (
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**PostgreSQL (트리거 필요):**
```sql
CREATE TABLE products (
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 2.3 LIMIT & OFFSET

**MySQL:**
```sql
-- 21~30번째 행
SELECT * FROM products
LIMIT 10 OFFSET 20;

-- 또는 (구형 문법)
SELECT * FROM products
LIMIT 20, 10;  -- OFFSET, LIMIT 순서!
```

**PostgreSQL:**
```sql
-- 21~30번째 행
SELECT * FROM products
LIMIT 10 OFFSET 20;  -- LIMIT, OFFSET 순서!
```

### 2.4 Boolean 타입

**MySQL:**
```sql
CREATE TABLE products (
    is_active TINYINT(1)  -- 0 또는 1
);

SELECT * FROM products WHERE is_active = 1;
```

**PostgreSQL:**
```sql
CREATE TABLE products (
    is_active BOOLEAN  -- true 또는 false
);

SELECT * FROM products WHERE is_active = true;
-- 또는
SELECT * FROM products WHERE is_active;
```

### 2.5 문자열 연결

**MySQL:**
```sql
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users;
```

**PostgreSQL:**
```sql
-- || 연산자
SELECT first_name || ' ' || last_name AS full_name FROM users;

-- CONCAT 함수도 사용 가능
SELECT CONCAT(first_name, ' ', last_name) AS full_name FROM users;
```

### 2.6 날짜/시간 함수

| 기능 | MySQL | PostgreSQL |
|------|-------|-----------|
| **현재 시각** | `NOW()`, `CURRENT_TIMESTAMP` | `NOW()`, `CURRENT_TIMESTAMP` |
| **날짜 추출** | `YEAR(date)`, `MONTH(date)` | `EXTRACT(YEAR FROM date)` |
| **날짜 연산** | `DATE_ADD(date, INTERVAL 1 DAY)` | `date + INTERVAL '1 day'` |
| **날짜 차이** | `DATEDIFF(date1, date2)` | `date1 - date2` (일 단위) |
| **포맷** | `DATE_FORMAT(date, '%Y-%m-%d')` | `TO_CHAR(date, 'YYYY-MM-DD')` |

**예시:**

```sql
-- MySQL
SELECT DATE_ADD(NOW(), INTERVAL 7 DAY);
SELECT DATEDIFF('2024-01-15', '2024-01-01');

-- PostgreSQL
SELECT NOW() + INTERVAL '7 days';
SELECT '2024-01-15'::DATE - '2024-01-01'::DATE;
```

### 2.7 IFNULL vs COALESCE

**MySQL:**
```sql
SELECT IFNULL(price, 0) FROM products;
```

**PostgreSQL:**
```sql
SELECT COALESCE(price, 0) FROM products;
-- MySQL도 COALESCE 지원 (표준)
```

### 2.8 대소문자 구분

**MySQL:**
```sql
-- 대소문자 무시 (기본)
SELECT * FROM products WHERE name = 'macbook';  -- 'MacBook' 매칭
```

**PostgreSQL:**
```sql
-- 대소문자 구분 (기본)
SELECT * FROM products WHERE name = 'macbook';  -- 'MacBook' 매칭 안됨

-- 대소문자 무시
SELECT * FROM products WHERE name ILIKE 'macbook';  -- 'MacBook' 매칭
```

### 2.9 RETURNING 절

**MySQL (8.0.21+, 제한적):**
```sql
-- INSERT만 지원
INSERT INTO products (name, price) VALUES ('iPhone', 1200000);
SELECT LAST_INSERT_ID();
```

**PostgreSQL:**
```sql
-- INSERT, UPDATE, DELETE 모두 지원
INSERT INTO products (name, price)
VALUES ('iPhone', 1200000)
RETURNING id, name, created_at;

UPDATE products SET price = 1100000
WHERE id = 1
RETURNING id, price, updated_at;

DELETE FROM products WHERE id = 1
RETURNING *;
```

---

## 3. JSON 기능 비교

### 3.1 JSON vs JSONB

| 항목 | MySQL JSON | PostgreSQL JSONB |
|------|-----------|------------------|
| **저장 형식** | Text (압축) | Binary |
| **인덱싱** | ❌ 제한적 | ✅ GIN 인덱스 |
| **쿼리 성능** | 느림 (파싱 필요) | 빠름 (Binary) |
| **중복 키** | 허용 | 제거 |
| **공백** | 유지 | 제거 |

### 3.2 JSON 쿼리

**MySQL:**
```sql
-- JSON 쿼리
SELECT JSON_EXTRACT(metadata, '$.color') AS color
FROM products;

-- 단축 표기
SELECT metadata->'$.color' AS color
FROM products;

-- WHERE 절
SELECT * FROM products
WHERE JSON_EXTRACT(metadata, '$.storage') = '256GB';
```

**PostgreSQL:**
```sql
-- JSONB 쿼리
SELECT metadata->'color' AS color  -- JSON 타입 반환
FROM products;

SELECT metadata->>'color' AS color  -- TEXT 타입 반환
FROM products;

-- WHERE 절
SELECT * FROM products
WHERE metadata->>'storage' = '256GB';

-- 포함 여부 (@>)
SELECT * FROM products
WHERE metadata @> '{"color": "black"}';

-- 키 존재 여부 (?)
SELECT * FROM products
WHERE metadata ? 'warranty';
```

### 3.3 JSON 인덱싱

**MySQL:**
```sql
-- 가상 컬럼 + 인덱스 (우회)
ALTER TABLE products
ADD COLUMN color_virtual VARCHAR(50)
    GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.color')));

CREATE INDEX idx_color ON products(color_virtual);
```

**PostgreSQL:**
```sql
-- GIN 인덱스 (직접)
CREATE INDEX idx_products_metadata ON products USING gin(metadata);

-- 특정 필드 인덱스
CREATE INDEX idx_products_color ON products ((metadata->>'color'));
```

---

## 4. Array 타입

### 4.1 MySQL (미지원)

**우회 방법:**
```sql
-- 1. 별도 테이블
CREATE TABLE product_tags (
    product_id BIGINT,
    tag VARCHAR(50),
    PRIMARY KEY (product_id, tag)
);

-- 2. JSON 배열
CREATE TABLE products (
    tags JSON  -- ["sale", "new", "bestseller"]
);
```

### 4.2 PostgreSQL (네이티브)

```sql
-- Array 타입
CREATE TABLE products (
    tags TEXT[]  -- ARRAY['sale', 'new', 'bestseller']
);

-- INSERT
INSERT INTO products (name, tags)
VALUES ('iPhone', ARRAY['smartphone', 'apple', 'new']);

-- 조회
SELECT * FROM products
WHERE 'sale' = ANY(tags);

-- 포함 여부
SELECT * FROM products
WHERE tags @> ARRAY['apple', 'new'];

-- 교집합
SELECT * FROM products
WHERE tags && ARRAY['sale', 'bestseller'];

-- 배열 길이
SELECT name, array_length(tags, 1) FROM products;

-- GIN 인덱스
CREATE INDEX idx_products_tags ON products USING gin(tags);
```

---

## 5. 고급 기능 비교

### 5.1 Window Functions

**MySQL 8.0+ (지원):**
```sql
SELECT
    name,
    price,
    ROW_NUMBER() OVER (ORDER BY price DESC) AS rank
FROM products;
```

**PostgreSQL (완전 지원):**
```sql
-- 기본
SELECT
    name,
    price,
    ROW_NUMBER() OVER (ORDER BY price DESC) AS row_num,
    RANK() OVER (ORDER BY price DESC) AS rank,
    DENSE_RANK() OVER (ORDER BY price DESC) AS dense_rank
FROM products;

-- PARTITION BY
SELECT
    category,
    name,
    price,
    RANK() OVER (PARTITION BY category ORDER BY price DESC) AS rank_in_category
FROM products;
```

### 5.2 CTE (Common Table Expressions)

**MySQL 8.0+ (지원):**
```sql
WITH top_products AS (
    SELECT * FROM products
    WHERE price > 1000000
)
SELECT * FROM top_products;
```

**PostgreSQL (+ Recursive CTE):**
```sql
-- 기본 CTE
WITH top_products AS (
    SELECT * FROM products
    WHERE price > 1000000
)
SELECT * FROM top_products;

-- Recursive CTE (카테고리 트리)
WITH RECURSIVE category_tree AS (
    SELECT id, name, parent_id, 0 AS level
    FROM categories
    WHERE parent_id IS NULL

    UNION ALL

    SELECT c.id, c.name, c.parent_id, ct.level + 1
    FROM categories c
    INNER JOIN category_tree ct ON c.parent_id = ct.id
)
SELECT * FROM category_tree;
```

### 5.3 Full-Text Search

**MySQL:**
```sql
-- FULLTEXT 인덱스 (InnoDB)
CREATE FULLTEXT INDEX idx_products_name ON products(name, description);

-- 검색
SELECT * FROM products
WHERE MATCH(name, description) AGAINST('MacBook' IN NATURAL LANGUAGE MODE);

-- Boolean Mode
SELECT * FROM products
WHERE MATCH(name, description) AGAINST('+MacBook -Air' IN BOOLEAN MODE);
```

**PostgreSQL:**
```sql
-- tsvector 타입
ALTER TABLE products
ADD COLUMN search_vector tsvector;

-- 업데이트 트리거
CREATE TRIGGER products_search_vector_update
BEFORE INSERT OR UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION
tsvector_update_trigger(search_vector, 'pg_catalog.english', name, description);

-- GIN 인덱스
CREATE INDEX idx_products_search ON products USING gin(search_vector);

-- 검색
SELECT * FROM products
WHERE search_vector @@ to_tsquery('MacBook & Pro');

-- 순위
SELECT name, ts_rank(search_vector, query) AS rank
FROM products, to_tsquery('MacBook') query
WHERE search_vector @@ query
ORDER BY rank DESC;
```

---

## 6. 선택 기준

### 6.1 PostgreSQL을 선택해야 하는 경우

| 상황 | 이유 | Portal Universe 적용 |
|------|------|---------------------|
| **복잡한 쿼리** | CTE, Window Functions, Subquery 최적화 | Shopping Service 분석 쿼리 |
| **JSONB 활용** | NoSQL 유연성 + RDBMS 일관성 | 상품 메타데이터, 배송지 |
| **Array 타입** | 네이티브 배열 지원 | 상품 태그, 카테고리 경로 |
| **높은 동시성** | MVCC로 읽기-쓰기 충돌 최소화 | 주문 처리 |
| **데이터 무결성** | 엄격한 제약 조건, 트리거 | 재고 관리, 결제 |
| **확장성** | Extension (PostGIS, pgcrypto) | 향후 위치 기반 서비스 |
| **전문 검색** | 내장 Full-Text Search | 상품 검색 |
| **표준 SQL** | SQL 표준 준수 | 다른 DB 전환 용이 |

### 6.2 MySQL을 유지해야 하는 경우

| 상황 | 이유 | Portal Universe 적용 |
|------|------|---------------------|
| **단순 CRUD** | MySQL 읽기 성능 우수, 운영 간편 | Auth Service |
| **레거시 시스템** | 마이그레이션 비용 | 기존 서비스 유지 |
| **팀 숙련도** | MySQL 경험 풍부, 학습 곡선 낮음 | 빠른 개발 |
| **읽기 중심** | SELECT 성능 최적화 | 캐싱 레이어 |
| **클라우드 호환성** | AWS RDS, Azure 등 광범위 지원 | 멀티 클라우드 |
| **운영 간편성** | 설정, 백업, 복제 간단 | DevOps 리소스 부족 |
| **레플리케이션** | Master-Slave 간단 | 읽기 분산 |

---

## 7. Portal Universe 서비스별 전략

### 7.1 전환 우선순위

| 서비스 | 현재 DB | 권장 DB | 우선순위 | 이유 |
|--------|---------|---------|---------|------|
| **Shopping** | MySQL | **PostgreSQL** | 🔴 높음 | 복잡한 쿼리, JSONB, Array, 동시성 |
| **Notification** | MySQL | **PostgreSQL** | 🟡 중간 | LISTEN/NOTIFY, Array 수신자 목록 |
| **Auth** | MySQL | **MySQL 유지** | 🟢 낮음 | 단순 CRUD, 전환 불필요 |
| **Blog** | MongoDB | **MongoDB 유지** | - | Document 모델 최적 |

### 7.2 Shopping Service 전환 시나리오

#### Before (MySQL)

```sql
-- products 테이블
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    stock INT NOT NULL,
    metadata JSON,  -- 제한적
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 태그는 별도 테이블
CREATE TABLE product_tags (
    product_id BIGINT,
    tag VARCHAR(50),
    PRIMARY KEY (product_id, tag)
);
```

#### After (PostgreSQL)

```sql
-- products 테이블
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    stock INTEGER NOT NULL,
    metadata JSONB,  -- Binary, 인덱싱 가능
    tags TEXT[],     -- 네이티브 배열
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- updated_at 트리거
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- GIN 인덱스
CREATE INDEX idx_products_metadata ON products USING gin(metadata);
CREATE INDEX idx_products_tags ON products USING gin(tags);
```

#### 개선 효과

| 항목 | Before (MySQL) | After (PostgreSQL) | 개선 |
|------|----------------|-------------------|------|
| **메타데이터 쿼리** | 느림 (JSON 파싱) | 빠름 (JSONB Binary) | 3~5배 |
| **태그 쿼리** | JOIN 필요 | 직접 조회 | 2~3배 |
| **복잡한 분석** | 느림 | Window Functions | 5~10배 |
| **동시 주문** | Lock 경쟁 | MVCC | 높은 처리량 |

---

## 8. 마이그레이션 체크리스트

### 8.1 사전 준비

- [ ] PostgreSQL 학습 (문법, 기능)
- [ ] 테스트 환경 구성 (Docker)
- [ ] 현재 스키마 분석
- [ ] 쿼리 패턴 분석 (Slow Query Log)

### 8.2 스키마 변환

- [ ] `AUTO_INCREMENT` → `SERIAL`
- [ ] `TINYINT(1)` → `BOOLEAN`
- [ ] `JSON` → `JSONB`
- [ ] `ON UPDATE` → 트리거
- [ ] 별도 테이블 → `Array` 타입 검토
- [ ] 인덱스 재설계 (GIN, GiST)

### 8.3 애플리케이션 수정

- [ ] `build.gradle` 의존성 변경
- [ ] `application.yml` 설정 변경
- [ ] JPA Entity 수정 (`@GeneratedValue` 전략)
- [ ] Native Query 수정
- [ ] 날짜/시간 함수 수정
- [ ] 테스트 코드 수정

### 8.4 데이터 이전

- [ ] pgloader 설치
- [ ] 변환 스크립트 작성
- [ ] 소량 데이터 테스트
- [ ] 전체 데이터 이전
- [ ] 데이터 검증

### 8.5 성능 테스트

- [ ] 읽기 쿼리 성능 측정
- [ ] 쓰기 쿼리 성능 측정
- [ ] 동시성 테스트
- [ ] EXPLAIN ANALYZE 분석
- [ ] 인덱스 최적화

---

## 9. 핵심 요약

| 분야 | MySQL 강점 | PostgreSQL 강점 |
|------|----------|----------------|
| **성능** | 단순 읽기, OLTP | 복잡한 쿼리, 동시성 |
| **기능** | 간결함, 운영 편의성 | JSONB, Array, Extension |
| **학습** | 낮은 진입 장벽 | 높은 확장성 |
| **사용 사례** | CMS, 단순 CRUD | ERP, 분석, 복잡한 도메인 |

**Portal Universe 권장:**
- **Shopping Service**: PostgreSQL 전환 (복잡한 쿼리, JSONB, 높은 동시성)
- **Auth Service**: MySQL 유지 (단순 CRUD, 전환 불필요)
- **Blog Service**: MongoDB 유지 (Document 모델 최적)

---

## 관련 문서

- 이전: [PostgreSQL SQL 기초](./postgresql-sql-fundamentals.md)
- 다음: [PostgreSQL 데이터 타입](./postgresql-data-types.md)
- 마이그레이션: [MySQL → PostgreSQL 마이그레이션](./postgresql-migration.md)

---

## 참고 자료

- [PostgreSQL vs MySQL Feature Matrix](https://www.postgresql.org/about/featurematrix/)
- [MySQL to PostgreSQL Migration](https://wiki.postgresql.org/wiki/Converting_from_other_Databases_to_PostgreSQL)
- [pgloader](https://pgloader.io/)
