# 🔐 Login Security 학습

> 로그인 시도를 추적하고 계정을 보호하는 기법

**난이도**: ⭐⭐ (기초)
**학습 시간**: 30분
**실습 시간**: 20분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] Brute Force 공격의 위험성 이해하기
- [ ] 로그인 시도 추적 구현하기
- [ ] 점진적 계정 잠금 정책 설계하기
- [ ] IP와 Email 기반 차단 적용하기

---

## 1️⃣ 왜 Login Security가 필요한가?

### Brute Force 공격

무차별 대입 공격으로 비밀번호를 추측

```
🔴 공격 시나리오
1. 공격자가 로그인 API에 자동화된 요청
2. 1초에 100번 시도 (Rate Limiting 없으면)
3. 일반적인 비밀번호 조합 대입
   - password123
   - admin1234
   - qwerty123
   - 생일조합 (19900101)

결과:
- 약한 비밀번호는 수 시간 내 뚫림
- 서버 리소스 고갈
- 정상 사용자 서비스 방해
```

### Credential Stuffing

유출된 계정 정보로 대량 로그인 시도

```
🔴 공격 방법
1. 다크웹에서 유출된 이메일/비밀번호 구매
   (다른 사이트 해킹으로 유출된 것)
2. 동일한 비밀번호를 재사용하는 사용자 찾기
3. 자동화 봇으로 대량 로그인 시도

통계:
- 사용자의 59%가 비밀번호 재사용
- 2020년 기준 193억 개의 계정 정보 유출
```

---

## 2️⃣ 점진적 잠금 정책

### 계단식 Lockout

실패 횟수에 따라 잠금 시간 증가

```
┌─────────────┬──────────────┬─────────────────┐
│ 실패 횟수   │  잠금 시간   │      설명       │
├─────────────┼──────────────┼─────────────────┤
│ 1-4회       │  없음        │ 정상 사용자 허용│
├─────────────┼──────────────┼─────────────────┤
│ 5-9회       │  15분        │ 의심스러운 시도 │
├─────────────┼──────────────┼─────────────────┤
│ 10-14회     │  1시간       │ 명백한 공격     │
├─────────────┼──────────────┼─────────────────┤
│ 15회 이상   │  24시간      │ 심각한 공격     │
└─────────────┴──────────────┴─────────────────┘
```

### 왜 점진적인가?

```
✅ 정상 사용자 보호
- 비밀번호 오타 3-4번은 흔함
- 즉시 잠금 시 사용자 경험 저하

⚖️ 공격자 지연
- 5회 실패 → 15분 대기
- 10회 실패 → 1시간 대기
- 공격 비용 증가, 속도 감소

🎯 균형
- 사용성 vs 보안
- 너무 엄격하면 불편, 너무 느슨하면 위험
```

---

## 3️⃣ 프로젝트 구현

### LoginAttemptService

```java
// services/auth-service/.../service/LoginAttemptService.java

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS_TIER_1 = 5;   // 15분 잠금
    private static final int MAX_ATTEMPTS_TIER_2 = 10;  // 1시간 잠금
    private static final int MAX_ATTEMPTS_TIER_3 = 15;  // 24시간 잠금

    // ConcurrentHashMap으로 멀티스레드 안전
    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    /**
     * 로그인 실패 기록
     */
    public void recordFailure(String key) {
        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());
        info.incrementFailures();

        // 점진적 잠금 적용
        if (info.getFailures() >= MAX_ATTEMPTS_TIER_3) {
            info.lockUntil(Duration.ofHours(24));
        } else if (info.getFailures() >= MAX_ATTEMPTS_TIER_2) {
            info.lockUntil(Duration.ofHours(1));
        } else if (info.getFailures() >= MAX_ATTEMPTS_TIER_1) {
            info.lockUntil(Duration.ofMinutes(15));
        }
    }

    /**
     * 로그인 성공 시 초기화
     */
    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /**
     * 잠금 여부 확인
     */
    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) {
            return false;
        }

        // 잠금 시간이 지났는지 확인
        if (info.getLockedUntil() != null &&
            LocalDateTime.now().isAfter(info.getLockedUntil())) {
            attempts.remove(key);  // 만료된 잠금 제거
            return false;
        }

        return info.getLockedUntil() != null;
    }

    /**
     * 남은 잠금 시간 조회 (초 단위)
     */
    public long getRemainingLockTime(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null || info.getLockedUntil() == null) {
            return 0;
        }

        long seconds = ChronoUnit.SECONDS.between(
            LocalDateTime.now(),
            info.getLockedUntil()
        );

        return Math.max(0, seconds);
    }
}
```

### AttemptInfo 클래스

```java
@Getter
public class AttemptInfo {
    private int failures = 0;
    private LocalDateTime lockedUntil;

    public void incrementFailures() {
        this.failures++;
    }

    public void lockUntil(Duration duration) {
        this.lockedUntil = LocalDateTime.now().plus(duration);
    }
}
```

---

## 4️⃣ Controller 통합

### AuthController

```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = getClientIp(httpRequest);
        String email = request.getEmail();

        // 1. IP 기반 차단 확인
        if (loginAttemptService.isBlocked(clientIp)) {
            long remainingSeconds = loginAttemptService.getRemainingLockTime(clientIp);
            throw new CustomBusinessException(
                AuthErrorCode.LOGIN_BLOCKED,
                "IP가 차단되었습니다. " + remainingSeconds + "초 후 재시도하세요."
            );
        }

        // 2. Email 기반 차단 확인
        if (loginAttemptService.isBlocked(email)) {
            long remainingSeconds = loginAttemptService.getRemainingLockTime(email);
            throw new CustomBusinessException(
                AuthErrorCode.ACCOUNT_LOCKED,
                "계정이 잠겼습니다. " + remainingSeconds + "초 후 재시도하세요."
            );
        }

        try {
            // 3. 로그인 시도
            LoginResponse response = authService.login(request);

            // 4. 성공 시 초기화
            loginAttemptService.recordSuccess(clientIp);
            loginAttemptService.recordSuccess(email);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (AuthenticationException e) {
            // 5. 실패 시 기록
            loginAttemptService.recordFailure(clientIp);
            loginAttemptService.recordFailure(email);

            throw new CustomBusinessException(
                AuthErrorCode.INVALID_CREDENTIALS,
                "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }
    }

    /**
     * X-Forwarded-For를 고려한 실제 IP 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For: client, proxy1, proxy2
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
```

---

## 5️⃣ IP vs Email 기반 차단

### IP 기반 차단

```java
String clientIp = "192.168.1.100";
loginAttemptService.isBlocked(clientIp);
```

**장점**:
- 공격자의 물리적 위치 차단
- 동일 IP의 모든 계정 시도 차단

**단점**:
- NAT 환경에서 정상 사용자 피해 가능
- VPN/Proxy로 우회 가능

**사용 사례**:
- 대량 봇 공격 차단
- DDoS 공격 완화

### Email 기반 차단

```java
String email = "target@example.com";
loginAttemptService.isBlocked(email);
```

**장점**:
- 특정 계정 타겟 공격 방어
- 정확한 계정 보호

**단점**:
- 공격자가 여러 계정 공격 시 무력화

**사용 사례**:
- 특정 계정 탈취 시도 차단
- Credential Stuffing 방어

### Composite 전략 (우리 프로젝트)

```java
// 둘 다 체크
if (loginAttemptService.isBlocked(clientIp) ||
    loginAttemptService.isBlocked(email)) {
    // 차단
}

// 둘 다 기록
loginAttemptService.recordFailure(clientIp);
loginAttemptService.recordFailure(email);
```

**효과**:
- IP 우회 공격도 Email로 차단
- Email 분산 공격도 IP로 차단

---

## 6️⃣ 메모리 관리

### 문제: 무한 증가

```java
// ❌ 문제: attempts Map이 무한 증가
private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

// 공격자가 100만 개의 IP로 시도
// → 100만 개의 AttemptInfo 객체 생성
// → OutOfMemoryError
```

### 해결 1: TTL 기반 제거

```java
@Scheduled(fixedRate = 60000)  // 1분마다
public void cleanupExpiredAttempts() {
    LocalDateTime now = LocalDateTime.now();

    attempts.entrySet().removeIf(entry -> {
        AttemptInfo info = entry.getValue();

        // 잠금 시간이 지났으면 제거
        if (info.getLockedUntil() != null &&
            now.isAfter(info.getLockedUntil())) {
            return true;
        }

        // 실패 기록이 1시간 이상 오래되었으면 제거
        if (info.getLastFailureTime() != null &&
            ChronoUnit.HOURS.between(info.getLastFailureTime(), now) > 1) {
            return true;
        }

        return false;
    });
}
```

### 해결 2: Redis 사용 (권장)

```java
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, AttemptInfo> redisTemplate;

    public void recordFailure(String key) {
        String redisKey = "login_attempt:" + key;

        AttemptInfo info = redisTemplate.opsForValue().get(redisKey);
        if (info == null) {
            info = new AttemptInfo();
        }

        info.incrementFailures();

        // TTL 설정 (24시간 후 자동 삭제)
        redisTemplate.opsForValue().set(
            redisKey,
            info,
            Duration.ofHours(24)
        );
    }
}
```

**장점**:
- 자동 TTL 관리
- 분산 환경 지원
- 메모리 효율적

---

## ✍️ 실습 과제

### 과제 1: 잠금 테스트 (기초)

로그인을 5번 실패하고 잠금을 확인하세요.

```bash
# 5번 실패
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}'
done

# 6번째 시도 (차단되어야 함)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrong"}' \
  -v
```

**확인사항**:
- [ ] 6번째 요청이 `ACCOUNT_LOCKED` 에러를 반환하는가?
- [ ] 응답에 남은 시간이 표시되는가?
- [ ] 15분 후 다시 시도할 수 있는가?

### 과제 2: IP vs Email 차단 (중급)

IP 차단과 Email 차단을 각각 테스트하세요.

```bash
# 시나리오 1: 동일 IP에서 여러 계정 공격
# → IP 차단으로 모든 계정 보호

for email in user1@test.com user2@test.com user3@test.com; do
  for i in {1..5}; do
    curl -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d "{\"email\":\"$email\",\"password\":\"wrong\"}"
  done
done

# 시나리오 2: 여러 IP에서 동일 계정 공격
# → Email 차단으로 계정 보호 (프록시 사용 시뮬레이션)

for proxy in proxy1 proxy2 proxy3; do
  for i in {1..5}; do
    curl -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -H "X-Forwarded-For: 192.168.$proxy.1" \
      -d '{"email":"target@example.com","password":"wrong"}'
  done
done
```

### 과제 3: 점진적 잠금 시간 확인 (고급)

각 단계별 잠금 시간을 확인하세요.

```java
@Test
void testProgressiveLockout() {
    String key = "test-user";

    // 5번 실패 → 15분 잠금
    for (int i = 0; i < 5; i++) {
        loginAttemptService.recordFailure(key);
    }
    assertThat(loginAttemptService.isBlocked(key)).isTrue();
    assertThat(loginAttemptService.getRemainingLockTime(key))
        .isCloseTo(900, Offset.offset(10L));  // 15분 = 900초

    loginAttemptService.recordSuccess(key);  // 초기화

    // 10번 실패 → 1시간 잠금
    for (int i = 0; i < 10; i++) {
        loginAttemptService.recordFailure(key);
    }
    assertThat(loginAttemptService.getRemainingLockTime(key))
        .isCloseTo(3600, Offset.offset(10L));  // 1시간 = 3600초

    loginAttemptService.recordSuccess(key);  // 초기화

    // 15번 실패 → 24시간 잠금
    for (int i = 0; i < 15; i++) {
        loginAttemptService.recordFailure(key);
    }
    assertThat(loginAttemptService.getRemainingLockTime(key))
        .isCloseTo(86400, Offset.offset(10L));  // 24시간 = 86400초
}
```

---

## 🔍 더 알아보기

### CAPTCHA 통합

일정 횟수 실패 후 CAPTCHA 요구

```java
if (info.getFailures() >= 3) {
    // CAPTCHA 검증 요구
    if (!captchaService.verify(request.getCaptchaToken())) {
        throw new CustomBusinessException(
            AuthErrorCode.INVALID_CAPTCHA
        );
    }
}
```

### 2FA (Two-Factor Authentication)

로그인 후 추가 인증

```
1. 비밀번호 입력 → 성공
2. SMS/Email OTP 전송
3. OTP 입력 → 최종 인증
```

### 이상 탐지 (Anomaly Detection)

ML 기반 비정상 로그인 탐지

```
- 평소와 다른 시간대 로그인
- 다른 국가에서의 로그인
- 새로운 디바이스
→ 이메일 알림 + 추가 인증 요구
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] Brute Force 공격의 위험성을 이해한다
- [ ] 점진적 잠금 정책의 장점을 설명할 수 있다
- [ ] IP와 Email 기반 차단의 차이를 이해한다
- [ ] 실제 프로젝트에서 로그인 차단을 테스트했다
- [ ] 메모리 관리 전략을 이해한다

---

## 📚 참고 자료

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Credential Stuffing 방어](https://owasp.org/www-community/attacks/Credential_stuffing)
- [Progressive Lockout Strategies](https://auth0.com/blog/dont-pass-on-the-new-nist-password-guidelines/)

---

**이전**: [JWT Key Rotation](./02-jwt-key-rotation.md)
**다음**: [Security Audit Logging 학습하기](./04-security-audit.md) →
