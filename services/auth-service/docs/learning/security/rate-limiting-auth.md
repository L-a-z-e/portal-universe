# Rate Limiting Auth (로그인 제한)

## 개요

인증 시스템에서 Rate Limiting은 무차별 대입 공격(Brute Force Attack)을 방지하는 핵심 보안 메커니즘입니다. Portal Universe auth-service는 Redis를 활용하여 로그인 시도 횟수를 제한하고, 반복적인 실패 시 계정을 일시적으로 잠금 처리합니다.

## Rate Limiting 전략

### 계층별 제한

```
┌─────────────────────────────────────────────────────────────┐
│                   Rate Limiting Layers                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Global Rate Limit (전체 API)                             │
│     └── IP당 1000 req/min                                   │
│                                                              │
│  2. Endpoint Rate Limit (특정 API)                           │
│     └── /api/auth/login: IP당 10 req/min                    │
│     └── /api/auth/signup: IP당 5 req/min                    │
│                                                              │
│  3. Account Rate Limit (계정별)                              │
│     └── 이메일당 5회 실패 → 15분 잠금                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 구현 방법

### 1. LoginAttemptService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_ATTEMPT_PREFIX = "login_attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;

    /**
     * 로그인 실패 기록
     */
    public void loginFailed(String email) {
        String key = LOGIN_ATTEMPT_PREFIX + email;

        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1) {
            // 첫 실패 시 TTL 설정
            redisTemplate.expire(key, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }

        log.warn("Login failed for email: {}, attempts: {}", email, attempts);
    }

    /**
     * 로그인 성공 시 카운터 초기화
     */
    public void loginSucceeded(String email) {
        String key = LOGIN_ATTEMPT_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Login attempt counter reset for email: {}", email);
    }

    /**
     * 계정 잠금 여부 확인
     */
    public boolean isBlocked(String email) {
        String key = LOGIN_ATTEMPT_PREFIX + email;
        Object attemptsObj = redisTemplate.opsForValue().get(key);

        if (attemptsObj == null) {
            return false;
        }

        int attempts = Integer.parseInt(attemptsObj.toString());
        return attempts >= MAX_ATTEMPTS;
    }

    /**
     * 남은 시도 횟수 조회
     */
    public int getRemainingAttempts(String email) {
        String key = LOGIN_ATTEMPT_PREFIX + email;
        Object attemptsObj = redisTemplate.opsForValue().get(key);

        if (attemptsObj == null) {
            return MAX_ATTEMPTS;
        }

        int attempts = Integer.parseInt(attemptsObj.toString());
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }

    /**
     * 잠금 해제까지 남은 시간 (초)
     */
    public long getLockRemainingSeconds(String email) {
        String key = LOGIN_ATTEMPT_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
}
```

### 2. AuthController 통합

```java
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String email = request.email();
        String clientIp = getClientIp(httpRequest);

        log.info("Login attempt for email: {} from IP: {}", email, clientIp);

        // 1. 계정 잠금 확인
        if (loginAttemptService.isBlocked(email)) {
            long remainingSeconds = loginAttemptService.getLockRemainingSeconds(email);
            log.warn("Account locked for email: {}, remaining: {}s", email, remainingSeconds);

            throw new CustomBusinessException(AuthErrorCode.ACCOUNT_LOCKED,
                "계정이 잠금되었습니다. " + (remainingSeconds / 60) + "분 후 다시 시도해주세요.");
        }

        // 2. 사용자 조회
        User user = userRepository.findByEmailWithProfile(email)
                .orElse(null);

        // 3. 인증 실패 처리
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.loginFailed(email);

            int remaining = loginAttemptService.getRemainingAttempts(email);
            log.warn("Invalid credentials for email: {}, remaining attempts: {}", email, remaining);

            if (remaining > 0) {
                throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS,
                    "이메일 또는 비밀번호가 일치하지 않습니다. (남은 시도: " + remaining + "회)");
            } else {
                throw new CustomBusinessException(AuthErrorCode.ACCOUNT_LOCKED,
                    "로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해주세요.");
            }
        }

        // 4. 로그인 성공
        loginAttemptService.loginSucceeded(email);

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);
        refreshTokenService.saveRefreshToken(user.getUuid(), refreshToken);

        log.info("Login successful for user: {}", user.getUuid());

        return ResponseEntity.ok(ApiResponse.success(
            new LoginResponse(accessToken, refreshToken, 900)));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 3. IP 기반 Rate Limiting (Bucket4j 사용)

```java
// build.gradle
implementation 'com.bucket4j:bucket4j-core:8.7.0'
implementation 'com.bucket4j:bucket4j-redis:8.7.0'
```

```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        // 로그인/회원가입 API만 제한
        if (path.equals("/api/auth/login") || path.equals("/api/users/signup")) {
            Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                    {"success":false,"error":{"code":"TOO_MANY_REQUESTS","message":"요청이 너무 많습니다. 잠시 후 다시 시도해주세요."}}
                """);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private Bucket newBucket(String key) {
        // 분당 10회 제한
        return Bucket.builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
            .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/auth/") && !path.equals("/api/users/signup");
    }
}
```

### 4. Redis 기반 분산 Rate Limiting

```java
@Service
@RequiredArgsConstructor
public class DistributedRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Sliding Window Rate Limiting 구현
     */
    public boolean isAllowed(String identifier, int maxRequests, Duration window) {
        String key = RATE_LIMIT_PREFIX + identifier;
        long now = System.currentTimeMillis();
        long windowStart = now - window.toMillis();

        // Sorted Set에서 윈도우 밖의 요청 제거
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 현재 윈도우의 요청 수 확인
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= maxRequests) {
            return false;  // 제한 초과
        }

        // 새 요청 기록
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, window.plusMinutes(1));  // 여유있게 TTL 설정

        return true;
    }
}
```

## Redis 데이터 구조

```
┌─────────────────────────────────────────────────────────────┐
│                  Login Attempt Storage                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key Pattern: login_attempt:{email}                          │
│  Value: 실패 횟수 (Integer)                                  │
│  TTL: 15분                                                   │
│                                                              │
│  예시:                                                       │
│  KEY:   login_attempt:user@example.com                      │
│  VALUE: 3                                                    │
│  TTL:   900 seconds                                          │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   IP Rate Limit Storage                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Key Pattern: rate_limit:{ip}:{endpoint}                     │
│  Type: Sorted Set                                            │
│  Members: 요청 타임스탬프                                     │
│  Score: 타임스탬프 (정렬용)                                   │
│                                                              │
│  예시:                                                       │
│  KEY:   rate_limit:192.168.1.100:login                      │
│  MEMBERS: [1704067200000, 1704067201000, 1704067202000]     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 에러 코드

```java
public enum AuthErrorCode implements ErrorCode {
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "A010",
        "Account is temporarily locked due to too many failed attempts"),

    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A020",
        "Too many requests. Please try again later"),
}
```

## Frontend 대응

```typescript
interface LoginResponse {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
}

interface LoginError {
  code: string;
  message: string;
  remainingAttempts?: number;
  lockDurationMinutes?: number;
}

async function login(email: string, password: string) {
  try {
    const response = await api.post<ApiResponse<LoginResponse>>(
      '/api/auth/login',
      { email, password }
    );
    return response.data.data;
  } catch (error: any) {
    const errorData = error.response?.data?.error as LoginError;

    if (errorData?.code === 'A010') {
      // 계정 잠금
      alert(`계정이 잠금되었습니다. ${errorData.lockDurationMinutes}분 후 다시 시도해주세요.`);
    } else if (errorData?.code === 'A002') {
      // 인증 실패
      alert(`${errorData.message}`);
    } else if (error.response?.status === 429) {
      // Rate Limit
      alert('요청이 너무 많습니다. 잠시 후 다시 시도해주세요.');
    }

    throw error;
  }
}
```

## 모니터링 및 알림

### 이상 탐지 알림

```java
@Component
@RequiredArgsConstructor
public class SecurityEventPublisher {

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Async
    public void publishBruteForceAttempt(String email, String ip, int attempts) {
        auditLogService.log(AuditEvent.builder()
            .type("BRUTE_FORCE_ATTEMPT")
            .email(email)
            .ip(ip)
            .attempts(attempts)
            .timestamp(Instant.now())
            .build());

        if (attempts >= 3) {
            // Slack/이메일 알림
            notificationService.sendSecurityAlert(
                "Brute force attempt detected",
                String.format("Email: %s, IP: %s, Attempts: %d", email, ip, attempts)
            );
        }
    }
}
```

### 메트릭

```java
@Component
@RequiredArgsConstructor
public class LoginMetrics {

    private final MeterRegistry meterRegistry;

    public void recordLoginAttempt(boolean success, String reason) {
        meterRegistry.counter("auth.login.attempts",
            "success", String.valueOf(success),
            "reason", reason
        ).increment();
    }

    public void recordAccountLock(String email) {
        meterRegistry.counter("auth.account.locked").increment();
    }
}
```

## 추가 보안 강화

### CAPTCHA 연동 (선택적)

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request,
        @RequestHeader(value = "X-Captcha-Token", required = false) String captchaToken) {

    int attempts = loginAttemptService.getAttempts(request.email());

    // 3회 이상 실패 시 CAPTCHA 요구
    if (attempts >= 3) {
        if (captchaToken == null || !captchaService.verify(captchaToken)) {
            throw new CustomBusinessException(AuthErrorCode.CAPTCHA_REQUIRED);
        }
    }

    // ... 로그인 처리
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/AuthController.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/RedisConfig.java`

## 참고 자료

- [OWASP Brute Force Protection](https://owasp.org/www-community/controls/Blocking_Brute_Force_Attacks)
- [Rate Limiting Strategies](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)
- [Bucket4j Documentation](https://bucket4j.com/)
