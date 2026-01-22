# Series Document - 시리즈 관리

## 개요

블로그 포스트를 연재물로 묶어서 관리하는 시리즈(Series) 기능의 설계를 학습합니다.

## 사용 사례

```
"Vue.js 완벽 가이드" 시리즈
├── 1편: Vue 시작하기
├── 2편: 컴포넌트 이해하기
├── 3편: 상태 관리
└── 4편: 라우팅과 네비게이션
```

## Document Schema

```java
@Document(collection = "series")
public class Series {
    @Id
    private String id;

    @NotBlank(message = "시리즈 제목은 필수입니다")
    @Size(max = 100)
    private String name;            // 시리즈 제목

    @Size(max = 500)
    private String description;     // 시리즈 설명

    @Indexed
    @NotBlank(message = "작성자는 필수입니다")
    private String authorId;        // 작성자 ID
    private String authorName;      // 작성자 이름 (비정규화)

    private String thumbnailUrl;    // 시리즈 썸네일

    // 핵심: 포스트 ID 목록 (순서 유지)
    @Builder.Default
    private List<String> postIds = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 설계 결정: Reference vs Embedding

### 현재 접근법: Post ID Reference

```java
// Series에는 Post ID만 저장
private List<String> postIds = new ArrayList<>();
```

**장점:**
- Post 수정 시 Series 업데이트 불필요
- 순서 변경이 간단 (리스트 순서만 변경)
- Series 문서 크기가 작음

**단점:**
- 시리즈 상세 조회 시 Post 추가 조회 필요

### 대안: Post 정보 Embedding (미사용)

```java
// 사용하지 않는 방식
private List<SeriesPost> posts; // title, summary 등 포함
```

**장점:**
- 시리즈 조회 시 한 번에 모든 정보 획득

**단점:**
- Post 수정 시 Series도 업데이트 필요
- 데이터 중복, 일관성 문제

## 비즈니스 메서드

### 포스트 관리

```java
// 시리즈에 포스트 추가 (맨 뒤)
public void addPost(String postId) {
    if (!this.postIds.contains(postId)) {
        this.postIds.add(postId);
        this.updatedAt = LocalDateTime.now();
    }
}

// 특정 위치에 포스트 추가
public void addPostAt(String postId, int index) {
    if (!this.postIds.contains(postId)) {
        if (index < 0 || index > this.postIds.size()) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
        this.postIds.add(index, postId);
        this.updatedAt = LocalDateTime.now();
    }
}

// 포스트 제거
public void removePost(String postId) {
    this.postIds.remove(postId);
    this.updatedAt = LocalDateTime.now();
}
```

### 순서 변경

```java
// 포스트 순서 변경 (전체 재배치)
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

### 조회 헬퍼

```java
// 특정 포스트가 시리즈에 포함되어 있는지
public boolean containsPost(String postId) {
    return this.postIds.contains(postId);
}

// 포스트의 순서(인덱스) 반환 (0-based)
public int getPostOrder(String postId) {
    return this.postIds.indexOf(postId);
}

// 포스트 개수
public int getPostCount() {
    return this.postIds.size();
}
```

## Service 구현

```java
@Service
public class SeriesService {

    // 시리즈에 포스트 추가
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

    // 특정 포스트가 포함된 시리즈 조회
    public List<SeriesListResponse> getSeriesByPostId(String postId) {
        List<Series> seriesList = seriesRepository.findByPostIdsContaining(postId);
        return seriesList.stream()
            .map(this::toListResponse)
            .toList();
    }
}
```

## 시리즈 네비게이션

```java
// Post 상세 페이지에서 시리즈 내 이전/다음 포스트 네비게이션
private SeriesNavigationResponse getSeriesNavigation(Post currentPost) {
    List<Series> seriesList = seriesRepository.findByPostIdsContaining(currentPost.getId());

    if (seriesList.isEmpty()) {
        return null;
    }

    Series series = seriesList.get(0);
    List<String> postIds = series.getPostIds();
    int currentIndex = postIds.indexOf(currentPost.getId());

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

## Repository

```java
public interface SeriesRepository extends MongoRepository<Series, String> {

    // 작성자별 시리즈 목록
    List<Series> findByAuthorIdOrderByCreatedAtDesc(String authorId);

    // 특정 포스트가 포함된 시리즈
    List<Series> findByPostIdsContaining(String postId);
}
```

## API Response

```java
// 시리즈 상세 응답
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

// 시리즈 목록 응답 (postIds 미포함)
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

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 관계 | Reference (Post ID 목록) |
| 순서 | List 인덱스로 관리 |
| 조회 | 시리즈 조회 후 Post 별도 조회 |
| 권한 | 작성자만 시리즈 관리 가능 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/domain/Series.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/service/SeriesService.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/series/repository/SeriesRepository.java`
