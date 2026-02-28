-- Denormalize userId from orders to deliveries for IDOR ownership verification
ALTER TABLE deliveries ADD COLUMN user_id VARCHAR(50);

-- Backfill from orders table
UPDATE deliveries d
SET user_id = o.user_id
FROM orders o
WHERE d.order_id = o.id;

-- Make NOT NULL after backfill
ALTER TABLE deliveries ALTER COLUMN user_id SET NOT NULL;

-- Index for ownership queries
CREATE INDEX idx_delivery_user_id ON deliveries (user_id);
