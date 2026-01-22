# MongoDB Pagination

## 개요

Blog Service에서 사용하는 페이지네이션 패턴을 학습합니다. Offset 기반과 Cursor 기반의 차이점을 이해합니다.

## Offset 기반 페이지네이션 (현재 사용)

### Spring Data MongoDB

```java
// Repository
Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

// Service
public Page<PostSummaryResponse> getPublishedPosts(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(
        PostStatus.PUBLISHED,
        pageable
    );
    return posts.map(this::convertToPostListResponse);
}
```

### MongoDB 쿼리 (내부 동작)

```javascript
// page=2, size=10 요청 시
db.posts.find({ status: "PUBLISHED" })
    .sort({ publishedAt: -1 })
    .skip(20)          // page * size
    .limit(10)         // size
```

### 장단점

| 장점 | 단점 |
|------|------|
| 구현 간단 | 대용량에서 성능 저하 |
| 특정 페이지 직접 이동 가능 | 데이터 변경 시 중복/누락 |
| UI 구현 쉬움 | skip() 비용 증가 |

## Cursor 기반 페이지네이션

### 개념

```
마지막으로 본 문서의 값을 기준으로 다음 문서 조회

1페이지: publishedAt > 없음 (처음부터)
2페이지: publishedAt < "2024-01-15T10:00:00" (마지막 문서 기준)
3페이지: publishedAt < "2024-01-14T15:30:00" (마지막 문서 기준)
```

### Repository 메서드

```java
public interface PostRepository {

    // Cursor 기반: 특정 시점 이후의 게시물
    Page<Post> findByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
        PostStatus status,
        LocalDateTime cursor,
        Pageable pageable
    );

    // 첫 페이지용 (cursor 없음)
    Page<Post> findByStatusOrderByPublishedAtDesc(
        PostStatus status,
        Pageable pageable
    );
}
```

### Service 구현

```java
public CursorPageResponse<PostSummaryResponse> getPostsWithCursor(
        String cursor,
        int size) {

    Pageable pageable = PageRequest.of(0, size + 1);  // 다음 페이지 확인용 +1
    List<Post> posts;

    if (cursor == null || cursor.isEmpty()) {
        // 첫 페이지
        posts = postRepository.findByStatusOrderByPublishedAtDesc(
            PostStatus.PUBLISHED, pageable
        ).getContent();
    } else {
        // 다음 페이지
        LocalDateTime cursorTime = LocalDateTime.parse(cursor);
        posts = postRepository.findByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            PostStatus.PUBLISHED, cursorTime, pageable
        ).getContent();
    }

    boolean hasNext = posts.size() > size;
    if (hasNext) {
        posts = posts.subList(0, size);  // 추가 조회한 1개 제거
    }

    String nextCursor = hasNext
        ? posts.get(posts.size() - 1).getPublishedAt().toString()
        : null;

    return new CursorPageResponse<>(
        posts.stream().map(this::convertToPostListResponse).toList(),
        nextCursor,
        hasNext
    );
}
```

### Response 구조

```java
public record CursorPageResponse<T>(
    List<T> content,
    String nextCursor,
    boolean hasNext
) {}
```

## 비교: Offset vs Cursor

### 성능

```
데이터: 100만 건

Offset (page=10000, size=10):
skip(100000) → 10만 건 스캔 후 10건 반환 → 느림

Cursor (cursor="2023-01-01T00:00:00"):
publishedAt < cursor → 인덱스 직접 접근 → 빠름
```

### 데이터 변경 시

```
시나리오: 1페이지 조회 후 새 게시물 추가

Offset:
- 1페이지: [A, B, C, D, E]
- 새 게시물 F 추가
- 2페이지: [E, F, G, H, I]  // E 중복!

Cursor:
- 1페이지: [A, B, C, D, E], cursor=E.publishedAt
- 새 게시물 F 추가
- 2페이지: publishedAt < cursor → [F, G, H, I, J]  // 중복 없음
```

## 네비게이션 패턴

### 이전/다음 게시물

```java
// 이전 게시물 (publishedAt이 현재보다 작은 것 중 가장 최신)
Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
    PostStatus status,
    LocalDateTime publishedAt
);

// 다음 게시물 (publishedAt이 현재보다 큰 것 중 가장 오래된 것)
Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
    PostStatus status,
    LocalDateTime publishedAt
);
```

### Service 구현

```java
public PostNavigationResponse getPostNavigation(String postId, String scope) {
    Post currentPost = postRepository.findById(postId).orElseThrow();

    PostSummaryResponse previousPost = postRepository
        .findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
            PostStatus.PUBLISHED,
            currentPost.getPublishedAt()
        )
        .map(this::convertToPostListResponse)
        .orElse(null);

    PostSummaryResponse nextPost = postRepository
        .findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
            PostStatus.PUBLISHED,
            currentPost.getPublishedAt()
        )
        .map(this::convertToPostListResponse)
        .orElse(null);

    return PostNavigationResponse.of(previousPost, nextPost, null);
}
```

## 정렬과 페이지네이션

### 다양한 정렬 지원

```java
public Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest request) {
    Pageable pageable = PageRequest.of(
        request.page(),
        request.size(),
        createSort(request.sortBy(), request.sortDirection())
    );
    // ...
}

private Sort createSort(PostSortType sortBy, SortDirection direction) {
    Sort.Direction sortDirection = direction == SortDirection.ASC
        ? Sort.Direction.ASC
        : Sort.Direction.DESC;

    String field = switch (sortBy) {
        case CREATED_AT -> "createdAt";
        case PUBLISHED_AT -> "publishedAt";
        case VIEW_COUNT -> "viewCount";
        case LIKE_COUNT -> "likeCount";
        case TITLE -> "title";
    };

    return Sort.by(sortDirection, field);
}
```

## 인덱스 최적화

```java
// 페이지네이션에 사용되는 필드에 인덱스 필요
new Index()
    .on("status", Sort.Direction.ASC)
    .on("publishedAt", Sort.Direction.DESC);

new Index()
    .on("status", Sort.Direction.ASC)
    .on("viewCount", Sort.Direction.DESC)
    .on("publishedAt", Sort.Direction.DESC);
```

## 선택 가이드

| 사용 사례 | 추천 방식 |
|----------|----------|
| 관리자 대시보드 | Offset (특정 페이지 이동 필요) |
| 무한 스크롤 피드 | Cursor (실시간 데이터, 성능) |
| 검색 결과 | Offset (총 결과 수 표시 필요) |
| 대용량 목록 | Cursor (skip 비용 회피) |

## 핵심 포인트

| 항목 | Offset | Cursor |
|------|--------|--------|
| 구현 | skip + limit | 조건 + limit |
| 성능 | 대용량에서 저하 | 일정 |
| 중복/누락 | 발생 가능 | 방지 |
| 페이지 점프 | 가능 | 불가능 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/service/PostServiceImpl.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/dto/PostNavigationResponse.java`
