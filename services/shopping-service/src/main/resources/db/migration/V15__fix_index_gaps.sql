-- =============================================================
-- V15: 인덱스 갭 수정 + 중복 인덱스 제거
-- =============================================================

-- 1. deliveries.order_number: Full Table Scan 방지
--    findByOrderNumber, findByOrderNumberWithHistories에서 사용
CREATE INDEX idx_delivery_order_number ON deliveries (order_number);

-- 2. products.category: Full Table Scan 방지
--    findByCategory(category, Pageable)에서 사용
CREATE INDEX idx_products_category ON products (category);

-- 3. queue_entries(queue_id, status, joined_at): 대기열 순번 조회 최적화
--    countByQueueAndStatus, countWaitingBefore, findTopWaiting에서 사용
--    joined_at ASC로 정렬까지 인덱스에서 해결 (Filesort 제거)
CREATE INDEX idx_queue_entry_queue_status_joined ON queue_entries (queue_id, status, joined_at ASC);

-- 4. 중복 인덱스 제거: UNIQUE 제약조건이 이미 동일 인덱스 역할
DROP INDEX IF EXISTS idx_coupons_code;              -- uk_coupons_code UNIQUE와 중복
DROP INDEX IF EXISTS idx_queue_entry_token;          -- uk_entry_token UNIQUE와 중복
DROP INDEX IF EXISTS idx_saga_order_number;          -- uq_saga_order_number UNIQUE(V2)와 중복
DROP INDEX IF EXISTS idx_coupons_source_coupon_id;   -- uq_coupons_source_coupon_id UNIQUE(V11)와 중복
