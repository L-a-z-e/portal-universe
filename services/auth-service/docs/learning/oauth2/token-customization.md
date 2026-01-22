# Token Customization (추가 클레임)

## 개요

JWT Token에 기본 정보 외에 애플리케이션 특화 정보(Custom Claims)를 추가하여 API 요청 시 추가적인 데이터베이스 조회 없이 필요한 정보를 얻을 수 있습니다. Portal Universe auth-service는 TokenService에서 직접 claims를 구성합니다.

## 현재 Token Claims 구조

### Access Token Claims

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "roles": "ROLE_USER",
  "email": "user@example.com",
  "nickname": "홍길동",
  "username": "hong_gildong",
  "iat": 1704067200,
  "exp": 1704068100
}
```

### Claims 설명

| Claim | 타입 | 필수 | 설명 | 용도 |
|-------|------|------|------|------|
| `sub` | String | O | User UUID | 사용자 식별 |
| `roles` | String | O | Spring Security 권한 | 권한 검사 |
| `email` | String | O | 이메일 주소 | 로깅, 알림 |
| `nickname` | String | X | 닉네임 | UI 표시 |
| `username` | String | X | 유저네임 | 프로필 URL |
| `iat` | Number | O | 발급 시간 | 토큰 정보 |
| `exp` | Number | O | 만료 시간 | 토큰 유효성 |

## TokenService에서 Claims 추가

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    /**
     * Access Token을 생성합니다.
     * Custom Claims를 직접 설정합니다.
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();

        // 1. 필수 Claims - 권한 정보
        claims.put("roles", user.getRole().getKey());  // "ROLE_USER" or "ROLE_ADMIN"

        // 2. 사용자 식별 정보
        claims.put("email", user.getEmail());

        // 3. Profile 정보 (존재하는 경우)
        if (user.getProfile() != null) {
            claims.put("nickname", user.getProfile().getNickname());

            // username은 설정된 경우에만 포함
            if (user.getProfile().getUsername() != null) {
                claims.put("username", user.getProfile().getUsername());
            }
        }

        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .claims(claims)                    // Custom claims 추가
                .subject(user.getUuid())           // sub claim
                .issuedAt(now)                     // iat claim
                .expiration(expiration)            // exp claim
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
}
```

## Claims 추가 패턴

### 패턴 1: 정적 정보 추가

자주 변경되지 않는 정보를 토큰에 포함:

```java
// 사용자 유형 추가
claims.put("userType", user.getUserType().name());  // "INDIVIDUAL", "BUSINESS"

// 가입 채널 추가
claims.put("signupChannel", user.getSignupChannel());  // "WEB", "MOBILE", "SOCIAL"

// 국가/언어 설정
claims.put("locale", user.getProfile().getLocale());  // "ko-KR", "en-US"
```

### 패턴 2: 권한 상세 정보

```java
// 다중 역할 지원
List<String> roles = user.getRoles().stream()
    .map(Role::getKey)
    .toList();
claims.put("roles", roles);  // ["ROLE_USER", "ROLE_PREMIUM"]

// 세부 권한 (Permission)
Set<String> permissions = user.getPermissions().stream()
    .map(Permission::getName)
    .collect(Collectors.toSet());
claims.put("permissions", permissions);  // ["post:write", "comment:delete"]
```

### 패턴 3: 구독/멤버십 정보

```java
// 구독 상태 (캐싱 효과)
Subscription sub = user.getSubscription();
if (sub != null) {
    claims.put("subscriptionType", sub.getType().name());  // "FREE", "PREMIUM"
    claims.put("subscriptionExpiry", sub.getExpiryDate().toString());
}
```

### 패턴 4: 조건부 Claims

```java
// Admin 전용 정보
if (user.getRole() == Role.ADMIN) {
    claims.put("adminLevel", user.getAdminLevel());  // 1, 2, 3
    claims.put("managedServices", user.getManagedServices());
}

// 소셜 로그인 사용자 정보
if (user.isSocialUser()) {
    claims.put("socialProvider", user.getPrimarySocialProvider());  // "GOOGLE"
}
```

## Claims 읽기

### JwtAuthenticationFilter에서

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain filterChain) {

    String token = extractToken(request);

    if (token != null && !tokenBlacklistService.isBlacklisted(token)) {
        try {
            Claims claims = tokenService.validateAccessToken(token);

            // Standard Claims
            String userId = claims.getSubject();

            // Custom Claims
            String roles = claims.get("roles", String.class);
            String email = claims.get("email", String.class);
            String nickname = claims.get("nickname", String.class);
            String username = claims.get("username", String.class);

            // 다른 서비스로 전달할 수 있는 형태로 구성
            Map<String, String> userContext = Map.of(
                "userId", userId,
                "email", email,
                "nickname", nickname != null ? nickname : ""
            );

            // ThreadLocal 또는 Request Attribute로 전달
            request.setAttribute("userContext", userContext);

            // Authentication 생성
            List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(roles));

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Principal에 추가 정보 설정
            authentication.setDetails(userContext);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.debug("JWT authentication failed: {}", e.getMessage());
        }
    }

    filterChain.doFilter(request, response);
}
```

### Controller에서 Custom Claims 사용

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getMyInfo(
            Authentication authentication) {

        // Principal에서 userId 추출
        String userId = (String) authentication.getPrincipal();

        // Details에서 추가 정보 추출
        @SuppressWarnings("unchecked")
        Map<String, String> userContext =
            (Map<String, String>) authentication.getDetails();

        String email = userContext.get("email");
        String nickname = userContext.get("nickname");

        // DB 조회 없이 기본 정보 반환 가능
        return ResponseEntity.ok(ApiResponse.success(
            new UserInfo(userId, email, nickname)
        ));
    }
}
```

## Frontend에서 Claims 활용

```typescript
import jwtDecode from 'jwt-decode';

interface TokenPayload {
  sub: string;
  roles: string;
  email: string;
  nickname: string;
  username?: string;
  exp: number;
  iat: number;
}

class AuthService {
  private tokenPayload: TokenPayload | null = null;

  setAccessToken(token: string): void {
    this.tokenPayload = jwtDecode<TokenPayload>(token);
    localStorage.setItem('access_token', token);
  }

  // Custom Claims 접근 메서드
  getUserId(): string | null {
    return this.tokenPayload?.sub ?? null;
  }

  getEmail(): string | null {
    return this.tokenPayload?.email ?? null;
  }

  getNickname(): string | null {
    return this.tokenPayload?.nickname ?? null;
  }

  getUsername(): string | null {
    return this.tokenPayload?.username ?? null;
  }

  getRoles(): string | null {
    return this.tokenPayload?.roles ?? null;
  }

  isAdmin(): boolean {
    return this.tokenPayload?.roles === 'ROLE_ADMIN';
  }

  // 토큰 만료 확인
  isTokenExpired(): boolean {
    if (!this.tokenPayload) return true;
    return this.tokenPayload.exp * 1000 < Date.now();
  }
}

export const authService = new AuthService();
```

### React Component에서 사용

```tsx
import { authService } from '@/services/authService';

export function UserProfile() {
  const nickname = authService.getNickname();
  const username = authService.getUsername();
  const isAdmin = authService.isAdmin();

  return (
    <div className="user-profile">
      <span className="nickname">{nickname}</span>
      {username && <span className="username">@{username}</span>}
      {isAdmin && <Badge>Admin</Badge>}
    </div>
  );
}
```

## 추가 Claims 고려사항

### 1. 토큰 크기 제한

```
┌─────────────────────────────────────────────────────────────┐
│                    Token Size Limits                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HTTP Header 최대 크기: ~8KB (서버마다 다름)                  │
│  권장 JWT 크기: < 1KB                                        │
│                                                              │
│  큰 데이터가 필요하면:                                        │
│  - API 별도 호출                                             │
│  - Redis 캐싱                                                │
│  - 토큰에는 ID만 포함                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 2. 변경 빈도 고려

| 데이터 | 변경 빈도 | 토큰 포함 |
|--------|----------|----------|
| UUID | 불변 | O |
| 권한 | 드물게 | O |
| 닉네임 | 가끔 | O (재로그인 필요) |
| 포인트 잔액 | 자주 | X (API 조회) |
| 알림 개수 | 매우 자주 | X (실시간 조회) |

### 3. 민감 정보 제외

```java
// 절대 포함하면 안 되는 정보
claims.put("password", user.getPassword());      // X - 비밀번호
claims.put("ssn", user.getSsn());                // X - 주민번호
claims.put("creditCard", user.getCreditCard());  // X - 카드 정보
claims.put("phoneNumber", user.getPhone());      // X - 전화번호

// 주의해서 포함할 정보
claims.put("email", user.getEmail());            // 로깅에 노출될 수 있음
```

### 4. Claims 버전 관리

```java
// 토큰 버전을 포함하여 구조 변경 시 대응
claims.put("v", 2);  // Claims 버전

// 검증 시 버전 확인
int version = claims.get("v", Integer.class);
if (version < CURRENT_VERSION) {
    // 재로그인 요청 또는 마이그레이션
}
```

## Claims 확장 예시

### 다중 테넌트 지원

```java
claims.put("tenantId", user.getTenantId());
claims.put("tenantRole", user.getTenantRole());
```

### Feature Flag 포함

```java
// 사용자별 기능 플래그
Map<String, Boolean> features = featureFlagService.getUserFeatures(user);
claims.put("features", features);
// { "newDashboard": true, "betaFeatures": false }
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/TokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/security/JwtAuthenticationFilter.java`

## 참고 자료

- [JWT Claims Best Practices](https://datatracker.ietf.org/doc/html/rfc7519#section-4)
- [IANA JWT Claims Registry](https://www.iana.org/assignments/jwt/jwt.xhtml)
