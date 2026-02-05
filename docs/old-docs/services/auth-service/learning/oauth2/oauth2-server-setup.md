# Authorization Server 설정

## 개요

Portal Universe auth-service는 Spring Security 기반의 Direct JWT 인증 방식을 사용합니다. 기존 Spring Authorization Server(OIDC)에서 단순화된 JWT 기반 인증으로 전환하여 복잡도를 낮추고 운영 효율성을 높였습니다.

## 아키텍처 결정

### Spring Authorization Server 대신 Direct JWT 선택 이유

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Authorization Server                 │
│  - OIDC Discovery 엔드포인트                                 │
│  - JWKS 공개키 관리                                          │
│  - OAuth2 Client Registration                                │
│  - Token Customizer                                          │
│  ↓ 복잡도 높음                                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Direct JWT (현재 방식)                    │
│  - HMAC-SHA256 대칭키                                        │
│  - TokenService에서 직접 생성/검증                           │
│  - Redis를 통한 토큰 관리                                    │
│  ↓ 단순하고 효율적                                           │
└─────────────────────────────────────────────────────────────┘
```

| 항목 | Spring Authorization Server | Direct JWT |
|------|---------------------------|------------|
| 키 관리 | 비대칭키 (RSA/EC) | 대칭키 (HMAC) |
| 복잡도 | 높음 | 낮음 |
| MSA 연동 | JWKS 엔드포인트 필요 | Secret 공유 |
| 설정량 | 많음 | 최소화 |

## 현재 설정 구조

### AuthorizationServerConfig.java

```java
package com.portal.universe.authservice.config;

import org.springframework.context.annotation.Configuration;

/**
 * JWT 인증 설정을 담당하는 클래스입니다.
 *
 * Direct JWT 인증 방식으로 전환되어, Spring Authorization Server(OIDC)는 사용하지 않습니다.
 * JWT 토큰은 대칭키(HMAC-SHA256)로 서명되며, TokenService에서 생성/검증됩니다.
 *
 * 제거된 항목:
 * - JWKS 엔드포인트: 대칭키 방식에서는 불필요
 * - OAuth2 Client Repository: Direct JWT 방식에서는 불필요
 * - OAuth2 Token Customizer: TokenService에서 직접 claims 설정
 */
@Configuration
public class AuthorizationServerConfig {
    // Direct JWT 방식에서는 추가 설정이 필요 없습니다.
    // JWT 생성/검증은 TokenService에서 담당합니다.
}
```

### JwtConfig.java - JWT 설정 바인딩

```java
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 서명에 사용되는 비밀키
     * 최소 256비트(32바이트) 이상이어야 합니다.
     */
    private String secretKey;

    /**
     * Access Token 만료 시간 (밀리초)
     * 기본값: 900000ms (15분)
     */
    private long accessTokenExpiration;

    /**
     * Refresh Token 만료 시간 (밀리초)
     * 기본값: 604800000ms (7일)
     */
    private long refreshTokenExpiration;
}
```

## application.yml 설정

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}  # 환경변수로 주입
  access-token-expiration: 900000      # 15분
  refresh-token-expiration: 604800000  # 7일

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

## 환경별 설정

### Local 환경 (.env.local)

```bash
JWT_SECRET_KEY=local-development-secret-key-min-32-bytes
REDIS_HOST=localhost
REDIS_PORT=6379
```

### Docker 환경 (.env.docker)

```bash
JWT_SECRET_KEY=docker-compose-secret-key-min-32-bytes
REDIS_HOST=redis
REDIS_PORT=6379
```

### Kubernetes 환경

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-service-secrets
type: Opaque
data:
  jwt-secret-key: <base64-encoded-secret>
```

## 토큰 생성/검증 흐름

```
┌──────────────┐     POST /api/auth/login     ┌──────────────┐
│    Client    │ ──────────────────────────▶  │ AuthController│
└──────────────┘                              └──────────────┘
                                                     │
                                                     ▼
                                              ┌──────────────┐
                                              │ TokenService │
                                              │  - generateAccessToken()
                                              │  - generateRefreshToken()
                                              └──────────────┘
                                                     │
                       ┌─────────────────────────────┼─────────────────────────────┐
                       ▼                             ▼                             ▼
              ┌──────────────┐           ┌──────────────────┐           ┌──────────────┐
              │   JWT 발급    │           │ RefreshTokenService│         │    Redis     │
              │ (HMAC-SHA256) │           │ (Redis 저장)       │           │ refresh_token:{uuid}
              └──────────────┘           └──────────────────┘           └──────────────┘
```

## Best Practices

### 1. Secret Key 관리

```bash
# 256비트 이상의 안전한 키 생성
openssl rand -base64 32
```

### 2. 토큰 만료 시간 설정

| 토큰 타입 | 권장 만료 시간 | 이유 |
|----------|--------------|------|
| Access Token | 15분 | 짧은 수명으로 탈취 피해 최소화 |
| Refresh Token | 7일 | 사용자 편의성과 보안 균형 |

### 3. 다른 서비스에서 토큰 검증

```java
// 다른 마이크로서비스에서 동일한 secret key로 검증
@Service
public class TokenValidator {

    @Value("${jwt.secret-key}")
    private String secretKey;

    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/AuthorizationServerConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/JwtConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java`

## 참고 자료

- [JJWT GitHub](https://github.com/jwtk/jjwt)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
