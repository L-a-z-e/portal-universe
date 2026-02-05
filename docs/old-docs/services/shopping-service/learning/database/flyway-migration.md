# Flyway Database Migration

## 개요

Flyway는 데이터베이스 스키마 버전 관리 도구입니다. shopping-service에서는 Flyway를 사용하여 MySQL 스키마를 버전 관리하고, 팀원 간 일관된 데이터베이스 상태를 유지합니다.

---

## 1. Flyway 기본 개념

### 1.1 버전 관리 방식

```
V1__Initial_products_schema.sql      # 버전 1
V2__Create_inventory_tables.sql       # 버전 2
V3__Create_cart_tables.sql            # 버전 3
V4__Create_order_tables.sql           # 버전 4
...
V13__Alter_user_id_to_varchar.sql     # 버전 13
```

### 1.2 파일 네이밍 규칙

```
V{버전}_{설명}.sql

예시:
V1__Initial_products_schema.sql
│└─ 설명 (언더스코어 2개로 구분)
└── 버전 번호
```

### 1.3 Flyway 테이블

Flyway는 `flyway_schema_history` 테이블에 마이그레이션 이력을 기록합니다:

```sql
SELECT version, description, script, installed_on, success
FROM flyway_schema_history;

-- 결과
| version | description                    | script                               | installed_on        | success |
|---------|--------------------------------|--------------------------------------|---------------------|---------|
| 1       | Initial products schema        | V1__Initial_products_schema.sql      | 2024-01-15 10:00:00 | 1       |
| 2       | Create inventory tables        | V2__Create_inventory_tables.sql      | 2024-01-15 10:00:01 | 1       |
| 3       | Create cart tables             | V3__Create_cart_tables.sql           | 2024-01-16 09:30:00 | 1       |
```

---

## 2. shopping-service 마이그레이션 구조

### 2.1 파일 위치

```
services/shopping-service/
└── src/main/resources/
    └── db/migration/
        ├── V1__Initial_products_schema.sql
        ├── V2__Create_inventory_tables.sql
        ├── V3__Create_cart_tables.sql
        ├── V4__Create_order_tables.sql
        ├── V5__Create_payment_tables.sql
        ├── V6__Create_delivery_tables.sql
        ├── V7__Create_saga_tables.sql
        ├── V8__Migrate_stock_to_inventory.sql
        ├── V9__Create_coupon_tables.sql
        ├── V10__Create_timedeal_tables.sql
        ├── V11__Add_coupon_columns_to_orders.sql
        ├── V12__Create_queue_tables.sql
        └── V13__Alter_user_id_to_varchar.sql
```

### 2.2 실제 마이그레이션 예시

#### V1: 상품 테이블

```sql
-- V1__Initial_products_schema.sql
-- products 테이블 스키마

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_products_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### V2: 재고 테이블

```sql
-- V2__Create_inventory_tables.sql
-- 재고 관리 테이블

-- 재고 테이블
CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL UNIQUE,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    total_quantity INT NOT NULL DEFAULT 0,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE UNIQUE INDEX idx_inventory_product_id ON inventory(product_id);

-- 재고 이동 이력 테이블
CREATE TABLE stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    previous_available INT NOT NULL,
    after_available INT NOT NULL,
    previous_reserved INT NOT NULL,
    after_reserved INT NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    reason VARCHAR(500),
    performed_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_movement_inventory FOREIGN KEY (inventory_id) REFERENCES inventory(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_stock_movement_inventory_id ON stock_movements(inventory_id);
CREATE INDEX idx_stock_movement_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movement_reference ON stock_movements(reference_type, reference_id);
CREATE INDEX idx_stock_movement_created_at ON stock_movements(created_at);
```

#### V4: 주문 테이블

```sql
-- V4__Create_order_tables.sql
-- 주문 테이블

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(12,2) NOT NULL,

    -- 배송지 주소 (Embedded)
    receiver_name VARCHAR(100),
    receiver_phone VARCHAR(20),
    zip_code VARCHAR(10),
    address1 VARCHAR(255),
    address2 VARCHAR(255),

    cancel_reason VARCHAR(500),
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE UNIQUE INDEX idx_order_number ON orders(order_number);
CREATE INDEX idx_order_user_id ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_order_created_at ON orders(created_at);

-- 주문 항목 테이블
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,

    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_order_item_order_id ON order_items(order_id);
CREATE INDEX idx_order_item_product_id ON order_items(product_id);
```

---

## 3. 데이터 마이그레이션

### V8: 기존 데이터 이전

```sql
-- V8__Migrate_stock_to_inventory.sql
-- 기존 products 테이블의 stock을 inventory 테이블로 마이그레이션

-- 기존 상품의 재고를 inventory 테이블로 이전
INSERT INTO inventory (product_id, available_quantity, reserved_quantity, total_quantity, version, created_at)
SELECT id, stock, 0, stock, 0, NOW()
FROM products
WHERE id NOT IN (SELECT product_id FROM inventory);

-- 마이그레이션 완료 후 이력 기록
INSERT INTO stock_movements (
    inventory_id, product_id, movement_type, quantity,
    previous_available, after_available, previous_reserved, after_reserved,
    reference_type, reference_id, reason, performed_by, created_at
)
SELECT
    i.id, i.product_id, 'INITIAL', i.total_quantity,
    0, i.available_quantity, 0, 0,
    'MIGRATION', CONCAT('V8_', i.product_id), 'Initial migration from products.stock', 'SYSTEM', NOW()
FROM inventory i
WHERE NOT EXISTS (
    SELECT 1 FROM stock_movements sm
    WHERE sm.inventory_id = i.id AND sm.reference_type = 'MIGRATION'
);
```

---

## 4. 스키마 변경

### V13: 컬럼 타입 변경

```sql
-- V13__Alter_user_id_to_varchar.sql
-- user_id 컬럼을 BIGINT에서 VARCHAR(36)으로 변경
-- 목적: JWT subject로 UUID를 사용하므로 일관성 확보

-- 1. user_coupons 테이블
ALTER TABLE user_coupons
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';

-- 2. time_deal_purchases 테이블
ALTER TABLE time_deal_purchases
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';

-- 3. queue_entries 테이블
ALTER TABLE queue_entries
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';
```

---

## 5. Flyway 설정

### 5.1 application.yml

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true       # 기존 DB에 Flyway 적용 시
    locations: classpath:db/migration
    validate-on-migrate: true       # 마이그레이션 전 검증
```

### 5.2 환경별 설정

```yaml
# application-local.yml
spring:
  flyway:
    enabled: true
    clean-disabled: false  # 로컬에서만 clean 허용

# application-docker.yml
spring:
  flyway:
    enabled: true
    clean-disabled: true   # 운영에서 clean 비활성화
```

---

## 6. 마이그레이션 작성 가이드

### 6.1 새 테이블 생성

```sql
-- V{n}__Create_{테이블명}_table.sql

CREATE TABLE {테이블명} (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 필드들...
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_{테이블명}_{컬럼} ON {테이블명}({컬럼});
```

### 6.2 컬럼 추가

```sql
-- V{n}__Add_{컬럼명}_to_{테이블명}.sql

ALTER TABLE {테이블명}
    ADD COLUMN {컬럼명} {타입} {제약조건} AFTER {기존컬럼};

-- 기존 데이터 업데이트 (필요시)
UPDATE {테이블명} SET {컬럼명} = {기본값} WHERE {컬럼명} IS NULL;
```

### 6.3 컬럼 수정

```sql
-- V{n}__Alter_{컬럼명}_in_{테이블명}.sql

ALTER TABLE {테이블명}
    MODIFY COLUMN {컬럼명} {새타입} {제약조건};
```

### 6.4 인덱스 추가

```sql
-- V{n}__Add_index_to_{테이블명}.sql

CREATE INDEX idx_{테이블명}_{컬럼들} ON {테이블명}({컬럼1}, {컬럼2});
```

---

## 7. 베스트 프랙티스

### 7.1 Do's

1. **한 파일에 한 가지 변경**
   ```
   V5__Create_payment_tables.sql    (O)
   V5__Create_everything.sql        (X)
   ```

2. **의미 있는 이름 사용**
   ```
   V8__Migrate_stock_to_inventory.sql    (O)
   V8__Fix.sql                           (X)
   ```

3. **Idempotent 쿼리 사용**
   ```sql
   CREATE TABLE IF NOT EXISTS ...
   CREATE INDEX IF NOT EXISTS ...
   ```

4. **롤백 스크립트 준비**
   ```sql
   -- V{n}__Add_column.sql
   -- Rollback: ALTER TABLE ... DROP COLUMN ...
   ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(12,2);
   ```

### 7.2 Don'ts

1. **이미 적용된 마이그레이션 수정 금지**
   - 체크섬 불일치로 애플리케이션 시작 실패

2. **순서 건너뛰기 금지**
   ```
   V1, V2, V4 (X) - V3 누락
   V1, V2, V3 (O)
   ```

3. **DML만 있는 마이그레이션 주의**
   - 멱등성 보장 필요

---

## 8. 트러블슈팅

### 8.1 체크섬 불일치

```
Flyway detected that migration V1 has been applied with
checksum -123456789 but resolved locally with different checksum 987654321
```

해결:
```sql
-- flyway_schema_history 테이블의 체크섬 수정 (비권장)
UPDATE flyway_schema_history
SET checksum = {새체크섬}
WHERE version = '1';

-- 또는 Flyway repair 사용
flyway repair
```

### 8.2 마이그레이션 실패

```sql
-- 상태 확인
SELECT * FROM flyway_schema_history WHERE success = 0;

-- 실패한 마이그레이션 제거 후 재시도
DELETE FROM flyway_schema_history WHERE version = '{실패버전}';
```

### 8.3 Baseline 설정

기존 데이터베이스에 Flyway 처음 적용 시:
```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 0  # 기존 스키마 버전
```

---

## 9. CLI 명령어

```bash
# 마이그레이션 정보 확인
./mvnw flyway:info

# 마이그레이션 실행
./mvnw flyway:migrate

# 마이그레이션 검증
./mvnw flyway:validate

# 체크섬 복구
./mvnw flyway:repair

# 데이터베이스 초기화 (개발 환경만!)
./mvnw flyway:clean
```

---

## 관련 문서

- [JPA Entity Mapping](./jpa-entity-mapping.md) - 엔티티 설계
- [Database Indexing](./database-indexing.md) - 인덱스 전략
- [Transaction Management](./transaction-management.md) - 트랜잭션 관리
