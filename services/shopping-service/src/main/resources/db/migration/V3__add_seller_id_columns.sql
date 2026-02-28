-- seller_id 컬럼 추가: Product → CartItem → OrderItem 데이터 흐름
-- Phase 2 Step 1: sellerId 1L 하드코딩 제거를 위한 스키마 변경

-- products: 기존 행은 seller 1 (seed data), 이후 DEFAULT 제거
ALTER TABLE products ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE products ALTER COLUMN seller_id DROP DEFAULT;

-- cart_items: 기존 행은 0 (미지정), 이후 DEFAULT 제거
ALTER TABLE cart_items ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cart_items ALTER COLUMN seller_id DROP DEFAULT;

-- order_items: 기존 행은 0 (미지정), 이후 DEFAULT 제거
ALTER TABLE order_items ADD COLUMN seller_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE order_items ALTER COLUMN seller_id DROP DEFAULT;

-- seller_id 인덱스
CREATE INDEX idx_products_seller_id ON products (seller_id);
CREATE INDEX idx_order_items_seller_id ON order_items (seller_id);
