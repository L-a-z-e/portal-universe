# MongoDB Text Search

## 개요

Blog Service에서 사용하는 MongoDB 텍스트 인덱스와 전문 검색 기능을 학습합니다.

## 텍스트 인덱스 생성

### Post 엔티티 설정

```java
@Document(collection = "posts")
public class Post {
    @TextIndexed(weight = 2.0f)
    private String title;       // 제목: 가중치 2.0

    @TextIndexed
    private String content;     // 내용: 가중치 1.0 (기본값)
}
```

### MongoConfig에서 인덱스 생성

```java
@Configuration
public class MongoConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        createTextIndex();
    }

    private void createTextIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");

        // 복합 텍스트 인덱스 생성
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
            .onField("title", 2.0f)    // 가중치 2.0
            .onField("content", 1.0f)  // 가중치 1.0
            .build();

        indexOps.createIndex(textIndex);
    }
}
```

## 텍스트 검색 쿼리

### Repository 메서드

```java
public interface PostRepository extends MongoRepository<Post, String> {

    /**
     * 전문 검색 (제목 + 내용)
     * MongoDB Text Index 활용
     */
    @Query("{ $text: { $search: ?0 }, status: ?1 }")
    Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);
}
```

### MongoDB 쿼리 구문

```javascript
// 기본 텍스트 검색
db.posts.find({
    $text: { $search: "Vue.js tutorial" },
    status: "PUBLISHED"
})

// 관련도 점수 포함
db.posts.find(
    { $text: { $search: "Vue.js tutorial" } },
    { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

## 검색 구문

### 기본 검색

```javascript
// "vue" 또는 "react" 포함
{ $text: { $search: "vue react" } }
```

### 정확한 구문 검색

```javascript
// "vue.js tutorial" 정확히 포함
{ $text: { $search: "\"vue.js tutorial\"" } }
```

### 단어 제외

```javascript
// "vue" 포함, "angular" 제외
{ $text: { $search: "vue -angular" } }
```

## Service 구현

```java
@Service
public class PostServiceImpl implements PostService {

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

    @Override
    public Page<PostSummaryResponse> searchPostsAdvanced(PostSearchRequest searchRequest) {
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
        // 다른 필터 처리...
    }
}
```

## 가중치의 효과

```
검색어: "Vue"

문서 A: title="Vue.js 시작하기", content="React도 좋지만..."
→ 점수: 2.0 (제목에서 매칭)

문서 B: title="프론트엔드 개발", content="Vue와 React 비교"
→ 점수: 1.0 (내용에서만 매칭)

결과: 문서 A가 더 높은 순위
```

## 텍스트 검색 제한사항

### 제한

1. **컬렉션당 하나의 텍스트 인덱스만 가능**
2. **부분 일치 미지원** (전체 단어 매칭)
3. **정규식 조합 불가**
4. **한국어 형태소 분석 제한적**

### 대안 (Elasticsearch 고려)

| 기능 | MongoDB Text Search | Elasticsearch |
|------|---------------------|---------------|
| 부분 일치 | X | O |
| 형태소 분석 | 제한적 | 다양한 언어 지원 |
| 하이라이팅 | X | O |
| 자동완성 | X | O |
| 복잡한 쿼리 | 제한적 | O |

## 인덱스 확인

```bash
# MongoDB Shell에서 인덱스 확인
db.posts.getIndexes()

# 결과 예시
[
    { "v": 2, "key": { "_id": 1 }, "name": "_id_" },
    {
        "v": 2,
        "key": { "_fts": "text", "_ftsx": 1 },
        "name": "title_text_content_text",
        "weights": { "title": 2, "content": 1 },
        "default_language": "english",
        "language_override": "language"
    }
]
```

## 검색 API 엔드포인트

```java
@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
            ApiResponse.success(postService.searchPosts(keyword, page, size))
        );
    }
}
```

## 성능 고려사항

### 인덱스 크기

```
텍스트 인덱스는 일반 인덱스보다 크기가 큼
→ 메모리 사용량 증가
→ 필요한 필드만 인덱싱
```

### 쿼리 최적화

```java
// 텍스트 검색 + 다른 조건 결합
@Query("{ $text: { $search: ?0 }, status: ?1, category: ?2 }")
Page<Post> findByTextAndStatusAndCategory(
    String searchText,
    PostStatus status,
    String category,
    Pageable pageable
);
```

## 핵심 포인트

| 항목 | 설명 |
|------|------|
| 인덱스 생성 | @TextIndexed 또는 TextIndexDefinition |
| 가중치 | 제목 2.0, 내용 1.0 |
| 검색 구문 | "구문 검색", -제외어 |
| 제한 | 컬렉션당 1개, 부분 일치 미지원 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/config/MongoConfig.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/repository/PostRepository.java`
