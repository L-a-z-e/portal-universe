# Follow System (팔로우/팔로워)

## 개요

Portal Universe auth-service는 사용자 간 팔로우 관계를 관리합니다. 팔로우/언팔로우 토글, 팔로워/팔로잉 목록 조회, 팔로우 상태 확인 등의 기능을 제공하며, blog-service의 피드 기능과 연동됩니다.

## 데이터 모델

### Follow Entity

```java
package com.portal.universe.authservice.follow.domain;

/**
 * 사용자 간의 팔로우 관계를 나타내는 엔티티입니다.
 * follower가 following을 팔로우합니다.
 */
@Entity
@Table(name = "follows",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_follow_relationship",
        columnNames = {"follower_id", "following_id"}
    ),
    indexes = {
        @Index(name = "idx_follower_id", columnList = "follower_id"),
        @Index(name = "idx_following_id", columnList = "following_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;  // 팔로우 하는 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following; // 팔로우 당하는 사람

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }
}
```

### 관계 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                    Follow Relationship                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User A (follower)  ──────────▶  User B (following)         │
│                                                              │
│  "A가 B를 팔로우한다"                                         │
│  - A의 팔로잉 목록에 B가 표시                                  │
│  - B의 팔로워 목록에 A가 표시                                  │
│                                                              │
│  ┌──────────┐                      ┌──────────┐             │
│  │  User A  │ ─────follows────▶    │  User B  │             │
│  │          │                      │          │             │
│  │ followings: [B, C]              │ followers: [A, D]      │
│  │ followers: [D]                  │ followings: [E]        │
│  └──────────┘                      └──────────┘             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## FollowRepository

```java
package com.portal.universe.authservice.follow.repository;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 팔로우 관계 존재 여부
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 특정 팔로우 관계 조회
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 팔로워 수 조회 (나를 팔로우하는 사람 수)
    long countByFollowing(User following);

    // 팔로잉 수 조회 (내가 팔로우하는 사람 수)
    long countByFollower(User follower);

    // 팔로워 목록 조회 (페이징)
    @Query("SELECT f.follower FROM Follow f WHERE f.following = :user")
    Page<User> findFollowersByUser(@Param("user") User user, Pageable pageable);

    // 팔로잉 목록 조회 (페이징)
    @Query("SELECT f.following FROM Follow f WHERE f.follower = :user")
    Page<User> findFollowingsByUser(@Param("user") User user, Pageable pageable);

    // 내가 팔로우하는 사용자들의 UUID 목록 (피드용)
    @Query("SELECT f.following.uuid FROM Follow f WHERE f.follower.id = :followerId")
    List<String> findFollowingUuidsByFollowerId(@Param("followerId") Long followerId);
}
```

## FollowService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 팔로우 토글 (팔로우/언팔로우)
     * 이미 팔로우 중이면 언팔로우, 아니면 팔로우
     */
    @Transactional
    public FollowResponse toggleFollow(Long currentUserId, String targetUsername) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        // 자기 자신 팔로우 방지
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new CustomBusinessException(AuthErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        boolean isFollowing;
        if (followRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
            // 이미 팔로우 중이면 언팔로우
            Follow follow = followRepository.findByFollowerAndFollowing(currentUser, targetUser)
                    .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.NOT_FOLLOWING));
            followRepository.delete(follow);
            isFollowing = false;
        } else {
            // 팔로우
            Follow follow = new Follow(currentUser, targetUser);
            followRepository.save(follow);
            isFollowing = true;
        }

        return new FollowResponse(
                isFollowing,
                getFollowerCount(targetUser),
                getFollowingCount(targetUser)
        );
    }

    /**
     * 팔로워 목록 조회
     */
    public FollowListResponse getFollowers(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followers = followRepository.findFollowersByUser(user, pageable);
        Page<FollowUserResponse> followUserResponses = followers.map(FollowUserResponse::from);

        return FollowListResponse.from(followUserResponses);
    }

    /**
     * 팔로잉 목록 조회
     */
    public FollowListResponse getFollowings(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<User> followings = followRepository.findFollowingsByUser(user, pageable);
        Page<FollowUserResponse> followUserResponses = followings.map(FollowUserResponse::from);

        return FollowListResponse.from(followUserResponses);
    }

    /**
     * 내가 팔로우하는 사용자들의 UUID 목록 조회
     * blog-service 피드 API에서 사용
     */
    public FollowingIdsResponse getMyFollowingIds(Long currentUserId) {
        List<String> followingIds = followRepository.findFollowingUuidsByFollowerId(currentUserId);
        return new FollowingIdsResponse(followingIds);
    }

    /**
     * 팔로우 상태 확인
     */
    public FollowStatusResponse getFollowStatus(Long currentUserId, String targetUsername) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.USER_NOT_FOUND));

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new CustomBusinessException(AuthErrorCode.FOLLOW_USER_NOT_FOUND));

        boolean isFollowing = followRepository.existsByFollowerAndFollowing(currentUser, targetUser);
        return new FollowStatusResponse(isFollowing);
    }

    public int getFollowerCount(User user) {
        return (int) followRepository.countByFollowing(user);
    }

    public int getFollowingCount(User user) {
        return (int) followRepository.countByFollower(user);
    }
}
```

## DTO 정의

### FollowResponse

```java
public record FollowResponse(
    boolean following,
    int followerCount,
    int followingCount
) {}
```

### FollowUserResponse

```java
public record FollowUserResponse(
    String uuid,
    String username,
    String nickname,
    String profileImageUrl
) {
    public static FollowUserResponse from(User user) {
        return new FollowUserResponse(
            user.getUuid(),
            user.getProfile().getUsername(),
            user.getProfile().getNickname(),
            user.getProfile().getProfileImageUrl()
        );
    }
}
```

### FollowListResponse

```java
public record FollowListResponse(
    List<FollowUserResponse> users,
    int totalPages,
    long totalElements,
    int currentPage,
    boolean hasNext
) {
    public static FollowListResponse from(Page<FollowUserResponse> page) {
        return new FollowListResponse(
            page.getContent(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
```

### FollowingIdsResponse

```java
public record FollowingIdsResponse(List<String> followingIds) {}
```

### FollowStatusResponse

```java
public record FollowStatusResponse(boolean following) {}
```

## FollowController

```java
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 팔로우/언팔로우 토글
     */
    @PostMapping("/{username}")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleFollow(
            Authentication authentication,
            @PathVariable String username) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            followService.toggleFollow(userId, username)));
    }

    /**
     * 팔로워 목록 조회
     */
    @GetMapping("/{username}/followers")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowers(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            followService.getFollowers(username, page, size)));
    }

    /**
     * 팔로잉 목록 조회
     */
    @GetMapping("/{username}/followings")
    public ResponseEntity<ApiResponse<FollowListResponse>> getFollowings(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
            followService.getFollowings(username, page, size)));
    }

    /**
     * 내가 팔로우하는 사용자 UUID 목록 (피드용)
     */
    @GetMapping("/me/following-ids")
    public ResponseEntity<ApiResponse<FollowingIdsResponse>> getMyFollowingIds(
            Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            followService.getMyFollowingIds(userId)));
    }

    /**
     * 팔로우 상태 확인
     */
    @GetMapping("/status/{username}")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            Authentication authentication,
            @PathVariable String username) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            followService.getFollowStatus(userId, username)));
    }
}
```

## blog-service 연동

### 피드 조회 시 팔로잉 목록 활용

```java
// blog-service의 FeedService
@Service
public class FeedService {

    private final AuthServiceClient authClient;
    private final PostRepository postRepository;

    public List<PostResponse> getFollowingFeed(String userUuid, int page, int size) {
        // 1. auth-service에서 팔로잉 목록 조회
        FollowingIdsResponse followingIds = authClient.getFollowingIds(userUuid);

        // 2. 팔로잉 사용자들의 게시글 조회
        List<String> authorIds = followingIds.followingIds();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return postRepository.findByAuthorIdIn(authorIds, pageable)
            .map(PostResponse::from)
            .toList();
    }
}
```

## Frontend 통합

### 팔로우 버튼 컴포넌트 (Vue)

```vue
<template>
  <button
    @click="handleFollow"
    :class="['follow-btn', { following: isFollowing }]"
    :disabled="loading"
  >
    {{ isFollowing ? '팔로잉' : '팔로우' }}
  </button>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { api } from '@/lib/api';

const props = defineProps<{
  username: string;
}>();

const emit = defineEmits<{
  (e: 'update', followerCount: number): void;
}>();

const isFollowing = ref(false);
const loading = ref(false);

onMounted(async () => {
  const { data } = await api.get(`/api/follow/status/${props.username}`);
  isFollowing.value = data.data.following;
});

async function handleFollow() {
  loading.value = true;
  try {
    const { data } = await api.post(`/api/follow/${props.username}`);
    isFollowing.value = data.data.following;
    emit('update', data.data.followerCount);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.follow-btn {
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.2s;
}

.follow-btn:not(.following) {
  background: #1da1f2;
  color: white;
}

.follow-btn.following {
  background: transparent;
  border: 1px solid #1da1f2;
  color: #1da1f2;
}

.follow-btn.following:hover {
  border-color: #e0245e;
  color: #e0245e;
}
</style>
```

### 팔로워/팔로잉 목록

```vue
<template>
  <div class="follow-list">
    <div v-for="user in users" :key="user.uuid" class="user-item">
      <img :src="user.profileImageUrl || defaultAvatar" class="avatar" />
      <div class="user-info">
        <router-link :to="`/@${user.username}`">
          <span class="nickname">{{ user.nickname }}</span>
          <span class="username">@{{ user.username }}</span>
        </router-link>
      </div>
      <FollowButton :username="user.username" v-if="!isMe(user)" />
    </div>

    <button v-if="hasNext" @click="loadMore" class="load-more">
      더 보기
    </button>
  </div>
</template>
```

## 에러 코드

```java
public enum AuthErrorCode implements ErrorCode {
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "A014", "Already following this user"),
    NOT_FOLLOWING(HttpStatus.NOT_FOUND, "A015", "Not following this user"),
    CANNOT_FOLLOW_YOURSELF(HttpStatus.BAD_REQUEST, "A016", "Cannot follow yourself"),
    FOLLOW_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A017", "Target user not found"),
}
```

## 관련 파일

- `/services/auth-service/src/main/java/com/portal/universe/authservice/follow/domain/Follow.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/follow/service/FollowService.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/follow/controller/FollowController.java`
- `/services/auth-service/src/main/java/com/portal/universe/authservice/follow/repository/FollowRepository.java`

## 참고 자료

- [Twitter-like Following System Design](https://www.linkedin.com/advice/0/how-do-you-design-twitter-like-social-network)
- [Spring Data JPA Pagination](https://docs.spring.io/spring-data/jpa/reference/repositories/query-by-example.html)
