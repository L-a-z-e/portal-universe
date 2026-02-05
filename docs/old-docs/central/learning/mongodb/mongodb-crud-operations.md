# MongoDB CRUD 연산

## 학습 목표
- insertOne/Many로 문서 생성
- find 쿼리와 연산자 활용
- update 연산자 ($set, $inc, $push 등)
- delete 연산 이해
- Spring Data MongoDB Repository 메서드 매핑

---

## 1. Create (생성)

### 1.1 insertOne

단일 문서를 삽입합니다.

```javascript
// MongoDB Shell
db.posts.insertOne({
  title: "첫 번째 게시물",
  content: "MongoDB CRUD 학습 중입니다.",
  authorId: "user123",
  authorName: "홍길동",
  status: "DRAFT",
  tags: ["mongodb", "tutorial"],
  viewCount: 0,
  createdAt: new Date()
})

// 결과
{
  "acknowledged": true,
  "insertedId": ObjectId("507f1f77bcf86cd799439011")
}
```

**Spring Data MongoDB:**

```java
// Repository
public interface PostRepository extends MongoRepository<Post, String> {}

// Service
@Transactional
public Post create(PostCreateRequest request) {
    Post post = Post.builder()
        .title(request.getTitle())
        .content(request.getContent())
        .authorId(userId)
        .authorName(userName)
        .status(PostStatus.DRAFT)
        .tags(request.getTags())
        .build();

    return postRepository.save(post);  // insertOne
}
```

### 1.2 insertMany

여러 문서를 한 번에 삽입합니다.

```javascript
// MongoDB Shell
db.posts.insertMany([
  {
    title: "게시물 1",
    content: "내용 1",
    status: "PUBLISHED"
  },
  {
    title: "게시물 2",
    content: "내용 2",
    status: "DRAFT"
  },
  {
    title: "게시물 3",
    content: "내용 3",
    status: "PUBLISHED"
  }
])

// 결과
{
  "acknowledged": true,
  "insertedIds": [
    ObjectId("...1"),
    ObjectId("...2"),
    ObjectId("...3")
  ]
}
```

**Spring Data MongoDB:**

```java
// 여러 문서 저장
List<Post> posts = Arrays.asList(post1, post2, post3);
postRepository.saveAll(posts);  // insertMany
```

### 1.3 Upsert (Insert or Update)

문서가 있으면 업데이트, 없으면 삽입합니다.

```javascript
// MongoDB Shell
db.tags.updateOne(
  { name: "mongodb" },           // 검색 조건
  {
    $set: { lastUsedAt: new Date() },
    $inc: { postCount: 1 },
    $setOnInsert: {              // 삽입 시에만 적용
      createdAt: new Date(),
      description: null
    }
  },
  { upsert: true }               // upsert 옵션
)
```

---

## 2. Read (조회)

### 2.1 기본 find

```javascript
// 전체 조회
db.posts.find()

// 조건 조회
db.posts.find({ status: "PUBLISHED" })

// 단일 문서 조회
db.posts.findOne({ _id: ObjectId("507f1f77bcf86cd799439011") })
```

### 2.2 비교 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `$eq` | 같음 | `{ status: { $eq: "PUBLISHED" } }` |
| `$ne` | 같지 않음 | `{ status: { $ne: "DRAFT" } }` |
| `$gt` | 초과 | `{ viewCount: { $gt: 100 } }` |
| `$gte` | 이상 | `{ viewCount: { $gte: 100 } }` |
| `$lt` | 미만 | `{ viewCount: { $lt: 50 } }` |
| `$lte` | 이하 | `{ viewCount: { $lte: 50 } }` |
| `$in` | 포함 | `{ status: { $in: ["DRAFT", "PUBLISHED"] } }` |
| `$nin` | 미포함 | `{ category: { $nin: ["spam", "deleted"] } }` |

```javascript
// 복합 조건
db.posts.find({
  status: "PUBLISHED",
  viewCount: { $gte: 100 },
  category: { $in: ["tech", "programming"] }
})
```

### 2.3 논리 연산자

| 연산자 | 설명 |
|--------|------|
| `$and` | 모든 조건 만족 |
| `$or` | 하나 이상 만족 |
| `$not` | 조건 부정 |
| `$nor` | 모든 조건 불만족 |

```javascript
// $or 예시: 인기 게시물 또는 최신 게시물
db.posts.find({
  status: "PUBLISHED",
  $or: [
    { viewCount: { $gte: 1000 } },
    { publishedAt: { $gte: new Date("2024-01-01") } }
  ]
})

// $and 예시 (암묵적 AND)
db.posts.find({
  status: "PUBLISHED",
  viewCount: { $gte: 100 }
})

// $and 명시적 사용 (같은 필드에 여러 조건)
db.posts.find({
  $and: [
    { viewCount: { $gte: 100 } },
    { viewCount: { $lte: 1000 } }
  ]
})
```

### 2.4 배열 연산자

| 연산자 | 설명 | 예시 |
|--------|------|------|
| `$all` | 모든 요소 포함 | `{ tags: { $all: ["mongodb", "nosql"] } }` |
| `$elemMatch` | 조건 만족 요소 존재 | 아래 참조 |
| `$size` | 배열 크기 일치 | `{ tags: { $size: 3 } }` |

```javascript
// 태그 검색: mongodb 또는 nosql 포함
db.posts.find({
  tags: { $in: ["mongodb", "nosql"] }
})

// 태그 검색: mongodb AND nosql 모두 포함
db.posts.find({
  tags: { $all: ["mongodb", "nosql"] }
})
```

**Portal Universe Repository 메서드:**

```java
// PostRepository.java
// tags 배열에서 주어진 태그들 중 하나라도 포함된 게시물
Page<Post> findByTagsInAndStatusOrderByPublishedAtDesc(
    List<String> tags,
    PostStatus status,
    Pageable pageable
);
```

### 2.5 필드 존재 및 타입

```javascript
// 필드 존재 여부
db.posts.find({ thumbnailUrl: { $exists: true } })

// 필드 타입 체크
db.posts.find({ viewCount: { $type: "long" } })

// null 체크 (필드 없거나 null인 경우)
db.posts.find({ productId: null })

// null이 아닌 것만
db.posts.find({
  productId: { $ne: null, $exists: true }
})
```

### 2.6 Projection (필드 선택)

```javascript
// 특정 필드만 조회
db.posts.find(
  { status: "PUBLISHED" },
  { title: 1, authorName: 1, publishedAt: 1, _id: 0 }
)

// 특정 필드 제외
db.posts.find(
  { status: "PUBLISHED" },
  { content: 0 }  // content 제외
)
```

**Spring Data MongoDB @Query:**

```java
@Query(value = "{ status: ?0 }", fields = "{ category: 1 }")
List<String> findDistinctCategoriesByStatus(PostStatus status);
```

### 2.7 정렬, 페이징, 제한

```javascript
// 정렬
db.posts.find({ status: "PUBLISHED" })
  .sort({ publishedAt: -1 })  // -1: 내림차순, 1: 오름차순

// 복합 정렬
db.posts.find({ status: "PUBLISHED" })
  .sort({ viewCount: -1, publishedAt: -1 })

// 페이지네이션
db.posts.find({ status: "PUBLISHED" })
  .sort({ publishedAt: -1 })
  .skip(20)    // 앞에서 20개 건너뜀 (3페이지 시작)
  .limit(10)   // 10개만 조회
```

**Spring Data MongoDB:**

```java
// Repository 메서드 이름 규칙
Page<Post> findByStatusOrderByPublishedAtDesc(
    PostStatus status,
    Pageable pageable
);

// 사용
Pageable pageable = PageRequest.of(2, 10);  // 3페이지, 10개씩
Page<Post> posts = postRepository.findByStatusOrderByPublishedAtDesc(
    PostStatus.PUBLISHED,
    pageable
);
```

### 2.8 텍스트 검색

```javascript
// 텍스트 인덱스 생성 (선행 필요)
db.posts.createIndex(
  { title: "text", content: "text" },
  { weights: { title: 2, content: 1 } }
)

// 텍스트 검색
db.posts.find({
  $text: { $search: "MongoDB 튜토리얼" },
  status: "PUBLISHED"
})

// 관련도 점수 포함
db.posts.find(
  { $text: { $search: "MongoDB" } },
  { score: { $meta: "textScore" } }
).sort({ score: { $meta: "textScore" } })
```

**Portal Universe:**

```java
// PostRepository.java
@Query("{ $text: { $search: ?0 }, status: ?1 }")
Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);
```

---

## 3. Update (수정)

### 3.1 updateOne / updateMany

```javascript
// 단일 문서 수정
db.posts.updateOne(
  { _id: ObjectId("...") },          // 필터
  { $set: { title: "수정된 제목" } }  // 업데이트
)

// 여러 문서 수정
db.posts.updateMany(
  { status: "DRAFT", authorId: "user123" },
  { $set: { status: "ARCHIVED" } }
)
```

### 3.2 업데이트 연산자

#### $set - 필드 설정

```javascript
db.posts.updateOne(
  { _id: ObjectId("...") },
  {
    $set: {
      title: "새 제목",
      "author.name": "새 이름",  // 중첩 필드
      tags: ["tag1", "tag2"]     // 배열 전체 교체
    }
  }
)
```

#### $unset - 필드 제거

```javascript
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $unset: { productId: "", thumbnailUrl: "" } }
)
```

#### $inc - 숫자 증감

```javascript
// 조회수 1 증가
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $inc: { viewCount: 1 } }
)

// 여러 필드 동시 증감
db.posts.updateOne(
  { _id: ObjectId("...") },
  {
    $inc: {
      viewCount: 1,
      likeCount: -1  // 감소
    }
  }
)
```

**Portal Universe:**

```java
// Post.java
public void incrementViewCount() {
    this.viewCount++;  // Java에서 증가 후 save
}

public void decrementLikeCount() {
    if (this.likeCount > 0) {
        this.likeCount--;
    }
}
```

#### $push / $pull - 배열 요소 추가/제거

```javascript
// 요소 추가
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $push: { tags: "new-tag" } }
)

// 여러 요소 추가
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $push: { tags: { $each: ["tag1", "tag2"] } } }
)

// 요소 제거
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $pull: { tags: "old-tag" } }
)

// 조건부 제거
db.comments.updateOne(
  { _id: ObjectId("...") },
  { $pull: { likes: { userId: "user123" } } }
)
```

#### $addToSet - 중복 없이 추가

```javascript
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $addToSet: { tags: "unique-tag" } }  // 이미 있으면 무시
)

// 여러 개 추가
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $addToSet: { tags: { $each: ["tag1", "tag2"] } } }
)
```

#### $rename - 필드 이름 변경

```javascript
db.posts.updateMany(
  {},
  { $rename: { "oldFieldName": "newFieldName" } }
)
```

#### $min / $max - 조건부 업데이트

```javascript
// viewCount가 현재보다 큰 경우에만 업데이트
db.posts.updateOne(
  { _id: ObjectId("...") },
  { $max: { viewCount: 1000 } }  // 1000보다 작으면 1000으로
)

// 가장 오래된 날짜 유지
db.stats.updateOne(
  { _id: "post123" },
  { $min: { firstViewedAt: new Date() } }
)
```

### 3.3 findOneAndUpdate

수정 후 문서를 반환합니다.

```javascript
db.posts.findOneAndUpdate(
  { _id: ObjectId("...") },
  { $inc: { viewCount: 1 } },
  {
    returnDocument: "after",  // 수정 후 문서 반환
    projection: { title: 1, viewCount: 1 }
  }
)
```

### 3.4 replaceOne

문서 전체를 교체합니다.

```javascript
db.posts.replaceOne(
  { _id: ObjectId("...") },
  {
    title: "완전히 새로운 문서",
    content: "새 내용",
    authorId: "user123",
    status: "DRAFT"
    // _id 유지, 나머지 필드 교체
  }
)
```

---

## 4. Delete (삭제)

### 4.1 deleteOne / deleteMany

```javascript
// 단일 삭제
db.posts.deleteOne({ _id: ObjectId("...") })

// 조건부 다중 삭제
db.posts.deleteMany({ status: "ARCHIVED" })

// 전체 삭제 (주의!)
db.posts.deleteMany({})
```

### 4.2 Soft Delete

실제 삭제 대신 플래그로 표시합니다.

```javascript
// Soft delete
db.comments.updateOne(
  { _id: ObjectId("...") },
  { $set: { isDeleted: true, deletedAt: new Date() } }
)
```

**Portal Universe:**

```java
// Comment.java
@Builder.Default
private Boolean isDeleted = false;

public void delete() {
    this.isDeleted = true;  // Soft delete
}

// CommentRepository.java
List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(String postId);
```

### 4.3 findOneAndDelete

삭제하면서 문서를 반환합니다.

```javascript
db.posts.findOneAndDelete(
  { _id: ObjectId("...") }
)
// 삭제된 문서 반환
```

---

## 5. Spring Data MongoDB 매핑

### 5.1 Repository 메서드 규칙

| 키워드 | MongoDB | 예시 |
|--------|---------|------|
| `findBy` | find | `findByAuthorId(String authorId)` |
| `And` | $and | `findByStatusAndAuthorId(...)` |
| `Or` | $or | `findByTitleOrContent(...)` |
| `Between` | $gt, $lt | `findByCreatedAtBetween(...)` |
| `LessThan` | $lt | `findByViewCountLessThan(Long count)` |
| `GreaterThan` | $gt | `findByLikeCountGreaterThan(...)` |
| `In` | $in | `findByStatusIn(List<PostStatus> statuses)` |
| `Containing` | $regex | `findByTitleContaining(String keyword)` |
| `OrderBy` | sort | `findByStatusOrderByCreatedAtDesc(...)` |
| `First` | limit(1) | `findFirstByStatusOrderByViewCountDesc(...)` |

### 5.2 @Query 어노테이션

```java
// 복잡한 쿼리 직접 작성
@Query("{ $or: [ { category: ?0 }, { tags: { $in: ?1 } } ], status: ?2, _id: { $ne: ?3 } }")
List<Post> findRelatedPosts(String category, List<String> tags, PostStatus status, String excludePostId);

// 텍스트 검색
@Query("{ $text: { $search: ?0 }, status: ?1 }")
Page<Post> findByTextSearchAndStatus(String searchText, PostStatus status, Pageable pageable);

// 뷰어블 게시물 조회
@Query("{ _id: ?0, $or: [ { status: 'PUBLISHED' }, { authorId: ?1 } ] }")
Optional<Post> findByIdAndViewableBy(String postId, String userId);
```

### 5.3 MongoTemplate 직접 사용

```java
@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public List<Post> findByComplexCriteria(...) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(status.name()));
        query.addCriteria(Criteria.where("publishedAt").gte(startDate));
        query.with(Sort.by(Direction.DESC, "viewCount"));
        query.skip(offset).limit(size);

        return mongoTemplate.find(query, Post.class);
    }
}
```

---

## 6. 핵심 정리

| 연산 | MongoDB | Spring Data |
|------|---------|-------------|
| **Create** | insertOne/Many | save/saveAll |
| **Read** | find/findOne | findBy.../findById |
| **Update** | updateOne/Many | save (dirty checking) |
| **Delete** | deleteOne/Many | delete/deleteById |

| 업데이트 연산자 | 용도 |
|----------------|------|
| `$set` | 필드 값 설정 |
| `$unset` | 필드 제거 |
| `$inc` | 숫자 증감 |
| `$push` | 배열에 추가 |
| `$pull` | 배열에서 제거 |
| `$addToSet` | 중복 없이 추가 |

---

## 다음 학습

- [MongoDB Aggregation](./mongodb-aggregation.md)
- [MongoDB 인덱스](./mongodb-indexes.md)
- [MongoDB Transactions](./mongodb-transactions.md)

---

## 참고 자료

- [MongoDB CRUD Operations](https://www.mongodb.com/docs/manual/crud/)
- [Update Operators](https://www.mongodb.com/docs/manual/reference/operator/update/)
- [Spring Data MongoDB Reference](https://docs.spring.io/spring-data/mongodb/reference/)
