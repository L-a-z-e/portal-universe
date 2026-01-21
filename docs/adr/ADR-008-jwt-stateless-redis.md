---
id: ADR-008
title: JWT Stateless + Redis 인증 아키텍처 전환
type: adr
status: accepted
created: 2026-01-21
updated: 2026-01-21
author: Laze
decision_date: 2026-01-21
reviewers: []
tags: [authentication, jwt, redis, security, stateless]
related:
  - ADR-003
  - ADR-004
---

# ADR-008: JWT Stateless + Redis 인증 아키텍처 전환

## 메타데이터

| 항목 | 내용 |
|------|------|
| **상태** | Accepted |
| **결정일** | 2026-01-21 |
| **작성자** | Laze |

---

## Context (배경)

### 문제 상황

기존 인증 시스템은 Spring Authorization Server의 OIDC Authorization Code Flow와 세션 기반 인증을 혼합하여 사용했습니다. 이로 인해 다음과 같은 문제가 발생했습니다:

1. **복잡한 인증 플로우**: OIDC 표준을 위한 다단계 리다이렉트 필요
2. **세션 관리 부담**: 서버 측 세션 저장소 필요, 수평 확장 시 세션 동기화 문제
3. **프론트엔드 복잡도**: oidc-client-ts 라이브러리 의존, 복잡한 토큰 관리
4. **마이크로서비스 부적합**: 서비스 간 인증 전파가 어려움

### 기존 아키텍처

```
Frontend → Authorization Server → Token 발급
         ↓
     OIDC Code Flow (복잡한 리다이렉트)
         ↓
     세션 + JWT 하이브리드 관리
```

### 기술적 요구사항

1. **Stateless 인증**: 서버 측 세션 없이 토큰만으로 인증
2. **토큰 즉시 무효화**: 로그아웃 시 발급된 토큰 즉시 무효화 필요
3. **소셜 로그인 지원**: Google, Naver, Kakao OAuth2 로그인 유지
4. **마이크로서비스 호환**: Gateway에서 JWT 검증, 서비스로 전파

---

## Decision Drivers (결정 요인)

1. **단순성**: 복잡한 OIDC 플로우 대신 직관적인 JWT 기반 인증
2. **확장성**: Stateless 아키텍처로 수평 확장 용이
3. **보안성**: Refresh Token 관리 및 토큰 블랙리스트로 보안 강화
4. **일관성**: 일반 로그인과 소셜 로그인의 동일한 토큰 체계
5. **성능**: Redis 기반 빠른 토큰 조회

---

## Considered Options (검토한 대안)

### Option 1: JWT Stateless + Redis (선택됨)

**아키텍처**:
```
┌─────────────────────────────────────────────────────────┐
│                    Client (Frontend)                     │
│                  Authorization: Bearer {JWT}             │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│               API Gateway (JWT 서명 검증)                │
│              Access Token: Stateless 검증               │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
   ┌───────────┐   ┌───────────┐   ┌───────────┐
   │Auth Service│   │Blog Service│   │Shopping  │
   └─────┬─────┘   └───────────┘   └───────────┘
         │
         ▼
   ┌───────────┐
   │   Redis   │
   │ - Refresh │
   │ - Blacklist│
   └───────────┘
```

**토큰 전략**:
| 토큰 | 저장 위치 | 만료 시간 | 검증 방식 |
|------|----------|----------|----------|
| Access Token | 클라이언트 메모리 | 15분 | Stateless (HMAC 서명) |
| Refresh Token | Redis | 7일 | Redis 조회 |
| Blacklist | Redis | Access Token 만료까지 | Redis 조회 |

**장점**:
- 완전한 Stateless 아키텍처 (Access Token)
- Redis로 토큰 즉시 무효화 가능
- 단순하고 직관적인 인증 플로우
- 마이크로서비스 확장에 적합

**단점**:
- Redis 의존성 추가
- Access Token 탈취 시 만료까지 사용 가능 (블랙리스트로 완화)

---

### Option 2: Spring Authorization Server OIDC 유지

**변경 사항**: 기존 아키텍처 유지

**장점**:
- OAuth 2.0/OIDC 표준 준수
- 타사 클라이언트 연동 용이

**단점**:
- 복잡한 인증 플로우 유지
- 세션 기반 문제 해결 안됨
- 프론트엔드 복잡도 유지

**평가**: **부적합** - 현재 문제 해결 안됨

---

### Option 3: Spring Session + Redis

**변경 사항**: 세션을 Redis에 저장하여 분산 환경 지원

**장점**:
- 기존 세션 기반 코드 최소 변경
- Spring Session 통합 용이

**단점**:
- 여전히 세션 기반 (Stateful)
- 모든 요청마다 Redis 조회 필요
- JWT의 장점 활용 못함

**평가**: **차선책** - Stateless 장점 포기

---

## Option 비교표

| 항목 | JWT + Redis | OIDC 유지 | Session + Redis |
|------|-------------|----------|-----------------|
| **Stateless** | O | X | X |
| **확장성** | 우수 | 보통 | 보통 |
| **복잡도** | 낮음 | 높음 | 낮음 |
| **즉시 무효화** | O | X | O |
| **표준 준수** | JWT | OIDC | 세션 |
| **Redis 의존** | O | X | O |

---

## Decision (최종 결정)

**Option 1: JWT Stateless + Redis 아키텍처를 채택합니다.**

### 구현된 컴포넌트

#### Backend (auth-service)

| 파일 | 역할 |
|------|------|
| `JwtConfig.java` | JWT 설정 (secret, expiration) |
| `RedisConfig.java` | Redis 연결 설정 |
| `TokenService.java` | Access/Refresh Token 발급 및 검증 |
| `RefreshTokenService.java` | Redis Refresh Token 관리 |
| `TokenBlacklistService.java` | Redis Access Token 블랙리스트 |
| `JwtAuthenticationFilter.java` | JWT 검증 필터 |
| `AuthController.java` | 로그인/로그아웃/토큰갱신 API |
| `OAuth2AuthenticationSuccessHandler.java` | 소셜 로그인 JWT 발급 |

#### Frontend (portal-shell)

| 파일 | 역할 |
|------|------|
| `authService.ts` | JWT 기반 인증 서비스 |
| `auth.ts` (store) | 토큰 상태 관리 |
| `OAuth2Callback.vue` | 소셜 로그인 콜백 처리 |
| `apiClient.ts` | Axios interceptor (자동 토큰 갱신) |

### API 명세

```
POST /api/auth/login
  Body: { "email": "...", "password": "..." }
  Response: {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 900,
    "tokenType": "Bearer"
  }

POST /api/auth/refresh
  Body: { "refreshToken": "..." }
  Response: { "accessToken": "...", "expiresIn": 900 }

POST /api/auth/logout
  Header: Authorization: Bearer {accessToken}
  Body: { "refreshToken": "..." }
  Response: { "message": "로그아웃 성공" }
```

### Redis Key 구조

```
refresh_token:{user_uuid} → {refresh_token_value}
  TTL: 7일

blacklist:{jti} → "blacklisted"
  TTL: Access Token 남은 만료 시간
```

---

## Consequences (영향)

### 긍정적 영향

1. **아키텍처 단순화**
   - OIDC 복잡한 플로우 제거
   - 프론트엔드에서 oidc-client-ts 제거
   - 직관적인 Bearer Token 인증

2. **확장성 향상**
   - Access Token Stateless 검증으로 서버 부담 감소
   - 수평 확장 시 세션 동기화 불필요
   - Gateway에서 JWT 검증 가능

3. **보안 강화**
   - Refresh Token Redis 저장으로 서버 측 관리
   - 토큰 블랙리스트로 즉시 무효화 지원
   - Access Token 짧은 만료 시간 (15분)

4. **일관된 인증 체계**
   - 일반 로그인, 소셜 로그인 동일한 JWT 발급
   - 모든 서비스에서 동일한 토큰 검증 방식

### 부정적 영향 (트레이드오프)

1. **Redis 의존성**
   - Redis 장애 시 Refresh Token 갱신 불가
   - 운영 환경에 Redis HA 구성 필요

2. **토큰 탈취 위험**
   - Access Token 탈취 시 만료까지 사용 가능
   - 완화: 짧은 만료 시간(15분), HTTPS 필수, 블랙리스트

3. **기존 코드 변경**
   - Spring Authorization Server 관련 코드 제거
   - 프론트엔드 인증 로직 재작성

---

## 검증 결과

### 통합 테스트 (2026-01-21)

| 테스트 | 결과 |
|--------|------|
| 일반 로그인 | ✅ Access + Refresh Token 발급 |
| 토큰 갱신 | ✅ 새 Access Token 발급 |
| 로그아웃 | ✅ 블랙리스트 등록 + Refresh 삭제 |
| 블랙리스트 검증 | ✅ 삭제된 토큰 거부 |
| 소셜 로그인 | ✅ OAuth2 → JWT 변환 |
| 프론트엔드 통합 | ✅ 로그인/로그아웃 정상 작동 |

### 검증 명령

```bash
# Redis 상태 확인
docker exec -it redis redis-cli KEYS "*"

# 로그인 테스트
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test1234"}'

# 토큰 갱신 테스트
curl -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"..."}'
```

---

## 롤백 계획

문제 발생 시:
1. `SecurityConfig.java`에서 JWT 필터 제거, 세션 기반 복원
2. `AuthorizationServerConfig.java` OIDC 설정 복원
3. 프론트엔드 oidc-client-ts 재설치
4. Gateway JWT 검증 비활성화

---

## 향후 고려사항

1. **토큰 저장 전략**: HttpOnly Cookie vs 메모리 (XSS/CSRF 트레이드오프)
2. **Redis HA**: Sentinel 또는 Cluster 구성
3. **Rate Limiting**: 로그인 시도 제한
4. **Audit Log**: 인증 이벤트 로깅

---

## 참고 자료

- [JWT.io Introduction](https://jwt.io/introduction)
- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)

---

**최종 업데이트**: 2026-01-21
**작성자**: Laze
