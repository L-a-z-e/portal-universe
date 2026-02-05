# MongoDB 인덱스

## 학습 목표
- Single Field, Compound Index 이해
- Text Index로 전문 검색 구현
- TTL Index로 자동 만료 처리
- Partial Index로 선택적 인덱싱
- Portal Universe의 인덱스 전략 분석

---

## 1. 인덱스 기초

### 1.1 인덱스란?

인덱스는 **쿼리 성능을 향상**시키는 데이터 구조입니다. 책의 색인처럼 전체 데이터를 스캔하지 않고 필요한 문서를 빠르게 찾습니다.

```
인덱스 없이 조회: 전체 컬렉션 스캔 (COLLSCAN)
인덱스 사용 조회: 인덱스 스캔 (IXSCAN) → 문서 조회
```

### 1.2 인덱스 구조 (B-Tree)

MongoDB는 **B-Tree** 구조를 사용합니다.

```
           [M]
          /   \
      [D,H]   [Q,U]
      / | \   / | \
   [A-C][E-G][I-L][N-P][R-T][V-Z]
```

- 정렬된 순서 유지
- O(log n) 검색 복잡도
- 범위 쿼리 효율적

### 1.3 _id 인덱스

모든 컬렉션은 `_id` 필드에 **자동으로 고유 인덱스**가 생성됩니다.

```javascript
// 자동 생성됨
{ "_id": 1 }  // unique index
```

---

## 2. Single Field Index

### 2.1 기본 생성

```javascript
// 오름차순 인덱스
db.posts.createIndex({ authorId: 1 })

// 내림차순 인덱스
db.posts.createIndex({ createdAt: -1 })
```

**Spring Data MongoDB:**

```java
// 어노테이션 방식
@Document(collection = "posts")
public class Post {
    @Indexed
    private String authorId;

    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime publishedAt;
}
```

### 2.2 고유 인덱스 (Unique)

```javascript
// 중복 값 허용 안 함
db.users.createIndex({ email: 1 }, { unique: true })

// 에러 발생 예시
db.users.insertOne({ email: "test@example.com" })  // OK
db.users.insertOne({ email: "test@example.com" })  // Error: duplicate key
```

**Spring Data MongoDB:**

```java
@Document(collection = "tags")
public class Tag {
    @Indexed(unique = true)
    private String name;
}
```

### 2.3 Sparse Index

null 값이 있는 문서는 인덱싱에서 제외합니다.

```javascript
db.posts.createIndex(
  { productId: 1 },
  { sparse: true }  // productId가 null인 문서 제외
)

// sparse + unique: null 중복 허용
db.users.createIndex(
  { phoneNumber: 1 },
  { unique: true, sparse: true }  // 전화번호 없는 사용자 여러 명 가능
)
```

---

## 3. Compound Index (복합 인덱스)

### 3.1 생성

여러 필드를 조합한 인덱스입니다.

```javascript
// status로 필터, publishedAt으로 정렬
db.posts.createIndex({ status: 1, publishedAt: -1 })

// 3개 필드 복합 인덱스
db.posts.createIndex({
  category: 1,
  status: 1,
  publishedAt: -1
})
```

### 3.2 Prefix Rule

복합 인덱스는 **왼쪽 접두사** 쿼리에 사용 가능합니다.

```javascript
// 인덱스: { a: 1, b: 1, c: 1 }

// 인덱스 사용 가능
db.coll.find({ a: 1 })                    // ✅ prefix: a
db.coll.find({ a: 1, b: 2 })              // ✅ prefix: a, b
db.coll.find({ a: 1, b: 2, c: 3 })        // ✅ prefix: a, b, c

// 인덱스 사용 불가
db.coll.find({ b: 2 })                    // ❌ a 없음
db.coll.find({ c: 3 })                    // ❌ a, b 없음
db.coll.find({ b: 2, c: 3 })              // ❌ a 없음
```

### 3.3 ESR 규칙 (Equality, Sort, Range)

복합 인덱스 필드 순서 최적화:

1. **E**quality (등호 조건) - 가장 먼저
2. **S**ort (정렬 필드) - 그 다음
3. **R**ange (범위 조건) - 마지막

```javascript
// 쿼리: status = 'PUBLISHED' AND publishedAt >= date ORDER BY viewCount DESC

// Good: ESR 순서
db.posts.createIndex({ status: 1, viewCount: -1, publishedAt: -1 })
//                     Equality    Sort           Range

// 결과: 인덱스만으로 정렬 가능 (in-memory sort 불필요)
```

### 3.4 Portal Universe 복합 인덱스

```java
// MongoConfig.java
private void createIndexes() {
    IndexOperations indexOps = mongoTemplate.indexOps("posts");

    // 발행된 게시물 조회 최적화 (메인 페이지)
    // 쿼리: status = 'PUBLISHED' ORDER BY publishedAt DESC
    indexOps.createIndex(
        new Index()
            .on("status", Sort.Direction.ASC)
            .on("publishedAt", Sort.Direction.DESC)
    );

    // 카테고리별 조회 최적화
    // 쿼리: category = ? AND status = 'PUBLISHED' ORDER BY publishedAt DESC
    indexOps.createIndex(
        new Index()
            .on("category", Sort.Direction.ASC)
            .on("status", Sort.Direction.ASC)
            .on("publishedAt", Sort.Direction.DESC)
    );

    // 인기 게시물 조회 최적화
    // 쿼리: status = 'PUBLISHED' ORDER BY viewCount DESC, publishedAt DESC
    indexOps.createIndex(
        new Index()
            .on("status", Sort.Direction.ASC)
            .on("viewCount", Sort.Direction.DESC)
            .on("publishedAt", Sort.Direction.DESC)
    );
}
```

### 3.5 Compound Unique Index

```java
// Like.java - 중복 좋아요 방지
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true  // (postId, userId) 조합이 유일
)
public class Like {
    @Indexed
    private String postId;

    @Indexed
    private String userId;
}
```

---

## 4. Text Index (전문 검색)

### 4.1 생성

```javascript
// 단일 필드
db.posts.createIndex({ title: "text" })

// 복수 필드 (가중치 설정)
db.posts.createIndex(
  { title: "text", content: "text" },
  { weights: { title: 2, content: 1 } }  // title 매칭이 2배 중요
)

// 와일드카드 (모든 문자열 필드)
db.posts.createIndex({ "$**": "text" })
```

### 4.2 검색

```javascript
// 기본 검색 (OR 조건)
db.posts.find({ $text: { $search: "MongoDB 튜토리얼" } })
// "MongoDB" OR "튜토리얼" 포함 문서

// 구문 검색 (정확한 구문)
db.posts.find({ $text: { $search: "\"MongoDB 튜토리얼\"" } })
// "MongoDB 튜토리얼" 정확히 포함

// 제외 검색
db.posts.find({ $text: { $search: "MongoDB -초보" } })
// "MongoDB" 포함, "초보" 제외

// 관련도 점수 포함
db.posts.find(
  { $text: { $search: "MongoDB" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

### 4.3 Portal Universe Text Index

```java
// MongoConfig.java
private void createIndexes() {
    IndexOperations indexOps = mongoTemplate.indexOps("posts");

    // 전문 검색 인덱스 (제목 가중치 2.0, 내용 가중치 1.0)
    TextIndexDefinition textIndex = TextIndexDefinition.builder()
        .onField("title", 2.0f)
        .onField("content", 1.0f)
        .build();
    indexOps.createIndex(textIndex);
}

// PostRepository.java
@Query("{ $text: { $search: ?0 }, status: ?1 }")
Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);
```

```java
// Post.java - @TextIndexed 어노테이션
@Document(collection = "posts")
public class Post {
    @TextIndexed(weight = 2.0f)
    private String title;

    @TextIndexed
    private String content;
}
```

### 4.4 Text Index 제약사항

| 제약 | 설명 |
|------|------|
| **컬렉션당 1개** | 복수 Text Index 불가 |
| **언어 지원** | 한국어 형태소 분석 제한적 |
| **정렬** | textScore 외 정렬 시 성능 저하 |
| **복합 인덱스** | 다른 필드와 조합 가능하나 제약 있음 |

---

## 5. TTL Index (Time To Live)

### 5.1 자동 만료

지정 시간 후 문서를 자동 삭제합니다.

```javascript
// 24시간 후 자동 삭제
db.sessions.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 86400 }  // 24 * 60 * 60
)

// 특정 날짜에 삭제 (expireAt 필드 사용)
db.notifications.createIndex(
  { expireAt: 1 },
  { expireAfterSeconds: 0 }  // expireAt 시간에 즉시 삭제
)

// 문서
{
  _id: "notif1",
  message: "알림 내용",
  expireAt: ISODate("2024-02-01T00:00:00Z")  // 이 시간에 삭제
}
```

### 5.2 사용 사례

```javascript
// 세션 관리
db.sessions.createIndex({ lastAccessedAt: 1 }, { expireAfterSeconds: 3600 })

// 임시 토큰
db.verificationTokens.createIndex({ createdAt: 1 }, { expireAfterSeconds: 600 })

// 로그 보관 (30일)
db.logs.createIndex({ timestamp: 1 }, { expireAfterSeconds: 2592000 })
```

### 5.3 주의사항

| 주의 | 설명 |
|------|------|
| **Date 타입 필수** | Date/ISODate 타입 필드에만 작동 |
| **단일 필드** | 복합 인덱스에 TTL 불가 |
| **백그라운드 삭제** | 즉시 삭제 아님 (60초마다 체크) |
| **복제 지연** | Primary에서 삭제, Secondary에 복제 |

---

## 6. Partial Index (부분 인덱스)

### 6.1 조건부 인덱싱

특정 조건을 만족하는 문서만 인덱싱합니다.

```javascript
// PUBLISHED 상태 게시물만 인덱싱
db.posts.createIndex(
  { publishedAt: -1 },
  { partialFilterExpression: { status: "PUBLISHED" } }
)

// 조회수 100 이상만 인덱싱
db.posts.createIndex(
  { viewCount: -1 },
  { partialFilterExpression: { viewCount: { $gte: 100 } } }
)
```

### 6.2 장점

| 장점 | 설명 |
|------|------|
| **저장 공간 절약** | 필요한 문서만 인덱싱 |
| **쓰기 성능 향상** | 인덱스 유지 비용 감소 |
| **쿼리 성능 유지** | 자주 조회되는 데이터만 인덱싱 |

### 6.3 사용 조건

Partial Index를 사용하려면 **쿼리 조건이 인덱스 조건을 포함**해야 합니다.

```javascript
// 인덱스: { publishedAt: -1 } where status = "PUBLISHED"

// 인덱스 사용 ✅
db.posts.find({ status: "PUBLISHED" }).sort({ publishedAt: -1 })

// 인덱스 사용 안 됨 ❌ (status 조건 없음)
db.posts.find({}).sort({ publishedAt: -1 })
```

---

## 7. Multikey Index (배열 인덱스)

### 7.1 자동 Multikey

배열 필드에 인덱스를 생성하면 **자동으로 Multikey Index**가 됩니다.

```javascript
// tags 배열에 인덱스
db.posts.createIndex({ tags: 1 })

// 문서
{
  _id: "post1",
  tags: ["mongodb", "nosql", "database"]
}

// 쿼리: 세 개의 인덱스 엔트리 생성
// ("mongodb", "post1"), ("nosql", "post1"), ("database", "post1")
```

### 7.2 Portal Universe 태그 인덱스

```java
// MongoConfig.java
indexOps.createIndex(
    new Index()
        .on("tags", Sort.Direction.ASC)
);

// PostRepository.java
Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(
    List<String> tags,
    PostStatus status,
    Pageable pageable
);
```

### 7.3 복합 Multikey 제약

```javascript
// 복합 인덱스에서 배열 필드는 1개만 가능
db.posts.createIndex({ tags: 1, images: 1 })  // ❌ 에러!

// OK: 배열 1개 + 스칼라
db.posts.createIndex({ tags: 1, status: 1 })  // ✅
```

---

## 8. 인덱스 관리

### 8.1 인덱스 확인

```javascript
// 컬렉션의 모든 인덱스
db.posts.getIndexes()

// 인덱스 통계
db.posts.aggregate([
  { $indexStats: {} }
])
```

### 8.2 인덱스 삭제

```javascript
// 이름으로 삭제
db.posts.dropIndex("status_1_publishedAt_-1")

// 정의로 삭제
db.posts.dropIndex({ status: 1, publishedAt: -1 })

// 모든 인덱스 삭제 (_id 제외)
db.posts.dropIndexes()
```

### 8.3 쿼리 분석 (explain)

```javascript
// 기본 실행 계획
db.posts.find({ status: "PUBLISHED" }).explain()

// 상세 통계 포함
db.posts.find({ status: "PUBLISHED" }).explain("executionStats")

// 핵심 지표
{
  "executionStats": {
    "executionSuccess": true,
    "nReturned": 100,          // 반환 문서 수
    "executionTimeMillis": 5,  // 실행 시간
    "totalKeysExamined": 100,  // 검사한 인덱스 키 수
    "totalDocsExamined": 100   // 검사한 문서 수
  },
  "winningPlan": {
    "stage": "IXSCAN",         // 인덱스 스캔 (좋음)
    "indexName": "status_1"
  }
}
```

| stage | 설명 |
|-------|------|
| `COLLSCAN` | 전체 컬렉션 스캔 (❌ 느림) |
| `IXSCAN` | 인덱스 스캔 (✅ 좋음) |
| `FETCH` | 문서 조회 |
| `SORT` | 메모리 정렬 (인덱스 정렬 아님) |

---

## 9. 인덱스 전략

### 9.1 인덱스 생성 가이드라인

| 상황 | 권장 |
|------|------|
| **자주 조회되는 필드** | 인덱스 생성 |
| **정렬에 사용되는 필드** | 인덱스 생성 |
| **고유성이 필요한 필드** | unique 인덱스 |
| **자주 변경되는 필드** | 인덱스 신중하게 |
| **카디널리티 낮은 필드** | 단독 인덱스 비효율 |

### 9.2 인덱스 비용

| 비용 | 설명 |
|------|------|
| **저장 공간** | 인덱스 데이터 저장 |
| **쓰기 성능** | INSERT/UPDATE 시 인덱스 유지 |
| **메모리** | 활성 인덱스는 RAM에 유지 권장 |

### 9.3 Portal Universe 인덱스 요약

```java
// MongoConfig.java - Blog Service 인덱스 전략
private void createIndexes() {
    IndexOperations indexOps = mongoTemplate.indexOps("posts");

    // 1. 전문 검색 (Text Index)
    TextIndexDefinition textIndex = TextIndexDefinition.builder()
        .onField("title", 2.0f)
        .onField("content", 1.0f)
        .build();
    indexOps.createIndex(textIndex);

    // 2. 메인 페이지: 발행된 게시물 최신순
    indexOps.createIndex(new Index()
        .on("status", ASC).on("publishedAt", DESC));

    // 3. 마이페이지: 작성자별 게시물
    indexOps.createIndex(new Index()
        .on("authorId", ASC).on("createdAt", DESC));

    // 4. 카테고리 페이지
    indexOps.createIndex(new Index()
        .on("category", ASC).on("status", ASC).on("publishedAt", DESC));

    // 5. 태그 검색
    indexOps.createIndex(new Index().on("tags", ASC));

    // 6. 인기 게시물
    indexOps.createIndex(new Index()
        .on("status", ASC).on("viewCount", DESC).on("publishedAt", DESC));

    // 7. 레거시 호환
    indexOps.createIndex(new Index().on("productId", ASC));
}
```

---

## 10. 핵심 정리

| 인덱스 유형 | 용도 | 예시 |
|-------------|------|------|
| **Single Field** | 단일 필드 조회 | `{ authorId: 1 }` |
| **Compound** | 복합 조건 조회 | `{ status: 1, publishedAt: -1 }` |
| **Text** | 전문 검색 | `{ title: "text", content: "text" }` |
| **TTL** | 자동 만료 | `{ createdAt: 1 }, expireAfterSeconds` |
| **Partial** | 조건부 인덱싱 | `partialFilterExpression` |
| **Multikey** | 배열 검색 | `{ tags: 1 }` |
| **Unique** | 고유값 보장 | `{ email: 1 }, unique: true` |

---

## 다음 학습

- [MongoDB Transactions](./mongodb-transactions.md)
- [MongoDB Portal Universe 분석](./mongodb-portal-universe.md)

---

## 참고 자료

- [MongoDB Indexes](https://www.mongodb.com/docs/manual/indexes/)
- [Indexing Strategies](https://www.mongodb.com/docs/manual/applications/indexes/)
- [Query Plans](https://www.mongodb.com/docs/manual/core/query-plans/)
