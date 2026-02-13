# Event Contracts

Polyglot 마이크로서비스 간 Kafka 이벤트 계약을 JSON Schema로 정의합니다.

## 구조

```
schemas/
  {domain}.{entity}.{event}.schema.json
```

## SSOT (Single Source of Truth)

이 디렉토리의 JSON Schema가 이벤트 구조의 정본입니다.

| 언어 | 구현 | 동기화 방법 |
|------|------|-----------|
| Java | `*-events/` 모듈의 record 클래스 | CI 검증 (`scripts/validate-event-contracts.js`) |
| TypeScript | `prism-service/src/modules/event/` interfaces | CI 검증 |

## 검증

```bash
node scripts/validate-event-contracts.js
```

## 스키마 추가 시

1. `schemas/` 에 JSON Schema 파일 추가
2. Java event record 필드가 스키마와 일치하는지 확인
3. TypeScript interface 필드가 스키마와 일치하는지 확인
4. `validate-event-contracts.js` 의 매핑 테이블에 항목 추가

## 관련 문서

- [ADR-032: Kafka Configuration Standardization](../../docs/adr/ADR-032-kafka-configuration-standardization.md)
- [ADR-038: Polyglot Event Contract Management](../../docs/adr/ADR-038-polyglot-event-contract-management.md)
