-- orders 테이블에 payment_intent_id 컬럼 추가 (Payment Intent 패턴)
ALTER TABLE orders ADD COLUMN payment_intent_id VARCHAR(36);

CREATE INDEX idx_order_payment_intent_id ON orders(payment_intent_id);
