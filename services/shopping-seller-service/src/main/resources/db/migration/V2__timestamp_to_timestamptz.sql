-- ===================================================================
-- V2: TIMESTAMP -> TIMESTAMPTZ migration (Asia/Seoul)
-- ===================================================================

-- sellers
ALTER TABLE sellers ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE sellers ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- products
ALTER TABLE products ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE products ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- inventory
ALTER TABLE inventory ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE inventory ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- stock_movements
ALTER TABLE stock_movements ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE stock_movements ADD COLUMN updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TRIGGER trg_stock_movements_updated_at
  BEFORE UPDATE ON stock_movements
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- coupons
ALTER TABLE coupons ALTER COLUMN starts_at TYPE TIMESTAMPTZ USING starts_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE coupons ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- time_deals
ALTER TABLE time_deals ALTER COLUMN starts_at TYPE TIMESTAMPTZ USING starts_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN ends_at TYPE TIMESTAMPTZ USING ends_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE time_deals ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- waiting_queues
ALTER TABLE waiting_queues ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE waiting_queues ALTER COLUMN activated_at TYPE TIMESTAMPTZ USING activated_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE waiting_queues ALTER COLUMN deactivated_at TYPE TIMESTAMPTZ USING deactivated_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE waiting_queues ADD COLUMN updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TRIGGER trg_waiting_queues_updated_at
  BEFORE UPDATE ON waiting_queues
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- queue_entries
ALTER TABLE queue_entries ALTER COLUMN joined_at TYPE TIMESTAMPTZ USING joined_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN entered_at TYPE TIMESTAMPTZ USING entered_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN expired_at TYPE TIMESTAMPTZ USING expired_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE queue_entries ALTER COLUMN left_at TYPE TIMESTAMPTZ USING left_at AT TIME ZONE 'Asia/Seoul';

-- product_images
ALTER TABLE product_images ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE product_images ADD COLUMN updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TRIGGER trg_product_images_updated_at
  BEFORE UPDATE ON product_images
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
