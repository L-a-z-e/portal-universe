# PostgreSQL JSONB

**난이도:** ⭐⭐⭐

## 학습 목표
- JSONB 타입의 특성 및 JSON과의 차이점 이해
- JSONB 연산자 완전 정복 (`->`, `->>`, `@>`, `?`, `||`, `-`)
- GIN 인덱싱을 통한 JSONB 검색 최적화
- 하이브리드 데이터 모델링 (고정 컬럼 + JSONB 메타데이터)
- Spring Data JPA에서 JSONB 매핑 및 활용
- Portal Universe Shopping Service의 JSONB 활용 사례

---

## 1. JSONB 개요

### 1.1 JSON vs JSONB

| 특성 | JSON | JSONB |
|------|------|-------|
| **저장 형식** | 텍스트 | 바이너리 |
| **입력 속도** | 빠름 | 느림 (파싱 필요) |
| **조회 속도** | 느림 | 빠름 |
| **인덱싱** | ❌ | ✅ (GIN) |
| **저장 공간** | 크다 | 작다 (압축) |
| **키 순서 유지** | ✅ | ❌ (정렬됨) |
| **중복 키** | 허용 | 제거 (마지막 값 유지) |
| **권장 사용** | 로그 저장 | 검색, 조회 |

**결론:** 대부분의 경우 **JSONB 사용을 권장**합니다.

### 1.2 JSONB 사용 시나리오

| 상황 | 예시 | 이유 |
|------|------|------|
| **유연한 스키마** | 제품 사양 (모델별 상이) | 고정 컬럼으로 표현 어려움 |
| **중첩 구조** | 주문 배송 주소 | 계층적 데이터 자연스러움 |
| **추가 메타데이터** | 이벤트 로그, 설정 | 자주 변경되는 속성 |
| **다중 값** | 제품 옵션, 변형 | 배열 및 객체 지원 |

### 1.3 MySQL JSON vs PostgreSQL JSONB

| 기능 | MySQL JSON | PostgreSQL JSONB |
|------|-----------|------------------|
| 기본 타입 | JSON (텍스트) | JSONB (바이너리) |
| 인덱싱 | Generated Column + Index | GIN 인덱스 |
| 연산자 | `->`, `->>`, `JSON_EXTRACT()` | `->, ->, @>, ?, ||, -` 등 |
| 함수 | `JSON_*()` 함수군 | `jsonb_*()` 함수군 |
| 성능 | 중간 | 빠름 |
| 표준 SQL | ✅ | PostgreSQL 전용 |

---

## 2. JSONB 연산자 완전 정복

### 2.1 추출 연산자 (`->`, `->>`)

**`->` : JSON 객체/배열 반환 (JSONB 타입)**

```sql
-- products 테이블
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    metadata JSONB
);

INSERT INTO products (name, metadata) VALUES
('iPhone 15 Pro', '{
    "brand": "Apple",
    "specs": {
        "cpu": "A17 Pro",
        "ram": "8GB",
        "storage": ["128GB", "256GB", "512GB", "1TB"]
    },
    "colors": ["Natural Titanium", "Blue Titanium", "White Titanium", "Black Titanium"],
    "price": {
        "base": 1550000,
        "currency": "KRW"
    }
}');

-- 1단계 추출 (JSONB 객체 반환)
SELECT metadata -> 'brand' FROM products;
-- 결과: "Apple" (JSONB 타입, 따옴표 포함)

SELECT metadata -> 'specs' FROM products;
-- 결과: {"cpu": "A17 Pro", "ram": "8GB", "storage": [...]}

-- 2단계 추출 (중첩)
SELECT metadata -> 'specs' -> 'cpu' FROM products;
-- 결과: "A17 Pro"

SELECT metadata -> 'price' -> 'base' FROM products;
-- 결과: 1550000
```

**`->>` : 텍스트 반환 (TEXT 타입)**

```sql
-- 텍스트로 추출 (따옴표 제거)
SELECT metadata ->> 'brand' FROM products;
-- 결과: Apple (TEXT 타입)

-- 중첩 추출 (-> + ->>)
SELECT metadata -> 'specs' ->> 'cpu' FROM products;
-- 결과: A17 Pro (TEXT)

-- WHERE 절에서 사용
SELECT name FROM products
WHERE metadata ->> 'brand' = 'Apple';

-- 숫자 비교 (CAST 필요)
SELECT name FROM products
WHERE (metadata -> 'price' ->> 'base')::BIGINT > 1000000;
```

### 2.2 배열 인덱싱 (`->`, `->>`)

```sql
-- 배열 요소 접근 (0-based index)
SELECT metadata -> 'colors' -> 0 FROM products;
-- 결과: "Natural Titanium"

SELECT metadata -> 'specs' -> 'storage' -> 2 FROM products;
-- 결과: "512GB"

-- 텍스트로 추출
SELECT metadata -> 'colors' ->> 1 FROM products;
-- 결과: Blue Titanium
```

### 2.3 포함 연산자 (`@>`, `<@`)

**`@>` : 왼쪽이 오른쪽을 포함**

```sql
-- 특정 키-값 쌍 포함 여부
SELECT * FROM products
WHERE metadata @> '{"brand": "Apple"}';

-- 중첩 객체 포함
SELECT * FROM products
WHERE metadata @> '{"specs": {"cpu": "A17 Pro"}}';

-- 배열 요소 포함
SELECT * FROM products
WHERE metadata -> 'colors' @> '["Blue Titanium"]';

-- 복합 조건
SELECT * FROM products
WHERE metadata @> '{"brand": "Apple", "price": {"currency": "KRW"}}';
```

**`<@` : 오른쪽이 왼쪽을 포함 (반대)**

```sql
SELECT * FROM products
WHERE '{"brand": "Apple"}' <@ metadata;
-- metadata가 {"brand": "Apple"}을 포함하는지
```

### 2.4 키 존재 연산자 (`?`, `?|`, `?&`)

**`?` : 키 존재 여부**

```sql
-- 특정 키 존재
SELECT * FROM products WHERE metadata ? 'brand';

-- 중첩 키 (-> 조합)
SELECT * FROM products
WHERE metadata -> 'specs' ? 'ram';
```

**`?|` : 여러 키 중 하나라도 존재 (OR)**

```sql
SELECT * FROM products
WHERE metadata ?| ARRAY['brand', 'manufacturer'];
-- brand 또는 manufacturer 키 존재
```

**`?&` : 모든 키 존재 (AND)**

```sql
SELECT * FROM products
WHERE metadata ?& ARRAY['brand', 'specs', 'colors'];
-- brand AND specs AND colors 모두 존재
```

### 2.5 연결 연산자 (`||`)

**JSONB 병합**

```sql
-- 키-값 추가
UPDATE products
SET metadata = metadata || '{"warranty": "1 year"}'
WHERE id = 1;

-- 중첩 객체 병합
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{specs}',
    metadata -> 'specs' || '{"bluetooth": "5.3"}'
)
WHERE id = 1;

-- 배열 추가
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{colors}',
    (metadata -> 'colors') || '["Red Titanium"]'::jsonb
)
WHERE id = 1;
```

### 2.6 삭제 연산자 (`-`)

**키 삭제**

```sql
-- 최상위 키 삭제
UPDATE products
SET metadata = metadata - 'warranty'
WHERE id = 1;

-- 여러 키 삭제
UPDATE products
SET metadata = metadata - ARRAY['key1', 'key2'];

-- 배열 요소 삭제 (인덱스)
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{colors}',
    (metadata -> 'colors') - 0  -- 첫 번째 요소 삭제
)
WHERE id = 1;
```

**경로 삭제 (`#-`)**

```sql
-- 중첩 키 삭제
UPDATE products
SET metadata = metadata #- '{specs, ram}'
WHERE id = 1;
-- specs.ram 삭제
```

---

## 3. JSONB 함수

### 3.1 주요 함수

| 함수 | 설명 | 예시 |
|------|------|------|
| `jsonb_set()` | 값 설정/수정 | `jsonb_set(data, '{key}', '"value"')` |
| `jsonb_insert()` | 값 삽입 | `jsonb_insert(data, '{arr, 0}', '"new"')` |
| `jsonb_object_keys()` | 키 목록 반환 | `SELECT jsonb_object_keys(metadata)` |
| `jsonb_each()` | 키-값 쌍 행으로 반환 | `SELECT * FROM jsonb_each(metadata)` |
| `jsonb_array_elements()` | 배열 요소 행으로 반환 | `SELECT * FROM jsonb_array_elements(arr)` |
| `jsonb_array_length()` | 배열 길이 | `jsonb_array_length(metadata -> 'colors')` |
| `jsonb_typeof()` | 타입 확인 | `jsonb_typeof(metadata -> 'price')` |
| `jsonb_pretty()` | 포맷된 출력 | `jsonb_pretty(metadata)` |

### 3.2 jsonb_set() 사용법

```sql
-- 단순 값 설정
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{brand}',
    '"Samsung"',
    true  -- create_if_missing (기본값: true)
)
WHERE id = 1;

-- 중첩 경로 설정
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{specs, cpu}',
    '"Snapdragon 8 Gen 3"'
)
WHERE id = 1;

-- 배열 요소 수정
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{colors, 0}',
    '"Phantom Black"'
)
WHERE id = 1;

-- 숫자 설정
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{price, base}',
    '1400000'  -- 문자열로 전달
)
WHERE id = 1;
```

### 3.3 jsonb_array_elements() 활용

```sql
-- 배열 요소를 행으로 변환
SELECT
    p.name,
    color.value AS color
FROM products p,
     jsonb_array_elements_text(p.metadata -> 'colors') AS color(value);

-- 결과:
-- | name          | color              |
-- |---------------|--------------------|
-- | iPhone 15 Pro | Natural Titanium   |
-- | iPhone 15 Pro | Blue Titanium      |
-- | iPhone 15 Pro | White Titanium     |
-- | iPhone 15 Pro | Black Titanium     |

-- 중첩 배열 처리
SELECT
    p.name,
    storage.value AS storage_option
FROM products p,
     jsonb_array_elements_text(p.metadata -> 'specs' -> 'storage') AS storage(value);
```

---

## 4. GIN 인덱싱

### 4.1 GIN 인덱스 생성

```sql
-- 기본 GIN 인덱스
CREATE INDEX idx_products_metadata_gin ON products USING GIN(metadata);

-- 특정 경로 GIN 인덱스
CREATE INDEX idx_products_specs_gin ON products USING GIN((metadata -> 'specs'));

-- jsonb_path_ops (더 작고 빠름, @> 전용)
CREATE INDEX idx_products_metadata_path_ops
ON products USING GIN(metadata jsonb_path_ops);
```

### 4.2 GIN vs GIN (jsonb_path_ops)

| 특성 | GIN | GIN (jsonb_path_ops) |
|------|-----|---------------------|
| 인덱스 크기 | 크다 | 작다 (30% 감소) |
| 지원 연산자 | `@>`, `?`, `?|`, `?&` | `@>` 만 |
| 검색 속도 | 보통 | 빠름 |
| 권장 사용 | 다양한 검색 | `@>` 전용 |

### 4.3 인덱스 활용 쿼리

```sql
-- ✅ 인덱스 사용
EXPLAIN ANALYZE
SELECT * FROM products
WHERE metadata @> '{"brand": "Apple"}';
-- Index Scan using idx_products_metadata_gin

-- ✅ 인덱스 사용
SELECT * FROM products WHERE metadata ? 'brand';

-- ❌ 인덱스 미사용 (->>, 함수 사용)
SELECT * FROM products WHERE metadata ->> 'brand' = 'Apple';
-- Seq Scan (전체 테이블 스캔)

-- ✅ 개선: @> 사용
SELECT * FROM products WHERE metadata @> '{"brand": "Apple"}';
```

### 4.4 부분 GIN 인덱스

```sql
-- NULL이 아닌 metadata만 인덱싱
CREATE INDEX idx_products_metadata_gin
ON products USING GIN(metadata)
WHERE metadata IS NOT NULL;

-- 특정 조건 JSONB만 인덱싱
CREATE INDEX idx_products_apple_metadata
ON products USING GIN(metadata)
WHERE metadata @> '{"brand": "Apple"}';
```

---

## 5. 하이브리드 모델링

### 5.1 설계 원칙

**고정 컬럼 vs JSONB**

| 데이터 특성 | 권장 방식 | 예시 |
|------------|----------|------|
| 필수 필드 | 고정 컬럼 | id, name, price |
| 자주 검색 | 고정 컬럼 | category_id, brand_id |
| 자주 변경되는 속성 | JSONB | specs, options |
| 모델별 상이한 속성 | JSONB | 노트북 vs 의류 |
| 중첩 구조 | JSONB | 배송 주소, 이벤트 로그 |

### 5.2 Products 테이블 설계

```sql
CREATE TABLE products (
    -- 고정 컬럼 (핵심 필드)
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,

    -- JSONB 메타데이터 (유연한 속성)
    metadata JSONB,

    -- 검색 최적화 (Generated Column)
    brand_name TEXT GENERATED ALWAYS AS (metadata ->> 'brand') STORED,

    -- 관리 필드
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 고정 컬럼 인덱스
CREATE INDEX idx_products_category_price
ON products(category_id, price DESC)
WHERE is_deleted = FALSE;

-- JSONB GIN 인덱스
CREATE INDEX idx_products_metadata_gin
ON products USING GIN(metadata)
WHERE metadata IS NOT NULL;

-- Generated Column 인덱스
CREATE INDEX idx_products_brand_name
ON products(brand_name)
WHERE is_deleted = FALSE;
```

**metadata 구조:**

```json
{
  "brand": "Apple",
  "model": "iPhone 15 Pro",
  "specs": {
    "cpu": "A17 Pro",
    "ram": "8GB",
    "storage": ["128GB", "256GB", "512GB", "1TB"],
    "display": {
      "size": "6.1 inch",
      "type": "OLED",
      "resolution": "2556 × 1179"
    }
  },
  "colors": ["Natural Titanium", "Blue Titanium", "White Titanium", "Black Titanium"],
  "features": ["Dynamic Island", "Always-On Display", "ProMotion"],
  "warranty": "1 year",
  "release_date": "2023-09-22"
}
```

### 5.3 Orders 테이블 설계

```sql
CREATE TABLE orders (
    -- 고정 컬럼
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,

    -- JSONB 배송 주소
    shipping_address JSONB NOT NULL,

    -- 검색 최적화 (Generated Column)
    shipping_city TEXT GENERATED ALWAYS AS (shipping_address ->> 'city') STORED,
    shipping_postal_code TEXT GENERATED ALWAYS AS (shipping_address ->> 'postal_code') STORED,

    -- 관리 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- GIN 인덱스
CREATE INDEX idx_orders_shipping_address_gin
ON orders USING GIN(shipping_address);

-- Generated Column 인덱스
CREATE INDEX idx_orders_shipping_city
ON orders(shipping_city);
```

**shipping_address 구조:**

```json
{
  "recipient": "홍길동",
  "phone": "010-1234-5678",
  "postal_code": "06234",
  "city": "서울특별시",
  "district": "강남구",
  "address": "테헤란로 123",
  "detail": "ABC빌딩 4층",
  "message": "부재 시 경비실에 맡겨주세요"
}
```

---

## 6. Spring Data JPA JSONB 매핑

### 6.1 Gradle 의존성

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.postgresql:postgresql'
    implementation 'com.vladmihalcea:hibernate-types-60:2.21.1'  // JSONB 지원
}
```

### 6.2 Product Entity

```java
package com.portal.universe.shoppingservice.product.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "brand_id")
    private Long brandId;

    // 방법 1: String으로 저장 (간단)
    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;

    // 방법 2: JsonNode 사용 (Jackson)
    // @Type(JsonBinaryType.class)
    // @Column(columnDefinition = "jsonb")
    // private JsonNode metadata;

    // 방법 3: DTO 클래스 사용
    // @Type(JsonBinaryType.class)
    // @Column(columnDefinition = "jsonb")
    // private ProductMetadata metadata;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 메타데이터 접근 메서드
    public String getMetadataValue(String key) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(metadata);
            JsonNode value = node.get(key);
            return value != null ? value.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void setMetadataValue(String key, Object value) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = metadata != null
                ? mapper.readTree(metadata)
                : mapper.createObjectNode();

            ((com.fasterxml.jackson.databind.node.ObjectNode) node)
                .putPOJO(key, value);

            this.metadata = mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set metadata", e);
        }
    }
}
```

### 6.3 ProductMetadata DTO (타입 안전성)

```java
package com.portal.universe.shoppingservice.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductMetadata implements Serializable {

    private String brand;
    private String model;
    private ProductSpecs specs;
    private List<String> colors;
    private List<String> features;
    private String warranty;
    private LocalDate releaseDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductSpecs implements Serializable {
        private String cpu;
        private String ram;
        private List<String> storage;
        private Display display;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Display implements Serializable {
            private String size;
            private String type;
            private String resolution;
        }
    }
}
```

**Entity에서 사용:**

```java
@Entity
public class Product {
    // ...

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private ProductMetadata metadata;

    // 타입 안전하게 접근
    public String getBrand() {
        return metadata != null ? metadata.getBrand() : null;
    }

    public void setBrand(String brand) {
        if (this.metadata == null) {
            this.metadata = new ProductMetadata();
        }
        this.metadata.setBrand(brand);
    }
}
```

### 6.4 Order Entity (ShippingAddress)

```java
package com.portal.universe.shoppingservice.order.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // JSONB 배송 주소
    @Type(JsonBinaryType.class)
    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private ShippingAddress shippingAddress;

    // ...
}
```

**ShippingAddress DTO:**

```java
package com.portal.universe.shoppingservice.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingAddress implements Serializable {

    private String recipient;
    private String phone;

    @JsonProperty("postal_code")
    private String postalCode;

    private String city;
    private String district;
    private String address;
    private String detail;
    private String message;
}
```

---

## 7. Repository 쿼리

### 7.1 JPQL + Native Query

```java
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 네이티브 쿼리: JSONB @> 연산자
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata @> :jsonQuery::jsonb " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByMetadata(@Param("jsonQuery") String jsonQuery);

    /**
     * 브랜드 검색
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata @> jsonb_build_object('brand', :brand) " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByBrand(@Param("brand") String brand);

    /**
     * 중첩 검색 (specs.cpu)
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata -> 'specs' ->> 'cpu' = :cpu " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByCpu(@Param("cpu") String cpu);

    /**
     * 배열 포함 검색 (colors)
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata -> 'colors' @> to_jsonb(:color::text) " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByColor(@Param("color") String color);

    /**
     * 키 존재 검색
     */
    @Query(value =
        "SELECT * FROM products " +
        "WHERE metadata ? :key " +
        "AND is_deleted = false",
        nativeQuery = true)
    List<Product> findByMetadataKeyExists(@Param("key") String key);
}
```

### 7.2 OrderRepository

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 도시별 주문 조회
     */
    @Query(value =
        "SELECT * FROM orders " +
        "WHERE shipping_address ->> 'city' = :city",
        nativeQuery = true)
    List<Order> findByShippingCity(@Param("city") String city);

    /**
     * 우편번호 검색
     */
    @Query(value =
        "SELECT * FROM orders " +
        "WHERE shipping_address ->> 'postal_code' LIKE :postalCode%",
        nativeQuery = true)
    List<Order> findByPostalCodeStartsWith(@Param("postalCode") String postalCode);

    /**
     * 복합 검색 (도시 + 구)
     */
    @Query(value =
        "SELECT * FROM orders " +
        "WHERE shipping_address @> " +
        "jsonb_build_object('city', :city, 'district', :district)",
        nativeQuery = true)
    List<Order> findByShippingCityAndDistrict(
        @Param("city") String city,
        @Param("district") String district
    );
}
```

---

## 8. Service 레이어 활용

### 8.1 ProductService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<ProductResponse> searchByBrand(String brand) {
        // 방법 1: 네이티브 쿼리
        List<Product> products = productRepository.findByBrand(brand);

        // 방법 2: JSON 문자열 구성
        String jsonQuery = String.format("{\"brand\": \"%s\"}", brand);
        products = productRepository.findByMetadata(jsonQuery);

        return products.stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchBySpecs(String cpu, String ram) {
        String jsonQuery = String.format(
            "{\"specs\": {\"cpu\": \"%s\", \"ram\": \"%s\"}}",
            cpu, ram
        );
        List<Product> products = productRepository.findByMetadata(jsonQuery);

        return products.stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductResponse updateMetadata(Long productId, Map<String, Object> updates) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        try {
            // 기존 메타데이터 로드
            JsonNode metadata = product.getMetadata() != null
                ? objectMapper.readTree(product.getMetadata())
                : objectMapper.createObjectNode();

            // 업데이트 병합
            ObjectNode objectNode = (ObjectNode) metadata;
            updates.forEach((key, value) -> objectNode.putPOJO(key, value));

            // 저장
            product.setMetadata(objectMapper.writeValueAsString(objectNode));
            productRepository.save(product);

            return ProductResponse.from(product);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update metadata", e);
        }
    }
}
```

### 8.2 OrderService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // ShippingAddress DTO 생성
        ShippingAddress shippingAddress = ShippingAddress.builder()
            .recipient(request.getRecipient())
            .phone(request.getPhone())
            .postalCode(request.getPostalCode())
            .city(request.getCity())
            .district(request.getDistrict())
            .address(request.getAddress())
            .detail(request.getDetail())
            .message(request.getMessage())
            .build();

        // Order 생성 (JSONB 자동 변환)
        Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(request.getUserId())
            .totalAmount(request.getTotalAmount())
            .status(OrderStatus.PENDING)
            .shippingAddress(shippingAddress)  // DTO → JSONB 자동 변환
            .build();

        order = orderRepository.save(order);

        return OrderResponse.from(order);
    }

    @Override
    public List<OrderResponse> findByCity(String city) {
        List<Order> orders = orderRepository.findByShippingCity(city);

        return orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }
}
```

---

## 9. 성능 최적화

### 9.1 인덱스 전략

```sql
-- ✅ 좋은 예: GIN 인덱스 + @> 연산자
CREATE INDEX idx_products_metadata_gin ON products USING GIN(metadata);

SELECT * FROM products
WHERE metadata @> '{"brand": "Apple"}';
-- Index Scan using idx_products_metadata_gin

-- ❌ 나쁜 예: ->> 연산자 (인덱스 미사용)
SELECT * FROM products
WHERE metadata ->> 'brand' = 'Apple';
-- Seq Scan

-- ✅ 개선: Generated Column + B-Tree 인덱스
ALTER TABLE products
ADD COLUMN brand_name TEXT GENERATED ALWAYS AS (metadata ->> 'brand') STORED;

CREATE INDEX idx_products_brand_name ON products(brand_name);

SELECT * FROM products WHERE brand_name = 'Apple';
-- Index Scan using idx_products_brand_name
```

### 9.2 부분 인덱스

```sql
-- 특정 브랜드만 인덱싱
CREATE INDEX idx_products_apple_specs
ON products USING GIN((metadata -> 'specs'))
WHERE metadata @> '{"brand": "Apple"}';

-- NULL이 아닌 메타데이터만 인덱싱
CREATE INDEX idx_products_metadata_not_null
ON products USING GIN(metadata)
WHERE metadata IS NOT NULL;
```

### 9.3 쿼리 최적화

```sql
-- ❌ 느린 쿼리: 함수 사용
SELECT * FROM products
WHERE jsonb_array_length(metadata -> 'colors') > 3;

-- ✅ 빠른 쿼리: 부분 인덱스 활용
CREATE INDEX idx_products_many_colors
ON products USING GIN((metadata -> 'colors'))
WHERE jsonb_array_length(metadata -> 'colors') > 3;

SELECT * FROM products
WHERE metadata -> 'colors' @> '["Blue"]'
  AND jsonb_array_length(metadata -> 'colors') > 3;
```

---

## 10. 핵심 정리

| 연산자/함수 | 사용 | 반환 타입 | 예시 |
|------------|------|----------|------|
| `->` | 키/인덱스 추출 | JSONB | `data -> 'key'` |
| `->>` | 키/인덱스 추출 | TEXT | `data ->> 'key'` |
| `@>` | 포함 여부 | BOOLEAN | `data @> '{"key": "val"}'` |
| `?` | 키 존재 | BOOLEAN | `data ? 'key'` |
| `||` | 병합 | JSONB | `data || '{"new": "val"}'` |
| `-` | 삭제 | JSONB | `data - 'key'` |
| `jsonb_set()` | 값 설정 | JSONB | `jsonb_set(data, '{path}', 'val')` |
| `jsonb_array_elements()` | 배열→행 | SETOF JSONB | `SELECT * FROM jsonb_array_elements(arr)` |

---

## 11. 실습 예제

### 실습 1: 제품 검색

```sql
-- 브랜드 검색
SELECT name, metadata ->> 'brand' AS brand
FROM products
WHERE metadata @> '{"brand": "Apple"}';

-- CPU 검색
SELECT name, metadata -> 'specs' ->> 'cpu' AS cpu
FROM products
WHERE metadata -> 'specs' @> '{"cpu": "A17 Pro"}';

-- 색상 검색
SELECT name, metadata -> 'colors' AS colors
FROM products
WHERE metadata -> 'colors' @> '["Blue Titanium"]';
```

### 실습 2: 메타데이터 업데이트

```sql
-- 보증 기간 추가
UPDATE products
SET metadata = metadata || '{"warranty": "2 years"}'
WHERE id = 1;

-- 색상 추가
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{colors}',
    (metadata -> 'colors') || '["Red Titanium"]'::jsonb
)
WHERE id = 1;

-- CPU 정보 수정
UPDATE products
SET metadata = jsonb_set(
    metadata,
    '{specs, cpu}',
    '"A18 Pro"'
)
WHERE id = 1;
```

---

## 12. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL Spring 통합](./postgresql-spring-integration.md)
- [PostgreSQL 고급 기능](./postgresql-advanced-features.md)

---

## 13. 참고 자료

- [PostgreSQL JSON Documentation](https://www.postgresql.org/docs/current/datatype-json.html)
- [PostgreSQL JSON Functions](https://www.postgresql.org/docs/current/functions-json.html)
- [Hibernate Types Library](https://github.com/vladmihalcea/hypersistence-utils)
- [JSON vs JSONB](https://www.postgresql.org/docs/current/datatype-json.html#JSON-INDEXING)
