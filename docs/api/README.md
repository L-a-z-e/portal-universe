# API Documentation Index

Portal Universe의 API 명세 문서 목록입니다.

## 문서 목록

| 서비스 | 문서 | Base URL | 상태 |
|--------|------|----------|------|
| Auth | [Security & Authentication API](./security-api-reference.md) | `http://localhost:8080` | 완료 |
| Shopping | [Shopping Service API](./shopping-api-reference.md) | `http://localhost:8080/api/v1/shopping` | 완료 |
| Shopping | [Coupon API](./coupon-api.md) | `http://localhost:8080/api/v1/shopping/coupons` | 완료 |
| Shopping | [TimeDeal API](./timedeal-api.md) | `http://localhost:8080/api/v1/shopping/time-deals` | 완료 |
| Shopping | [Admin Products API](./admin-products-api.md) | `http://localhost:8080/api/v1/shopping/admin/products` | 통합됨 |

## 에러 코드 범위

| 서비스 | Prefix | 범위 |
|--------|--------|------|
| Common | C | C001 ~ C099 |
| Auth | A | A001 ~ A099 |
| Blog | B | B001 ~ B099 |
| Shopping | S | S001 ~ S099 |
| Notification | N | N001 ~ N099 |

## 관련 문서

- [ADR 목록](../adr/) - 아키텍처 결정 기록
- [Scenarios](../scenarios/) - 업무 시나리오
