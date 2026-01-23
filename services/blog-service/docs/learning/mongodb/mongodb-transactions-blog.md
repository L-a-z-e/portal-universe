# MongoDB Transactions in Blog Service

## 개요

MongoDB 트랜잭션의 개념과 Blog Service에서의 활용을 학습합니다.

## MongoDB 트랜잭션 기본

### 요구사항

- MongoDB 4.0+ (Replica Set) 또는 4.2+ (Sharded Cluster)
- WiredTiger 스토리지 엔진

### 트랜잭션 특성

| ACID | MongoDB |
|------|---------|
| Atomicity | 단일 문서: 항상 보장, 다중 문서: 트랜잭션 필요 |
| Consistency | 스키마 검증, 유니크 인덱스 |
| Isolation | 기본: Read Concern, 트랜잭션: Snapshot |
| Durability | Write Concern으로 제어 |

## Spring Data MongoDB 트랜잭션

### 설정

```java
@Configuration
public class MongoTransactionConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
```

### @Transactional 사용

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 기본: 읽기 전용
public class PostServiceImpl implements PostService {

    @Override
    @Transactional  // 쓰기 트랜잭션
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        Post post = Post.builder()
            .title(request.title())
            .content(request.content())
            .authorId(authorId)
            .build();

        Post savedPost = postRepository.save(post);

        // 태그 카운트 업데이트 (같은 트랜잭션)
        if (request.tags() != null) {
            for (String tagName : request.tags()) {
                Tag tag = tagService.getOrCreateTag(tagName);
                tag.incrementPostCount();
                tagRepository.save(tag);
            }
        }

        return convertToPostResponse(savedPost);
    }
}
```

## 트랜잭션이 필요한 시나리오

### 1. 좋아요 토글 (Like + Post)

```java
@Transactional
public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
    // 1. Post 조회
    Post post = postRepository.findById(postId).orElseThrow();

    // 2. Like 확인 및 추가/삭제
    Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

    if (existingLike != null) {
        likeRepository.delete(existingLike);  // Like 삭제
        post.decrementLikeCount();            // Post 카운터 감소
    } else {
        likeRepository.save(Like.builder()
            .postId(postId)
            .userId(userId)
            .build());
        post.incrementLikeCount();            // Post 카운터 증가
    }

    // 3. Post 저장
    postRepository.save(post);

    // 모든 작업이 성공해야 커밋, 실패 시 롤백
    return LikeToggleResponse.of(existingLike == null, post.getLikeCount());
}
```

### 2. 댓글 생성 (Comment + Post)

```java
@Transactional
public CommentResponse createComment(CommentCreateRequest request, String authorId) {
    // 1. 댓글 저장
    Comment comment = Comment.builder()
        .postId(request.postId())
        .content(request.content())
        .authorId(authorId)
        .build();
    commentRepository.save(comment);

    // 2. Post 댓글 수 증가
    Post post = postRepository.findById(request.postId()).orElseThrow();
    post.incrementCommentCount();
    postRepository.save(post);

    return toResponse(comment);
}
```

### 3. 게시물 삭제 (Post + 관련 데이터)

```java
@Transactional
public void deletePost(String postId, String userId) {
    Post post = postRepository.findById(postId).orElseThrow();

    if (!post.getAuthorId().equals(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_DELETE_FORBIDDEN);
    }

    // 1. 관련 좋아요 삭제
    likeRepository.deleteByPostId(postId);

    // 2. 관련 댓글 삭제
    commentRepository.deleteByPostId(postId);

    // 3. 태그 카운트 감소
    for (String tagName : post.getTags()) {
        tagService.decrementTagPostCount(tagName);
    }

    // 4. 게시물 삭제
    postRepository.delete(post);
}
```

## 트랜잭션 없이도 되는 경우

### 단일 문서 작업

```java
// 트랜잭션 불필요 - 단일 문서 원자적 업데이트
@Override
public PostResponse updatePost(String postId, PostUpdateRequest request, String userId) {
    Post post = postRepository.findById(postId).orElseThrow();
    post.update(request.title(), request.content(), ...);
    return convertToPostResponse(postRepository.save(post));
}
```

### 조회 전용

```java
@Transactional(readOnly = true)  // 읽기 전용
public PostResponse getPostById(String postId) {
    Post post = postRepository.findById(postId).orElseThrow();
    return convertToPostResponse(post);
}
```

## 트랜잭션 고려사항

### 성능 영향

```
트랜잭션 사용 시:
- 락 획득/해제 오버헤드
- 스냅샷 관리 비용
- 롤백 가능성을 위한 추가 메모리

권장:
- 필요한 경우에만 사용
- 트랜잭션 범위 최소화
- 장시간 트랜잭션 피하기
```

### 타임아웃 설정

```java
@Transactional(timeout = 30)  // 30초 타임아웃
public void longRunningOperation() {
    // ...
}
```

### 롤백 규칙

```java
// RuntimeException만 롤백 (기본)
@Transactional
public void defaultRollback() { ... }

// 특정 예외에 롤백
@Transactional(rollbackFor = CustomException.class)
public void customRollback() { ... }

// 특정 예외에 롤백 안 함
@Transactional(noRollbackFor = WarningException.class)
public void noRollback() { ... }
```

## 대안: 최종 일관성

### 이벤트 기반 동기화

```java
// 트랜잭션 대신 이벤트 발행
@Transactional
public LikeToggleResponse toggleLike(String postId, String userId) {
    // Like 저장/삭제만 트랜잭션
    Like like = ...
    likeRepository.save(like);

    // 이벤트 발행 (비동기 처리)
    eventPublisher.publish(new LikeAddedEvent(postId, userId));

    return ...;
}

// 이벤트 리스너에서 카운터 업데이트
@EventListener
public void handleLikeAdded(LikeAddedEvent event) {
    postRepository.incrementLikeCount(event.getPostId());
}
```

### 정기 동기화 배치

```java
@Scheduled(cron = "0 0 3 * * ?")
public void syncLikeCounts() {
    // 실제 Like 수와 Post.likeCount 동기화
}
```

## Blog Service 트랜잭션 정리

| 작업 | 트랜잭션 필요 | 이유 |
|------|-------------|------|
| Post CRUD | 선택적 | 단일 문서 |
| Like 토글 | 필요 | Like + Post 동시 수정 |
| 댓글 생성/삭제 | 필요 | Comment + Post 동시 수정 |
| Post 삭제 | 필요 | 관련 데이터 함께 삭제 |
| 조회 | 불필요 | 읽기 전용 |

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 단일 문서 | 항상 원자적 (트랜잭션 불필요) |
| 다중 문서 | 트랜잭션 필요 |
| 성능 | 필요한 경우에만 사용 |
| 대안 | 이벤트 기반, 배치 동기화 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/service/LikeService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/service/CommentService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/service/PostServiceImpl.java`
