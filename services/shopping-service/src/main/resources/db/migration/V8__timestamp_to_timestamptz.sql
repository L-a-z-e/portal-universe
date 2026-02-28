-- ===================================================================
-- V8: TIMESTAMP → TIMESTAMPTZ 마이그레이션
-- LocalDateTime → Instant 전환에 맞춰 모든 TIMESTAMP 컬럼을 TIMESTAMPTZ로 변경
-- 기존 데이터는 Asia/Seoul 기준으로 변환
-- ===================================================================

-- products
ALTER TABLE products ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE products ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- product_images
ALTER TABLE product_images ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';

-- carts
ALTER TABLE carts ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE carts ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- cart_items
ALTER TABLE cart_items ALTER COLUMN added_at TYPE TIMESTAMPTZ USING added_at AT TIME ZONE 'Asia/Seoul';

-- coupons
ALTER TABLE coupons ALTER COLUMN starts_at TYPE TIMESTAMPTZ USING starts_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- user_coupons
ALTER TABLE user_coupons ALTER COLUMN issued_at TYPE TIMESTAMPTZ USING issued_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_coupons ALTER COLUMN used_at TYPE TIMESTAMPTZ USING used_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE user_coupons ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'Asia/Seoul';

-- orders
ALTER TABLE orders ALTER COLUMN cancelled_at TYPE TIMESTAMPTZ USING cancelled_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE orders ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE orders ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- inventory
ALTER TABLE inventory ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE inventory ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- stock_movements
ALTER TABLE stock_movements ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';

-- deliveries
ALTER TABLE deliveries ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE deliveries ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- delivery_histories
ALTER TABLE delivery_histories ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';

-- saga_states
ALTER TABLE saga_states ALTER COLUMN started_at TYPE TIMESTAMPTZ USING started_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE saga_states ALTER COLUMN completed_at TYPE TIMESTAMPTZ USING completed_at AT TIME ZONE 'Asia/Seoul';

-- time_deals
ALTER TABLE time_deals ALTER COLUMN starts_at TYPE TIMESTAMPTZ USING starts_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN ends_at TYPE TIMESTAMPTZ USING ends_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- time_deal_purchases
ALTER TABLE time_deal_purchases ALTER COLUMN purchased_at TYPE TIMESTAMPTZ USING purchased_at AT TIME ZONE 'Asia/Seoul';

-- waiting_queues
ALTER TABLE waiting_queues ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE waiting_queues ALTER COLUMN activated_at TYPE TIMESTAMPTZ USING activated_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE waiting_queues ALTER COLUMN deactivated_at TYPE TIMESTAMPTZ USING deactivated_at AT TIME ZONE 'Asia/Seoul';

-- queue_entries
ALTER TABLE queue_entries ALTER COLUMN joined_at TYPE TIMESTAMPTZ USING joined_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN entered_at TYPE TIMESTAMPTZ USING entered_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN expired_at TYPE TIMESTAMPTZ USING expired_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN left_at TYPE TIMESTAMPTZ USING left_at AT TIME ZONE 'Asia/Seoul';
