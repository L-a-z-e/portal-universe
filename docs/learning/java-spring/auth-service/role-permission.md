# Role & Permission (RBAC 구현)

## 개요

Portal Universe auth-service는 Role-Based Access Control (RBAC) 모델을 사용하여 사용자 권한을 관리합니다. 현재는 간단한 Role 기반 구조를 사용하며, 필요시 세부 Permission 시스템으로 확장할 수 있습니다.

## 현재 RBAC 구조

### Role Enum

```java
package com.portal.universe.authservice.domain;

@Getter
@RequiredArgsConstructor
public enum Role {
    /**
     * 일반 사용자 역할
     */
    USER("ROLE_USER"),

    /**
     * 관리자 역할
     */
    ADMIN("ROLE_ADMIN");

    /**
     * Spring Security에서 사용하는 권한 키 (e.g., 'ROLE_USER')
     */
    private final String key;
}
```

### Role 권한 매핑

```
┌─────────────────────────────────────────────────────────────┐
│                      Role Hierarchy                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ROLE_ADMIN                                                  │
│  ├── 모든 사용자 관리                                         │
│  ├── 시스템 설정 관리                                         │
│  ├── 통계 조회                                               │
│  └── includes ROLE_USER                                      │
│                                                              │
│  ROLE_USER                                                   │
│  ├── 자신의 프로필 조회/수정                                  │
│  ├── 게시글 작성/수정/삭제                                    │
│  ├── 댓글 작성                                               │
│  └── 팔로우/팔로워 관리                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Spring Security 통합

### SecurityConfig에서 권한 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 인증 없이 접근 가능
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/**", "/ping").permitAll()

                // ADMIN 역할 필요
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 인증된 사용자만 접근 가능
                .requestMatchers("/api/profile/**").authenticated()

                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // ...
        ;
        return http.build();
    }
}
```

### JWT에서 권한 추출

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) {

        String token = extractToken(request);

        if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
            try {
                Claims claims = tokenService.validateAccessToken(token);
                String userId = claims.getSubject();

                // JWT claims에서 roles 추출
                String roles = claims.get("roles", String.class);  // "ROLE_USER"

                // Spring Security Authority 생성
                List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority(roles));

                // Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                    );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 인증 실패
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

### TokenService에서 Role 포함

```java
@Service
public class TokenService {

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // Role을 Spring Security 형식으로 저장
        claims.put("roles", user.getRole().getKey());  // "ROLE_USER" or "ROLE_ADMIN"
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUuid())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() +
                           jwtConfig.getAccessTokenExpiration()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
}
```

## 권한 검사

### Controller 레벨

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    /**
     * SecurityConfig에서 /api/admin/** 은 ADMIN만 접근 가능하도록 설정됨
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        // ADMIN만 접근 가능
        return ResponseEntity.ok(ApiResponse.success(userService.findAll()));
    }
}
```

### 메서드 레벨 (선택적)

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
    // @PreAuthorize, @PostAuthorize 활성화
}

@Service
public class UserService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        // ADMIN만 실행 가능
    }

    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public UserDto getUser(Long userId) {
        // ADMIN이거나 본인만 조회 가능
    }
}
```

## 확장된 RBAC 구조 (선택적)

### Permission 기반 세분화

```java
// Permission Enum
public enum Permission {
    // 사용자 관리
    USER_READ("user:read"),
    USER_WRITE("user:write"),
    USER_DELETE("user:delete"),

    // 게시글 관리
    POST_READ("post:read"),
    POST_WRITE("post:write"),
    POST_DELETE("post:delete"),

    // 관리자 권한
    ADMIN_ACCESS("admin:access");

    private final String permission;
}

// Role과 Permission 매핑
public enum Role {
    USER(Set.of(
        Permission.USER_READ,
        Permission.POST_READ,
        Permission.POST_WRITE
    )),
    ADMIN(Set.of(Permission.values()));

    private final Set<Permission> permissions;

    public Set<SimpleGrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = permissions.stream()
            .map(p -> new SimpleGrantedAuthority(p.getPermission()))
            .collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }
}
```

### JWT에 다중 권한 포함

```java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();

    // 다중 권한 지원
    List<String> authorities = user.getRole().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    claims.put("authorities", authorities);
    // ["ROLE_USER", "user:read", "post:read", "post:write"]

    return Jwts.builder()
            .claims(claims)
            .subject(user.getUuid())
            // ...
            .compact();
}
```

### 세부 권한 검사

```java
@PreAuthorize("hasAuthority('post:delete')")
public void deletePost(Long postId) {
    // post:delete 권한이 있는 사용자만 실행 가능
}

@PreAuthorize("hasAuthority('admin:access')")
public void accessAdminPanel() {
    // admin:access 권한 필요
}
```

## Role 계층 구조

### RoleHierarchy 설정 (선택적)

```java
@Configuration
public class RoleHierarchyConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    @Bean
    public DefaultWebSecurityExpressionHandler webSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultWebSecurityExpressionHandler handler =
            new DefaultWebSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
```

이 설정으로 ADMIN은 자동으로 USER 권한도 가지게 됩니다.

## 권한 관련 에러 처리

### 접근 거부 예외

```java
@ControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("FORBIDDEN", "접근 권한이 없습니다"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("UNAUTHORIZED", "인증이 필요합니다"));
    }
}
```

## Frontend 권한 처리

### 권한 기반 UI 렌더링

```typescript
// authService.ts
class AuthService {
  private tokenPayload: TokenPayload | null = null;

  getRoles(): string | null {
    return this.tokenPayload?.roles ?? null;
  }

  isAdmin(): boolean {
    return this.tokenPayload?.roles === 'ROLE_ADMIN';
  }

  hasRole(role: string): boolean {
    return this.tokenPayload?.roles === role;
  }
}

// 사용 예시 (Vue)
<template>
  <div>
    <AdminPanel v-if="isAdmin" />
    <UserDashboard v-else />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { authService } from '@/services/authService';

const isAdmin = computed(() => authService.isAdmin());
</script>
```

### 라우터 가드

```typescript
// router/index.ts
const routes = [
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      { path: 'users', component: UserManagement },
      { path: 'settings', component: SystemSettings },
    ]
  }
];

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAdmin && !authService.isAdmin()) {
    next('/unauthorized');
    return;
  }
  next();
});
```

## Best Practices

### 1. 최소 권한 원칙

```java
// BAD: 모든 인증된 사용자에게 열림
.anyRequest().authenticated()

// GOOD: 필요한 권한만 부여
.requestMatchers("/api/users/**").hasRole("USER")
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

### 2. 서버 측 검증 필수

```java
// 클라이언트 전송 role 무시
// 항상 DB에서 조회한 role 사용

User user = userRepository.findById(userId);
if (user.getRole() != Role.ADMIN) {
    throw new AccessDeniedException("Admin only");
}
```

### 3. 권한 변경 시 토큰 재발급

```java
public void changeUserRole(Long userId, Role newRole) {
    User user = findUser(userId);
    user.setRole(newRole);
    userRepository.save(user);

    // 기존 토큰 무효화 (선택적)
    refreshTokenService.deleteRefreshToken(user.getUuid());

    // 사용자에게 재로그인 요청
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/Role.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/SecurityConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/security/JwtAuthenticationFilter.java`

## 참고 자료

- [Spring Security Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [RBAC Model](https://en.wikipedia.org/wiki/Role-based_access_control)
