-- ===================================================================
-- V1: Create notification tables
-- ===================================================================

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    link VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME,
    INDEX idx_notification_user_status (user_id, status),
    INDEX idx_notification_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
