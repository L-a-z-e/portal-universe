# Refresh Token Rotation (보안 강화)

## 개요

Refresh Token Rotation은 토큰 갱신 시마다 새로운 Refresh Token을 발급하여 탈취된 토큰의 재사용을 방지하는 보안 메커니즘입니다. Portal Universe auth-service는 Redis를 활용하여 이 패턴을 구현합니다.

## 기본 개념

### Refresh Token 탈취 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│               Without Rotation (위험)                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  공격자가 Refresh Token 탈취                                  │
│                                                              │
│  정상 사용자 ──▶ /refresh → 새 Access Token                  │
│  공격자 ──────▶ /refresh → 새 Access Token (성공!)          │
│  공격자 ──────▶ /refresh → 새 Access Token (계속 성공!)     │
│                                                              │
│  → 7일간 계속 사용 가능                                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│               With Rotation (안전)                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  공격자가 Refresh Token 탈취                                  │
│                                                              │
│  공격자 ──────▶ /refresh → 새 Access Token + 새 Refresh     │
│  정상 사용자 ──▶ /refresh → 실패! (기존 토큰 무효화됨)       │
│                                                              │
│  → 사용자에게 재로그인 요청 (탈취 감지!)                      │
│  → 공격자의 새 토큰도 무효화 가능                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 현재 구현 (기본 방식)

Portal Universe auth-service는 현재 단일 Refresh Token 방식을 사용하며, Redis에 사용자당 하나의 토큰만 저장합니다.

### RefreshTokenService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token을 Redis에 저장합니다.
     * 기존 토큰이 있으면 덮어씁니다 (Rotation 효과)
     */
    public void saveRefreshToken(String userId, String token) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                token,
                jwtConfig.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );
        log.info("Refresh token saved for user: {}", userId);
    }

    /**
     * 저장된 Refresh Token과 요청된 토큰을 비교하여 검증합니다.
     */
    public boolean validateRefreshToken(String userId, String token) {
        String storedToken = getRefreshToken(userId);
        if (storedToken == null) {
            log.warn("Refresh token not found for user: {}", userId);
            return false;
        }

        boolean isValid = storedToken.equals(token);
        if (!isValid) {
            log.warn("Refresh token mismatch for user: {}", userId);
        }
        return isValid;
    }
}
```

## Refresh Token Rotation 구현

### 방법 1: 갱신 시 새 Refresh Token 발급

```java
@PostMapping("/refresh")
public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
        @Valid @RequestBody RefreshRequest request) {

    // 1. 기존 Refresh Token 검증
    Claims claims = tokenService.validateAccessToken(request.refreshToken());
    String userId = claims.getSubject();

    if (!refreshTokenService.validateRefreshToken(userId, request.refreshToken())) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 2. 사용자 조회
    User user = userRepository.findByUuidWithProfile(userId)
            .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

    // 3. 새 Access Token 발급
    String newAccessToken = tokenService.generateAccessToken(user);

    // 4. 새 Refresh Token 발급 (ROTATION)
    String newRefreshToken = tokenService.generateRefreshToken(user);
    refreshTokenService.saveRefreshToken(userId, newRefreshToken);  // 기존 토큰 무효화

    // 5. 양쪽 토큰 모두 반환
    RotatedRefreshResponse response = new RotatedRefreshResponse(
        newAccessToken,
        newRefreshToken,  // 새 Refresh Token
        900
    );
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### 방법 2: Token Family 패턴 (고급)

```java
@Service
public class TokenFamilyService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TOKEN_FAMILY_PREFIX = "token_family:";
    private static final String USED_TOKEN_PREFIX = "used_token:";

    /**
     * Refresh Token과 Family ID를 함께 저장
     */
    public void saveTokenWithFamily(String userId, String familyId, String token) {
        // 1. 현재 유효한 토큰 저장
        String key = TOKEN_FAMILY_PREFIX + userId;
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("familyId", familyId);
        tokenData.put("token", token);
        redisTemplate.opsForHash().putAll(key, tokenData);

        // 2. 사용된 토큰으로 기록 (재사용 감지용)
        String usedKey = USED_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(usedKey, familyId, Duration.ofDays(7));
    }

    /**
     * 토큰 검증 및 재사용 감지
     */
    public TokenValidationResult validateWithReplayDetection(
            String userId, String token) {

        // 1. 이미 사용된 토큰인지 확인
        String usedKey = USED_TOKEN_PREFIX + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(usedKey))) {
            String familyId = (String) redisTemplate.opsForValue().get(usedKey);

            // 재사용 감지! - 해당 Family의 모든 토큰 무효화
            log.error("Token replay detected! Invalidating family: {}", familyId);
            invalidateTokenFamily(userId, familyId);

            return TokenValidationResult.REPLAY_DETECTED;
        }

        // 2. 현재 유효한 토큰인지 확인
        String key = TOKEN_FAMILY_PREFIX + userId;
        String storedToken = (String) redisTemplate.opsForHash().get(key, "token");

        if (!token.equals(storedToken)) {
            return TokenValidationResult.INVALID;
        }

        return TokenValidationResult.VALID;
    }

    /**
     * Token Family 무효화 (보안 침해 감지 시)
     */
    public void invalidateTokenFamily(String userId, String familyId) {
        String key = TOKEN_FAMILY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Token family invalidated for user: {}", userId);

        // 선택적: 사용자에게 알림 발송
        // notificationService.sendSecurityAlert(userId, "세션이 무효화되었습니다.");
    }
}
```

### 방법 3: Sliding Window Rotation

```java
@Service
public class SlidingWindowTokenService {

    /**
     * 최근 N개의 Refresh Token을 유효하게 유지
     * 네트워크 지연으로 인한 동시 요청 처리
     */
    public void saveWithWindow(String userId, String newToken, int windowSize) {
        String key = "refresh_tokens:" + userId;

        // 새 토큰을 리스트 앞에 추가
        redisTemplate.opsForList().leftPush(key, newToken);

        // windowSize만큼만 유지 (예: 3개)
        redisTemplate.opsForList().trim(key, 0, windowSize - 1);
    }

    /**
     * 윈도우 내에 있는 토큰인지 확인
     */
    public boolean isTokenInWindow(String userId, String token) {
        String key = "refresh_tokens:" + userId;
        List<Object> tokens = redisTemplate.opsForList().range(key, 0, -1);

        return tokens != null && tokens.contains(token);
    }
}
```

## Frontend 대응

### Rotation 대응 코드

```typescript
interface TokenResponse {
  accessToken: string;
  refreshToken?: string;  // Rotation 시 새 Refresh Token
  expiresIn: number;
}

async function refreshTokens(): Promise<boolean> {
  const currentRefreshToken = localStorage.getItem('refresh_token');

  if (!currentRefreshToken) {
    return false;
  }

  try {
    const response = await api.post<ApiResponse<TokenResponse>>(
      '/api/auth/refresh',
      { refreshToken: currentRefreshToken }
    );

    const { accessToken, refreshToken } = response.data.data;

    // Access Token 업데이트
    localStorage.setItem('access_token', accessToken);

    // 새 Refresh Token이 있으면 업데이트 (Rotation)
    if (refreshToken) {
      localStorage.setItem('refresh_token', refreshToken);
    }

    return true;
  } catch (error) {
    // Refresh 실패 - 재로그인 필요
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    window.location.href = '/login';
    return false;
  }
}
```

### 동시 요청 처리

```typescript
// 여러 API 요청이 동시에 401을 받았을 때
// refresh가 중복 호출되지 않도록 처리

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback);
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 refresh 진행 중이면 대기
        return new Promise((resolve) => {
          subscribeTokenRefresh((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(api(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const success = await refreshTokens();
        if (success) {
          const newToken = localStorage.getItem('access_token')!;
          onTokenRefreshed(newToken);
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        }
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```

## 보안 고려사항

### 1. Rotation 감지 알림

```java
@EventListener
public void handleTokenReuseDetected(TokenReuseEvent event) {
    // 보안 이벤트 로깅
    securityAuditLog.log(
        event.getUserId(),
        "REFRESH_TOKEN_REUSE_DETECTED",
        event.getIpAddress()
    );

    // 사용자 알림
    notificationService.sendEmail(
        event.getUserEmail(),
        "보안 알림: 비정상적인 로그인 시도가 감지되었습니다."
    );
}
```

### 2. Rotation 주기 설정

| 전략 | 보안 | 사용성 | 권장 상황 |
|------|------|--------|----------|
| 매 갱신마다 | 높음 | 중간 | 금융, 의료 |
| 일정 시간마다 | 중간 | 높음 | 일반 서비스 |
| 로그인마다만 | 낮음 | 최고 | 저위험 서비스 |

### 3. Grace Period 설정

```java
// 네트워크 지연을 고려한 유예 기간
public boolean validateWithGracePeriod(String userId, String token, String previousToken) {
    String currentToken = getRefreshToken(userId);

    // 현재 토큰과 일치
    if (currentToken.equals(token)) {
        return true;
    }

    // 이전 토큰과 일치 (Grace Period 내)
    String prevKey = REFRESH_TOKEN_PREFIX + userId + ":prev";
    String storedPrevToken = (String) redisTemplate.opsForValue().get(prevKey);

    if (token.equals(storedPrevToken)) {
        // 10초 내의 요청만 허용
        Long ttl = redisTemplate.getExpire(prevKey);
        if (ttl != null && ttl > 0) {
            return true;
        }
    }

    return false;
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/RefreshTokenService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/AuthController.java`

## 참고 자료

- [OAuth 2.0 Security Best Current Practice](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)
- [Refresh Token Rotation](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)
