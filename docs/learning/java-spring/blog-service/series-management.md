# Series Management

## 개요

블로그 포스트를 연재물로 묶어서 관리하는 시리즈(Series) 기능을 학습합니다.

## API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/series` | 시리즈 생성 |
| GET | `/api/v1/series/{id}` | 시리즈 상세 조회 |
| GET | `/api/v1/series/author/{authorId}` | 작성자별 시리즈 목록 |
| PUT | `/api/v1/series/{id}` | 시리즈 수정 |
| DELETE | `/api/v1/series/{id}` | 시리즈 삭제 |
| POST | `/api/v1/series/{id}/posts/{postId}` | 포스트 추가 |
| DELETE | `/api/v1/series/{id}/posts/{postId}` | 포스트 제거 |
| PUT | `/api/v1/series/{id}/posts/reorder` | 순서 변경 |

## 시리즈 생성

### Request DTO

```java
public record SeriesCreateRequest(
    @NotBlank(message = "시리즈 제목은 필수입니다")
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String description,

    String thumbnailUrl
) {}
```

### Service 구현

```java
public SeriesResponse createSeries(SeriesCreateRequest request, String authorId, String authorName) {
    Series series = Series.builder()
        .name(request.name())
        .description(request.description())
        .authorId(authorId)
        .authorName(authorName)
        .thumbnailUrl(request.thumbnailUrl())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    seriesRepository.save(series);
    return toResponse(series);
}
```

## 포스트 추가

### Service 구현

```java
public SeriesResponse addPostToSeries(String seriesId, String postId, String authorId) {
    Series series = seriesRepository.findById(seriesId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

    // 권한 검증
    if (!series.getAuthorId().equals(authorId)) {
        throw new CustomBusinessException(BlogErrorCode.SERIES_ADD_POST_FORBIDDEN);
    }

    series.addPost(postId);
    seriesRepository.save(series);

    return toResponse(series);
}
```

### Domain 메서드

```java
// Series.java
public void addPost(String postId) {
    if (!this.postIds.contains(postId)) {
        this.postIds.add(postId);
        this.updatedAt = LocalDateTime.now();
    }
}

public void addPostAt(String postId, int index) {
    if (!this.postIds.contains(postId)) {
        if (index < 0 || index > this.postIds.size()) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        this.postIds.add(index, postId);
        this.updatedAt = LocalDateTime.now();
    }
}
```

## 포스트 제거

```java
public SeriesResponse removePostFromSeries(String seriesId, String postId, String authorId) {
    Series series = seriesRepository.findById(seriesId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

    if (!series.getAuthorId().equals(authorId)) {
        throw new CustomBusinessException(BlogErrorCode.SERIES_REMOVE_POST_FORBIDDEN);
    }

    series.removePost(postId);
    seriesRepository.save(series);

    return toResponse(series);
}
```

## 순서 변경

### Request DTO

```java
public record SeriesPostOrderRequest(
    @NotNull(message = "포스트 ID 목록은 필수입니다")
    List<String> postIds
) {}
```

### Service 구현

```java
public SeriesResponse reorderPosts(String seriesId, SeriesPostOrderRequest request, String authorId) {
    Series series = seriesRepository.findById(seriesId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.SERIES_NOT_FOUND));

    if (!series.getAuthorId().equals(authorId)) {
        throw new CustomBusinessException(BlogErrorCode.SERIES_REORDER_FORBIDDEN);
    }

    series.reorderPosts(request.postIds());
    seriesRepository.save(series);

    return toResponse(series);
}
```

### Domain 메서드

```java
public void reorderPosts(List<String> newPostIds) {
    // 검증: 기존 포스트 ID와 동일한지 확인
    if (!this.postIds.containsAll(newPostIds) ||
        !newPostIds.containsAll(this.postIds)) {
        throw new IllegalArgumentException("Post IDs mismatch");
    }
    this.postIds = new ArrayList<>(newPostIds);
    this.updatedAt = LocalDateTime.now();
}
```

## 시리즈 네비게이션 (Post 조회 시)

### Service 구현

```java
private SeriesNavigationResponse getSeriesNavigation(Post currentPost) {
    List<Series> seriesList = seriesRepository.findByPostIdsContaining(currentPost.getId());

    if (seriesList.isEmpty()) {
        return null;
    }

    // 첫 번째 시리즈 사용 (일반적으로 하나의 포스트는 하나의 시리즈에만 속함)
    Series series = seriesList.get(0);
    List<String> postIds = series.getPostIds();
    int currentIndex = postIds.indexOf(currentPost.getId());

    if (currentIndex == -1) {
        return null;
    }

    String previousPostId = currentIndex > 0 ? postIds.get(currentIndex - 1) : null;
    String nextPostId = currentIndex < postIds.size() - 1 ? postIds.get(currentIndex + 1) : null;

    return SeriesNavigationResponse.of(
        series.getId(),
        series.getName(),
        currentIndex,
        series.getPostCount(),
        previousPostId,
        nextPostId
    );
}
```

### Response DTO

```java
public record SeriesNavigationResponse(
    String seriesId,
    String seriesName,
    int currentOrder,       // 0-based
    int totalPosts,
    String previousPostId,  // null if first
    String nextPostId       // null if last
) {
    public static SeriesNavigationResponse of(String seriesId, String seriesName,
            int currentOrder, int totalPosts, String previousPostId, String nextPostId) {
        return new SeriesNavigationResponse(
            seriesId, seriesName, currentOrder, totalPosts, previousPostId, nextPostId
        );
    }
}
```

## Response DTOs

### 시리즈 상세

```java
public record SeriesResponse(
    String id,
    String name,
    String description,
    String authorId,
    String authorName,
    String thumbnailUrl,
    List<String> postIds,
    int postCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### 시리즈 목록

```java
public record SeriesListResponse(
    String id,
    String name,
    String description,
    String authorId,
    String authorName,
    String thumbnailUrl,
    int postCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

## Repository

```java
public interface SeriesRepository extends MongoRepository<Series, String> {

    // 작성자별 시리즈 목록
    List<Series> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    // 특정 포스트가 포함된 시리즈
    List<Series> findByPostIdsContaining(String postId);
}
```

## 프론트엔드 UI

### 시리즈 네비게이션 컴포넌트

```vue
<script setup lang="ts">
import { computed } from 'vue'

interface SeriesNavigation {
  seriesId: string
  seriesName: string
  currentOrder: number
  totalPosts: number
  previousPostId: string | null
  nextPostId: string | null
}

const props = defineProps<{
  navigation: SeriesNavigation
}>()

const progress = computed(() =>
  ((props.navigation.currentOrder + 1) / props.navigation.totalPosts) * 100
)
</script>

<template>
  <div class="series-navigation">
    <div class="series-header">
      <router-link :to="`/series/${navigation.seriesId}`">
        {{ navigation.seriesName }}
      </router-link>
      <span class="progress">
        {{ navigation.currentOrder + 1 }} / {{ navigation.totalPosts }}
      </span>
    </div>

    <div class="progress-bar">
      <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
    </div>

    <div class="navigation-buttons">
      <router-link
        v-if="navigation.previousPostId"
        :to="`/posts/${navigation.previousPostId}`"
        class="prev-button"
      >
        이전 글
      </router-link>

      <router-link
        v-if="navigation.nextPostId"
        :to="`/posts/${navigation.nextPostId}`"
        class="next-button"
      >
        다음 글
      </router-link>
    </div>
  </div>
</template>
```

## 에러 코드

```java
SERIES_NOT_FOUND(HttpStatus.NOT_FOUND, "B040", "Series not found"),
SERIES_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "B041", "You are not allowed to update this series"),
SERIES_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "B042", "You are not allowed to delete this series"),
SERIES_ADD_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B043", "You are not allowed to add posts to this series"),
SERIES_REMOVE_POST_FORBIDDEN(HttpStatus.FORBIDDEN, "B044", "You are not allowed to remove posts from this series"),
SERIES_REORDER_FORBIDDEN(HttpStatus.FORBIDDEN, "B045", "You are not allowed to reorder posts in this series");
```

## 핵심 포인트

| 기능 | 핵심 사항 |
|------|----------|
| 저장 | Post ID 목록만 저장 (Reference) |
| 순서 | List 인덱스로 관리 |
| 네비게이션 | 이전/다음 포스트 링크 제공 |
| 권한 | 작성자만 관리 가능 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/domain/Series.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/service/SeriesService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/controller/SeriesController.java`
