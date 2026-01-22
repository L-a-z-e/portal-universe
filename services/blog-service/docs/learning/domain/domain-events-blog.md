# Blog Domain Events

## 개요

Blog Service에서 발생하는 도메인 이벤트와 Kafka를 통한 이벤트 발행 패턴을 학습합니다.

## 이벤트 목록

| 이벤트 | 발생 시점 | 구독자 |
|--------|----------|--------|
| PostCreated | 포스트 생성 | Notification, Search |
| PostPublished | 포스트 발행 | Notification, Search, Feed |
| PostUpdated | 포스트 수정 | Search |
| PostDeleted | 포스트 삭제 | Search, Feed |
| CommentCreated | 댓글 작성 | Notification |
| LikeAdded | 좋아요 추가 | Notification |

## Kafka 설정

### application.yml

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    bootstrap-servers: localhost:9092
```

## 이벤트 정의

### PostCreatedEvent

```java
public record PostCreatedEvent(
    String eventId,
    String postId,
    String authorId,
    String authorName,
    String title,
    PostStatus status,
    LocalDateTime createdAt,
    LocalDateTime eventTime
) {
    public static PostCreatedEvent from(Post post) {
        return new PostCreatedEvent(
            UUID.randomUUID().toString(),
            post.getId(),
            post.getAuthorId(),
            post.getAuthorName(),
            post.getTitle(),
            post.getStatus(),
            post.getCreatedAt(),
            LocalDateTime.now()
        );
    }
}
```

### PostPublishedEvent

```java
public record PostPublishedEvent(
    String eventId,
    String postId,
    String authorId,
    String title,
    String summary,
    Set<String> tags,
    String category,
    LocalDateTime publishedAt,
    LocalDateTime eventTime
) {
    public static PostPublishedEvent from(Post post) {
        return new PostPublishedEvent(
            UUID.randomUUID().toString(),
            post.getId(),
            post.getAuthorId(),
            post.getTitle(),
            post.getSummary(),
            post.getTags(),
            post.getCategory(),
            post.getPublishedAt(),
            LocalDateTime.now()
        );
    }
}
```

### CommentCreatedEvent

```java
public record CommentCreatedEvent(
    String eventId,
    String commentId,
    String postId,
    String authorId,
    String authorName,
    String content,
    String parentCommentId,
    LocalDateTime createdAt,
    LocalDateTime eventTime
) {
    public static CommentCreatedEvent from(Comment comment) {
        return new CommentCreatedEvent(
            UUID.randomUUID().toString(),
            comment.getId(),
            comment.getPostId(),
            comment.getAuthorId(),
            comment.getAuthorName(),
            comment.getContent(),
            comment.getParentCommentId(),
            comment.getCreatedAt(),
            LocalDateTime.now()
        );
    }
}
```

### LikeAddedEvent

```java
public record LikeAddedEvent(
    String eventId,
    String likeId,
    String postId,
    String userId,
    String userName,
    Long totalLikeCount,
    LocalDateTime eventTime
) {
    public static LikeAddedEvent from(Like like, Long totalCount) {
        return new LikeAddedEvent(
            UUID.randomUUID().toString(),
            like.getId(),
            like.getPostId(),
            like.getUserId(),
            like.getUserName(),
            totalCount,
            LocalDateTime.now()
        );
    }
}
```

## Event Publisher

```java
@Service
@RequiredArgsConstructor
public class BlogEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String POST_EVENTS_TOPIC = "blog.post.events";
    private static final String COMMENT_EVENTS_TOPIC = "blog.comment.events";
    private static final String LIKE_EVENTS_TOPIC = "blog.like.events";

    public void publishPostCreated(Post post) {
        PostCreatedEvent event = PostCreatedEvent.from(post);
        kafkaTemplate.send(POST_EVENTS_TOPIC, post.getId(), event);
        log.info("Published PostCreatedEvent: {}", event.postId());
    }

    public void publishPostPublished(Post post) {
        PostPublishedEvent event = PostPublishedEvent.from(post);
        kafkaTemplate.send(POST_EVENTS_TOPIC, post.getId(), event);
        log.info("Published PostPublishedEvent: {}", event.postId());
    }

    public void publishCommentCreated(Comment comment) {
        CommentCreatedEvent event = CommentCreatedEvent.from(comment);
        kafkaTemplate.send(COMMENT_EVENTS_TOPIC, comment.getPostId(), event);
        log.info("Published CommentCreatedEvent: {}", event.commentId());
    }

    public void publishLikeAdded(Like like, Long totalCount) {
        LikeAddedEvent event = LikeAddedEvent.from(like, totalCount);
        kafkaTemplate.send(LIKE_EVENTS_TOPIC, like.getPostId(), event);
        log.info("Published LikeAddedEvent: {}", event.likeId());
    }
}
```

## Service 통합

```java
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final BlogEventPublisher eventPublisher;

    @Override
    @Transactional
    public PostResponse createPost(PostCreateRequest request, String authorId) {
        Post post = Post.builder()
            // ... 생성 로직
            .build();

        Post savedPost = postRepository.save(post);

        // 이벤트 발행
        eventPublisher.publishPostCreated(savedPost);

        if (savedPost.isPublished()) {
            eventPublisher.publishPostPublished(savedPost);
        }

        return convertToPostResponse(savedPost);
    }

    @Override
    @Transactional
    public PostResponse changePostStatus(String postId, PostStatus newStatus, String userId) {
        Post post = postRepository.findById(postId).orElseThrow();

        if (newStatus == PostStatus.PUBLISHED) {
            post.publish();
            postRepository.save(post);

            // 발행 이벤트
            eventPublisher.publishPostPublished(post);
        }

        return convertToPostResponse(post);
    }
}
```

## Notification Service 수신 예시

```java
@Service
public class NotificationEventListener {

    @KafkaListener(topics = "blog.post.events", groupId = "notification-service")
    public void handlePostEvent(String message) {
        // 이벤트 타입 판별 및 처리
        if (message.contains("PostPublishedEvent")) {
            handlePostPublished(message);
        }
    }

    @KafkaListener(topics = "blog.comment.events", groupId = "notification-service")
    public void handleCommentEvent(CommentCreatedEvent event) {
        // 포스트 작성자에게 댓글 알림 발송
        notifyPostAuthor(event.postId(),
            event.authorName() + "님이 댓글을 작성했습니다.");
    }

    @KafkaListener(topics = "blog.like.events", groupId = "notification-service")
    public void handleLikeEvent(LikeAddedEvent event) {
        // 포스트 작성자에게 좋아요 알림 발송
        notifyPostAuthor(event.postId(),
            event.userName() + "님이 좋아요를 눌렀습니다.");
    }
}
```

## 이벤트 흐름 다이어그램

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Blog Service  │────▶│   Kafka         │────▶│  Notification   │
│   (Producer)    │     │   Broker        │     │   Service       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │
        │                       │               ┌─────────────────┐
        │                       └──────────────▶│  Search Service │
        │                                       │  (Elasticsearch)│
        │                                       └─────────────────┘
        │
        ▼
┌─────────────────┐
│   MongoDB       │
│   (저장)         │
└─────────────────┘
```

## 토픽 설계

| 토픽 | 파티션 키 | 설명 |
|------|----------|------|
| blog.post.events | postId | 포스트 관련 이벤트 |
| blog.comment.events | postId | 댓글 관련 이벤트 |
| blog.like.events | postId | 좋아요 관련 이벤트 |

**파티션 키로 postId 사용:**
- 동일 포스트의 이벤트는 같은 파티션으로
- 순서 보장
- 관련 이벤트 그룹화

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 발행 타이밍 | 트랜잭션 커밋 후 |
| 직렬화 | JSON (JsonSerializer) |
| 파티션 키 | postId (순서 보장) |
| 멱등성 | eventId로 중복 처리 방지 |

## 관련 파일

- `/services/blog-service/src/main/resources/application.yml` (Kafka 설정)
- `/services/common-library/` (공통 이벤트 타입 정의 가능)
