-- =====================================================
-- V13: user_id 컬럼을 BIGINT에서 VARCHAR(36)으로 변경
-- 목적: JWT subject로 UUID를 사용하므로 일관성 확보
-- =====================================================

-- 1. user_coupons 테이블
ALTER TABLE user_coupons
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';

-- 2. time_deal_purchases 테이블
ALTER TABLE time_deal_purchases
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';

-- 3. queue_entries 테이블
ALTER TABLE queue_entries
    MODIFY COLUMN user_id VARCHAR(36) NOT NULL COMMENT '사용자 UUID';
