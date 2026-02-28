# Payment Service 아키텍처 문서

> Payment Service의 시스템 아키텍처 문서입니다.

---

## 문서 목록

| 문서 | 설명 |
|------|------|
| [system-overview.md](./system-overview.md) | 전체 아키텍처, 컴포넌트, 데이터 플로우 |

---

## 서비스 개요

| 항목 | 내용 |
|------|------|
| **포트** | :8090 |
| **DB** | payment_db (PostgreSQL) |
| **패턴** | Payment Intent + Transactional Outbox |
| **분리 일시** | 2026-02-28 (from shopping-service) |

---

## 관련 문서

- [Payment Service API](../../api/payment-service/README.md)
- [Payment Service DB Schema](../database/payment-service-schema.md)
- [ADR-055: Payment Service Extraction](../../adr/ADR-055-payment-service-extraction.md)

---

**최종 업데이트**: 2026-02-28
