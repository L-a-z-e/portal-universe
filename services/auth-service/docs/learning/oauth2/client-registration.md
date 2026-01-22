# Client Registration (클라이언트 관리)

## 개요

OAuth2 Client Registration은 소셜 로그인 제공자(Google, Naver, Kakao 등)와의 연동 설정을 관리합니다. Portal Universe auth-service는 Spring Security OAuth2 Client의 자동 설정을 활용하여 간편하게 소셜 로그인을 구성합니다.

## Client Registration 개념

### OAuth2 용어 정리

| 용어 | 설명 | Portal Universe |
|------|------|----------------|
| Client | 사용자 대신 리소스에 접근하는 애플리케이션 | auth-service |
| Resource Owner | 보호된 리소스의 소유자 | 최종 사용자 |
| Authorization Server | 토큰 발급 서버 | Google, Naver, Kakao |
| Resource Server | 보호된 리소스를 호스팅하는 서버 | Google UserInfo API 등 |

### Client 등록 정보

```
┌─────────────────────────────────────────────────────────────┐
│                   Client Registration                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  client-id          : 클라이언트 식별자                       │
│  client-secret      : 클라이언트 인증용 비밀키                │
│  redirect-uri       : 인증 후 리다이렉트 URL                  │
│  scope              : 요청할 권한 범위                        │
│  authorization-uri  : 인증 요청 URL                          │
│  token-uri          : 토큰 교환 URL                          │
│  user-info-uri      : 사용자 정보 조회 URL                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Spring Security OAuth2 Client 설정

### application.yml 전체 구조

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          # 각 Provider 등록
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

          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope:
              - user:email
              - read:user

        provider:
          # Provider 엔드포인트 설정 (표준이 아닌 경우)
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
```

## Provider별 상세 설정

### Google (OpenID Connect)

```yaml
registration:
  google:
    client-id: ${GOOGLE_CLIENT_ID}
    client-secret: ${GOOGLE_CLIENT_SECRET}
    scope:
      - email      # 이메일 주소
      - profile    # 이름, 프로필 사진
    # redirect-uri, provider는 자동 설정 (CommonOAuth2Provider.GOOGLE)
```

Google은 OpenID Connect를 지원하므로 별도 provider 설정이 불필요합니다.

**Google Cloud Console 설정:**
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. 프로젝트 생성 또는 선택
3. APIs & Services > Credentials
4. Create Credentials > OAuth client ID
5. Web application 선택
6. Authorized redirect URIs 추가:
   - `http://localhost:10001/login/oauth2/code/google` (개발)
   - `https://api.portal-universe.com/login/oauth2/code/google` (운영)

### Naver (커스텀 Provider)

```yaml
registration:
  naver:
    client-id: ${NAVER_CLIENT_ID}
    client-secret: ${NAVER_CLIENT_SECRET}
    redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
    authorization-grant-type: authorization_code
    scope:
      - name           # 이름
      - email          # 이메일
      - profile_image  # 프로필 이미지
    client-name: Naver

provider:
  naver:
    authorization-uri: https://nid.naver.com/oauth2.0/authorize
    token-uri: https://nid.naver.com/oauth2.0/token
    user-info-uri: https://openapi.naver.com/v1/nid/me
    user-name-attribute: response  # 응답에서 사용자 식별 속성
```

**Naver Developers 설정:**
1. [Naver Developers](https://developers.naver.com/) 접속
2. Application > 애플리케이션 등록
3. 사용 API: 네이버 로그인 선택
4. 환경: PC 웹 선택
5. 서비스 URL 및 Callback URL 설정

### Kakao (커스텀 Provider)

```yaml
registration:
  kakao:
    client-id: ${KAKAO_CLIENT_ID}
    client-secret: ${KAKAO_CLIENT_SECRET}
    redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
    authorization-grant-type: authorization_code
    scope:
      - profile_nickname   # 닉네임
      - profile_image      # 프로필 이미지
      - account_email      # 이메일 (동의 필요)
    client-name: Kakao
    client-authentication-method: client_secret_post  # POST body로 전송

provider:
  kakao:
    authorization-uri: https://kauth.kakao.com/oauth/authorize
    token-uri: https://kauth.kakao.com/oauth/token
    user-info-uri: https://kapi.kakao.com/v2/user/me
    user-name-attribute: id
```

**Kakao Developers 설정:**
1. [Kakao Developers](https://developers.kakao.com/) 접속
2. 내 애플리케이션 > 애플리케이션 추가
3. 플랫폼 > Web 플랫폼 등록
4. 카카오 로그인 > 활성화
5. Redirect URI 등록

### GitHub

```yaml
registration:
  github:
    client-id: ${GITHUB_CLIENT_ID}
    client-secret: ${GITHUB_CLIENT_SECRET}
    scope:
      - user:email   # 이메일 (private 포함)
      - read:user    # 프로필 정보
    # redirect-uri, provider는 자동 설정 (CommonOAuth2Provider.GITHUB)
```

**GitHub Settings:**
1. GitHub > Settings > Developer settings
2. OAuth Apps > New OAuth App
3. Homepage URL: `http://localhost:30000`
4. Callback URL: `http://localhost:10001/login/oauth2/code/github`

## SecurityConfig의 조건부 OAuth2 활성화

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    // OAuth2 관련 빈들은 선택적 주입 (설정이 없으면 null)
    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired(required = false)
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                // ... 다른 설정
            );

        // OAuth2 소셜 로그인: ClientRegistrationRepository가 있을 때만 활성화
        if (isOAuth2Enabled()) {
            log.info("OAuth2 소셜 로그인 활성화됨");
            http.oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        } else {
            log.warn("OAuth2 소셜 로그인 비활성화됨 - " +
                     "ClientRegistrationRepository가 설정되지 않음");
        }

        return http.build();
    }

    /**
     * OAuth2 소셜 로그인이 활성화 가능한지 확인합니다.
     */
    private boolean isOAuth2Enabled() {
        return clientRegistrationRepository != null
            && customOAuth2UserService != null
            && oAuth2AuthenticationSuccessHandler != null
            && oAuth2AuthenticationFailureHandler != null;
    }
}
```

## 환경별 설정 관리

### 환경 변수 파일

```bash
# .env.local (개발 환경)
GOOGLE_CLIENT_ID=your-dev-google-client-id
GOOGLE_CLIENT_SECRET=your-dev-google-client-secret
NAVER_CLIENT_ID=your-dev-naver-client-id
NAVER_CLIENT_SECRET=your-dev-naver-client-secret
KAKAO_CLIENT_ID=your-dev-kakao-client-id
KAKAO_CLIENT_SECRET=your-dev-kakao-client-secret

# .env.production (운영 환경)
GOOGLE_CLIENT_ID=your-prod-google-client-id
GOOGLE_CLIENT_SECRET=your-prod-google-client-secret
# ...
```

### Kubernetes Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: oauth2-secrets
  namespace: auth
type: Opaque
data:
  google-client-id: <base64-encoded>
  google-client-secret: <base64-encoded>
  naver-client-id: <base64-encoded>
  naver-client-secret: <base64-encoded>
  kakao-client-id: <base64-encoded>
  kakao-client-secret: <base64-encoded>
```

## Redirect URI 패턴

### 기본 패턴

```
{baseUrl}/login/oauth2/code/{registrationId}
```

| 변수 | 설명 | 예시 |
|------|------|------|
| `{baseUrl}` | 서버 기본 URL | `http://localhost:10001` |
| `{registrationId}` | Provider 이름 | `google`, `naver`, `kakao` |

### 환경별 Redirect URI

| 환경 | Redirect URI |
|------|-------------|
| Local | `http://localhost:10001/login/oauth2/code/google` |
| Docker | `http://auth-service:10001/login/oauth2/code/google` |
| K8s | `https://api.portal-universe.com/login/oauth2/code/google` |

## 동적 Client Registration (고급)

### 데이터베이스 기반 등록

```java
@Repository
public interface OAuth2ClientRepository extends JpaRepository<OAuth2Client, String> {
    Optional<OAuth2Client> findByRegistrationId(String registrationId);
}

@Service
@RequiredArgsConstructor
public class JpaClientRegistrationRepository
        implements ClientRegistrationRepository {

    private final OAuth2ClientRepository repository;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return repository.findByRegistrationId(registrationId)
            .map(this::toClientRegistration)
            .orElse(null);
    }

    private ClientRegistration toClientRegistration(OAuth2Client client) {
        return ClientRegistration.withRegistrationId(client.getRegistrationId())
            .clientId(client.getClientId())
            .clientSecret(client.getClientSecret())
            .redirectUri(client.getRedirectUri())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope(client.getScopes())
            .authorizationUri(client.getAuthorizationUri())
            .tokenUri(client.getTokenUri())
            .userInfoUri(client.getUserInfoUri())
            .userNameAttributeName(client.getUserNameAttributeName())
            .clientName(client.getClientName())
            .build();
    }
}
```

## 보안 고려사항

### 1. Client Secret 보호

```bash
# Bad: 코드에 하드코딩
client-secret: "my-secret-key"

# Good: 환경 변수 사용
client-secret: ${GOOGLE_CLIENT_SECRET}
```

### 2. Redirect URI 검증

```java
// Spring Security가 자동으로 검증
// 등록된 redirect-uri와 요청의 redirect_uri 비교
```

### 3. State Parameter

```java
// Spring Security가 CSRF 방지를 위해 자동 생성/검증
// 요청 시 state 생성 → 콜백 시 state 검증
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/SecurityConfig.java`
- `/services/auth-service/src/main/resources/application.yml`
- `/services/auth-service/.env.example`

## 참고 자료

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [OAuth 2.0 Client Registration](https://datatracker.ietf.org/doc/html/rfc6749#section-2)
