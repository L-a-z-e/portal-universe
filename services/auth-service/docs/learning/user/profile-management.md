# Profile Management (프로필 관리)

## 개요

Portal Universe auth-service는 사용자 프로필 관리 기능을 제공합니다. 프로필 조회, 수정, 비밀번호 변경, 회원 탈퇴 등의 기능을 ProfileService와 UserService를 통해 처리합니다.

## 프로필 데이터 구조

### UserProfile Entity

```java
@Entity
@Table(name = "user_profiles")
@Getter
public class UserProfile {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;       // 표시 이름 (필수)

    @Column(length = 50)
    private String realName;       // 실명 (선택)

    @Column(unique = true, length = 20)
    private String username;       // @username (1회 설정)

    @Column(length = 200)
    private String bio;            // 자기소개

    @Column(length = 20)
    private String phoneNumber;    // 전화번호

    @Column(length = 255)
    private String profileImageUrl; // 프로필 이미지

    @Column(length = 255)
    private String website;        // 개인 웹사이트

    @Column(nullable = false)
    private boolean marketingAgree; // 마케팅 동의
}
```

## ProfileService 구현

```java
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 프로필을 조회합니다.
     */
    public ProfileResponse getProfile(Long userId) {
        User user = findUserById(userId);
        return ProfileResponse.from(user);
    }

    /**
     * 사용자 프로필을 수정합니다.
     */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        UserProfile profile = user.getProfile();

        // 각 필드가 null이 아닌 경우에만 업데이트
        if (request.nickname() != null) {
            profile.updateNickname(request.nickname());
        }
        if (request.realName() != null) {
            profile.updateRealName(request.realName());
        }
        if (request.phoneNumber() != null) {
            profile.updatePhoneNumber(request.phoneNumber());
        }
        if (request.profileImageUrl() != null) {
            profile.updateProfileImageUrl(request.profileImageUrl());
        }
        if (request.marketingAgree() != null) {
            profile.updateMarketingAgree(request.marketingAgree());
        }

        log.info("Profile updated for user: {}", userId);
        return ProfileResponse.from(user);
    }

    /**
     * 비밀번호를 변경합니다.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        // 소셜 로그인 사용자 체크
        if (user.isSocialUser()) {
            throw new CustomBusinessException(
                AuthErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호 확인
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new CustomBusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);

        log.info("Password changed for user: {}", userId);
    }

    /**
     * 회원 탈퇴를 처리합니다. (Soft Delete)
     */
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = findUserById(userId);

        // 소셜 로그인 사용자가 아닌 경우 비밀번호 확인
        if (!user.isSocialUser()) {
            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new CustomBusinessException(AuthErrorCode.INVALID_PASSWORD);
            }
        }

        // Soft Delete - 상태를 WITHDRAWAL_PENDING으로 변경
        user.markForWithdrawal();

        log.info("Account marked for withdrawal: userId={}, reason={}",
                 userId, request.reason());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}
```

## UserService - 공개 프로필 조회

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    /**
     * Username으로 사용자 프로필 조회 (공개)
     */
    public UserProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * 내 프로필 조회
     */
    public UserProfileResponse getMyProfile(Long userId) {
        User user = findUserByIdOrThrow(userId);
        FollowCounts counts = getFollowCounts(user);

        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * 프로필 수정
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, String nickname, String bio,
                                            String profileImageUrl, String website) {
        User user = findUserByIdOrThrow(userId);
        user.getProfile().updateProfile(nickname, bio, profileImageUrl, website);

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * Username 설정 (최초 1회)
     */
    @Transactional
    public UserProfileResponse setUsername(Long userId, String username) {
        // Username 형식 검증
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
        }

        User user = findUserByIdOrThrow(userId);

        // 이미 설정된 경우
        if (user.getProfile().getUsername() != null) {
            throw new CustomBusinessException(AuthErrorCode.USERNAME_ALREADY_SET);
        }

        // 중복 확인
        if (userRepository.existsByUsername(username)) {
            throw new CustomBusinessException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        user.getProfile().setUsername(username);

        FollowCounts counts = getFollowCounts(user);
        return UserProfileResponse.from(user, counts.followerCount(), counts.followingCount());
    }

    /**
     * Username 중복 확인
     */
    public boolean checkUsernameAvailability(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new CustomBusinessException(AuthErrorCode.INVALID_USERNAME_FORMAT);
        }
        return !userRepository.existsByUsername(username);
    }

    private FollowCounts getFollowCounts(User user) {
        int followerCount = (int) followRepository.countByFollowing(user);
        int followingCount = (int) followRepository.countByFollower(user);
        return new FollowCounts(followerCount, followingCount);
    }

    private record FollowCounts(int followerCount, int followingCount) {}
}
```

## DTO 정의

### ProfileResponse

```java
public record ProfileResponse(
    String uuid,
    String email,
    String nickname,
    String realName,
    String username,
    String bio,
    String phoneNumber,
    String profileImageUrl,
    String website,
    boolean marketingAgree,
    boolean isSocialUser
) {
    public static ProfileResponse from(User user) {
        UserProfile profile = user.getProfile();
        return new ProfileResponse(
            user.getUuid(),
            user.getEmail(),
            profile.getNickname(),
            profile.getRealName(),
            profile.getUsername(),
            profile.getBio(),
            profile.getPhoneNumber(),
            profile.getProfileImageUrl(),
            profile.getWebsite(),
            profile.isMarketingAgree(),
            user.isSocialUser()
        );
    }
}
```

### UserProfileResponse (공개 프로필)

```java
public record UserProfileResponse(
    String uuid,
    String username,
    String nickname,
    String bio,
    String profileImageUrl,
    String website,
    int followerCount,
    int followingCount
) {
    public static UserProfileResponse from(User user, int followerCount, int followingCount) {
        UserProfile profile = user.getProfile();
        return new UserProfileResponse(
            user.getUuid(),
            profile.getUsername(),
            profile.getNickname(),
            profile.getBio(),
            profile.getProfileImageUrl(),
            profile.getWebsite(),
            followerCount,
            followingCount
        );
    }
}
```

### UpdateProfileRequest

```java
public record UpdateProfileRequest(
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    String nickname,

    @Size(max = 50, message = "실명은 50자 이하여야 합니다")
    String realName,

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    String phoneNumber,

    @Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다")
    String profileImageUrl,

    Boolean marketingAgree
) {}
```

## Controller 구현

### ProfileController

```java
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 내 프로필 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile(userId)));
    }

    /**
     * 프로필 수정
     */
    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            profileService.updateProfile(userId, request)));
    }

    /**
     * 비밀번호 변경
     */
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        profileService.changePassword(userId, request);
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("message", "비밀번호가 변경되었습니다")));
    }

    /**
     * 회원 탈퇴
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        profileService.deleteAccount(userId, request);
        return ResponseEntity.ok(
            ApiResponse.success(Map.of("message", "회원 탈퇴가 요청되었습니다")));
    }
}
```

### UserController

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile(userId)));
    }

    /**
     * Username으로 프로필 조회 (공개)
     */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileByUsername(
            @PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(
            userService.getProfileByUsername(username)));
    }

    /**
     * Username 설정
     */
    @PostMapping("/username")
    public ResponseEntity<ApiResponse<UserProfileResponse>> setUsername(
            Authentication authentication,
            @Valid @RequestBody UsernameSetRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            userService.setUsername(userId, request.username())));
    }

    /**
     * Username 중복 확인
     */
    @GetMapping("/username/check")
    public ResponseEntity<ApiResponse<UsernameCheckResponse>> checkUsername(
            @RequestParam String username) {
        boolean available = userService.checkUsernameAvailability(username);
        return ResponseEntity.ok(ApiResponse.success(new UsernameCheckResponse(available)));
    }
}
```

## Frontend 통합

### 프로필 조회 (Vue)

```vue
<template>
  <div class="profile-page">
    <div class="profile-header">
      <img :src="profile.profileImageUrl || defaultAvatar" class="avatar" />
      <div class="profile-info">
        <h1>{{ profile.nickname }}</h1>
        <p v-if="profile.username">@{{ profile.username }}</p>
        <p class="bio">{{ profile.bio }}</p>
      </div>
    </div>

    <div class="stats">
      <span>팔로워 {{ profile.followerCount }}</span>
      <span>팔로잉 {{ profile.followingCount }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '@/lib/api';

interface UserProfile {
  uuid: string;
  username: string;
  nickname: string;
  bio: string;
  profileImageUrl: string;
  followerCount: number;
  followingCount: number;
}

const profile = ref<UserProfile | null>(null);

onMounted(async () => {
  const { data } = await api.get('/api/users/me');
  profile.value = data.data;
});
</script>
```

### 프로필 수정 폼

```vue
<template>
  <form @submit.prevent="handleSubmit">
    <div class="form-group">
      <label>닉네임</label>
      <input v-model="form.nickname" maxlength="50" />
    </div>

    <div class="form-group">
      <label>자기소개</label>
      <textarea v-model="form.bio" maxlength="200" />
    </div>

    <div class="form-group">
      <label>프로필 이미지 URL</label>
      <input v-model="form.profileImageUrl" />
    </div>

    <div class="form-group">
      <label>웹사이트</label>
      <input v-model="form.website" placeholder="https://" />
    </div>

    <button type="submit">저장</button>
  </form>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { api } from '@/lib/api';

const form = ref({
  nickname: '',
  bio: '',
  profileImageUrl: '',
  website: ''
});

async function handleSubmit() {
  try {
    await api.patch('/api/profile', form.value);
    alert('프로필이 수정되었습니다.');
  } catch (error) {
    alert('프로필 수정에 실패했습니다.');
  }
}
</script>
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/ProfileService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/service/UserService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/ProfileController.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/controller/UserController.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/domain/UserProfile.java`

## 참고 자료

- [Spring Data JPA Entity Lifecycle](https://docs.spring.io/spring-data/jpa/reference/)
- [Partial Updates with PATCH](https://datatracker.ietf.org/doc/html/rfc5789)
