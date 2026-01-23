# MongoDB 소개

## 학습 목표
- MongoDB의 특성과 Document 모델 이해
- RDBMS와의 차이점 파악
- Portal Universe Blog Service에서의 활용 개요

---

## 1. MongoDB란?

MongoDB는 **Document 지향 NoSQL 데이터베이스**입니다. JSON과 유사한 BSON(Binary JSON) 형식으로 데이터를 저장하며, 유연한 스키마를 제공합니다.

### 핵심 특징

| 특성 | 설명 |
|------|------|
| **Document Model** | JSON 유사 구조로 데이터 저장 |
| **유연한 스키마** | 컬렉션 내 문서별 다른 필드 가능 |
| **수평 확장** | 샤딩으로 대용량 데이터 처리 |
| **고성능** | 인덱스, 인메모리 처리 최적화 |
| **풍부한 쿼리** | Aggregation Pipeline 지원 |

### 용어 비교: RDBMS vs MongoDB

| RDBMS | MongoDB | 설명 |
|-------|---------|------|
| Database | Database | 데이터베이스 |
| Table | Collection | 문서들의 집합 |
| Row | Document | 하나의 레코드 |
| Column | Field | 문서 내 속성 |
| Primary Key | _id | 고유 식별자 |
| Foreign Key | Reference / Embedding | 관계 표현 |

---

## 2. Document 모델

### 2.1 기본 구조

```json
{
  "_id": "507f1f77bcf86cd799439011",
  "title": "MongoDB 소개",
  "content": "MongoDB는 Document 지향 NoSQL...",
  "authorId": "user123",
  "tags": ["mongodb", "nosql", "database"],
  "viewCount": 150,
  "createdAt": "2024-01-15T10:30:00Z",
  "comments": [
    {
      "author": "홍길동",
      "text": "좋은 글이네요!",
      "createdAt": "2024-01-15T11:00:00Z"
    }
  ]
}
```

### 2.2 BSON 데이터 타입

| 타입 | 설명 | 예시 |
|------|------|------|
| String | 문자열 | `"hello"` |
| Integer | 정수 (32/64bit) | `42` |
| Double | 부동소수점 | `3.14` |
| Boolean | 참/거짓 | `true` |
| Array | 배열 | `["a", "b", "c"]` |
| Object | 중첩 문서 | `{name: "value"}` |
| ObjectId | 12바이트 고유 ID | `ObjectId("...")` |
| Date | 날짜/시간 | `ISODate("...")` |
| Null | null 값 | `null` |

### 2.3 _id 필드

모든 문서는 `_id` 필드를 가집니다.

```
ObjectId: 507f1f77bcf86cd799439011
          └──────┘└──────┘└──┘└────┘
          타임스탬프 머신ID PID 카운터
          (4 bytes) (3 bytes)(2)(3)
```

- 12바이트 구성
- 자동 생성 또는 직접 지정 가능
- 시간순 정렬 가능

---

## 3. 데이터 모델링 전략

### 3.1 Embedding (내장)

관련 데이터를 **하나의 문서에 포함**합니다.

```json
{
  "_id": "post123",
  "title": "게시물 제목",
  "author": {
    "id": "user456",
    "name": "홍길동",
    "avatar": "avatar.jpg"
  },
  "comments": [
    { "text": "댓글1", "author": "김철수" },
    { "text": "댓글2", "author": "이영희" }
  ]
}
```

**장점:**
- 단일 쿼리로 관련 데이터 조회
- 원자적 업데이트 가능
- 읽기 성능 우수

**단점:**
- 문서 크기 제한 (16MB)
- 중복 데이터 발생 가능

### 3.2 Referencing (참조)

관련 데이터를 **별도 컬렉션에 저장**하고 ID로 참조합니다.

```json
// posts 컬렉션
{
  "_id": "post123",
  "title": "게시물 제목",
  "authorId": "user456"
}

// comments 컬렉션
{
  "_id": "comment1",
  "postId": "post123",
  "text": "댓글 내용",
  "authorId": "user789"
}
```

**장점:**
- 문서 크기 제한 없음
- 데이터 일관성 유지 용이
- 독립적 업데이트

**단점:**
- 조인 필요 ($lookup)
- 읽기 시 추가 쿼리

### 3.3 선택 기준

| 상황 | 권장 방식 |
|------|----------|
| 1:1 관계 | Embedding |
| 1:Few (소수) | Embedding |
| 1:Many (다수) | Referencing |
| Many:Many | Referencing |
| 자주 함께 조회 | Embedding |
| 독립적 업데이트 필요 | Referencing |
| 큰 데이터 | Referencing |

---

## 4. Portal Universe: Blog Service 구조

### 4.1 컬렉션 설계

```
blog_db/
├── posts          # 블로그 게시물
├── comments       # 댓글 (별도 컬렉션)
├── likes          # 좋아요
├── tags           # 태그 (역정규화)
└── series         # 연재물
```

### 4.2 Post Document (Hybrid 접근)

```java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;

    @TextIndexed(weight = 2.0f)    // 전문 검색 가중치
    private String title;

    @TextIndexed
    private String content;

    private String summary;

    @Indexed                        // 인덱스
    private String authorId;

    private String authorName;      // 역정규화 (빠른 조회)

    @Indexed
    private PostStatus status;      // DRAFT, PUBLISHED

    @Indexed
    private Set<String> tags;       // 다중 태그 (Embedding)

    @Indexed
    private String category;

    // 카운터 (역정규화)
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;

    @Indexed
    private LocalDateTime publishedAt;

    private String thumbnailUrl;
    private List<String> images = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**설계 결정:**
- `authorName` 역정규화: 게시물 조회 시 사용자 조인 불필요
- `tags` Embedding: 게시물당 태그 수가 적음 (1:Few)
- `comments` 별도 컬렉션: 댓글이 많을 수 있음 (1:Many)
- 카운터 역정규화: 매번 count 쿼리 방지

### 4.3 Comment Document

```java
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @Indexed
    private String postId;           // 게시물 참조

    @Indexed
    private String authorId;

    private String authorName;       // 역정규화
    private String content;

    private String parentCommentId;  // 대댓글 지원

    private Long likeCount = 0L;
    private Boolean isDeleted = false;  // Soft delete

    @CreatedDate
    private LocalDateTime createdAt;
}
```

### 4.4 Like Document

```java
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true                     // 중복 방지
)
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

## 5. RDBMS vs MongoDB 선택

### 5.1 MongoDB가 적합한 경우

| 상황 | 이유 |
|------|------|
| **스키마 변경 잦음** | 유연한 스키마 |
| **계층적 데이터** | 중첩 문서 자연스러움 |
| **높은 쓰기 처리량** | 수평 확장 |
| **로그/이벤트 데이터** | 대용량 빠른 저장 |
| **프로토타이핑** | 빠른 개발 |

### 5.2 RDBMS가 적합한 경우

| 상황 | 이유 |
|------|------|
| **복잡한 관계** | JOIN 효율적 |
| **트랜잭션 중요** | ACID 보장 |
| **데이터 무결성** | 외래 키, 제약조건 |
| **복잡한 집계** | SQL 강력함 |
| **기존 시스템 연동** | SQL 표준 |

### 5.3 Portal Universe 선택

| 서비스 | DB | 이유 |
|--------|-----|------|
| **Shopping** | MySQL | 주문/결제 트랜잭션, 복잡한 관계 |
| **Blog** | MongoDB | 스키마 유연성, 문서 구조 자연스러움 |
| **Auth** | MySQL | 사용자 데이터 무결성 |

---

## 6. 기본 CRUD 연산

### 6.1 Create

```javascript
// 단일 문서 삽입
db.posts.insertOne({
  title: "첫 번째 게시물",
  content: "내용입니다.",
  tags: ["mongo", "intro"]
})

// 다중 문서 삽입
db.posts.insertMany([
  { title: "게시물1" },
  { title: "게시물2" }
])
```

### 6.2 Read

```javascript
// 조건 검색
db.posts.find({ status: "PUBLISHED" })

// 필드 선택 (Projection)
db.posts.find(
  { authorId: "user123" },
  { title: 1, createdAt: 1, _id: 0 }
)

// 정렬 및 페이지네이션
db.posts.find({ status: "PUBLISHED" })
  .sort({ publishedAt: -1 })
  .skip(20)
  .limit(10)
```

### 6.3 Update

```javascript
// 단일 필드 수정
db.posts.updateOne(
  { _id: "post123" },
  { $set: { title: "수정된 제목" } }
)

// 카운터 증가
db.posts.updateOne(
  { _id: "post123" },
  { $inc: { viewCount: 1 } }
)

// 배열에 추가
db.posts.updateOne(
  { _id: "post123" },
  { $push: { tags: "new-tag" } }
)
```

### 6.4 Delete

```javascript
// 단일 삭제
db.posts.deleteOne({ _id: "post123" })

// 다중 삭제
db.posts.deleteMany({ status: "DRAFT" })
```

---

## 7. 인덱스 기초

### 7.1 인덱스 생성

```javascript
// 단일 필드 인덱스
db.posts.createIndex({ authorId: 1 })

// 복합 인덱스
db.posts.createIndex({ status: 1, publishedAt: -1 })

// 텍스트 인덱스
db.posts.createIndex(
  { title: "text", content: "text" },
  { weights: { title: 2, content: 1 } }
)

// 고유 인덱스
db.users.createIndex({ email: 1 }, { unique: true })
```

### 7.2 인덱스 사용 확인

```javascript
db.posts.find({ authorId: "user123" }).explain("executionStats")
```

---

## 8. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Document** | JSON 유사 데이터 단위 |
| **Collection** | 문서들의 집합 |
| **BSON** | Binary JSON 저장 형식 |
| **Embedding** | 관련 데이터 문서 내 포함 |
| **Referencing** | ID로 다른 컬렉션 참조 |
| **역정규화** | 조회 성능을 위한 데이터 중복 |

---

## 다음 학습

- [MongoDB 데이터 모델링 심화](./mongodb-data-modeling.md)
- [MongoDB Aggregation](./mongodb-aggregation.md)
- [MongoDB Spring 통합](./mongodb-spring-integration.md)

---

## 참고 자료

- [MongoDB 공식 문서](https://www.mongodb.com/docs/)
- [MongoDB University](https://university.mongodb.com/)
- [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
