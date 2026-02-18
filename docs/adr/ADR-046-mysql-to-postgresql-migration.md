# ADR-046: MySQL to PostgreSQL Migration

- **Status**: Accepted
- **Date**: 2026-02-18
- **Author**: Laze

## Context

MySQL이 랜섬웨어 공격을 받아 모든 데이터베이스가 삭제되었다.

**근본 원인 분석:**
1. Docker 포트가 `0.0.0.0`으로 바인딩되어 외부에서 직접 접근 가능
2. macOS 방화벽 비활성화 상태
3. 취약한 비밀번호 (`root/root`)

기존에 auth-service, shopping-service, shopping-seller-service, shopping-settlement-service가 MySQL을 사용하고 있었으며, prism-service와 drive-service는 이미 PostgreSQL을 사용 중이었다.

## Decision

### DB 마이그레이션
- **notification-service만 MySQL 유지**, 나머지 4개 서비스를 PostgreSQL로 전환
- 이유: notification-service는 단순 CRUD 위주로 MySQL이 적합하고, 나머지 서비스들은 이미 PostgreSQL을 사용하는 서비스(prism, drive)와 동일 인프라를 공유하는 것이 효율적

### 인프라 보안 강화
- 모든 Docker 포트를 `127.0.0.1`로 바인딩 (외부 접근 차단)
- 비밀번호 강화: MySQL `Portal2026!`, PostgreSQL/MongoDB `Laze2026!`

### Flyway 마이그레이션 전략
- 기존 V1~Vn 파일을 최종 상태의 V1__init.sql 하나로 통합
- DB 엔진이 변경되므로 flyway_schema_history가 없어 V1부터 새로 시작

### PostgreSQL 전환 규칙
| MySQL | PostgreSQL |
|-------|-----------|
| `AUTO_INCREMENT` | `GENERATED ALWAYS AS IDENTITY` |
| `enum('A','B')` | `VARCHAR(50)` |
| `ON UPDATE CURRENT_TIMESTAMP` | PostgreSQL Trigger |
| `datetime` | `TIMESTAMP` |
| `tinyint(1)` | `BOOLEAN` |

## Consequences

### Positive
- PostgreSQL로 통합하여 인프라 복잡도 감소 (MySQL은 notification-service 전용으로 축소)
- `127.0.0.1` 바인딩으로 외부 직접 접근 원천 차단
- 비밀번호 강화로 보안 수준 향상
- MySQL 컨테이너 리소스 절감 (1GB → 512MB)

### Negative
- Polyglot DB 유지 (MySQL, PostgreSQL, MongoDB) - 완전 통합은 아님
- 기존 MySQL 기반 쿼리/설정 파일 전면 변경 필요

### Neutral
- `updated_at` 자동 갱신을 위해 PostgreSQL Trigger 사용 (prism-service 기존 패턴 동일)
- Flyway 마이그레이션 히스토리 초기화 (새 DB이므로 자연스러움)

## Affected Services

| Service | Before | After |
|---------|--------|-------|
| auth-service | MySQL | **PostgreSQL** |
| shopping-service | MySQL | **PostgreSQL** |
| shopping-seller-service | MySQL | **PostgreSQL** |
| shopping-settlement-service | MySQL | **PostgreSQL** |
| notification-service | MySQL | MySQL (유지) |
| prism-service | PostgreSQL | PostgreSQL (유지) |
| drive-service | PostgreSQL | PostgreSQL (유지) |
