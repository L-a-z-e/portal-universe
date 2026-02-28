-- Coupon: seller-service 원본 동기화 필드 추가
ALTER TABLE coupons ADD COLUMN source_coupon_id BIGINT;
ALTER TABLE coupons ADD COLUMN seller_id BIGINT;
ALTER TABLE coupons ADD CONSTRAINT uq_coupons_source_coupon_id UNIQUE (source_coupon_id);
CREATE INDEX idx_coupons_seller_id ON coupons (seller_id);

-- TimeDeal: seller-service 원본 동기화 필드 추가
ALTER TABLE time_deals ADD COLUMN source_time_deal_id BIGINT;
ALTER TABLE time_deals ADD COLUMN seller_id BIGINT;
ALTER TABLE time_deals ADD CONSTRAINT uq_time_deals_source_time_deal_id UNIQUE (source_time_deal_id);
CREATE INDEX idx_time_deals_seller_id ON time_deals (seller_id);

-- TimeDealProduct: product FK 제거 (seller-service productId와 다를 수 있음)
ALTER TABLE time_deal_products DROP CONSTRAINT IF EXISTS fk_tdp_product;
