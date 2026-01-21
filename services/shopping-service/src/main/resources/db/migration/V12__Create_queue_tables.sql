-- =====================================================
-- V12: 대기열 관련 테이블 생성
-- =====================================================

-- 대기열 설정 테이블
CREATE TABLE IF NOT EXISTS waiting_queues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL COMMENT '이벤트 유형 (TIMEDEAL, FLASH_SALE 등)',
    event_id BIGINT NOT NULL COMMENT '이벤트 ID (TimeDeal ID 등)',
    max_capacity INT NOT NULL COMMENT '동시 입장 가능 인원',
    entry_batch_size INT NOT NULL COMMENT '한 번에 입장시킬 인원',
    entry_interval_seconds INT NOT NULL COMMENT '입장 간격 (초)',
    is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT '활성화 여부',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at DATETIME COMMENT '활성화 일시',
    deactivated_at DATETIME COMMENT '비활성화 일시',
    INDEX idx_waiting_queues_event (event_type, event_id),
    INDEX idx_waiting_queues_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 대기열 엔트리 테이블
CREATE TABLE IF NOT EXISTS queue_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    queue_id BIGINT NOT NULL COMMENT '대기열 ID',
    user_id BIGINT NOT NULL COMMENT '사용자 ID',
    entry_token VARCHAR(36) NOT NULL UNIQUE COMMENT '고유 토큰 (UUID)',
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT '상태 (WAITING, ENTERED, EXPIRED, LEFT)',
    joined_at DATETIME NOT NULL COMMENT '대기열 참가 일시',
    entered_at DATETIME COMMENT '입장 일시',
    expired_at DATETIME COMMENT '만료 일시',
    left_at DATETIME COMMENT '이탈 일시',
    CONSTRAINT fk_queue_entry_queue FOREIGN KEY (queue_id) REFERENCES waiting_queues(id),
    INDEX idx_queue_entry_queue_user (queue_id, user_id),
    INDEX idx_queue_entry_token (entry_token),
    INDEX idx_queue_entry_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
