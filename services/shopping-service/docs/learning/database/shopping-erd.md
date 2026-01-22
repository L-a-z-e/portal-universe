# Shopping Service ERD

## 학습 목표
- 테이블 간 관계 이해
- 인덱스 전략 파악
- 데이터 무결성 규칙 학습

---

## 1. ERD 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SHOPPING SERVICE DATABASE                              │
└─────────────────────────────────────────────────────────────────────────────────┘

┌───────────────────┐                              ┌───────────────────────────┐
│     products      │                              │        coupons            │
├───────────────────┤                              ├───────────────────────────┤
│ PK id             │◄─────────────────┐           │ PK id                     │
│    name           │                  │           │ UK code                   │
│    description    │                  │           │    name                   │
│    price          │                  │           │    description            │
│    stock (legacy) │                  │           │    discount_type          │
│    created_at     │                  │           │    discount_value         │
│    updated_at     │                  │           │    minimum_order_amount   │
└───────────────────┘                  │           │    maximum_discount_amount│
         │                             │           │    total_quantity         │
         │ 1:1                         │           │    issued_quantity        │
         ▼                             │           │    status                 │
┌───────────────────┐                  │           │    starts_at              │
│    inventory      │                  │           │    expires_at             │
├───────────────────┤                  │           │    created_at             │
│ PK id             │                  │           └───────────────────────────┘
│ UK product_id  ───┘                  │                        │
│    available_qty  │                  │                        │ 1:N
│    reserved_qty   │                  │                        ▼
│    total_qty      │                  │           ┌───────────────────────────┐
│    version        │                  │           │      user_coupons         │
│    created_at     │                  │           ├───────────────────────────┤
│    updated_at     │                  │           │ PK id                     │
└───────────────────┘                  │           │    user_id                │
         │                             │           │ FK coupon_id              │
         │ 1:N                         │           │    status                 │
         ▼                             │           │    order_id               │
┌───────────────────┐                  │           │    issued_at              │
│  stock_movements  │                  │           │    used_at                │
├───────────────────┤                  │           │    expires_at             │
│ PK id             │                  │           │ UK (user_id, coupon_id)   │
│ FK inventory_id   │                  │           └───────────────────────────┘
│ FK product_id     │                  │                        │
│    movement_type  │                  │                        │
│    quantity       │                  │                        │
│    prev_available │                  │           ┌────────────┘
│    after_available│                  │           │
│    prev_reserved  │                  │           │
│    after_reserved │                  │           │
│    reference_type │                  │           │
│    reference_id   │                  │           │
│    reason         │                  │           │
│    performed_by   │                  │           │
│    created_at     │                  │           │
└───────────────────┘                  │           │
                                       │           │
                                       │           │
┌───────────────────┐                  │           │
│      carts        │                  │           │
├───────────────────┤                  │           │
│ PK id             │                  │           │
│    user_id        │                  │           │
│    status         │                  │           │
│    created_at     │                  │           │
│    updated_at     │                  │           │
└───────────────────┘                  │           │
         │                             │           │
         │ 1:N                         │           │
         ▼                             │           │
┌───────────────────┐                  │           │
│    cart_items     │                  │           │
├───────────────────┤                  │           │
│ PK id             │                  │           │
│ FK cart_id        │                  │           │
│    product_id  ───┼──────────────────┘           │
│    product_name   │  (스냅샷)                     │
│    price          │  (스냅샷)                     │
│    quantity       │                              │
│    added_at       │                              │
└───────────────────┘                              │
                                                   │
                                                   │
┌───────────────────────────┐                      │
│         orders            │◄─────────────────────┘
├───────────────────────────┤
│ PK id                     │
│ UK order_number           │
│    user_id                │
│    status                 │
│    total_amount           │
│    discount_amount        │
│    final_amount           │
│ FK applied_user_coupon_id │
│    receiver_name          │  ┐
│    receiver_phone         │  │ Embedded
│    zip_code               │  │ Address
│    address1               │  │
│    address2               │  ┘
│    cancel_reason          │
│    cancelled_at           │
│    created_at             │
│    updated_at             │
└───────────────────────────┘
         │
         │
    ┌────┴─────┬─────────────────┬─────────────────┐
    │ 1:N      │ 1:N             │ 1:1             │ 1:1
    ▼          ▼                 ▼                 ▼
┌────────────┐ ┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│order_items │ │   payments    │ │  deliveries   │ │  saga_states  │
├────────────┤ ├───────────────┤ ├───────────────┤ ├───────────────┤
│PK id       │ │PK id          │ │PK id          │ │PK id          │
│FK order_id │ │UK payment_num │ │UK tracking_num│ │UK saga_id     │
│  product_id│ │FK order_id    │ │FK order_id    │ │FK order_id    │
│  prod_name │ │   order_number│ │   order_number│ │   order_number│
│  price     │ │   user_id     │ │   status      │ │   current_step│
│  quantity  │ │   amount      │ │   carrier     │ │   status      │
│  subtotal  │ │   status      │ │   recv_name   │ │   completed_  │
└────────────┘ │   pay_method  │ │   recv_phone  │ │   steps       │
               │   pg_trans_id │ │   zip_code    │ │   last_error  │
               │   pg_response │ │   address1    │ │   comp_attempts│
               │   failure_rsn │ │   address2    │ │   started_at  │
               │   paid_at     │ │   est_delivery│ │   completed_at│
               │   refunded_at │ │   actual_deliv│ └───────────────┘
               │   created_at  │ │   created_at  │
               │   updated_at  │ │   updated_at  │
               └───────────────┘ └───────────────┘
                                          │
                                          │ 1:N
                                          ▼
                                 ┌────────────────────┐
                                 │delivery_histories  │
                                 ├────────────────────┤
                                 │PK id               │
                                 │FK delivery_id      │
                                 │   status           │
                                 │   location         │
                                 │   description      │
                                 │   created_at       │
                                 └────────────────────┘


┌────────────────────┐
│    time_deals      │
├────────────────────┤
│ PK id              │
│    name            │
│    description     │
│    status          │
│    starts_at       │
│    ends_at         │
│    created_at      │
│    updated_at      │
└────────────────────┘
         │
         │ 1:N
         ▼
┌────────────────────────────┐
│   time_deal_products       │
├────────────────────────────┤
│ PK id                      │
│ FK time_deal_id            │
│ FK product_id  ────────────┼─────► products
│    deal_price              │
│    deal_quantity           │
│    sold_quantity           │
│    max_per_user            │
└────────────────────────────┘
         │
         │ 1:N
         ▼
┌────────────────────────────┐
│  time_deal_purchases       │
├────────────────────────────┤
│ PK id                      │
│    user_id                 │
│ FK time_deal_product_id    │
│    quantity                │
│    purchase_price          │
│    order_id                │
│    purchased_at            │
└────────────────────────────┘
```

---

## 2. 테이블 상세

### 2.1 상품/재고 테이블

#### products

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 상품 ID |
| name | VARCHAR(255) | NOT NULL | 상품명 |
| description | TEXT | | 상품 설명 |
| price | DOUBLE | NOT NULL | 가격 |
| stock | INT | DEFAULT 0 | 재고 (legacy, inventory로 이관) |
| created_at | TIMESTAMP | | 생성 시간 |
| updated_at | TIMESTAMP | | 수정 시간 |

#### inventory

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 재고 ID |
| product_id | BIGINT | FK, UNIQUE | 상품 ID (1:1) |
| available_quantity | INT | DEFAULT 0 | 판매 가능 수량 |
| reserved_quantity | INT | DEFAULT 0 | 예약된 수량 |
| total_quantity | INT | DEFAULT 0 | 전체 수량 |
| version | BIGINT | DEFAULT 0 | 낙관적 잠금 |
| created_at | TIMESTAMP | | 생성 시간 |
| updated_at | TIMESTAMP | | 수정 시간 |

**관계:** `total_quantity = available_quantity + reserved_quantity`

#### stock_movements

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 이동 ID |
| inventory_id | BIGINT | FK | 재고 ID |
| product_id | BIGINT | FK | 상품 ID |
| movement_type | VARCHAR(20) | NOT NULL | INBOUND, RESERVE, RELEASE, DEDUCT, RETURN |
| quantity | INT | NOT NULL | 변동 수량 |
| previous_available | INT | | 이전 가용 수량 |
| after_available | INT | | 이후 가용 수량 |
| previous_reserved | INT | | 이전 예약 수량 |
| after_reserved | INT | | 이후 예약 수량 |
| reference_type | VARCHAR(50) | | ORDER, PAYMENT 등 |
| reference_id | VARCHAR(100) | | 참조 ID |
| reason | VARCHAR(500) | | 사유 |
| performed_by | VARCHAR(100) | | 수행자 |
| created_at | TIMESTAMP | | 생성 시간 |

### 2.2 주문 테이블

#### orders

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주문 ID |
| order_number | VARCHAR(30) | UNIQUE, NOT NULL | 주문번호 (ORD-...) |
| user_id | VARCHAR(100) | NOT NULL | 사용자 ID |
| status | VARCHAR(20) | DEFAULT 'PENDING' | 주문 상태 |
| total_amount | DECIMAL(12,2) | NOT NULL | 총액 |
| discount_amount | DECIMAL(12,2) | DEFAULT 0 | 할인 금액 |
| final_amount | DECIMAL(12,2) | | 최종 결제 금액 |
| applied_user_coupon_id | BIGINT | FK | 적용된 쿠폰 |
| receiver_name | VARCHAR(100) | | 수령인 |
| receiver_phone | VARCHAR(20) | | 연락처 |
| zip_code | VARCHAR(10) | | 우편번호 |
| address1 | VARCHAR(255) | | 기본 주소 |
| address2 | VARCHAR(255) | | 상세 주소 |
| cancel_reason | VARCHAR(500) | | 취소 사유 |
| cancelled_at | TIMESTAMP | | 취소 시간 |
| created_at | TIMESTAMP | | 생성 시간 |
| updated_at | TIMESTAMP | | 수정 시간 |

#### order_items

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 항목 ID |
| order_id | BIGINT | FK (CASCADE) | 주문 ID |
| product_id | BIGINT | NOT NULL | 상품 ID |
| product_name | VARCHAR(255) | NOT NULL | 상품명 (스냅샷) |
| price | DECIMAL(12,2) | NOT NULL | 단가 (스냅샷) |
| quantity | INT | NOT NULL | 수량 |
| subtotal | DECIMAL(12,2) | NOT NULL | 소계 |

### 2.3 결제/배송 테이블

#### payments

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 결제 ID |
| payment_number | VARCHAR(30) | UNIQUE, NOT NULL | 결제번호 |
| order_id | BIGINT | FK | 주문 ID |
| order_number | VARCHAR(30) | NOT NULL | 주문번호 |
| user_id | VARCHAR(100) | NOT NULL | 사용자 ID |
| amount | DECIMAL(12,2) | NOT NULL | 결제 금액 |
| status | VARCHAR(20) | DEFAULT 'PENDING' | 결제 상태 |
| payment_method | VARCHAR(30) | NOT NULL | 결제 수단 |
| pg_transaction_id | VARCHAR(100) | | PG 거래 ID |
| pg_response | TEXT | | PG 응답 |
| failure_reason | VARCHAR(500) | | 실패 사유 |
| paid_at | TIMESTAMP | | 결제 시간 |
| refunded_at | TIMESTAMP | | 환불 시간 |
| created_at | TIMESTAMP | | 생성 시간 |
| updated_at | TIMESTAMP | | 수정 시간 |

#### deliveries

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 배송 ID |
| tracking_number | VARCHAR(30) | UNIQUE, NOT NULL | 송장번호 |
| order_id | BIGINT | FK | 주문 ID |
| order_number | VARCHAR(30) | NOT NULL | 주문번호 |
| status | VARCHAR(20) | DEFAULT 'PREPARING' | 배송 상태 |
| carrier | VARCHAR(50) | | 택배사 |
| (배송지 필드) | | | Embedded Address |
| estimated_delivery_date | DATE | | 예정일 |
| actual_delivery_date | DATE | | 실제 배송일 |
| created_at | TIMESTAMP | | 생성 시간 |
| updated_at | TIMESTAMP | | 수정 시간 |

---

## 3. 인덱스 전략

### 3.1 Primary Key Indexes

모든 테이블의 `id` 컬럼에 자동 생성됩니다.

### 3.2 Unique Indexes

| 테이블 | 인덱스명 | 컬럼 | 목적 |
|--------|----------|------|------|
| orders | idx_order_number | order_number | 주문번호 유일성 |
| payments | idx_payment_number | payment_number | 결제번호 유일성 |
| coupons | (컬럼명) | code | 쿠폰 코드 유일성 |
| deliveries | idx_delivery_tracking_number | tracking_number | 송장번호 유일성 |
| user_coupons | uk_user_coupon | (user_id, coupon_id) | 중복 발급 방지 |
| inventory | idx_inventory_product_id | product_id | 1:1 관계 보장 |

### 3.3 검색 최적화 Indexes

| 테이블 | 인덱스명 | 컬럼 | 목적 |
|--------|----------|------|------|
| orders | idx_order_user_id | user_id | 사용자 주문 조회 |
| orders | idx_order_status | status | 상태별 필터링 |
| orders | idx_order_created_at | created_at | 날짜별 조회 |
| payments | idx_payment_order_id | order_id | 주문별 결제 |
| payments | idx_payment_status | status | 상태별 필터링 |
| payments | idx_payment_pg_transaction_id | pg_transaction_id | PG 중복 방지 |
| coupons | idx_coupons_status | status | 활성 쿠폰 조회 |
| coupons | idx_coupons_expires_at | expires_at | 만료 쿠폰 정리 |
| time_deals | idx_time_deals_starts_at | starts_at | 활성 타임딜 |

### 3.4 복합 Indexes

| 테이블 | 인덱스명 | 컬럼 | 목적 |
|--------|----------|------|------|
| carts | idx_cart_user_status | (user_id, status) | 활성 장바구니 조회 |
| stock_movements | idx_stock_movement_reference | (reference_type, reference_id) | 참조 추적 |
| time_deal_purchases | idx_tdp_user_product | (user_id, time_deal_product_id) | 구매 제한 검증 |

---

## 4. Foreign Key 관계

### 4.1 CASCADE DELETE

```sql
-- 주문 삭제 시 주문 항목 자동 삭제
ALTER TABLE order_items
ADD CONSTRAINT fk_order_item_order
FOREIGN KEY (order_id) REFERENCES orders(id)
ON DELETE CASCADE;

-- 장바구니 삭제 시 장바구니 항목 자동 삭제
ALTER TABLE cart_items
ADD CONSTRAINT fk_cart_item_cart
FOREIGN KEY (cart_id) REFERENCES carts(id)
ON DELETE CASCADE;

-- 배송 삭제 시 배송 이력 자동 삭제
ALTER TABLE delivery_histories
ADD CONSTRAINT fk_delivery_history_delivery
FOREIGN KEY (delivery_id) REFERENCES deliveries(id)
ON DELETE CASCADE;
```

### 4.2 NO ACTION (기본)

```sql
-- 결제는 주문 삭제 시 에러 (데이터 보존)
ALTER TABLE payments
ADD CONSTRAINT fk_payment_order
FOREIGN KEY (order_id) REFERENCES orders(id);

-- 배송은 주문 삭제 시 에러
ALTER TABLE deliveries
ADD CONSTRAINT fk_delivery_order
FOREIGN KEY (order_id) REFERENCES orders(id);
```

---

## 5. 데이터 무결성 규칙

### 5.1 비즈니스 규칙

```
1. 재고 일관성
   inventory.total_qty = available_qty + reserved_qty

2. 주문 금액 계산
   order.final_amount = total_amount - discount_amount
   order.final_amount >= 0

3. 쿠폰 발급 제한
   coupon.issued_qty <= coupon.total_qty
   user_coupon: 동일 (user_id, coupon_id) 1건만

4. 타임딜 수량 제한
   time_deal_product.sold_qty <= deal_qty
   user 구매량 <= max_per_user

5. 상태 전이 규칙
   Order: PENDING → CONFIRMED → PAID → SHIPPING → DELIVERED
   Payment: PENDING → PROCESSING → COMPLETED/FAILED
```

### 5.2 Application 레벨 검증

| 규칙 | 검증 위치 | 처리 |
|------|----------|------|
| 재고 부족 | InventoryService | InsufficientStockException |
| 쿠폰 만료 | CouponService | CouponExpiredException |
| 중복 결제 | PaymentService | DuplicatePaymentException |
| 상태 전이 위반 | Order.validateTransition() | IllegalStateException |

---

## 6. Migration 히스토리

| 버전 | 파일명 | 내용 |
|------|--------|------|
| V1 | V1__Create_products_table.sql | products 테이블 |
| V2 | V2__Create_inventory_tables.sql | inventory, stock_movements |
| V3 | V3__Create_cart_tables.sql | carts, cart_items |
| V4 | V4__Create_order_tables.sql | orders, order_items |
| V5 | V5__Create_payment_table.sql | payments |
| V6 | V6__Create_delivery_tables.sql | deliveries, delivery_histories |
| V7 | V7__Create_saga_state_table.sql | saga_states |
| V8 | V8__Migrate_inventory_data.sql | products.stock → inventory |
| V9 | V9__Create_coupon_tables.sql | coupons, user_coupons |
| V10 | V10__Create_timedeal_tables.sql | time_deals, time_deal_products |
| V11 | V11__Add_coupon_to_orders.sql | orders 쿠폰 컬럼 추가 |
| V12 | V12__Create_queue_tables.sql | 대기열 테이블 |
| V13 | V13__Alter_user_id_type.sql | user_id VARCHAR 타입 변경 |

---

## 7. 핵심 정리

| 항목 | 설명 |
|------|------|
| **총 테이블 수** | 13개 |
| **주요 관계** | Order-OrderItem (1:N CASCADE) |
| **동시성 제어** | inventory.version (Optimistic Lock) |
| **감사 추적** | stock_movements, delivery_histories |
| **스냅샷 패턴** | order_items, cart_items (가격/상품명 저장) |

---

## 다음 학습

- [JPA Entity 매핑](./jpa-entity-mapping.md)
- [JPA 쿼리 최적화](./jpa-query-optimization.md)
- [트랜잭션 관리](./transaction-management.md)
