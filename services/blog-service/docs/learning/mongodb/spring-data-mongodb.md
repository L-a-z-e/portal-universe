# Spring Data MongoDB Repository 패턴

## 개요

Blog Service에서 사용하는 Spring Data MongoDB Repository 패턴과 커스텀 쿼리 구현을 학습합니다.

## Repository 계층 구조

```
PostRepository (Interface)
├── extends MongoRepository<Post, String>    // 기본 CRUD
└── extends PostRepositoryCustom             // 커스텀 쿼리

PostRepositoryCustom (Interface)
└── PostRepositoryCustomImpl (Implementation) // MongoTemplate 사용
```

## MongoRepository 기본 메서드

```java
public interface MongoRepository<T, ID> {
    // CRUD
    <S extends T> S save(S entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(T entity);
    void deleteById(ID id);
    long count();

    // 페이징
    Page<T> findAll(Pageable pageable);
}
```

## 메서드 이름 기반 쿼리

### 규칙

```
find + By + 필드명 + 조건 + OrderBy + 필드명 + 정렬방향
```

### 예시

```java
public interface PostRepository extends MongoRepository<Post, String> {

    // SELECT * FROM posts WHERE status = ? ORDER BY publishedAt DESC
    Page<Post> findByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    // SELECT * FROM posts WHERE authorId = ? ORDER BY createdAt DESC
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    // SELECT * FROM posts WHERE authorId = ? AND status = ?
    Page<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(
        String authorId, PostStatus status, Pageable pageable);

    // SELECT * FROM posts WHERE category = ? AND status = ?
    Page<Post> findByCategoryAndStatusOrderByPublishedAtDesc(
        String category, PostStatus status, Pageable pageable);

    // SELECT * FROM posts WHERE tags IN (?)
    Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(
        List<String> tags, PostStatus status, Pageable pageable);

    // SELECT COUNT(*) FROM posts WHERE authorId = ? AND status = ?
    long countByAuthorIdAndStatus(String authorId, PostStatus status);

    // SELECT * FROM posts WHERE status = ? AND publishedAt > ?
    Page<Post> findByStatusAndPublishedAtAfterOrderByPublishedAtDesc(
        PostStatus status, LocalDateTime since, Pageable pageable);

    // 첫 번째 결과만 (이전/다음 게시물용)
    Optional<Post> findFirstByStatusAndPublishedAtLessThanOrderByPublishedAtDesc(
        PostStatus status, LocalDateTime publishedAt);
}
```

### 키워드 정리

| 키워드 | 설명 | 예시 |
|--------|------|------|
| And | AND 조건 | findByNameAndAge |
| Or | OR 조건 | findByNameOrAge |
| Is, Equals | 같음 | findByName, findByNameIs |
| Between | 범위 | findByAgeBetween |
| LessThan | 작음 | findByAgeLessThan |
| GreaterThan | 큼 | findByAgeGreaterThan |
| Like | 패턴 매칭 | findByNameLike |
| In | 포함 | findByNameIn |
| OrderBy | 정렬 | findByNameOrderByAgeDesc |
| First | 첫 번째만 | findFirstByName |

## @Query 어노테이션

### 기본 사용

```java
// 텍스트 검색
@Query("{ $text: { $search: ?0 }, status: ?1 }")
Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

// 복합 조건
@Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
Optional<Post> findByIdAndViewableBy(String postId, String userId);

// 관련 게시물 (OR 조건)
@Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);
```

### 필드 선택 (Projection)

```java
@Query(value = "{ status: ?0 }", fields = "{ category: 1 }")
List<String> findDistinctCategoriesByStatus(PostStatus status);
```

## Custom Repository 구현

### 인터페이스 정의

```java
public interface PostRepositoryCustom {
    List<CategoryStats> aggregateCategoryStats(PostStatus status);
    List<TagStats> aggregatePopularTags(PostStatus status, int limit);
    Page<Post> aggregateTrendingPosts(PostStatus status, LocalDateTime startDate,
                                       double halfLifeHours, int page, int size);
}
```

### 구현체 (MongoTemplate 사용)

```java
@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<CategoryStats> aggregateCategoryStats(PostStatus status) {
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

        AggregationResults<CategoryStatsResult> results = mongoTemplate.aggregate(
            aggregation, Post.class, CategoryStatsResult.class
        );

        return results.getMappedResults().stream()
            .map(r -> new CategoryStats(r.id(), r.postCount(), r.latestPostDate()))
            .toList();
    }
}
```

## MongoTemplate vs MongoRepository

| 기능 | MongoRepository | MongoTemplate |
|------|----------------|---------------|
| 기본 CRUD | O | O |
| 메서드 이름 쿼리 | O | X |
| Aggregation | 제한적 | O |
| 벌크 연산 | X | O |
| 복잡한 쿼리 | 제한적 | O |
| 원자적 업데이트 | 제한적 | O |

## 페이징 처리

### Pageable 생성

```java
// 기본 페이징
Pageable pageable = PageRequest.of(page, size);

// 정렬 포함
Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));

// 복합 정렬
Pageable pageable = PageRequest.of(page, size,
    Sort.by(Sort.Direction.DESC, "viewCount")
        .and(Sort.by(Sort.Direction.DESC, "publishedAt"))
);
```

### Page 결과 사용

```java
Page<Post> postPage = postRepository.findByStatusOrderByPublishedAtDesc(
    PostStatus.PUBLISHED, pageable);

// Page 정보
postPage.getContent();        // 결과 목록
postPage.getTotalElements();  // 전체 개수
postPage.getTotalPages();     // 전체 페이지 수
postPage.getNumber();         // 현재 페이지 (0-based)
postPage.getSize();           // 페이지 크기
postPage.hasNext();           // 다음 페이지 존재 여부
postPage.hasPrevious();       // 이전 페이지 존재 여부

// 변환
Page<PostSummaryResponse> responsePage = postPage.map(this::convertToPostListResponse);
```

## 동적 쿼리 (Criteria API)

```java
public List<Post> searchPosts(PostSearchCriteria criteria) {
    Query query = new Query();

    if (criteria.getStatus() != null) {
        query.addCriteria(Criteria.where("status").is(criteria.getStatus()));
    }

    if (criteria.getAuthorId() != null) {
        query.addCriteria(Criteria.where("authorId").is(criteria.getAuthorId()));
    }

    if (criteria.getCategory() != null) {
        query.addCriteria(Criteria.where("category").is(criteria.getCategory()));
    }

    if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
        query.addCriteria(Criteria.where("tags").in(criteria.getTags()));
    }

    query.with(Sort.by(Sort.Direction.DESC, "publishedAt"));
    query.with(PageRequest.of(criteria.getPage(), criteria.getSize()));

    return mongoTemplate.find(query, Post.class);
}
```

## 핵심 포인트

| 패턴 | 사용 시나리오 |
|------|--------------|
| 메서드 이름 쿼리 | 단순 조회 |
| @Query | 복잡한 조건 |
| Custom + MongoTemplate | Aggregation, 벌크 연산 |
| Criteria API | 동적 쿼리 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepositoryCustom.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepositoryCustomImpl.java`
