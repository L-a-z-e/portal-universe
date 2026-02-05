# 🔑 Password Policy 학습

> 비밀번호 복잡도와 재사용을 관리하여 계정 보안을 강화하는 기법

**난이도**: ⭐⭐ (기초)
**학습 시간**: 30분
**실습 시간**: 25분

---

## 🎯 학습 목표

이 문서를 마치면 다음을 할 수 있습니다:
- [ ] 비밀번호 정책의 필요성 이해하기
- [ ] 복잡도 검증 구현하기
- [ ] 비밀번호 히스토리 관리하기
- [ ] 만료 정책 적용하기

---

## 1️⃣ 왜 Password Policy가 필요한가?

### 약한 비밀번호의 위험

```
🔴 가장 흔한 비밀번호 (2025년 기준)
1. 123456
2. password
3. 123456789
4. 12345678
5. 12345

통계:
- 전체 사용자의 23%가 이런 비밀번호 사용
- Brute Force 공격 시 수 초 내 돌파
- 사전 공격(Dictionary Attack)에 취약
```

### 비밀번호 재사용의 위험

```
🔴 문제 시나리오
1. 사용자가 여러 사이트에서 같은 비밀번호 사용
   - 쇼핑몰: user@email.com / password123
   - 은행:   user@email.com / password123
   - SNS:    user@email.com / password123

2. 쇼핑몰이 해킹당해 계정 정보 유출

3. 공격자가 은행/SNS에 같은 비밀번호 시도
   → 모든 계정 탈취

통계:
- 사용자의 59%가 비밀번호 재사용
- 계정 유출 피해의 81%가 비밀번호 재사용 때문
```

---

## 2️⃣ NIST 비밀번호 가이드라인

### 최신 권장사항 (NIST SP 800-63B)

```
✅ 해야 할 것:
- 최소 8자 이상 (권장 15자)
- 유출된 비밀번호 차단
- 복사/붙여넣기 허용
- 비밀번호 표시 옵션 제공

❌ 하지 말아야 할 것:
- 복잡도 요구사항 강제 (특수문자 등)
  → 사용자가 "Password1!" 같은 패턴 사용
- 주기적 비밀번호 변경 강제 (90일마다 등)
  → 사용자가 간단한 변형만 함 (password1 → password2)
- 힌트 질문 사용 (어머니 성함?)
  → 소셜 엔지니어링에 취약
```

### 우리 프로젝트의 선택

```yaml
# 균형 잡힌 정책
password:
  min-length: 8               # 최소 8자
  require-uppercase: true     # 대문자 1개 이상
  require-digit: true         # 숫자 1개 이상
  require-special-char: true  # 특수문자 1개 이상
  history-count: 5            # 최근 5개 재사용 금지
  expiry-days: 90             # 90일 후 변경 권장 (강제 X)
```

**이유**:
- 복잡도: 최소한의 강제로 균형
- 히스토리: 바로 이전 비밀번호 재사용 방지
- 만료: 강제 아닌 권장으로 사용자 부담 완화

---

## 3️⃣ 프로젝트 구현

### PasswordPolicyProperties

```java
// services/auth-service/.../config/PasswordPolicyProperties.java

@ConfigurationProperties(prefix = "password.policy")
@Getter
@Setter
public class PasswordPolicyProperties {

    /**
     * 최소 길이
     */
    private int minLength = 8;

    /**
     * 대문자 필수 여부
     */
    private boolean requireUppercase = true;

    /**
     * 소문자 필수 여부
     */
    private boolean requireLowercase = true;

    /**
     * 숫자 필수 여부
     */
    private boolean requireDigit = true;

    /**
     * 특수문자 필수 여부
     */
    private boolean requireSpecialChar = true;

    /**
     * 히스토리 확인 개수 (0 = 비활성화)
     */
    private int historyCount = 5;

    /**
     * 비밀번호 만료 일수 (0 = 비활성화)
     */
    private int expiryDays = 90;
}
```

### PasswordValidator

```java
// services/auth-service/.../password/PasswordValidator.java

public interface PasswordValidator {

    /**
     * 비밀번호 정책 검증
     */
    ValidationResult validate(String password);

    /**
     * 이전 비밀번호와 재사용 여부 확인
     */
    boolean isPasswordReused(User user, String newPassword);
}
```

### PasswordValidatorImpl

```java
@Component
@RequiredArgsConstructor
public class PasswordValidatorImpl implements PasswordValidator {

    private final PasswordPolicyProperties properties;
    private final PasswordHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        // 1. 길이 검증
        if (password.length() < properties.getMinLength()) {
            errors.add(String.format(
                "비밀번호는 최소 %d자 이상이어야 합니다",
                properties.getMinLength()
            ));
        }

        // 2. 대문자 검증
        if (properties.isRequireUppercase() &&
            !password.matches(".*[A-Z].*")) {
            errors.add("비밀번호에 대문자가 최소 1개 이상 포함되어야 합니다");
        }

        // 3. 소문자 검증
        if (properties.isRequireLowercase() &&
            !password.matches(".*[a-z].*")) {
            errors.add("비밀번호에 소문자가 최소 1개 이상 포함되어야 합니다");
        }

        // 4. 숫자 검증
        if (properties.isRequireDigit() &&
            !password.matches(".*\\d.*")) {
            errors.add("비밀번호에 숫자가 최소 1개 이상 포함되어야 합니다");
        }

        // 5. 특수문자 검증
        if (properties.isRequireSpecialChar() &&
            !password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            errors.add("비밀번호에 특수문자가 최소 1개 이상 포함되어야 합니다");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    @Override
    public boolean isPasswordReused(User user, String newPassword) {
        if (properties.getHistoryCount() == 0) {
            return false;  // 히스토리 체크 비활성화
        }

        // 최근 N개 비밀번호 조회
        List<PasswordHistory> recentPasswords = historyRepository
            .findTopNByUserOrderByCreatedAtDesc(
                user,
                properties.getHistoryCount()
            );

        // 새 비밀번호가 이전 비밀번호와 일치하는지 확인
        return recentPasswords.stream()
            .anyMatch(history ->
                passwordEncoder.matches(newPassword, history.getPasswordHash())
            );
    }
}
```

### ValidationResult

```java
@Getter
@AllArgsConstructor
public class ValidationResult {
    private boolean valid;
    private List<String> errors;

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
```

---

## 4️⃣ 비밀번호 히스토리 관리

### PasswordHistory 엔티티

```java
@Entity
@Table(name = "password_history")
@Getter
@NoArgsConstructor
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String passwordHash;  // BCrypt 해시

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public PasswordHistory(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }
}
```

### Repository

```java
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * 최근 N개의 비밀번호 조회
     */
    @Query("""
        SELECT ph FROM PasswordHistory ph
        WHERE ph.user = :user
        ORDER BY ph.createdAt DESC
        LIMIT :count
        """)
    List<PasswordHistory> findTopNByUserOrderByCreatedAtDesc(
        @Param("user") User user,
        @Param("count") int count
    );

    /**
     * 오래된 히스토리 삭제 (정리 작업)
     */
    @Modifying
    @Query("""
        DELETE FROM PasswordHistory ph
        WHERE ph.user = :user
          AND ph.id NOT IN (
            SELECT ph2.id FROM PasswordHistory ph2
            WHERE ph2.user = :user
            ORDER BY ph2.createdAt DESC
            LIMIT :keepCount
          )
        """)
    void deleteOldHistories(
        @Param("user") User user,
        @Param("keepCount") int keepCount
    );
}
```

---

## 5️⃣ Service 통합

### 회원가입 시 검증

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordValidator passwordValidator;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {
        // 1. 비밀번호 정책 검증
        ValidationResult result = passwordValidator.validate(request.getPassword());
        if (!result.isValid()) {
            throw new CustomBusinessException(
                AuthErrorCode.INVALID_PASSWORD,
                String.join(", ", result.getErrors())
            );
        }

        // 2. 사용자 생성
        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        userRepository.save(user);

        // 3. 첫 비밀번호 히스토리 저장
        PasswordHistory history = PasswordHistory.builder()
            .user(user)
            .passwordHash(user.getPassword())
            .build();

        passwordHistoryRepository.save(history);

        return user;
    }
}
```

### 비밀번호 변경 시 검증

```java
@Service
@RequiredArgsConstructor
public class UserService {

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

        // 1. 현재 비밀번호 확인
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {
            throw new CustomBusinessException(
                AuthErrorCode.INVALID_PASSWORD,
                "현재 비밀번호가 일치하지 않습니다"
            );
        }

        // 2. 새 비밀번호 정책 검증
        ValidationResult result = passwordValidator.validate(request.getNewPassword());
        if (!result.isValid()) {
            throw new CustomBusinessException(
                AuthErrorCode.INVALID_PASSWORD,
                String.join(", ", result.getErrors())
            );
        }

        // 3. 비밀번호 재사용 확인
        if (passwordValidator.isPasswordReused(user, request.getNewPassword())) {
            throw new CustomBusinessException(
                AuthErrorCode.PASSWORD_REUSED,
                "최근에 사용한 비밀번호는 재사용할 수 없습니다"
            );
        }

        // 4. 비밀번호 변경
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(encodedPassword);

        // 5. 히스토리 저장
        PasswordHistory history = PasswordHistory.builder()
            .user(user)
            .passwordHash(encodedPassword)
            .build();

        passwordHistoryRepository.save(history);

        // 6. 오래된 히스토리 삭제 (5개만 유지)
        passwordHistoryRepository.deleteOldHistories(user, 5);
    }
}
```

---

## 6️⃣ 비밀번호 만료

### User 엔티티

```java
@Entity
@Getter
@NoArgsConstructor
public class User {

    // ...기존 필드들...

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    public void changePassword(String newPassword) {
        this.password = newPassword;
        this.passwordChangedAt = LocalDateTime.now();

        // 90일 후 만료
        if (passwordPolicyProperties.getExpiryDays() > 0) {
            this.passwordExpiresAt = LocalDateTime.now()
                .plusDays(passwordPolicyProperties.getExpiryDays());
        }
    }

    /**
     * 비밀번호 만료 여부 확인
     */
    public boolean isPasswordExpired() {
        if (passwordExpiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(passwordExpiresAt);
    }

    /**
     * 비밀번호 만료까지 남은 일수
     */
    public long getDaysUntilPasswordExpiry() {
        if (passwordExpiresAt == null) {
            return -1;  // 만료 없음
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), passwordExpiresAt);
    }
}
```

### 만료 알림

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        User user = // ...인증 로직...

        // 비밀번호 만료 확인
        if (user.isPasswordExpired()) {
            throw new CustomBusinessException(
                AuthErrorCode.PASSWORD_EXPIRED,
                "비밀번호가 만료되었습니다. 변경해주세요."
            );
        }

        // 만료 임박 경고 (7일 이내)
        long daysLeft = user.getDaysUntilPasswordExpiry();
        if (daysLeft >= 0 && daysLeft <= 7) {
            // 응답에 경고 메시지 포함
            return LoginResponse.builder()
                .accessToken(...)
                .refreshToken(...)
                .warning(String.format(
                    "비밀번호가 %d일 후 만료됩니다. 변경을 권장합니다.",
                    daysLeft
                ))
                .build();
        }

        return LoginResponse.builder()
            .accessToken(...)
            .refreshToken(...)
            .build();
    }
}
```

---

## ✍️ 실습 과제

### 과제 1: 비밀번호 정책 테스트 (기초)

다양한 비밀번호를 테스트하세요.

```java
@Test
void testPasswordValidation() {
    PasswordValidator validator = // ...

    // ❌ 너무 짧음
    ValidationResult result1 = validator.validate("Pass1!");
    assertThat(result1.isValid()).isFalse();
    assertThat(result1.getErrors()).contains("최소 8자 이상");

    // ❌ 대문자 없음
    ValidationResult result2 = validator.validate("password1!");
    assertThat(result2.isValid()).isFalse();
    assertThat(result2.getErrors()).contains("대문자");

    // ❌ 숫자 없음
    ValidationResult result3 = validator.validate("Password!");
    assertThat(result3.isValid()).isFalse();

    // ❌ 특수문자 없음
    ValidationResult result4 = validator.validate("Password1");
    assertThat(result4.isValid()).isFalse();

    // ✅ 모든 조건 만족
    ValidationResult result5 = validator.validate("Password1!");
    assertThat(result5.isValid()).isTrue();
}
```

### 과제 2: 비밀번호 재사용 방지 (중급)

히스토리를 확인하는 테스트를 작성하세요.

```java
@Test
void testPasswordReuse() {
    User user = // ...사용자 생성...

    // 비밀번호 5번 변경
    String[] passwords = {
        "Password1!",
        "Password2!",
        "Password3!",
        "Password4!",
        "Password5!"
    };

    for (String pwd : passwords) {
        userService.changePassword(user.getId(), new PasswordChangeRequest(
            user.getPassword(),  // 현재 비밀번호
            pwd                  // 새 비밀번호
        ));
    }

    // 첫 번째 비밀번호 재사용 시도 (6번째 변경)
    assertThatThrownBy(() ->
        userService.changePassword(user.getId(), new PasswordChangeRequest(
            "Password5!",
            "Password1!"  // 첫 번째로 돌아감
        ))
    ).isInstanceOf(CustomBusinessException.class)
     .hasMessageContaining("재사용할 수 없습니다");
}
```

### 과제 3: 비밀번호 만료 알림 (고급)

만료 임박 시 경고 메시지를 확인하세요.

```java
@Test
void testPasswordExpiryWarning() {
    User user = // ...사용자 생성...

    // 83일 전 비밀번호 변경 (90일 정책, 7일 남음)
    user.changePassword("Password1!");
    user.setPasswordChangedAt(LocalDateTime.now().minusDays(83));

    // 로그인 시도
    LoginResponse response = authService.login(new LoginRequest(
        user.getEmail(),
        "Password1!"
    ));

    // 경고 메시지 확인
    assertThat(response.getWarning())
        .contains("7일 후 만료됩니다");
}
```

---

## 🔍 더 알아보기

### Passphrase (암호문)

긴 문장을 비밀번호로 사용

```
❌ 복잡한 비밀번호: P@ssw0rd123!
   → 외우기 어려움, 타이핑 실수 많음

✅ Passphrase: ILoveMyDog2026!
   → 외우기 쉬움, 길이가 길어 안전
   → 16자 이상 권장
```

### Password Strength Meter

실시간 강도 표시

```javascript
// Frontend (Vue)
<PasswordStrengthMeter v-model="password" />

// 강도 계산
- 길이
- 문자 다양성 (대소문자, 숫자, 특수문자)
- 일반적인 패턴 회피
- 사전 단어 포함 여부

// 표시
Weak (빨강): 0-40점
Fair (주황): 41-60점
Good (노랑): 61-80점
Strong (초록): 81-100점
```

### Have I Been Pwned API

유출된 비밀번호 확인

```java
@Service
public class PwnedPasswordService {

    public boolean isPasswordPwned(String password) {
        // 1. SHA-1 해시 계산
        String hash = DigestUtils.sha1Hex(password).toUpperCase();

        // 2. 첫 5자리로 API 호출
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);

        ResponseEntity<String> response = restTemplate.getForEntity(
            "https://api.pwnedpasswords.com/range/" + prefix,
            String.class
        );

        // 3. 결과에서 해시 찾기
        return response.getBody().contains(suffix);
    }
}
```

---

## 🎯 체크리스트

학습을 마쳤다면 체크해보세요:

- [ ] NIST 비밀번호 가이드라인을 이해한다
- [ ] 복잡도 검증을 구현할 수 있다
- [ ] 비밀번호 히스토리를 관리할 수 있다
- [ ] 비밀번호 만료 정책을 적용할 수 있다
- [ ] 재사용 방지 테스트를 작성했다

---

## 📚 참고 자료

- [NIST SP 800-63B: Digital Identity Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [Have I Been Pwned](https://haveibeenpwned.com/)

---

**이전**: [Security Audit Logging](./04-security-audit.md)
**다음**: [Input Validation 학습하기](./06-input-validation.md) →
