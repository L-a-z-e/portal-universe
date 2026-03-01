-- =============================================================
-- V13: 복합 인덱스 추가 (성능 최적화 Phase 6)
-- =============================================================

-- 1. Filesort 제거: 사용자 주문 목록 (user_id + status + created_at DESC)
CREATE INDEX idx_order_user_status_created ON orders (user_id, status, created_at DESC);

-- 2. Filesort 제거: 재고 이력 페이징 (product_id + created_at DESC)
CREATE INDEX idx_stock_movement_product_created ON stock_movements (product_id, created_at DESC);

-- 3. Full Scan 방지: Saga order_number 조회
CREATE INDEX idx_saga_order_number ON saga_states (order_number);

-- 4. Full Scan 방지: 쿠폰 발급 수 집계 (coupon_id FK 역참조)
CREATE INDEX idx_user_coupons_coupon_id ON user_coupons (coupon_id);

-- 5. Full Scan 방지: seller-service 쿠폰 동기화 (Kafka Consumer)
CREATE INDEX idx_coupons_source_coupon_id ON coupons (source_coupon_id);

-- 6. 복합 인덱스: 배치 dead saga 조회 (status + started_at 정렬)
CREATE INDEX idx_saga_status_started ON saga_states (status, started_at ASC);

-- 7. 복합 인덱스: 구매 전 사용 가능 쿠폰 조회
CREATE INDEX idx_user_coupons_user_status_expires ON user_coupons (user_id, status, expires_at);

-- 8. 복합 인덱스: 배치 만료 쿠폰 스캔
CREATE INDEX idx_user_coupons_status_expires ON user_coupons (status, expires_at ASC);

-- 9. 복합 인덱스: 사용 가능 쿠폰 목록
CREATE INDEX idx_coupons_status_starts_expires ON coupons (status, starts_at, expires_at);

-- 10. 복합 인덱스: 배치 만료 쿠폰
CREATE INDEX idx_coupons_status_expires ON coupons (status, expires_at ASC);
