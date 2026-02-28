-- 동일 주문에 대한 Saga 중복 생성 방지
ALTER TABLE saga_states
    ADD CONSTRAINT uq_saga_order_number UNIQUE (order_number);
