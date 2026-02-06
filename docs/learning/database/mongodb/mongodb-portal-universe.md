# Portal Universe MongoDB 분석

## 학습 목표
- Blog Service의 MongoDB 아키텍처 이해
- 실제 Document 모델링 분석
- Repository 패턴 및 쿼리 전략 학습
- 인덱스 설계 의도 파악

---

## 1. 아키텍처 개요

### 1.1 Portal Universe 데이터베이스 선택

| 서비스 | 데이터베이스 | 선택 이유 |
|--------|-------------|-----------|
| **Auth Service** | MySQL | 사용자 인증/권한 - 데이터 무결성 중요 |
| **Shopping Service** | MySQL | 주문/결제 - ACID 트랜잭션 필수 |
| **Blog Service** | MongoDB | 콘텐츠 관리 - 스키마 유연성, 문서 구조 |
| **Notification** | (Kafka) | 이벤트 기반 비동기 처리 |

### 1.2 Blog Service MongoDB 구조

```
blog_db/
├── posts          # 블로그 게시물 (핵심 컬렉션)
├── comments       # 댓글 (1:Many 관계)
├── likes          # 좋아요 (N:M 연결 테이블 역할)
├── tags           # 태그 마스터 (역정규화된 카운트)
└── series         # 연재물/시리즈
```

### 1.3 연결 설정

```yaml
# application-local.yml
spring:
  data:
    mongodb:
      uri: mongodb://laze:password@localhost:27017/blog_db?authSource=admin
```

---

## 2. Document 모델 분석

### 2.1 Post (게시물)

**핵심 Document - Hybrid 접근법 적용**

```java
@Document(collection = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    private String id;                    // MongoDB ObjectId (String)

    // === 전문 검색 지원 ===
    @TextIndexed(weight = 2.0f)           // 제목에 2배 가중치
    @NotBlank
    @Size(max = 200)
    private String title;

    @TextIndexed                          // 본문 검색
    @NotBlank
    private String content;

    @Size(max = 500)
    private String summary;               // 목록 표시용 요약

    // === 작성자 정보 (Hybrid: ID + 역정규화) ===
    @Indexed
    @NotBlank
    private String authorId;              // 참조 ID (일관성)
    private String authorName;            // 역정규화 (표시용)

    // === 상태 및 분류 ===
    @Indexed
    private PostStatus status = PostStatus.DRAFT;

    @Indexed
    private Set<String> tags = new HashSet<>();  // Embedding (1:Few)

    @Indexed
    private String category;

    // === 카운터 (역정규화) ===
    private Long viewCount = 0L;
    private Long likeCount = 0L;          // likes 컬렉션과 동기화
    private Long commentCount = 0L;       // comments 컬렉션과 동기화

    // === 발행 정보 ===
    @Indexed
    private LocalDateTime publishedAt;
    private String metaDescription;       // SEO

    // === 미디어 ===
    private String thumbnailUrl;
    private List<String> images = new ArrayList<>();  // Embedding

    // === 레거시 호환 ===
    private String productId;             // Shopping 연동용

    // === 감사 필드 ===
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**설계 결정 분석:**

| 필드 | 전략 | 이유 |
|------|------|------|
| `authorId` + `authorName` | Hybrid | ID로 일관성, 이름으로 빠른 조회 |
| `tags` | Embedding | 게시물당 소수 (1:Few) |
| `images` | Embedding | 게시물과 1:1 생명주기 |
| `likeCount`, `commentCount` | 역정규화 | count() 쿼리 방지 |
| `comments` | Referencing (별도 컬렉션) | 다수 댓글 가능 (1:Many) |

### 2.2 Comment (댓글)

**별도 컬렉션 - 1:Many 관계**

```java
@Document(collection = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    private String id;

    @Indexed                              // 게시물별 댓글 조회
    @NotBlank
    private String postId;                // Post 참조

    @Indexed                              // 작성자별 댓글 조회
    @NotBlank
    private String authorId;

    private String authorName;            // 역정규화

    @NotBlank
    private String content;

    private String parentCommentId;       // 대댓글 지원 (자기 참조)

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Boolean isDeleted = false;    // Soft Delete

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void delete() {
        this.isDeleted = true;            // Soft delete
    }

    public boolean isRootComment() {
        return parentCommentId == null;
    }
}
```

**대댓글 구조:**

```
Comment (parent = null)          <- 루트 댓글
├── Comment (parent = root_id)   <- 1단계 대댓글
│   └── Comment (parent = ...)   <- 2단계 대댓글
└── Comment (parent = root_id)   <- 1단계 대댓글
```

### 2.3 Like (좋아요)

**N:M 연결 - Compound Unique Index**

```java
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true                         // 중복 좋아요 방지
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Like {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String postId;

    @Indexed
    @NotBlank
    private String userId;

    private String userName;              // 역정규화

    @CreatedDate
    private LocalDateTime createdAt;
}
```

**역할:**
- User ↔ Post 간 N:M 관계 표현
- Compound Unique Index로 중복 방지
- 좋아요 토글 시 Post.likeCount 동기화

### 2.4 Tag (태그)

**역정규화된 통계 관리**

```java
@Document(collection = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    private String id;

    @TextIndexed                          // 태그 검색
    @Indexed(unique = true)               // 고유 태그명
    @NotBlank
    @Size(max = 50)
    private String name;

    @Builder.Default
    private Long postCount = 0L;          // 역정규화: 해당 태그 사용 게시물 수

    @Size(max = 200)
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;     // 인기 태그 정렬용

    // 비즈니스 메서드
    public void incrementPostCount() {
        this.postCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void decrementPostCount() {
        if (this.postCount > 0) {
            this.postCount--;
        }
    }

    public boolean isUnused() {
        return this.postCount == 0;
    }
}
```

### 2.5 Series (연재물)

**게시물 그룹화 - 순서 유지 배열**

```java
@Document(collection = "series")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Series {

    @Id
    private String id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Indexed
    @NotBlank
    private String authorId;

    private String authorName;

    private String thumbnailUrl;

    @Builder.Default
    private List<String> postIds = new ArrayList<>();  // 순서 유지 배열

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void addPost(String postId) {
        if (!this.postIds.contains(postId)) {
            this.postIds.add(postId);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void reorderPosts(List<String> newPostIds) {
        if (!this.postIds.containsAll(newPostIds) ||
            !newPostIds.containsAll(this.postIds)) {
            throw new IllegalArgumentException("Post IDs mismatch");
        }
        this.postIds = new ArrayList<>(newPostIds);
        this.updatedAt = LocalDateTime.now();
    }

    public int getPostOrder(String postId) {
        return this.postIds.indexOf(postId);  // 0-based index
    }
}
```

---

## 3. Repository 패턴

### 3.1 MongoRepository 확장

```java
public interface PostRepository extends MongoRepository<Post, String>, PostRepositoryCustom {

    // 메서드 이름 기반 쿼리
    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(
        String authorId, PostStatus status, Pageable pageable);

    Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(
        List<String> tags, PostStatus status, Pageable pageable);

    // @Query 어노테이션
    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

    @Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
    Optional<Post> findByIdAndViewableBy(String postId, String userId);

    @Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
    List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

    // 네비게이션
    Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
        PostStatus status, LocalDateTime publishedAt);

    Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
        PostStatus status, LocalDateTime publishedAt);

    // 통계
    long countByAuthorIdAndStatus(String authorId, PostStatus status);
    long countByCategoryAndStatus(String category, PostStatus status);
}
```

### 3.2 Custom Repository (Aggregation)

```java
public interface PostRepositoryCustom {
    List<CategoryStats> aggregateCategoryStats(PostStatus status);
    List<TagStats> aggregatePopularTags(PostStatus status, int limit);
    Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                       double halfLifeHours, int page, int size);
}

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<CategoryStats> aggregateCategoryStats(PostStatus status) {
        // $match → $group → $sort
        MatchOperation matchStage = Aggregation.match(
            Criteria.where("status").is(status.name())
                    .and("category").ne(null)
        );

        GroupOperation groupStage = Aggregation.group("category")
            .count().as("postCount")
            .max("publishedAt").as("latestPostDate");

        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");

        Aggregation aggregation = Aggregation.newAggregation(
            matchStage, groupStage, sortStage
        );

        return mongoTemplate.aggregate(aggregation, Post.class, CategoryStatsResult.class)
            .getMappedResults().stream()
            .map(r -> new CategoryStats(r.id(), r.postCount(), r.latestPostDate()))
            .toList();
    }

    @Override
    public List<TagStats> aggregatePopularTags(PostStatus status, int limit) {
        // $match → $unwind → $group → $sort → $limit
        MatchOperation matchStage = Aggregation.match(
            Criteria.where("status").is(status.name())
        );

        UnwindOperation unwindStage = Aggregation.unwind("tags");

        GroupOperation groupStage = Aggregation.group("tags")
            .count().as("postCount")
            .sum("viewCount").as("totalViews");

        SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "postCount");
        LimitOperation limitStage = Aggregation.limit(limit);

        Aggregation aggregation = Aggregation.newAggregation(
            matchStage, unwindStage, groupStage, sortStage, limitStage
        );

        return mongoTemplate.aggregate(aggregation, Post.class, TagStatsResult.class)
            .getMappedResults().stream()
            .map(r -> new TagStats(r.id(), r.postCount(), r.totalViews()))
            .toList();
    }
}
```

### 3.3 다른 Repository 패턴

```java
// CommentRepository.java
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);
    List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(String parentCommentId);
    long countByPostIdAndIsDeletedFalse(String postId);
}

// LikeRepository.java
public interface LikeRepository extends MongoRepository<Like, String> {
    Optional<Like> findByPostIdAndUserId(String postId, String userId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    Page<Like> findByPostId(String postId, Pageable pageable);
    long countByPostId(String postId);
}

// TagRepository.java
public interface TagRepository extends MongoRepository<Tag, String> {
    Optional<Tag> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    List<Tag> findByPostCountGreaterThanOrderByPostCountDesc(Long minCount, Pageable pageable);
    List<Tag> findByNameContainingIgnoreCaseOrderByPostCountDesc(String keyword, Pageable pageable);
}
```

---

## 4. 인덱스 전략

### 4.1 MongoConfig 인덱스 생성

```java
@Configuration
@RequiredArgsConstructor
public class MongoConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        createIndexes();
    }

    private void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");

        // 1. 전문 검색 인덱스
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
            .onField("title", 2.0f)       // 제목 가중치 2배
            .onField("content", 1.0f)
            .build();
        indexOps.createIndex(textIndex);

        // 2. 메인 페이지: status + publishedAt
        indexOps.createIndex(
            new Index()
                .on("status", Sort.Direction.ASC)
                .on("publishedAt", Sort.Direction.DESC)
        );

        // 3. 마이페이지: authorId + createdAt
        indexOps.createIndex(
            new Index()
                .on("authorId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
        );

        // 4. 카테고리 페이지: category + status + publishedAt
        indexOps.createIndex(
            new Index()
                .on("category", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("publishedAt", Sort.Direction.DESC)
        );

        // 5. 태그 검색 (Multikey)
        indexOps.createIndex(new Index().on("tags", Sort.Direction.ASC));

        // 6. 인기 게시물: status + viewCount + publishedAt
        indexOps.createIndex(
            new Index()
                .on("status", Sort.Direction.ASC)
                .on("viewCount", Sort.Direction.DESC)
                .on("publishedAt", Sort.Direction.DESC)
        );

        // 7. 레거시: productId
        indexOps.createIndex(new Index().on("productId", Sort.Direction.ASC));
    }
}
```

### 4.2 인덱스별 쿼리 매핑

| 인덱스 | 지원 쿼리 | Repository 메서드 |
|--------|----------|-------------------|
| Text(title, content) | 전문 검색 | `findByTextSearchAndStatus` |
| {status, publishedAt} | 메인 게시물 목록 | `findByStatusOrderByPublishedAtDesc` |
| {authorId, createdAt} | 마이페이지 | `findByAuthorIdOrderByCreatedAtDesc` |
| {category, status, publishedAt} | 카테고리별 | `findByCategoryAndStatusOrderByPublishedAtDesc` |
| {tags} | 태그 검색 | `findByTagsInAndStatusOrderByPublishedAtDesc` |
| {status, viewCount, publishedAt} | 인기 게시물 | `findByStatusOrderByViewCountDescPublishedAtDesc` |

---

## 5. 데이터 동기화 패턴

### 5.1 좋아요 토글 & 카운트 동기화

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    @Transactional
    public LikeToggleResponse toggleLike(String postId, String userId, String userName) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

        Like existingLike = likeRepository.findByPostIdAndUserId(postId, userId).orElse(null);

        boolean liked;
        if (existingLike != null) {
            // 좋아요 취소
            likeRepository.delete(existingLike);
            post.decrementLikeCount();       // Post 카운트 감소
            liked = false;
        } else {
            // 좋아요 추가
            Like newLike = Like.builder()
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .build();
            likeRepository.save(newLike);
            post.incrementLikeCount();       // Post 카운트 증가
            liked = true;
        }

        postRepository.save(post);           // 카운트 저장
        return LikeToggleResponse.of(liked, post.getLikeCount());
    }
}
```

### 5.2 댓글 생성 & 카운트 동기화

```java
@Transactional
public CommentResponse createComment(String postId, String userId, CommentCreateRequest request) {
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new CustomBusinessException(BlogErrorCode.POST_NOT_FOUND));

    Comment comment = Comment.builder()
        .postId(postId)
        .authorId(userId)
        .authorName(userName)
        .content(request.getContent())
        .parentCommentId(request.getParentCommentId())
        .createdAt(LocalDateTime.now())
        .build();

    comment = commentRepository.save(comment);

    // Post 댓글 수 증가 (역정규화 동기화)
    post.incrementCommentCount();
    postRepository.save(post);

    return CommentResponse.from(comment);
}
```

---

## 6. 쿼리 패턴 분석

### 6.1 게시물 상세 조회 (권한 포함)

```java
// PostRepository.java
@Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
Optional<Post> findByIdAndViewableBy(String postId, String userId);

// 설명:
// - 발행된 게시물: 누구나 조회 가능
// - 미발행 게시물: 작성자만 조회 가능
```

### 6.2 관련 게시물 추천

```java
@Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

// 설명:
// - 같은 카테고리 OR 공통 태그
// - 현재 게시물 제외
// - 발행된 것만
```

### 6.3 피드 (팔로잉 작성자)

```java
Page<Post> findByAuthorIdInAndStatusOrderByPublishedAtDesc(
    List<String> authorIds, PostStatus status, Pageable pageable);

// 사용:
// List<String> followingUserIds = getFollowingUserIds(currentUserId);
// Page<Post> feed = postRepository.findByAuthorIdInAndStatusOrderByPublishedAtDesc(
//     followingUserIds, PostStatus.PUBLISHED, pageable
// );
```

### 6.4 네비게이션 (이전/다음 게시물)

```java
// 이전 게시물
Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
    PostStatus status, LocalDateTime publishedAt);

// 다음 게시물
Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
    PostStatus status, LocalDateTime publishedAt);
```

---

## 7. 핵심 정리

### 7.1 모델링 패턴 요약

| Document | 전략 | 핵심 포인트 |
|----------|------|------------|
| **Post** | Hybrid | tags/images 내장, comments 참조, 카운터 역정규화 |
| **Comment** | Referencing | postId 참조, 대댓글 자기참조, Soft Delete |
| **Like** | 연결 테이블 | Compound Unique로 중복 방지, Post.likeCount 동기화 |
| **Tag** | 역정규화 | postCount로 인기 태그 빠른 조회 |
| **Series** | 배열 Embedding | postIds 순서 유지 |

### 7.2 인덱스 전략 요약

| 쿼리 패턴 | 인덱스 |
|-----------|--------|
| 전문 검색 | Text(title^2, content) |
| 메인 목록 | {status, publishedAt} |
| 작성자별 | {authorId, createdAt} |
| 카테고리별 | {category, status, publishedAt} |
| 태그 검색 | {tags} (Multikey) |
| 인기 게시물 | {status, viewCount, publishedAt} |

### 7.3 Repository 레이어

| 유형 | 사용 시점 |
|------|----------|
| **MongoRepository** | 단순 CRUD, 메서드 이름 쿼리 |
| **@Query** | 복잡한 조건, 프로젝션 |
| **MongoTemplate** | Aggregation Pipeline, 동적 쿼리 |

---

## 다음 학습

- [MongoDB Transactions](./mongodb-transactions.md)
- [MongoDB 인덱스](./mongodb-indexes.md)
- [MongoDB Aggregation](./mongodb-aggregation.md)

---

## 참고 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepositoryCustomImpl.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/config/MongoConfig.java`
