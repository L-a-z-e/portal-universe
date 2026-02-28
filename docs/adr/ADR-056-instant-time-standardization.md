# ADR-056: Instant 시간 표준화

**Status**: Accepted
**Date**: 2026-02-28
**Author**: Laze

---

## Context

Polyglot 마이크로서비스 환경(Java/NestJS/Python)에서 시간대 정보가 없는 `LocalDateTime`(Java), `timestamp without time zone`(PostgreSQL), `timestamp`(TypeORM)를 혼용하여 서비스 간 시간 데이터의 의미론적 불일치가 발생하였다. 특히 분산 환경에서 서비스 인스턴스별 JVM 시간대 설정이 다를 경우 동일 시각이 서로 다른 값으로 저장·조회될 수 있으며, Kafka 이벤트를 통한 서비스 간 시간 비교 시 오류가 발생할 위험이 있었다.

## Decision

모든 Java 서비스에서 날짜-시간 타입을 `LocalDateTime` → `Instant`로 전환하고, PostgreSQL 컬럼 타입을 `TIMESTAMP` → `TIMESTAMPTZ`(timestamp with time zone)로 마이그레이션한다. Avro 스키마의 시간 논리 타입은 `timestamp-millis`로 통일한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① 현행 유지 (LocalDateTime) | 변경 비용 없음 | 서비스 간 시간 불일치 지속, equals() 비교 불안전 |
| ② ZonedDateTime 사용 | 시간대 명시적 표현 | 직렬화 복잡성 증가, DB 매핑 추가 설정 필요 |
| ③ Instant (선택) | UTC 기준 단일 표현, JS Date/Python datetime(UTC)와 동치 | 가독성 약간 저하 (epoch 기반) |

## Rationale

- **UTC 기준 일관성**: `Instant`는 항상 UTC를 기준으로 하여 서버 시간대와 무관하게 동일한 값을 보장한다
- **JVM/DB 독립성**: JVM `-Duser.timezone` 설정이나 DB 서버 시간대에 영향받지 않는다
- **Polyglot 동치성**: JavaScript `Date`와 Python `datetime(tzinfo=UTC)`와 동일한 의미론적 표현을 공유한다
- **equals() 안전성**: `LocalDateTime.equals()`는 시간대 고려 없이 비교하여 오류를 유발하지만, `Instant.equals()`는 절대 시각 기준으로 안전하다
- **TIMESTAMPTZ 저장**: PostgreSQL `TIMESTAMPTZ`는 UTC로 정규화하여 저장하고 조회 시 세션 시간대로 변환하여 정확성을 보장한다

## Trade-offs

✅ **장점**:
- 모든 서비스에서 시간 비교 및 정렬의 정확성 보장
- Kafka 이벤트의 `timestamp-millis` 필드와 일관된 시간 표현
- 분산 시스템에서 글로벌 사용자 서비스 확장 시 시간대 이슈 사전 차단
- Spring Data JPA와 PostgreSQL JDBC 드라이버가 `Instant` ↔ `TIMESTAMPTZ` 매핑을 네이티브 지원

⚠️ **단점 및 완화**:
- [가독성 저하] Instant는 human-readable하지 않음 → (완화: 로그 및 API 응답에서는 ISO-8601 포맷으로 직렬화)
- [마이그레이션 비용] 기존 컬럼 타입 변경 필요 → (완화: PostgreSQL `TIMESTAMP → TIMESTAMPTZ` 타입 변환은 데이터 손실 없이 ALTER TABLE로 수행 가능)
- [notification-service 제외] MySQL 유지 서비스는 `DATETIME` 타입 그대로 사용 → (완화: 해당 서비스는 독립적으로 시간대 정책 적용, 외부 이벤트 경계에서 변환)

## Implementation

### Java 서비스
- `LocalDateTime` 필드를 `Instant`로 전환 (auth, shopping, shopping-seller, shopping-settlement 서비스)
- JPA `@Column` 어노테이션 타입 매핑 자동 적용 (Spring Data JPA + PostgreSQL JDBC)

### PostgreSQL 마이그레이션
- auth-service: `V2__timestamp_to_timestamptz.sql`
- shopping-service: `V8__timestamp_to_timestamptz.sql`
- shopping-seller-service: `V2__timestamp_to_timestamptz.sql`
- shopping-settlement-service: `V4__timestamp_to_timestamptz.sql`

### NestJS (prism-service)
- TypeORM 엔티티 컬럼 타입 `timestamp` → `timestamptz` 전환

### Avro 이벤트 계약
- 시간 필드: `"logicalType": "timestamp-millis"` 통일 적용

### 제외 대상
- notification-service: MySQL `DATETIME` 유지 (ADR-046 결정에 따라 MySQL 유지)

## References

- [ADR-046: MySQL → PostgreSQL 마이그레이션](./ADR-046-mysql-to-postgresql-migration.md)
- [ADR-047: Avro 및 Schema Registry 도입](./ADR-047-avro-schema-registry-adoption.md)
- [ADR-038: Polyglot 이벤트 계약 관리 전략](./ADR-038-polyglot-event-contract-management.md)
- [PostgreSQL TIMESTAMPTZ 공식 문서](https://www.postgresql.org/docs/current/datatype-datetime.html)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-28 | 초안 작성 | Laze |
