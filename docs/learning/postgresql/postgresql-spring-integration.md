# PostgreSQL Spring Boot 통합

**난이도:** ⭐⭐⭐

## 학습 목표
- Spring Boot에서 PostgreSQL 연결 설정 방법 학습
- JPA/Hibernate PostgreSQL Dialect 설정
- Entity 매핑 및 ID 생성 전략 이해
- Flyway를 통한 마이그레이션 관리
- HikariCP 연결 풀 최적화
- Portal Universe Shopping Service PostgreSQL 통합 구현

---

## 1. 프로젝트 설정

### 1.1 의존성 추가 (build.gradle)

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.portal.universe'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // PostgreSQL Driver
    runtimeOnly 'org.postgresql:postgresql:42.7.1'

    // Flyway Migration
    implementation 'org.flywaydb:flyway-core:10.4.1'
    implementation 'org.flywaydb:flyway-database-postgresql:10.4.1'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.h2database:h2'  // 테스트용
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 1.2 MySQL vs PostgreSQL 의존성 비교

| 항목 | MySQL | PostgreSQL |
|------|-------|------------|
| Driver | `mysql:mysql-connector-java` | `org.postgresql:postgresql` |
| Dialect | `MySQL8Dialect` | `PostgreSQLDialect` |
| Flyway | `flyway-mysql` | `flyway-database-postgresql` |
| JDBC URL | `jdbc:mysql://` | `jdbc:postgresql://` |
| 기본 포트 | 3306 | 5432 |

---

## 2. application.yml 설정

### 2.1 로컬 개발 환경 (application-local.yml)

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/shopping_db
    username: postgres
    password: postgres

    # HikariCP 설정
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: ShoppingHikariPool
      connection-test-query: SELECT 1
      auto-commit: true

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Flyway 사용 시 validate 권장
    properties:
      hibernate:
        format_sql: true
        show_sql: false  # logging.level로 대체
        use_sql_comments: true
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
        # PostgreSQL 특화 설정
        dialect:
          postgres:
            force_string_in_between: true
    show-sql: false
    open-in-view: false  # OSIV 비활성화 (성능 최적화)

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: public
    table: flyway_schema_history
    validate-on-migrate: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
    com.zaxxer.hikari: DEBUG
    org.flywaydb: INFO
```

### 2.2 Docker 환경 (application-docker.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/shopping_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

    hikari:
      maximum-pool-size: 20
      minimum-idle: 10

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        show_sql: false

logging:
  level:
    org.hibernate.SQL: INFO
    org.hibernate.orm.jdbc.bind: INFO
```

### 2.3 Kubernetes 환경 (application-k8s.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:postgres-service}:5432/${DB_NAME:shopping_db}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

    hikari:
      maximum-pool-size: 30
      minimum-idle: 15
      connection-timeout: 20000
      leak-detection-threshold: 60000

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        show_sql: false

logging:
  level:
    org.hibernate.SQL: WARN
```

### 2.4 MySQL vs PostgreSQL 설정 비교

| 설정 | MySQL | PostgreSQL |
|------|-------|------------|
| **Driver** | `com.mysql.cj.jdbc.Driver` | `org.postgresql.Driver` |
| **URL** | `jdbc:mysql://host:3306/db` | `jdbc:postgresql://host:5432/db` |
| **Dialect** | `org.hibernate.dialect.MySQL8Dialect` | `org.hibernate.dialect.PostgreSQLDialect` |
| **Default Schema** | database name | `public` |
| **URL Parameters** | `?useUnicode=true&characterEncoding=utf8` | `?currentSchema=public` |

---

## 3. Entity 매핑

### 3.1 Product Entity

```java
package com.portal.universe.shoppingservice.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category_price", columnList = "category_id,price"),
        @Index(name = "idx_products_brand_created", columnList = "brand_id,created_at"),
        @Index(name = "idx_products_name", columnList = "name")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // PostgreSQL SERIAL
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;

    // PostgreSQL Array 타입
    @Column(columnDefinition = "text[]")
    private String[] tags;

    // PostgreSQL JSONB 타입
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;  // JSON 문자열로 저장

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version  // 낙관적 락
    private Long version;

    // 비즈니스 로직
    public void decreaseStock(Integer quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalArgumentException("재고가 부족합니다.");
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(Integer quantity) {
        this.stockQuantity += quantity;
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }
}
```

### 3.2 Order Entity

```java
package com.portal.universe.shoppingservice.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_orders_status_created", columnList = "status,created_at"),
        @Index(name = "idx_orders_number", columnList = "order_number", unique = true)
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // PostgreSQL JSONB 타입
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String shippingAddress;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 양방향 관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // 비즈니스 로직
    public void markAsPaid() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 가능한 상태가 아닙니다.");
        }
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("취소 가능한 상태가 아닙니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PAID;
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}
```

### 3.3 OrderItem Entity

```java
package com.portal.universe.shoppingservice.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id"),
        @Index(name = "idx_order_items_product", columnList = "product_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
```

### 3.4 ID 생성 전략 비교

| 전략 | MySQL | PostgreSQL |
|------|-------|------------|
| **AUTO** | AUTO_INCREMENT | SERIAL |
| **IDENTITY** | `@GeneratedValue(strategy = IDENTITY)` | `@GeneratedValue(strategy = IDENTITY)` |
| **SEQUENCE** | ❌ (지원 안 함) | ✅ `@GeneratedValue(strategy = SEQUENCE)` |
| **TABLE** | ✅ (성능 낮음) | ✅ (권장 안 함) |
| **UUID** | CHAR(36) | UUID (네이티브 타입) |

**PostgreSQL SEQUENCE 사용:**

```java
@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_seq")
    @SequenceGenerator(
        name = "product_seq",
        sequenceName = "products_id_seq",
        allocationSize = 1
    )
    private Long id;
}
```

**PostgreSQL UUID 사용:**

```java
@Entity
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;
}
```

---

## 4. Repository 패턴

### 4.1 ProductRepository

```java
package com.portal.universe.shoppingservice.product.repository;

import com.portal.universe.shoppingservice.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 카테고리별 제품 조회
     */
    Page<Product> findByCategoryIdAndIsDeletedFalse(
        Long categoryId,
        Pageable pageable
    );

    /**
     * 가격 범위 검색
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.categoryId = :categoryId " +
           "AND p.price BETWEEN :minPrice AND :maxPrice " +
           "AND p.isDeleted = false")
    Page<Product> findByCategoryAndPriceRange(
        @Param("categoryId") Long categoryId,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * 비관적 락으로 제품 조회 (재고 차감용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    /**
     * 재고 부족 제품 조회
     */
    @Query("SELECT p FROM Product p " +
           "WHERE p.stockQuantity < :threshold " +
           "AND p.isDeleted = false")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    /**
     * PostgreSQL Full-Text Search (네이티브 쿼리)
     */
    @Query(value =
        "SELECT * FROM products p " +
        "WHERE to_tsvector('english', p.name || ' ' || COALESCE(p.description, '')) " +
        "@@ to_tsquery('english', :query) " +
        "AND p.is_deleted = false",
        nativeQuery = true)
    List<Product> searchByFullText(@Param("query") String query);

    /**
     * JSONB 메타데이터 검색
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata @> :jsonQuery::jsonb " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByMetadata(@Param("jsonQuery") String jsonQuery);

    /**
     * Array 태그 검색
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE tags @> ARRAY[:tag]::text[] " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByTag(@Param("tag") String tag);
}
```

### 4.2 OrderRepository

```java
package com.portal.universe.shoppingservice.order.repository;

import com.portal.universe.shoppingservice.order.entity.Order;
import com.portal.universe.shoppingservice.order.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 사용자별 주문 목록
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * 주문 번호로 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * 비관적 락으로 주문 조회 (결제 처리용)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    /**
     * 상태별 주문 조회
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.status = :status " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByStatusAndDateRange(
        @Param("status") OrderStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 월별 매출 집계 (PostgreSQL EXTRACT 함수)
     */
    @Query(value =
        "SELECT " +
        "  EXTRACT(YEAR FROM created_at) AS year, " +
        "  EXTRACT(MONTH FROM created_at) AS month, " +
        "  COUNT(*) AS order_count, " +
        "  SUM(total_amount) AS total_revenue " +
        "FROM orders " +
        "WHERE status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
        "GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at) " +
        "ORDER BY year DESC, month DESC",
        nativeQuery = true)
    List<Object[]> getMonthlySalesReport();

    /**
     * JSONB 배송 주소 검색
     */
    @Query(value =
        "SELECT * FROM orders " +
        "WHERE shipping_address->>'city' = :city",
        nativeQuery = true)
    List<Order> findByShippingCity(@Param("city") String city);
}
```

---

## 5. Flyway 마이그레이션

### 5.1 디렉토리 구조

```
src/main/resources/db/migration/
├── V1__create_products_table.sql
├── V2__create_orders_table.sql
├── V3__create_order_items_table.sql
├── V4__add_product_indexes.sql
└── V5__add_jsonb_support.sql
```

### 5.2 V1__create_products_table.sql

```sql
-- Products 테이블 생성
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
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 기본 인덱스
CREATE INDEX idx_products_category_price ON products(category_id, price DESC)
WHERE is_deleted = FALSE;

CREATE INDEX idx_products_brand_created ON products(brand_id, created_at DESC)
WHERE is_deleted = FALSE;

CREATE INDEX idx_products_name ON products(name);

-- GIN 인덱스 (배열)
CREATE INDEX idx_products_tags_gin ON products USING GIN(tags)
WHERE is_deleted = FALSE;

-- GIN 인덱스 (JSONB)
CREATE INDEX idx_products_metadata_gin ON products USING GIN(metadata)
WHERE metadata IS NOT NULL;

-- 전문 검색 인덱스
CREATE INDEX idx_products_fulltext ON products
USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- 업데이트 트리거 (updated_at 자동 갱신)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 코멘트 추가
COMMENT ON TABLE products IS '제품 정보';
COMMENT ON COLUMN products.metadata IS 'JSON 형식의 추가 메타데이터';
COMMENT ON COLUMN products.tags IS '제품 태그 배열';
```

### 5.3 V2__create_orders_table.sql

```sql
-- Orders 테이블 생성
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    shipping_address JSONB NOT NULL,
    payment_method VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_status_created ON orders(status, created_at DESC);
CREATE UNIQUE INDEX idx_orders_number ON orders(order_number);

-- 부분 인덱스 (처리 대기 주문)
CREATE INDEX idx_orders_pending ON orders(created_at)
WHERE status IN ('PENDING', 'PAID');

-- GIN 인덱스 (배송 주소 검색)
CREATE INDEX idx_orders_shipping_address_gin ON orders USING GIN(shipping_address);

-- 월별 집계용 표현식 인덱스
CREATE INDEX idx_orders_year_month ON orders(
    EXTRACT(YEAR FROM created_at),
    EXTRACT(MONTH FROM created_at)
);

-- updated_at 트리거
CREATE TRIGGER orders_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 제약 조건
ALTER TABLE orders ADD CONSTRAINT chk_orders_status
CHECK (status IN ('PENDING', 'PAID', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

ALTER TABLE orders ADD CONSTRAINT chk_orders_total_amount
CHECK (total_amount >= 0);

COMMENT ON TABLE orders IS '주문 정보';
COMMENT ON COLUMN orders.shipping_address IS 'JSONB 형식의 배송 주소';
```

### 5.4 V3__create_order_items_table.sql

```sql
-- Order Items 테이블 생성
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id, created_at DESC);

-- Covering Index
CREATE INDEX idx_order_items_order_covering ON order_items(order_id)
INCLUDE (product_id, quantity, unit_price);

-- 제약 조건
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_quantity
CHECK (quantity > 0);

ALTER TABLE order_items ADD CONSTRAINT chk_order_items_unit_price
CHECK (unit_price >= 0);

COMMENT ON TABLE order_items IS '주문 아이템';
```

### 5.5 MySQL vs PostgreSQL 마이그레이션 차이

| 기능 | MySQL | PostgreSQL |
|------|-------|------------|
| **AUTO_INCREMENT** | `id BIGINT AUTO_INCREMENT` | `id BIGSERIAL` |
| **현재 시간** | `DEFAULT CURRENT_TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` |
| **자동 업데이트** | `ON UPDATE CURRENT_TIMESTAMP` | 트리거 필요 |
| **배열** | ❌ (JSON 사용) | `TEXT[]` |
| **JSON** | JSON 타입 | JSONB 타입 (바이너리) |
| **전문 검색** | FULLTEXT INDEX | GIN + tsvector |
| **부분 인덱스** | ❌ | `WHERE` 조건 |
| **표현식 인덱스** | ❌ | `EXTRACT()` 등 |

---

## 6. HikariCP 연결 풀 최적화

### 6.1 설정 가이드

```yaml
spring:
  datasource:
    hikari:
      # 최대 연결 수
      maximum-pool-size: 20  # CPU 코어 수 × 2 + 디스크 수

      # 최소 유휴 연결 수
      minimum-idle: 10  # maximum-pool-size의 50%

      # 연결 타임아웃 (ms)
      connection-timeout: 30000  # 30초

      # 유휴 연결 타임아웃 (ms)
      idle-timeout: 600000  # 10분

      # 연결 최대 수명 (ms)
      max-lifetime: 1800000  # 30분 (PostgreSQL wait_timeout보다 작게)

      # 연결 풀 이름
      pool-name: ShoppingHikariPool

      # 연결 테스트 쿼리
      connection-test-query: SELECT 1

      # 자동 커밋
      auto-commit: true

      # 누수 감지 임계값 (ms)
      leak-detection-threshold: 60000  # 60초

      # 연결 초기화 SQL
      connection-init-sql: SET TIME ZONE 'Asia/Seoul'
```

### 6.2 연결 풀 크기 계산

**공식:**

```
connections = ((core_count * 2) + effective_spindle_count)

예시:
- CPU 코어: 4개
- HDD/SSD: 1개
- connections = (4 * 2) + 1 = 9 ≈ 10개
```

**환경별 권장 설정:**

| 환경 | CPU 코어 | 권장 maximum-pool-size |
|------|---------|----------------------|
| 로컬 개발 | 4 | 10 |
| Docker | 2 | 5~10 |
| Kubernetes (Pod) | 2 | 10~15 |
| 프로덕션 (서버) | 8 | 20~30 |

### 6.3 PostgreSQL 서버 설정

```sql
-- postgresql.conf

# 최대 연결 수
max_connections = 100

# 각 애플리케이션 인스턴스의 maximum-pool-size 합계보다 크게 설정
# 예: 애플리케이션 인스턴스 3개 × pool-size 20 = 60
# max_connections = 100 (여유분 포함)

# 연결 타임아웃
statement_timeout = 30000  # 30초
idle_in_transaction_session_timeout = 60000  # 60초
```

### 6.4 연결 풀 모니터링

```java
@Component
@RequiredArgsConstructor
public class HikariMetrics {

    private final DataSource dataSource;

    @Scheduled(fixedRate = 60000)  // 1분마다
    public void logHikariStats() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

            log.info("HikariCP Stats: " +
                "Active={}, Idle={}, Total={}, Waiting={}",
                poolBean.getActiveConnections(),
                poolBean.getIdleConnections(),
                poolBean.getTotalConnections(),
                poolBean.getThreadsAwaitingConnection()
            );
        }
    }
}
```

---

## 7. JPA 설정 최적화

### 7.1 Batch Insert/Update

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20  # 배치 크기
        order_inserts: true  # INSERT 정렬
        order_updates: true  # UPDATE 정렬
```

```java
// 배치 Insert 예시
@Transactional
public void bulkInsertProducts(List<Product> products) {
    int batchSize = 20;
    for (int i = 0; i < products.size(); i++) {
        productRepository.save(products.get(i));

        if (i % batchSize == 0 && i > 0) {
            entityManager.flush();
            entityManager.clear();
        }
    }
}
```

### 7.2 N+1 문제 해결

```java
// ❌ N+1 문제 발생
public List<OrderResponse> getOrders(Long userId) {
    List<Order> orders = orderRepository.findByUserId(userId);
    return orders.stream()
        .map(order -> {
            List<OrderItem> items = order.getOrderItems();  // 각 Order마다 쿼리 발생
            return OrderResponse.from(order, items);
        })
        .collect(Collectors.toList());
}

// ✅ Fetch Join으로 해결
@Query("SELECT o FROM Order o " +
       "JOIN FETCH o.orderItems " +
       "WHERE o.userId = :userId")
List<Order> findByUserIdWithItems(@Param("userId") Long userId);

// ✅ Entity Graph로 해결
@EntityGraph(attributePaths = {"orderItems"})
List<Order> findByUserId(Long userId);
```

### 7.3 OSIV (Open Session In View) 비활성화

```yaml
spring:
  jpa:
    open-in-view: false  # 성능 최적화
```

```java
// OSIV 비활성화 시 DTO로 변환
@Transactional(readOnly = true)
public OrderResponse getOrder(Long orderId) {
    Order order = orderRepository.findByIdWithItems(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    // 트랜잭션 내에서 DTO 변환 (Lazy Loading 가능)
    return OrderResponse.from(order);
}
```

---

## 8. Portal Universe Shopping Service 전체 예시

### 8.1 Application.java

```java
package com.portal.universe.shoppingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // @CreatedDate, @LastModifiedDate 활성화
public class ShoppingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingServiceApplication.class, args);
    }
}
```

### 8.2 OrderService 구현

```java
package com.portal.universe.shoppingservice.order.service;

import com.portal.universe.shoppingservice.order.dto.OrderRequest;
import com.portal.universe.shoppingservice.order.dto.OrderResponse;
import com.portal.universe.shoppingservice.order.entity.Order;
import com.portal.universe.shoppingservice.order.entity.OrderItem;
import com.portal.universe.shoppingservice.order.entity.OrderStatus;
import com.portal.universe.shoppingservice.order.repository.OrderRepository;
import com.portal.universe.shoppingservice.product.entity.Product;
import com.portal.universe.shoppingservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 재고 확인 및 차감 (비관적 락)
        List<Product> products = productRepository
            .findAllByIdInForUpdate(request.getProductIds());

        for (OrderItemRequest item : request.getItems()) {
            Product product = products.stream()
                .filter(p -> p.getId().equals(item.getProductId()))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(item.getProductId()));

            product.decreaseStock(item.getQuantity());
        }

        // 2. 주문 생성
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(request.getUserId())
            .totalAmount(request.calculateTotalAmount())
            .status(OrderStatus.PENDING)
            .shippingAddress(request.getShippingAddressAsJson())
            .paymentMethod(request.getPaymentMethod())
            .build();

        order = orderRepository.save(order);

        // 3. 주문 아이템 생성
        List<OrderItem> orderItems = createOrderItems(order, request.getItems());
        orderItemRepository.saveAll(orderItems);

        return OrderResponse.from(order, orderItems);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void processPayment(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        order.markAsPaid();
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private List<OrderItem> createOrderItems(Order order, List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> OrderItem.builder()
                .order(order)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build())
            .collect(Collectors.toList());
    }
}
```

---

## 9. 핵심 정리

| 항목 | MySQL | PostgreSQL |
|------|-------|------------|
| **Driver** | `mysql-connector-java` | `postgresql` |
| **Dialect** | `MySQL8Dialect` | `PostgreSQLDialect` |
| **ID 생성** | AUTO_INCREMENT | SERIAL / SEQUENCE |
| **JSON** | JSON | JSONB |
| **배열** | ❌ | TEXT[] 등 |
| **전문 검색** | FULLTEXT INDEX | GIN + tsvector |
| **자동 업데이트** | ON UPDATE | 트리거 필요 |
| **연결 풀** | HikariCP | HikariCP |

---

## 10. 실습 예제

### 실습 1: PostgreSQL 컨테이너 실행

```bash
docker run -d \
  --name postgres-shopping \
  -e POSTGRES_DB=shopping_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 실습 2: Flyway 마이그레이션 실행

```bash
./gradlew flywayMigrate
```

### 실습 3: 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

---

## 11. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL 트랜잭션](./postgresql-transactions.md)
- [PostgreSQL JSONB](./postgresql-jsonb.md)
- [PostgreSQL 마이그레이션](./postgresql-migration.md)

---

## 12. 참고 자료

- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Hibernate PostgreSQL Dialect](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#database-postgresql)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
