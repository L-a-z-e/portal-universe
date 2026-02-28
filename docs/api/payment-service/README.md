# Payment Service API Documentation

> Payment Service의 API 엔드포인트 명세서입니다.

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/payment` |
| **포트** | :8090 |
| **역할** | 결제 의향(Intent) 관리, 결제 처리, 환불/취소 |
| **인증** | Bearer Token (JWT) |
| **버전** | v1 |

> **2026-02-28 신규**: Shopping Service에서 독립 분리된 결제 전용 서비스입니다. Payment Intent 패턴 적용.

---

## API 목록

| 문서 | 설명 |
|------|------|
| [payment-api.md](./payment-api.md) | Intent 관리, 결제 확인/조회/취소/환불 |

---

## 관련 문서

- [Payment Service Architecture](../../architecture/payment-service/system-overview.md)
- [ADR-055: Payment Service Extraction](../../adr/ADR-055-payment-service-extraction.md)
- [Shopping Service API](../shopping-service/README.md)

---

**최종 업데이트**: 2026-02-28
