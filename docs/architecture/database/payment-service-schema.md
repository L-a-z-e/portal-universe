---
id: arch-db-payment-service-schema
title: Payment Service Database Schema
type: architecture
status: current
created: 2026-02-28
updated: 2026-02-28
author: Laze
tags: [database, postgresql, payment-service, schema]
related:
  - arch-payment-service-overview
  - ADR-055-payment-service-extraction
  - ADR-046-mysql-to-postgresql-migration
---

# Payment Service Database Schema

**Database**: PostgreSQL (payment_db)
**Table Count**: 3 (payment_intents, payments, outbox_events)
**Last Updated**: 2026-02-28

> **2026-02-28 신규**: Shopping Service에서 독립 분리된 결제 전용 DB입니다. payments 테이블은 shopping_db에서 제거(`V7__drop_payments_table.sql`)되고 payment_db로 이전되었습니다.

---

## ERD

```mermaid
erDiagram
    PaymentIntent {
        BIGINT id PK "GENERATED ALWAYS AS IDENTITY"
        VARCHAR(36) intent_id UK "UUID"
        VARCHAR(30) order_number "주문 번호"
        VARCHAR(100) user_id "사용자 ID"
        DECIMAL(12,2) amount "결제 금액"
        VARCHAR(30) status "REQUIRES_PAYMENT | PROCESSING | SUCCEEDED | FAILED | CANCELLED | EXPIRED"
        JSONB metadata "메타데이터 (선택)"
        TIMESTAMPTZ expires_at "만료 시각"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    Payment {
        BIGINT id PK "GENERATED ALWAYS AS IDENTITY"
        VARCHAR(30) payment_number UK "PAY-{8자리}"
        VARCHAR(36) intent_id FK "PaymentIntent.intent_id 참조"
        VARCHAR(30) order_number "주문 번호"
        VARCHAR(100) user_id "사용자 ID"
        DECIMAL(12,2) amount "결제 금액"
        VARCHAR(20) status "PENDING | PROCESSING | COMPLETED | FAILED | CANCELLED | REFUNDED"
        VARCHAR(30) payment_method "CARD | BANK_TRANSFER | KAKAO_PAY | NAVER_PAY"
        VARCHAR(100) pg_transaction_id "PG 트랜잭션 ID (PG- 또는 RF-)"
        TEXT pg_response "PG 응답 원문 (JSON)"
        VARCHAR(500) failure_reason "실패 사유"
        TIMESTAMPTZ paid_at "결제 완료 시각"
        TIMESTAMPTZ refunded_at "환불 완료 시각"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ updated_at
    }

    OutboxEvent {
        BIGINT id PK "GENERATED ALWAYS AS IDENTITY"
        VARCHAR(50) aggregate_type "집계 타입 (PAYMENT)"
        VARCHAR(100) aggregate_id "집계 ID (paymentNumber)"
        VARCHAR(200) event_type "이벤트 FQCN"
        VARCHAR(100) topic "Kafka 토픽"
        VARCHAR(100) event_key "Kafka 메시지 키"
        TEXT payload "Avro 직렬화 이벤트 (JSON)"
        VARCHAR(20) status "PENDING | PUBLISHED | FAILED"
        INT retry_count "재시도 횟수"
        TIMESTAMPTZ created_at
        TIMESTAMPTZ published_at "발행 완료 시각"
    }

    PaymentIntent ||--o| Payment : "confirmed as"
```

---

## 테이블 상세

### payment_intents

Payment Intent 패턴의 핵심 테이블. 서버가 결제 금액을 확정하고 클라이언트에 intentId를 전달합니다.

| 컬럼 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | BIGINT | Y | PK (GENERATED ALWAYS AS IDENTITY) |
| `intent_id` | VARCHAR(36) | Y | UUID (UNIQUE) |
| `order_number` | VARCHAR(30) | Y | 주문 번호 |
| `user_id` | VARCHAR(100) | Y | 사용자 ID |
| `amount` | DECIMAL(12,2) | Y | 결제 금액 (서버 확정) |
| `status` | VARCHAR(30) | Y | 상태 (기본: REQUIRES_PAYMENT) |
| `metadata` | JSONB | N | 추가 메타데이터 |
| `expires_at` | TIMESTAMPTZ | Y | 만료 시각 (생성 + 30분) |
| `created_at` | TIMESTAMPTZ | Y | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | N | 수정 시각 (트리거 자동 갱신) |

**인덱스**:

| 인덱스명 | 컬럼 | 목적 |
|---------|------|------|
| `uq_intent_id` | intent_id | UNIQUE, Intent 조회 |
| `idx_intent_order_number` | order_number | 주문별 Intent 조회 |
| `idx_intent_user_id` | user_id | 사용자별 Intent 조회 |
| `idx_intent_status_expires` | (status, expires_at) WHERE status = 'REQUIRES_PAYMENT' | Expiry Scheduler 최적화 |

---

### payments

실제 결제 처리 결과를 기록합니다.

| 컬럼 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | BIGINT | Y | PK (GENERATED ALWAYS AS IDENTITY) |
| `payment_number` | VARCHAR(30) | Y | 결제 번호 `PAY-{UUID 앞 8자리 대문자}` (UNIQUE) |
| `intent_id` | VARCHAR(36) | Y | FK → payment_intents.intent_id |
| `order_number` | VARCHAR(30) | Y | 주문 번호 |
| `user_id` | VARCHAR(100) | Y | 사용자 ID |
| `amount` | DECIMAL(12,2) | Y | 결제 금액 |
| `status` | VARCHAR(20) | Y | 결제 상태 (기본: PENDING) |
| `payment_method` | VARCHAR(30) | Y | 결제 수단 |
| `pg_transaction_id` | VARCHAR(100) | N | PG 트랜잭션 ID |
| `pg_response` | TEXT | N | PG 응답 원문 (JSON 문자열) |
| `failure_reason` | VARCHAR(500) | N | 실패/취소 사유 |
| `paid_at` | TIMESTAMPTZ | N | 결제 완료 시각 |
| `refunded_at` | TIMESTAMPTZ | N | 환불 완료 시각 |
| `created_at` | TIMESTAMPTZ | Y | 생성 시각 |
| `updated_at` | TIMESTAMPTZ | N | 수정 시각 (트리거 자동 갱신) |

**인덱스**:

| 인덱스명 | 컬럼 | 목적 |
|---------|------|------|
| `uq_payment_number` | payment_number | UNIQUE, 결제 번호 조회 |
| `idx_payment_order_number` | order_number | 주문별 결제 조회 |
| `idx_payment_user_id` | user_id | 사용자별 결제 조회 |
| `idx_payment_status` | status | 상태별 결제 조회 |
| `idx_payment_intent_id` | intent_id | Intent-Payment 연결 조회 |

---

### outbox_events

Transactional Outbox 패턴 구현. 결제 이벤트와 DB 저장의 원자성을 보장합니다.

| 컬럼 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | BIGINT | Y | PK (GENERATED ALWAYS AS IDENTITY) |
| `aggregate_type` | VARCHAR(50) | Y | 집계 타입 (예: PAYMENT) |
| `aggregate_id` | VARCHAR(100) | Y | 집계 ID (paymentNumber) |
| `event_type` | VARCHAR(200) | Y | 이벤트 FQCN |
| `topic` | VARCHAR(100) | Y | Kafka 토픽명 |
| `event_key` | VARCHAR(100) | Y | Kafka 메시지 키 |
| `payload` | TEXT | Y | 이벤트 페이로드 (JSON) |
| `status` | VARCHAR(20) | Y | PENDING / PUBLISHED / FAILED (기본: PENDING) |
| `retry_count` | INT | Y | 재시도 횟수 (기본: 0) |
| `created_at` | TIMESTAMPTZ | Y | 생성 시각 |
| `published_at` | TIMESTAMPTZ | N | 발행 완료 시각 |

**인덱스**:

| 인덱스명 | 컬럼 | 목적 |
|---------|------|------|
| `idx_outbox_pending` | (status, created_at) WHERE status = 'PENDING' | Polling 최적화 |

---

## 마이그레이션

| 버전 | 파일 | 설명 |
|------|------|------|
| V1 | `V1__init.sql` | payment_intents, payments, outbox_events 초기 스키마 |

**Migration 경로**: `services/payment-service/src/main/resources/db/migration/`

---

## Shopping Service 스키마 변경

Payment 서비스 분리에 따라 shopping_db에서 다음 변경이 적용되었습니다:

| 마이그레이션 | 변경 내용 |
|------------|---------|
| `V6__add_payment_intent_id.sql` | orders 테이블에 `payment_intent_id` 컬럼 추가 |
| `V7__drop_payments_table.sql` | shopping_db에서 payments 테이블 제거 |

---

## 관련 문서

- [Payment Service Architecture](../payment-service/system-overview.md)
- [Shopping Service Schema](./shopping-service-schema.md)
- [DB Overview](./erd-overview.md)
- [ADR-055: Payment Service Extraction](../../adr/ADR-055-payment-service-extraction.md)
- [ADR-046: MySQL to PostgreSQL Migration](../../adr/ADR-046-mysql-to-postgresql-migration.md)

---

**마지막 업데이트**: 2026-02-28
