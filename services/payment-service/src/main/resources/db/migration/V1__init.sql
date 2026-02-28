-- ===================================================================
-- Payment Service Database Schema
-- ===================================================================

-- Trigger function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- ===================================================================
-- Payment Intents — 결제 의향 (서버가 확정한 금액과 메타데이터)
-- ===================================================================
CREATE TABLE IF NOT EXISTS payment_intents (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    intent_id       VARCHAR(36) NOT NULL,
    order_number    VARCHAR(30) NOT NULL,
    user_id         VARCHAR(100) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'REQUIRES_PAYMENT',
    metadata        JSONB,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_intent_id UNIQUE (intent_id)
);

CREATE INDEX idx_intent_order_number ON payment_intents (order_number);
CREATE INDEX idx_intent_user_id ON payment_intents (user_id);
CREATE INDEX idx_intent_status_expires ON payment_intents (status, expires_at)
    WHERE status = 'REQUIRES_PAYMENT';

CREATE TRIGGER trg_payment_intents_updated_at
    BEFORE UPDATE ON payment_intents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ===================================================================
-- Payments — 실제 결제 기록
-- ===================================================================
CREATE TABLE IF NOT EXISTS payments (
    id                  BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    payment_number      VARCHAR(30) NOT NULL,
    intent_id           VARCHAR(36) NOT NULL,
    order_number        VARCHAR(30) NOT NULL,
    user_id             VARCHAR(100) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30) NOT NULL,
    pg_transaction_id   VARCHAR(100) DEFAULT NULL,
    pg_response         TEXT,
    failure_reason      VARCHAR(500) DEFAULT NULL,
    paid_at             TIMESTAMPTZ DEFAULT NULL,
    refunded_at         TIMESTAMPTZ DEFAULT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_payment_number UNIQUE (payment_number),
    CONSTRAINT fk_payment_intent FOREIGN KEY (intent_id) REFERENCES payment_intents (intent_id)
);

CREATE INDEX idx_payment_order_number ON payments (order_number);
CREATE INDEX idx_payment_user_id ON payments (user_id);
CREATE INDEX idx_payment_status ON payments (status);
CREATE INDEX idx_payment_intent_id ON payments (intent_id);

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ===================================================================
-- Outbox Events — Transactional Outbox for Kafka
-- ===================================================================
CREATE TABLE IF NOT EXISTS outbox_events (
    id              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    event_key       VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMPTZ,
    PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_pending ON outbox_events (status, created_at)
    WHERE status = 'PENDING';
