-- ===================================================================
-- TIMESTAMP → TIMESTAMPTZ 마이그레이션
-- 기존 TIMESTAMP 데이터를 KST로 해석하여 TIMESTAMPTZ(UTC)로 변환
-- ===================================================================

-- settlement_periods
ALTER TABLE settlement_periods ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlement_periods ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- settlements
ALTER TABLE settlements ALTER COLUMN paid_at    TYPE TIMESTAMPTZ USING paid_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlements ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlements ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'Asia/Seoul';

-- settlement_details (updated_at 컬럼 추가)
ALTER TABLE settlement_details ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlement_details ADD COLUMN updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TRIGGER trg_settlement_details_updated_at
    BEFORE UPDATE ON settlement_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- settlement_ledger (updated_at 컬럼 추가)
ALTER TABLE settlement_ledger ALTER COLUMN event_at    TYPE TIMESTAMPTZ USING event_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlement_ledger ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'Asia/Seoul';
ALTER TABLE settlement_ledger ADD COLUMN updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE TRIGGER trg_settlement_ledger_updated_at
    BEFORE UPDATE ON settlement_ledger
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
