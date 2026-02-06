# Social Login (Google, GitHub OAuth)

## 개요

Portal Universe auth-service는 Spring Security OAuth2 Client를 사용하여 Google, Naver, Kakao 등 다양한 소셜 로그인을 지원합니다. 소셜 로그인 사용자는 비밀번호 없이 소셜 계정으로 인증하며, 기존 계정과의 연동도 가능합니다.

## 지원 Provider

| Provider | 특징 | 사용자 정보 |
|----------|------|------------|
| Google | OpenID Connect 지원 | sub, email, name, picture |
| Naver | 한국 사용자 다수 | id, email, nickname, profile_image |
| Kakao | 한국 사용자 다수 | id, email, nickname, profile_image |
| GitHub | 개발자 대상 | id, email, login, avatar_url |

## 아키텍처

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│  Client  │────▶│ auth-service │────▶│ Social Provider  │
│ (Browser)│◀────│    OAuth2    │◀────│ (Google, Naver)  │
└──────────┘     └──────────────┘     └──────────────────┘
     │                 │                      │
     │  1. 소셜 로그인  │                      │
     │  버튼 클릭      │                      │
     │─────────────▶  │  2. Redirect to      │
     │                 │  Provider            │
     │                 │─────────────────────▶│
     │                 │                      │
     │  3. 사용자 인증 │                      │
     │◀───────────────────────────────────────│
     │                 │  4. Auth Code        │
     │                 │◀─────────────────────│
     │                 │                      │
     │                 │  5. Exchange for     │
     │                 │  Access Token        │
     │                 │─────────────────────▶│
     │                 │                      │
     │                 │  6. User Info        │
     │                 │◀─────────────────────│
     │                 │                      │
     │  7. JWT 토큰    │                      │
     │◀────────────────│                      │
```

## 설정

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope:
              - email
              - profile
          naver:
            client-id: ${NAVER_CLIENT_ID}
            client-secret: ${NAVER_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            scope:
              - name
              - email
              - profile_image
            client-name: Naver
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            scope:
              - profile_nickname
              - profile_image
              - account_email
            client-name: Kakao
            client-authentication-method: client_secret_post
        provider:
          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

app:
  frontend:
    base-url: ${FRONTEND_BASE_URL:http://localhost:30000}
```

## 구현 상세

### OAuth2UserInfo 추상 클래스

```java
/**
 * 소셜 로그인 제공자별 사용자 정보를 추상화한 인터페이스입니다.
 * 각 제공자마다 응답 형식이 다르므로 일관된 방식으로 정보를 추출합니다.
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** 소셜 로그인 제공자에서 발급한 고유 식별자 */
    public abstract String getId();

    /** 사용자 이메일 */
    public abstract String getEmail();

    /** 사용자 이름 (닉네임) */
    public abstract String getName();

    /** 프로필 이미지 URL */
    public abstract String getImageUrl();
}
```

### GoogleOAuth2UserInfo

```java
/**
 * Google OAuth2 사용자 정보를 파싱하는 클래스입니다.
 *
 * Google UserInfo 응답 예시:
 * {
 *   "sub": "1234567890",
 *   "name": "홍길동",
 *   "given_name": "길동",
 *   "family_name": "홍",
 *   "picture": "https://lh3.googleusercontent.com/...",
 *   "email": "user@gmail.com",
 *   "email_verified": true,
 *   "locale": "ko"
 * }
 */
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

### OAuth2UserInfoFactory

```java
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId, Map<String, Object> attributes) {

        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "naver" -> new NaverOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            case "github" -> new GitHubOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                "Unsupported provider: " + registrationId
            );
        };
    }
}
```

### CustomOAuth2UserService

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

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

        log.debug("OAuth2 로그인 시도 - provider: {}", registrationId);

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
            .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        User user = processOAuth2User(registrationId, userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes(), userNameAttributeName);
    }

    /**
     * OAuth2 사용자 정보를 처리하여 User 엔티티를 반환합니다.
     */
    private User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());
        String providerId = userInfo.getId();
        String email = userInfo.getEmail();

        // 1. 기존 소셜 계정으로 가입된 사용자 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderId(provider, providerId);

        if (existingSocialAccount.isPresent()) {
            log.debug("기존 소셜 계정으로 로그인 - provider: {}, email: {}", provider, email);
            return existingSocialAccount.get().getUser();
        }

        // 2. 동일 이메일로 가입된 사용자가 있는지 확인
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // 기존 사용자에게 소셜 계정 연동
            User user = existingUser.get();
            log.debug("기존 사용자에 소셜 계정 연동 - provider: {}", provider);
            linkSocialAccount(user, provider, providerId);
            return user;
        }

        // 3. 신규 사용자 생성
        log.debug("신규 소셜 사용자 생성 - provider: {}, email: {}", provider, email);
        return createNewUser(provider, providerId, userInfo);
    }

    private void linkSocialAccount(User user, SocialProvider provider, String providerId) {
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);
        userRepository.save(user);
    }

    private User createNewUser(SocialProvider provider, String providerId,
                               OAuth2UserInfo userInfo) {
        // User 생성 (password는 null - 소셜 로그인 사용자)
        User user = new User(userInfo.getEmail(), null, Role.USER);

        // UserProfile 생성
        String nickname = userInfo.getName() != null
            ? userInfo.getName()
            : "User_" + providerId.substring(0, 8);
        UserProfile profile = new UserProfile(user, nickname, userInfo.getImageUrl());
        user.setProfile(profile);

        // SocialAccount 생성
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);

        return userRepository.save(user);
    }
}
```

## Entity 구조

### SocialAccount

```java
@Entity
@Table(name = "social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider provider;  // GOOGLE, NAVER, KAKAO, GITHUB

    @Column(nullable = false)
    private String providerId;  // 소셜 서비스에서의 고유 ID

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### SocialProvider Enum

```java
public enum SocialProvider {
    GOOGLE,
    NAVER,
    KAKAO,
    GITHUB
}
```

## Success Handler

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
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        log.info("OAuth2 로그인 성공 - email: {}, uuid: {}", user.getEmail(), user.getUuid());

        // JWT 토큰 발급
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        // Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        // URL Fragment로 토큰 전달
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

## Frontend 통합

### 소셜 로그인 버튼

```vue
<template>
  <div class="social-login">
    <button @click="loginWith('google')" class="btn-google">
      <GoogleIcon /> Google로 로그인
    </button>
    <button @click="loginWith('naver')" class="btn-naver">
      <NaverIcon /> 네이버로 로그인
    </button>
    <button @click="loginWith('kakao')" class="btn-kakao">
      <KakaoIcon /> 카카오로 로그인
    </button>
  </div>
</template>

<script setup lang="ts">
const AUTH_BASE_URL = import.meta.env.VITE_AUTH_BASE_URL;

function loginWith(provider: string) {
  window.location.href = `${AUTH_BASE_URL}/oauth2/authorization/${provider}`;
}
</script>
```

### Callback 처리

```typescript
// /oauth2/callback 페이지
export function OAuth2CallbackPage() {
  useEffect(() => {
    const hash = window.location.hash.substring(1);
    const params = new URLSearchParams(hash);

    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken && refreshToken) {
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);
      window.location.href = '/';
    } else {
      // 에러 처리
      const error = params.get('error');
      console.error('OAuth2 login failed:', error);
      window.location.href = '/login?error=' + error;
    }
  }, []);

  return <div>로그인 처리 중...</div>;
}
```

## 소셜 사용자 특별 처리

### 비밀번호 변경 불가

```java
public void changePassword(Long userId, ChangePasswordRequest request) {
    User user = findUserById(userId);

    // 소셜 로그인 사용자 체크
    if (user.isSocialUser()) {
        throw new CustomBusinessException(
            AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD
        );
    }

    // ... 비밀번호 변경 로직
}
```

### 소셜 사용자 판별

```java
// User.java
public boolean isSocialUser() {
    return this.password == null && !this.socialAccounts.isEmpty();
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/CustomOAuth2UserService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/OAuth2AuthenticationSuccessHandler.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/OAuth2UserInfo.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/GoogleOAuth2UserInfo.java`

## 참고 자료

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google OAuth 2.0](https://developers.google.com/identity/protocols/oauth2)
- [Naver Login API](https://developers.naver.com/docs/login/api/api.md)
- [Kakao Login API](https://developers.kakao.com/docs/latest/ko/kakaologin/common)
