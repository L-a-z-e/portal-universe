# Blog Search

## 개요

Blog Service의 검색 기능 구현을 학습합니다. MongoDB 텍스트 검색과 다양한 필터링 옵션을 포함합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/posts/search` | 키워드 검색 |
| POST | `/api/v1/posts/search/advanced` | 고급 검색 |
| GET | `/api/v1/posts/category/{category}` | 카테고리별 조회 |
| GET | `/api/v1/posts/tag/{tag}` | 태그별 조회 |

## 기본 검색 (키워드)

### Controller

```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> searchPosts(
        @RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    return ResponseEntity.ok(
        ApiResponse.success(postService.searchPosts(keyword, page, size))
    );
}
```

### Service 구현

```java
@Override
public Page<PostSummaryResponse> searchPosts(String keyword, int page, int size) {
    log.info("Searching posts with keyword: {}", keyword);

    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByTextSearchAndStatus(
        keyword,
        PostStatus.PUBLISHED,
        pageable
    );

    return posts.map(this::convertToPostListResponse);
}
```

### Repository

```java
/**
 * 전문 검색 (제목 + 내용)
 * MongoDB Text Index 활용
 */
@Query("{ $text: { $search: ?0 }, status: ?1 }")
Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);
```

## 고급 검색

### Request DTO

```java
public record PostSearchRequest(
    String keyword,         // 검색어
    String category,        // 카테고리 필터
    List<String> tags,      // 태그 필터 (OR 조건)
    String authorId,        // 작성자 필터
    PostStatus status,      // 상태 필터 (관리자용)

    // 페이징
    @Min(0) int page,
    @Min(1) @Max(100) int size,

    // 정렬
    PostSortType sortBy,    // 정렬 기준
    SortDirection sortDirection  // 정렬 방향
) {}
```

### 정렬 타입

```java
public enum PostSortType {
    CREATED_AT,
    PUBLISHED_AT,
    VIEW_COUNT,
    LIKE_COUNT,
    TITLE
}

public enum SortDirection {
    ASC, DESC
}
```

### Service 구현

```java
@Override
public Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest searchRequest) {
    log.info("Advanced search with request: {}", searchRequest);

    Pageable pageable = PageRequest.of(
        searchRequest.page(),
        searchRequest.size(),
        createSort(searchRequest.sortBy(), searchRequest.sortDirection())
    );

    // 키워드 검색 우선
    if (searchRequest.keyword() != null && !searchRequest.keyword().isEmpty()) {
        PostStatus status = searchRequest.status() != null
            ? searchRequest.status()
            : PostStatus.PUBLISHED;

        Page<Post> posts = postRepository.findByTextSearchAndStatus(
            searchRequest.keyword(),
            status,
            pageable
        );
        return posts.map(this::convertToPostListResponse);
    }

    // 카테고리 필터
    if (searchRequest.category() != null) {
        return getPostsByCategory(searchRequest.category(),
            searchRequest.page(), searchRequest.size());
    }

    // 태그 필터
    if (searchRequest.tags() != null && !searchRequest.tags().isEmpty()) {
        return getPostsByTags(searchRequest.tags(),
            searchRequest.page(), searchRequest.size());
    }

    // 작성자 필터
    if (searchRequest.authorId() != null) {
        return getPostsByAuthor(searchRequest.authorId(),
            searchRequest.page(), searchRequest.size());
    }

    // 기본: 발행된 게시물 목록
    return getPublishedPosts(searchRequest.page(), searchRequest.size());
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

## 카테고리별 조회

```java
@Override
public Page<PostSummaryResponse> getPostsByCategory(String category, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByCategoryAndStatusOrderByPublishedAtDesc(
        category, PostStatus.PUBLISHED, pageable);
    return posts.map(this::convertToPostListResponse);
}
```

## 태그별 조회

```java
@Override
public Page<PostSummaryResponse> getPostsByTags(List<String> tags, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByTagsInAndStatusOrderByPublishedAtDesc(
        tags, PostStatus.PUBLISHED, pageable);
    return posts.map(this::convertToPostListResponse);
}
```

## 인기 게시물 조회

```java
@Override
public Page<PostSummaryResponse> getPopularPosts(int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByStatusOrderByViewCountDescPublishedAtDesc(
        PostStatus.PUBLISHED, pageable);
    return posts.map(this::convertToPostListResponse);
}
```

## 트렌딩 게시물 조회

### 점수 계산 공식

```
baseScore = viewCount + (likeCount × 3) + (commentCount × 5)
timeDecay = 2^(-hoursElapsed / halfLife)
trendingScore = baseScore × timeDecay
```

### Service 구현

```java
@Override
public Page<PostSummaryResponse> getTrendingPosts(String period, int page, int size) {
    LocalDateTime startDate = calculateStartDateByPeriod(period);
    double halfLifeHours = getHalfLifeByPeriod(period);

    // MongoDB Aggregation으로 점수 계산 및 정렬
    Page<Post> trendingPosts = postRepository.aggregateTrendingPosts(
        PostStatus.PUBLISHED, startDate, halfLifeHours, page, size);

    return trendingPosts.map(this::convertToPostListResponse);
}

private double getHalfLifeByPeriod(String period) {
    return switch (period) {
        case "today" -> 6.0;    // 6시간
        case "week" -> 48.0;    // 2일
        case "month" -> 168.0;  // 7일
        case "year" -> 720.0;   // 30일
        default -> 48.0;
    };
}

private LocalDateTime calculateStartDateByPeriod(String period) {
    LocalDateTime now = LocalDateTime.now();
    return switch (period) {
        case "today" -> now.toLocalDate().atStartOfDay();
        case "week" -> now.minusDays(7);
        case "month" -> now.minusDays(30);
        case "year" -> now.minusYears(1);
        default -> now.minusDays(7);
    };
}
```

## 관련 게시물 조회

```java
@Override
public List<PostSummaryResponse> getRelatedPosts(String postId, int limit) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    // 동일 카테고리 OR 공통 태그
    List<Post> relatedPosts = postRepository.findRelatedPosts(
        post.getCategory(),
        new ArrayList<>(post.getTags()),
        PostStatus.PUBLISHED,
        postId  // 현재 게시물 제외
    );

    return relatedPosts.stream()
        .limit(limit)
        .map(this::convertToPostListResponse)
        .collect(Collectors.toList());
}
```

### Repository

```java
@Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);
```

## 통계 조회

### 카테고리 통계

```java
@Override
public List<CategoryStats> getCategoryStats() {
    return postRepository.aggregateCategoryStats(PostStatus.PUBLISHED);
}
```

### 인기 태그

```java
@Override
public List<TagStats> getPopularTags(int limit) {
    return postRepository.aggregatePopularTags(PostStatus.PUBLISHED, limit);
}
```

## 프론트엔드: 검색 UI

### 검색 컴포넌트 (Vue)

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useDebounceFn } from '@vueuse/core'

const router = useRouter()
const route = useRoute()

const keyword = ref(route.query.keyword as string || '')
const category = ref(route.query.category as string || '')
const selectedTags = ref<string[]>([])
const sortBy = ref(route.query.sortBy as string || 'publishedAt')

const search = useDebounceFn(() => {
  router.push({
    path: '/posts',
    query: {
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      tags: selectedTags.value.length ? selectedTags.value.join(',') : undefined,
      sortBy: sortBy.value
    }
  })
}, 300)

watch([keyword, category, selectedTags, sortBy], search)
</script>

<template>
  <div class="search-panel">
    <!-- 키워드 검색 -->
    <input
      v-model="keyword"
      type="search"
      placeholder="검색어를 입력하세요..."
    />

    <!-- 카테고리 필터 -->
    <select v-model="category">
      <option value="">모든 카테고리</option>
      <option v-for="cat in categories" :key="cat" :value="cat">
        {{ cat }}
      </option>
    </select>

    <!-- 정렬 -->
    <select v-model="sortBy">
      <option value="publishedAt">최신순</option>
      <option value="viewCount">조회수순</option>
      <option value="likeCount">좋아요순</option>
    </select>

    <!-- 태그 필터 -->
    <TagSelector v-model="selectedTags" />
  </div>
</template>
```

## 핵심 포인트

| 기능 | 구현 방식 |
|------|----------|
| 키워드 검색 | MongoDB Text Index |
| 카테고리/태그 | 필드 필터링 |
| 정렬 | 동적 Sort 생성 |
| 트렌딩 | Aggregation + 점수 계산 |
| 관련 게시물 | OR 조건 쿼리 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/service/PostServiceImpl.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/dto/PostSearchRequest.java`
