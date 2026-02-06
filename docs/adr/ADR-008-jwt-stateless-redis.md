# ADR-008: JWT Stateless + Redis 인증 아키텍처 전환

**Status**: Accepted
**Date**: 2026-01-21

## Context

기존 인증 시스템은 Spring Authorization Server의 OIDC Authorization Code Flow와 세션 기반 인증을 혼합 사용했습니다. 이로 인해 복잡한 리다이렉트 플로우, 세션 관리 부담, 프론트엔드 oidc-client-ts 의존성 등의 문제가 발생했으며 마이크로서비스 환경에서 서비스 간 인증 전파가 어려웠습니다. Stateless 아키텍처로 전환하면서도 토큰 즉시 무효화 기능이 필요했습니다.

## Decision

**JWT Stateless + Redis 기반 인증 아키텍처로 전환합니다.**

Access Token은 Stateless 검증(HMAC 서명), Refresh Token과 블랙리스트는 Redis에 저장하여 즉시 무효화를 지원합니다.

## Rationale

- **단순성**: 복잡한 OIDC 플로우 제거, 직관적인 Bearer Token 인증
- **확장성**: Access Token Stateless 검증으로 수평 확장 용이
- **보안성**: Refresh Token Redis 관리 + 블랙리스트로 즉시 무효화 지원
- **일관성**: 일반 로그인과 소셜 로그인의 동일한 JWT 토큰 체계
- **성능**: API Gateway에서 서명 검증만으로 빠른 인증 처리

## Trade-offs

✅ **장점**:
- 완전한 Stateless 아키텍처 (Access Token)
- 서버 측 세션 불필요 (수평 확장 용이)
- 프론트엔드 복잡도 감소 (oidc-client-ts 제거)
- 마이크로서비스 간 인증 전파 간편

⚠️ **단점 및 완화**:
- Redis 의존성 추가 → (완화: 프로덕션 환경에서 Redis HA 구성)
- Access Token 탈취 시 만료까지 사용 가능 → (완화: 짧은 만료 시간 15분, 블랙리스트 지원, HTTPS 필수)

## Implementation

**토큰 전략**:
| 토큰 | 저장 위치 | 만료 시간 | 검증 방식 |
|------|----------|----------|----------|
| Access Token | 클라이언트 메모리 | 15분 | Stateless (HMAC 서명) |
| Refresh Token | Redis | 7일 | Redis 조회 |
| Blacklist | Redis | Access Token 만료까지 | Redis 조회 |

**Backend (auth-service)**:
- `JwtConfig.java` - JWT 설정 (secret, expiration)
- `TokenService.java` - Access/Refresh Token 발급 및 검증
- `RefreshTokenService.java` - Redis Refresh Token 관리
- `TokenBlacklistService.java` - Redis 블랙리스트
- `JwtAuthenticationFilter.java` - JWT 검증 필터
- `AuthController.java` - 로그인/로그아웃/토큰갱신 API

**Frontend (portal-shell)**:
- `authService.ts` - JWT 기반 인증 서비스
- `auth.ts` (store) - 토큰 상태 관리
- `apiClient.ts` - Axios interceptor (자동 토큰 갱신)

**Redis Key 구조**:
```
refresh_token:{user_uuid} → {refresh_token_value} (TTL: 7일)
blacklist:{jti} → "blacklisted" (TTL: Access Token 남은 만료 시간)
```

## References

- [JWT.io Introduction](https://jwt.io/introduction)
- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [ADR-003: Authorization Strategy](./ADR-003-authorization-strategy.md)

---

📂 상세: [old-docs/central/adr/ADR-008-jwt-stateless-redis.md](../old-docs/central/adr/ADR-008-jwt-stateless-redis.md)
