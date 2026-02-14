# ADR-041: Shopping Service 분해 및 멀티셀러 마켓플레이스 전환

**Status**: Accepted
**Date**: 2026-02-14
**Author**: Laze
**Supersedes**: ADR-016 Shopping Feature Implementation (단일 서비스 구조)

## Context

shopping-service가 153개 Java 파일, 17개 테이블, 10개 도메인을 단일 서비스에 포함하며 모놀리식으로 성장했다.
Buyer(구매) 기능과 Seller/Admin(판매자 관리) 기능이 동일 서비스에 혼재되어 독립적 배포/스케일링이 불가능하고,
정산(Settlement) 시스템이 전혀 없어 실제 마켓플레이스 운영에 필수적인 판매자별 매출 집계/수수료 계산이 누락되어 있다.
멀티셀러 마켓플레이스로 확장하기 위해 서비스 분해가 필요하다.

## Decision

shopping-service를 3개 서비스로 분해하고, 멀티셀러 마켓플레이스 도메인 모델을 도입한다.

| 서비스 | 역할 | 포트 |
|--------|------|------|
| shopping-service | Buyer 쇼핑 경험 (Cart, Order, Payment, Delivery 조회, Search) | 8083 |
| shopping-seller-service | Seller 관리 (Seller, Product, Inventory, Coupon, TimeDeal, Queue) | 8088 |
| shopping-settlement-service | 정산 배치 (Settlement, Seed Data, Reconciliation) | 8089 |

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| 현 상태 유지 (단일 서비스) | 변경 없음, 트랜잭션 단순 | 스케일링 불가, 관심사 혼재, 정산 기능 추가 어려움 |
| Order/Payment 분리 우선 | 가장 독립적인 도메인부터 분리 | Seller 개념 미도입, 마켓플레이스 확장 불가 |
| **Buyer / Seller / Settlement 3분할** | 도메인 경계 명확, 멀티셀러 대응, 각각 독립 스케일링 | Saga 분산 복잡도 증가, 초기 개발 비용 |
| CQRS 기반 (Command/Query 분리) | 읽기/쓰기 독립 스케일링 | 기존 구조와 너무 다름, 학습 비용 높음 |

## Rationale

- **도메인 경계 일치**: Buyer(구매 경험)와 Seller(상품/재고 관리)는 비즈니스 도메인 자체가 다르다
- **독립 배포**: Seller 기능 수정이 Buyer 서비스에 영향을 주지 않음
- **멀티셀러 필수 기능**: Seller 엔티티, 판매자별 상품 관리, 정산은 마켓플레이스의 핵심 요구사항
- **배치 분리**: 정산 배치는 리소스 소모가 크므로 별도 서비스로 운영해야 주문/결제 서비스에 영향 없음
- **기존 코드 재활용**: Admin Controller/Page가 이미 분리되어 있어 이전 비용이 낮음

## Trade-offs

**장점:**
- Buyer/Seller 독립 배포 및 스케일링
- 멀티셀러 마켓플레이스 도메인 모델 도입
- 정산 배치가 운영 트래픽에 영향 없음
- 각 서비스 팀별 독립 개발 가능

**단점 및 완화:**
- Saga가 서비스 간 분산됨 -> 하이브리드 접근 (1차 Feign 기반 Orchestrator 유지, 2차 이벤트 Choreography 전환)
- Product/Inventory 공유 데이터 -> Schema 분리 + Feign internal API + 이벤트 동기화
- 서비스 간 네트워크 지연 -> Internal API에 circuit breaker + retry 적용 (Resilience4j 기존 사용 중)
- 초기 개발/마이그레이션 비용 -> Phase별 점진적 전환, 기존 Admin 코드 그대로 이전

## Implementation

- `services/shopping-seller-service/` -- Spring Boot 3.5.5, MySQL (shopping_seller_db), Redis, Kafka
- `services/shopping-settlement-service/` -- Spring Boot 3.5.5 + Spring Batch, MySQL (shopping_settlement_db)
- `frontend/shopping-seller-frontend/` -- React 18, Module Federation Remote (:30006)
- `services/shopping-service/` -- Admin 도메인 제거, Feign Client 추가
- `services/api-gateway/` -- `/api/v1/seller/**`, `/api/v1/settlement/**` 라우팅 추가

### Database Strategy

동일 MySQL 인스턴스에서 Schema(DB) 분리:
```
MySQL (:3307)
|-- shopping_db           -- Cart, Order, Payment, Delivery, UserCoupon, SagaState
|-- shopping_seller_db    -- Seller, Product, Inventory, Coupon, TimeDeal, Queue
|-- shopping_settlement_db -- Settlement, Spring Batch Meta
```

FK 제거 후 ID 참조만 사용. 서비스 간 데이터 조회는 Feign internal API 또는 Kafka 이벤트.

### Saga Hybrid Transition

Phase 1 (Feign-based):
```
shopping-service (OrderSagaOrchestrator) --Feign--> shopping-seller-service (/internal/inventory/*)
```

Phase 2 (Event-driven, 안정화 후):
```
shopping-service --Kafka(order.created)--> shopping-seller-service (재고 예약)
shopping-seller-service --Kafka(inventory.reserved)--> shopping-service (결제 진행)
```

## References

- ADR-016: Shopping Feature Implementation (현재 단일 서비스 구조)
- ADR-025: Distributed Data Consistency (Redis-DB 정합성)
- ADR-026: Saga Compensation Failure Policy (보상 실패 처리)
- ADR-027: Cart Stock Reservation Policy (장바구니 재고 예약)
- ADR-038: Polyglot Event Contract Management (이벤트 스키마)

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2026-02-14 | 구현 완료, Accepted로 변경 | Laze |
| 2026-02-14 | initial draft | Laze |
