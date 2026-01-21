-- =====================================================
-- V11: 주문 테이블에 쿠폰 관련 컬럼 추가
-- =====================================================

-- 할인 금액 컬럼 추가
ALTER TABLE orders
    ADD COLUMN discount_amount DECIMAL(12, 2) DEFAULT 0.00 COMMENT '할인 금액';

-- 최종 결제 금액 컬럼 추가
ALTER TABLE orders
    ADD COLUMN final_amount DECIMAL(12, 2) COMMENT '최종 결제 금액';

-- 적용된 사용자 쿠폰 ID 컬럼 추가
ALTER TABLE orders
    ADD COLUMN applied_user_coupon_id BIGINT COMMENT '적용된 사용자 쿠폰 ID';

-- 외래 키 제약 조건 추가
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_user_coupon
    FOREIGN KEY (applied_user_coupon_id) REFERENCES user_coupons(id);

-- 기존 데이터에 대해 final_amount를 total_amount로 설정
UPDATE orders SET final_amount = total_amount WHERE final_amount IS NULL;
