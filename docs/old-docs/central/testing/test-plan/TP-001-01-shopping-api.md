---
id: TP-001-01
title: Shopping API 테스트 계획
type: test-plan
status: draft
created: 2026-01-19
updated: 2026-01-19
related:
  - PRD-001
---

# Shopping API 테스트 계획

## 개요

| 항목 | 내용 |
|------|------|
| **관련 PRD** | [PRD-001-ecommerce-core](../../prd/PRD-001-ecommerce-core.md) |
| **테스트 범위** | Shopping Service REST API |
| **테스트 환경** | Local (H2), CI (Testcontainers) |

## 테스트 목표

1. 모든 API 엔드포인트의 정상 동작 검증
2. 비즈니스 로직의 정확성 검증
3. 에러 처리 및 예외 상황 검증
4. 동시성 시나리오 검증

## 테스트 범위

### In Scope

- Product API (CRUD)
- Cart API
- Order API
- Inventory API
- Admin API

### Out of Scope

- UI 테스트 (별도 E2E 테스트 계획)
- 외부 연동 (결제사 등)
- 성능 테스트 (별도 계획)

## 테스트 케이스 요약

### Product API

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-001-01-01 | 상품 목록 조회 | P1 | Pending |
| TC-001-01-02 | 상품 상세 조회 | P1 | Pending |
| TC-001-01-03 | 상품 생성 (Admin) | P1 | Pending |
| TC-001-01-04 | 상품 수정 (Admin) | P1 | Pending |
| TC-001-01-05 | 상품 삭제 (Admin) | P2 | Pending |
| TC-001-01-06 | 존재하지 않는 상품 조회 | P2 | Pending |

### Cart API

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-001-01-10 | 장바구니 조회 | P1 | Pending |
| TC-001-01-11 | 장바구니에 상품 추가 | P1 | Pending |
| TC-001-01-12 | 장바구니 상품 수량 변경 | P1 | Pending |
| TC-001-01-13 | 장바구니 상품 삭제 | P1 | Pending |
| TC-001-01-14 | 품절 상품 추가 시도 | P2 | Pending |

### Order API

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-001-01-20 | 주문 생성 | P1 | Pending |
| TC-001-01-21 | 주문 목록 조회 | P1 | Pending |
| TC-001-01-22 | 주문 상세 조회 | P1 | Pending |
| TC-001-01-23 | 주문 취소 | P1 | Pending |
| TC-001-01-24 | 재고 부족 시 주문 실패 | P1 | Pending |
| TC-001-01-25 | 동시 주문 시 재고 동시성 | P1 | Pending |

### Inventory API

| TC ID | 시나리오 | 우선순위 | 상태 |
|-------|----------|----------|------|
| TC-001-01-30 | 재고 조회 | P1 | Pending |
| TC-001-01-31 | 재고 예약 | P1 | Pending |
| TC-001-01-32 | 재고 차감 | P1 | Pending |
| TC-001-01-33 | 재고 복원 | P1 | Pending |

## 합격 기준

| 항목 | 기준 |
|------|------|
| P1 테스트 통과율 | 100% |
| P2 테스트 통과율 | 95% |
| 코드 커버리지 | Service Layer 80% |
| Critical Bug | 0건 |

## 테스트 데이터

### 사전 조건 데이터

```sql
-- Test User
INSERT INTO users (id, email, username) VALUES (1, 'test@test.com', 'TestUser');

-- Test Products
INSERT INTO products (id, name, price, stock) VALUES
  (1, 'Test Product 1', 10000, 100),
  (2, 'Test Product 2', 20000, 50),
  (3, 'Out of Stock Product', 30000, 0);
```

### 테스트 데이터 관리

- `@Sql` 어노테이션으로 테스트별 데이터 주입
- `@Transactional`로 테스트 후 롤백
- Testcontainers로 격리된 DB 환경

## 테스트 실행

### 로컬 실행

```bash
cd services/shopping-service
./gradlew test
```

### 커버리지 리포트

```bash
./gradlew jacocoTestReport
# 리포트: build/reports/jacoco/test/html/index.html
```

## 일정

| 단계 | 예상 일정 |
|------|----------|
| 테스트 케이스 작성 | TBD |
| 테스트 코드 구현 | TBD |
| 테스트 실행 및 버그 수정 | TBD |
| 리포트 작성 | TBD |

## 관련 문서

- [테스트 전략](../test-strategy.md)
- [PRD-001 E-commerce Core](../../prd/PRD-001-ecommerce-core.md)
- [Learning - Domain Model](../../learning/notes/01-domain-model.md)
