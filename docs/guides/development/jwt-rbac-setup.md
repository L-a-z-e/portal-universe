---
id: jwt-rbac-setup
title: JWT RBAC 설정 가이드
type: guide
status: current
created: 2026-01-19
updated: 2026-01-19
author: Laze
tags:
  - jwt
  - rbac
  - security
  - spring-security
  - authentication
  - authorization
related:
  - ADR-004-jwt-rbac-auto-configuration
  - ADR-003-authorization-strategy
---

# JWT RBAC 설정 가이드

## 사전 요구사항

- Spring Boot 3.x
- Spring Security 6.x
- Common Library 의존성 추가
- Auth Service가 발급하는 JWT 토큰

## 빠른 시작

### Servlet 환경 (Spring MVC)

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.portal.universe</groupId>
    <artifactId>common-library</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

### Reactive 환경 (Spring WebFlux)

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain apiSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(authorize -> authorize
                .pathMatchers("/api/public/**").permitAll()
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
```

## Common Library 자동 설정 동작 원리

### JwtSecurityAutoConfiguration

Common Library는 **Spring Boot Auto-Configuration** 메커니즘을 사용하여 JWT 권한 변환기를 자동으로 등록합니다.

```java
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationConverter.class)
public class JwtSecurityAutoConfiguration {

    // Servlet 환경
    @Bean
    @ConditionalOnWebApplication(type = SERVLET)
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return JwtAuthenticationConverterAdapter.createDefault();
    }

    // Reactive 환경
    @Bean
    @ConditionalOnWebApplication(type = REACTIVE)
    @ConditionalOnMissingBean(name = "reactiveJwtAuthenticationConverter")
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter();
    }
}
```

### 자동 감지 조건

| 조건 | 설명 |
|------|------|
| `@ConditionalOnClass` | 클래스패스에 `JwtAuthenticationConverter`가 있을 때만 동작 |
| `@ConditionalOnWebApplication` | 애플리케이션 타입(Servlet/Reactive)을 자동으로 감지 |
| `@ConditionalOnMissingBean` | 사용자가 커스텀 Bean을 정의하지 않았을 때만 자동 등록 |

### 동작 방식

1. **애플리케이션 시작 시**: Spring Boot가 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`를 읽어 Auto-Configuration 클래스를 로드
2. **환경 감지**: Servlet 또는 Reactive 환경인지 자동 판단
3. **Bean 등록**: 해당 환경에 맞는 JWT 권한 변환기 Bean을 자동 등록
4. **커스터마이징 우선**: 마이크로서비스에서 동일한 타입의 Bean을 정의하면 자동 설정은 비활성화

## JWT 토큰 구조

### 표준 JWT 형식

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MSIsImlhdCI6MTcwNTAwMDAwMCwiZXhwIjoxNzA1MDAzNjAwfQ.signature
```

### 구성 요소

| 부분 | 이름 | 내용 |
|------|------|------|
| 1 | Header | 알고리즘, 타입 |
| 2 | Payload | 사용자 정보, 권한, 메타데이터 |
| 3 | Signature | 서명 (RSA256) |

### Payload 예시

```json
{
  "sub": "user123",
  "roles": [
    "ROLE_USER",
    "ROLE_ADMIN"
  ],
  "iss": "http://localhost:8081",
  "iat": 1705000000,
  "exp": 1705003600
}
```

### roles 클레임

- **위치**: Payload 내부
- **형식**: 문자열 배열 `["ROLE_USER", "ROLE_ADMIN"]`
- **접두사**: Auth Service가 이미 `ROLE_` 접두사를 포함하여 발급
- **Spring Security 변환**: Common Library가 자동으로 `GrantedAuthority`로 변환

### 토큰 디코딩 방법

1. [jwt.io](https://jwt.io/) 접속
2. 토큰 전체 문자열을 붙여넣기
3. Decoded 섹션에서 Payload 확인

## Servlet 환경 (Spring MVC) 사용법

### 1. 의존성 추가

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.portal.universe</groupId>
    <artifactId>common-library</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 2. application.yml 설정

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

### 3. SecurityConfig 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * API 엔드포인트에 대한 보안 필터 체인을 설정합니다.
     */
    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 공개 API
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                // 인증된 사용자
                .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")

                // 관리자 전용
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
```

### 4. Controller에서 @PreAuthorize 사용

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    // 누구나 접근 가능
    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getProducts()));
    }

    // ADMIN 권한 필요
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.createProduct(request)));
    }

    // USER 또는 ADMIN 권한 필요
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Review>> createReview(
            @PathVariable String id,
            @RequestBody ReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.createReview(id, request)));
    }
}
```

### 5. 메서드 레벨 보안 활성화

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    // @PreAuthorize 사용을 위해 필요
}
```

## Reactive 환경 (Spring WebFlux) 사용법

### 1. API Gateway에서 사용

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * API Gateway의 통합 보안 설정
     */
    @Bean
    public SecurityWebFilterChain apiSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(authorize -> authorize
                // 공개 경로
                .pathMatchers("/auth-service/**").permitAll()
                .pathMatchers("/api/shopping/products/**").permitAll()

                // 관리자 전용
                .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

                // 그 외 인증 필요
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    /**
     * JWT 디코더 설정
     * 내부망에서는 jwk-set-uri로 키를 가져오고,
     * issuer-uri로 토큰의 발급자를 검증
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder =
            NimbusReactiveJwtDecoder.withJwkSetUri(this.jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
            JwtValidators.createDefaultWithIssuer(this.issuerUri);
        jwtDecoder.setJwtValidator(issuerValidator);

        return jwtDecoder;
    }
}
```

### 2. Reactive Controller에서 인증 정보 접근

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public Mono<ResponseEntity<UserInfo>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        List<String> roles = jwt.getClaimAsStringList("roles");

        UserInfo userInfo = new UserInfo(userId, roles);
        return Mono.just(ResponseEntity.ok(userInfo));
    }
}
```

## 커스터마이징 방법

### 1. 다른 클레임 이름 사용

기본값은 `roles` 클레임을 사용하지만, 다른 이름을 사용하려면 커스텀 Bean을 정의합니다.

#### Servlet 환경

```java
@Configuration
public class CustomSecurityConfig {

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        // "authorities" 클레임 사용, 접두사 없음
        return JwtAuthenticationConverterAdapter.create("authorities", "");
    }
}
```

#### Reactive 환경

```java
@Configuration
public class CustomSecurityConfig {

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> reactiveJwtAuthenticationConverter() {
        // "permissions" 클레임 사용, 접두사 없음
        return new ReactiveJwtAuthenticationConverterAdapter("permissions", "");
    }
}
```

### 2. 권한 접두사 변경

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    // JWT에 ["USER", "ADMIN"]이 들어있고, "ROLE_" 접두사를 추가하려는 경우
    return JwtAuthenticationConverterAdapter.create("roles", "ROLE_");
}
```

> **주의**: Portal Universe 프로젝트에서는 Auth Service가 이미 `ROLE_` 접두사를 포함하여 발급하므로 접두사를 추가하지 않습니다.

### 3. 완전히 커스텀한 변환 로직

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();

    // 여러 클레임에서 권한을 추출하는 복잡한 로직
    grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
    grantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

    // Principal 이름을 "sub" 대신 "username" 클레임에서 가져오기
    converter.setPrincipalClaimName("username");

    return converter;
}
```

## 권한 체크 방법 비교

### 1. SecurityConfig에서 URL 기반 체크 (권장)

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

**장점**: 중앙화된 보안 정책, 명확한 URL 패턴
**단점**: 복잡한 조건 표현 어려움

### 2. @PreAuthorize 어노테이션 (세밀한 제어)

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@PreAuthorize("hasRole('ADMIN') and #userId == authentication.principal.subject")
```

**장점**: 메서드 레벨 세밀한 제어, SpEL 표현식 사용 가능
**단점**: 분산된 보안 정책

### 3. 코드에서 직접 체크

```java
@GetMapping("/orders/{id}")
public ResponseEntity<Order> getOrder(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    List<String> roles = jwt.getClaimAsStringList("roles");
    String userId = jwt.getSubject();

    // 본인 주문이거나 ADMIN이어야 조회 가능
    Order order = orderService.getOrder(id);
    if (!order.getUserId().equals(userId) && !roles.contains("ROLE_ADMIN")) {
        throw new CustomBusinessException(CommonErrorCode.FORBIDDEN);
    }

    return ResponseEntity.ok(order);
}
```

**장점**: 복잡한 비즈니스 로직 기반 권한 체크 가능
**단점**: 비즈니스 로직과 보안 로직 혼재

## 트러블슈팅

### 1. 403 Forbidden 오류

```
Access Denied: User does not have required role
```

**원인**: JWT에 필요한 권한이 없거나, roles 클레임이 없음

**해결 방법**:
1. JWT 토큰을 [jwt.io](https://jwt.io/)에서 디코딩하여 `roles` 클레임 확인
2. Auth Service에서 토큰 발급 시 roles가 포함되는지 확인
3. SecurityConfig의 권한 설정 확인

```bash
# 로그에서 JWT 내용 확인
# application.yml에 추가
logging:
  level:
    org.springframework.security: DEBUG
```

### 2. JWT 변환기가 동작하지 않음

**증상**: roles 클레임이 있는데도 권한 체크 실패

**원인**: 커스텀 Bean이 자동 설정을 덮어씀

**해결 방법**:
1. 커스텀 `JwtAuthenticationConverter` Bean이 정의되어 있는지 확인
2. `JwtAuthenticationConverterAdapter.createDefault()` 사용 확인
3. `@ConditionalOnMissingBean` 조건 이해

### 3. issuer 검증 실패

```
An error occurred while attempting to decode the Jwt:
Jwt issuer did not match
```

**원인**: JWT의 `iss` 클레임과 `issuer-uri` 설정이 불일치

**해결 방법**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # 외부 주소
          issuer-uri: http://localhost:8081
          # 내부 주소 (Docker Network)
          jwk-set-uri: http://auth-service:8081/.well-known/jwks.json
```

### 4. Reactive 환경에서 Bean 중복

```
The bean 'reactiveJwtAuthenticationConverter', defined in class path resource,
could not be registered
```

**원인**: Bean 이름 충돌

**해결 방법**:
```java
@Bean
public Converter<Jwt, Mono<AbstractAuthenticationToken>> customJwtConverter() {
    return new ReactiveJwtAuthenticationConverterAdapter();
}
```

### 5. Method Security가 동작하지 않음

**증상**: `@PreAuthorize`를 추가했는데 권한 체크가 안 됨

**해결 방법**:
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
}
```

## 참고 문서

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [JWT.io - JWT Decoder](https://jwt.io/)
- [Common Library 소스 코드](/services/common-library/src/main/java/com/portal/universe/commonlibrary/security/)
- [Shopping Service SecurityConfig 예시](/services/shopping-service/src/main/java/com/portal/universe/shoppingservice/common/config/SecurityConfig.java)
- [API Gateway SecurityConfig 예시](/services/api-gateway/src/main/java/com/portal/universe/apigateway/config/SecurityConfig.java)
- [에러 처리 가이드](/.claude/rules/error-handling.md)
