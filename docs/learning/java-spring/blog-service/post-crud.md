# Post CRUD 기능

## 개요

Blog Service의 핵심인 Post(게시물) CRUD 기능 구현을 학습합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/posts` | 게시물 생성 |
| GET | `/api/v1/posts/{id}` | 게시물 상세 조회 |
| GET | `/api/v1/posts` | 게시물 목록 조회 |
| PUT | `/api/v1/posts/{id}` | 게시물 수정 |
| DELETE | `/api/v1/posts/{id}` | 게시물 삭제 |
| PATCH | `/api/v1/posts/{id}/status` | 상태 변경 |

## Create (생성)

### Request DTO

```java
public record PostCreateRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200)
    String title,

    @NotBlank(message = "내용은 필수입니다")
    String content,

    @Size(max = 500)
    String summary,

    Set<String> tags,
    String category,

    @Size(max = 160)
    String metaDescription,

    String thumbnailUrl,
    String productId,

    Boolean publishImmediately  // 즉시 발행 여부
) {}
```

### Service 구현

```java
@Override
@Transactional
public PostResponse createPost(PostCreateRequest request, String authorId) {
    log.info("Creating post with title: {}, authorId: {}", request.title(), authorId);

    Post post = Post.builder()
        .title(request.title())
        .content(request.content())
        .summary(request.summary())
        .authorId(authorId)
        .authorName(extractAuthorName(authorId))
        .status(request.publishImmediately() ? PostStatus.PUBLISHED : PostStatus.DRAFT)
        .tags(request.tags())
        .category(request.category())
        .metaDescription(request.metaDescription())
        .thumbnailUrl(request.thumbnailUrl())
        .productId(request.productId())
        .build();

    // 즉시 발행인 경우 발행일시 설정
    if (request.publishImmediately()) {
        post.publish();
    }

    Post savedPost = postRepository.save(post);
    log.info("Post created successfully with id: {}", savedPost.getId());

    return convertToPostResponse(savedPost);
}
```

### Controller

```java
@PostMapping
public ResponseEntity<ApiResponse<PostResponse>> createPost(
        @Valid @RequestBody PostCreateRequest request,
        @RequestHeader("X-User-Id") String userId) {

    PostResponse response = postService.createPost(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(response));
}
```

## Read (조회)

### 단일 조회 (조회수 증가 포함)

```java
@Override
@Transactional
public PostResponse getPostByIdWithViewIncrement(String postId, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 조회 권한 확인 (발행된 글 또는 작성자 본인)
    if (!post.isViewableBy(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND);
    }

    // 조회수 증가
    post.incrementViewCount();
    postRepository.save(post);

    return convertToPostResponse(post);
}
```

### 목록 조회 (페이징)

```java
@Override
public Page<PostSummaryResponse> getPublishedPosts(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(
        PostStatus.PUBLISHED, pageable);
    return posts.map(this::convertToPostListResponse);
}
```

### 다양한 필터링

```java
// 카테고리별
public Page<PostSummaryResponse> getPostsByCategory(String category, int page, int size);

// 태그별
public Page<PostSummaryResponse> getPostsByTags(List<String> tags, int page, int size);

// 작성자별
public Page<PostSummaryResponse> getPostsByAuthor(String authorId, int page, int size);

// 인기 게시물
public Page<PostSummaryResponse> getPopularPosts(int page, int size);

// 트렌딩 게시물
public Page<PostSummaryResponse> getTrendingPosts(String period, int page, int size);
```

## Update (수정)

### Request DTO

```java
public record PostUpdateRequest(
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200)
    String title,

    @NotBlank(message = "내용은 필수입니다")
    String content,

    @Size(max = 500)
    String summary,

    Set<String> tags,
    String category,

    @Size(max = 160)
    String metaDescription,

    String thumbnailUrl,
    List<String> images
) {}
```

### Service 구현

```java
@Override
@Transactional
public PostResponse updatePost(String postId, PostUpdateRequest request, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 권한 검증: 작성자만 수정 가능
    if (!post.getAuthorId().equals(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
    }

    // 도메인 메서드로 수정
    post.update(
        request.title(),
        request.content(),
        request.summary(),
        request.tags(),
        request.category(),
        request.metaDescription(),
        request.thumbnailUrl(),
        request.images()
    );

    Post updatedPost = postRepository.save(post);
    return convertToPostResponse(updatedPost);
}
```

## Delete (삭제)

```java
@Override
@Transactional
public void deletePost(String postId, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 권한 검증: 작성자만 삭제 가능
    if (!post.getAuthorId().equals(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_DELETE_FORBIDDEN);
    }

    postRepository.delete(post);
    log.info("Post deleted successfully: {}", postId);
}
```

## 상태 변경 (Publish/Unpublish)

### Request DTO

```java
public record PostStatusChangeRequest(
    @NotNull(message = "상태는 필수입니다")
    PostStatus status
) {}
```

### Service 구현

```java
@Override
@Transactional
public PostResponse changePostStatus(String postId, PostStatus newStatus, String userId) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 권한 검증
    if (!post.getAuthorId().equals(userId)) {
        throw new CustomBusinessException(BlogErrorCode.POST_UPDATE_FORBIDDEN);
    }

    // 상태 변경
    if (newStatus == PostStatus.PUBLISHED) {
        post.publish();  // publishedAt 설정
    } else if (newStatus == PostStatus.DRAFT) {
        post.unpublish();
    }

    Post updatedPost = postRepository.save(post);
    return convertToPostResponse(updatedPost);
}
```

## Response DTO

### 상세 조회용

```java
public record PostResponse(
    String id,
    String title,
    String content,
    String summary,
    String authorId,
    String authorName,
    PostStatus status,
    Set<String> tags,
    String category,
    String metaDescription,
    String thumbnailUrl,
    List<String> images,
    Long viewCount,
    Long likeCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime publishedAt,
    String productId
) {}
```

### 목록 조회용 (요약)

```java
public record PostSummaryResponse(
    String id,
    String title,
    String summary,
    String authorId,
    String authorName,
    Set<String> tags,
    String category,
    String thumbnailUrl,
    List<String> images,
    Long viewCount,
    Long likeCount,
    Long commentCount,
    LocalDateTime publishedAt,
    int estimatedReadTime
) {}
```

## 읽기 시간 계산

```java
private int calculateReadTime(String content) {
    if (content == null || content.isEmpty()) return 1;
    int wordCount = content.length();
    int readTime = (int) Math.ceil(wordCount / 200.0);  // 분당 200자
    return Math.max(1, readTime);  // 최소 1분
}
```

## 에러 코드

```java
POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "Post not found"),
POST_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B002", "You are not allowed to update this post"),
POST_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B003", "You are not allowed to delete this post"),
POST_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "B004", "Post is not published yet");
```

## 핵심 포인트

| 기능 | 핵심 사항 |
|------|----------|
| 생성 | 즉시 발행 옵션, 요약 자동 생성 |
| 조회 | 권한 확인, 조회수 증가 |
| 수정 | 작성자만 가능 |
| 삭제 | 작성자만 가능 |
| 상태 변경 | publish() 시 publishedAt 설정 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/controller/PostController.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/service/PostServiceImpl.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/dto/`
