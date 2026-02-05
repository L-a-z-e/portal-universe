# Registration (회원가입 플로우)

## 개요

Portal Universe auth-service는 두 가지 회원가입 경로를 지원합니다: 이메일 회원가입과 소셜 로그인 회원가입. 두 경로 모두 User와 UserProfile 엔티티가 함께 생성되며, Kafka를 통해 다른 서비스에 가입 이벤트를 전파합니다.

## 회원가입 플로우

### 이메일 회원가입

```
┌──────────┐    POST /api/users/signup    ┌──────────────┐
│  Client  │ ────────────────────────────▶│ UserController│
└──────────┘   {email, password,          └──────┬───────┘
               nickname, realName,                │
               marketingAgree}                    │
                                                  ▼
                                          ┌──────────────┐
                                          │ UserService  │
                                          │ registerUser │
                                          └──────┬───────┘
                                                  │
                    ┌─────────────────────────────┼─────────────────────────────┐
                    ▼                             ▼                             ▼
            ┌──────────────┐            ┌──────────────┐            ┌──────────────┐
            │ 이메일 중복   │            │ 비밀번호      │            │ User +       │
            │ 확인          │            │ BCrypt 암호화 │            │ Profile 생성 │
            └──────────────┘            └──────────────┘            └──────────────┘
                                                                            │
                                                                            ▼
                                                                    ┌──────────────┐
                                                                    │ JPA Save     │
                                                                    │ (Cascade)    │
                                                                    └──────┬───────┘
                                                                            │
                                                                            ▼
                                                                    ┌──────────────┐
                                                                    │ Kafka Event  │
                                                                    │ user-signup  │
                                                                    └──────────────┘
```

### 소셜 로그인 회원가입

```
┌──────────┐    /oauth2/authorization/google    ┌──────────────────────┐
│  Client  │ ──────────────────────────────────▶│ OAuth2 Flow          │
└──────────┘                                    │ (Spring Security)    │
                                                └──────────┬───────────┘
                                                           │
                                                           ▼
                                                ┌──────────────────────┐
                                                │ CustomOAuth2UserSvc  │
                                                │ loadUser()           │
                                                └──────────┬───────────┘
                                                           │
                    ┌──────────────────────────────────────┼───────────────────────────────────┐
                    ▼                                      ▼                                   ▼
            ┌──────────────┐                      ┌──────────────┐                    ┌──────────────┐
            │ 기존 소셜    │     있음             │ 동일 이메일   │     있음          │ 신규 사용자  │
            │ 계정 조회    │ ─────────▶ 로그인    │ 사용자 조회   │ ─────────▶ 연동   │ 생성         │
            └──────────────┘     (기존 User 반환)  └──────────────┘  (소셜 계정 추가)   └──────────────┘
```

## UserService 구현

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    /**
     * 회원가입 요청 DTO
     */
    public record SignupCommand(
        String email,
        String password,
        String nickname,
        String realName,
        boolean marketingAgree
    ) {}

    /**
     * 일반 이메일 회원가입 처리
     */
    @Transactional
    public Long registerUser(SignupCommand command) {
        // 1. 이메일 중복 확인
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new CustomBusinessException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(command.password());

        // 3. User 엔티티 생성
        User newUser = new User(command.email(), encodedPassword, Role.USER);

        // 4. UserProfile 엔티티 생성 및 연결
        UserProfile profile = new UserProfile(
                newUser,
                command.nickname(),
                command.realName(),
                command.marketingAgree()
        );
        newUser.setProfile(profile);

        // 5. 저장 (Cascade로 인해 Profile도 함께 저장)
        User savedUser = userRepository.save(newUser);

        // 6. 이벤트 발행
        UserSignedUpEvent event = new UserSignedUpEvent(
                savedUser.getUuid(),
                savedUser.getEmail(),
                savedUser.getProfile().getNickname()
        );
        kafkaTemplate.send("user-signup", event);

        return savedUser.getId();
    }
}
```

## Controller 구현

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 API
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Long>>> signup(
            @Valid @RequestBody SignupRequest request) {

        UserService.SignupCommand command = new UserService.SignupCommand(
            request.email(),
            request.password(),
            request.nickname(),
            request.realName(),
            request.marketingAgree()
        );

        Long userId = userService.registerUser(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("userId", userId)));
    }
}
```

## Request/Response DTO

### SignupRequest

```java
public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    String password,

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    String nickname,

    @Size(max = 50, message = "실명은 50자 이하여야 합니다")
    String realName,

    boolean marketingAgree
) {}
```

## 소셜 로그인 회원가입

### CustomOAuth2UserService

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory
            .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        User user = processOAuth2User(registrationId, userInfo);

        return new CustomOAuth2User(user, oAuth2User.getAttributes(),
            userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName());
    }

    private User processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        SocialProvider provider = SocialProvider.valueOf(registrationId.toUpperCase());
        String providerId = userInfo.getId();
        String email = userInfo.getEmail();

        // 1. 기존 소셜 계정 확인
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
                .findByProviderAndProviderId(provider, providerId);

        if (existingSocialAccount.isPresent()) {
            return existingSocialAccount.get().getUser();
        }

        // 2. 동일 이메일 사용자 확인 → 계정 연동
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            linkSocialAccount(user, provider, providerId);
            return user;
        }

        // 3. 신규 사용자 생성
        return createNewUser(provider, providerId, userInfo);
    }

    private User createNewUser(SocialProvider provider, String providerId,
                               OAuth2UserInfo userInfo) {
        // User 생성 (password는 null - 소셜 로그인 사용자)
        User user = new User(userInfo.getEmail(), null, Role.USER);

        // UserProfile 생성
        String nickname = userInfo.getName() != null
            ? userInfo.getName()
            : "User_" + providerId.substring(0, 8);
        UserProfile profile = new UserProfile(user, nickname, userInfo.getImageUrl());
        user.setProfile(profile);

        // SocialAccount 생성
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);

        return userRepository.save(user);
    }

    private void linkSocialAccount(User user, SocialProvider provider, String providerId) {
        SocialAccount socialAccount = new SocialAccount(user, provider, providerId);
        user.getSocialAccounts().add(socialAccount);
        userRepository.save(user);
    }
}
```

## 이벤트 발행 (Kafka)

### UserSignedUpEvent

```java
package com.portal.universe.common.event;

public record UserSignedUpEvent(
    String userId,      // UUID
    String email,
    String nickname
) {}
```

### 이벤트 발행

```java
UserSignedUpEvent event = new UserSignedUpEvent(
    savedUser.getUuid(),
    savedUser.getEmail(),
    savedUser.getProfile().getNickname()
);
kafkaTemplate.send("user-signup", event);
```

### 이벤트 구독 (notification-service)

```java
@KafkaListener(topics = "user-signup", groupId = "notification-service")
public void handleUserSignup(UserSignedUpEvent event) {
    // 환영 이메일 발송
    emailService.sendWelcomeEmail(event.email(), event.nickname());

    // 가입 축하 알림 생성
    notificationService.createNotification(
        event.userId(),
        "가입을 축하합니다!",
        NotificationType.WELCOME
    );
}
```

## Validation 규칙

### 이메일

```java
@NotBlank(message = "이메일은 필수입니다")
@Email(message = "올바른 이메일 형식이 아닙니다")
String email
```

### 비밀번호

```java
@NotBlank(message = "비밀번호는 필수입니다")
@Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
    message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다"
)
String password
```

### Username (설정 시)

```java
// UserService.java
private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

public UserProfileResponse setUsername(Long userId, String username) {
    if (!USERNAME_PATTERN.matcher(username).matches()) {
        throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
    }
    // ...
}
```

## 에러 처리

### AuthErrorCode

```java
public enum AuthErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "Email already exists"),
    USERNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "A011", "Username already exists"),
    INVALID_USERNAME_FORMAT(HttpStatus.BAD_REQUEST, "A013",
        "Invalid username format. Only lowercase letters, numbers, and underscores are allowed (3-20 characters)"),
    // ...
}
```

## Frontend 통합

### 회원가입 폼 예시 (Vue)

```vue
<template>
  <form @submit.prevent="handleSignup">
    <input v-model="email" type="email" placeholder="이메일" required />
    <input v-model="password" type="password" placeholder="비밀번호" required />
    <input v-model="nickname" placeholder="닉네임" required />
    <input v-model="realName" placeholder="실명 (선택)" />
    <label>
      <input v-model="marketingAgree" type="checkbox" />
      마케팅 정보 수신 동의
    </label>
    <button type="submit">가입하기</button>
  </form>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '@/lib/api';

const email = ref('');
const password = ref('');
const nickname = ref('');
const realName = ref('');
const marketingAgree = ref(false);

async function handleSignup() {
  try {
    await api.post('/api/users/signup', {
      email: email.value,
      password: password.value,
      nickname: nickname.value,
      realName: realName.value || null,
      marketingAgree: marketingAgree.value
    });
    alert('가입이 완료되었습니다. 로그인해주세요.');
    router.push('/login');
  } catch (error) {
    if (error.response?.data?.code === 'A001') {
      alert('이미 사용 중인 이메일입니다.');
    }
  }
}
</script>
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/UserService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/UserController.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/oauth2/CustomOAuth2UserService.java`

## 참고 자료

- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/)
- [Spring Data JPA Cascade](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html)
