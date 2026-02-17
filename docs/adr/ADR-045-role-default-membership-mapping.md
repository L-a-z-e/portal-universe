# ADR-045: Role-Default Membership Mapping

- **Status**: Accepted
- **Date**: 2026-02-18
- **Author**: Laze
- **Deciders**: Laze

## Context

Phase 2(ADR-044)에서 Role multi-include DAG가 완성되었다. 현재 신규 사용자 RBAC 초기화(`RbacInitializationService`)와 판매자 승인(`SellerApplicationService`)에서 멤버십 할당이 하드코딩되어 있다:

- `RbacInitializationService`: ROLE_USER 할당 후 user:blog/FREE, user:shopping/FREE 멤버십 직접 생성
- `SellerApplicationService`: ROLE_SHOPPING_SELLER 할당 후 seller:shopping/BRONZE 멤버십 직접 생성

이 접근법의 문제점:
1. 새 역할이나 멤버십 그룹 추가 시 코드 변경 필요
2. 매핑 관계가 코드에 산재하여 관리 어려움
3. 관리자가 런타임에 매핑을 변경할 수 없음

## Decision

### 1. `role_default_memberships` 테이블 기반 선언적 매핑

역할-멤버십 매핑을 DB 테이블로 관리한다. `role_default_memberships` 테이블이 "이 역할이 할당되면 이 멤버십 그룹의 이 티어를 기본 부여한다"를 정의한다.

### 2. Dual ApplicationEvent Handler 패턴

auth-service가 자기 자신의 Kafka 토픽을 소비하는 대신, Spring ApplicationEvent를 사용한다:

```
RbacService.assignRole() / SellerApplicationService.review()
  → applicationEventPublisher.publishEvent(RoleAssignedEvent)
      ├── [Handler 1] @EventListener (동기, 같은 트랜잭션)
      │     → MembershipAutoAssignHandler
      │     → role_default_memberships 조회 → UserMembership 자동 생성
      └── [Handler 2] @TransactionalEventListener(AFTER_COMMIT)
            → RoleAssignedKafkaPublisher
            → kafkaTemplate.send("auth.role.assigned", event)
```

- **Handler 1**: `@EventListener` — 동기 실행, 호출자 트랜잭션에 참여 (멤버십 생성이 원자적으로 커밋됨)
- **Handler 2**: `@TransactionalEventListener(AFTER_COMMIT)` — 트랜잭션 커밋 후 Kafka 발행 (외부 서비스 구독용)

### 3. Clean Switch (하드코딩 완전 제거)

기존 하드코딩 로직을 완전히 제거하고 이벤트 기반으로 전환한다:
- `RbacInitializationService.createDefaultMemberships()` 삭제
- `SellerApplicationService.assignSellerRoleAndMembership()` 리팩토링

## Consequences

### Positive
- 역할-멤버십 매핑이 DB 기반 SSOT로 관리됨
- 관리자 API를 통해 런타임 매핑 변경 가능
- 새 역할/멤버십 추가 시 코드 변경 없이 DB INSERT로 해결
- Kafka 이벤트로 외부 서비스가 역할 할당을 구독할 수 있음

### Negative
- DB 조회 1회 추가 (role_default_memberships), 캐싱으로 완화 가능
- 매핑 데이터가 없으면 멤버십이 생성되지 않으므로 초기 데이터 관리 필요

### Risks
- Flyway 마이그레이션으로 초기 데이터를 삽입하여 기존 동작 보장
- `MembershipAutoAssignHandler`는 `@Async` 금지 — 같은 트랜잭션 보장 필수

## Schema

```sql
CREATE TABLE role_default_memberships (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_key VARCHAR(50) NOT NULL,
  membership_group VARCHAR(50) NOT NULL,
  default_tier_key VARCHAR(50) NOT NULL,
  created_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_group (role_key, membership_group),
  CONSTRAINT fk_rdm_membership_tier
    FOREIGN KEY (membership_group, default_tier_key)
    REFERENCES membership_tiers (membership_group, tier_key)
);
```

## Initial Data

| role_key | membership_group | default_tier_key |
|----------|-----------------|-----------------|
| ROLE_USER | user:blog | FREE |
| ROLE_USER | user:shopping | FREE |
| ROLE_SHOPPING_SELLER | seller:shopping | BRONZE |
