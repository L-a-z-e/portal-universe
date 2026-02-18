-- ===================================================================
-- Shopping Service - Initial Schema (PostgreSQL)
-- Consolidated from V1__baseline.sql + V2__product_discount_images.sql
-- Generated: 2026-02-18
-- ===================================================================

-- ===================================================================
-- Trigger Function: updated_at 자동 갱신
-- ===================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ===================================================================
-- Tables
-- ===================================================================

-- 상품 (V2: discount_price, featured 컬럼 포함)
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    price           DECIMAL(19,2) NOT NULL,
    discount_price  DECIMAL(19,2) DEFAULT NULL,
    stock           INT NOT NULL DEFAULT 0,
    image_url       VARCHAR(500) DEFAULT NULL,
    category        VARCHAR(100) DEFAULT NULL,
    featured        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 상품 이미지 (V2)
CREATE TABLE IF NOT EXISTS product_images (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    product_id  BIGINT NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    alt_text    VARCHAR(255) DEFAULT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- 장바구니
CREATE TABLE IF NOT EXISTS carts (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id     VARCHAR(100) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 장바구니 항목
CREATE TABLE IF NOT EXISTS cart_items (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    cart_id         BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    price           DECIMAL(12,2) NOT NULL,
    quantity        INT NOT NULL,
    added_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE
);

-- 쿠폰
CREATE TABLE IF NOT EXISTS coupons (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    code                    VARCHAR(50) NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    description             TEXT,
    discount_type           VARCHAR(20) NOT NULL,
    discount_value          DECIMAL(10,2) NOT NULL,
    minimum_order_amount    DECIMAL(10,2) DEFAULT NULL,
    maximum_discount_amount DECIMAL(10,2) DEFAULT NULL,
    total_quantity          INT NOT NULL,
    issued_quantity         INT NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    starts_at               TIMESTAMP NOT NULL,
    expires_at              TIMESTAMP NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
);

-- 사용자 쿠폰
CREATE TABLE IF NOT EXISTS user_coupons (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id     VARCHAR(36) NOT NULL,
    coupon_id   BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    order_id    BIGINT DEFAULT NULL,
    issued_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at     TIMESTAMP DEFAULT NULL,
    expires_at  TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_coupon UNIQUE (user_id, coupon_id),
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
);

-- 주문
CREATE TABLE IF NOT EXISTS orders (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    order_number            VARCHAR(30) NOT NULL,
    user_id                 VARCHAR(100) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount            DECIMAL(12,2) NOT NULL,
    discount_amount         DECIMAL(12,2) DEFAULT 0.00,
    final_amount            DECIMAL(12,2) DEFAULT NULL,
    applied_user_coupon_id  BIGINT DEFAULT NULL,
    receiver_name           VARCHAR(100) DEFAULT NULL,
    receiver_phone          VARCHAR(20) DEFAULT NULL,
    zip_code                VARCHAR(10) DEFAULT NULL,
    address1                VARCHAR(255) DEFAULT NULL,
    address2                VARCHAR(255) DEFAULT NULL,
    cancel_reason           VARCHAR(500) DEFAULT NULL,
    cancelled_at            TIMESTAMP DEFAULT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT idx_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user_coupon FOREIGN KEY (applied_user_coupon_id) REFERENCES user_coupons (id)
);

-- 주문 항목
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    order_id        BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    price           DECIMAL(12,2) NOT NULL,
    quantity        INT NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

-- 결제
CREATE TABLE IF NOT EXISTS payments (
    id                  BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    payment_number      VARCHAR(30) NOT NULL,
    order_id            BIGINT NOT NULL,
    order_number        VARCHAR(30) NOT NULL,
    user_id             VARCHAR(100) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30) NOT NULL,
    pg_transaction_id   VARCHAR(100) DEFAULT NULL,
    pg_response         TEXT,
    failure_reason      VARCHAR(500) DEFAULT NULL,
    paid_at             TIMESTAMP DEFAULT NULL,
    refunded_at         TIMESTAMP DEFAULT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT idx_payment_number UNIQUE (payment_number),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- 재고
CREATE TABLE IF NOT EXISTS inventory (
    id                  BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    product_id          BIGINT NOT NULL,
    available_quantity  INT NOT NULL DEFAULT 0,
    reserved_quantity   INT NOT NULL DEFAULT 0,
    total_quantity      INT NOT NULL DEFAULT 0,
    version             BIGINT DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT idx_inventory_product_id UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- 재고 이동 이력
CREATE TABLE IF NOT EXISTS stock_movements (
    id                  BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    inventory_id        BIGINT NOT NULL,
    product_id          BIGINT NOT NULL,
    movement_type       VARCHAR(20) NOT NULL,
    quantity            INT NOT NULL,
    previous_available  INT NOT NULL,
    after_available     INT NOT NULL,
    previous_reserved   INT NOT NULL,
    after_reserved      INT NOT NULL,
    reference_type      VARCHAR(50) DEFAULT NULL,
    reference_id        VARCHAR(100) DEFAULT NULL,
    reason              VARCHAR(500) DEFAULT NULL,
    performed_by        VARCHAR(100) DEFAULT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_movement_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id)
);

-- 배송
CREATE TABLE IF NOT EXISTS deliveries (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    tracking_number         VARCHAR(30) NOT NULL,
    order_id                BIGINT NOT NULL,
    order_number            VARCHAR(30) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    carrier                 VARCHAR(50) DEFAULT NULL,
    receiver_name           VARCHAR(100) DEFAULT NULL,
    receiver_phone          VARCHAR(20) DEFAULT NULL,
    zip_code                VARCHAR(10) DEFAULT NULL,
    address1                VARCHAR(255) DEFAULT NULL,
    address2                VARCHAR(255) DEFAULT NULL,
    estimated_delivery_date DATE DEFAULT NULL,
    actual_delivery_date    DATE DEFAULT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT idx_delivery_tracking_number UNIQUE (tracking_number),
    CONSTRAINT fk_delivery_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- 배송 이력
CREATE TABLE IF NOT EXISTS delivery_histories (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    delivery_id BIGINT NOT NULL,
    status      VARCHAR(20) NOT NULL,
    location    VARCHAR(255) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_delivery_history_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE CASCADE
);

-- Saga 상태
CREATE TABLE IF NOT EXISTS saga_states (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    saga_id                 VARCHAR(50) NOT NULL,
    order_id                BIGINT NOT NULL,
    order_number            VARCHAR(30) NOT NULL,
    current_step            VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    completed_steps         VARCHAR(500) DEFAULT NULL,
    last_error_message      VARCHAR(1000) DEFAULT NULL,
    compensation_attempts   INT NOT NULL DEFAULT 0,
    started_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at            TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT idx_saga_id UNIQUE (saga_id)
);

-- 타임딜
CREATE TABLE IF NOT EXISTS time_deals (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    starts_at   TIMESTAMP NOT NULL,
    ends_at     TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 타임딜 상품
CREATE TABLE IF NOT EXISTS time_deal_products (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    time_deal_id    BIGINT NOT NULL,
    product_id      BIGINT NOT NULL,
    deal_price      DECIMAL(10,2) NOT NULL,
    deal_quantity   INT NOT NULL,
    sold_quantity   INT NOT NULL DEFAULT 0,
    max_per_user    INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_tdp_time_deal FOREIGN KEY (time_deal_id) REFERENCES time_deals (id),
    CONSTRAINT fk_tdp_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- 타임딜 구매
CREATE TABLE IF NOT EXISTS time_deal_purchases (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id                 VARCHAR(36) NOT NULL,
    time_deal_product_id    BIGINT NOT NULL,
    quantity                INT NOT NULL,
    purchase_price          DECIMAL(10,2) NOT NULL,
    order_id                BIGINT DEFAULT NULL,
    purchased_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tdpurchase_product FOREIGN KEY (time_deal_product_id) REFERENCES time_deal_products (id)
);

-- 대기열
CREATE TABLE IF NOT EXISTS waiting_queues (
    id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    event_type              VARCHAR(50) NOT NULL,
    event_id                BIGINT NOT NULL,
    max_capacity            INT NOT NULL,
    entry_batch_size        INT NOT NULL,
    entry_interval_seconds  INT NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at            TIMESTAMP DEFAULT NULL,
    deactivated_at          TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id)
);

-- 대기열 항목
CREATE TABLE IF NOT EXISTS queue_entries (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    queue_id    BIGINT NOT NULL,
    user_id     VARCHAR(36) NOT NULL,
    entry_token VARCHAR(36) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    joined_at   TIMESTAMP NOT NULL,
    entered_at  TIMESTAMP DEFAULT NULL,
    expired_at  TIMESTAMP DEFAULT NULL,
    left_at     TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_entry_token UNIQUE (entry_token),
    CONSTRAINT fk_queue_entry_queue FOREIGN KEY (queue_id) REFERENCES waiting_queues (id)
);

-- ===================================================================
-- Indexes
-- ===================================================================

CREATE INDEX idx_products_name ON products (name);

CREATE INDEX idx_product_images_product_id ON product_images (product_id);
CREATE INDEX idx_product_images_sort_order ON product_images (product_id, sort_order);

CREATE INDEX idx_cart_user_status ON carts (user_id, status);

CREATE INDEX idx_cart_item_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_item_product_id ON cart_items (product_id);

CREATE INDEX idx_coupons_code ON coupons (code);
CREATE INDEX idx_coupons_status ON coupons (status);
CREATE INDEX idx_coupons_expires_at ON coupons (expires_at);

CREATE INDEX idx_user_coupons_user_id ON user_coupons (user_id);
CREATE INDEX idx_user_coupons_status ON user_coupons (status);
CREATE INDEX idx_user_coupons_expires_at ON user_coupons (expires_at);

CREATE INDEX idx_order_user_id ON orders (user_id);
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_created_at ON orders (created_at);

CREATE INDEX idx_order_item_order_id ON order_items (order_id);
CREATE INDEX idx_order_item_product_id ON order_items (product_id);

CREATE INDEX idx_payment_order_id ON payments (order_id);
CREATE INDEX idx_payment_user_id ON payments (user_id);
CREATE INDEX idx_payment_status ON payments (status);
CREATE INDEX idx_payment_pg_transaction_id ON payments (pg_transaction_id);

CREATE INDEX idx_stock_movement_inventory_id ON stock_movements (inventory_id);
CREATE INDEX idx_stock_movement_product_id ON stock_movements (product_id);
CREATE INDEX idx_stock_movement_reference ON stock_movements (reference_type, reference_id);
CREATE INDEX idx_stock_movement_created_at ON stock_movements (created_at);

CREATE INDEX idx_delivery_order_id ON deliveries (order_id);
CREATE INDEX idx_delivery_status ON deliveries (status);

CREATE INDEX idx_delivery_history_delivery_id ON delivery_histories (delivery_id);
CREATE INDEX idx_delivery_history_created_at ON delivery_histories (created_at);

CREATE INDEX idx_saga_order_id ON saga_states (order_id);
CREATE INDEX idx_saga_status ON saga_states (status);

CREATE INDEX idx_time_deals_status ON time_deals (status);
CREATE INDEX idx_time_deals_starts_at ON time_deals (starts_at);
CREATE INDEX idx_time_deals_ends_at ON time_deals (ends_at);

CREATE INDEX idx_tdp_time_deal_id ON time_deal_products (time_deal_id);
CREATE INDEX idx_tdp_product_id ON time_deal_products (product_id);

CREATE INDEX idx_tdp_user_id ON time_deal_purchases (user_id);
CREATE INDEX idx_tdp_user_product ON time_deal_purchases (user_id, time_deal_product_id);

CREATE INDEX idx_waiting_queues_event ON waiting_queues (event_type, event_id);
CREATE INDEX idx_waiting_queues_active ON waiting_queues (is_active);

CREATE INDEX idx_queue_entry_queue_user ON queue_entries (queue_id, user_id);
CREATE INDEX idx_queue_entry_token ON queue_entries (entry_token);
CREATE INDEX idx_queue_entry_status ON queue_entries (status);

-- ===================================================================
-- Triggers: updated_at 자동 갱신
-- ===================================================================

CREATE TRIGGER trg_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_coupons_updated_at
    BEFORE UPDATE ON coupons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_inventory_updated_at
    BEFORE UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_deliveries_updated_at
    BEFORE UPDATE ON deliveries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_time_deals_updated_at
    BEFORE UPDATE ON time_deals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
