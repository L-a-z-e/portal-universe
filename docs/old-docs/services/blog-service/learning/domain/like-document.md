# Like Document - 좋아요 카운터 패턴

## 개요

블로그 포스트 좋아요 시스템의 카운터 패턴과 중복 방지 전략을 학습합니다.

## Document Schema

```java
@Document(collection = "likes")
@CompoundIndex(name = "postId_userId_unique", def = "{'postId': 1, 'userId': 1}", unique = true)
public class Like {
    @Id
    private String id;

    @Indexed
    @NotBlank(message = "게시물 ID는 필수입니다")
    private String postId;

    @Indexed
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    private String userName;        // 표시용 (비정규화)

    @CreatedDate
    private LocalDateTime createdAt;
}
```

## 중복 방지 전략

### Compound Index (복합 인덱스)

```java
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true
)
```

**효과:**
- (postId, userId) 조합의 유일성 보장
- DB 레벨에서 중복 좋아요 방지
- 별도 검증 로직 없이 안전

## 카운터 패턴: 역정규화

### Post에 likeCount 저장

```java
// Post.java
private Long likeCount = 0L;

public void incrementLikeCount() {
    this.likeCount++;
}

public void decrementLikeCount() {
    if (this.likeCount > 0) {
        this.likeCount--;
    }
}
```

### 동기화 이유

**Like 컬렉션만 사용하면:**
```javascript
// 매 조회마다 집계 필요 (비효율적)
db.likes.countDocuments({ postId: "post123" })
```

**likeCount 역정규화 시:**
```javascript
// 즉시 반환 (효율적)
db.posts.findOne({ _id: "post123" }).likeCount
```

## Service 구현

### Toggle 패턴

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
    }

    // 3. Post 저장 (likeCount 반영)
    postRepository.save(post);

    return LikeToggleResponse.of(liked, post.getLikeCount());
}
```

### 상태 확인

```java
public LikeStatusResponse getLikeStatus(String postId, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);

    return LikeStatusResponse.of(liked, post.getLikeCount());
}
```

### 좋아요한 사용자 목록

```java
public Page<LikerResponse> getLikers(String postId, Pageable pageable) {
    postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    return likeRepository.findByPostId(postId, pageable)
        .map(LikerResponse::from);
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

    // 사용자가 좋아요한 포스트 목록
    List<Like> findByUserId(String userId);
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

// 좋아요한 사용자 정보
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

## 카운터 불일치 해결

### 정기 동기화 (배치)

```java
@Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
public void syncLikeCounts() {
    List<Post> posts = postRepository.findAll();

    for (Post post : posts) {
        long actualCount = likeRepository.countByPostId(post.getId());
        if (!post.getLikeCount().equals(actualCount)) {
            post.setLikeCount(actualCount);
            postRepository.save(post);
            log.info("Synced likeCount for post {}: {} -> {}",
                post.getId(), post.getLikeCount(), actualCount);
        }
    }
}
```

### 실시간 검증 (선택적)

```java
// 조회 시 검증 (성능 비용 있음)
public PostResponse getPostWithVerifiedLikeCount(String postId) {
    Post post = postRepository.findById(postId).orElseThrow();
    long actualCount = likeRepository.countByPostId(postId);

    if (!post.getLikeCount().equals(actualCount)) {
        post.setLikeCount(actualCount);
        postRepository.save(post);
    }

    return convertToResponse(post);
}
```

## 동시성 고려사항

```java
// MongoDB의 원자적 업데이트 사용 (선택적 최적화)
@Query("{ '_id': ?0 }")
@Update("{ '$inc': { 'likeCount': ?1 } }")
void updateLikeCount(String postId, int delta);
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 중복 방지 | Compound Index (postId + userId) |
| 카운터 | Post.likeCount 역정규화 |
| 토글 | 있으면 삭제, 없으면 추가 |
| 동기화 | 배치로 주기적 검증 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/domain/Like.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/service/LikeService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/repository/LikeRepository.java`
