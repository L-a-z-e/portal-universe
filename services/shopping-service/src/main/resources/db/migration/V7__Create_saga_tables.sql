-- V7__Create_saga_tables.sql
-- Saga 상태 추적 테이블

CREATE TABLE saga_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    saga_id VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    order_number VARCHAR(30) NOT NULL,
    current_step VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    completed_steps VARCHAR(500),
    last_error_message VARCHAR(1000),
    compensation_attempts INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE UNIQUE INDEX idx_saga_id ON saga_states(saga_id);
CREATE INDEX idx_saga_order_id ON saga_states(order_id);
CREATE INDEX idx_saga_status ON saga_states(status);
