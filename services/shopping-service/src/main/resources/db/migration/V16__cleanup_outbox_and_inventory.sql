-- Remove duplicate aggregate_id column from outbox_events (eventKey already stores the same value)
ALTER TABLE outbox_events DROP COLUMN IF EXISTS aggregate_id;

-- Remove stock_movements table (inventory write operations moved to seller-service)
DROP TABLE IF EXISTS stock_movements;
