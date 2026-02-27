-- 동일 주문의 동일 이벤트 타입 중복 방지 (Kafka at-least-once 대응)
ALTER TABLE settlement_ledger
    ADD CONSTRAINT uq_ledger_order_event UNIQUE (order_number, event_type);
