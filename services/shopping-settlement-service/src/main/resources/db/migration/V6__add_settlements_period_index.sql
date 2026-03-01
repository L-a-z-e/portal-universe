-- =============================================================
-- V6: settlements period_id 인덱스 추가 (성능 최적화 Phase 6)
-- =============================================================

-- Full Scan 방지: 정산 확인/지급 API (findByPeriodId)
CREATE INDEX idx_settlements_period_id ON settlements (period_id);
