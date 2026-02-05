# Like System

## 개요

Blog Service의 좋아요 시스템 구현을 학습합니다. 토글 방식의 좋아요와 카운터 동기화를 포함합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/posts/{postId}/like` | 좋아요 토글 |
| GET | `/api/v1/posts/{postId}/like/status` | 좋아요 상태 확인 |
| GET | `/api/v1/posts/{postId}/like/likers` | 좋아요한 사용자 목록 |

## 좋아요 토글

### Service 구현

```java
@Transactional
public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
    // 1. Post 존재 여부 확인
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 2. 기존 좋아요 확인
    boolean liked;
    Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

    if (existingLike != null) {
        // 좋아요 취소
        likeRepository.delete(existingLike);
        post.decrementLikeCount();
        liked = false;
        log.info("Like removed: postId={}, userId={}", postId, userId);
    } else {
        // 좋아요 추가
        Like newLike = Like.builder()
            .postId(postId)
            .userId(userId)
            .userName(userName)
            .build();
        likeRepository.save(newLike);
        post.incrementLikeCount();
        liked = true;
        log.info("Like added: postId={}, userId={}", postId, userId);
    }

    // 3. Post 저장 (likeCount 반영)
    postRepository.save(post);

    return LikeToggleResponse.of(liked, post.getLikeCount());
}
```

### Controller

```java
@PostMapping("/{postId}/like")
public ResponseEntity<ApiResponse<LikeToggleResponse>> toggleLike(
        @PathVariable String postId,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Name") String userName) {

    LikeToggleResponse response = likeService.toggleLike(postId, userId, userName);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

## 좋아요 상태 확인

### Service 구현

```java
public LikeStatusResponse getLikeStatus(String postId, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);

    return LikeStatusResponse.of(liked, post.getLikeCount());
}
```

### Controller

```java
@GetMapping("/{postId}/like/status")
public ResponseEntity<ApiResponse<LikeStatusResponse>> getLikeStatus(
        @PathVariable String postId,
        @RequestHeader("X-User-Id") String userId) {

    LikeStatusResponse response = likeService.getLikeStatus(postId, userId);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

## 좋아요한 사용자 목록

### Service 구현

```java
public Page<LikerResponse> getLikers(String postId, Pageable pageable) {
    postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    return likeRepository.findByPostId(postId, pageable)
        .map(LikerResponse::from);
}
```

### Response DTO

```java
public record LikerResponse(
    String userId,
    String userName,
    LocalDateTime likedAt
) {
    public static LikerResponse from(Like like) {
        return new LikerResponse(
            like.getUserId(),
            like.getUserName(),
            like.getCreatedAt()
        );
    }
}
```

## Response DTOs

```java
// 좋아요 토글 결과
public record LikeToggleResponse(
    boolean liked,      // 현재 좋아요 상태
    Long likeCount      // 총 좋아요 수
) {
    public static LikeToggleResponse of(boolean liked, Long likeCount) {
        return new LikeToggleResponse(liked, likeCount);
    }
}

// 좋아요 상태 조회
public record LikeStatusResponse(
    boolean liked,
    Long likeCount
) {
    public static LikeStatusResponse of(boolean liked, Long likeCount) {
        return new LikeStatusResponse(liked, likeCount);
    }
}
```

## 중복 방지

### Compound Index

```java
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true
)
public class Like {
    @Id
    private String id;

    @Indexed
    private String postId;

    @Indexed
    private String userId;

    private String userName;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

## Repository

```java
public interface LikeRepository extends MongoRepository<Like, String> {

    // 특정 좋아요 조회
    Optional<Like> findByPostIdAndUserId(String postId, String userId);

    // 좋아요 존재 여부 확인
    boolean existsByPostIdAndUserId(String postId, String userId);

    // 포스트의 좋아요 목록 (페이징)
    Page<Like> findByPostId(String postId, Pageable pageable);

    // 좋아요 수 조회
    long countByPostId(String postId);

    // 사용자가 좋아요한 포스트
    List<Like> findByUserId(String userId);

    // 포스트 삭제 시 관련 좋아요 삭제
    void deleteByPostId(String postId);
}
```

## 프론트엔드 구현

### 좋아요 버튼 컴포넌트 (Vue)

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { toggleLike, getLikeStatus } from '@/api/likes'

const props = defineProps<{
  postId: string
}>()

const liked = ref(false)
const likeCount = ref(0)
const loading = ref(false)

onMounted(async () => {
  const status = await getLikeStatus(props.postId)
  liked.value = status.liked
  likeCount.value = status.likeCount
})

async function handleLike() {
  if (loading.value) return

  loading.value = true
  try {
    // Optimistic Update
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1

    const result = await toggleLike(props.postId)
    liked.value = result.liked
    likeCount.value = result.likeCount
  } catch (error) {
    // Rollback on error
    liked.value = !liked.value
    likeCount.value += liked.value ? 1 : -1
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <button
    @click="handleLike"
    :class="['like-button', { liked }]"
    :disabled="loading"
  >
    <HeartIcon :filled="liked" />
    <span>{{ likeCount }}</span>
  </button>
</template>

<style scoped>
.like-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.like-button.liked {
  border-color: #ff4081;
  color: #ff4081;
}

.like-button:hover {
  background: #f5f5f5;
}
</style>
```

### React 버전

```tsx
import { useState, useEffect } from 'react'
import { toggleLike, getLikeStatus } from '@/api/likes'

interface LikeButtonProps {
  postId: string
}

export const LikeButton: React.FC<LikeButtonProps> = ({ postId }) => {
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    getLikeStatus(postId).then(status => {
      setLiked(status.liked)
      setLikeCount(status.likeCount)
    })
  }, [postId])

  const handleLike = async () => {
    if (loading) return

    setLoading(true)
    try {
      // Optimistic Update
      setLiked(!liked)
      setLikeCount(prev => prev + (liked ? -1 : 1))

      const result = await toggleLike(postId)
      setLiked(result.liked)
      setLikeCount(result.likeCount)
    } catch (error) {
      // Rollback
      setLiked(liked)
      setLikeCount(prev => prev + (liked ? 1 : -1))
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      onClick={handleLike}
      className={`like-button ${liked ? 'liked' : ''}`}
      disabled={loading}
    >
      <HeartIcon filled={liked} />
      <span>{likeCount}</span>
    </button>
  )
}
```

## 에러 코드

```java
LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B020", "Like not found"),
LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "B021", "Like already exists"),
LIKE_OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "B022", "Like operation failed");
```

## 핵심 포인트

| 기능 | 핵심 사항 |
|------|----------|
| 토글 | 있으면 삭제, 없으면 추가 |
| 중복 방지 | Compound Index (postId + userId) |
| 카운터 | Post.likeCount 역정규화 |
| UI | Optimistic Update 권장 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/domain/Like.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/service/LikeService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/controller/LikeController.java`
