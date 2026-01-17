-- V6__Create_delivery_tables.sql
-- 배송 테이블

-- 배송 테이블
CREATE TABLE deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(30) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
    carrier VARCHAR(50),

    -- 배송지 주소 (Embedded)
    receiver_name VARCHAR(100),
    receiver_phone VARCHAR(20),
    zip_code VARCHAR(10),
    address1 VARCHAR(255),
    address2 VARCHAR(255),

    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_delivery_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE UNIQUE INDEX idx_delivery_tracking_number ON deliveries(tracking_number);
CREATE INDEX idx_delivery_order_id ON deliveries(order_id);
CREATE INDEX idx_delivery_status ON deliveries(status);

-- 배송 이력 테이블
CREATE TABLE delivery_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    location VARCHAR(255),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_delivery_history_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_delivery_history_delivery_id ON delivery_histories(delivery_id);
CREATE INDEX idx_delivery_history_created_at ON delivery_histories(created_at);
