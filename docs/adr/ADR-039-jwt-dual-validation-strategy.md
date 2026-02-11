# ADR-039: JWT 검증 전략 — Gateway 단일 검증으로 통일

**Status**: Accepted
**Date**: 2026-02-11
**Author**: Laze
**Supersedes**: 기존 auth-service 이중 검증 구조

## Context

Portal Universe는 Spring Cloud Gateway를 통해 모든 외부 요청을 중앙에서 처리하며, Gateway가 JWT 검증 후 `X-User-Id`, `X-User-Roles`, `X-User-Effective-Roles` 등의 헤더를 설정하여 하위 서비스로 전달합니다. 대부분의 하위 서비스(blog-service, shopping-service 등)는 Gateway가 설정한 헤더를 신뢰하고 JWT를 직접 검증하지 않습니다.

그러나 auth-service만 Gateway의 JwtAuthenticationFilter(WebFlux)와 별도로 자체 JwtAuthenticationFilter(Servlet)를 통해 JWT를 2차 검증하고 있습니다. Gateway를 통한 정상 경로에서는 JWT 서명 검증과 Redis 블랙리스트 조회가 2회 중복되며, 서비스 간 인증 구조의 일관성이 깨지는 문제가 있습니다.

## Decision

auth-service의 자체 JWT 검증(JwtAuthenticationFilter)을 제거하고, 다른 서비스와 동일하게 Gateway가 설정한 `X-User-*` 헤더를 신뢰하는 단일 검증 구조로 통일합니다.

## Alternatives

| 대안 | 장점 | 단점 |
|------|------|------|
| **A: Gateway만 검증 (채택)** | 검증 1회로 성능 우수, 모든 서비스가 동일한 구조, Redis 조회 중복 제거 | Gateway 우회 시 auth-service 무방비 |
| B: 모든 서비스에서 이중 검증 | 완전한 Defense in Depth | 모든 서비스에 JWT 키 배포 필요, Redis 블랙리스트 조회 N배 증가, 성능 저하 |
| C: auth-service만 이중 검증 (기존) | auth-service 독립 실행 가능, 내부 우회 방어 | auth-service만 구조가 다름, Gateway 통과 시 중복 작업 |

## Rationale

- 모든 서비스가 동일한 인증 패턴을 따르면 구조를 이해하기 쉽고 유지보수 비용이 줄어듭니다.
- Gateway가 이미 JWT 검증 + 블랙리스트 체크 + Role Hierarchy 해석을 수행하므로, auth-service에서 같은 작업을 반복할 이유가 없습니다.
- Gateway 우회는 네트워크 정책(Kubernetes NetworkPolicy, 보안 그룹 등)으로 방어하는 것이 적절하며, 애플리케이션 레벨의 이중 검증보다 인프라 레벨의 접근 제어가 더 효과적입니다.
- auth-service의 인증 필요 API(프로필 조회 등)는 Gateway가 설정한 `X-User-Id` 헤더로 사용자를 식별하면 충분합니다.
- JWT 비밀 키를 auth-service(발급)와 Gateway(검증)만 보유하는 것은 변경 없이 유지됩니다.

## Trade-offs

✅ **장점**:
- JWT 검증 + Redis 블랙리스트 조회가 1회로 줄어 auth-service 경로 성능 개선
- 모든 서비스가 동일한 인증 패턴을 따라 구조의 일관성 확보
- auth-service의 JwtAuthenticationFilter 및 관련 설정 코드 제거로 복잡도 감소

⚠️ **단점 및 완화**:
- Gateway 우회 시 auth-service가 무방비 상태 → (완화: Kubernetes NetworkPolicy로 내부 서비스 포트 외부 접근 차단, 개발 환경에서도 Gateway 경유 습관화)
- auth-service 독립 실행(Standalone) 시 인증 미동작 → (완화: 개발 시 Gateway를 함께 실행하거나, 테스트 프로파일에서 인증 비활성화)

## Implementation

### 변경 필요 사항

1. **제거**: `services/auth-service/src/main/java/.../auth/security/JwtAuthenticationFilter.java`
   - auth-service의 자체 JWT 검증 필터 제거
2. **수정**: auth-service SecurityConfig에서 JwtAuthenticationFilter 등록 해제, GatewayAuthenticationFilter 등록
3. **변경 불필요**: 컨트롤러 수정 불필요 — GatewayAuthenticationFilter가 동일하게 SecurityContext를 설정하므로 `@AuthenticationPrincipal`이 그대로 동작
4. **유지**: `TokenBlacklistService`는 블랙리스트 **등록(쓰기)** 전용이므로 그대로 유지

### 변경 없는 사항

- `services/api-gateway/src/main/java/.../filter/JwtAuthenticationFilter.java` — Gateway 검증 유지
- `services/api-gateway/src/main/java/.../service/TokenBlacklistChecker.java` — Gateway 블랙리스트 체크 유지
- `services/auth-service/src/main/java/.../auth/service/TokenBlacklistService.java` — 로그아웃 시 블랙리스트 등록 유지

## References

- [ADR-008: JWT Stateless + Redis 인증 전환](./ADR-008-jwt-stateless-redis.md)
- [ADR-029: Cross-cutting 보안 처리 계층 설계](./ADR-029-cross-cutting-security-layer.md)
- [ADR-035: Polyglot 서비스 인증 표준화](./ADR-035-polyglot-authentication-standardization.md)

---

## 변경 이력

| 날짜 | 변경 내용 | 작성자 |
|------|----------|--------|
| 2026-02-11 | 초안 작성 — Gateway 단일 검증으로 결정 | Laze |
| 2026-02-12 | 구현 완료 — Status: Accepted로 변경 | Laze |
