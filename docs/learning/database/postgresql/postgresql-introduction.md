# PostgreSQL 소개

## 학습 목표
- PostgreSQL의 특징과 강점 이해
- MySQL과의 주요 차이점 파악
- Docker Compose 기반 로컬 환경 구성
- Portal Universe 적용 시나리오 탐색

---

## 1. PostgreSQL이란?

PostgreSQL은 **객체-관계형 데이터베이스 관리 시스템(ORDBMS)**입니다. 1986년 UC Berkeley에서 시작된 오픈소스 프로젝트로, 강력한 표준 SQL 준수와 확장성을 제공합니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **ACID 보장** | 트랜잭션 무결성 완전 지원 |
| **MVCC** | Multi-Version Concurrency Control로 높은 동시성 |
| **확장성** | Extension을 통한 기능 확장 (PostGIS, pgcrypto 등) |
| **JSONB** | Binary JSON 저장 및 인덱싱 |
| **Array** | 배열 데이터 타입 네이티브 지원 |
| **Full-Text Search** | 내장된 전문 검색 |
| **Window Functions** | 고급 분석 쿼리 지원 |
| **CTE** | Common Table Expressions (WITH 절) |

### PostgreSQL vs MySQL 간단 비교

| 항목 | PostgreSQL | MySQL |
|------|-----------|-------|
| **라이선스** | PostgreSQL License (MIT 유사) | GPL (Community) / Commercial |
| **동시성** | MVCC (우수) | Locking (InnoDB MVCC 지원) |
| **JSON** | JSONB (binary, 인덱싱) | JSON (text, 제한적) |
| **Array** | ✅ 네이티브 지원 | ❌ 미지원 |
| **Full-Text** | ✅ 내장 | ✅ 내장 (제한적) |
| **Window Functions** | ✅ 완전 지원 | ✅ MySQL 8.0+ |
| **학습 곡선** | 중간~높음 | 낮음 |
| **읽기 성능** | 우수 | 매우 우수 |
| **쓰기 성능** | 우수 | 우수 |
| **복잡한 쿼리** | 매우 우수 | 보통 |

---

## 2. PostgreSQL 주요 개념

### 2.1 MVCC (Multi-Version Concurrency Control)

PostgreSQL은 MVCC를 사용하여 읽기-쓰기 충돌을 최소화합니다.

```
트랜잭션 1 (읽기)  ──────────────────────>
                    |
트랜잭션 2 (쓰기)  ─────────┐              |
                            └──> 블로킹 없음!
```

**핵심 특징:**
- 읽기 작업은 쓰기 작업을 블로킹하지 않음
- 쓰기 작업은 읽기 작업을 블로킹하지 않음
- 스냅샷 격리 (Snapshot Isolation)

### 2.2 Extension

PostgreSQL은 Extension을 통해 기능을 확장할 수 있습니다.

```sql
-- PostGIS (지리 정보)
CREATE EXTENSION postgis;

-- pgcrypto (암호화)
CREATE EXTENSION pgcrypto;

-- uuid-ossp (UUID 생성)
CREATE EXTENSION "uuid-ossp";

-- pg_trgm (유사도 검색)
CREATE EXTENSION pg_trgm;
```

### 2.3 Schema

PostgreSQL은 데이터베이스 내에 스키마(네임스페이스)를 지원합니다.

```sql
-- 스키마 생성
CREATE SCHEMA shopping;
CREATE SCHEMA auth;

-- 스키마별 테이블
CREATE TABLE shopping.products (...);
CREATE TABLE auth.users (...);
```

---

## 3. Portal Universe 적용 시나리오

### 3.1 현재 데이터베이스 구성

| 서비스 | 현재 DB | 특징 |
|--------|---------|------|
| **auth-service** | MySQL | 단순 CRUD, 높은 일관성 |
| **shopping-service** | MySQL | 복잡한 도메인, 트랜잭션 중요 |
| **blog-service** | MongoDB | Document 모델, 유연한 스키마 |
| **notification-service** | MySQL | Queue 관리 |

### 3.2 PostgreSQL 전환 고려 대상

#### ✅ Shopping Service (높은 우선순위)

**전환 이유:**
- 복잡한 쿼리 및 분석 (Window Functions, CTE)
- 상품 메타데이터 (JSONB 활용)
- 태그 배열 (Array 타입)
- 전문 검색 (Full-Text Search)

**예시:**
```sql
-- products 테이블
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    stock INTEGER NOT NULL,
    metadata JSONB,  -- {"color": "red", "size": ["S","M"], "features": {...}}
    tags TEXT[],     -- ['sale', 'new', 'bestseller']
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- JSONB 쿼리
SELECT * FROM products
WHERE metadata->>'color' = 'red';

-- Array 쿼리
SELECT * FROM products
WHERE 'sale' = ANY(tags);
```

#### 🔶 Notification Service (중간 우선순위)

**전환 이유:**
- Queue 관리에 LISTEN/NOTIFY 활용
- Array 타입으로 수신자 목록 관리

#### ❌ Auth Service (전환 불필요)

**유지 이유:**
- 단순 CRUD 위주
- MySQL 성능 충분
- 마이그레이션 비용 대비 이득 적음

---

## 4. Docker Compose 환경 구성

### 4.1 docker-compose.yml 추가

Portal Universe의 `docker-compose.yml`에 PostgreSQL 추가:

```yaml
services:
  # 기존 서비스들...

  postgres:
    image: postgres:16-alpine
    container_name: portal-postgres
    environment:
      POSTGRES_USER: portal
      POSTGRES_PASSWORD: portal123
      POSTGRES_DB: shopping_db
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d
    networks:
      - portal-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U portal"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
```

### 4.2 초기화 스크립트

`docker/postgres/init/01-init-shopping.sql`:

```sql
-- 데이터베이스 생성 (이미 POSTGRES_DB로 생성됨)

-- Extension 추가
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 스키마 생성 (옵션)
-- CREATE SCHEMA shopping;

-- 기본 테이블 (예시)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0,
    metadata JSONB,
    tags TEXT[] DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_products_name ON products USING gin(name gin_trgm_ops);
CREATE INDEX idx_products_metadata ON products USING gin(metadata);
CREATE INDEX idx_products_tags ON products USING gin(tags);
```

### 4.3 PostgreSQL 시작

```bash
# PostgreSQL 컨테이너 시작
docker-compose up -d postgres

# 연결 확인
docker exec -it portal-postgres psql -U portal -d shopping_db
```

---

## 5. 기본 CLI 명령어 (psql)

### 5.1 psql 접속

```bash
# 로컬
psql -U portal -d shopping_db

# Docker
docker exec -it portal-postgres psql -U portal -d shopping_db
```

### 5.2 메타 명령어

| 명령어 | 설명 |
|--------|------|
| `\l` | 데이터베이스 목록 |
| `\c database_name` | 데이터베이스 전환 |
| `\dt` | 테이블 목록 |
| `\d table_name` | 테이블 구조 |
| `\di` | 인덱스 목록 |
| `\df` | 함수 목록 |
| `\dn` | 스키마 목록 |
| `\du` | 사용자(Role) 목록 |
| `\dx` | Extension 목록 |
| `\q` | 종료 |

### 5.3 기본 쿼리

```sql
-- 데이터베이스 목록
SELECT datname FROM pg_database;

-- 테이블 크기 확인
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 현재 연결 수
SELECT count(*) FROM pg_stat_activity;

-- 버전 확인
SELECT version();
```

---

## 6. Spring Boot 연결 (간단 예시)

### 6.1 의존성 (build.gradle)

```gradle
dependencies {
    // PostgreSQL Driver
    runtimeOnly 'org.postgresql:postgresql'

    // Spring Data JPA
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
}
```

### 6.2 application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/shopping_db
    username: portal
    password: portal123
    driver-class-name: org.postgresql.Driver

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Flyway 사용 시
    properties:
      hibernate:
        format_sql: true
        default_schema: public
```

---

## 7. PostgreSQL vs MySQL 선택 기준

### 7.1 PostgreSQL을 선택해야 하는 경우

| 상황 | 이유 |
|------|------|
| **복잡한 쿼리** | CTE, Window Functions, Subquery 최적화 우수 |
| **JSONB 활용** | NoSQL 유연성 + RDBMS 일관성 |
| **높은 동시성** | MVCC로 읽기-쓰기 충돌 최소화 |
| **데이터 무결성** | 엄격한 제약 조건, 트리거 |
| **확장성** | Extension 생태계 |
| **전문 검색** | 내장 Full-Text Search |

### 7.2 MySQL을 유지해야 하는 경우

| 상황 | 이유 |
|------|------|
| **단순 CRUD** | MySQL 읽기 성능 우수, 운영 간편 |
| **레거시 시스템** | 마이그레이션 비용 |
| **팀 숙련도** | MySQL 경험 풍부 |
| **읽기 중심** | MySQL 읽기 최적화 우수 |

### 7.3 Portal Universe 권장 전략

```
Phase 1: Shopping Service 전환 (3~6개월)
├── PostgreSQL 학습 및 테스트
├── 스키마 마이그레이션
├── 애플리케이션 코드 수정
└── 성능 테스트 및 최적화

Phase 2: Notification Service 검토 (옵션)
└── LISTEN/NOTIFY 활용

Phase 3: Auth Service 유지
└── MySQL 유지 (전환 불필요)
```

---

## 8. 핵심 요약

- [ ] PostgreSQL은 **ORDBMS**로 강력한 표준 SQL과 확장성 제공
- [ ] **MVCC**로 높은 동시성, 읽기-쓰기 충돌 최소화
- [ ] **JSONB, Array, Full-Text Search** 등 고급 기능 지원
- [ ] **Extension**으로 기능 확장 가능
- [ ] Portal Universe에서 **Shopping Service 전환 고려**
- [ ] **복잡한 쿼리, JSONB 활용** 시 PostgreSQL 유리
- [ ] **단순 CRUD** 시 MySQL 충분

---

## 관련 문서

- 다음: [PostgreSQL SQL 기초](./postgresql-sql-fundamentals.md)
- 비교: [MySQL vs PostgreSQL](./mysql-vs-postgresql.md)
- 통합: [PostgreSQL Spring 통합](./postgresql-spring-integration.md)

---

## 참고 자료

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [PostgreSQL vs MySQL 비교](https://www.postgresql.org/about/featurematrix/)
- [Spring Data JPA + PostgreSQL](https://spring.io/guides/gs/accessing-data-jpa/)
