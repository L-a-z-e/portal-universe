-- =================================================================
-- Post-Load-Test Integrity Verification Queries
-- Run against respective databases after load tests complete
-- =================================================================

-- -----------------------------------------------------------------
-- 1. 재고 정합성 (shopping_seller_db)
-- Saga 완료 후 reserved_quantity는 0이어야 정상
-- (모든 reserve → deduct 또는 reserve → release 완료)
-- -----------------------------------------------------------------
SELECT
    i.product_id,
    p.name AS product_name,
    i.available_quantity,
    i.reserved_quantity,
    i.total_quantity,
    CASE WHEN i.reserved_quantity > 0 THEN 'FAIL' ELSE 'PASS' END AS status
FROM inventory i
JOIN products p ON p.id = i.product_id
WHERE i.reserved_quantity > 0
ORDER BY i.reserved_quantity DESC;

-- -----------------------------------------------------------------
-- 2. Saga 상태 분포 (shopping_db)
-- COMPLETED, COMPENSATED가 대부분이어야 정상
-- PENDING이나 IN_PROGRESS가 남아있으면 미완료 Saga 존재
-- -----------------------------------------------------------------
SELECT
    status,
    COUNT(*) AS count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) AS percentage
FROM saga_states
GROUP BY status
ORDER BY count DESC;

-- -----------------------------------------------------------------
-- 3. 쿠폰 초과발급 확인 (shopping_seller_db)
-- issued count > total_quantity이면 동시성 제어 실패
-- -----------------------------------------------------------------
SELECT
    c.id,
    c.code,
    c.name,
    c.total_quantity,
    COUNT(uc.id) AS issued_count,
    CASE WHEN COUNT(uc.id) > c.total_quantity THEN 'FAIL' ELSE 'PASS' END AS status
FROM coupons c
LEFT JOIN user_coupons uc ON uc.coupon_id = c.id
GROUP BY c.id, c.code, c.name, c.total_quantity
HAVING COUNT(uc.id) > c.total_quantity
ORDER BY c.code;

-- -----------------------------------------------------------------
-- 4. TimeDeal 초과판매 확인 (shopping_seller_db)
-- sold_quantity > deal_quantity이면 수량 제어 실패
-- -----------------------------------------------------------------
SELECT
    tdp.id,
    td.name AS deal_name,
    tdp.product_id,
    tdp.deal_quantity,
    tdp.sold_quantity,
    CASE WHEN tdp.sold_quantity > tdp.deal_quantity THEN 'FAIL' ELSE 'PASS' END AS status
FROM time_deal_products tdp
JOIN time_deals td ON td.id = tdp.time_deal_id
WHERE tdp.sold_quantity > tdp.deal_quantity
ORDER BY tdp.sold_quantity DESC;

-- -----------------------------------------------------------------
-- 5. 주문 상태 분포 (shopping_db)
-- -----------------------------------------------------------------
SELECT
    status,
    COUNT(*) AS count
FROM orders
GROUP BY status
ORDER BY count DESC;

-- -----------------------------------------------------------------
-- 6. 결제 Intent 상태 분포 (payment_db)
-- -----------------------------------------------------------------
SELECT
    status,
    COUNT(*) AS count
FROM payment_intents
GROUP BY status
ORDER BY count DESC;

-- -----------------------------------------------------------------
-- 7. 재고 요약 (shopping_seller_db)
-- available + reserved = total 확인
-- -----------------------------------------------------------------
SELECT
    COUNT(*) AS total_products,
    SUM(CASE WHEN reserved_quantity = 0 THEN 1 ELSE 0 END) AS clean_products,
    SUM(CASE WHEN reserved_quantity > 0 THEN 1 ELSE 0 END) AS stuck_products,
    SUM(CASE WHEN available_quantity + reserved_quantity != total_quantity THEN 1 ELSE 0 END) AS inconsistent_products
FROM inventory;
