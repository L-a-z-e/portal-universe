# Authorization Code Flow (PKCE)

## 개요

Portal Universe auth-service는 두 가지 인증 방식을 지원합니다:

1. **Direct Login**: 자체 이메일/비밀번호 인증
2. **OAuth2 Social Login**: Google, Naver, Kakao 등 소셜 로그인

소셜 로그인에서는 Authorization Code Flow를 사용하며, SPA(Single Page Application) 환경에서는 PKCE(Proof Key for Code Exchange)를 통해 보안을 강화합니다.

## Authorization Code Flow 이해

### 기본 흐름

```
┌──────────┐                                    ┌──────────────┐
│  사용자   │                                    │  Auth Server │
│ (Browser)│                                    │ (auth-service)
└────┬─────┘                                    └──────┬───────┘
     │                                                 │
     │  1. 소셜 로그인 버튼 클릭                        │
     │─────────────────────────────────────────────────▶
     │                                                 │
     │  2. Redirect to Social Provider                │
     │◀─────────────────────────────────────────────────
     │                                                 │
     │                    ┌──────────────────┐         │
     │  3. 소셜 인증      │  Social Provider │         │
     │───────────────────▶│ (Google/Naver)   │         │
     │                    └────────┬─────────┘         │
     │  4. Authorization Code      │                   │
     │◀────────────────────────────│                   │
     │                                                 │
     │  5. Code + redirect_uri                        │
     │─────────────────────────────────────────────────▶
     │                                                 │
     │  6. Exchange Code for Tokens                   │
     │                    ┌──────────────────┐         │
     │                    │  Social Provider │◀────────│
     │                    └──────────────────┘         │
     │                                                 │
     │  7. JWT Tokens (Access + Refresh)              │
     │◀─────────────────────────────────────────────────
     │                                                 │
```

## 구현 상세

### 1. OAuth2 로그인 엔드포인트

```
GET /oauth2/authorization/{provider}
```

- `{provider}`: google, naver, kakao
- Spring Security가 자동으로 소셜 로그인 URL로 리다이렉트

### 2. Callback URL 처리

```
GET /login/oauth2/code/{provider}
```

- Authorization Code를 받아 Token 교환
- `CustomOAuth2UserService`에서 사용자 정보 처리

### 3. CustomOAuth2UserService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 1. Provider 정보 추출
        String registrationId = userRequest.getClientRegistration()
                                          .getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

        // 2. Provider별 사용자 정보 파싱
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
            .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 3. 사용자 생성 또는 조회
        User user = processOAuth2User(registrationId, userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes(),
                                   userNameAttributeName);
    }
}
```

### 4. 사용자 처리 로직

```java
private User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
    SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());
    String providerId = userInfo.getId();
    String email = userInfo.getEmail();

    // Case 1: 기존 소셜 계정 존재
    Optional<SocialAccount> existingSocialAccount = socialAccountRepository
            .findByProviderAndProviderId(provider, providerId);
    if (existingSocialAccount.isPresent()) {
        return existingSocialAccount.get().getUser();
    }

    // Case 2: 동일 이메일 사용자 존재 - 소셜 계정 연동
    Optional<User> existingUser = userRepository.findByEmail(email);
    if (existingUser.isPresent()) {
        User user = existingUser.get();
        linkSocialAccount(user, provider, providerId);
        return user;
    }

    // Case 3: 신규 사용자 생성
    return createNewUser(provider, providerId, userInfo);
}
```

## PKCE 구현 (SPA용)

### PKCE란?

Public Client(SPA, 모바일 앱)에서 Authorization Code가 탈취되어도 Token 교환을 방지하는 메커니즘입니다.

```
┌─────────────────────────────────────────────────────────────┐
│                      PKCE Flow                               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. code_verifier 생성 (랜덤 문자열)                         │
│     → "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"        │
│                                                              │
│  2. code_challenge 생성 (SHA256 해시)                        │
│     → BASE64URL(SHA256(code_verifier))                      │
│                                                              │
│  3. Authorization Request에 code_challenge 포함             │
│     → /oauth2/authorize?code_challenge=xxx&...              │
│                                                              │
│  4. Token Request에 code_verifier 포함                       │
│     → POST /oauth2/token { code_verifier: "xxx" }           │
│                                                              │
│  5. Server에서 code_verifier 검증                            │
│     → SHA256(code_verifier) == stored_code_challenge        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Frontend 구현 예시

```typescript
// PKCE 유틸리티
function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const hash = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(hash));
}

// 로그인 시작
async function initiateOAuthLogin(provider: string) {
  const codeVerifier = generateCodeVerifier();
  const codeChallenge = await generateCodeChallenge(codeVerifier);

  // verifier를 sessionStorage에 임시 저장
  sessionStorage.setItem('pkce_verifier', codeVerifier);

  // Authorization 요청
  const authUrl = new URL(`/oauth2/authorization/${provider}`, AUTH_BASE_URL);
  authUrl.searchParams.set('code_challenge', codeChallenge);
  authUrl.searchParams.set('code_challenge_method', 'S256');

  window.location.href = authUrl.toString();
}
```

## Success Handler 처리

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.base-url:http://localhost:30000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // JWT 토큰 발급
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        // Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        // URL Fragment로 토큰 전달 (보안상 query string보다 안전)
        String targetUrl = UriComponentsBuilder
            .fromUriString(frontendBaseUrl + "/oauth2/callback")
            .fragment("access_token=" + accessToken +
                     "&refresh_token=" + refreshToken +
                     "&expires_in=900")
            .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

## Frontend Callback 처리

```typescript
// /oauth2/callback 페이지
export function OAuth2Callback() {
  useEffect(() => {
    // URL Fragment에서 토큰 추출
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken && refreshToken) {
      // 토큰 저장
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);

      // 메인 페이지로 이동
      window.location.href = '/';
    } else {
      // 에러 처리
      console.error('OAuth2 callback failed');
    }
  }, []);

  return <div>Processing login...</div>;
}
```

## 보안 고려사항

### 1. State Parameter

CSRF 공격 방지를 위한 state 파라미터 사용:

```java
// Spring Security가 자동으로 처리
// 요청 시 state 생성 → 콜백 시 state 검증
```

### 2. Token 전달 방식

| 방식 | 장점 | 단점 | 권장 |
|------|-----|------|-----|
| Query String | 구현 간단 | 서버 로그에 노출 | X |
| URL Fragment | 서버로 전송 안됨 | JavaScript 필요 | O |
| HttpOnly Cookie | 가장 안전 | CORS 설정 복잡 | 고려 |

### 3. Refresh Token 보안

```java
// Redis에 저장하여 탈취 시 무효화 가능
refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

// 로그아웃 시 삭제
refreshTokenService.deleteRefreshToken(userId);
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/CustomOAuth2UserService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/OAuth2AuthenticationSuccessHandler.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/OAuth2UserInfo.java`

## 참고 자료

- [OAuth 2.0 Authorization Code Flow](https://oauth.net/2/grant-types/authorization-code/)
- [PKCE RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636)
