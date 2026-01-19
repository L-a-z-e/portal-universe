-- =====================================================
-- V10: 타임딜 관련 테이블 생성
-- =====================================================

-- 타임딜 테이블
CREATE TABLE IF NOT EXISTS time_deals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '타임딜명',
    description TEXT COMMENT '타임딜 설명',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT '상태 (SCHEDULED, ACTIVE, ENDED, CANCELLED)',
    starts_at DATETIME NOT NULL COMMENT '시작 일시',
    ends_at DATETIME NOT NULL COMMENT '종료 일시',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_time_deals_status (status),
    INDEX idx_time_deals_starts_at (starts_at),
    INDEX idx_time_deals_ends_at (ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 타임딜 상품 테이블
CREATE TABLE IF NOT EXISTS time_deal_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    time_deal_id BIGINT NOT NULL COMMENT '타임딜 ID',
    product_id BIGINT NOT NULL COMMENT '상품 ID',
    deal_price DECIMAL(10, 2) NOT NULL COMMENT '할인 가격',
    deal_quantity INT NOT NULL COMMENT '타임딜 수량',
    sold_quantity INT NOT NULL DEFAULT 0 COMMENT '판매된 수량',
    max_per_user INT NOT NULL DEFAULT 1 COMMENT '1인당 최대 구매 수량',
    FOREIGN KEY (time_deal_id) REFERENCES time_deals(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_tdp_time_deal_id (time_deal_id),
    INDEX idx_tdp_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 타임딜 구매 기록 테이블
CREATE TABLE IF NOT EXISTS time_deal_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    time_deal_product_id BIGINT NOT NULL COMMENT '타임딜 상품 ID',
    quantity INT NOT NULL COMMENT '구매 수량',
    purchase_price DECIMAL(10, 2) NOT NULL COMMENT '구매 가격',
    order_id BIGINT COMMENT '주문 ID',
    purchased_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '구매 일시',
    FOREIGN KEY (time_deal_product_id) REFERENCES time_deal_products(id),
    INDEX idx_tdp_user_id (user_id),
    INDEX idx_tdp_user_product (user_id, time_deal_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
