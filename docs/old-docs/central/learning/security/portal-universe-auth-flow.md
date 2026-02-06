# Portal Universe Auth Flow

Portal Universe의 전체 인증 흐름을 분석합니다. 실제 코드 기반으로 일반 로그인, 소셜 로그인, 토큰 갱신, 로그아웃 플로우를 상세히 설명합니다.

## 1. 시스템 아키텍처

### 1.1 인증 관련 컴포넌트

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Portal Universe Auth Architecture                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Frontend Layer                                  │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │    │
│  │  │ portal-shell │  │blog-frontend │  │shopping-fe   │              │    │
│  │  │  (Vue 3)     │  │  (Vue 3)     │  │  (React 18)  │              │    │
│  │  │              │  │              │  │              │              │    │
│  │  │ authStore    │◀─┤ Module Fed   │◀─┤ Module Fed   │              │    │
│  │  │ apiClient    │──┤ Integration  │──┤ Integration  │              │    │
│  │  └──────┬───────┘  └──────────────┘  └──────────────┘              │    │
│  └─────────│───────────────────────────────────────────────────────────┘    │
│            │                                                                 │
│            │  HTTP (JWT Bearer)                                             │
│            ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                   API Gateway (:8080)                                │    │
│  │  ┌──────────────────────────────────────────────────────────────┐   │    │
│  │  │  JwtAuthenticationFilter (HMAC-SHA256)                        │   │    │
│  │  │  - JWT 검증                                                   │   │    │
│  │  │  - X-User-Id, X-User-Roles 헤더 추가                          │   │    │
│  │  └──────────────────────────────────────────────────────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│            │                                                                 │
│            │  Routing                                                        │
│            ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                      Backend Services                                │    │
│  │                                                                     │    │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │    │
│  │  │ auth-service │  │ blog-service │  │shopping-serv │              │    │
│  │  │   (:8081)    │  │   (:8082)    │  │   (:8083)    │              │    │
│  │  │              │  │              │  │              │              │    │
│  │  │ - 로그인     │  │ - X-User-Id  │  │ - X-User-Id  │              │    │
│  │  │ - 회원가입   │  │   헤더 사용   │  │   헤더 사용   │              │    │
│  │  │ - 토큰 발급  │  │              │  │              │              │    │
│  │  │ - 토큰 갱신  │  │              │  │              │              │    │
│  │  └──────┬───────┘  └──────────────┘  └──────────────┘              │    │
│  │         │                                                           │    │
│  └─────────│───────────────────────────────────────────────────────────┘    │
│            │                                                                 │
│            ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                        Data Layer                                    │    │
│  │  ┌──────────────┐  ┌──────────────┐                                 │    │
│  │  │    MySQL     │  │    Redis     │                                 │    │
│  │  │              │  │              │                                 │    │
│  │  │ - users      │  │ - refresh_   │                                 │    │
│  │  │ - social_    │  │   token:{id} │                                 │    │
│  │  │   accounts   │  │ - blacklist: │                                 │    │
│  │  │              │  │   {token}    │                                 │    │
│  │  └──────────────┘  └──────────────┘                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 클래스 구조

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Auth Service Class Diagram                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────┐      ┌────────────────────┐                         │
│  │   AuthController   │      │   UserController   │                         │
│  ├────────────────────┤      ├────────────────────┤                         │
│  │ POST /api/auth/    │      │ POST /api/users/   │                         │
│  │      login         │      │      signup        │                         │
│  │ POST /api/auth/    │      │ GET /api/users/me  │                         │
│  │      refresh       │      └─────────┬──────────┘                         │
│  │ POST /api/auth/    │                │                                    │
│  │      logout        │                │                                    │
│  └─────────┬──────────┘                │                                    │
│            │                           │                                    │
│            ▼                           ▼                                    │
│  ┌────────────────────────────────────────────────────────┐                │
│  │                    TokenService                         │                │
│  ├────────────────────────────────────────────────────────┤                │
│  │ + generateAccessToken(User): String                     │                │
│  │ + generateRefreshToken(User): String                    │                │
│  │ + validateAccessToken(String): Claims                   │                │
│  │ + getUserIdFromToken(String): String                    │                │
│  │ + getRemainingExpiration(String): long                  │                │
│  └────────────────────────────────────────────────────────┘                │
│            │                                                                │
│            │                                                                │
│  ┌─────────▼──────────┐      ┌────────────────────┐                        │
│  │ RefreshTokenService│      │TokenBlacklistService│                       │
│  ├────────────────────┤      ├────────────────────┤                        │
│  │ + save(userId,     │      │ + addToBlacklist(  │                        │
│  │     token)         │      │     token, ttl)    │                        │
│  │ + get(userId)      │      │ + isBlacklisted(   │                        │
│  │ + validate(userId, │      │     token)         │                        │
│  │     token)         │      └─────────┬──────────┘                        │
│  │ + delete(userId)   │                │                                    │
│  └─────────┬──────────┘                │                                    │
│            │                           │                                    │
│            └───────────┬───────────────┘                                    │
│                        ▼                                                    │
│              ┌─────────────────┐                                            │
│              │     Redis       │                                            │
│              │ refresh_token:* │                                            │
│              │ blacklist:*     │                                            │
│              └─────────────────┘                                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 일반 로그인 플로우

### 2.1 시퀀스 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Email/Password Login Flow                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Browser      Portal Shell      Gateway      Auth Service      Redis   MySQL │
│    │              │               │              │              │        │   │
│    │              │               │              │              │        │   │
│ 1. │──Login Form──▶│              │              │              │        │   │
│    │  email/pw     │              │              │              │        │   │
│    │              │               │              │              │        │   │
│ 2. │              │──POST /api/auth/login────────▶│              │        │   │
│    │              │   {email, password}          │              │        │   │
│    │              │               │              │              │        │   │
│ 3. │              │               │              │──Find User───│────────▶│  │
│    │              │               │              │  by email    │        │   │
│    │              │               │              │              │    User│   │
│    │              │               │              │◀─────────────│────────│   │
│    │              │               │              │              │        │   │
│ 4. │              │               │              │──Verify Password      │   │
│    │              │               │              │  BCrypt.matches()     │   │
│    │              │               │              │              │        │   │
│ 5. │              │               │              │──Generate Access Token│   │
│    │              │               │              │  JWT (HS256, 15min)   │   │
│    │              │               │              │              │        │   │
│ 6. │              │               │              │──Generate Refresh Token   │
│    │              │               │              │  JWT (HS256, 7days)   │   │
│    │              │               │              │              │        │   │
│ 7. │              │               │              │──Save Refresh─▶│       │   │
│    │              │               │              │  Token (7d TTL)│       │   │
│    │              │               │              │              │        │   │
│ 8. │              │◀──────────────│◀─────────────│              │        │   │
│    │              │  {accessToken, refreshToken, expiresIn: 900}│        │   │
│    │              │               │              │              │        │   │
│ 9. │              │──Store in Memory (Pinia)    │              │        │   │
│    │              │               │              │              │        │   │
│10. │◀──Redirect───│              │              │              │        │   │
│    │  to Home     │              │              │              │        │   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 실제 코드 분석

**AuthController.java**
```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request) {

    log.info("Login attempt for email: {}", request.email());

    // 1. 사용자 조회 (프로필 포함 - JWT에 nickname 포함 위해)
    User user = userRepository.findByEmailWithProfile(request.email())
            .orElseThrow(() -> new CustomBusinessException(
                AuthErrorCode.INVALID_CREDENTIALS));

    // 2. 비밀번호 검증 (BCrypt)
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    // 3. Access Token 발급 (15분)
    String accessToken = tokenService.generateAccessToken(user);

    // 4. Refresh Token 발급 및 Redis 저장 (7일)
    String refreshToken = tokenService.generateRefreshToken(user);
    refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

    log.info("Login successful for user: {}", user.getUuid());

    LoginResponse response = new LoginResponse(accessToken, refreshToken, 900);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**TokenService.java - Access Token 생성**
```java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("roles", user.getRole().getKey());      // ROLE_USER
    claims.put("email", user.getEmail());

    // Profile 정보 추가
    if (user.getProfile() != null) {
        claims.put("nickname", user.getProfile().getNickname());
        if (user.getProfile().getUsername() != null) {
            claims.put("username", user.getProfile().getUsername());
        }
    }

    Date now = new Date();
    Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

    return Jwts.builder()
            .claims(claims)
            .subject(user.getUuid())        // sub: 사용자 UUID
            .issuedAt(now)                   // iat: 발급 시간
            .expiration(expiration)          // exp: 만료 시간
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
}
```

---

## 3. 소셜 로그인 플로우

### 3.1 시퀀스 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     OAuth2 Social Login Flow                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Browser      Portal Shell   Auth Service   Social Provider   Redis   MySQL  │
│    │              │              │                │            │        │    │
│    │              │              │                │            │        │    │
│ 1. │──Click───────▶│              │                │            │        │    │
│    │  "Login with  │              │                │            │        │    │
│    │   Google"     │              │                │            │        │    │
│    │              │               │                │            │        │    │
│ 2. │◀──Redirect to Auth Server───│                │            │        │    │
│    │  /oauth2/authorization/google                │            │        │    │
│    │              │               │                │            │        │    │
│ 3. │──────────────│───────────────▶│               │            │        │    │
│    │              │               │                │            │        │    │
│ 4. │              │               │──Redirect to──▶│            │        │    │
│    │◀─────────────│───────────────│◀──────────────│            │        │    │
│    │  Google Login Page           │                │            │        │    │
│    │              │               │                │            │        │    │
│ 5. │──User Login──│───────────────│───────────────▶│            │        │    │
│    │  (Google credentials)        │                │            │        │    │
│    │              │               │                │            │        │    │
│ 6. │◀─────────────│───────────────│◀───Code────────│            │        │    │
│    │  Redirect with auth code     │                │            │        │    │
│    │              │               │                │            │        │    │
│ 7. │──────────────│───────────────▶│               │            │        │    │
│    │              │               │──Exchange Code─▶│            │        │    │
│    │              │               │◀─Access Token──│            │        │    │
│    │              │               │                │            │        │    │
│ 8. │              │               │──Get UserInfo──▶│            │        │    │
│    │              │               │◀─{email, name}─│            │        │    │
│    │              │               │                │            │        │    │
│ 9. │              │               │──CustomOAuth2UserService────│────────▶│   │
│    │              │               │  (Create or Link User)      │     User│   │
│    │              │               │◀────────────────│────────────│────────│   │
│    │              │               │                │            │        │    │
│10. │              │               │──Generate JWT──│            │        │    │
│    │              │               │──Save Refresh──│────────────▶│        │   │
│    │              │               │                │            │        │    │
│11. │◀─────────────│◀──────────────│                │            │        │    │
│    │  Redirect to /oauth2/callback                 │            │        │    │
│    │  #access_token=...&refresh_token=...          │            │        │    │
│    │              │               │                │            │        │    │
│12. │──────────────▶│              │                │            │        │    │
│    │              │──Extract tokens from fragment  │            │        │    │
│    │              │──Store in Pinia (memory)       │            │        │    │
│    │              │               │                │            │        │    │
│13. │◀──Home Page──│              │                │            │        │    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 핵심 코드

**CustomOAuth2UserService.java**
```java
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

    // Provider별 사용자 정보 파싱
    OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
            registrationId, oAuth2User.getAttributes());

    // 기존 사용자 확인 또는 신규 생성
    User user = processOAuth2User(registrationId, userInfo);

    return new CustomOAuth2User(user, oAuth2User.getAttributes(), userNameAttributeName);
}

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

    // 2. 동일 이메일로 가입된 사용자 확인 → 계정 연동
    Optional<User> existingUser = userRepository.findByEmail(email);

    if (existingUser.isPresent()) {
        User user = existingUser.get();
        linkSocialAccount(user, provider, providerId);
        return user;
    }

    // 3. 신규 사용자 생성
    return createNewUser(provider, providerId, userInfo);
}
```

**OAuth2AuthenticationSuccessHandler.java**
```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication)
        throws IOException, ServletException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
    User user = oAuth2User.getUser();

    // JWT 토큰 발급
    String accessToken = tokenService.generateAccessToken(user);
    String refreshToken = tokenService.generateRefreshToken(user);

    // Refresh Token Redis 저장
    refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

    // URL Fragment로 토큰 전달 (보안상 서버 로그에 노출 안됨)
    String targetUrl = UriComponentsBuilder
            .fromUriString(frontendBaseUrl + "/oauth2/callback")
            .fragment("access_token=" + accessToken +
                     "&refresh_token=" + refreshToken +
                     "&expires_in=900")
            .build().toUriString();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
}
```

---

## 4. 토큰 갱신 플로우

### 4.1 시퀀스 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Token Refresh Flow                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Portal Shell            Gateway           Auth Service         Redis        │
│      │                     │                    │                 │          │
│      │                     │                    │                 │          │
│   1. │──API Request (Expired Token)──▶│        │                 │          │
│      │                     │                    │                 │          │
│   2. │◀──401 Unauthorized──│                    │                 │          │
│      │   X-Auth-Error: Token expired            │                 │          │
│      │                     │                    │                 │          │
│   3. │──POST /api/auth/refresh────────────────▶│                 │          │
│      │   {refreshToken}    │                    │                 │          │
│      │                     │                    │                 │          │
│   4. │                     │                    │──Validate JWT   │          │
│      │                     │                    │  Signature      │          │
│      │                     │                    │                 │          │
│   5. │                     │                    │──Check Redis────▶│         │
│      │                     │                    │  GET refresh_token:{uuid}  │
│      │                     │                    │◀──stored token──│          │
│      │                     │                    │                 │          │
│   6. │                     │                    │──Compare Tokens │          │
│      │                     │                    │  request == stored?        │
│      │                     │                    │                 │          │
│   7. │                     │                    │──Generate New   │          │
│      │                     │                    │  Access Token   │          │
│      │                     │                    │                 │          │
│   8. │◀───────────────────────────────────────│                 │          │
│      │   {accessToken, expiresIn: 900}         │                 │          │
│      │                     │                    │                 │          │
│   9. │──Store New Token────│                    │                 │          │
│      │  (Pinia memory)     │                    │                 │          │
│      │                     │                    │                 │          │
│  10. │──Retry Original Request (New Token)────▶│                 │          │
│      │                     │                    │                 │          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 코드 분석

**AuthController.java - Refresh**
```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @Valid @RequestBody RefreshRequest request) {

    try {
        // 1. Refresh Token JWT 서명 검증
        Claims claims = tokenService.validateAccessToken(request.refreshToken());
        String userId = claims.getSubject();

        // 2. Redis에 저장된 Refresh Token과 비교
        if (!refreshTokenService.validateRefreshToken(userId, request.refreshToken())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. 사용자 정보 조회 (JWT claims 갱신 위해)
        User user = userRepository.findByUuidWithProfile(userId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 4. 새로운 Access Token 발급
        String accessToken = tokenService.generateAccessToken(user);

        RefreshResponse response = new RefreshResponse(accessToken, 900);
        return ResponseEntity.ok(ApiResponse.success(response));

    } catch (Exception e) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
}
```

**Frontend Axios Interceptor**
```typescript
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const authStore = useAuthStore();

        // 401 에러이고, 아직 재시도하지 않은 경우
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            const errorHeader = error.response.headers['x-auth-error'];

            if (errorHeader === 'Token expired') {
                try {
                    // Refresh 요청
                    const response = await api.post('/api/auth/refresh', {
                        refreshToken: authStore.refreshToken
                    });

                    const newToken = response.data.data.accessToken;
                    authStore.setAccessToken(newToken);

                    // 새 토큰으로 원래 요청 재시도
                    originalRequest.headers.Authorization = `Bearer ${newToken}`;
                    return api(originalRequest);

                } catch (refreshError) {
                    // Refresh 실패 - 로그아웃
                    authStore.logout();
                    window.location.href = '/login';
                }
            }
        }

        return Promise.reject(error);
    }
);
```

---

## 5. 로그아웃 플로우

### 5.1 시퀀스 다이어그램

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Logout Flow                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Portal Shell                Auth Service                   Redis            │
│      │                           │                            │              │
│      │                           │                            │              │
│   1. │──POST /api/auth/logout───▶│                            │              │
│      │   Authorization: Bearer {accessToken}                  │              │
│      │   Body: {refreshToken}    │                            │              │
│      │                           │                            │              │
│   2. │                           │──Validate Access Token     │              │
│      │                           │  Extract userId            │              │
│      │                           │                            │              │
│   3. │                           │──Calculate Remaining TTL   │              │
│      │                           │  = exp - now               │              │
│      │                           │                            │              │
│   4. │                           │──SET blacklist:{token}─────▶│             │
│      │                           │   "blacklisted"            │              │
│      │                           │   EX {remainingTTL}        │              │
│      │                           │                            │              │
│   5. │                           │──DEL refresh_token:{uuid}──▶│             │
│      │                           │                            │              │
│   6. │◀────────────────────────│                            │              │
│      │   {message: "로그아웃 성공"}                           │              │
│      │                           │                            │              │
│   7. │──Clear tokens from Pinia  │                            │              │
│      │──Redirect to /login       │                            │              │
│      │                           │                            │              │
│                                                                              │
│  ═══════════════════════════════════════════════════════════════════════════│
│                                                                              │
│  이후 동일 Access Token으로 API 요청 시:                                     │
│                                                                              │
│  Portal Shell                Gateway                        Redis            │
│      │                           │                            │              │
│      │──API Request─────────────▶│                            │              │
│      │   Authorization: Bearer {oldToken}                     │              │
│      │                           │                            │              │
│      │                           │──EXISTS blacklist:{token}──▶│             │
│      │                           │◀────────── 1 ──────────────│              │
│      │                           │                            │              │
│      │◀──401 Unauthorized────────│                            │              │
│      │   (Token blacklisted)     │                            │              │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 코드 분석

**AuthController.java - Logout**
```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse<Map<String, String>>> logout(
        @RequestHeader("Authorization") String authorization,
        @Valid @RequestBody LogoutRequest request) {

    // 1. Authorization 헤더에서 Access Token 추출
    String accessToken = TokenUtils.extractBearerToken(authorization);

    try {
        // 2. Access Token 검증 및 userId 추출
        Claims claims = tokenService.validateAccessToken(accessToken);
        String userId = claims.getSubject();

        // 3. Access Token 블랙리스트 추가 (남은 TTL 만큼)
        long remainingExpiration = tokenService.getRemainingExpiration(accessToken);
        tokenBlacklistService.addToBlacklist(accessToken, remainingExpiration);

        // 4. Refresh Token Redis에서 삭제
        refreshTokenService.deleteRefreshToken(userId);

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("message", "로그아웃 성공")));

    } catch (Exception e) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_TOKEN);
    }
}
```

**TokenBlacklistService.java**
```java
public void addToBlacklist(String token, long remainingExpiration) {
    if (remainingExpiration <= 0) {
        return;  // 이미 만료된 토큰은 블랙리스트 불필요
    }

    String key = BLACKLIST_PREFIX + token;  // "blacklist:{token}"
    redisTemplate.opsForValue().set(
            key,
            "blacklisted",
            remainingExpiration,
            TimeUnit.MILLISECONDS
    );
}

public boolean isBlacklisted(String token) {
    String key = BLACKLIST_PREFIX + token;
    Boolean exists = redisTemplate.hasKey(key);
    return Boolean.TRUE.equals(exists);
}
```

---

## 6. API 요청 인증 플로우

### 6.1 전체 흐름

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                    API Request Authentication Flow                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Frontend              Gateway                Auth/Blog/Shop      Redis      │
│     │                     │                       │                │         │
│     │                     │                       │                │         │
│  1. │──GET /api/blog/posts/1──────▶│              │                │         │
│     │   Authorization: Bearer {jwt}│              │                │         │
│     │                     │                       │                │         │
│  2. │                     │──JwtAuthenticationFilter              │         │
│     │                     │  Extract Token        │                │         │
│     │                     │                       │                │         │
│  3. │                     │──Verify Signature     │                │         │
│     │                     │  HMAC-SHA256          │                │         │
│     │                     │                       │                │         │
│  4. │                     │──Check Expiration     │                │         │
│     │                     │  exp > now?           │                │         │
│     │                     │                       │                │         │
│  5. │                     │──(Optional) Check Blacklist──────────▶│         │
│     │                     │  EXISTS blacklist:{token}             │         │
│     │                     │◀──────────── 0 ──────│                │         │
│     │                     │                       │                │         │
│  6. │                     │──Extract Claims       │                │         │
│     │                     │  sub: userId          │                │         │
│     │                     │  roles: ROLE_USER     │                │         │
│     │                     │                       │                │         │
│  7. │                     │──Add Headers          │                │         │
│     │                     │  X-User-Id: {uuid}    │                │         │
│     │                     │  X-User-Roles: ROLE_USER              │         │
│     │                     │                       │                │         │
│  8. │                     │──Forward Request──────▶│               │         │
│     │                     │                       │                │         │
│  9. │                     │                       │──Business Logic│         │
│     │                     │                       │  Use X-User-Id │         │
│     │                     │                       │                │         │
│ 10. │◀────────────────────│◀──Response───────────│                │         │
│     │                     │                       │                │         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Gateway JwtAuthenticationFilter

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getPath().value();

    // 1. 공개 경로는 JWT 검증 생략
    if (isPublicPath(path)) {
        return chain.filter(exchange);
    }

    // 2. Authorization 헤더에서 토큰 추출
    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
        return chain.filter(exchange);  // 토큰 없음 - 권한 검사에서 처리
    }

    String token = authHeader.substring(BEARER_PREFIX.length());

    try {
        // 3. JWT 검증 (서명, 만료)
        Claims claims = validateToken(token);
        String userId = claims.getSubject();
        String roles = claims.get("roles", String.class);

        // 4. SecurityContext 설정
        List<SimpleGrantedAuthority> authorities = roles != null
                ? List.of(new SimpleGrantedAuthority(roles))
                : Collections.emptyList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // 5. 하위 서비스로 전달할 헤더 추가
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles != null ? roles : "")
                .build();

        // 6. 다음 필터로 진행
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

    } catch (ExpiredJwtException e) {
        return handleUnauthorized(exchange, "Token expired");
    } catch (JwtException e) {
        return handleUnauthorized(exchange, "Invalid token");
    }
}
```

---

## 7. Redis 데이터 구조

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Redis Data Structure                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    Refresh Token Storage                             │    │
│  ├─────────────────────────────────────────────────────────────────────┤    │
│  │                                                                     │    │
│  │  Key:     refresh_token:{user_uuid}                                 │    │
│  │  Value:   {jwt_refresh_token}                                       │    │
│  │  TTL:     7 days (604800000 ms)                                     │    │
│  │                                                                     │    │
│  │  Example:                                                           │    │
│  │  > GET refresh_token:550e8400-e29b-41d4-a716-446655440000          │    │
│  │  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."                         │    │
│  │                                                                     │    │
│  │  > TTL refresh_token:550e8400-e29b-41d4-a716-446655440000          │    │
│  │  (integer) 604720                                                   │    │
│  │                                                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    Token Blacklist                                   │    │
│  ├─────────────────────────────────────────────────────────────────────┤    │
│  │                                                                     │    │
│  │  Key:     blacklist:{access_token}                                  │    │
│  │  Value:   "blacklisted"                                             │    │
│  │  TTL:     Remaining expiration of the token                         │    │
│  │                                                                     │    │
│  │  Example:                                                           │    │
│  │  > GET blacklist:eyJhbGciOiJIUzI1NiIs...                           │    │
│  │  "blacklisted"                                                      │    │
│  │                                                                     │    │
│  │  > TTL blacklist:eyJhbGciOiJIUzI1NiIs...                           │    │
│  │  (integer) 720  # 토큰 만료까지 남은 시간                            │    │
│  │                                                                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 설정 파일

### 8.1 JWT Configuration

```yaml
# application.yml (auth-service)
jwt:
  secret-key: ${JWT_SECRET_KEY:your-256-bit-secret-key-for-jwt-signing-minimum-32-characters-required}
  access-token-expiration: 900000      # 15분 (ms)
  refresh-token-expiration: 604800000  # 7일 (ms)
```

```java
// JwtConfig.java
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
```

### 8.2 OAuth2 Configuration

```yaml
# application-local.yml (OAuth2 설정이 있는 경우)
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email, profile, openid
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            # ...
```

---

## 9. 요약

| 플로우 | 핵심 단계 | 관련 서비스 |
|--------|----------|-----------|
| **일반 로그인** | email/pw 검증 → JWT 발급 → Redis 저장 | auth-service |
| **소셜 로그인** | OAuth2 인증 → 사용자 생성/연동 → JWT 발급 | auth-service, Social Provider |
| **토큰 갱신** | Refresh Token 검증 → 새 Access Token 발급 | auth-service, Redis |
| **로그아웃** | Access Token 블랙리스트 → Refresh Token 삭제 | auth-service, Redis |
| **API 인증** | JWT 검증 → 헤더 추가 → 하위 서비스 전달 | api-gateway |

---

## 10. 관련 문서

- [OAuth2 Fundamentals](./oauth2-fundamentals.md)
- [JWT Deep Dive](./jwt-deep-dive.md)
- [Spring Security Architecture](./spring-security-architecture.md)
- [RBAC Implementation](./rbac-implementation.md)
- [API Gateway Security](./api-gateway-security.md)
- [PKCE for SPA](./pkce-spa-security.md)
