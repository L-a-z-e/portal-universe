-- Idempotency 체크를 위한 인덱스 추가
CREATE INDEX idx_notification_ref
ON notifications (reference_id, reference_type, user_id);
