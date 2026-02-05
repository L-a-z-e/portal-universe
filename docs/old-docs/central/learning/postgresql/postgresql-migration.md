# PostgreSQL 마이그레이션 가이드

**난이도:** ⭐⭐⭐⭐

## 학습 목표
- MySQL에서 PostgreSQL로의 완전한 마이그레이션 프로세스 이해
- 스키마 변환 체크리스트 및 자동화 도구 활용
- pgloader를 사용한 대용량 데이터 이전
- Spring Boot 코드 수정 및 호환성 처리
- Portal Universe Shopping Service의 실제 마이그레이션 시나리오
- 검증 및 롤백 전략 수립

---

## 1. 마이그레이션 개요

### 1.1 마이그레이션 5단계

```
1. 분석 (Analysis)
   ↓
2. 스키마 변환 (Schema Conversion)
   ↓
3. 데이터 이전 (Data Migration)
   ↓
4. 코드 수정 (Application Update)
   ↓
5. 검증 및 배포 (Verification & Deployment)
```

### 1.2 주요 차이점 요약

| 항목 | MySQL | PostgreSQL | 변환 필요 |
|------|-------|------------|----------|
| **AUTO_INCREMENT** | `AUTO_INCREMENT` | `SERIAL` / `SEQUENCE` | ✅ |
| **자동 업데이트** | `ON UPDATE CURRENT_TIMESTAMP` | 트리거 필요 | ✅ |
| **Boolean** | `TINYINT(1)` | `BOOLEAN` | ✅ |
| **날짜/시간** | `DATETIME` | `TIMESTAMP` | ⚠️ |
| **문자열 비교** | 대소문자 구분 안 함 | 대소문자 구분 | ✅ |
| **LIMIT** | `LIMIT n, m` | `LIMIT n OFFSET m` | ✅ |
| **인덱스 힌트** | `USE INDEX`, `FORCE INDEX` | ❌ (제거 필요) | ✅ |
| **저장 프로시저** | MySQL 문법 | PostgreSQL 문법 | ✅ |
| **JSON** | `JSON` | `JSONB` | ⚠️ |

---

## 2. 1단계: 분석 (Analysis)

### 2.1 데이터베이스 구조 분석

```sql
-- MySQL: 데이터베이스 크기 확인
SELECT
    table_schema AS database_name,
    ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS size_mb
FROM information_schema.TABLES
WHERE table_schema = 'shopping_db'
GROUP BY table_schema;

-- MySQL: 테이블별 크기 및 행 수
SELECT
    table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb,
    table_rows
FROM information_schema.TABLES
WHERE table_schema = 'shopping_db'
ORDER BY (data_length + index_length) DESC;

-- MySQL: 인덱스 확인
SELECT
    table_name,
    index_name,
    column_name,
    non_unique
FROM information_schema.STATISTICS
WHERE table_schema = 'shopping_db'
ORDER BY table_name, index_name, seq_in_index;

-- MySQL: 외래 키 확인
SELECT
    constraint_name,
    table_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.KEY_COLUMN_USAGE
WHERE table_schema = 'shopping_db'
  AND referenced_table_name IS NOT NULL;
```

### 2.2 호환성 체크리스트

| 항목 | 확인 사항 | 조치 |
|------|----------|------|
| **테이블 수** | 100개 이하 | 순차 마이그레이션 |
| **데이터 크기** | 10GB 이하 | pgloader 사용 |
| **외래 키** | 순환 참조 없음 | 순서 조정 필요 |
| **저장 프로시저** | 개수 확인 | 수동 변환 |
| **트리거** | 개수 확인 | 수동 변환 |
| **View** | 개수 확인 | 수동 변환 |
| **특수 컬럼** | ENUM, SET | 변환 필요 |
| **문자셋** | UTF-8 | 호환 |

---

## 3. 2단계: 스키마 변환

### 3.1 자동 변환 도구

**방법 1: pgloader (권장)**

pgloader는 MySQL 스키마를 자동으로 PostgreSQL로 변환합니다.

```bash
# macOS 설치
brew install pgloader

# Ubuntu 설치
sudo apt-get install pgloader
```

**방법 2: MySQL Workbench → SQL Export → 수동 변환**

### 3.2 스키마 변환 체크리스트

**1. AUTO_INCREMENT → SERIAL**

```sql
-- MySQL
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- PostgreSQL
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);
```

**2. ON UPDATE CURRENT_TIMESTAMP → 트리거**

```sql
-- MySQL
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- PostgreSQL
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
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
CREATE TRIGGER products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

**3. TINYINT(1) → BOOLEAN**

```sql
-- MySQL
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    is_deleted TINYINT(1) DEFAULT 0
);

-- PostgreSQL
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    is_deleted BOOLEAN DEFAULT FALSE
);
```

**4. DATETIME → TIMESTAMP**

```sql
-- MySQL
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- PostgreSQL
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**5. ENUM → VARCHAR + CHECK**

```sql
-- MySQL
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')
);

-- PostgreSQL (방법 1: VARCHAR + CHECK)
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')
    )
);

-- PostgreSQL (방법 2: ENUM 타입)
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED');

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    status order_status NOT NULL
);
```

**6. 인덱스 변환**

```sql
-- MySQL
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category_price ON products(category_id, price DESC);

-- PostgreSQL (동일)
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category_price ON products(category_id, price DESC);
```

**7. 외래 키 변환**

```sql
-- MySQL
ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_order
FOREIGN KEY (order_id) REFERENCES orders(id)
ON DELETE CASCADE;

-- PostgreSQL (동일)
ALTER TABLE order_items
ADD CONSTRAINT fk_order_items_order
FOREIGN KEY (order_id) REFERENCES orders(id)
ON DELETE CASCADE;
```

### 3.3 Portal Universe Shopping Service 스키마 변환

**MySQL 원본:**

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_products_category (category_id),
    INDEX idx_products_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED') NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_user (user_id),
    INDEX idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**PostgreSQL 변환:**

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);

CREATE TRIGGER products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_orders_status CHECK (
        status IN ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED')
    )
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);

CREATE TRIGGER orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

---

## 4. 3단계: 데이터 이전

### 4.1 pgloader 사용 (권장)

**설정 파일 생성: `shopping_db.load`**

```lisp
LOAD DATABASE
    FROM mysql://root:password@localhost:3306/shopping_db
    INTO postgresql://postgres:postgres@localhost:5432/shopping_db

WITH
    include drop,
    create tables,
    create indexes,
    reset sequences,
    downcase identifiers,
    data only

SET
    work_mem to '256MB',
    maintenance_work_mem to '512MB'

CAST
    type tinyint to boolean drop typemod using tinyint-to-boolean,
    type datetime to timestamp drop timezone drop not null drop default using zero-dates-to-null

BEFORE LOAD DO
    $$ DROP SCHEMA IF EXISTS public CASCADE; $$,
    $$ CREATE SCHEMA public; $$

AFTER LOAD DO
    $$ ALTER SEQUENCE products_id_seq RESTART WITH 1000; $$;
```

**실행:**

```bash
# 전체 마이그레이션
pgloader shopping_db.load

# 특정 테이블만
pgloader mysql://root:password@localhost/shopping_db \
          postgresql://postgres:postgres@localhost/shopping_db \
          --with "include only table names matching 'products'"
```

**출력 예시:**

```
                table name     errors       rows      bytes      total time
-------------------------  ---------  ---------  ---------  --------------
              fetch meta        0          3                     0.234s
               Create Schemas   0          1                     0.012s
             Create SQL Types   0          0                     0.005s
                Create tables   0         15                     0.089s
               Set Table OIDs   0         15                     0.034s
-------------------------  ---------  ---------  ---------  --------------
        public.products        0     100000    25.6 MB         5.234s
        public.categories      0         20     2.3 KB         0.012s
        public.orders          0      50000    12.5 MB         2.456s
        public.order_items     0     150000    18.9 MB         3.789s
-------------------------  ---------  ---------  ---------  --------------
COPY Threads Completion        0          4                    11.567s
         Create Indexes        0         12                     2.345s
        Reset Sequences        0          4                     0.123s
           Primary Keys        0          4                     0.234s
    Create Foreign Keys        0          6                     0.456s
        Create Triggers        0          8                     0.678s
       Install Comments        0          0                     0.000s
-------------------------  ---------  ---------  ---------  --------------
      Total import time       ✓     300020    57.0 MB        15.234s
```

### 4.2 수동 데이터 이전 (소규모)

**MySQL 데이터 덤프:**

```bash
# 데이터만 덤프 (INSERT 문)
mysqldump -u root -p \
    --no-create-info \
    --skip-triggers \
    --complete-insert \
    --skip-extended-insert \
    shopping_db > data.sql

# 특정 테이블만
mysqldump -u root -p \
    --no-create-info \
    shopping_db products > products_data.sql
```

**PostgreSQL로 수정:**

```bash
# 1. AUTO_INCREMENT 제거
sed -i '' 's/AUTO_INCREMENT=[0-9]*//g' data.sql

# 2. 따옴표 변환 (MySQL: `, PostgreSQL: ")
sed -i '' "s/\`/\"/g" data.sql

# 3. TINYINT(1) 값 변환 (0 → FALSE, 1 → TRUE)
sed -i '' 's/,0,/,FALSE,/g' data.sql
sed -i '' 's/,1,/,TRUE,/g' data.sql

# 4. NOW() → CURRENT_TIMESTAMP
sed -i '' 's/NOW()/CURRENT_TIMESTAMP/g' data.sql
```

**PostgreSQL로 임포트:**

```bash
psql -U postgres -d shopping_db -f data.sql
```

### 4.3 대용량 데이터 이전 (COPY 명령)

**1. MySQL에서 CSV 추출:**

```sql
-- MySQL
SELECT * INTO OUTFILE '/tmp/products.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM products;
```

**2. PostgreSQL로 COPY:**

```sql
-- PostgreSQL
COPY products (id, name, description, price, stock_quantity, category_id, brand_id, is_deleted, created_at, updated_at)
FROM '/tmp/products.csv'
WITH (FORMAT csv, HEADER true);
```

### 4.4 시퀀스 재설정

```sql
-- 시퀀스 확인
SELECT
    schemaname,
    sequencename,
    last_value
FROM pg_sequences;

-- 시퀀스 재설정 (현재 최대값 + 1)
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));

-- 또는 자동 재설정
SELECT
    'SELECT setval(''' || sequence_name || ''', (SELECT MAX(' || column_name || ') FROM ' || table_name || '));' AS sql
FROM information_schema.columns
WHERE column_default LIKE 'nextval%'
  AND table_schema = 'public';
```

---

## 5. 4단계: Spring Boot 코드 수정

### 5.1 build.gradle 수정

```gradle
dependencies {
    // MySQL 제거
    // runtimeOnly 'com.mysql:mysql-connector-j'

    // PostgreSQL 추가
    runtimeOnly 'org.postgresql:postgresql:42.7.1'

    // Flyway PostgreSQL
    implementation 'org.flywaydb:flyway-core:10.4.1'
    implementation 'org.flywaydb:flyway-database-postgresql:10.4.1'
}
```

### 5.2 application.yml 수정

```yaml
spring:
  datasource:
    # MySQL
    # driver-class-name: com.mysql.cj.jdbc.Driver
    # url: jdbc:mysql://localhost:3306/shopping_db?useUnicode=true&characterEncoding=utf8
    # username: root
    # password: password

    # PostgreSQL
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/shopping_db
    username: postgres
    password: postgres

  jpa:
    # MySQL
    # database-platform: org.hibernate.dialect.MySQL8Dialect

    # PostgreSQL
    database-platform: org.hibernate.dialect.PostgreSQLDialect

    hibernate:
      ddl-auto: validate  # Flyway 사용 시

  flyway:
    enabled: true
    # MySQL
    # locations: classpath:db/migration/mysql

    # PostgreSQL
    locations: classpath:db/migration/postgresql
```

### 5.3 Entity 수정

```java
// BEFORE (MySQL)
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_deleted", columnDefinition = "TINYINT(1)")
    private Boolean isDeleted = false;

    @Column(name = "created_at", columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
}

// AFTER (PostgreSQL)
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // SERIAL
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;  // BOOLEAN

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // TIMESTAMP

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;  // TIMESTAMP (트리거로 자동 업데이트)
}
```

### 5.4 Repository 쿼리 수정

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    // BEFORE (MySQL)
    // @Query(value = "SELECT * FROM products LIMIT ?1, ?2", nativeQuery = true)
    // List<Product> findWithPagination(int offset, int limit);

    // AFTER (PostgreSQL)
    @Query(value = "SELECT * FROM products LIMIT ?2 OFFSET ?1", nativeQuery = true)
    List<Product> findWithPagination(int offset, int limit);

    // BEFORE (MySQL - 대소문자 구분 안 함)
    // Optional<Product> findByName(String name);

    // AFTER (PostgreSQL - 대소문자 구분)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(:name)")
    Optional<Product> findByName(@Param("name") String name);

    // 또는 네이티브 쿼리
    @Query(value = "SELECT * FROM products WHERE name ILIKE :name", nativeQuery = true)
    Optional<Product> findByNameIgnoreCase(@Param("name") String name);
}
```

### 5.5 날짜/시간 처리

```java
// MySQL: DATETIME (로컬 시간)
// PostgreSQL: TIMESTAMP (UTC)

// application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul  # PostgreSQL에서 로컬 시간 사용

// 또는 Entity에서
@Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
private ZonedDateTime createdAt;
```

---

## 6. 5단계: 검증 및 배포

### 6.1 데이터 무결성 검증

```sql
-- 1. 행 수 비교
-- MySQL
SELECT 'products' AS table_name, COUNT(*) AS row_count FROM products
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

-- PostgreSQL (동일한 쿼리 실행)

-- 2. 체크섬 비교 (샘플링)
-- MySQL
SELECT
    SUM(CRC32(CONCAT_WS(',', id, name, price))) AS checksum
FROM (SELECT * FROM products ORDER BY id LIMIT 1000) AS sample;

-- PostgreSQL
SELECT
    SUM(hashtext(CONCAT_WS(',', id::TEXT, name, price::TEXT))::BIGINT) AS checksum
FROM (SELECT * FROM products ORDER BY id LIMIT 1000) AS sample;

-- 3. 최대/최소 ID 비교
SELECT MAX(id), MIN(id) FROM products;
SELECT MAX(id), MIN(id) FROM orders;

-- 4. 합계 비교
SELECT SUM(total_amount) FROM orders WHERE status = 'PAID';
```

### 6.2 기능 테스트

```java
@SpringBootTest
@ActiveProfiles("postgresql")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void testProductCreation() {
        ProductRequest request = ProductRequest.builder()
            .name("Test Product")
            .price(BigDecimal.valueOf(10000))
            .categoryId(1L)
            .build();

        ProductResponse response = productService.createProduct(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void testProductSearch() {
        List<ProductResponse> products = productService.searchByName("iPhone");

        assertThat(products).isNotEmpty();
    }

    @Test
    void testTransactionRollback() {
        assertThrows(InsufficientStockException.class, () -> {
            productService.decreaseStock(999L, 100);
        });

        // 롤백 확인
        Product product = productRepository.findById(999L).orElseThrow();
        assertThat(product.getStockQuantity()).isEqualTo(10);  // 변경 안 됨
    }
}
```

### 6.3 성능 테스트

```java
@SpringBootTest
class PerformanceTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void benchmarkProductSearch() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            productRepository.findByCategoryId(5L);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("1000 queries: " + duration + "ms");
        assertThat(duration).isLessThan(5000);  // 5초 이내
    }
}
```

### 6.4 롤백 전략

**시나리오 1: 마이그레이션 실패 시 즉시 롤백**

```bash
# PostgreSQL 백업
pg_dump -U postgres shopping_db > backup_before_migration.sql

# 마이그레이션 실행
pgloader shopping_db.load

# 실패 시 롤백
psql -U postgres -c "DROP DATABASE shopping_db;"
psql -U postgres -c "CREATE DATABASE shopping_db;"
psql -U postgres shopping_db < backup_before_migration.sql
```

**시나리오 2: 블루-그린 배포**

```
1. PostgreSQL 환경 구성 (Green)
2. 데이터 마이그레이션
3. 애플리케이션 배포 (Green)
4. 트래픽 일부 전환 (10%)
5. 모니터링 (1시간)
6. 문제 없으면 전체 전환 (100%)
7. 문제 발생 시 즉시 롤백 (Blue)
```

**시나리오 3: 점진적 마이그레이션**

```
1. MySQL + PostgreSQL 동시 운영 (Dual Write)
2. 읽기 트래픽만 PostgreSQL로 전환
3. 모니터링 (1주일)
4. 쓰기 트래픽도 PostgreSQL로 전환
5. MySQL은 백업으로 유지 (1개월)
6. 최종 MySQL 종료
```

---

## 7. Portal Universe Shopping Service 마이그레이션 시나리오

### 7.1 사전 준비

```bash
# 1. PostgreSQL 설치 (Docker)
docker run -d \
  --name postgres-shopping \
  -e POSTGRES_DB=shopping_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:16-alpine

# 2. pgloader 설치
brew install pgloader

# 3. MySQL 백업
mysqldump -u root -p shopping_db > mysql_backup.sql
```

### 7.2 마이그레이션 실행

**1. 스키마 변환 스크립트 작성: `schema_conversion.sql`**

```sql
-- updated_at 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- products 트리거
CREATE TRIGGER products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- orders 트리거
CREATE TRIGGER orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- order_items 트리거
CREATE TRIGGER order_items_updated_at
BEFORE UPDATE ON order_items
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- CHECK 제약 추가
ALTER TABLE orders
ADD CONSTRAINT chk_orders_status
CHECK (status IN ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- 부분 인덱스
CREATE INDEX idx_products_active
ON products(category_id, price DESC)
WHERE is_deleted = FALSE;

CREATE INDEX idx_orders_pending
ON orders(created_at)
WHERE status IN ('PENDING', 'PAID');
```

**2. pgloader 설정: `shopping_db.load`**

```lisp
LOAD DATABASE
    FROM mysql://root:password@localhost:3306/shopping_db
    INTO postgresql://postgres:postgres@localhost:5432/shopping_db

WITH
    include drop,
    create tables,
    create indexes,
    reset sequences,
    workers = 8,
    concurrency = 4

SET
    work_mem to '256MB',
    maintenance_work_mem to '1GB'

CAST
    type tinyint to boolean drop typemod,
    type datetime to timestamp drop timezone

BEFORE LOAD DO
    $$ DROP SCHEMA IF EXISTS public CASCADE; $$,
    $$ CREATE SCHEMA public; $$

AFTER LOAD DO
    $$ \i /path/to/schema_conversion.sql $$;
```

**3. 실행**

```bash
# 마이그레이션 실행
pgloader shopping_db.load

# 로그 확인
tail -f /tmp/pgloader.log
```

### 7.3 애플리케이션 수정

**build.gradle**

```gradle
dependencies {
    runtimeOnly 'org.postgresql:postgresql:42.7.1'
    implementation 'org.flywaydb:flyway-database-postgresql:10.4.1'
}
```

**application-postgresql.yml**

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/shopping_db
    username: postgres
    password: postgres
    hikari:
      maximum-pool-size: 20

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql
```

### 7.4 검증

```bash
# 1. 데이터 비교
mysql -u root -p -e "SELECT COUNT(*) FROM shopping_db.products;"
psql -U postgres -d shopping_db -c "SELECT COUNT(*) FROM products;"

# 2. 애플리케이션 테스트
./gradlew test -Dspring.profiles.active=postgresql

# 3. 통합 테스트
./gradlew bootRun -Dspring.profiles.active=postgresql
```

---

## 8. 핵심 정리

| 단계 | 도구/방법 | 소요 시간 (10GB 기준) |
|------|----------|---------------------|
| **1. 분석** | SQL 쿼리 | 1시간 |
| **2. 스키마 변환** | 수동 or pgloader | 2시간 |
| **3. 데이터 이전** | pgloader | 30분 |
| **4. 코드 수정** | 수동 | 4시간 |
| **5. 검증** | 테스트 | 2시간 |
| **총 소요 시간** | - | **1일** |

---

## 9. 실습 예제

### 실습 1: 샘플 데이터베이스 마이그레이션

```bash
# MySQL 샘플 데이터 생성
mysql -u root -p -e "
CREATE DATABASE test_migration;
USE test_migration;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255),
    is_active TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO users (name, email) VALUES
('Alice', 'alice@example.com'),
('Bob', 'bob@example.com'),
('Charlie', 'charlie@example.com');
"

# pgloader로 마이그레이션
pgloader mysql://root:password@localhost/test_migration \
          postgresql://postgres:postgres@localhost/test_migration

# 확인
psql -U postgres test_migration -c "SELECT * FROM users;"
```

---

## 10. 관련 문서

- [PostgreSQL Spring 통합](./postgresql-spring-integration.md)
- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL Best Practices](./postgresql-best-practices.md)

---

## 11. 참고 자료

- [pgloader Documentation](https://pgloader.readthedocs.io/)
- [PostgreSQL Migration Guide](https://www.postgresql.org/docs/current/migration.html)
- [MySQL to PostgreSQL Migration](https://wiki.postgresql.org/wiki/Converting_from_other_Databases_to_PostgreSQL#MySQL)
- [AWS Database Migration Service](https://aws.amazon.com/dms/)
