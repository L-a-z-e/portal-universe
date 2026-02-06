# PostgreSQL Best Practices

**난이도:** ⭐⭐⭐⭐

## 학습 목표
- PostgreSQL 네이밍 컨벤션 및 스키마 설계 원칙 습득
- 백업 및 복구 전략 수립 (pg_dump, pg_restore)
- 보안 강화 (Role, GRANT, Row-Level Security)
- Vacuum 및 유지보수 관리
- 운영 환경 최적화 팁
- Portal Universe 프로덕션 환경 가이드

---

## 1. 네이밍 컨벤션

### 1.1 기본 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| **테이블** | 복수형, snake_case | `products`, `order_items` |
| **컬럼** | 단수형, snake_case | `user_id`, `created_at` |
| **기본 키** | `id` | `id BIGSERIAL PRIMARY KEY` |
| **외래 키** | `{참조테이블}_id` | `category_id`, `user_id` |
| **인덱스** | `idx_{테이블}_{컬럼}` | `idx_products_category` |
| **유니크 인덱스** | `idx_{테이블}_{컬럼}_unique` | `idx_users_email_unique` |
| **외래 키 제약** | `fk_{테이블}_{참조테이블}` | `fk_order_items_orders` |
| **체크 제약** | `chk_{테이블}_{컬럼}` | `chk_orders_status` |
| **시퀀스** | `{테이블}_id_seq` | `products_id_seq` |
| **트리거** | `{테이블}_{동작}` | `products_updated_at` |
| **함수** | snake_case, 동사 시작 | `update_updated_at_column()` |

### 1.2 예시

```sql
-- ✅ 좋은 예
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_created_at ON products(created_at DESC);

ALTER TABLE products
ADD CONSTRAINT fk_products_categories
FOREIGN KEY (category_id) REFERENCES categories(id);

-- ❌ 나쁜 예
CREATE TABLE Product (  -- 대문자 사용
    ID BIGSERIAL PRIMARY KEY,  -- 대문자
    ProductName VARCHAR(255),  -- 카멜케이스
    CategoryID BIGINT,  -- 카멜케이스
    isDeleted BOOLEAN,  -- 카멜케이스
    CreatedAt TIMESTAMP  -- 카멜케이스
);
```

### 1.3 예약어 회피

```sql
-- ❌ 나쁜 예: 예약어 사용
CREATE TABLE user (  -- 'user'는 예약어
    id BIGSERIAL PRIMARY KEY,
    order VARCHAR(255)  -- 'order'는 예약어
);

-- ✅ 좋은 예
CREATE TABLE users (  -- 복수형으로 회피
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(255)  -- 명확한 이름
);
```

---

## 2. 스키마 설계 원칙

### 2.1 정규화 vs 역정규화

**정규화 (Normalization)**

```sql
-- 정규화 (3NF)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- 장점: 데이터 중복 없음, 업데이트 용이
-- 단점: JOIN 필요, 읽기 성능 저하
```

**역정규화 (Denormalization)**

```sql
-- 역정규화
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(255) NOT NULL  -- 역정규화
);

-- 장점: JOIN 없이 빠른 조회
-- 단점: 데이터 중복, 업데이트 시 동기화 필요
```

**Portal Universe 전략:**

```sql
-- 하이브리드 접근
CREATE TABLE blog_posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author_id BIGINT NOT NULL,  -- 정규화 (변경 가능)
    author_name VARCHAR(255) NOT NULL,  -- 역정규화 (빠른 조회)
    view_count BIGINT DEFAULT 0,  -- 역정규화 (집계)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- author_name은 트리거로 동기화
CREATE OR REPLACE FUNCTION sync_author_name()
RETURNS TRIGGER AS $$
BEGIN
    SELECT name INTO NEW.author_name
    FROM users
    WHERE id = NEW.author_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER blog_posts_sync_author
BEFORE INSERT OR UPDATE OF author_id ON blog_posts
FOR EACH ROW
EXECUTE FUNCTION sync_author_name();
```

### 2.2 데이터 타입 선택

| 용도 | 나쁜 선택 | 좋은 선택 | 이유 |
|------|----------|----------|------|
| **Boolean** | `CHAR(1)`, `SMALLINT` | `BOOLEAN` | 명확성, 공간 효율 |
| **정수** | `INTEGER` (충분할 때) | `BIGINT` | ID는 BIGINT 권장 |
| **금액** | `FLOAT`, `DOUBLE` | `NUMERIC(10,2)` | 정확성 (부동소수점 오류 방지) |
| **짧은 문자열** | `TEXT` | `VARCHAR(n)` | 제약 조건 명확 |
| **긴 문자열** | `VARCHAR(65535)` | `TEXT` | 유연성 |
| **날짜** | `VARCHAR` | `DATE`, `TIMESTAMP` | 타입 안전성, 연산 가능 |
| **JSON** | `TEXT` | `JSONB` | 인덱싱, 성능 |
| **UUID** | `CHAR(36)` | `UUID` | 네이티브 타입, 공간 효율 |

**예시:**

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,  -- BIGINT (향후 확장성)
    order_number VARCHAR(50) NOT NULL,  -- 고정 길이
    total_amount NUMERIC(10, 2) NOT NULL,  -- 금액 (정확성)
    is_paid BOOLEAN DEFAULT FALSE,  -- Boolean (명확성)
    metadata JSONB,  -- JSON (인덱싱 가능)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 날짜/시간
);
```

### 2.3 NOT NULL 제약

```sql
-- ✅ 필수 필드는 NOT NULL
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,  -- 필수
    name VARCHAR(255) NOT NULL,  -- 필수
    phone VARCHAR(20),  -- 선택
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 최적화 (NULL 제외)
CREATE INDEX idx_users_phone ON users(phone) WHERE phone IS NOT NULL;
```

### 2.4 기본값 설정

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL DEFAULT 0,  -- 기본값
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,  -- 기본값
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 기본값
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. 백업 및 복구

### 3.1 pg_dump (논리적 백업)

**전체 데이터베이스 백업:**

```bash
# 기본 백업
pg_dump -U postgres shopping_db > shopping_db_backup.sql

# 압축 백업 (Custom Format, 권장)
pg_dump -U postgres -Fc shopping_db > shopping_db_backup.dump

# 압축 백업 (gzip)
pg_dump -U postgres shopping_db | gzip > shopping_db_backup.sql.gz

# 원격 서버 백업
pg_dump -h remote-host -U postgres -Fc shopping_db > backup.dump
```

**특정 테이블만 백업:**

```bash
# 단일 테이블
pg_dump -U postgres -t products shopping_db > products_backup.sql

# 여러 테이블
pg_dump -U postgres -t products -t categories shopping_db > backup.sql

# 스키마만 (데이터 제외)
pg_dump -U postgres -s shopping_db > schema_only.sql

# 데이터만 (스키마 제외)
pg_dump -U postgres -a shopping_db > data_only.sql
```

**고급 옵션:**

```bash
# 병렬 백업 (빠름)
pg_dump -U postgres -Fd -j 4 shopping_db -f backup_dir/

# INSERT 문으로 백업 (호환성)
pg_dump -U postgres --inserts shopping_db > backup.sql

# 컬럼명 포함 INSERT
pg_dump -U postgres --column-inserts shopping_db > backup.sql
```

### 3.2 pg_restore (복구)

```bash
# Custom Format 복구
pg_restore -U postgres -d shopping_db shopping_db_backup.dump

# 데이터베이스 재생성 후 복구
dropdb -U postgres shopping_db
createdb -U postgres shopping_db
pg_restore -U postgres -d shopping_db shopping_db_backup.dump

# 특정 테이블만 복구
pg_restore -U postgres -d shopping_db -t products backup.dump

# 병렬 복구 (빠름)
pg_restore -U postgres -d shopping_db -j 4 backup.dump

# 복구 전 기존 객체 삭제
pg_restore -U postgres -d shopping_db --clean backup.dump

# 복구 시 에러 무시
pg_restore -U postgres -d shopping_db --no-owner --no-acl backup.dump
```

### 3.3 연속 아카이빙 (WAL Archiving)

**postgresql.conf:**

```conf
# WAL 아카이빙 활성화
wal_level = replica
archive_mode = on
archive_command = 'cp %p /var/lib/postgresql/wal_archive/%f'
archive_timeout = 3600  # 1시간마다 강제 아카이브

# 백업
max_wal_senders = 3
```

**베이스 백업:**

```bash
# 베이스 백업 (온라인 백업)
pg_basebackup -U postgres -D /backup/base -Ft -z -P

# 설명:
# -D: 백업 디렉토리
# -Ft: tar 포맷
# -z: 압축
# -P: 진행 상황 표시
```

**복구:**

```bash
# 1. PostgreSQL 중지
systemctl stop postgresql

# 2. 데이터 디렉토리 초기화
rm -rf /var/lib/postgresql/data/*

# 3. 베이스 백업 복원
tar -xzf /backup/base/base.tar.gz -C /var/lib/postgresql/data/

# 4. WAL 아카이브 복사
cp /var/lib/postgresql/wal_archive/* /var/lib/postgresql/data/pg_wal/

# 5. recovery.conf 생성
cat > /var/lib/postgresql/data/recovery.conf <<EOF
restore_command = 'cp /var/lib/postgresql/wal_archive/%f %p'
recovery_target_time = '2024-06-15 10:00:00'
EOF

# 6. PostgreSQL 시작
systemctl start postgresql
```

### 3.4 자동 백업 스크립트

```bash
#!/bin/bash
# /usr/local/bin/backup_postgres.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/postgresql"
DB_NAME="shopping_db"
RETENTION_DAYS=7

# 디렉토리 생성
mkdir -p $BACKUP_DIR

# 백업 실행
pg_dump -U postgres -Fc $DB_NAME > $BACKUP_DIR/${DB_NAME}_${DATE}.dump

# 압축
gzip $BACKUP_DIR/${DB_NAME}_${DATE}.dump

# 오래된 백업 삭제 (7일 이상)
find $BACKUP_DIR -name "*.dump.gz" -mtime +$RETENTION_DAYS -delete

# S3 업로드 (선택)
# aws s3 cp $BACKUP_DIR/${DB_NAME}_${DATE}.dump.gz s3://my-bucket/backups/

echo "Backup completed: ${DB_NAME}_${DATE}.dump.gz"
```

**크론 등록:**

```bash
# 매일 새벽 2시 백업
crontab -e
0 2 * * * /usr/local/bin/backup_postgres.sh >> /var/log/postgres_backup.log 2>&1
```

---

## 4. 보안 강화

### 4.1 Role 및 권한 관리

**기본 Role 생성:**

```sql
-- 읽기 전용 Role
CREATE ROLE readonly;
GRANT CONNECT ON DATABASE shopping_db TO readonly;
GRANT USAGE ON SCHEMA public TO readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO readonly;

-- 애플리케이션 Role
CREATE ROLE app_user WITH LOGIN PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE shopping_db TO app_user;
GRANT USAGE ON SCHEMA public TO app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO app_user;

-- 관리자 Role
CREATE ROLE admin WITH LOGIN PASSWORD 'admin_password' SUPERUSER;
```

**사용자 생성:**

```sql
-- 애플리케이션 사용자
CREATE USER shopping_app WITH PASSWORD 'app_secure_password';
GRANT app_user TO shopping_app;

-- 읽기 전용 사용자 (분석용)
CREATE USER analytics_user WITH PASSWORD 'analytics_password';
GRANT readonly TO analytics_user;
```

**권한 확인:**

```sql
-- Role 확인
SELECT rolname, rolsuper, rolcanlogin FROM pg_roles;

-- 테이블 권한 확인
SELECT
    grantee,
    table_name,
    privilege_type
FROM information_schema.table_privileges
WHERE table_schema = 'public'
ORDER BY grantee, table_name;

-- 권한 취소
REVOKE INSERT, UPDATE, DELETE ON products FROM readonly;
```

### 4.2 Row-Level Security (RLS)

**사용자별 데이터 격리:**

```sql
-- RLS 활성화
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- 정책 생성: 자신의 주문만 조회 가능
CREATE POLICY orders_user_isolation ON orders
FOR SELECT
TO app_user
USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- 정책 생성: 자신의 주문만 수정 가능
CREATE POLICY orders_user_update ON orders
FOR UPDATE
TO app_user
USING (user_id = current_setting('app.current_user_id')::BIGINT)
WITH CHECK (user_id = current_setting('app.current_user_id')::BIGINT);

-- 관리자는 모든 데이터 접근
CREATE POLICY orders_admin_all ON orders
FOR ALL
TO admin
USING (true);

-- 애플리케이션에서 사용
-- SET SESSION app.current_user_id = 123;
-- SELECT * FROM orders;  -- user_id = 123인 주문만 조회
```

**Spring Boot 통합:**

```java
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        // RLS 컨텍스트 설정
        jdbcTemplate.execute(
            "SET LOCAL app.current_user_id = " + userId
        );

        // RLS 정책이 자동 적용됨
        return orderRepository.findAll().stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }
}
```

### 4.3 연결 보안

**postgresql.conf:**

```conf
# SSL 활성화
ssl = on
ssl_cert_file = '/etc/ssl/certs/server.crt'
ssl_key_file = '/etc/ssl/private/server.key'
ssl_ca_file = '/etc/ssl/certs/ca.crt'

# 연결 제한
max_connections = 100
listen_addresses = 'localhost'  # 로컬만 허용 (프로덕션: 특정 IP)
```

**pg_hba.conf:**

```conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD

# 로컬 연결 (Unix socket)
local   all             postgres                                peer

# IPv4 local connections
host    all             all             127.0.0.1/32            scram-sha-256

# 애플리케이션 서버만 허용
host    shopping_db     app_user        10.0.1.0/24             scram-sha-256

# SSL 강제
hostssl shopping_db     all             0.0.0.0/0               scram-sha-256

# 읽기 전용 사용자
host    shopping_db     readonly        10.0.2.0/24             scram-sha-256

# 거부
host    all             all             0.0.0.0/0               reject
```

**비밀번호 정책:**

```sql
-- 비밀번호 만료
ALTER ROLE app_user VALID UNTIL '2025-12-31';

-- 비밀번호 변경
ALTER ROLE app_user WITH PASSWORD 'new_password';

-- 연결 제한
ALTER ROLE app_user CONNECTION LIMIT 20;
```

---

## 5. Vacuum 관리

### 5.1 Auto Vacuum 최적화

**postgresql.conf:**

```conf
# Auto Vacuum 설정
autovacuum = on
autovacuum_max_workers = 3  # CPU 코어 수 고려
autovacuum_naptime = 1min
autovacuum_vacuum_threshold = 50
autovacuum_vacuum_scale_factor = 0.2
autovacuum_analyze_threshold = 50
autovacuum_analyze_scale_factor = 0.1

# 대용량 테이블
autovacuum_vacuum_cost_delay = 10ms
autovacuum_vacuum_cost_limit = 200

# 로깅
log_autovacuum_min_duration = 0  # 모든 autovacuum 로그
```

**테이블별 설정:**

```sql
-- 자주 업데이트되는 테이블 (공격적 Vacuum)
ALTER TABLE products SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.02
);

-- 큰 테이블 (느린 Vacuum)
ALTER TABLE orders SET (
    autovacuum_vacuum_cost_delay = 20,
    autovacuum_vacuum_cost_limit = 100
);

-- 로그 테이블 (Vacuum 비활성화)
ALTER TABLE event_logs SET (
    autovacuum_enabled = false
);
```

### 5.2 수동 Vacuum 전략

```bash
#!/bin/bash
# 주간 Vacuum 스크립트

# 1. 통계 수집
psql -U postgres -d shopping_db -c "ANALYZE;"

# 2. 일반 Vacuum
psql -U postgres -d shopping_db -c "VACUUM VERBOSE;"

# 3. Full Vacuum (주말 새벽)
if [ $(date +%u) -eq 7 ]; then
    psql -U postgres -d shopping_db -c "VACUUM FULL VERBOSE products;"
fi

# 크론: 매주 일요일 새벽 3시
# 0 3 * * 0 /usr/local/bin/weekly_vacuum.sh
```

### 5.3 Bloat 모니터링

```sql
-- Bloat 확인
CREATE EXTENSION IF NOT EXISTS pgstattuple;

SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS total_size,
    round(100 * pgstattuple(schemaname || '.' || tablename)::record::pgstattuple_type.dead_tuple_percent, 2) AS dead_tuple_percent
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;

-- Bloat 10% 이상이면 Vacuum 필요
```

---

## 6. 운영 환경 최적화

### 6.1 postgresql.conf 튜닝

```conf
# 메모리 설정
shared_buffers = 4GB  # 총 메모리의 25%
effective_cache_size = 12GB  # 총 메모리의 75%
work_mem = 64MB  # 동시 연결 수 고려
maintenance_work_mem = 1GB  # Vacuum, CREATE INDEX

# 연결
max_connections = 100
superuser_reserved_connections = 3

# WAL
wal_buffers = 16MB
checkpoint_completion_target = 0.9
max_wal_size = 4GB
min_wal_size = 1GB

# 쿼리 플래닝
random_page_cost = 1.1  # SSD 환경
effective_io_concurrency = 200

# 로깅
logging_collector = on
log_directory = '/var/log/postgresql'
log_filename = 'postgresql-%Y-%m-%d.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_min_duration_statement = 1000  # 1초 이상 쿼리 로그
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_statement = 'ddl'

# 성능
shared_preload_libraries = 'pg_stat_statements'
```

### 6.2 모니터링

**pg_stat_statements 활성화:**

```sql
-- 확장 설치
CREATE EXTENSION pg_stat_statements;

-- 느린 쿼리 Top 10
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 가장 많이 실행된 쿼리
SELECT
    query,
    calls,
    total_exec_time
FROM pg_stat_statements
ORDER BY calls DESC
LIMIT 10;

-- 통계 리셋
SELECT pg_stat_statements_reset();
```

**시스템 뷰:**

```sql
-- 활성 쿼리
SELECT
    pid,
    usename,
    application_name,
    state,
    query_start,
    NOW() - query_start AS duration,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;

-- 테이블 통계
SELECT
    schemaname,
    tablename,
    n_live_tup,
    n_dead_tup,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;

-- 인덱스 사용 통계
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### 6.3 알림 설정

```sql
-- 긴 트랜잭션 알림
SELECT
    pid,
    usename,
    NOW() - xact_start AS duration,
    state,
    query
FROM pg_stat_activity
WHERE xact_start < NOW() - INTERVAL '10 minutes'
  AND state != 'idle';

-- Disk 사용량 알림
SELECT
    pg_database.datname,
    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database
WHERE pg_database_size(pg_database.datname) > 10737418240;  -- 10GB
```

---

## 7. Portal Universe 프로덕션 가이드

### 7.1 환경별 설정

**application-production.yml:**

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://postgres-primary:5432/shopping_db?ssl=true&sslmode=require
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

    hikari:
      maximum-pool-size: 30
      minimum-idle: 15
      connection-timeout: 20000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
      pool-name: ShoppingProdPool

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # 프로덕션: validate만
    properties:
      hibernate:
        format_sql: false
        show_sql: false
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
    open-in-view: false

  flyway:
    enabled: true
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false

logging:
  level:
    org.hibernate.SQL: WARN
    org.hibernate.orm.jdbc.bind: WARN
    com.zaxxer.hikari: INFO
```

### 7.2 배포 체크리스트

| 항목 | 확인 사항 |
|------|----------|
| **백업** | 자동 백업 스크립트 실행 확인 |
| **복제** | Replica 서버 동기화 확인 |
| **모니터링** | Prometheus, Grafana 설정 |
| **알림** | Slack, Email 알림 설정 |
| **연결 풀** | HikariCP 설정 확인 |
| **인덱스** | 필수 인덱스 생성 확인 |
| **Vacuum** | Auto Vacuum 설정 확인 |
| **보안** | SSL, RLS, 방화벽 설정 |
| **로깅** | 느린 쿼리 로그 활성화 |
| **스케일링** | Read Replica 준비 |

### 7.3 장애 대응

**시나리오 1: 느린 쿼리**

```sql
-- 1. 활성 쿼리 확인
SELECT * FROM pg_stat_activity WHERE state != 'idle';

-- 2. 느린 쿼리 종료
SELECT pg_cancel_backend(12345);  -- PID

-- 3. 쿼리 분석
EXPLAIN ANALYZE <slow_query>;

-- 4. 인덱스 추가
CREATE INDEX CONCURRENTLY idx_fix ON table(column);
```

**시나리오 2: 디스크 부족**

```bash
# 1. 디스크 사용량 확인
df -h

# 2. 큰 테이블 확인
psql -c "SELECT pg_size_pretty(pg_total_relation_size('orders'));"

# 3. Vacuum Full
psql -c "VACUUM FULL orders;"

# 4. 오래된 데이터 아카이브
```

**시나리오 3: 연결 부족**

```sql
-- 1. 연결 수 확인
SELECT COUNT(*) FROM pg_stat_activity;

-- 2. max_connections 증가
ALTER SYSTEM SET max_connections = 200;
SELECT pg_reload_conf();

-- 3. 유휴 연결 종료
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND state_change < NOW() - INTERVAL '1 hour';
```

---

## 8. 핵심 정리

| 항목 | Best Practice |
|------|---------------|
| **네이밍** | snake_case, 복수형 테이블 |
| **타입** | BIGINT (ID), NUMERIC (금액), BOOLEAN (플래그) |
| **백업** | 일일 pg_dump + WAL 아카이빙 |
| **보안** | Role 기반 권한, RLS, SSL |
| **Vacuum** | Auto Vacuum 활성화, 주기적 수동 Vacuum |
| **모니터링** | pg_stat_statements, 느린 쿼리 로그 |
| **연결 풀** | HikariCP 최적화 |

---

## 9. 실습 예제

### 실습 1: Role 및 권한 설정

```sql
-- 1. Role 생성
CREATE ROLE app_readonly;
GRANT CONNECT ON DATABASE shopping_db TO app_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;

-- 2. 사용자 생성
CREATE USER analytics WITH PASSWORD 'secure_password';
GRANT app_readonly TO analytics;

-- 3. 확인
\du
SELECT * FROM information_schema.table_privileges WHERE grantee = 'app_readonly';
```

### 실습 2: 백업 및 복구

```bash
# 백업
pg_dump -U postgres -Fc shopping_db > backup.dump

# 복구
pg_restore -U postgres -d shopping_db_restored backup.dump

# 검증
psql -U postgres shopping_db_restored -c "SELECT COUNT(*) FROM products;"
```

---

## 10. 관련 문서

- [PostgreSQL 인덱싱](./postgresql-indexing.md)
- [PostgreSQL 트랜잭션](./postgresql-transactions.md)
- [PostgreSQL 성능 튜닝](./postgresql-performance-tuning.md)
- [PostgreSQL 마이그레이션](./postgresql-migration.md)

---

## 11. 참고 자료

- [PostgreSQL Official Documentation](https://www.postgresql.org/docs/)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/security.html)
- [PostgreSQL Backup and Restore](https://www.postgresql.org/docs/current/backup.html)
- [PostgreSQL Wiki](https://wiki.postgresql.org/)
