# CORS Configuration

API Gateway에서 CORS(Cross-Origin Resource Sharing)를 설정하는 방법을 학습합니다.

## 개요

Gateway 레벨에서 CORS를 처리하면 각 마이크로서비스의 CORS 설정 부담을 줄일 수 있습니다.

```
Browser                    API Gateway                 Backend
   │                           │                          │
   │  Preflight (OPTIONS)      │                          │
   ├──────────────────────────>│                          │
   │                           │  CORS Headers 응답       │
   │<──────────────────────────│                          │
   │                           │                          │
   │  Actual Request (GET)     │                          │
   ├──────────────────────────>│────────────────────────>│
   │                           │<────────────────────────│
   │  Response + CORS Headers  │                          │
   │<──────────────────────────│                          │
```

## Portal Universe 구현

### SecurityConfig의 CorsWebFilter

```java
package com.portal.universe.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Spring Cloud Gateway의 전역 CORS 설정을 담당합니다.
     * Gateway 단계에서 Preflight 요청(OPTIONS)을 처리하여
     * 각 마이크로서비스의 CORS 부담을 덜어줍니다.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용 Origin 설정
        configuration.setAllowedOrigins(List.of(
                "http://localhost:30000",       // Local Frontend
                "http://localhost:8080",        // Local Gateway
                "https://portal-universe:30000" // Docker/K8s Frontend
        ));

        // 허용 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));

        // 허용 헤더
        configuration.setAllowedHeaders(List.of("*"));

        // 자격 증명 허용 (쿠키, Authorization 헤더)
        configuration.setAllowCredentials(true);

        // 로컬 개발 환경에서 Origin이 'null'인 경우 허용
        configuration.addAllowedOrigin("null");

        // Preflight 요청 결과 캐시 (3600초 = 1시간)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}
```

## YAML 기반 CORS 설정

### application.yml 방식

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':                              # 모든 경로에 적용
            allowedOrigins:
              - "http://localhost:30000"
              - "https://portal-universe:30000"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600

        # Preflight OPTIONS 요청 라우팅 활성화
        add-to-simple-url-handler-mapping: true
```

### 경로별 차등 설정

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          # 공개 API: 모든 Origin 허용
          '[/api/public/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
            allowCredentials: false

          # 관리자 API: 특정 Origin만 허용
          '[/api/admin/**]':
            allowedOrigins:
              - "https://admin.portal-universe.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders:
              - Authorization
              - Content-Type
            allowCredentials: true

          # 기본 설정
          '[/**]':
            allowedOrigins:
              - "http://localhost:30000"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
```

## 환경별 CORS 설정

### Properties를 활용한 동적 설정

```java
@Configuration
@ConfigurationProperties(prefix = "app.cors")
@Data
public class CorsProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = new ArrayList<>();
    private boolean allowCredentials = true;
    private long maxAge = 3600;
}

@Configuration
@RequiredArgsConstructor
public class DynamicCorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);
    }
}
```

```yaml
# application-local.yml
app:
  cors:
    allowed-origins:
      - "http://localhost:30000"
      - "http://localhost:3000"
      - "http://localhost:5173"        # Vite dev server

# application-kubernetes.yml
app:
  cors:
    allowed-origins:
      - "https://portal-universe.com"
      - "https://www.portal-universe.com"
      - "https://admin.portal-universe.com"
```

## CORS 헤더 설명

### 요청 헤더 (Preflight)

```
OPTIONS /api/blog/posts HTTP/1.1
Origin: http://localhost:30000
Access-Control-Request-Method: POST
Access-Control-Request-Headers: Content-Type, Authorization
```

### 응답 헤더

```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:30000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
Access-Control-Expose-Headers: X-Request-Id, X-Trace-Id
```

| 헤더 | 설명 |
|------|------|
| `Access-Control-Allow-Origin` | 허용된 Origin |
| `Access-Control-Allow-Methods` | 허용된 HTTP 메서드 |
| `Access-Control-Allow-Headers` | 허용된 요청 헤더 |
| `Access-Control-Allow-Credentials` | 자격 증명(쿠키 등) 허용 여부 |
| `Access-Control-Max-Age` | Preflight 결과 캐시 시간(초) |
| `Access-Control-Expose-Headers` | JS에서 접근 가능한 응답 헤더 |

## Exposed Headers 설정

클라이언트 JavaScript에서 접근해야 하는 커스텀 헤더를 노출합니다.

```java
configuration.setExposedHeaders(List.of(
        "X-Request-Id",           // 요청 추적 ID
        "X-Trace-Id",             // 분산 추적 ID
        "X-RateLimit-Remaining",  // Rate Limit 남은 횟수
        "X-Total-Count",          // 페이지네이션 총 개수
        "Link"                    // 페이지네이션 링크
));
```

### Frontend에서 접근

```javascript
const response = await fetch('/api/blog/posts');
const totalCount = response.headers.get('X-Total-Count');
const traceId = response.headers.get('X-Trace-Id');
```

## 일반적인 CORS 에러와 해결

### 1. Origin 불일치

```
Access to fetch at 'http://localhost:8080/api/blog' from origin
'http://localhost:3000' has been blocked by CORS policy
```

**해결**: 해당 Origin을 `allowedOrigins`에 추가

### 2. Credentials와 와일드카드 충돌

```
The value of the 'Access-Control-Allow-Origin' header must not be
the wildcard '*' when the request's credentials mode is 'include'
```

**해결**: `allowCredentials: true`일 때는 구체적인 Origin 지정

```java
// 잘못된 설정
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowCredentials(true);

// 올바른 설정
configuration.setAllowedOrigins(List.of("http://localhost:30000"));
configuration.setAllowCredentials(true);
```

### 3. Preflight 요청 실패

```
Response to preflight request doesn't pass access control check
```

**해결**: OPTIONS 메서드 허용 확인

```java
configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS"  // OPTIONS 포함
));
```

### 4. 커스텀 헤더 차단

```
Request header field X-Custom-Header is not allowed
```

**해결**: 해당 헤더를 `allowedHeaders`에 추가

```java
configuration.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "X-Custom-Header"
));
// 또는
configuration.setAllowedHeaders(List.of("*"));  // 모든 헤더 허용
```

## CORS vs CSRF

| 구분 | CORS | CSRF |
|------|------|------|
| 목적 | Cross-Origin 요청 허용 | Cross-Site 요청 위조 방지 |
| 동작 | 브라우저가 검증 | 서버가 검증 |
| 보호 대상 | 서버 리소스 접근 제어 | 사용자 인증 세션 보호 |

```java
// Gateway에서 CSRF 비활성화 (API 서버이므로)
@Bean
public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
}
```

## 테스트

### cURL로 Preflight 테스트

```bash
curl -X OPTIONS http://localhost:8080/api/blog/posts \
  -H "Origin: http://localhost:30000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type, Authorization" \
  -v
```

### 예상 응답

```
< HTTP/1.1 200 OK
< Access-Control-Allow-Origin: http://localhost:30000
< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
< Access-Control-Allow-Headers: Content-Type, Authorization
< Access-Control-Allow-Credentials: true
< Access-Control-Max-Age: 3600
```

## 참고 자료

- Portal Universe: `services/api-gateway/src/main/java/.../config/SecurityConfig.java`
- [MDN CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Spring Cloud Gateway CORS](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/cors-configuration.html)
