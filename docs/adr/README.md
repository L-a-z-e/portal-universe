# Architecture Decision Records (ADR)

Portal Universe 프로젝트의 아키텍처 결정을 기록하는 ADR 문서입니다.

## ADR 목록

| ID | 제목 | 상태 | 작성일 |
|----|------|------|--------|
| ADR-001 | [Admin 컴포넌트 구조](./ADR-001-admin-component-structure.md) | Accepted | 2026-01-17 |
| ADR-002 | [Admin API 엔드포인트 설계](./ADR-002-api-endpoint-design.md) | Accepted | 2026-01-17 |
| ADR-003 | [Admin 권한 검증 전략](./ADR-003-authorization-strategy.md) | Accepted | 2026-01-17 |
| ADR-004 | [JWT RBAC 자동 설정 전략](./ADR-004-jwt-rbac-auto-configuration.md) | Accepted | 2026-01-19 |
| ADR-005 | [민감 데이터 관리 전략](./ADR-005-sensitive-data-management.md) | Accepted | 2026-01-19 |
| ADR-006 | [Config Service 및 Discovery Service 제거](./ADR-006-remove-config-service.md) | Accepted | 2026-01-20 |
| ADR-007 | [Elasticsearch 버전 업그레이드](./ADR-007-elasticsearch-version-upgrade.md) | Accepted | 2026-01-21 |
| ADR-008 | [JWT Stateless vs Redis](./ADR-008-jwt-stateless-redis.md) | Accepted | 2026-01-21 |
| ADR-009 | [Settings Page 아키텍처 설계](./ADR-009-settings-page-architecture.md) | Accepted | 2026-01-21 |
| ADR-010 | [보안 강화 아키텍처](./ADR-010-security-enhancement-architecture.md) | Accepted | 2026-01-25 |
| ADR-011 | [계층적 RBAC + 멤버십 시스템](./ADR-011-hierarchical-rbac-membership-system.md) | Proposed | 2026-01-28 |
| ADR-012 | [Shopping Frontend-Backend Gap Analysis](./ADR-012-shopping-frontend-backend-gap-analysis.md) | Accepted | 2026-01-28 |
| ADR-013 | [서비스 상태 모니터링 전략](./ADR-013-service-status-monitoring.md) | Accepted | 2026-01-21 |
| ADR-014 | [마이프로필 단계별 구현 전략](./ADR-014-my-profile-phased-approach.md) | Accepted | 2026-01-21 |

## 상태 정의

| 상태 | 설명 |
|------|------|
| **Proposed** | 제안됨, 검토 중 |
| **Accepted** | 승인됨, 현재 적용 중 |
| **Deprecated** | 폐기됨, 더 이상 적용 안 함 |
| **Superseded** | 대체됨, 새로운 ADR이 이를 대체 |

## 작성 가이드

ADR 작성 방법은 [docs_template/guide/adr/how-to-write.md](../../docs_template/guide/adr/how-to-write.md)를 참조하세요.

## 관련 문서

- [Architecture 문서](../architecture/)
- [API 명세서](../api/)
- [Scenarios](../scenarios/)
