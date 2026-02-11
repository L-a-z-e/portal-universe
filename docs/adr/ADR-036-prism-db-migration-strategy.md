# ADR-036: Prism 서비스 DB 마이그레이션 전략

**Status**: Proposed
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: -

## Context

Prism 서비스는 현재 TypeORM의 `synchronize: true` 옵션으로 개발 환경에서 엔티티 변경 시 자동으로 스키마를 동기화한다. 그러나 프로덕션 환경에서는 `synchronize: false`로 설정되어 있어 스키마 변경 방법이 없는 상태다. Portal Universe의 모든 Java 서비스는 Flyway를 사용하여 SQL 기반 마이그레이션을 관리하고 있으나, NestJS 기반 Prism 서비스만 마이그레이션 도구가 부재하다.

## Decision

**TypeORM Migration CLI**를 채택하여 모든 환경에서 `synchronize: false`로 통일하고, 마이그레이션 파일로 스키마를 관리한다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① TypeORM Migration CLI | 현재 TypeORM 유지, 추가 의존성 없음, `migration:generate`로 SQL 자동 생성 | TypeORM에 종속, 생성된 마이그레이션이 가끔 불필요한 변경 포함 |
| ② Flyway (Node.js 호출) | Java 서비스와 도구 통일, SQL 직접 작성으로 예측 가능 | Node 프로젝트에 Java 도구 의존, TypeORM 엔티티와 SQL 이중 관리 |
| ③ Prisma 전환 | 우수한 마이그레이션 UX, 타입 안전, `prisma db push/pull` | ORM 전면 교체 비용 (전체 엔티티/쿼리 재작성), 대규모 변경 |
| ④ dbmate (Go CLI) | 언어 독립적, 단순한 up/down SQL | 외부 바이너리 의존, TypeORM 엔티티와 SQL 이중 관리 |

## Rationale

- **기존 코드베이스 유지**: TypeORM을 이미 사용 중이므로 별도 도구 도입 없이 마이그레이션 기능만 활성화
- **자동 생성으로 생산성 향상**: `migration:generate`가 엔티티 diff를 분석하여 SQL을 자동 생성, 수동 작성 부담 최소화
- **프로덕션 안전성**: `synchronize: false`로 전환하여 예상치 못한 스키마 변경 방지, 명시적 마이그레이션으로 변경 이력 추적
- **Java 서비스와의 일관성**: Flyway와 동일하게 순차적 버전 관리 (V1, V2, ...), 롤백 SQL 관리
- **팀 학습 비용 최소화**: TypeORM을 이미 숙지한 상태, 신규 도구 학습 불필요

## Trade-offs

✅ **장점**:
- 추가 의존성 없이 TypeORM 내장 기능 활용
- `migration:generate`로 엔티티 변경 감지 및 SQL 자동 생성
- 모든 환경에서 동일한 스키마 관리 프로세스 (local/docker/k8s 일관성)
- Git으로 마이그레이션 이력 추적 가능
- Java 서비스와 유사한 버전 관리 패턴 (순차적 파일명)

⚠️ **단점 및 완화**:
- TypeORM에 종속 → (완화: NestJS 생태계에서 표준 ORM, 추후 Prisma 전환 시에도 마이그레이션 파일 재사용 가능)
- `migration:generate`가 불필요한 변경 포함 가능 → (완화: 생성된 SQL을 반드시 검토 후 커밋, `migration:create`로 수동 작성 병행)
- 초기 baseline 마이그레이션 생성 필요 → (완화: `init-prism.sql`을 기준으로 baseline 마이그레이션 작성)

## Implementation

- `services/prism-service/src/config/configuration.ts` - `synchronize: false`로 전환 (모든 환경)
- `services/prism-service/src/config/typeorm.config.ts` - Migration CLI용 DataSource 설정 (신규)
- `services/prism-service/src/migrations/` - 마이그레이션 파일 디렉토리 (신규)
- `services/prism-service/package.json` - `migration:generate`, `migration:run`, `migration:revert` scripts 추가
- `services/prism-service/src/app.module.ts` - TypeORM 설정에 `migrations: [__dirname + '/migrations/**/*.ts']` 추가

## References

- ADR-017: Prism AI Agent 칸반 시스템 (PostgreSQL 5테이블 설계)
- `infrastructure/init-scripts/init-prism.sql` (현재 초기 스키마)
- `services/auth-service/src/main/resources/db/migration/` (Flyway 참고)
- TypeORM Migration 공식 문서: https://typeorm.io/migrations

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 | Laze |
