-- payments 테이블을 payment-service로 이관 완료. shopping_db에서 제거.
DROP TRIGGER IF EXISTS trg_payments_updated_at ON payments;
DROP TABLE IF EXISTS payments;
