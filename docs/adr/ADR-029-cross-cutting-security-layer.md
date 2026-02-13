# ADR-029: Shopping Service Cross-cutting 보안 처리 계층 설계

**Status**: Accepted
**Date**: 2026-02-07
**Author**: Laze

## Context

Shopping Service 코드 리뷰에서 세 가지 cross-cutting 보안 이슈가 발견되었습니다.

1. **CORS 미설정 (S6)**: `SecurityConfig`에 CORS 설정이 없습니다. 현재는 API Gateway를 통해서만 접근하므로 문제가 없지만, Swagger UI 직접 접근이나 로컬 개발 시 프론트엔드에서 직접 호출하면 CORS 에러가 발생합니다.

2. **Security Headers 미설정 (S9)**: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security` 등 표준 보안 헤더가 설정되지 않았습니다. Spring Security의 기본 헤더도 명시적으로 구성되어 있지 않습니다.

3. **Rate Limiting 부재 (P3)**: API 레벨의 요청 제한이 없어, 대기열 등록이나 결제 등 민감한 엔드포인트에 무제한 요청이 가능합니다.

이 세 가지는 모두 API Gateway에서 처리하는 것이 MSA 표준 패턴이지만, 서비스 레벨의 방어 계층이 전혀 없는 것은 defense-in-depth 원칙에 위배됩니다.

## Decision

**API Gateway를 1차 방어선으로 두되, 서비스 레벨에서 최소한의 방어를 추가합니다 (Gateway Primary + Service Override).**

- **CORS**: API Gateway에서 통합 관리. 서비스 레벨에서는 Spring Security 기본 CORS를 비활성화 (Gateway가 이미 처리).
- **Security Headers**: Spring Security 기본 헤더 활성화 (추가 설정 없이 `headers()` 기본값 사용).
- **Rate Limiting**: API Gateway에 전역 rate limit 설정. 서비스 레벨에서는 민감 엔드포인트(결제, 대기열 등록)에만 `@RateLimiter` 적용.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| ① API Gateway 단독 | 관리 포인트 최소, 일관성 | 서비스 직접 접근 시 무방비 |
| ② 서비스별 개별 설정 | 독립적 방어 | 설정 중복, 정책 불일치 위험 |
| ③ Gateway 기본 + Service override (채택) | Defense-in-depth, 유연성 | 두 계층 설정 관리 필요 |

## Rationale

- **Defense-in-depth**: MSA에서 Gateway가 유일한 방어선이면, Gateway 우회(내부 통신, 직접 접근) 시 무방비입니다. 서비스 레벨의 최소 방어는 보안의 기본입니다.
- **Spring Security 기본값 활용**: `headers()` 기본값만으로 `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control` 등이 자동 적용됩니다. 추가 코드가 거의 없습니다.
- **Rate Limiting 차등 적용**: 전역 rate limit은 Gateway에서, 비즈니스 로직 기반 제한(예: 사용자당 결제 5회/분)은 서비스에서 처리하는 것이 관심사 분리에 적합합니다.
- **로컬 개발 편의**: CORS를 서비스에서 별도 설정하지 않으면, 로컬에서 Gateway 없이 프론트엔드 직접 연결 시 문제가 없습니다 (Spring Security CORS 비활성 + 프론트엔드 프록시 설정).

## Trade-offs

✅ **장점**:
- 최소 코드 변경으로 보안 헤더 확보
- Gateway와 서비스 간 역할 분담 명확
- 민감 엔드포인트만 서비스 레벨 rate limit → 오버엔지니어링 방지

⚠️ **단점 및 완화**:
- 두 계층의 rate limit 정책 동기화 필요 → (완화: Gateway는 IP 기반 전역 limit, 서비스는 userId 기반 비즈니스 limit으로 관심사 분리)
- 서비스 간 rate limit 설정이 다를 수 있음 → (완화: common-library에 기본 설정 제공, 서비스별 override)

## Implementation

### 1. SecurityConfig - 보안 헤더 (S9 해결)
```java
// 기존 csrf(AbstractHttpConfigurer::disable) 다음에 추가
.headers(headers -> headers.defaultsDisabled().disable()) // Spring Security 기본 헤더 활성화
```
→ Spring Security의 기본 동작이 이미 헤더를 추가하므로, 현재 명시적으로 비활성화하지 않았다면 기본 적용됨을 확인

### 2. API Gateway - CORS (S6 해결)
- `api-gateway`의 `application.yml`에 글로벌 CORS 설정 추가
- Shopping Service에서는 별도 CORS 설정 불필요

### 3. Rate Limiting (P3 해결)
- API Gateway: Spring Cloud Gateway `RequestRateLimiter` 필터 (Redis 기반)
- Shopping Service: Resilience4j `@RateLimiter` → 결제(`POST /payments`), 대기열 등록(`POST /queue/*/enter`)에 적용
- 설정값: 결제 5req/min/user, 대기열 등록 10req/min/user

### 4. Polyglot Input Validation (XSS 방어 확장)
- **NestJS (Prism)**: `@NoXss()` custom validator decorator + `@NoSqlInjection()` decorator
  - `common/validators/no-xss.validator.ts`, `no-sql-injection.validator.ts`
  - 8개 DTO의 사용자 입력 string 필드에 적용
- **Python (Chatbot)**: `check_no_xss()` Pydantic field validator
  - `app/core/validators.py` — 동일한 XSS 패턴 세트
  - `ChatRequest.message`에 적용 + 길이 제한 (1-10000)
- **Path Traversal 방지**: `documents.py`에서 `Path.resolve().is_relative_to()` 검증 추가

### 5. Polyglot Audit Logging
- **NestJS (Prism)**: `AuditInterceptor` — POST/PUT/PATCH/DELETE 요청에 userId, method, path, duration, status 로깅
- **Python (Chatbot)**: `AuditMiddleware` — 동일 패턴의 ASGI 미들웨어

### 코드 참조
- `SecurityConfig.java` (전체)
- `prism-service/src/common/validators/` (XSS/SQLi validators)
- `prism-service/src/common/interceptors/audit.interceptor.ts`
- `chatbot-service/app/core/validators.py`, `app/core/audit.py`

## References

- [ADR-010: 보안 강화 아키텍처](./ADR-010-security-enhancement-architecture.md)
- [Spring Security Headers](https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html)
- [Resilience4j Rate Limiter](https://resilience4j.readme.io/docs/ratelimiter)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-07 | 초안 작성 | Laze |
| 2026-02-13 | Accepted: Polyglot XSS validation, audit logging, path traversal 방어 추가 | Laze |
