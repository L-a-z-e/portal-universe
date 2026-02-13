# ADR-010: Security Enhancement Architecture

**Status**: Accepted
**Date**: 2026-01-23

## Context

Portal Universe는 JWT 인증(ADR-008), RBAC 인가(ADR-003)를 구현했으나, 운영 환경 보안 위협(DDoS, 무차별 대입 공격, 감사 추적 부재, XSS/CSRF)에 대한 추가 방어 체계가 필요합니다. 규제 준수(개인정보보호법, ISMS)와 침해 대응 능력 확보가 요구됩니다.

## Decision

4가지 보안 강화 아키텍처를 채택합니다: **Rate Limiting**, **보안 감사 로깅**, **로그인 보안**, **보안 헤더**.

## Rationale

- **Rate Limiting**: Sliding Window 알고리즘 + Redis로 DDoS 방어, 리소스 보호
- **감사 로깅**: Kafka + ELK 기반 보안 이벤트 수집으로 규정 준수, 사고 추적 가능
- **로그인 보안**: 점진적 지연(Progressive Delay) + 계정 잠금으로 무차별 대입 공격 방어
- **보안 헤더**: Gateway 레벨 CSP, XSS 방지, Clickjacking 방어

## Trade-offs

✅ **장점**:
- DDoS 방어, 계정 보호, 브라우저 보안 강화
- 감사 로그로 규정 준수, 침해 대응 능력 확보
- common-library 모듈로 일관된 보안 적용

⚠️ **단점 및 완화**:
- Redis 조회 증가로 성능 영향 → (완화: Redis Cluster, 로컬 캐시)
- 로그인 지연/잠금으로 사용자 불편 → (완화: 명확한 안내 메시지, 30분 자동 해제)
- 로그 저장 공간 증가 → (완화: 90일 보관 정책, 압축)
- Redis 장애 시 보안 기능 저하 → (완화: Sentinel/Cluster, Circuit Breaker, DB Fallback)

## Implementation

### 1. Rate Limiting
- **위치**: API Gateway Filter
- **알고리즘**: Sliding Window Counter (Redis 기반)
- **정책**: IP별/User별, 엔드포인트별 차등 (예: Login 10 req/min, GET 300 req/min)
- **응답**: 429 + `Retry-After` 헤더

### 2. 보안 감사 로깅
- **위치**: `@AuditLog` Aspect (common-library)
- **전송**: Kafka Topic `security.audit.events` (90일 보관)
- **저장**: Elasticsearch (검색) + PostgreSQL (장기)
- **카테고리**: AUTH, ACCESS, DATA, ADMIN

### 3. 로그인 보안
- **점진적 지연**: 실패 횟수에 따라 `2^(n-2)` 초 지연 (최대 16초)
- **계정 잠금**: 10회 실패 → 30분, 20회 → 2시간, 30회 → 24시간
- **해제 방법**: 자동 해제, 이메일 인증, 관리자 해제, 비밀번호 재설정

### 4. 보안 헤더
- **위치**: Gateway SecurityHeadersFilter (Global)
- **헤더**: `X-Content-Type-Options`, `X-Frame-Options`, `HSTS`, `CSP` (경로별 차등)
- **CSP**: API는 `default-src 'none'`, Shell은 `script-src 'unsafe-eval'` 허용 (Module Federation)

### 구현 로드맵 (4주)
- Phase 1: Rate Limiting (1주)
- Phase 2: 로그인 보안 (1주)
- Phase 3: 보안 감사 로깅 (2주)
- Phase 4: 보안 헤더 (3일)

## References

- [OWASP Rate Limiting](https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [ADR-003 Admin 권한 검증 전략](./ADR-003-authorization-strategy.md)
- [ADR-008 JWT Stateless + Redis](./ADR-008-jwt-stateless-redis.md)

---

📂 상세: [old-docs/central/adr/ADR-010-security-enhancement-architecture.md](../old-docs/central/adr/ADR-010-security-enhancement-architecture.md)
