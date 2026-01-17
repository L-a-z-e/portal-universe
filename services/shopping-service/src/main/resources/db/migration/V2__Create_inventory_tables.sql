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
