# Shopping Seller Service Architecture

> Shopping Seller Service 아키텍처 문서 디렉토리

---

## 📋 문서 목록

| 문서 | 설명 | 작성일 |
|------|------|--------|
| [System Overview](./system-overview.md) | 전체 시스템 아키텍처 및 도메인 구조 | 2026-02-14 |

---

## 도메인 개요

Shopping Seller Service는 판매자 및 상품 관리를 담당하는 마이크로서비스로, 3개 핵심 도메인을 포함합니다:

1. **Seller**: 판매자 등록, 정보 관리, 승인 프로세스
2. **Product**: 판매자별 상품 CRUD, Internal API 제공
3. **Inventory**: 재고 관리, Saga 패턴 재고 예약/차감/해제

---

## 기술 스택

- Java 17, Spring Boot 3.5.5
- MySQL 8.0 (shopping_seller_db)
- Redis + Redisson (분산 락, Lua Script)
- Spring Kafka (이벤트 구독)
- Pessimistic Lock (재고 동시성 제어)

---

## 관련 문서

### Architecture
- [Shopping Service Architecture](../shopping-service/) - Buyer 도메인
- [Shopping Settlement Service Architecture](../shopping-settlement-service/) - 정산 도메인

### API
- [Shopping Seller Service API](../../api/shopping-seller-service/README.md)

---

**최종 업데이트**: 2026-02-14
