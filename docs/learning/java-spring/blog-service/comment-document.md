# Comment Document - 대댓글 구조

## 개요

블로그 댓글 시스템의 대댓글(nested comment) 구조와 설계 전략을 학습합니다.

## Document Schema

```java
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @Indexed
    @NotBlank(message = "게시물 ID는 필수입니다")
    private String postId;          // 소속 게시물

    @Indexed
    @NotBlank(message = "작성자 ID는 필수입니다")
    private String authorId;        // 작성자 ID
    private String authorName;      // 작성자 이름 (비정규화)

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;         // 댓글 내용

    // 대댓글 구조 핵심
    private String parentCommentId; // 부모 댓글 ID (null이면 루트)

    @Builder.Default
    private Long likeCount = 0L;    // 좋아요 수

    @Builder.Default
    private Boolean isDeleted = false; // Soft Delete

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 대댓글 설계 패턴

### Adjacency List 패턴 (현재 사용)

```
parentCommentId 필드로 부모-자식 관계 표현

Root Comment (parentCommentId = null)
├── Reply 1 (parentCommentId = "root_id")
│   └── Reply 1-1 (parentCommentId = "reply1_id")
└── Reply 2 (parentCommentId = "root_id")
```

**장점:**
- 단순한 구조
- 댓글 추가/삭제가 쉬움
- 무한 깊이 지원 가능

**단점:**
- 트리 전체 조회 시 여러 번 쿼리 또는 클라이언트 조립 필요

### 코드 구현

```java
// 루트 댓글인지 확인
public boolean isRootComment() {
    return parentCommentId == null;
}
```

## Repository 쿼리

```java
public interface CommentRepository extends MongoRepository<Comment, String> {

    // 게시물의 모든 댓글 조회 (삭제되지 않은 것만, 생성순)
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);

    // 특정 부모의 대댓글 조회
    List<Comment> findByParentCommentIdAndIsDeletedFalse(String parentCommentId);

    // 게시물의 루트 댓글만 조회
    List<Comment> findByPostIdAndParentCommentIdIsNullAndIsDeletedFalse(String postId);
}
```

## 트리 구조 조립 (클라이언트 측)

```javascript
// Frontend에서 트리 구조 조립
function buildCommentTree(comments) {
    const map = new Map();
    const roots = [];

    // 1. 모든 댓글을 Map에 저장
    comments.forEach(comment => {
        map.set(comment.id, { ...comment, replies: [] });
    });

    // 2. 부모-자식 관계 연결
    comments.forEach(comment => {
        const node = map.get(comment.id);
        if (comment.parentCommentId) {
            const parent = map.get(comment.parentCommentId);
            if (parent) {
                parent.replies.push(node);
            }
        } else {
            roots.push(node);
        }
    });

    return roots;
}
```

## Soft Delete

```java
// 삭제 시 실제 삭제 대신 플래그 변경
public void delete() {
    this.isDeleted = true;
}
```

**Soft Delete를 사용하는 이유:**
1. 대댓글이 있는 댓글 삭제 시 구조 유지
2. "삭제된 댓글입니다" 표시 가능
3. 감사 로그 유지
4. 실수로 삭제된 댓글 복구 가능

## 댓글 수 동기화

```java
// CommentService에서 Post의 commentCount 동기화
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
```

## API Response 구조

```java
public record CommentResponse(
    String id,
    String postId,
    String authorId,
    String authorName,
    String content,
    String parentCommentId,
    Long likeCount,
    Boolean isDeleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

## 대안 패턴 비교

| 패턴 | 장점 | 단점 | 사용 시나리오 |
|------|------|------|--------------|
| Adjacency List | 단순함, 수정 용이 | 트리 조회 복잡 | 일반적인 댓글 시스템 |
| Materialized Path | 트리 조회 빠름 | 경로 업데이트 복잡 | 계층이 깊은 경우 |
| Nested Set | 트리 조회 최적 | 삽입/삭제 비용 높음 | 읽기 위주 시스템 |

## 깊이 제한 고려사항

```java
// 대댓글 깊이 제한 (선택적 구현)
public static final int MAX_DEPTH = 3;

public boolean canAddReply(int currentDepth) {
    return currentDepth < MAX_DEPTH;
}
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 구조 | Adjacency List (parentCommentId) |
| 삭제 | Soft Delete (isDeleted 플래그) |
| 조회 | 플랫 리스트 → 클라이언트 조립 |
| 동기화 | Post.commentCount 역정규화 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/domain/Comment.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/service/CommentService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/repository/CommentRepository.java`
