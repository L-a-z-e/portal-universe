# ADR (Architecture Decision Records)

Portal Universe 프로젝트의 아키텍처 결정을 기록합니다.

## 목록

| ID      | 제목                                                                                            | 상태         | 날짜         |
|---------|-----------------------------------------------------------------------------------------------|------------|------------|
| ADR-001 | [Admin 컴포넌트 구조](./ADR-001-admin-component-structure.md)                                       | Accepted   | 2026-01-17 |
| ADR-002 | [Admin API 엔드포인트 설계](./ADR-002-api-endpoint-design.md)                                        | Accepted   | 2026-01-17 |
| ADR-003 | [Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)                                         | Accepted   | 2026-01-17 |
| ADR-004 | [JWT RBAC 자동 설정 전략](./ADR-004-jwt-rbac-auto-configuration.md)                                 | Accepted   | 2026-01-19 |
| ADR-005 | [민감 데이터 관리 전략](./ADR-005-sensitive-data-management.md)                                        | Accepted   | 2026-01-19 |
| ADR-006 | [Config Service 및 Discovery Service 제거](./ADR-006-remove-config-service.md)                   | Accepted   | 2026-01-20 |
| ADR-007 | [Elasticsearch 버전 업그레이드](./ADR-007-elasticsearch-version-upgrade.md)                          | Accepted   | 2026-01-19 |
| ADR-008 | [JWT Stateless + Redis 인증 전환](./ADR-008-jwt-stateless-redis.md)                               | Accepted   | 2026-01-21 |
| ADR-009 | [Settings Page 아키텍처](./ADR-009-settings-page-architecture.md)                                 | Accepted   | 2026-01-21 |
| ADR-010 | [보안 강화 아키텍처](./ADR-010-security-enhancement-architecture.md)                                  | Accepted   | 2026-01-23 |
| ADR-011 | [계층적 RBAC + 멤버십 시스템](./ADR-011-hierarchical-rbac-membership-system.md)                        | Superseded | 2026-01-28 |
| ADR-012 | [Shopping Frontend-Backend Gap Analysis](./ADR-012-shopping-frontend-backend-gap-analysis.md) | Accepted   | 2026-01-28 |
| ADR-013 | [서비스 상태 모니터링 전략](./ADR-013-service-status-monitoring.md)                                      | Accepted   | 2026-01-21 |
| ADR-014 | [마이프로필 단계별 구현 전략](./ADR-014-my-profile-phased-approach.md)                                    | Accepted   | 2026-01-21 |
| ADR-015 | [Role Hierarchy 구현 방안](./ADR-015-role-hierarchy-implementation.md)                            | Accepted   | 2026-01-31 |
| ADR-016 | [Shopping Saga Pattern과 분산 트랜잭션](./ADR-016-shopping-feature-implementation.md)                | Accepted   | 2026-02-01 |
| ADR-017 | [Prism AI Agent 칸반 시스템](./ADR-017-prism-basic-implementation.md)                              | Accepted   | 2026-02-01 |
| ADR-018 | [Design System Single Source of Truth](./ADR-018-design-system-architecture.md)               | Accepted   | 2026-02-01 |
| ADR-019 | [Frontend Design Refactoring](./ADR-019-frontend-design-refactoring.md)                       | Accepted   | 2026-02-01 |
| ADR-020 | [Redis Sorted Set 대기열 시스템](./ADR-020-shopping-queue-system.md)                                | Accepted   | 2026-01-19 |
| ADR-021 | [역할+서비스 복합 멤버십 재구조화](./ADR-021-role-based-membership-restructure.md)                          | Accepted   | 2026-02-07 |
| ADR-022 | [auth-service 도메인 경계 재정의](./ADR-022-auth-service-domain-boundary.md)                          | Deprecated | 2026-02-07 |
| ADR-023 | [API Response Wrapper 표준화](./ADR-023-api-response-wrapper-standardization.md)                 | Deprecated | 2026-02-07 |
| ADR-024 | [Controller 인증 파라미터 표준화](./ADR-024-controller-auth-parameter-standardization.md)              | Accepted   | 2026-02-07 |
| ADR-025 | [Shopping Service 분산 데이터 정합성 전략](./ADR-025-distributed-data-consistency.md)                   | Proposed   | 2026-02-07 |
| ADR-026 | [Saga 보상 액션 실패 처리 정책](./ADR-026-saga-compensation-failure-policy.md)                          | Proposed   | 2026-02-07 |
| ADR-027 | [장바구니 재고 예약 정책](./ADR-027-cart-stock-reservation-policy.md)                                   | Accepted   | 2026-02-07 |
| ADR-028 | [SSE 실시간 엔드포인트 인증 방식](./ADR-028-sse-endpoint-authentication.md)                               | Accepted   | 2026-02-07 |
| ADR-029 | [Cross-cutting 보안 처리 계층 설계](./ADR-029-cross-cutting-security-layer.md)                        | Proposed   | 2026-02-07 |
| ADR-030 | [환경별 보안 프로파일 정책](./ADR-030-environment-security-profile.md)                                   | Accepted   | 2026-02-07 |
| ADR-031 | [Unified API Response Strategy](./ADR-031-unified-api-response-strategy.md)                   | Accepted   | 2026-02-08 |
| ADR-032 | [Kafka Configuration Standardization](./ADR-032-kafka-configuration-standardization.md)       | Accepted   | 2026-02-10 |
| ADR-033 | [Polyglot 서비스 관찰성 통일 전략](./ADR-033-polyglot-observability-strategy.md)                   | Accepted   | 2026-02-11 |
| ADR-034 | [비Java 서비스 CI/CD 파이프라인 통합](./ADR-034-non-java-cicd-integration.md)                 | Accepted   | 2026-02-13 |
| ADR-035 | [Polyglot 서비스 인증 표준화](./ADR-035-polyglot-authentication-standardization.md)             | Accepted   | 2026-02-11 |
| ADR-036 | [Prism 서비스 DB 마이그레이션 전략](./ADR-036-prism-db-migration-strategy.md)                      | Proposed   | 2026-02-11 |
| ADR-037 | [NestJS(Prism) 서비스 장기 스택 전략](./ADR-037-nestjs-prism-long-term-strategy.md)              | Proposed   | 2026-02-11 |
| ADR-038 | [Polyglot 이벤트 계약 관리 전략](./ADR-038-polyglot-event-contract-management.md)                 | Accepted   | 2026-02-13 |
| ADR-039 | [JWT 이중 검증 전략](./ADR-039-jwt-dual-validation-strategy.md)                                 | Accepted   | 2026-02-11 |
| ADR-040 | [Frontend Error Handling Standardization](./ADR-040-frontend-error-handling-standardization.md) | Accepted   | 2026-02-14 |


## 상태 정의

| 상태 | 설명 |
|------|------|
| **Proposed** | 제안됨, 검토 중 |
| **Accepted** | 승인됨, 현재 적용 중 |
| **Deprecated** | 폐기됨, 더 이상 적용 안 함 |
| **Superseded** | 대체됨, 새로운 ADR이 이를 대체 |

## 작성 가이드

새 ADR 작성 시 [ADR 템플릿](../templates/adr-template.md)을 참조하세요.

## 관련 문서

- [Architecture 문서](../architecture/)
- [API 명세서](../api/)
