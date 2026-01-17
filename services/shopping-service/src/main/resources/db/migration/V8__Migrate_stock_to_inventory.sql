-- V8__Migrate_stock_to_inventory.sql
-- 기존 products 테이블의 stock을 inventory 테이블로 마이그레이션

-- 기존 상품의 재고를 inventory 테이블로 이전
INSERT INTO inventory (product_id, available_quantity, reserved_quantity, total_quantity, version, created_at)
SELECT id, stock, 0, stock, 0, NOW()
FROM products
WHERE id NOT IN (SELECT product_id FROM inventory);

-- 마이그레이션 완료 후 이력 기록
INSERT INTO stock_movements (
    inventory_id, product_id, movement_type, quantity,
    previous_available, after_available, previous_reserved, after_reserved,
    reference_type, reference_id, reason, performed_by, created_at
)
SELECT
    i.id, i.product_id, 'INITIAL', i.total_quantity,
    0, i.available_quantity, 0, 0,
    'MIGRATION', CONCAT('V8_', i.product_id), 'Initial migration from products.stock', 'SYSTEM', NOW()
FROM inventory i
WHERE NOT EXISTS (
    SELECT 1 FROM stock_movements sm
    WHERE sm.inventory_id = i.id AND sm.reference_type = 'MIGRATION'
);
