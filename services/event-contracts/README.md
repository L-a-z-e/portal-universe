# Event Contracts

Polyglot 마이크로서비스 간 Kafka 이벤트 계약을 Avro Schema로 정의합니다.

## 구조

```
schemas/
  com/portal/universe/event/
    auth/       # Auth 이벤트 (2개)
    blog/       # Blog 이벤트 (4개)
    drive/      # Drive 이벤트 (3개)
    prism/      # Prism 이벤트 (2개 + TaskStatus enum)
    shopping/   # Shopping 이벤트 (9개 + nested record 2개)
src/
  main/java/com/portal/universe/event/
    auth/AuthTopics.java
    blog/BlogTopics.java
    drive/DriveTopics.java
    prism/PrismTopics.java
    shopping/ShoppingTopics.java
```

## SSOT (Single Source of Truth)

`.avsc` 파일이 이벤트 구조의 정본입니다. `gradle-avro-plugin`이 Java 클래스를 자동 생성합니다.

| 언어 | 구현 | 동기화 방법 |
|------|------|-----------|
| Java | `build/generated-main-avro-java/` (SpecificRecord) | gradle-avro-plugin 자동 생성 |
| TypeScript | `@kafkajs/confluent-schema-registry` 런타임 encode/decode | Schema Registry에서 스키마 조회 |

## 빌드

```bash
# Avro Schema → Java 클래스 생성
./gradlew :event-contracts:generateAvroJava

# 전체 빌드
./gradlew :event-contracts:build
```

## 스키마 추가 시

1. `schemas/com/portal/universe/event/{domain}/` 에 `.avsc` 파일 추가
2. `./gradlew :event-contracts:generateAvroJava` 실행하여 Java 클래스 생성 확인
3. 해당 도메인의 `*Topics.java`에 Topic 상수 추가
4. Schema Registry에 등록 후 호환성 검증 (`BACKWARD_TRANSITIVE`)

## 호환성 규칙

- 정책: `BACKWARD_TRANSITIVE` (새 스키마로 모든 이전 버전 메시지 읽기 가능)
- 필드 추가: 반드시 `default` 값 포함 (`["null", "string"]`, `default: null`)
- 필드 삭제: 해당 필드에 `default`가 이미 존재해야 함
- 타입 변경: 금지 (새 필드 추가 + 기존 필드 deprecated 처리)

## 관련 문서

- [ADR-047: Avro 및 Schema Registry 도입](../../docs/adr/ADR-047-avro-schema-registry-adoption.md)
- [ADR-032: Kafka Configuration Standardization](../../docs/adr/ADR-032-kafka-configuration-standardization.md)
