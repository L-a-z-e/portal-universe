-- =====================================================
-- V9: 쿠폰 관련 테이블 생성
-- =====================================================

-- 쿠폰 테이블
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '쿠폰 코드',
    name VARCHAR(100) NOT NULL COMMENT '쿠폰명',
    description TEXT COMMENT '쿠폰 설명',
    discount_type VARCHAR(20) NOT NULL COMMENT '할인 유형 (FIXED, PERCENTAGE)',
    discount_value DECIMAL(10, 2) NOT NULL COMMENT '할인 값',
    minimum_order_amount DECIMAL(10, 2) COMMENT '최소 주문 금액',
    maximum_discount_amount DECIMAL(10, 2) COMMENT '최대 할인 금액',
    total_quantity INT NOT NULL COMMENT '총 발급 수량',
    issued_quantity INT NOT NULL DEFAULT 0 COMMENT '발급된 수량',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '상태 (ACTIVE, EXHAUSTED, EXPIRED, INACTIVE)',
    starts_at DATETIME NOT NULL COMMENT '발급 시작 일시',
    expires_at DATETIME NOT NULL COMMENT '만료 일시',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_coupons_code (code),
    INDEX idx_coupons_status (status),
    INDEX idx_coupons_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자 쿠폰 테이블
CREATE TABLE IF NOT EXISTS user_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    coupon_id BIGINT NOT NULL COMMENT '쿠폰 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '상태 (AVAILABLE, USED, EXPIRED)',
    order_id BIGINT COMMENT '사용된 주문 ID',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 일시',
    used_at DATETIME COMMENT '사용 일시',
    expires_at DATETIME NOT NULL COMMENT '만료 일시',
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    UNIQUE KEY uk_user_coupon (user_id, coupon_id),
    INDEX idx_user_coupons_user_id (user_id),
    INDEX idx_user_coupons_status (status),
    INDEX idx_user_coupons_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
