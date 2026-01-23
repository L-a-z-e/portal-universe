# MongoDB Spring Data 통합

## 학습 목표
- Spring Data MongoDB 설정 방법 이해
- MongoRepository와 MongoTemplate 활용법 습득
- Portal Universe Blog Service의 실제 구현 패턴 학습

---

## 1. 의존성 설정

### build.gradle

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
}
```

---

## 2. 설정

### 2.1 application.yml

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://laze:password@localhost:27017/blog_db?authSource=admin
```

### 2.2 환경별 설정

```yaml
# application-local.yml
spring.data.mongodb.uri: mongodb://laze:password@localhost:27017/blog_db?authSource=admin

# application-docker.yml
spring.data.mongodb.uri: mongodb://laze:password@mongodb:27017/blog_db?authSource=admin

# application-kubernetes.yml
spring.data.mongodb.uri: mongodb://laze:password@mongodb:27017/blog_db?authSource=admin
```

### 2.3 MongoConfig.java

```java
@Configuration
@EnableMongoAuditing  // @CreatedDate, @LastModifiedDate 활성화
public class MongoConfig implements InitializingBean {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        // 애플리케이션 시작 시 인덱스 생성
        createIndexes();
    }

    private void createIndexes() {
        // 텍스트 인덱스 (제목 가중치 2.0, 내용 가중치 1.0)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
            .onField("title", 2.0F)
            .onField("content", 1.0F)
            .build();
        mongoTemplate.indexOps(Post.class).ensureIndex(textIndex);

        // 복합 인덱스: 상태 + 발행일 (메인 페이지용)
        mongoTemplate.indexOps(Post.class).ensureIndex(
            new Index()
                .on("status", Sort.Direction.ASC)
                .on("publishedAt", Sort.Direction.DESC)
        );

        // 복합 인덱스: 작성자 + 생성일 (마이페이지용)
        mongoTemplate.indexOps(Post.class).ensureIndex(
            new Index()
                .on("authorId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
        );

        // 복합 인덱스: 카테고리 + 상태 + 발행일
        mongoTemplate.indexOps(Post.class).ensureIndex(
            new Index()
                .on("category", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("publishedAt", Sort.Direction.DESC)
        );

        log.info("MongoDB indexes created successfully");
    }
}
```

---

## 3. Document 클래스 정의

### 3.1 Post Document

```java
@Document(collection = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post {

    @Id
    private String id;

    @TextIndexed(weight = 2.0f)
    private String title;

    @TextIndexed
    private String content;

    private String summary;

    @Indexed
    private String authorId;

    private String authorName;

    @Indexed
    private PostStatus status;

    @Indexed
    private Set<String> tags;

    @Indexed
    private String category;

    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long commentCount = 0L;

    @Indexed
    private LocalDateTime publishedAt;

    private String metaDescription;
    private String thumbnailUrl;

    @Builder.Default
    private List<String> images = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 비즈니스 메서드
    public void publish() {
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void updateLikeCount(long delta) {
        this.likeCount += delta;
    }

    public void updateCommentCount(long delta) {
        this.commentCount += delta;
    }
}
```

### 3.2 어노테이션 설명

| 어노테이션 | 설명 |
|-----------|------|
| `@Document` | MongoDB 컬렉션과 매핑 |
| `@Id` | 문서 고유 식별자 (_id) |
| `@Indexed` | 단일 필드 인덱스 생성 |
| `@TextIndexed` | 텍스트 검색 인덱스 (가중치 설정 가능) |
| `@CompoundIndex` | 복합 인덱스 정의 |
| `@CreatedDate` | 생성 시간 자동 설정 |
| `@LastModifiedDate` | 수정 시간 자동 갱신 |

### 3.3 Like Document (복합 인덱스 예시)

```java
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {

    @Id
    private String id;

    @Indexed
    private String postId;

    @Indexed
    private String userId;

    private String userName;

    @CreatedDate
    private LocalDateTime createdAt;
}
```

---

## 4. MongoRepository 인터페이스

### 4.1 기본 Repository

```java
public interface PostRepository extends MongoRepository<Post, String>,
                                        PostRepositoryCustom {

    // 메서드 이름 기반 쿼리
    Page<Post> findByStatusOrderByPublishedAtDesc(
        PostStatus status,
        Pageable pageable
    );

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(
        String authorId,
        Pageable pageable
    );

    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(
        String authorId,
        PostStatus status,
        Pageable pageable
    );

    // 태그 검색 (다중 태그 OR 조건)
    Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(
        List<String> tags,
        PostStatus status,
        Pageable pageable
    );

    // 인기 게시물 (조회수 기준)
    Page<Post> findByStatusOrderByViewCountDescPublishedAtDesc(
        PostStatus status,
        Pageable pageable
    );

    // 통계
    long countByAuthorIdAndStatus(String authorId, PostStatus status);

    // 피드 (팔로우 사용자 게시물)
    Page<Post> findByAuthorIdInAndStatusOrderByPublishedAtDesc(
        List<String> authorIds,
        PostStatus status,
        Pageable pageable
    );
}
```

### 4.2 @Query 어노테이션

```java
public interface PostRepository extends MongoRepository<Post, String> {

    // 전문 검색 (MongoDB Text Index)
    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(
        String searchText,
        PostStatus status,
        Pageable pageable
    );

    // 권한 기반 조회 (PUBLISHED 또는 본인 글만)
    @Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
    Optional<Post> findByIdAndViewableBy(String postId, String userId);

    // 날짜 범위 검색
    @Query("{ status: ?0, publishedAt: { $gte: ?1, $lte: ?2 } }")
    List<Post> findByStatusAndPublishedAtBetween(
        PostStatus status,
        LocalDateTime start,
        LocalDateTime end
    );
}
```

### 4.3 네비게이션 쿼리 (이전/다음 게시물)

```java
public interface PostRepository extends MongoRepository<Post, String> {

    // 이전 게시물
    Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
        PostStatus status,
        LocalDateTime publishedAt
    );

    // 다음 게시물
    Optional<Post> findFirstByStatusAndPublishedAtGreaterThanOrderByPublishedAtAsc(
        PostStatus status,
        LocalDateTime publishedAt
    );
}
```

---

## 5. Custom Repository (Aggregation)

### 5.1 인터페이스 정의

```java
public interface PostRepositoryCustom {

    List<CategoryStats> aggregateCategoryStats(PostStatus status);

    List<TagStats> aggregatePopularTags(PostStatus status, int limit);

    Page<Post> aggregateTrendingPosts(
        PostStatus status,
        LocalDateTime startDate,
        double halfLifeHours,
        int page,
        int size
    );
}
```

### 5.2 구현체

```java
@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    /**
     * 카테고리별 통계 집계
     * Pipeline: $match → $group → $sort
     */
    @Override
    public List<CategoryStats> aggregateCategoryStats(PostStatus status) {
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 필터링
            Aggregation.match(
                Criteria.where("status").is(status)
                        .and("category").ne(null)
            ),
            // 2. 그룹핑
            Aggregation.group("category")
                .count().as("postCount")
                .max("publishedAt").as("latestPostDate"),
            // 3. 정렬
            Aggregation.sort(Sort.Direction.DESC, "postCount")
        );

        return mongoTemplate.aggregate(
            aggregation,
            Post.class,
            CategoryStats.class
        ).getMappedResults();
    }

    /**
     * 인기 태그 집계
     * Pipeline: $match → $unwind → $group → $sort → $limit
     */
    @Override
    public List<TagStats> aggregatePopularTags(PostStatus status, int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            // 1. 발행된 게시물만
            Aggregation.match(Criteria.where("status").is(status)),
            // 2. 태그 배열 펼치기
            Aggregation.unwind("tags"),
            // 3. 태그별 집계
            Aggregation.group("tags")
                .count().as("postCount")
                .sum("viewCount").as("totalViews"),
            // 4. 정렬 및 제한
            Aggregation.sort(Sort.Direction.DESC, "postCount"),
            Aggregation.limit(limit)
        );

        return mongoTemplate.aggregate(
            aggregation,
            Post.class,
            TagStats.class
        ).getMappedResults();
    }

    /**
     * 트렌딩 게시물 (시간 감쇠 알고리즘)
     * 점수 = (조회수 + 좋아요×3 + 댓글×5) × 2^(-경과시간/반감기)
     */
    @Override
    public Page<Post> aggregateTrendingPosts(
            PostStatus status,
            LocalDateTime startDate,
            double halfLifeHours,
            int page,
            int size) {

        // 시간 감쇠 계산을 위한 AggregationExpression
        AggregationExpression trendingScore = context -> {
            Document baseScore = new Document("$add", Arrays.asList(
                "$viewCount",
                new Document("$multiply", Arrays.asList("$likeCount", 3)),
                new Document("$multiply", Arrays.asList("$commentCount", 5))
            ));

            Document hoursElapsed = new Document("$divide", Arrays.asList(
                new Document("$subtract", Arrays.asList(
                    new Date(),
                    "$publishedAt"
                )),
                3600000.0  // ms → hours
            ));

            Document timeDecay = new Document("$pow", Arrays.asList(
                2.0,
                new Document("$divide", Arrays.asList(
                    new Document("$multiply", Arrays.asList(hoursElapsed, -1)),
                    halfLifeHours
                ))
            ));

            return new Document("$multiply", Arrays.asList(baseScore, timeDecay));
        };

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where("status").is(status)
                        .and("publishedAt").gte(startDate)
            ),
            Aggregation.addFields()
                .addField("trendingScore").withValue(trendingScore)
                .build(),
            Aggregation.sort(Sort.Direction.DESC, "trendingScore"),
            Aggregation.skip((long) page * size),
            Aggregation.limit(size)
        );

        List<Post> results = mongoTemplate.aggregate(
            aggregation,
            Post.class,
            Post.class
        ).getMappedResults();

        // 총 개수 조회
        long total = mongoTemplate.count(
            Query.query(
                Criteria.where("status").is(status)
                        .and("publishedAt").gte(startDate)
            ),
            Post.class
        );

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }
}
```

### 5.3 DTO 클래스

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStats {
    @Field("_id")
    private String category;
    private Long postCount;
    private LocalDateTime latestPostDate;
}

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TagStats {
    @Field("_id")
    private String tag;
    private Long postCount;
    private Long totalViews;
}
```

---

## 6. Service 계층 구현

### 6.1 PostServiceImpl

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public PostDetailResponse getPost(String postId, String currentUserId) {
        Post post = postRepository.findByIdAndViewableBy(postId, currentUserId)
            .orElseThrow(() -> new CustomBusinessException(POST_NOT_FOUND));

        // 조회수 증가 (비동기 처리 권장)
        post.incrementViewCount();
        postRepository.save(post);

        return PostDetailResponse.from(post);
    }

    @Override
    public Page<PostSummaryResponse> getPublishedPosts(Pageable pageable) {
        return postRepository
            .findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, pageable)
            .map(PostSummaryResponse::from);
    }

    @Override
    @Transactional
    public PostDetailResponse createPost(PostCreateRequest request, String userId) {
        Post post = Post.builder()
            .title(request.getTitle())
            .content(request.getContent())
            .summary(request.getSummary())
            .authorId(userId)
            .authorName(request.getAuthorName())
            .status(PostStatus.DRAFT)
            .tags(request.getTags())
            .category(request.getCategory())
            .build();

        Post saved = postRepository.save(post);
        return PostDetailResponse.from(saved);
    }

    @Override
    @Transactional
    public PostDetailResponse publishPost(String postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomBusinessException(POST_NOT_FOUND));

        if (!post.getAuthorId().equals(userId)) {
            throw new CustomBusinessException(UNAUTHORIZED_ACCESS);
        }

        post.publish();
        Post saved = postRepository.save(post);
        return PostDetailResponse.from(saved);
    }

    @Override
    public List<CategoryStats> getCategoryStats() {
        return postRepository.aggregateCategoryStats(PostStatus.PUBLISHED);
    }

    @Override
    public List<TagStats> getPopularTags(int limit) {
        return postRepository.aggregatePopularTags(PostStatus.PUBLISHED, limit);
    }

    @Override
    public Page<PostSummaryResponse> getTrendingPosts(Pageable pageable) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        double halfLifeHours = 48.0;  // 48시간 반감기

        return postRepository.aggregateTrendingPosts(
            PostStatus.PUBLISHED,
            startDate,
            halfLifeHours,
            pageable.getPageNumber(),
            pageable.getPageSize()
        ).map(PostSummaryResponse::from);
    }
}
```

---

## 7. MongoTemplate 직접 사용

### 7.1 동적 쿼리

```java
@Repository
@RequiredArgsConstructor
public class PostSearchRepository {

    private final MongoTemplate mongoTemplate;

    public List<Post> searchPosts(PostSearchCondition condition) {
        Query query = new Query();

        // 동적 조건 추가
        if (condition.getKeyword() != null) {
            query.addCriteria(
                new TextCriteria().matching(condition.getKeyword())
            );
        }

        if (condition.getCategory() != null) {
            query.addCriteria(
                Criteria.where("category").is(condition.getCategory())
            );
        }

        if (condition.getTags() != null && !condition.getTags().isEmpty()) {
            query.addCriteria(
                Criteria.where("tags").in(condition.getTags())
            );
        }

        if (condition.getAuthorId() != null) {
            query.addCriteria(
                Criteria.where("authorId").is(condition.getAuthorId())
            );
        }

        // 발행된 게시물만
        query.addCriteria(Criteria.where("status").is(PostStatus.PUBLISHED));

        // 정렬 및 페이지네이션
        query.with(Sort.by(Sort.Direction.DESC, "publishedAt"));
        query.skip((long) condition.getPage() * condition.getSize());
        query.limit(condition.getSize());

        return mongoTemplate.find(query, Post.class);
    }
}
```

### 7.2 부분 업데이트

```java
@Service
@RequiredArgsConstructor
public class PostUpdateService {

    private final MongoTemplate mongoTemplate;

    /**
     * 조회수만 증가 (전체 문서 로드 없이)
     */
    public void incrementViewCount(String postId) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Post.class);
    }

    /**
     * 좋아요 수 업데이트
     */
    public void updateLikeCount(String postId, long delta) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().inc("likeCount", delta);
        mongoTemplate.updateFirst(query, update, Post.class);
    }

    /**
     * 태그 추가
     */
    public void addTag(String postId, String tag) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().addToSet("tags", tag);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
}
```

---

## 8. 트랜잭션 처리

### 8.1 Multi-Document Transaction

```java
@Service
@RequiredArgsConstructor
public class LikeService {

    private final MongoTemplate mongoTemplate;

    /**
     * 좋아요 토글 (트랜잭션으로 일관성 보장)
     */
    @Transactional
    public boolean toggleLike(String postId, String userId, String userName) {
        Optional<Like> existingLike = findLike(postId, userId);

        if (existingLike.isPresent()) {
            // 좋아요 취소
            mongoTemplate.remove(existingLike.get());
            updatePostLikeCount(postId, -1);
            return false;
        } else {
            // 좋아요 추가
            Like like = Like.builder()
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .build();
            mongoTemplate.save(like);
            updatePostLikeCount(postId, 1);
            return true;
        }
    }

    private void updatePostLikeCount(String postId, long delta) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().inc("likeCount", delta);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
}
```

---

## 9. 핵심 정리

| 컴포넌트 | 용도 |
|----------|------|
| `@Document` | MongoDB 컬렉션 매핑 |
| `MongoRepository` | 기본 CRUD + 메서드 쿼리 |
| `@Query` | MongoDB 쿼리 직접 작성 |
| `MongoTemplate` | 동적 쿼리, 부분 업데이트 |
| `Aggregation` | Pipeline 집계 연산 |
| `@Transactional` | Multi-Document 트랜잭션 |

---

## 다음 학습

- [MongoDB Aggregation 심화](./mongodb-aggregation.md)
- [MongoDB 인덱스 최적화](./mongodb-indexes.md)
- [Portal Universe Blog 분석](./mongodb-portal-universe.md)

---

## 참고 자료

- [Spring Data MongoDB 공식 문서](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [MongoDB Query Operators](https://www.mongodb.com/docs/manual/reference/operator/query/)
- [Aggregation Pipeline Operators](https://www.mongodb.com/docs/manual/reference/operator/aggregation/)
