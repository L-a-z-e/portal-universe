-- ===================================================================
-- Shopping Seller Service - Consolidated Init Schema (PostgreSQL)
-- Merged: V1__baseline + V2__product_discount_images
-- ===================================================================

-- Trigger function for updated_at auto-update
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

-- 판매자
CREATE TABLE IF NOT EXISTS sellers (
  id                  BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  user_id             VARCHAR(255) NOT NULL,
  business_name       VARCHAR(100) NOT NULL,
  business_number     VARCHAR(20) DEFAULT NULL,
  representative_name VARCHAR(50) DEFAULT NULL,
  phone               VARCHAR(20) DEFAULT NULL,
  email               VARCHAR(100) DEFAULT NULL,
  bank_name           VARCHAR(50) DEFAULT NULL,
  bank_account        VARCHAR(30) DEFAULT NULL,
  commission_rate     DECIMAL(5,2) NOT NULL DEFAULT 10.00,
  status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_sellers_user_id UNIQUE (user_id)
);

-- 상품 (V2: discount_price, featured 컬럼 포함)
CREATE TABLE IF NOT EXISTS products (
  id             BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  seller_id      BIGINT NOT NULL DEFAULT 1,
  name           VARCHAR(255) NOT NULL,
  description    TEXT,
  price          DECIMAL(19,2) NOT NULL,
  discount_price DECIMAL(19,2) DEFAULT NULL,
  stock          INT NOT NULL DEFAULT 0,
  image_url      VARCHAR(500) DEFAULT NULL,
  category       VARCHAR(100) DEFAULT NULL,
  featured       BOOLEAN NOT NULL DEFAULT FALSE,
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id)
);

-- 재고
CREATE TABLE IF NOT EXISTS inventory (
  id                 BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  product_id         BIGINT NOT NULL,
  available_quantity INT NOT NULL DEFAULT 0,
  reserved_quantity  INT NOT NULL DEFAULT 0,
  total_quantity     INT NOT NULL DEFAULT 0,
  version            BIGINT DEFAULT 0,
  created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_inventory_product_id UNIQUE (product_id),
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id)
);

-- 재고 이동 이력
CREATE TABLE IF NOT EXISTS stock_movements (
  id                 BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  inventory_id       BIGINT NOT NULL,
  product_id         BIGINT NOT NULL,
  movement_type      VARCHAR(20) NOT NULL,
  quantity           INT NOT NULL,
  previous_available INT NOT NULL,
  after_available    INT NOT NULL,
  previous_reserved  INT NOT NULL,
  after_reserved     INT NOT NULL,
  reference_type     VARCHAR(50) DEFAULT NULL,
  reference_id       VARCHAR(100) DEFAULT NULL,
  reason             VARCHAR(500) DEFAULT NULL,
  performed_by       VARCHAR(100) DEFAULT NULL,
  created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_stock_movement_inventory FOREIGN KEY (inventory_id) REFERENCES inventory (id)
);

-- 쿠폰
CREATE TABLE IF NOT EXISTS coupons (
  id                      BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  seller_id               BIGINT NOT NULL DEFAULT 1,
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

-- 타임딜
CREATE TABLE IF NOT EXISTS time_deals (
  id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  seller_id   BIGINT NOT NULL DEFAULT 1,
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
  id            BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  time_deal_id  BIGINT NOT NULL,
  product_id    BIGINT NOT NULL,
  deal_price    DECIMAL(10,2) NOT NULL,
  deal_quantity INT NOT NULL,
  sold_quantity INT NOT NULL DEFAULT 0,
  max_per_user  INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT fk_tdp_time_deal FOREIGN KEY (time_deal_id) REFERENCES time_deals (id),
  CONSTRAINT fk_tdp_product   FOREIGN KEY (product_id)   REFERENCES products (id)
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

-- 상품 이미지 (V2)
CREATE TABLE IF NOT EXISTS product_images (
  id         BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
  product_id BIGINT NOT NULL,
  image_url  VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  alt_text   VARCHAR(255) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- ===================================================================
-- Indexes
-- ===================================================================

-- sellers
CREATE INDEX idx_sellers_status        ON sellers (status);
CREATE INDEX idx_sellers_business_name ON sellers (business_name);

-- products
CREATE INDEX idx_products_name      ON products (name);
CREATE INDEX idx_products_seller_id ON products (seller_id);
CREATE INDEX idx_products_category  ON products (category);

-- stock_movements
CREATE INDEX idx_stock_movement_inventory_id ON stock_movements (inventory_id);
CREATE INDEX idx_stock_movement_product_id   ON stock_movements (product_id);
CREATE INDEX idx_stock_movement_reference    ON stock_movements (reference_type, reference_id);
CREATE INDEX idx_stock_movement_created_at   ON stock_movements (created_at);

-- coupons
CREATE INDEX idx_coupons_seller_id  ON coupons (seller_id);
CREATE INDEX idx_coupons_status     ON coupons (status);
CREATE INDEX idx_coupons_expires_at ON coupons (expires_at);

-- time_deals
CREATE INDEX idx_time_deals_seller_id  ON time_deals (seller_id);
CREATE INDEX idx_time_deals_status     ON time_deals (status);
CREATE INDEX idx_time_deals_starts_at  ON time_deals (starts_at);
CREATE INDEX idx_time_deals_ends_at    ON time_deals (ends_at);

-- time_deal_products
CREATE INDEX idx_tdp_time_deal_id ON time_deal_products (time_deal_id);
CREATE INDEX idx_tdp_product_id   ON time_deal_products (product_id);

-- waiting_queues
CREATE INDEX idx_waiting_queues_event  ON waiting_queues (event_type, event_id);
CREATE INDEX idx_waiting_queues_active ON waiting_queues (is_active);

-- queue_entries
CREATE INDEX idx_queue_entry_queue_user ON queue_entries (queue_id, user_id);
CREATE INDEX idx_queue_entry_status     ON queue_entries (status);

-- product_images
CREATE INDEX idx_product_images_product_id  ON product_images (product_id);
CREATE INDEX idx_product_images_sort_order  ON product_images (product_id, sort_order);

-- ===================================================================
-- Triggers (updated_at auto-update)
-- ===================================================================

CREATE TRIGGER trg_sellers_updated_at
  BEFORE UPDATE ON sellers
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_products_updated_at
  BEFORE UPDATE ON products
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_inventory_updated_at
  BEFORE UPDATE ON inventory
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_coupons_updated_at
  BEFORE UPDATE ON coupons
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_time_deals_updated_at
  BEFORE UPDATE ON time_deals
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
