-- =============================================================
-- V4: 복합 인덱스 추가 (성능 최적화 Phase 6)
-- =============================================================

-- 1. Filesort 제거: 재고 이력 페이징 (product_id + created_at DESC)
CREATE INDEX idx_stock_movement_product_created ON stock_movements (product_id, created_at DESC);

-- 2. 복합 인덱스: 판매자별 카테고리 필터
CREATE INDEX idx_products_seller_category ON products (seller_id, category);

-- 3. 복합 인덱스: 판매자별 쿠폰 상태 집계
CREATE INDEX idx_coupons_seller_status ON coupons (seller_id, status);

-- 4. 복합 인덱스: 배치 만료 쿠폰
CREATE INDEX idx_coupons_status_expires ON coupons (status, expires_at ASC);

-- 5. 복합 인덱스: 판매자별 타임딜 상태 집계
CREATE INDEX idx_time_deals_seller_status ON time_deals (seller_id, status);

-- 6. 복합 인덱스: 스케줄러 시작 대상 타임딜
CREATE INDEX idx_time_deals_status_starts ON time_deals (status, starts_at ASC);

-- 7. 복합 인덱스: 스케줄러 종료 대상 타임딜
CREATE INDEX idx_time_deals_status_ends ON time_deals (status, ends_at ASC);
