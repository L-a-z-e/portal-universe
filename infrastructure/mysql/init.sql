-- =====================================================
-- MySQL 초기화 스크립트 (notification-service 전용)
-- 컨테이너 최초 시작 시 자동 실행
-- =====================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS notification_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 권한 부여
GRANT ALL PRIVILEGES ON notification_db.* TO 'laze'@'%';

-- MySQL Exporter 계정 (Prometheus 모니터링용)
CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'exporter';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';

FLUSH PRIVILEGES;
