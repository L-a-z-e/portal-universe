# Kafka Schema Evolution

## 개요

마이크로서비스 아키텍처에서 서비스 간 통신에 사용되는 메시지 스키마는 시간이 지남에 따라 변경될 수밖에 없습니다. Schema Evolution(스키마 진화)은 이러한 변경을 안전하게 관리하여 Producer와 Consumer 간의 호환성을 유지하는 방법론입니다.

---

## 1. 스키마 진화의 필요성

### 1.1 왜 스키마가 변경되는가?

```
┌─────────────────────────────────────────────────────────────────┐
│                     스키마 변경 원인                              │
├─────────────────────────────────────────────────────────────────┤
│  비즈니스 요구사항 변경                                           │
│  ├── 새로운 필드 추가 (예: 주문에 쿠폰 코드 추가)                   │
│  ├── 기존 필드 제거 (예: deprecated된 속성 삭제)                   │
│  └── 필드 타입 변경 (예: int → long)                              │
│                                                                   │
│  기술적 개선                                                       │
│  ├── 성능 최적화를 위한 구조 변경                                   │
│  ├── 데이터 정규화/역정규화                                        │
│  └── 보안 요구사항 (예: PII 필드 암호화)                           │
│                                                                   │
│  버그 수정                                                         │
│  └── 잘못된 데이터 타입 또는 구조 수정                              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 스키마 진화가 없으면 발생하는 문제

```
시나리오: 동시 배포 불가능

     v1.0 (old schema)              v2.0 (new schema)
    ┌──────────────┐               ┌──────────────┐
    │   Producer   │               │   Producer   │
    │ {name, age}  │               │ {name, age,  │
    └──────┬───────┘               │  email}      │
           │                       └──────┬───────┘
           ▼                              ▼
    ┌──────────────────────────────────────────┐
    │              Kafka Topic                  │
    │   [v1 msg] [v2 msg] [v1 msg] [v2 msg]   │
    └──────────────────────────────────────────┘
           │                              │
           ▼                              ▼
    ┌──────────────┐               ┌──────────────┐
    │  Consumer    │   ❌ ERROR    │  Consumer    │
    │ expects v1   │◄──────────────│ expects v2   │
    └──────────────┘               └──────────────┘

문제점:
- 모든 Producer/Consumer를 동시에 배포해야 함 (Big Bang 배포)
- 롤백 불가능
- 다운타임 발생
```

### 1.3 스키마 진화의 이점

| 이점 | 설명 |
|-----|------|
| **Rolling Deployment** | 서비스별 독립적 배포 가능 |
| **Zero Downtime** | 구/신 버전 공존 가능 |
| **Backward/Forward 호환** | 다양한 버전의 Consumer 지원 |
| **데이터 계약 명시화** | 서비스 간 인터페이스 문서화 |

---

## 2. Schema Registry 개요

### 2.1 Schema Registry란?

Schema Registry는 스키마를 중앙에서 관리하고 검증하는 서비스입니다. Confluent에서 제공하는 Schema Registry가 가장 널리 사용됩니다.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Schema Registry 아키텍처                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│     ┌───────────┐                      ┌───────────┐             │
│     │ Producer  │                      │ Consumer  │             │
│     └─────┬─────┘                      └─────┬─────┘             │
│           │                                  │                   │
│           │ 1. Register Schema               │ 3. Get Schema     │
│           │    (if new)                      │    by ID          │
│           ▼                                  ▼                   │
│     ┌─────────────────────────────────────────────┐             │
│     │              Schema Registry                 │             │
│     │  ┌─────────────────────────────────────┐   │             │
│     │  │  Schemas Store (Kafka Internal)     │   │             │
│     │  │  - subject: order-value             │   │             │
│     │  │  - version: 1, 2, 3...              │   │             │
│     │  │  - schema: {...}                    │   │             │
│     │  │  - id: globally unique              │   │             │
│     │  └─────────────────────────────────────┘   │             │
│     └─────────────────────────────────────────────┘             │
│           │                                  │                   │
│           │ 2. Serialize with                │ 4. Deserialize    │
│           │    Schema ID                     │    with Schema    │
│           ▼                                  ▼                   │
│     ┌─────────────────────────────────────────────┐             │
│     │                 Kafka Broker                 │             │
│     │         [Schema ID + Data Payload]          │             │
│     └─────────────────────────────────────────────┘             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 주요 개념

```java
// Subject: 스키마의 논리적 그룹
// 일반적으로 "{topic-name}-key" 또는 "{topic-name}-value" 형식

Subject: "shopping.order.created-value"
├── Version 1: { orderNumber, userId, amount }
├── Version 2: { orderNumber, userId, amount, couponCode }  // 필드 추가
└── Version 3: { orderNumber, userId, totalAmount, couponCode }  // 필드명 변경

// 각 버전은 고유한 Schema ID를 가짐
Schema ID: 1 → Version 1
Schema ID: 2 → Version 2
Schema ID: 3 → Version 3
```

### 2.3 메시지 포맷

```
┌─────────────────────────────────────────────────────────────────┐
│                     Wire Format (on Kafka)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────┬───────────┬────────────────────────────────────┐      │
│   │Magic│ Schema ID │         Serialized Data            │      │
│   │Byte │ (4 bytes) │       (Avro/Protobuf/JSON)        │      │
│   │(1)  │           │                                    │      │
│   └─────┴───────────┴────────────────────────────────────┘      │
│     0x00   00000005   binary data...                            │
│                                                                   │
│   Magic Byte: Schema Registry 사용 표시                          │
│   Schema ID: Registry에서 스키마를 조회하는 키                     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 호환성 유형 (Compatibility Types)

### 3.1 호환성 매트릭스

```
┌──────────────────────────────────────────────────────────────────────┐
│                        호환성 유형 비교                                │
├──────────────┬────────────────┬────────────────┬────────────────────┤
│   유형        │   허용되는 변경   │   사용 시나리오   │   배포 순서        │
├──────────────┼────────────────┼────────────────┼────────────────────┤
│ BACKWARD     │ 필드 삭제       │ Consumer가     │ Consumer 먼저     │
│ (기본값)      │ 기본값 있는     │ 최신 스키마로   │                   │
│              │ 필드 추가       │ 구 데이터 읽기  │                   │
├──────────────┼────────────────┼────────────────┼────────────────────┤
│ FORWARD      │ 필드 추가       │ Producer가     │ Producer 먼저     │
│              │ 기본값 있는     │ 최신 스키마로   │                   │
│              │ 필드 삭제       │ 메시지 발행     │                   │
├──────────────┼────────────────┼────────────────┼────────────────────┤
│ FULL         │ 기본값 있는     │ 양방향 호환     │ 순서 무관         │
│              │ 필드 추가/삭제  │ 필요시          │                   │
├──────────────┼────────────────┼────────────────┼────────────────────┤
│ NONE         │ 모든 변경       │ 개발 환경       │ Big Bang 배포     │
│              │                │ (비권장)        │                   │
└──────────────┴────────────────┴────────────────┴────────────────────┘
```

### 3.2 BACKWARD Compatibility (하위 호환성)

**정의**: 새 스키마로 이전 데이터를 읽을 수 있음

```
BACKWARD: 새 Consumer가 구 Producer의 메시지를 읽을 수 있음

    Producer (v1)                    Consumer (v2)
   ┌──────────────┐                 ┌──────────────┐
   │ {            │                 │ {            │
   │   name,      │  ──────────▶   │   name,      │
   │   age        │     ✅ OK       │   age,       │
   │ }            │                 │   email?     │  ← 기본값 사용
   └──────────────┘                 └──────────────┘

허용되는 변경:
✅ 기본값이 있는 새 필드 추가
✅ 필드 삭제 (Consumer가 무시)
❌ 필수 필드 추가 (구 데이터에 없음)
❌ 필드 타입 변경 (역호환 불가)
```

```avro
// Schema v1
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "name", "type": "string"},
    {"name": "age", "type": "int"}
  ]
}

// Schema v2 (BACKWARD compatible)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "name", "type": "string"},
    {"name": "age", "type": "int"},
    {"name": "email", "type": ["null", "string"], "default": null}  // 기본값 필수!
  ]
}
```

### 3.3 FORWARD Compatibility (상위 호환성)

**정의**: 이전 스키마로 새 데이터를 읽을 수 있음

```
FORWARD: 구 Consumer가 새 Producer의 메시지를 읽을 수 있음

    Producer (v2)                    Consumer (v1)
   ┌──────────────┐                 ┌──────────────┐
   │ {            │                 │ {            │
   │   name,      │  ──────────▶   │   name,      │  ← email 무시
   │   age,       │     ✅ OK       │   age        │
   │   email      │                 │ }            │
   │ }            │                 └──────────────┘
   └──────────────┘

허용되는 변경:
✅ 새 필드 추가 (구 Consumer가 무시)
✅ 기본값이 있는 필드 삭제
❌ 필수 필드 삭제 (구 Consumer가 필요로 함)
```

### 3.4 FULL Compatibility (완전 호환성)

**정의**: BACKWARD + FORWARD (양방향 호환)

```
FULL: 모든 버전 간 상호 호환

    Producer (v1)     ◀─────────▶     Consumer (v2)
   ┌──────────────┐     ✅ OK        ┌──────────────┐
   │ {name, age}  │                  │ {name, age,  │
   └──────────────┘                  │  email?}     │
                                     └──────────────┘

    Producer (v2)     ◀─────────▶     Consumer (v1)
   ┌──────────────┐     ✅ OK        ┌──────────────┐
   │ {name, age,  │                  │ {name, age}  │
   │  email}      │                  └──────────────┘
   └──────────────┘

허용되는 변경 (매우 제한적):
✅ 기본값이 있는 새 필드 추가
✅ 기본값이 있는 필드 삭제
❌ 필수 필드 추가/삭제
❌ 필드 타입 변경
```

### 3.5 TRANSITIVE vs NON-TRANSITIVE

```
┌─────────────────────────────────────────────────────────────────┐
│                    Transitive 호환성                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  NON-TRANSITIVE (기본):                                          │
│    v1 ←→ v2 ←→ v3                                               │
│    (인접 버전만 호환 검사)                                         │
│                                                                   │
│    v1 ──?──▶ v3  (직접 호환 여부는 검사하지 않음)                  │
│                                                                   │
│  TRANSITIVE:                                                      │
│    v1 ←→ v2 ←→ v3                                               │
│    v1 ←─────────→ v3                                             │
│    (모든 이전 버전과 호환 검사)                                    │
│                                                                   │
│  유형:                                                            │
│  - BACKWARD_TRANSITIVE: 모든 이전 버전 데이터 읽기 가능            │
│  - FORWARD_TRANSITIVE: 모든 이전 Consumer가 새 데이터 읽기 가능    │
│  - FULL_TRANSITIVE: 모든 버전 간 양방향 호환                       │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 직렬화 포맷 비교: Avro, Protobuf, JSON Schema

### 4.1 비교 매트릭스

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        직렬화 포맷 상세 비교                                   │
├────────────┬─────────────────┬─────────────────┬─────────────────────────────┤
│   항목      │      Avro       │    Protobuf     │       JSON Schema          │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 개발사      │ Apache          │ Google          │ JSON Schema Org            │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 스키마 정의 │ JSON            │ .proto 파일     │ JSON                       │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 인코딩      │ Binary          │ Binary          │ Text (JSON)                │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 메시지 크기 │ 매우 작음 ⭐    │ 작음            │ 큼                          │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 직렬화 속도 │ 빠름            │ 매우 빠름 ⭐    │ 느림                        │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 스키마 진화 │ 강력함 ⭐       │ 강력함          │ 제한적                      │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 코드 생성   │ 선택적          │ 필수            │ 선택적                      │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 디버깅      │ 어려움          │ 어려움          │ 쉬움 ⭐                     │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ Null 처리   │ Union 타입      │ optional        │ nullable                   │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ 학습 곡선   │ 중간            │ 높음            │ 낮음 ⭐                     │
├────────────┼─────────────────┼─────────────────┼─────────────────────────────┤
│ Kafka 지원  │ 최상 ⭐         │ 좋음            │ 좋음                        │
└────────────┴─────────────────┴─────────────────┴─────────────────────────────┘
```

### 4.2 Avro

```json
// 스키마 정의 (avsc 파일)
{
  "type": "record",
  "name": "OrderCreatedEvent",
  "namespace": "com.portal.universe.events",
  "fields": [
    {"name": "orderNumber", "type": "string"},
    {"name": "userId", "type": "string"},
    {"name": "totalAmount", "type": {"type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2}},
    {"name": "itemCount", "type": "int"},
    {"name": "createdAt", "type": {"type": "long", "logicalType": "timestamp-millis"}},
    {
      "name": "items",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "OrderItemInfo",
          "fields": [
            {"name": "productId", "type": "long"},
            {"name": "productName", "type": "string"},
            {"name": "quantity", "type": "int"},
            {"name": "price", "type": {"type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2}}
          ]
        }
      }
    },
    // 선택적 필드 (스키마 진화에 유리)
    {"name": "couponCode", "type": ["null", "string"], "default": null}
  ]
}
```

**Avro의 장점**:
- 스키마가 데이터와 함께 저장되지 않아 메시지 크기가 작음
- Schema Registry와 최적의 통합
- 동적 타입 지원 (코드 생성 없이 사용 가능)

### 4.3 Protobuf

```protobuf
// 스키마 정의 (.proto 파일)
syntax = "proto3";
package com.portal.universe.events;

import "google/protobuf/timestamp.proto";

message OrderCreatedEvent {
  string order_number = 1;
  string user_id = 2;
  string total_amount = 3;  // Decimal as string
  int32 item_count = 4;
  google.protobuf.Timestamp created_at = 5;
  repeated OrderItemInfo items = 6;

  // 선택적 필드 (proto3에서는 기본적으로 optional)
  optional string coupon_code = 7;
}

message OrderItemInfo {
  int64 product_id = 1;
  string product_name = 2;
  int32 quantity = 3;
  string price = 4;
}
```

**Protobuf의 장점**:
- 필드 번호 기반으로 강력한 스키마 진화 지원
- 매우 빠른 직렬화/역직렬화 성능
- gRPC와 완벽한 통합

**주의사항**:
```protobuf
// 절대 하지 말아야 할 것
message User {
  string name = 1;
  // int32 age = 2;  // 삭제됨 - 필드 번호 2는 재사용 금지!
  string email = 3;
  int32 phone = 2;   // ❌ 번호 재사용은 호환성 파괴!
}
```

### 4.4 JSON Schema

```json
// 스키마 정의
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "OrderCreatedEvent.schema.json",
  "title": "OrderCreatedEvent",
  "type": "object",
  "required": ["orderNumber", "userId", "totalAmount", "itemCount", "createdAt", "items"],
  "properties": {
    "orderNumber": {
      "type": "string",
      "description": "주문 번호"
    },
    "userId": {
      "type": "string",
      "description": "사용자 ID"
    },
    "totalAmount": {
      "type": "number",
      "description": "총 금액"
    },
    "itemCount": {
      "type": "integer",
      "description": "상품 수량"
    },
    "createdAt": {
      "type": "string",
      "format": "date-time",
      "description": "생성 일시"
    },
    "items": {
      "type": "array",
      "items": {
        "$ref": "#/definitions/OrderItemInfo"
      }
    },
    "couponCode": {
      "type": ["string", "null"],
      "description": "쿠폰 코드 (선택)"
    }
  },
  "definitions": {
    "OrderItemInfo": {
      "type": "object",
      "required": ["productId", "productName", "quantity", "price"],
      "properties": {
        "productId": {"type": "integer"},
        "productName": {"type": "string"},
        "quantity": {"type": "integer"},
        "price": {"type": "number"}
      }
    }
  }
}
```

**JSON Schema의 장점**:
- 사람이 읽기 쉬움 (디버깅 용이)
- 별도의 코드 생성 불필요
- 기존 JSON 기반 시스템과 호환

### 4.5 포맷 선택 가이드

```
┌─────────────────────────────────────────────────────────────────┐
│                    포맷 선택 의사결정 트리                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│                    메시지 볼륨이 높은가?                           │
│                           │                                       │
│              ┌────────────┴────────────┐                         │
│              │                         │                         │
│             Yes                        No                        │
│              │                         │                         │
│              ▼                         ▼                         │
│     성능이 최우선인가?           디버깅 용이성이                   │
│              │                   중요한가?                        │
│     ┌────────┴────────┐              │                           │
│     │                 │         ┌────┴────┐                      │
│    Yes               No        Yes       No                      │
│     │                 │         │         │                      │
│     ▼                 ▼         ▼         ▼                      │
│ ┌────────┐      ┌────────┐ ┌────────┐ ┌────────┐                │
│ │Protobuf│      │  Avro  │ │  JSON  │ │  Avro  │                │
│ └────────┘      └────────┘ │ Schema │ └────────┘                │
│                            └────────┘                            │
│                                                                   │
│  권장 시나리오:                                                   │
│  - Avro: Kafka 중심 아키텍처, Confluent 생태계                    │
│  - Protobuf: gRPC 사용, 다국어 환경, 최고 성능 필요                │
│  - JSON Schema: 초기 개발, 작은 규모, 디버깅 중요                  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 스키마 변경 모범 사례

### 5.1 안전한 스키마 변경 규칙

```
┌─────────────────────────────────────────────────────────────────┐
│                    스키마 변경 허용/금지 매트릭스                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ✅ 안전한 변경 (권장)                                            │
│  ├── 기본값이 있는 새 필드 추가                                    │
│  ├── 선택적 필드(nullable)를 필수가 아닌 상태로 유지               │
│  ├── 필드에 alias 추가 (일부 포맷)                                │
│  └── 문서/설명 추가                                               │
│                                                                   │
│  ⚠️ 주의가 필요한 변경                                            │
│  ├── 필드 삭제 (Consumer가 여전히 사용 중일 수 있음)               │
│  ├── 필드명 변경 (alias로 처리 권장)                              │
│  └── 타입 확장 (int → long은 일부 포맷에서 가능)                   │
│                                                                   │
│  ❌ 금지된 변경 (Breaking Changes)                                │
│  ├── 필수 필드를 기본값 없이 추가                                  │
│  ├── 필드 타입을 호환되지 않게 변경 (string → int)                 │
│  ├── 필드 번호 재사용 (Protobuf)                                  │
│  ├── enum 값 제거 (기존 데이터 손상)                               │
│  └── 필드명 대소문자만 변경                                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 필드 추가 모범 사례

```java
// ✅ Good: 기본값이 있는 선택적 필드 추가
public record OrderCreatedEvent(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt,
    // v2에서 추가된 필드 - nullable로 정의
    @Nullable String couponCode,
    @Nullable String promotionId
) {
    // 기본 생성자 (v1 데이터 역직렬화용)
    public OrderCreatedEvent(
            String orderNumber, String userId, BigDecimal totalAmount,
            int itemCount, List<OrderItemInfo> items, LocalDateTime createdAt) {
        this(orderNumber, userId, totalAmount, itemCount, items, createdAt, null, null);
    }
}

// ❌ Bad: 필수 필드 추가 (기존 데이터 역직렬화 실패)
public record OrderCreatedEventBad(
    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt,
    String couponCode  // 필수 필드 - 구 데이터에 없어서 실패!
) {}
```

### 5.3 필드 제거 모범 사례

```
단계적 필드 제거 프로세스 (Deprecation Strategy)

Phase 1: Deprecation 선언 (2-4주)
┌───────────────────────────────────────┐
│ /**                                   │
│  * @deprecated v3.0에서 제거 예정     │
│  * 대신 'shippingAddress' 사용        │
│  */                                   │
│ @Deprecated                           │
│ String address;                       │
│                                       │
│ String shippingAddress;  // 새 필드   │
└───────────────────────────────────────┘
- 모든 Consumer에 마이그레이션 알림
- 새 필드와 구 필드 모두 전송

Phase 2: Consumer 마이그레이션 (4-8주)
┌───────────────────────────────────────┐
│ // Consumer 코드                      │
│ String address = Optional             │
│     .ofNullable(event.shippingAddress)│
│     .orElse(event.address);  // 폴백  │
└───────────────────────────────────────┘
- Consumer들이 새 필드 사용하도록 업데이트
- 모니터링: 구 필드 사용량 추적

Phase 3: Producer에서 구 필드 제거
┌───────────────────────────────────────┐
│ // 구 필드는 null로 전송              │
│ new Event(                            │
│     null,  // address (deprecated)    │
│     "123 Main St"  // shippingAddress │
│ );                                    │
└───────────────────────────────────────┘

Phase 4: 스키마에서 필드 제거 (필요시)
- 모든 Consumer 확인 후 스키마에서 제거
- 최소 N 버전 이전 데이터는 이미 만료됨 확인
```

### 5.4 버전 관리 전략

```
┌─────────────────────────────────────────────────────────────────┐
│                    스키마 버전 관리 전략                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  전략 1: Topic 기반 버전 관리                                     │
│  ────────────────────────────                                    │
│  shopping.order.created.v1 → shopping.order.created.v2           │
│                                                                   │
│  장점: 깔끔한 분리, 롤백 용이                                      │
│  단점: Consumer가 여러 토픽 구독 필요                             │
│                                                                   │
│  전략 2: Header 기반 버전 관리                                    │
│  ────────────────────────────                                    │
│  Topic: shopping.order.created                                   │
│  Header: { "schema-version": "2.0" }                             │
│                                                                   │
│  장점: 단일 토픽 유지, 유연한 라우팅                               │
│  단점: Consumer 복잡도 증가                                       │
│                                                                   │
│  전략 3: Schema Registry (권장) ⭐                                │
│  ────────────────────────────                                    │
│  Topic: shopping.order.created                                   │
│  Message: [Schema ID] + [Data]                                   │
│                                                                   │
│  장점: 자동 버전 관리, 호환성 검증, 중앙 집중 관리                  │
│  단점: 추가 인프라 필요                                           │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 5.5 CI/CD 통합

```yaml
# GitHub Actions 예시: 스키마 호환성 검사
name: Schema Compatibility Check

on:
  pull_request:
    paths:
      - 'schemas/**'
      - '**/event/*.java'

jobs:
  compatibility-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Check Schema Compatibility
        run: |
          # Schema Registry에 호환성 검사 요청
          for schema in schemas/*.avsc; do
            subject=$(basename "$schema" .avsc)-value
            curl -X POST \
              -H "Content-Type: application/vnd.schemaregistry.v1+json" \
              -d @"$schema" \
              "$SCHEMA_REGISTRY_URL/compatibility/subjects/$subject/versions/latest"
          done
        env:
          SCHEMA_REGISTRY_URL: ${{ secrets.SCHEMA_REGISTRY_URL }}
```

---

## 6. Portal Universe의 JSON 직렬화 분석

### 6.1 현재 아키텍처

Portal Universe는 현재 Spring Kafka의 JSON Serializer/Deserializer를 사용하고 있습니다.

```
┌─────────────────────────────────────────────────────────────────┐
│              Portal Universe 현재 Kafka 설정                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  shopping-service (Producer)                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  KafkaConfig.java                                        │    │
│  │  - JsonSerializer 사용                                   │    │
│  │  - acks=all, retries=3, idempotence=true                │    │
│  │  - Topics: shopping.order.*, shopping.payment.*         │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                     │
│                            ▼                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                    Kafka Broker                          │    │
│  │           [JSON Message + Type Headers]                  │    │
│  └─────────────────────────────────────────────────────────┘    │
│                            │                                     │
│                            ▼                                     │
│  notification-service (Consumer)                                 │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  KafkaConsumerConfig.java                                │    │
│  │  - ErrorHandlingDeserializer + JsonDeserializer          │    │
│  │  - TRUSTED_PACKAGES: "com.portal.universe.*"            │    │
│  │  - USE_TYPE_INFO_HEADERS: true                          │    │
│  │  - Dead Letter Queue 지원                                │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 현재 이벤트 스키마

```java
// common-library의 이벤트 정의
// services/common-library/src/main/java/com/portal/universe/common/event/shopping/

public record OrderCreatedEvent(
    String orderNumber,        // 주문 번호
    String userId,             // 사용자 ID
    BigDecimal totalAmount,    // 총 금액
    int itemCount,             // 상품 수
    List<OrderItemInfo> items, // 상품 목록
    LocalDateTime createdAt    // 생성 시간
) {
    public record OrderItemInfo(
        Long productId,
        String productName,
        int quantity,
        BigDecimal price
    ) {}
}

public record UserSignedUpEvent(
    String userId,
    String email,
    String name
) {}
```

### 6.3 현재 방식의 장단점

```
┌─────────────────────────────────────────────────────────────────┐
│                 JSON 직렬화 방식 분석                            │
├────────────────────────────────┬────────────────────────────────┤
│           장점 ✅               │           단점 ❌              │
├────────────────────────────────┼────────────────────────────────┤
│ • 설정 단순                     │ • 스키마 검증 없음             │
│ • 디버깅 용이 (사람 읽기 가능)   │ • 메시지 크기가 큼             │
│ • 코드 생성 불필요              │ • 직렬화 성능이 낮음           │
│ • 빠른 개발 속도                │ • 호환성 자동 검증 불가        │
│ • Java record와 잘 통합         │ • 타입 불일치 런타임 오류      │
│ • common-library로 공유 용이    │ • Polyglot 환경에서 복잡       │
├────────────────────────────────┴────────────────────────────────┤
│                                                                   │
│  현재 상태: 개발 초기 단계에 적합                                  │
│  트래픽 증가 시: Schema Registry 도입 고려                        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 6.4 스키마 진화 대응 현황

```java
// 현재 방식: Java Record + Jackson

// 장점: Jackson의 유연한 역직렬화
// - @JsonIgnoreProperties(ignoreUnknown = true)로 새 필드 무시 가능
// - Optional 필드로 누락 필드 처리 가능

// Consumer 측 설정 (KafkaConsumerConfig.java)
props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
// → Producer가 보낸 타입 정보 헤더를 사용하여 역직렬화

// 현재 한계:
// 1. 필드 타입 변경 시 런타임 오류
// 2. 필드명 변경 시 데이터 손실
// 3. 스키마 버전 추적 불가
// 4. 호환성 검증 자동화 없음
```

### 6.5 개선 권장 사항

```
┌─────────────────────────────────────────────────────────────────┐
│              단계별 스키마 관리 개선 로드맵                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Phase 1: 현재 (JSON + common-library)                           │
│  ─────────────────────────────────────                           │
│  • 공통 라이브러리에서 이벤트 클래스 관리                          │
│  • @JsonIgnoreProperties(ignoreUnknown = true) 적용              │
│  • 이벤트 버전을 필드로 관리                                      │
│                                                                   │
│  public record OrderCreatedEvent(                                │
│      String schemaVersion,  // "1.0", "1.1" 등                   │
│      String orderNumber,                                         │
│      ...                                                         │
│  ) {}                                                            │
│                                                                   │
│  Phase 2: JSON Schema 도입                                       │
│  ─────────────────────────────                                   │
│  • JSON Schema 파일로 스키마 문서화                               │
│  • CI/CD에서 스키마 유효성 검사                                   │
│  • Schema Registry 없이 기본 검증                                 │
│                                                                   │
│  Phase 3: Schema Registry 도입 (트래픽 증가 시)                   │
│  ────────────────────────────────────────────                    │
│  • Confluent Schema Registry 또는 Apicurio 도입                  │
│  • Avro 또는 Protobuf로 마이그레이션                              │
│  • 자동 호환성 검증 및 스키마 버전 관리                            │
│                                                                   │
│  Phase 4: Polyglot 지원 (Go, Python 서비스 추가 시)               │
│  ───────────────────────────────────────────────                 │
│  • Protobuf로 통일 (다국어 코드 생성)                             │
│  • 또는 Avro + 언어별 클라이언트                                  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 6.6 즉시 적용 가능한 개선

```java
// 1. Jackson 설정 개선 (역직렬화 관용성)
@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}

// 2. 이벤트 클래스에 버전 정보 추가
public record OrderCreatedEvent(
    @JsonProperty("_version") @JsonInclude(JsonInclude.Include.NON_NULL)
    String version,

    String orderNumber,
    String userId,
    BigDecimal totalAmount,
    int itemCount,
    List<OrderItemInfo> items,
    LocalDateTime createdAt,

    // 새 필드는 항상 Optional 또는 @Nullable로 추가
    @Nullable String couponCode
) {
    // 이전 버전 호환 생성자
    public OrderCreatedEvent(
            String orderNumber, String userId, BigDecimal totalAmount,
            int itemCount, List<OrderItemInfo> items, LocalDateTime createdAt) {
        this("1.0", orderNumber, userId, totalAmount, itemCount, items, createdAt, null);
    }
}

// 3. Consumer에서 버전 기반 처리
@KafkaListener(topics = "shopping.order.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    String version = Optional.ofNullable(event.version()).orElse("1.0");

    switch (version) {
        case "1.0" -> processV1(event);
        case "1.1" -> processV1_1(event);
        default -> {
            log.warn("Unknown event version: {}, using latest handler", version);
            processLatest(event);
        }
    }
}
```

---

## 7. 요약

### 7.1 핵심 포인트

| 항목 | 권장 사항 |
|-----|----------|
| **호환성 유형** | FULL (또는 BACKWARD) - 양방향 호환 권장 |
| **직렬화 포맷** | 초기: JSON, 성장기: Avro, 대규모: Protobuf |
| **필드 추가** | 항상 기본값 또는 nullable로 |
| **필드 제거** | 단계적 deprecation 프로세스 |
| **버전 관리** | Schema Registry 또는 헤더 기반 |
| **CI/CD** | 스키마 호환성 검사 자동화 |

### 7.2 Portal Universe 권장 다음 단계

1. **즉시**: `@JsonIgnoreProperties(ignoreUnknown = true)` 적용
2. **단기**: 이벤트 클래스에 버전 필드 추가
3. **중기**: JSON Schema 문서화 및 CI 검증
4. **장기**: Schema Registry 도입 검토 (트래픽 증가 시)

---

## 참고 자료

- [Confluent Schema Registry Documentation](https://docs.confluent.io/platform/current/schema-registry/index.html)
- [Apache Avro Specification](https://avro.apache.org/docs/current/spec.html)
- [Protocol Buffers Language Guide](https://developers.google.com/protocol-buffers/docs/proto3)
- [JSON Schema](https://json-schema.org/)
- [Kafka Schema Evolution Best Practices](https://www.confluent.io/blog/schema-registry-avro-in-spring-boot-application-tutorial/)
