# Post Document Schema

## 개요

Blog Service의 핵심 도메인인 Post Document의 스키마 설계와 Embedding 전략을 학습합니다.

## Document Schema

```java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;

    // 검색 최적화 필드
    @TextIndexed(weight = 2.0f)
    private String title;           // 제목 (가중치 2.0)

    @TextIndexed
    private String content;         // 내용 (가중치 1.0)

    private String summary;         // 요약 (200자 자동 생성)

    // 작성자 정보 (Embedding)
    @Indexed
    private String authorId;        // 작성자 ID
    private String authorName;      // 작성자 이름 (비정규화)

    // 상태 관리
    @Indexed
    private PostStatus status;      // DRAFT, PUBLISHED, ARCHIVED

    @Indexed
    private LocalDateTime publishedAt;

    // 분류 체계
    @Indexed
    private Set<String> tags;       // 태그 목록

    @Indexed
    private String category;        // 카테고리

    // 참여도 지표 (역정규화)
    private Long viewCount = 0L;    // 조회수
    private Long likeCount = 0L;    // 좋아요 수
    private Long commentCount = 0L; // 댓글 수

    // SEO 최적화
    private String metaDescription; // 160자 제한

    // 미디어
    private String thumbnailUrl;    // 썸네일
    private List<String> images;    // 첨부 이미지 목록

    // 확장 기능
    private String productId;       // 연관 상품 (선택적)

    // 타임스탬프
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

## Embedding 전략

### 1. 작성자 정보 Embedding

```java
// authorName을 Post 문서에 직접 저장 (역정규화)
private String authorId;    // Reference (조회용)
private String authorName;  // Embedded (표시용)
```

**결정 근거:**
- 작성자 이름은 Post 조회 시 항상 필요
- 이름 변경 빈도 낮음 (허용 가능한 비일관성)
- N+1 쿼리 방지 (Post 목록 조회 시 User 서비스 호출 불필요)

### 2. 태그 Embedding

```java
// 태그 이름을 Post에 직접 저장
private Set<String> tags = new HashSet<>();
```

**결정 근거:**
- 태그는 단순 문자열로 충분
- 별도 Tag 컬렉션은 통계/검색 용도로만 사용
- Post 조회 시 JOIN 없이 태그 표시 가능

### 3. 카운터 Embedding (역정규화)

```java
// 참여도 지표를 Post에 직접 저장
private Long viewCount = 0L;
private Long likeCount = 0L;
private Long commentCount = 0L;
```

**결정 근거:**
- 정렬/필터링에 자주 사용
- Aggregation 없이 즉시 표시 가능
- 약간의 비일관성 허용 (최종 일관성)

## 비즈니스 메서드

### 상태 관리

```java
public void publish() {
    this.status = PostStatus.PUBLISHED;
    this.publishedAt = LocalDateTime.now();
}

public void unpublish() {
    this.status = PostStatus.DRAFT;
    this.publishedAt = null;
}

public boolean isViewableBy(String userId) {
    return isPublished() || this.authorId.equals(userId);
}
```

### 카운터 관리

```java
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

### 자동 생성 기능

```java
// 내용에서 요약 자동 생성
private String generateSummary(String content) {
    if (content == null || content.isEmpty()) return "";
    String clean = content.replaceAll("<[^>]*>", "");
    return clean.length() > 200 ? clean.substring(0, 200) + "..." : clean;
}

// SEO 메타 설명 자동 생성
private String generateMetaDescription(String content) {
    if (content == null || content.isEmpty()) return "";
    String clean = content.replaceAll("<[^>]*>", "");
    return clean.length() > 160 ? clean.substring(0, 160) + "..." : clean;
}
```

## MongoDB 인덱스

```java
// MongoConfig.java에서 생성
TextIndexDefinition textIndex = TextIndexDefinition.builder()
    .onField("title", 2.0f)
    .onField("content", 1.0f)
    .build();

// 복합 인덱스
new Index()
    .on("status", Sort.Direction.ASC)
    .on("publishedAt", Sort.Direction.DESC);

new Index()
    .on("authorId", Sort.Direction.ASC)
    .on("createdAt", Sort.Direction.DESC);
```

## 핵심 포인트

| 전략 | 설명 | 이점 |
|------|------|------|
| 작성자 이름 Embedding | 비정규화로 직접 저장 | N+1 쿼리 방지 |
| 카운터 Embedding | 참여도 지표 직접 저장 | 정렬/필터 최적화 |
| 태그 배열 | Set으로 중복 방지 | JOIN 없는 검색 |
| 텍스트 인덱스 | 제목에 가중치 2.0 | 검색 관련도 향상 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/PostStatus.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/config/MongoConfig.java`
