# Comment System

## 개요

Blog Service의 댓글 시스템 구현을 학습합니다. 대댓글(nested comment) 구조를 지원합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/comments` | 댓글 작성 |
| GET | `/api/v1/comments/post/{postId}` | 게시물 댓글 목록 |
| PUT | `/api/v1/comments/{id}` | 댓글 수정 |
| DELETE | `/api/v1/comments/{id}` | 댓글 삭제 |

## 댓글 생성

### Request DTO

```java
public record CommentCreateRequest(
    @NotBlank(message = "게시물 ID는 필수입니다")
    String postId,

    @NotBlank(message = "댓글 내용은 필수입니다")
    String content,

    String parentCommentId  // null이면 루트 댓글, 값이 있으면 대댓글
) {}
```

### Service 구현

```java
@Transactional
public CommentResponse createComment(CommentCreateRequest request, String authorId, String authorName) {
    // 1. 댓글 생성
    Comment comment = Comment.builder()
        .postId(request.postId())
        .parentCommentId(request.parentCommentId())
        .authorId(authorId)
        .authorName(authorName)
        .content(request.content())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    commentRepository.save(comment);

    // 2. 게시물 댓글 수 증가 (트렌딩 점수 계산용)
    updatePostCommentCount(request.postId(), true);

    return toResponse(comment);
}

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

### Controller

```java
@PostMapping
public ResponseEntity<ApiResponse<CommentResponse>> createComment(
        @Valid @RequestBody CommentCreateRequest request,
        @RequestHeader("X-User-Id") String userId,
        @RequestHeader("X-User-Name") String userName) {

    CommentResponse response = commentService.createComment(request, userId, userName);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response));
}
```

## 댓글 목록 조회

### Service 구현

```java
@Transactional(readOnly = true)
public List<CommentResponse> getCommentsByPostId(String postId) {
    List<Comment> comments = commentRepository
        .findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);

    return comments.stream()
        .map(this::toResponse)
        .toList();
}
```

### 트리 구조 조립 (프론트엔드)

```typescript
interface Comment {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId: string | null;
  likeCount: number;
  isDeleted: boolean;
  createdAt: string;
  replies?: Comment[];
}

function buildCommentTree(comments: Comment[]): Comment[] {
  const map = new Map<string, Comment>();
  const roots: Comment[] = [];

  // 1. 모든 댓글을 Map에 저장
  comments.forEach(comment => {
    map.set(comment.id, { ...comment, replies: [] });
  });

  // 2. 부모-자식 관계 연결
  comments.forEach(comment => {
    const node = map.get(comment.id)!;
    if (comment.parentCommentId) {
      const parent = map.get(comment.parentCommentId);
      if (parent) {
        parent.replies!.push(node);
      }
    } else {
      roots.push(node);
    }
  });

  return roots;
}
```

## 댓글 수정

### Request DTO

```java
public record CommentUpdateRequest(
    @NotBlank(message = "댓글 내용은 필수입니다")
    String content
) {}
```

### Service 구현

```java
@Transactional
public CommentResponse updateComment(String commentId, CommentUpdateRequest request, String authorId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

    // 권한 검증
    if (!comment.getAuthorId().equals(authorId)) {
        throw new CustomBusinessException(BlogErrorCode.COMMENT_UPDATE_FORBIDDEN);
    }

    comment.update(request.content());
    commentRepository.save(comment);

    return toResponse(comment);
}
```

## 댓글 삭제 (Soft Delete)

### Service 구현

```java
@Transactional
public void deleteComment(String commentId, String authorId) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.COMMENT_NOT_FOUND));

    // 권한 검증
    if (!comment.getAuthorId().equals(authorId)) {
        throw new CustomBusinessException(BlogErrorCode.COMMENT_DELETE_FORBIDDEN);
    }

    // Soft Delete
    comment.delete();
    commentRepository.save(comment);

    // 게시물 댓글 수 감소
    updatePostCommentCount(comment.getPostId(), false);
}
```

### Domain 메서드

```java
// Comment.java
public void delete() {
    this.isDeleted = true;
}
```

## Response DTO

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

## Repository

```java
public interface CommentRepository extends MongoRepository<Comment, String> {

    // 게시물의 모든 댓글 (삭제되지 않은 것만, 생성순)
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);

    // 특정 부모의 대댓글
    List<Comment> findByParentCommentIdAndIsDeletedFalse(String parentCommentId);

    // 루트 댓글만
    List<Comment> findByPostIdAndParentCommentIdIsNullAndIsDeletedFalse(String postId);

    // 게시물 삭제 시 관련 댓글 삭제
    void deleteByPostId(String postId);

    // 댓글 수 조회 (삭제되지 않은 것만)
    long countByPostIdAndIsDeletedFalse(String postId);
}
```

## 프론트엔드 UI

### 댓글 목록 컴포넌트 (Vue)

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getComments, createComment } from '@/api/comments'

const props = defineProps<{ postId: string }>()

const comments = ref<Comment[]>([])
const newComment = ref('')

onMounted(async () => {
  const data = await getComments(props.postId)
  comments.value = buildCommentTree(data)
})

async function submitComment(parentId?: string) {
  await createComment({
    postId: props.postId,
    content: newComment.value,
    parentCommentId: parentId
  })
  newComment.value = ''
  // 댓글 목록 새로고침
}
</script>

<template>
  <div class="comment-section">
    <!-- 댓글 입력 -->
    <div class="comment-form">
      <textarea v-model="newComment" placeholder="댓글을 작성하세요..."></textarea>
      <button @click="submitComment()">댓글 작성</button>
    </div>

    <!-- 댓글 목록 (재귀 렌더링) -->
    <CommentItem
      v-for="comment in comments"
      :key="comment.id"
      :comment="comment"
      @reply="submitComment"
    />
  </div>
</template>
```

### 댓글 아이템 (재귀)

```vue
<script setup lang="ts">
defineProps<{ comment: Comment }>()
const emit = defineEmits<{ (e: 'reply', parentId: string): void }>()
const showReplyForm = ref(false)
</script>

<template>
  <div class="comment-item" :class="{ 'is-reply': comment.parentCommentId }">
    <div class="comment-header">
      <span class="author">{{ comment.authorName }}</span>
      <span class="date">{{ formatDate(comment.createdAt) }}</span>
    </div>

    <div class="comment-content">
      <template v-if="comment.isDeleted">
        삭제된 댓글입니다.
      </template>
      <template v-else>
        {{ comment.content }}
      </template>
    </div>

    <div class="comment-actions">
      <button @click="showReplyForm = !showReplyForm">답글</button>
    </div>

    <!-- 대댓글 입력 -->
    <div v-if="showReplyForm" class="reply-form">
      <!-- 답글 폼 -->
    </div>

    <!-- 대댓글 목록 (재귀) -->
    <div v-if="comment.replies?.length" class="replies">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        @reply="emit('reply', $event)"
      />
    </div>
  </div>
</template>
```

## 에러 코드

```java
COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "B030", "Comment not found"),
COMMENT_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B031", "You are not allowed to update this comment"),
COMMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B032", "You are not allowed to delete this comment");
```

## 핵심 포인트

| 기능 | 핵심 사항 |
|------|----------|
| 대댓글 | parentCommentId로 구분 |
| 삭제 | Soft Delete (isDeleted 플래그) |
| 트리 | 프론트엔드에서 조립 |
| 동기화 | Post.commentCount 역정규화 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/domain/Comment.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/service/CommentService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/comment/controller/CommentController.java`
