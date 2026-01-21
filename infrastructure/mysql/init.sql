-- =====================================================
-- MySQL 초기화 스크립트
-- 컨테이너 최초 시작 시 자동 실행
-- =====================================================

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS shopping_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 권한 부여
GRANT ALL PRIVILEGES ON auth_db.* TO 'laze'@'%';
GRANT ALL PRIVILEGES ON shopping_db.* TO 'laze'@'%';
FLUSH PRIVILEGES;
