-- ===================================================================
-- Shopping Settlement Service - Initial Schema (PostgreSQL)
-- ===================================================================

-- updated_at 자동 갱신 트리거 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 정산 주기
CREATE TABLE IF NOT EXISTS settlement_periods (
    id          BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    period_type VARCHAR(20) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 판매자별 정산
CREATE TABLE IF NOT EXISTS settlements (
    id                BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    period_id         BIGINT NOT NULL,
    seller_id         BIGINT NOT NULL,
    total_sales       DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_orders      INT NOT NULL DEFAULT 0,
    total_refunds     DECIMAL(15,2) NOT NULL DEFAULT 0,
    commission_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_amount        DECIMAL(15,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
    paid_at           TIMESTAMP DEFAULT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (period_id, seller_id),
    CONSTRAINT fk_settlements_period FOREIGN KEY (period_id) REFERENCES settlement_periods (id)
);

-- 정산 상세 (주문 단위)
CREATE TABLE IF NOT EXISTS settlement_details (
    id                BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    settlement_id     BIGINT NOT NULL,
    order_number      VARCHAR(50) NOT NULL,
    order_amount      DECIMAL(15,2) NOT NULL,
    refund_amount     DECIMAL(15,2) NOT NULL DEFAULT 0,
    commission_rate   DECIMAL(5,2) NOT NULL,
    commission_amount DECIMAL(15,2) NOT NULL,
    net_amount        DECIMAL(15,2) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_settlement_details_settlement FOREIGN KEY (settlement_id) REFERENCES settlements (id)
);

-- 정산 원장 (Kafka 이벤트에서 수집)
CREATE TABLE IF NOT EXISTS settlement_ledger (
    id           BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY,
    order_number VARCHAR(50) NOT NULL,
    seller_id    BIGINT NOT NULL,
    event_type   VARCHAR(30) NOT NULL,
    amount       DECIMAL(15,2) NOT NULL,
    event_at     TIMESTAMP NOT NULL,
    processed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- ===================================================================
-- Indexes
-- ===================================================================

CREATE INDEX idx_settlement_periods_type   ON settlement_periods (period_type);
CREATE INDEX idx_settlement_periods_status ON settlement_periods (status);
CREATE INDEX idx_settlement_periods_dates  ON settlement_periods (start_date, end_date);

CREATE INDEX idx_settlements_seller_id ON settlements (seller_id);
CREATE INDEX idx_settlements_status    ON settlements (status);

CREATE INDEX idx_settlement_details_settlement ON settlement_details (settlement_id);
CREATE INDEX idx_settlement_details_order      ON settlement_details (order_number);

CREATE INDEX idx_ledger_order     ON settlement_ledger (order_number);
CREATE INDEX idx_ledger_seller    ON settlement_ledger (seller_id);
CREATE INDEX idx_ledger_processed ON settlement_ledger (processed);
CREATE INDEX idx_ledger_event_at  ON settlement_ledger (event_at);

-- ===================================================================
-- Triggers (updated_at 자동 갱신)
-- ===================================================================

CREATE TRIGGER trg_settlement_periods_updated_at
    BEFORE UPDATE ON settlement_periods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_settlements_updated_at
    BEFORE UPDATE ON settlements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
