-- Covering index for batch partitioner and reader queries
CREATE INDEX idx_ledger_batch_lookup
    ON settlement_ledger (processed, event_at, seller_id)
    INCLUDE (amount, event_type, order_number);

-- Idempotency unique constraint for settlement periods
ALTER TABLE settlement_periods
    ADD CONSTRAINT uq_period_type_dates UNIQUE (period_type, start_date, end_date);

-- Drop single-column indexes now covered by the composite index
DROP INDEX IF EXISTS idx_ledger_processed;
DROP INDEX IF EXISTS idx_ledger_event_at;
