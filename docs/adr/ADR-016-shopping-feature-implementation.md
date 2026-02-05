# ADR-016: Shopping Feature Implementation - Saga Pattern과 분산 트랜잭션 관리

**Status**: Accepted
**Date**: 2026-02-01
**Source**: PDCA archive (shopping)
**Supersedes**: shopping-service 내부 ADR-001 (Saga Pattern)

## Context

E-commerce 도메인에서 주문 생성은 재고 예약, 결제 처리, 배송 생성 등 여러 도메인 서비스 간 트랜잭션이 필요하다. 단일 DB 트랜잭션으로 처리할 수 없는 분산 환경에서, 일관성을 보장하면서도 부분 실패 시 보상(compensation)이 가능한 메커니즘이 필요했다.

또한 쿠폰 선착순 발급, 타임딜 동시 구매 등 고동시성 시나리오에서 재고 일관성과 성능을 동시에 확보해야 했다.

## Decision

**Saga Orchestration Pattern**을 채택하고, 동시성 제어는 **Pessimistic Lock + Redis Lua Script**를 혼합 사용한다.

## Rationale

- **Saga Pattern**: 5단계 순차 실행(RESERVE_INVENTORY → PROCESS_PAYMENT → DEDUCT_INVENTORY → CREATE_DELIVERY → CONFIRM_ORDER), 각 단계마다 보상 로직 정의
- **Pessimistic Lock (JPA)**: 재고 예약/차감 시 DB row lock으로 동시 접근 직렬화
- **Optimistic Lock (JPA @Version)**: 재고 충돌 감지용 추가 방어선
- **Redis Lua Script**: 쿠폰 발급, 타임딜 구매의 원자적 연산 (DECR + SADD를 atomic하게)
- **Event-Driven (Kafka)**: 주문 확정, 결제 완료 등 비동기 알림 발행

## Trade-offs

✅ **장점**:
- 분산 트랜잭션의 일관성 보장 (보상 트랜잭션으로 롤백 가능)
- 고동시성 환경에서 재고/쿠폰 정합성 유지
- Kafka 이벤트로 notification-service와 느슨한 결합

⚠️ **단점 및 완화**:
- Saga 실패 시 보상 로직 복잡도 증가 → SagaState 테이블로 상태 추적, compensation 자동화
- Pessimistic Lock으로 인한 성능 저하 → Redis 캐시로 읽기 부하 완화, Lock 대기 시간 최소화
- Lua Script 디버깅 어려움 → 단위 테스트 + E2E 검증 강화

## Implementation

- **Saga Orchestrator**: `services/shopping-service/src/main/java/.../order/saga/OrderSagaOrchestrator.java`
- **Pessimistic Lock**: `repository/InventoryRepository.java` - `@Lock(PESSIMISTIC_WRITE)`
- **Redis Lua**: `service/impl/CouponRedisService.java`, `TimeDealRedisService.java`
- **Event Publisher**: `event/EventPublisher.java` - Kafka 7개 토픽
- **E2E 테스트**: `e2e-tests/tests/shopping/checkout.spec.ts`, `order.spec.ts` (154개 테스트)

## References

- PDCA: `pdca/archive/2026-02/shopping/`
- 관련 시나리오: `docs/scenarios/SCENARIO-001-coupon-race.md`, `SCENARIO-002-order-saga.md`
- 아키텍처: Shopping Service 65+ endpoints, 10개 도메인 모듈
