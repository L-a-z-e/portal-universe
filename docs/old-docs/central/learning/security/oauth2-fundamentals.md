# OAuth2 Fundamentals

OAuth2는 제3자 애플리케이션이 사용자의 리소스에 안전하게 접근할 수 있도록 하는 **권한 위임(Authorization Delegation)** 프레임워크입니다.

## 1. OAuth2의 핵심 개념

### 1.1 참여자 (Roles)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          OAuth2 Participants                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────┐                       ┌─────────────────┐         │
│  │  Resource Owner │                       │ Resource Server │         │
│  │    (사용자)      │                       │  (보호된 리소스)  │         │
│  └────────┬────────┘                       └────────▲────────┘         │
│           │                                         │                   │
│           │ 권한 부여                          Access Token              │
│           ▼                                         │                   │
│  ┌─────────────────┐    Authorization Code   ┌─────────────────┐       │
│  │     Client      │ ◀──────────────────────▶│ Authorization   │       │
│  │  (애플리케이션)   │     Access Token         │    Server       │       │
│  └─────────────────┘                         │   (인가 서버)    │       │
│                                              └─────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
```

| 역할 | 설명 | Portal Universe 매핑 |
|------|------|---------------------|
| **Resource Owner** | 리소스 소유자 (최종 사용자) | 블로그/쇼핑 사용자 |
| **Client** | 리소스에 접근하려는 애플리케이션 | portal-shell, shopping-frontend |
| **Authorization Server** | 인증/인가를 처리하고 토큰 발급 | auth-service |
| **Resource Server** | 보호된 리소스를 제공하는 서버 | blog-service, shopping-service |

### 1.2 토큰 종류

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Token Types                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     Access Token                                 │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │  - 리소스 접근 권한을 나타내는 토큰                                 │   │
│  │  - 짧은 만료 시간 (15분 ~ 1시간)                                   │   │
│  │  - Bearer 토큰으로 HTTP 헤더에 포함                                │   │
│  │  - 탈취 시 위험 → 짧은 수명으로 완화                               │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Refresh Token                                 │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │  - 새로운 Access Token을 발급받기 위한 토큰                        │   │
│  │  - 긴 만료 시간 (7일 ~ 30일)                                      │   │
│  │  - 안전한 저장소에 보관 (서버 측 Redis 등)                         │   │
│  │  - 한 번 사용 후 갱신하는 Rotation 전략 권장                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                  Authorization Code                              │   │
│  ├─────────────────────────────────────────────────────────────────┤   │
│  │  - Access Token 교환용 임시 코드                                  │   │
│  │  - 매우 짧은 수명 (보통 10분)                                      │   │
│  │  - 일회용 (한 번 사용 후 무효화)                                   │   │
│  │  - 프론트 채널에서 백 채널로 안전하게 전달                          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. OAuth2 Grant Types

OAuth2는 다양한 시나리오에 맞는 여러 Grant Type을 제공합니다.

### 2.1 Authorization Code Grant

**가장 안전한 방식**으로, 서버 사이드 애플리케이션에 권장됩니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Authorization Code Flow                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    User        Browser         Client           Auth Server    Resource      │
│     │            │               │                   │          Server       │
│     │            │               │                   │            │          │
│  1. │──Login────▶│               │                   │            │          │
│     │            │──Redirect────▶│                   │            │          │
│     │            │               │                   │            │          │
│  2. │            │               │──Authorization───▶│            │          │
│     │            │               │    Request        │            │          │
│     │            │               │                   │            │          │
│  3. │◀───────────│───────────────│◀──Login Page─────│            │          │
│     │            │               │                   │            │          │
│  4. │──Credentials───────────────│──────────────────▶│            │          │
│     │            │               │                   │            │          │
│  5. │            │◀──────────────│◀──Authorization──│            │          │
│     │            │   Redirect    │      Code         │            │          │
│     │            │   + Code      │                   │            │          │
│     │            │               │                   │            │          │
│  6. │            │               │───Token Request──▶│            │          │
│     │            │               │   (Code + Secret) │            │          │
│     │            │               │                   │            │          │
│  7. │            │               │◀──Access Token───│            │          │
│     │            │               │   Refresh Token   │            │          │
│     │            │               │                   │            │          │
│  8. │            │               │───API Call───────│────────────▶│          │
│     │            │               │   (Access Token)  │            │          │
│     │            │               │                   │            │          │
│  9. │            │◀──────────────│◀──────────────────│◀──Resource─│          │
│     │            │   Response    │                   │            │          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Authorization Request

```http
GET /oauth2/authorize?
    response_type=code
    &client_id=portal-shell
    &redirect_uri=https://portal.example.com/callback
    &scope=openid profile email
    &state=xyz123                    # CSRF 방지
```

#### Token Request

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https://portal.example.com/callback
&client_id=portal-shell
&client_secret=secret123
```

#### Token Response

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 900,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2g...",
  "scope": "openid profile email"
}
```

### 2.2 Client Credentials Grant

**서버 간 통신**에 사용됩니다. 사용자 컨텍스트 없이 클라이언트 자체가 인증됩니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Client Credentials Flow                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    Service A                   Auth Server                   Service B       │
│        │                           │                             │           │
│        │                           │                             │           │
│     1. │───Token Request──────────▶│                             │           │
│        │   client_id + secret      │                             │           │
│        │                           │                             │           │
│     2. │◀──Access Token────────────│                             │           │
│        │                           │                             │           │
│     3. │───API Call + Token────────│─────────────────────────────▶│          │
│        │                           │                             │           │
│     4. │◀──────────────────────────│◀──────────Response──────────│           │
│        │                           │                             │           │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### 사용 사례

```java
// blog-service가 auth-service의 사용자 정보를 조회할 때
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUser(
        @PathVariable String userId,
        @RequestHeader("Authorization") String token  // Service Token
    );
}
```

#### Token Request

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic base64(client_id:client_secret)

grant_type=client_credentials
&scope=service.read service.write
```

### 2.3 Refresh Token Grant

Access Token이 만료되면 **Refresh Token으로 새 Access Token을 발급**받습니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Refresh Token Flow                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    Client                          Auth Server                               │
│      │                                  │                                    │
│      │                                  │                                    │
│   1. │───Access Token (Expired)─────────│──▶ Resource Server                │
│      │                                  │      ↓                             │
│      │◀──────401 Unauthorized───────────│◀────┘                             │
│      │                                  │                                    │
│   2. │───Refresh Token─────────────────▶│                                    │
│      │   grant_type=refresh_token       │                                    │
│      │                                  │──Validate Token                    │
│      │                                  │──Check Redis                       │
│      │                                  │                                    │
│   3. │◀──New Access Token───────────────│                                    │
│      │   (+ Optional: New Refresh)      │                                    │
│      │                                  │                                    │
│   4. │───Retry Request──────────────────│──▶ Resource Server                │
│      │◀──────Response───────────────────│◀────┘                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

#### Portal Universe에서의 구현

```java
// AuthController.java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @Valid @RequestBody RefreshRequest request) {

    // 1. Refresh Token JWT 서명 검증
    Claims claims = tokenService.validateAccessToken(request.refreshToken());
    String userId = claims.getSubject();

    // 2. Redis에 저장된 Refresh Token과 비교
    if (!refreshTokenService.validateRefreshToken(userId, request.refreshToken())) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 3. 새로운 Access Token 발급
    User user = userRepository.findByUuidWithProfile(userId)
            .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

    String accessToken = tokenService.generateAccessToken(user);

    return ResponseEntity.ok(ApiResponse.success(
        new RefreshResponse(accessToken, 900)));  // 15분
}
```

---

## 3. OAuth2 소셜 로그인 (Social Login)

### 3.1 지원 제공자

Portal Universe는 다음 OAuth2 제공자를 지원합니다:

| Provider | 특징 | 반환 데이터 |
|----------|------|-----------|
| **Google** | 글로벌, OpenID Connect 지원 | email, name, picture |
| **Kakao** | 국내 최다 사용자 | kakao_account.email, properties.nickname |
| **Naver** | 국내, 이메일 인증됨 | response.email, response.name |

### 3.2 소셜 로그인 플로우

```
┌──────────────────────────────────────────────────────────────────────────────┐
│               Portal Universe Social Login Flow                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Browser          Portal Shell       Auth Service        Social Provider     │
│     │                  │                  │                    │             │
│     │                  │                  │                    │             │
│  1. │──Login Click────▶│                  │                    │             │
│     │                  │                  │                    │             │
│  2. │                  │──Redirect───────▶│                    │             │
│     │◀─────────────────│──────────────────│───Redirect────────▶│            │
│     │                  │                  │                    │             │
│  3. │──User Login──────│──────────────────│───────────────────▶│            │
│     │                  │                  │                    │             │
│  4. │◀──Callback + Code│◀─────────────────│◀──Authorization───│            │
│     │                  │                  │      Code          │             │
│     │                  │                  │                    │             │
│  5. │                  │                  │───Token Request───▶│            │
│     │                  │                  │◀──Access Token─────│            │
│     │                  │                  │                    │             │
│  6. │                  │                  │───UserInfo Request▶│            │
│     │                  │                  │◀──User Profile─────│            │
│     │                  │                  │                    │             │
│  7. │                  │                  │ Create/Link User   │             │
│     │                  │                  │ Generate JWT       │             │
│     │                  │                  │                    │             │
│  8. │◀──Fragment Redirect───────────────│                    │             │
│     │   #access_token=...               │                    │             │
│     │                  │                  │                    │             │
│  9. │ Extract Token    │                  │                    │             │
│     │ Store in Memory  │                  │                    │             │
│     │──────────────────▶│                  │                    │             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 OAuth2UserInfo 구현

```java
// OAuth2UserInfoFactory.java
public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId,
            Map<String, Object> attributes) {

        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                "Unsupported provider: " + registrationId);
        };
    }
}

// GoogleOAuth2UserInfo.java
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
```

### 3.4 소셜 계정 연동 로직

```java
// CustomOAuth2UserService.java
private User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
    SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());
    String providerId = userInfo.getId();
    String email = userInfo.getEmail();

    // 1. 기존 소셜 계정으로 가입된 사용자 확인
    Optional<SocialAccount> existingSocialAccount = socialAccountRepository
            .findByProviderAndProviderId(provider, providerId);

    if (existingSocialAccount.isPresent()) {
        return existingSocialAccount.get().getUser();
    }

    // 2. 동일 이메일로 가입된 사용자가 있는지 확인
    Optional<User> existingUser = userRepository.findByEmail(email);

    if (existingUser.isPresent()) {
        // 기존 사용자에게 소셜 계정 연동
        User user = existingUser.get();
        linkSocialAccount(user, provider, providerId);
        return user;
    }

    // 3. 신규 사용자 생성
    return createNewUser(provider, providerId, userInfo);
}
```

---

## 4. Security 고려사항

### 4.1 State Parameter (CSRF 방지)

```javascript
// Frontend: 로그인 시작
function startOAuth2Login(provider) {
    const state = generateRandomString(32);
    sessionStorage.setItem('oauth2_state', state);

    window.location.href = `/oauth2/authorization/${provider}?state=${state}`;
}

// Callback 처리
function handleCallback() {
    const urlParams = new URLSearchParams(window.location.search);
    const state = urlParams.get('state');
    const savedState = sessionStorage.getItem('oauth2_state');

    if (state !== savedState) {
        throw new Error('CSRF attack detected!');
    }

    // Continue with token extraction...
}
```

### 4.2 Redirect URI 검증

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
```

Authorization Server는 등록된 redirect URI만 허용해야 합니다.

### 4.3 Token Storage

| 저장 위치 | 장점 | 단점 | 권장 |
|----------|------|------|------|
| **LocalStorage** | 접근 용이 | XSS 취약 | 비권장 |
| **SessionStorage** | 탭 격리 | XSS 취약 | 단기 임시 저장 |
| **Memory** | XSS 안전 | 새로고침 시 손실 | Access Token |
| **HttpOnly Cookie** | XSS 안전 | CSRF 필요 | Refresh Token |
| **Server (Redis)** | 완전 안전 | 추가 요청 | Portal Universe 방식 |

---

## 5. 다음 단계

1. [JWT Deep Dive](./jwt-deep-dive.md) - JWT 토큰의 구조와 보안
2. [PKCE for SPA](./pkce-spa-security.md) - SPA에서의 안전한 OAuth2
3. [Portal Universe Auth Flow](./portal-universe-auth-flow.md) - 실제 구현 분석
