# API Contracts

> 서비스 간 공유 계약(JSON Schema, 헤더 규격, 에러 코드)을 정의하는 디렉토리.
> 모든 서비스(Java/Spring, NestJS, Python)는 이 계약을 준수해야 한다.

**마지막 업데이트**: 2026-02-14

---

## 스키마 목록

| 파일 | 역할 | 관련 ADR |
|------|------|----------|
| [api-response.schema.json](api-response.schema.json) | 성공 응답 wrapper (`ApiResponse`) | [ADR-031](../adr/ADR-031-unified-api-response-strategy.md) |
| [error-response.schema.json](error-response.schema.json) | 에러 응답 wrapper | [ADR-031](../adr/ADR-031-unified-api-response-strategy.md) |
| [page-response.schema.json](page-response.schema.json) | 페이지네이션 응답 wrapper | [ADR-031](../adr/ADR-031-unified-api-response-strategy.md) |
| [sse-envelope.schema.json](sse-envelope.schema.json) | SSE 실시간 이벤트 envelope | [ADR-028](../adr/ADR-028-sse-endpoint-authentication.md) |
| [error-codes.md](error-codes.md) | 서비스별 에러 코드 prefix 할당표 | [ADR-031](../adr/ADR-031-unified-api-response-strategy.md) |
| [gateway-headers.md](gateway-headers.md) | Gateway → Downstream 헤더 규격 | [ADR-035](../adr/ADR-035-polyglot-authentication-standardization.md) |

---

## 서비스별 구현체 매핑

| 계약 | Java/Spring | NestJS (Prism) | Python (Chatbot) |
|------|-------------|----------------|------------------|
| ApiResponse wrapper | `common-lib` `ApiResponse.java` | `api-response.interceptor.ts` | `api_response.py` middleware |
| ErrorCode enum | `CommonErrorCode` + 서비스별 enum | `error-codes.enum.ts` | `error_codes.py` |
| ExceptionHandler | `GlobalExceptionHandler` | `AllExceptionsFilter` | `exception_handlers.py` |
| Gateway 헤더 파싱 | `SecurityContextFilter` | `AuthGuard` + decorator | `get_current_user()` dependency |
| XSS 필터링 | `XssFilter` + `HtmlCharacterEscapes` | `helmet` + `class-transformer` | `bleach` sanitizer |

---

## 새 계약 추가 절차

1. **JSON Schema 작성**: `docs/contracts/`에 `{name}.schema.json` 추가
2. **문서 작성**: 필요 시 `.md` 파일로 상세 규격 기술
3. **ADR 작성**: 계약 도입 배경과 결정을 `docs/adr/`에 기록
4. **서비스 구현**: 각 스택별 구현체를 계약에 맞춰 작성
5. **이 README 업데이트**: 스키마 목록 테이블에 항목 추가

---

## Kafka 이벤트 계약

Kafka 이벤트 스키마는 별도 관리:

- **계약 정의**: `docs/contracts/events/` (JSON Schema, 계획)
- **관리 전략**: [ADR-038](../adr/ADR-038-polyglot-event-contract-management.md)
- **토픽 목록**: [Event-Driven Architecture](../architecture/system/event-driven-architecture.md)

---

## 관련 문서

- [Unified API Response Strategy (ADR-031)](../adr/ADR-031-unified-api-response-strategy.md)
- [Polyglot Event Contract Management (ADR-038)](../adr/ADR-038-polyglot-event-contract-management.md)
- [Polyglot Authentication Standardization (ADR-035)](../adr/ADR-035-polyglot-authentication-standardization.md)
- [Common Library Architecture](../architecture/system/common-library.md)

---

작성자: Laze
