# MongoDB Counter Pattern

## 개요

Blog Service에서 조회수, 좋아요 수, 댓글 수 등 카운터를 관리하는 패턴을 학습합니다.

## 카운터 필드 설계

### Post Document

```java
@Document(collection = "posts")
public class Post {
    // 카운터 필드 (역정규화)
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;
}
```

## 패턴 1: 애플리케이션 레벨 증가

### 구현

```java
// Post.java
public void incrementViewCount() {
    this.viewCount++;
}

public void incrementLikeCount() {
    this.likeCount++;
}

public void decrementLikeCount() {
    if (this.likeCount > 0) {
        this.likeCount--;
    }
}
```

### Service에서 사용

```java
@Transactional
public PostResponse getPostByIdWithViewIncrement(String postId, String userId) {
    Post post = postRepository.findById(postId).orElseThrow();

    // 조회수 증가
    post.incrementViewCount();
    postRepository.save(post);

    return convertToPostResponse(post);
}
```

**장점:**
- 구현 간단
- 비즈니스 로직 제어 가능

**단점:**
- 동시성 문제 가능 (Lost Update)

## 패턴 2: MongoDB 원자적 연산 ($inc)

### Repository 메서드

```java
public interface PostRepository {

    // 원자적 조회수 증가
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'viewCount': 1 } }")
    void incrementViewCount(String postId);

    // 원자적 좋아요 증가/감소
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'likeCount': ?1 } }")
    void updateLikeCount(String postId, int delta);

    // 원자적 댓글 수 증가/감소
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'commentCount': ?1 } }")
    void updateCommentCount(String postId, int delta);
}
```

### MongoDB 쿼리

```javascript
// $inc 연산자: 원자적 증가
db.posts.updateOne(
    { _id: ObjectId("...") },
    { $inc: { viewCount: 1 } }
)

// 동시에 여러 카운터 업데이트
db.posts.updateOne(
    { _id: ObjectId("...") },
    { $inc: { viewCount: 1, likeCount: -1 } }
)
```

**장점:**
- 원자적 연산 보장
- 동시성 문제 없음
- 네트워크 왕복 1회

**단점:**
- 업데이트된 값 즉시 확인 불가 (별도 조회 필요)

## 패턴 3: findAndModify (조회 + 업데이트 동시)

### MongoTemplate 사용

```java
@Repository
public class PostRepositoryCustomImpl {

    private final MongoTemplate mongoTemplate;

    public Post incrementViewCountAndGet(String postId) {
        Query query = Query.query(Criteria.where("id").is(postId));
        Update update = new Update().inc("viewCount", 1);

        return mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),  // 업데이트 후 값 반환
            Post.class
        );
    }
}
```

**장점:**
- 원자적 연산
- 업데이트된 값 즉시 반환

## 좋아요 카운터 동기화

### LikeService 구현

```java
@Transactional
public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
    Post post = postRepository.findById(postId).orElseThrow();
    Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

    boolean liked;
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

    postRepository.save(post);
    return LikeToggleResponse.of(liked, post.getLikeCount());
}
```

## 댓글 카운터 동기화

### CommentService 구현

```java
private void updatePostCommentCount(String postId, boolean increment) {
    postRepository.findById(postId).ifPresent(post -> {
        if (increment) {
            post.incrementCommentCount();
        } else {
            post.decrementCommentCount();
        }
        postRepository.save(post);
    });
}

public CommentResponse createComment(CommentCreateRequest request, String authorId) {
    Comment comment = Comment.builder()
        .postId(request.postId())
        .content(request.content())
        .authorId(authorId)
        .build();

    commentRepository.save(comment);

    // 카운터 동기화
    updatePostCommentCount(request.postId(), true);

    return toResponse(comment);
}

public void deleteComment(String commentId, String authorId) {
    Comment comment = commentRepository.findById(commentId).orElseThrow();
    comment.delete();  // Soft delete
    commentRepository.save(comment);

    // 카운터 동기화
    updatePostCommentCount(comment.getPostId(), false);
}
```

## 카운터 불일치 복구

### 정기 동기화 배치

```java
@Scheduled(cron = "0 0 3 * * ?")  // 매일 새벽 3시
public void syncCounters() {
    List<Post> posts = postRepository.findAll();

    for (Post post : posts) {
        // 실제 좋아요 수 계산
        long actualLikeCount = likeRepository.countByPostId(post.getId());

        // 실제 댓글 수 계산 (삭제되지 않은 것만)
        long actualCommentCount = commentRepository
            .countByPostIdAndIsDeletedFalse(post.getId());

        boolean needsUpdate = false;

        if (!post.getLikeCount().equals(actualLikeCount)) {
            post.setLikeCount(actualLikeCount);
            needsUpdate = true;
        }

        if (!post.getCommentCount().equals(actualCommentCount)) {
            post.setCommentCount(actualCommentCount);
            needsUpdate = true;
        }

        if (needsUpdate) {
            postRepository.save(post);
            log.info("Synced counters for post {}", post.getId());
        }
    }
}
```

### Aggregation으로 동기화

```java
public void syncLikeCountsWithAggregation() {
    // Like 컬렉션에서 postId별 count 집계
    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.group("postId").count().as("count")
    );

    AggregationResults<LikeCountResult> results = mongoTemplate.aggregate(
        aggregation, Like.class, LikeCountResult.class
    );

    // 각 Post 업데이트
    results.getMappedResults().forEach(result -> {
        Query query = Query.query(Criteria.where("id").is(result.postId()));
        Update update = Update.update("likeCount", result.count());
        mongoTemplate.updateFirst(query, update, Post.class);
    });
}
```

## 조회수 중복 방지 (선택적)

### Redis 활용

```java
@Service
public class ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String VIEW_KEY_PREFIX = "post:view:";

    public boolean shouldIncrementViewCount(String postId, String userId) {
        String key = VIEW_KEY_PREFIX + postId + ":" + userId;

        // 24시간 내 조회 여부 확인
        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(key, "1", Duration.ofHours(24));

        return Boolean.TRUE.equals(isNew);
    }
}
```

## 핵심 포인트

| 패턴 | 동시성 | 성능 | 복잡도 |
|------|--------|------|--------|
| 애플리케이션 레벨 | 문제 가능 | 중간 | 낮음 |
| $inc 원자적 연산 | 안전 | 높음 | 낮음 |
| findAndModify | 안전 | 높음 | 중간 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/service/LikeService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/service/CommentService.java`
