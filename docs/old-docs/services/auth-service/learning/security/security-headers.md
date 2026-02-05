# Security Headers (CORS, CSP)

## 개요

Portal Universe auth-service는 마이크로서비스 아키텍처에서 브라우저 보안을 위한 HTTP 보안 헤더를 적절히 설정합니다. CORS(Cross-Origin Resource Sharing), CSP(Content Security Policy), 그리고 기타 보안 헤더들을 통해 XSS, Clickjacking 등의 공격을 방어합니다.

## CORS (Cross-Origin Resource Sharing)

### CORS가 필요한 이유

```
┌─────────────────────────────────────────────────────────────┐
│                 Same-Origin Policy                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  브라우저: http://localhost:30000 (Frontend)                 │
│      ↓                                                       │
│  요청: http://localhost:10001/api/users (Backend)           │
│      ↓                                                       │
│  🚫 브라우저가 차단! (Origin이 다름)                          │
│                                                              │
│  해결책: 서버에서 CORS 헤더 설정                              │
│  Access-Control-Allow-Origin: http://localhost:30000        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Portal Universe 환경

```
Frontend (Browser)                    Backend Services
┌──────────────────┐                ┌──────────────────┐
│ portal-shell     │                │ auth-service     │
│ :30000           │ ────CORS────▶  │ :10001           │
│                  │                └──────────────────┘
│ shopping-frontend│                ┌──────────────────┐
│ (Module Fed)     │ ────CORS────▶  │ blog-service     │
│                  │                │ :10002           │
└──────────────────┘                └──────────────────┘
```

### SecurityConfig CORS 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ... 기타 설정

            // CORS 설정 비활성화 (Gateway에서 처리 또는 별도 Bean 사용)
            .cors(AbstractHttpConfigurer::disable);

            // 또는 명시적 설정
            // .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }
}
```

### CorsConfig (별도 설정 클래스)

```java
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:30000}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));

        // 노출할 헤더 (프론트엔드에서 접근 가능)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count"
        ));

        // 쿠키/인증 정보 포함 허용
        configuration.setAllowCredentials(true);

        // Preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth2/**", configuration);

        return source;
    }
}
```

### 환경별 CORS 설정

```yaml
# application.yml
app:
  cors:
    allowed-origins: >
      http://localhost:30000,
      http://localhost:30001,
      http://localhost:30002

# application-production.yml
app:
  cors:
    allowed-origins: >
      https://portal-universe.com,
      https://www.portal-universe.com
```

## Security Headers

### SecurityConfig 헤더 설정

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            // X-Frame-Options: iframe 삽입 방지
            .frameOptions(frame -> frame
                .sameOrigin()  // 동일 도메인에서만 iframe 허용
                // .deny()     // 완전히 비활성화
            )

            // X-Content-Type-Options: MIME 스니핑 방지
            .contentTypeOptions(content -> {})  // nosniff 헤더 추가

            // X-XSS-Protection (레거시, CSP 권장)
            .xssProtection(xss -> xss
                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

            // HTTP Strict Transport Security
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))  // 1년

            // Content-Security-Policy
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; " +
                                 "script-src 'self'; " +
                                 "style-src 'self' 'unsafe-inline'; " +
                                 "img-src 'self' data: https:; " +
                                 "font-src 'self'; " +
                                 "connect-src 'self' " + getAllowedApiOrigins()))

            // Referrer-Policy
            .referrerPolicy(referrer -> referrer
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

            // Permissions-Policy
            .permissionsPolicy(permissions -> permissions
                .policy("geolocation=(), microphone=(), camera=()"))
        );

    return http.build();
}
```

### 주요 보안 헤더

| 헤더 | 값 | 목적 |
|------|-----|------|
| `X-Frame-Options` | `SAMEORIGIN` | Clickjacking 방지 |
| `X-Content-Type-Options` | `nosniff` | MIME 스니핑 방지 |
| `X-XSS-Protection` | `1; mode=block` | XSS 필터 (레거시) |
| `Strict-Transport-Security` | `max-age=31536000` | HTTPS 강제 |
| `Content-Security-Policy` | `default-src 'self'` | 리소스 출처 제한 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Referrer 정보 제한 |

## Content Security Policy (CSP)

### CSP 지시자

```
Content-Security-Policy:
  default-src 'self';           # 기본: 같은 출처만
  script-src 'self';            # JS: 같은 출처만 (inline 금지)
  style-src 'self' 'unsafe-inline';  # CSS: inline 허용 (주의)
  img-src 'self' data: https:;  # 이미지: data URI, HTTPS 허용
  font-src 'self';              # 폰트: 같은 출처만
  connect-src 'self' https://api.portal-universe.com;  # AJAX
  frame-ancestors 'none';       # iframe 삽입 금지
  form-action 'self';           # 폼 제출 대상
```

### API 서비스용 CSP (간소화)

```java
// auth-service는 API만 제공하므로 간소화된 CSP
.contentSecurityPolicy(csp -> csp
    .policyDirectives("default-src 'none'; frame-ancestors 'none'"))
```

## ForwardedHeaderFilter

API Gateway 뒤에서 동작할 때 원본 요청 정보 보존:

```java
@Bean
public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
    FilterRegistrationBean<ForwardedHeaderFilter> bean = new FilterRegistrationBean<>();
    bean.setFilter(new ForwardedHeaderFilter());
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
}
```

### 처리되는 헤더

| 헤더 | 설명 |
|------|------|
| `X-Forwarded-For` | 클라이언트 원본 IP |
| `X-Forwarded-Proto` | 원본 프로토콜 (http/https) |
| `X-Forwarded-Host` | 원본 호스트 |
| `X-Forwarded-Port` | 원본 포트 |

## OAuth2 Redirect URI 보안

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend.base-url:http://localhost:30000}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(...) throws IOException {
        // 허용된 redirect URI만 사용
        String targetUrl = UriComponentsBuilder
            .fromUriString(frontendBaseUrl + "/oauth2/callback")
            .fragment("access_token=" + accessToken + "&...")
            .build().toUriString();

        // URL Fragment 사용 (Query String보다 안전)
        // - 서버 로그에 남지 않음
        // - Referrer로 전송되지 않음

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
```

## API Gateway에서의 CORS 처리

마이크로서비스 아키텍처에서는 API Gateway에서 CORS를 중앙 처리하는 것이 권장됩니다:

```yaml
# Kong Gateway 예시
plugins:
  - name: cors
    config:
      origins:
        - http://localhost:30000
        - https://portal-universe.com
      methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
      headers:
        - Authorization
        - Content-Type
      credentials: true
      max_age: 3600
```

이 경우 개별 서비스에서는 CORS 비활성화:

```java
.cors(AbstractHttpConfigurer::disable)
```

## 응답 헤더 확인

```bash
# curl로 CORS 헤더 확인
curl -I -X OPTIONS \
  -H "Origin: http://localhost:30000" \
  -H "Access-Control-Request-Method: POST" \
  http://localhost:10001/api/auth/login

# 예상 응답
# Access-Control-Allow-Origin: http://localhost:30000
# Access-Control-Allow-Methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
# Access-Control-Allow-Headers: Authorization,Content-Type
# Access-Control-Allow-Credentials: true
# Access-Control-Max-Age: 3600
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/SecurityConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/RequestLoggingFilter.java`

## 참고 자료

- [MDN CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Content Security Policy](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [OWASP Secure Headers](https://owasp.org/www-project-secure-headers/)
- [Spring Security Headers](https://docs.spring.io/spring-security/reference/servlet/exploits/headers.html)
