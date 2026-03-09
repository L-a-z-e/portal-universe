-- Portal Universe - PostgreSQL Database Initialization
-- 서비스별 DB 생성 (CREATE DATABASE만 담당, 테이블 생성은 각 서비스의 Flyway/TypeORM이 담당)
-- PostgreSQL 18

SELECT 'CREATE DATABASE auth_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec

SELECT 'CREATE DATABASE shopping_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_db')\gexec

SELECT 'CREATE DATABASE shopping_seller_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_seller_db')\gexec

SELECT 'CREATE DATABASE shopping_settlement_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shopping_settlement_db')\gexec

SELECT 'CREATE DATABASE prism_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'prism_db')\gexec

SELECT 'CREATE DATABASE drive_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'drive_db')\gexec

SELECT 'CREATE DATABASE payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec

-- PostgreSQL Exporter 전용 모니터링 사용자
-- pg_monitor 역할: pg_stat_* 뷰 읽기 권한 포함
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'postgres_exporter') THEN
        CREATE ROLE postgres_exporter WITH LOGIN PASSWORD 'exporter_pass';
        GRANT pg_monitor TO postgres_exporter;
    END IF;
END
$$;
