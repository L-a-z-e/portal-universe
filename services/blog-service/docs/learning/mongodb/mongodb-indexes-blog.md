# MongoDB Indexes for Blog Service

## 개요

Blog Service의 쿼리 최적화를 위한 MongoDB 인덱스 설계와 관리를 학습합니다.

## 인덱스 생성 코드

### MongoConfig.java

```java
@Configuration
@RequiredArgsConstructor
public class MongoConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        createIndexes();
    }

    private void createIndexes() {
        IndexOperations indexOps = mongoTemplate.indexOps("posts");

        // 1. 전문 검색 인덱스
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
            .onField("title", 2.0f)
            .onField("content", 1.0f)
            .build();
        indexOps.createIndex(textIndex);

        // 2. 발행된 게시물 조회 (메인 페이지)
        indexOps.createIndex(new Index()
            .on("status", Sort.Direction.ASC)
            .on("publishedAt", Sort.Direction.DESC)
        );

        // 3. 작성자별 조회 (마이페이지)
        indexOps.createIndex(new Index()
            .on("authorId", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.DESC)
        );

        // 4. 카테고리별 조회
        indexOps.createIndex(new Index()
            .on("category", Sort.Direction.ASC)
            .on("status", Sort.Direction.ASC)
            .on("publishedAt", Sort.Direction.DESC)
        );

        // 5. 태그 검색
        indexOps.createIndex(new Index()
            .on("tags", Sort.Direction.ASC)
        );

        // 6. 인기 게시물 (조회수 정렬)
        indexOps.createIndex(new Index()
            .on("status", Sort.Direction.ASC)
            .on("viewCount", Sort.Direction.DESC)
            .on("publishedAt", Sort.Direction.DESC)
        );

        // 7. 상품 연관 (하위 호환성)
        indexOps.createIndex(new Index()
            .on("productId", Sort.Direction.ASC)
        );
    }
}
```

## 인덱스 유형별 설명

### 1. 텍스트 인덱스 (Text Index)

```java
TextIndexDefinition textIndex = TextIndexDefinition.builder()
    .onField("title", 2.0f)   // 가중치 2.0
    .onField("content", 1.0f) // 가중치 1.0
    .build();
```

**용도:** 전문 검색 (`$text: { $search: "키워드" }`)

**제한:**
- 컬렉션당 1개만 가능
- 부분 일치 미지원

### 2. 단일 필드 인덱스

```java
@Indexed
private String authorId;

@Indexed
private PostStatus status;
```

**용도:** 단일 필드 조회

### 3. 복합 인덱스 (Compound Index)

```java
new Index()
    .on("status", Sort.Direction.ASC)
    .on("publishedAt", Sort.Direction.DESC)
```

**용도:** 여러 필드 조합 쿼리

**인덱스 프리픽스 규칙:**
```
인덱스: { status: 1, publishedAt: -1 }

사용 가능:
- find({ status: "PUBLISHED" })
- find({ status: "PUBLISHED" }).sort({ publishedAt: -1 })

사용 불가:
- find({}).sort({ publishedAt: -1 })  // status 없음
```

### 4. 유니크 인덱스

```java
// Tag.java
@Indexed(unique = true)
private String name;

// Like.java - 복합 유니크
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true
)
```

**용도:** 중복 값 방지

### 5. 배열 필드 인덱스 (Multikey Index)

```java
new Index().on("tags", Sort.Direction.ASC)
```

**용도:** 배열 필드 검색

```javascript
// 자동으로 Multikey Index 생성
// tags: ["vue", "react", "javascript"] 각각 인덱싱
db.posts.find({ tags: "vue" })  // 인덱스 사용
```

## 쿼리별 인덱스 매핑

| 쿼리 | 사용 인덱스 |
|------|------------|
| `findByStatusOrderByPublishedAtDesc` | status + publishedAt |
| `findByAuthorIdOrderByCreatedAtDesc` | authorId + createdAt |
| `findByCategoryAndStatusOrderByPublishedAtDesc` | category + status + publishedAt |
| `findByTagsInAndStatus` | tags |
| `findByTextSearchAndStatus` | text index + status |
| `findByStatusOrderByViewCountDesc` | status + viewCount + publishedAt |

## 인덱스 확인 방법

### MongoDB Shell

```javascript
// 인덱스 목록 확인
db.posts.getIndexes()

// 쿼리 실행 계획 확인
db.posts.find({ status: "PUBLISHED" })
    .sort({ publishedAt: -1 })
    .explain("executionStats")

// 결과에서 확인할 항목:
// - winningPlan.inputStage.stage: "IXSCAN" (인덱스 스캔)
// - totalDocsExamined: 스캔한 문서 수
// - executionTimeMillis: 실행 시간
```

## 인덱스 선택 가이드라인

### ESR 규칙 (Equality, Sort, Range)

```
인덱스 필드 순서: Equality → Sort → Range

예시:
쿼리: find({ status: "PUBLISHED", publishedAt: { $gte: date } }).sort({ viewCount: -1 })

최적 인덱스: { status: 1, viewCount: -1, publishedAt: 1 }
             (Equality)  (Sort)        (Range)
```

### 선택성 (Selectivity)

```
선택성 높은 필드를 앞에 배치

예시:
- authorId: 선택성 높음 (특정 사용자)
- status: 선택성 낮음 (PUBLISHED가 대부분)

좋은 예: { authorId: 1, status: 1 }
나쁜 예: { status: 1, authorId: 1 }
```

## 인덱스 비용

### 메모리 사용

```
인덱스도 메모리에 로드되어야 빠름
→ 인덱스가 많으면 메모리 부족 가능
→ 필요한 인덱스만 생성
```

### 쓰기 성능

```
문서 삽입/업데이트 시 모든 관련 인덱스 업데이트
→ 인덱스가 많으면 쓰기 성능 저하
→ 읽기/쓰기 비율 고려
```

## 인덱스 모니터링

### 사용되지 않는 인덱스 확인

```javascript
// 인덱스 사용 통계
db.posts.aggregate([
    { $indexStats: {} }
])

// accesses.ops가 0이면 미사용
```

## Blog Service 인덱스 요약

| 컬렉션 | 인덱스 | 용도 |
|--------|--------|------|
| posts | text (title, content) | 전문 검색 |
| posts | status + publishedAt | 메인 목록 |
| posts | authorId + createdAt | 작성자별 목록 |
| posts | category + status + publishedAt | 카테고리별 |
| posts | tags (multikey) | 태그 검색 |
| posts | status + viewCount + publishedAt | 인기 글 |
| likes | postId + userId (unique) | 중복 좋아요 방지 |
| tags | name (unique) | 태그 중복 방지 |

## 관련 파일

- `/services/blog-service/src/main/java/com/portal/universe/blogservice/config/MongoConfig.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/post/domain/Post.java`
- `/services/blog-service/src/main/java/com/portal/universe/blogservice/like/domain/Like.java`
