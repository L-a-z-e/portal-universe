# RBAC Implementation

**Role-Based Access Control (RBAC)**은 사용자에게 역할을 부여하고, 역할에 따라 리소스 접근을 제어하는 권한 관리 모델입니다.

## 1. RBAC 개념

### 1.1 RBAC vs 다른 접근 제어 모델

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Access Control Models Comparison                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                            DAC                                       │    │
│  │              (Discretionary Access Control)                          │    │
│  │                                                                     │    │
│  │   Owner가 자원에 대한 접근 권한을 직접 관리                          │    │
│  │   예: 파일 시스템 권한 (rwx)                                         │    │
│  │                                                                     │    │
│  │   User A ──owns──▶ Resource X ──grants──▶ User B                    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                            MAC                                       │    │
│  │              (Mandatory Access Control)                              │    │
│  │                                                                     │    │
│  │   시스템이 보안 레벨에 따라 접근 제어                                 │    │
│  │   예: 군사 시스템 (Top Secret, Secret, Confidential)                │    │
│  │                                                                     │    │
│  │   User [Secret] ──can access──▶ Resource [Secret or below]          │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                           RBAC ★                                    │    │
│  │              (Role-Based Access Control)                             │    │
│  │                                                                     │    │
│  │   사용자에게 역할을 부여하고, 역할에 따라 권한 부여                   │    │
│  │   예: Portal Universe (USER, ADMIN)                                 │    │
│  │                                                                     │    │
│  │   User ──assigned──▶ Role ──has──▶ Permissions                      │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                            ABAC                                      │    │
│  │            (Attribute-Based Access Control)                          │    │
│  │                                                                     │    │
│  │   사용자, 리소스, 환경의 속성을 기반으로 접근 제어                    │    │
│  │   예: "부서가 개발팀이고, 근무시간이면 접근 허용"                     │    │
│  │                                                                     │    │
│  │   Policy: if (user.dept == "dev" && time.isWorkHour) then ALLOW     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 RBAC 구성 요소

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          RBAC Components                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────┐        ┌─────────┐        ┌─────────────┐                     │
│   │  User   │──M:N──▶│  Role   │──M:N──▶│ Permission  │                     │
│   └─────────┘        └─────────┘        └─────────────┘                     │
│                                                                              │
│   ┌───────────────────────────────────────────────────────────────────┐     │
│   │                         Example                                    │     │
│   ├───────────────────────────────────────────────────────────────────┤     │
│   │                                                                    │     │
│   │   Users           Roles              Permissions                   │     │
│   │   ┌────────┐     ┌──────────┐       ┌─────────────────────┐       │     │
│   │   │ user1  │────▶│   USER   │──────▶│ blog:read          │       │     │
│   │   │ user2  │────▶│          │──────▶│ blog:write         │       │     │
│   │   │ admin1 │┐    └──────────┘       │ profile:read       │       │     │
│   │   └────────┘│    ┌──────────┐       │ profile:write      │       │     │
│   │             └───▶│  ADMIN   │──────▶│ shopping:read      │       │     │
│   │                  │          │──────▶│ shopping:write     │       │     │
│   │                  └──────────┘       │ admin:read ★       │       │     │
│   │                                     │ admin:write ★      │       │     │
│   │                                     │ user:manage ★      │       │     │
│   │                                     └─────────────────────┘       │     │
│   │                                                                    │     │
│   │   ★ = ADMIN 전용 권한                                              │     │
│   │                                                                    │     │
│   └───────────────────────────────────────────────────────────────────┘     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Portal Universe RBAC 구현

### 2.1 Role Enum

```java
// Role.java
@Getter
@RequiredArgsConstructor
public enum Role {
    /**
     * 일반 사용자 역할
     * - 블로그 읽기/쓰기
     * - 프로필 관리
     * - 쇼핑 (상품 조회, 장바구니, 주문)
     */
    USER("ROLE_USER"),

    /**
     * 관리자 역할
     * - 모든 USER 권한 포함
     * - 사용자 관리
     * - 상품 관리
     * - 시스템 설정
     */
    ADMIN("ROLE_ADMIN");

    /**
     * Spring Security에서 사용하는 권한 키
     * "ROLE_" 접두사는 hasRole() 사용 시 자동 추가됨
     */
    private final String key;
}
```

### 2.2 User Entity

```java
// User.java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // USER 또는 ADMIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    // ...
}
```

### 2.3 JWT에 Role 포함

```java
// TokenService.java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();

    // Role을 JWT claims에 포함
    claims.put("roles", user.getRole().getKey());  // "ROLE_USER" 또는 "ROLE_ADMIN"
    claims.put("email", user.getEmail());

    // ...

    return Jwts.builder()
            .claims(claims)
            .subject(user.getUuid())
            // ...
            .compact();
}
```

---

## 3. Spring Security 역할 기반 접근 제어

### 3.1 URL 패턴 기반 제어

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> authorize
        // ===== 공개 API =====
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/users/signup").permitAll()

        // ===== 관리자 전용 =====
        // hasRole("ADMIN") = hasAuthority("ROLE_ADMIN")
        .requestMatchers("/api/admin/**").hasRole("ADMIN")

        // ===== 인증된 사용자 (USER 또는 ADMIN) =====
        .requestMatchers("/api/profile/**").authenticated()

        // ===== 기본값 =====
        .anyRequest().authenticated()
    );

    return http.build();
}
```

### 3.2 API Gateway 역할 제어

```java
// API Gateway SecurityConfig.java
@Bean
public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(authorize -> authorize
            // 공개 경로
            .pathMatchers("/auth-service/**", "/api/auth/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
            .pathMatchers("/api/shopping/products/**").permitAll()

            // 관리자 전용 - 상품 관리, 주문 관리
            .pathMatchers("/api/shopping/admin/**").hasRole("ADMIN")

            // 인증 필요
            .anyExchange().authenticated()
        )
        .build();
}
```

### 3.3 JwtAuthenticationFilter에서 Authority 설정

```java
// JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(...) {
    // JWT에서 역할 추출
    String roles = claims.get("roles", String.class);  // "ROLE_USER" 또는 "ROLE_ADMIN"

    // GrantedAuthority로 변환
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority(roles));

    // Authentication 객체 생성
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities  // [ROLE_USER] 또는 [ROLE_ADMIN]
        );

    SecurityContextHolder.getContext().setAuthentication(authentication);
}
```

---

## 4. 메서드 레벨 보안

### 4.1 활성화

```java
// SecurityConfig.java or MethodSecurityConfig.java
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {
}
```

### 4.2 어노테이션 사용

```java
// BlogService.java
@Service
public class BlogService {

    // ADMIN만 접근 가능
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePermanently(Long postId) {
        // 영구 삭제
    }

    // USER 또는 ADMIN이 접근 가능
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public Post createPost(PostRequest request) {
        // 게시글 생성
    }

    // 본인 게시글만 수정 가능
    @PreAuthorize("hasRole('USER') and @postService.isOwner(#postId, authentication.principal)")
    public Post updatePost(Long postId, PostRequest request) {
        // 게시글 수정
    }

    // 결과 객체의 소유자만 볼 수 있음
    @PostAuthorize("returnObject.authorId == authentication.principal or hasRole('ADMIN')")
    public PostDetailResponse getPost(Long postId) {
        // 게시글 조회
    }

    // 헬퍼 메서드
    public boolean isOwner(Long postId, String userId) {
        return postRepository.findById(postId)
            .map(post -> post.getAuthorId().equals(userId))
            .orElse(false);
    }
}
```

### 4.3 @Secured 어노테이션

```java
// ShoppingService.java
@Service
public class ShoppingService {

    @Secured("ROLE_ADMIN")
    public void createProduct(ProductRequest request) {
        // 상품 등록 - 관리자만
    }

    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public void addToCart(Long productId, int quantity) {
        // 장바구니 추가 - 로그인 사용자
    }
}
```

---

## 5. 동적 권한 검사

### 5.1 프로그래밍 방식 권한 검사

```java
// PostController.java
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        Post post = postService.findById(id);

        // 1. 본인 게시글인지 확인
        boolean isOwner = post.getAuthorId().equals(userId);

        // 2. 관리자인지 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 3. 삭제 권한 검사
        if (!isOwner && !isAdmin) {
            throw new CustomBusinessException(BlogErrorCode.FORBIDDEN);
        }

        postService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}
```

### 5.2 SpEL을 활용한 복잡한 권한 로직

```java
// OrderService.java
@Service
public class OrderService {

    // 본인 주문이거나 관리자
    @PreAuthorize("#userId == authentication.principal or hasRole('ADMIN')")
    public OrderResponse getOrder(String userId, Long orderId) {
        return orderRepository.findById(orderId)
            .map(OrderResponse::from)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));
    }

    // 주문 취소는 주문자 본인만, 단 배송 전 상태일 때만
    @PreAuthorize("#userId == authentication.principal")
    public void cancelOrder(String userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new CustomBusinessException(ShoppingErrorCode.ORDER_NOT_FOUND));

        if (!order.canCancel()) {
            throw new CustomBusinessException(ShoppingErrorCode.CANNOT_CANCEL);
        }

        order.cancel();
    }
}
```

---

## 6. 역할 계층 (Role Hierarchy)

### 6.1 역할 계층 설정

관리자가 일반 사용자 권한을 자동으로 갖도록 설정합니다.

```java
// SecurityConfig.java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    // ADMIN은 USER의 모든 권한을 포함
    hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
    return hierarchy;
}

@Bean
public SecurityExpressionHandler<FilterInvocation> securityExpressionHandler() {
    DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
    handler.setRoleHierarchy(roleHierarchy());
    return handler;
}
```

### 6.2 계층 구조 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          Role Hierarchy                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                        ┌─────────────┐                                       │
│                        │ ROLE_ADMIN  │                                       │
│                        └──────┬──────┘                                       │
│                               │                                              │
│                               │ inherits                                     │
│                               ▼                                              │
│                        ┌─────────────┐                                       │
│                        │  ROLE_USER  │                                       │
│                        └─────────────┘                                       │
│                                                                              │
│   ADMIN이 접근 가능한 리소스:                                                 │
│   - /api/admin/** (ADMIN 전용)                                               │
│   - /api/profile/** (USER 권한)                                              │
│   - /api/blog/** (USER 권한)                                                 │
│   - /api/shopping/** (USER 권한)                                             │
│                                                                              │
│   USER가 접근 가능한 리소스:                                                  │
│   - /api/profile/**                                                          │
│   - /api/blog/**                                                             │
│   - /api/shopping/** (admin 제외)                                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Portal Universe 역할별 권한 매트릭스

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Permission Matrix                                          │
├────────────────────────┬─────────────┬─────────────┬────────────────────────┤
│        Resource        │   GUEST     │    USER     │        ADMIN           │
├────────────────────────┼─────────────┼─────────────┼────────────────────────┤
│ Auth                   │             │             │                        │
│   - 회원가입           │      O      │      -      │          -             │
│   - 로그인/로그아웃    │      O      │      O      │          O             │
│   - 토큰 갱신          │      -      │      O      │          O             │
├────────────────────────┼─────────────┼─────────────┼────────────────────────┤
│ Profile                │             │             │                        │
│   - 본인 조회/수정     │      -      │      O      │          O             │
│   - 타인 프로필 조회   │      O      │      O      │          O             │
│   - 비밀번호 변경      │      -      │      O      │          O             │
│   - 계정 탈퇴          │      -      │      O      │          O             │
├────────────────────────┼─────────────┼─────────────┼────────────────────────┤
│ Blog                   │             │             │                        │
│   - 게시글 목록 조회   │      O      │      O      │          O             │
│   - 게시글 상세 조회   │      O      │      O      │          O             │
│   - 게시글 작성        │      -      │      O      │          O             │
│   - 본인 게시글 수정   │      -      │      O      │          O             │
│   - 본인 게시글 삭제   │      -      │      O      │          O             │
│   - 타인 게시글 삭제   │      -      │      -      │          O             │
│   - 댓글 작성          │      -      │      O      │          O             │
├────────────────────────┼─────────────┼─────────────┼────────────────────────┤
│ Shopping               │             │             │                        │
│   - 상품 목록 조회     │      O      │      O      │          O             │
│   - 상품 상세 조회     │      O      │      O      │          O             │
│   - 장바구니           │      -      │      O      │          O             │
│   - 주문               │      -      │      O      │          O             │
│   - 본인 주문 조회     │      -      │      O      │          O             │
│   - 상품 등록/수정     │      -      │      -      │          O             │
│   - 모든 주문 조회     │      -      │      -      │          O             │
│   - 쿠폰 관리          │      -      │      -      │          O             │
├────────────────────────┼─────────────┼─────────────┼────────────────────────┤
│ Admin                  │             │             │                        │
│   - 사용자 목록 조회   │      -      │      -      │          O             │
│   - 사용자 역할 변경   │      -      │      -      │          O             │
│   - 시스템 설정        │      -      │      -      │          O             │
└────────────────────────┴─────────────┴─────────────┴────────────────────────┘
```

---

## 8. 확장 고려사항

### 8.1 세분화된 권한 시스템

향후 더 복잡한 권한이 필요한 경우:

```java
// 권한 세분화 예시
public enum Permission {
    // Blog
    BLOG_READ("blog:read"),
    BLOG_WRITE("blog:write"),
    BLOG_DELETE("blog:delete"),
    BLOG_ADMIN("blog:admin"),

    // Shopping
    PRODUCT_READ("product:read"),
    PRODUCT_WRITE("product:write"),
    ORDER_READ("order:read"),
    ORDER_WRITE("order:write"),
    ORDER_MANAGE("order:manage"),

    // User Management
    USER_READ("user:read"),
    USER_MANAGE("user:manage");

    private final String permission;
}

// Role이 Permission 목록을 가짐
public enum Role {
    USER(Set.of(
        BLOG_READ, BLOG_WRITE,
        PRODUCT_READ, ORDER_READ, ORDER_WRITE
    )),
    MODERATOR(Set.of(
        // USER 권한 + 추가 권한
        BLOG_READ, BLOG_WRITE, BLOG_DELETE,
        PRODUCT_READ, ORDER_READ
    )),
    ADMIN(Set.of(Permission.values()));  // 모든 권한

    private final Set<Permission> permissions;
}
```

### 8.2 동적 역할 관리 (DB 기반)

```java
// 테이블 구조
// users (id, email, ...)
// roles (id, name, description)
// permissions (id, name, resource, action)
// user_roles (user_id, role_id)
// role_permissions (role_id, permission_id)

@Entity
public class Role {
    @Id @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String name;

    @ManyToMany
    private Set<Permission> permissions;
}
```

---

## 9. 다음 단계

1. [API Gateway Security](./api-gateway-security.md)
2. [Portal Universe Auth Flow](./portal-universe-auth-flow.md)
