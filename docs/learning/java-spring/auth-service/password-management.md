# Password Management (BCrypt, 비밀번호 정책)

## 개요

Portal Universe auth-service는 BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해시하고 저장합니다. 소셜 로그인 사용자는 비밀번호가 없으며, 비밀번호 변경 시 현재 비밀번호 확인을 요구합니다.

## BCrypt 알고리즘

### 특징

```
┌─────────────────────────────────────────────────────────────┐
│                      BCrypt 특징                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Salted Hash                                              │
│     - 각 비밀번호마다 고유한 Salt 자동 생성                   │
│     - Rainbow Table 공격 방지                                │
│                                                              │
│  2. Adaptive Function                                        │
│     - Cost Factor로 연산 강도 조절 가능                       │
│     - 하드웨어 발전에 대응                                    │
│                                                              │
│  3. 단방향 해시                                               │
│     - 원본 비밀번호 복원 불가                                 │
│     - 비교만 가능 (matches)                                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### BCrypt 해시 구조

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGpN.hXbWi5kHqr1Z.BwM2mYvTWi
 │  │  └─────────────────────────────────────────────────────────┐
 │  │                                                            │
 │  └── Cost Factor (2^10 = 1024 iterations)                     │
 │                                                                │
 └──── Algorithm identifier ($2a$ = BCrypt)                      │
                                                                  │
       ├──────────────────────┤├─────────────────────────────────┤
                Salt (22자)            Hash (31자)
```

## Spring Security 설정

### PasswordEncoder Bean

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 비밀번호를 안전하게 암호화하기 위한 PasswordEncoder를 Bean으로 등록합니다.
     * BCrypt 알고리즘을 사용합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### BCryptPasswordEncoder 옵션

```java
// 기본 strength: 10 (2^10 iterations)
new BCryptPasswordEncoder()

// Custom strength 설정 (10-31, 기본값: 10)
new BCryptPasswordEncoder(12)  // 2^12 = 4096 iterations

// SecureRandom 지정
new BCryptPasswordEncoder(10, new SecureRandom())
```

## 비밀번호 처리

### 회원가입 시 암호화

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long registerUser(SignupCommand command) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // User 엔티티 생성 (암호화된 비밀번호 저장)
        User newUser = new User(command.email(), encodedPassword, Role.USER);

        return userRepository.save(newUser).getId();
    }
}
```

### 로그인 시 검증

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest request) {

    User user = userRepository.findByEmailWithProfile(request.email())
            .orElseThrow(() -> new CustomBusinessException(
                AuthErrorCode.INVALID_CREDENTIALS));

    // 비밀번호 검증
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    // 토큰 발급...
}
```

## 비밀번호 변경

### ProfileService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 비밀번호를 변경합니다.
     * 소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // 1. 소셜 로그인 사용자 체크
        if (user.isSocialUser()) {
            throw new CustomBusinessException(
                AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD
            );
        }

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 3. 새 비밀번호 확인
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 4. 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);

        log.info("Password changed for user: {}", userId);
    }
}
```

### ChangePasswordRequest DTO

```java
public record ChangePasswordRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    String currentPassword,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    String newPassword,

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    String confirmPassword
) {}
```

### User Entity 메서드

```java
@Entity
public class User {
    // ...

    /**
     * 비밀번호를 변경합니다.
     * @param encodedPassword 암호화된 새 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 소셜 로그인 사용자인지 확인합니다.
     * 비밀번호가 null이고 소셜 계정이 연결되어 있으면 소셜 사용자입니다.
     */
    public boolean isSocialUser() {
        return this.password == null && !this.socialAccounts.isEmpty();
    }
}
```

## 비밀번호 정책

### 현재 정책

```java
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
    message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
)
```

| 규칙 | 설명 |
|------|------|
| 최소 길이 | 8자 이상 |
| 최대 길이 | 100자 이하 |
| 영문 | 최소 1자 |
| 숫자 | 최소 1자 |
| 특수문자 | 최소 1자 (@$!%*#?&) |

### 강화된 정책 예시

```java
public class PasswordPolicy {

    // 비밀번호 강도 검사
    public PasswordStrength checkStrength(String password) {
        int score = 0;

        if (password.length() >= 12) score += 2;
        else if (password.length() >= 8) score += 1;

        if (password.matches(".*[a-z].*")) score += 1;
        if (password.matches(".*[A-Z].*")) score += 1;
        if (password.matches(".*\\d.*")) score += 1;
        if (password.matches(".*[@$!%*#?&].*")) score += 1;

        // 연속 문자 감점
        if (password.matches(".*(.)\\1{2,}.*")) score -= 1;
        // 키보드 패턴 감점
        if (containsKeyboardPattern(password)) score -= 1;

        if (score >= 6) return PasswordStrength.STRONG;
        if (score >= 4) return PasswordStrength.MEDIUM;
        return PasswordStrength.WEAK;
    }

    // 이전 비밀번호와의 유사도 검사
    public boolean isTooSimilarToPrevious(String newPassword, String previousHash) {
        // 이전 비밀번호 해시와 비교는 불가능
        // 대신 이전 N개 해시 저장 후 동일 여부 확인
        return false;
    }

    // 금지어 검사
    private static final Set<String> FORBIDDEN_WORDS = Set.of(
        "password", "123456", "qwerty", "admin"
    );

    public boolean containsForbiddenWord(String password) {
        String lower = password.toLowerCase();
        return FORBIDDEN_WORDS.stream()
            .anyMatch(lower::contains);
    }
}

public enum PasswordStrength {
    WEAK, MEDIUM, STRONG
}
```

## 에러 코드

```java
public enum AuthErrorCode implements ErrorCode {

    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "A006",
        "Social login users cannot change password"),

    INVALID_CURRENT_PASSWORD(HttpStatus.UNAUTHORIZED, "A007",
        "Current password is incorrect"),

    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "A008",
        "Password confirmation does not match"),

    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "A009",
        "Password is incorrect"),
}
```

## Controller Endpoint

```java
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 비밀번호 변경
     */
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        Long id = getUserIdFromUuid(userId);
        profileService.changePassword(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(Map.of("message", "비밀번호가 변경되었습니다"))
        );
    }
}
```

## Frontend 구현

### 비밀번호 변경 폼 (Vue)

```vue
<template>
  <form @submit.prevent="handleChangePassword">
    <div class="form-group">
      <label>현재 비밀번호</label>
      <input v-model="currentPassword" type="password" required />
    </div>

    <div class="form-group">
      <label>새 비밀번호</label>
      <input v-model="newPassword" type="password" required />
      <PasswordStrengthIndicator :password="newPassword" />
    </div>

    <div class="form-group">
      <label>새 비밀번호 확인</label>
      <input v-model="confirmPassword" type="password" required />
      <span v-if="!passwordsMatch" class="error">비밀번호가 일치하지 않습니다</span>
    </div>

    <button type="submit" :disabled="!isValid">비밀번호 변경</button>
  </form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { api } from '@/lib/api';

const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

const passwordsMatch = computed(() =>
  newPassword.value === confirmPassword.value
);

const isValid = computed(() =>
  currentPassword.value &&
  newPassword.value.length >= 8 &&
  passwordsMatch.value
);

async function handleChangePassword() {
  try {
    await api.post('/api/profile/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    });
    alert('비밀번호가 변경되었습니다.');
  } catch (error) {
    if (error.response?.data?.code === 'A007') {
      alert('현재 비밀번호가 일치하지 않습니다.');
    }
  }
}
</script>
```

### 비밀번호 강도 표시기

```vue
<template>
  <div class="strength-indicator">
    <div class="bar" :class="strengthClass">
      <div class="fill" :style="{ width: strengthWidth }"></div>
    </div>
    <span>{{ strengthLabel }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ password: string }>();

const strength = computed(() => {
  const { password } = props;
  if (!password) return 0;

  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[@$!%*#?&]/.test(password)) score++;

  return Math.min(score, 5);
});

const strengthClass = computed(() => {
  if (strength.value <= 2) return 'weak';
  if (strength.value <= 4) return 'medium';
  return 'strong';
});

const strengthWidth = computed(() => `${(strength.value / 5) * 100}%`);
const strengthLabel = computed(() => ['', '매우 약함', '약함', '보통', '강함', '매우 강함'][strength.value]);
</script>
```

## 보안 고려사항

### 1. 비밀번호 전송

```
항상 HTTPS 사용
- 평문 비밀번호가 네트워크에서 암호화되어 전송
- 개발 환경에서도 가능하면 TLS 사용
```

### 2. 비밀번호 로깅 금지

```java
// BAD: 비밀번호를 로그에 기록
log.info("Login attempt: email={}, password={}", email, password);

// GOOD: 비밀번호 제외
log.info("Login attempt: email={}", email);
```

### 3. 타이밍 공격 방지

```java
// BCryptPasswordEncoder.matches()는 내부적으로 상수 시간 비교 사용
// 직접 문자열 비교는 타이밍 공격에 취약할 수 있음
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/config/SecurityConfig.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/ProfileService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/dto/profile/ChangePasswordRequest.java`

## 참고 자료

- [BCrypt Algorithm](https://en.wikipedia.org/wiki/Bcrypt)
- [Spring Security Password Encoding](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
