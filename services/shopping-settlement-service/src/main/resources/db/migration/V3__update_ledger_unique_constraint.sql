-- 멀티셀러 정산 지원을 위한 UNIQUE constraint 변경
-- 기존: (order_number, event_type) → 주문당 이벤트 1건만 허용
-- 변경: (order_number, seller_id, event_type) → 주문+판매자당 이벤트 1건 허용

ALTER TABLE settlement_ledger DROP CONSTRAINT uq_ledger_order_event;

ALTER TABLE settlement_ledger
    ADD CONSTRAINT uq_ledger_order_seller_event UNIQUE (order_number, seller_id, event_type);
