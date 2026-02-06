# MongoDB 데이터 모델링

## 학습 목표
- Embedding vs Referencing 전략 이해
- 1:1, 1:N, N:M 관계 설계 패턴 학습
- Portal Universe Blog Service 모델링 분석

---

## 1. 데이터 모델링 기본 원칙

MongoDB 데이터 모델링은 **쿼리 패턴**을 기반으로 설계합니다. RDBMS처럼 정규화에 집중하지 않고, **읽기 성능 최적화**를 위해 데이터를 구조화합니다.

### 핵심 질문

| 질문 | 영향 |
|------|------|
| 데이터를 어떻게 조회하는가? | 문서 구조 결정 |
| 읽기 vs 쓰기 비율은? | Embedding vs Referencing |
| 데이터는 얼마나 자주 변경되는가? | 역정규화 수준 |
| 관련 데이터 크기는? | 문서 크기 제한 고려 |

---

## 2. Embedding (내장) 패턴

관련 데이터를 **하나의 문서에 포함**시키는 방식입니다.

### 2.1 구조

```json
{
  "_id": "post123",
  "title": "MongoDB 모델링",
  "content": "본문 내용...",
  "author": {
    "id": "user456",
    "name": "홍길동",
    "avatar": "profile.jpg"
  },
  "tags": ["mongodb", "modeling", "nosql"],
  "metadata": {
    "viewCount": 150,
    "likeCount": 25
  }
}
```

### 2.2 장점과 단점

| 장점 | 단점 |
|------|------|
| 단일 쿼리로 전체 데이터 조회 | 문서 크기 제한 (16MB) |
| 원자적 업데이트 가능 | 중복 데이터 발생 가능 |
| JOIN 불필요 | 내장 데이터 독립 조회 불가 |
| 읽기 성능 우수 | 내장 배열이 커지면 성능 저하 |

### 2.3 적합한 상황

```
- 1:1 관계 (사용자 ↔ 프로필)
- 1:Few 관계 (게시물 ↔ 태그 3-5개)
- 함께 조회되는 데이터 (게시물 + 작성자 정보)
- 자주 변경되지 않는 데이터
```

---

## 3. Referencing (참조) 패턴

관련 데이터를 **별도 컬렉션에 저장**하고 ID로 연결합니다.

### 3.1 구조

```json
// posts 컬렉션
{
  "_id": "post123",
  "title": "MongoDB 참조",
  "authorId": "user456",    // 참조
  "categoryId": "cat789"    // 참조
}

// users 컬렉션
{
  "_id": "user456",
  "name": "홍길동",
  "email": "hong@example.com"
}

// comments 컬렉션
{
  "_id": "comment001",
  "postId": "post123",      // 참조
  "authorId": "user789",    // 참조
  "content": "좋은 글이네요"
}
```

### 3.2 장점과 단점

| 장점 | 단점 |
|------|------|
| 문서 크기 제한 없음 | $lookup 필요 (JOIN) |
| 데이터 일관성 유지 용이 | 추가 쿼리 발생 |
| 독립적 업데이트 가능 | 읽기 성능 상대적 저하 |
| 유연한 데이터 모델 | 어플리케이션 레벨 JOIN 필요 |

### 3.3 적합한 상황

```
- 1:Many 관계 (게시물 ↔ 댓글 수백개)
- N:M 관계 (게시물 ↔ 카테고리)
- 독립적으로 조회해야 하는 데이터
- 자주 변경되는 데이터
- 큰 데이터 (이미지 메타데이터 등)
```

---

## 4. 관계 유형별 모델링

### 4.1 1:1 관계

**권장: Embedding**

```json
// 사용자와 설정 (1:1)
{
  "_id": "user123",
  "name": "홍길동",
  "email": "hong@example.com",
  "settings": {
    "theme": "dark",
    "language": "ko",
    "notifications": true
  }
}
```

**예외: 큰 데이터 분리**

```json
// 사용자 기본 정보
{
  "_id": "user123",
  "name": "홍길동"
}

// 사용자 상세 프로필 (별도 컬렉션 - 큰 데이터)
{
  "_id": "profile123",
  "userId": "user123",
  "bio": "매우 긴 소개글...",
  "portfolio": [...],  // 큰 배열
  "certifications": [...]
}
```

### 4.2 1:N 관계 (Few)

**권장: Embedding (배열)**

```json
// 게시물과 태그 (1:Few)
{
  "_id": "post123",
  "title": "MongoDB 가이드",
  "tags": ["mongodb", "database", "nosql"]  // 보통 3-10개
}
```

**Portal Universe 예시:**

```java
// Post.java - tags는 내장
@Document(collection = "posts")
public class Post {
    @Indexed
    private Set<String> tags = new HashSet<>();  // 1:Few - Embedding

    private List<String> images = new ArrayList<>();  // 1:Few - Embedding
}
```

### 4.3 1:N 관계 (Many)

**권장: Referencing (별도 컬렉션)**

```json
// posts 컬렉션
{
  "_id": "post123",
  "title": "인기 게시물",
  "commentCount": 1523  // 역정규화된 카운트
}

// comments 컬렉션 (별도)
{
  "_id": "comment001",
  "postId": "post123",  // 부모 참조
  "content": "댓글 내용",
  "authorId": "user789"
}
```

**Portal Universe 예시:**

```java
// Comment.java - 별도 컬렉션
@Document(collection = "comments")
public class Comment {
    @Indexed
    private String postId;      // 1:Many - Referencing

    private String parentCommentId;  // 대댓글 지원
}
```

### 4.4 N:M 관계

**방법 1: 한쪽에 배열 내장 (조회 패턴 따라)**

```json
// 게시물에 태그 ID 배열
{
  "_id": "post123",
  "title": "MongoDB",
  "tagIds": ["tag1", "tag2", "tag3"]
}

// 태그 컬렉션
{
  "_id": "tag1",
  "name": "mongodb",
  "postCount": 42  // 역정규화
}
```

**방법 2: 연결 컬렉션 사용**

```json
// posts 컬렉션
{ "_id": "post1", "title": "Post 1" }

// categories 컬렉션
{ "_id": "cat1", "name": "Technology" }

// post_categories 컬렉션 (연결)
{
  "_id": "pc1",
  "postId": "post1",
  "categoryId": "cat1",
  "addedAt": "2024-01-15T10:00:00Z"
}
```

**Portal Universe 예시:**

```java
// Like.java - 연결 컬렉션 역할 (User ↔ Post N:M)
@Document(collection = "likes")
@CompoundIndex(name = "postId_userId_unique",
               def = "{'postId': 1, 'userId': 1}",
               unique = true)
public class Like {
    @Indexed
    private String postId;   // Post 참조

    @Indexed
    private String userId;   // User 참조
}
```

---

## 5. 역정규화 (Denormalization)

### 5.1 개념

성능을 위해 **의도적으로 데이터를 중복 저장**합니다.

```json
// 정규화 (참조만)
{
  "_id": "post123",
  "authorId": "user456"  // 이름 조회 시 추가 쿼리 필요
}

// 역정규화 (데이터 복사)
{
  "_id": "post123",
  "authorId": "user456",
  "authorName": "홍길동"  // 중복 저장 - 추가 쿼리 불필요
}
```

### 5.2 Portal Universe 역정규화 사례

```java
// Post.java
@Document(collection = "posts")
public class Post {
    private String authorId;       // 원본 참조 (일관성)
    private String authorName;     // 역정규화 (읽기 성능)

    // 카운터 역정규화 (매번 count() 방지)
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;
}

// Tag.java
@Document(collection = "tags")
public class Tag {
    private String name;
    private Long postCount = 0L;   // 역정규화된 카운트
    private LocalDateTime lastUsedAt;  // 인기 태그 조회 최적화
}
```

### 5.3 역정규화 주의사항

| 고려사항 | 설명 |
|----------|------|
| **동기화 필요** | 원본 변경 시 복사본도 업데이트 |
| **일관성 트레이드오프** | 일시적 불일치 허용 여부 |
| **쓰기 비용 증가** | 여러 문서 업데이트 필요 |
| **적합한 데이터** | 자주 읽고, 드물게 변경되는 것 |

```java
// LikeService.java - 역정규화 동기화 예시
@Transactional
public LikeToggleResponse toggleLike(String postId, String userId) {
    Post post = postRepository.findById(postId)...;

    if (existingLike != null) {
        likeRepository.delete(existingLike);
        post.decrementLikeCount();  // 역정규화 동기화
    } else {
        likeRepository.save(newLike);
        post.incrementLikeCount();  // 역정규화 동기화
    }

    postRepository.save(post);  // 카운트 저장
}
```

---

## 6. Hybrid 패턴

### 6.1 부분 Embedding

자주 사용되는 필드만 내장하고, 상세는 참조합니다.

```json
// 게시물에 작성자 요약 정보만 내장
{
  "_id": "post123",
  "title": "MongoDB 가이드",
  "author": {
    "id": "user456",       // 참조 ID (상세 조회용)
    "name": "홍길동",       // 내장 (표시용)
    "avatar": "thumb.jpg"   // 내장 (표시용)
  }
}

// users 컬렉션에 전체 정보
{
  "_id": "user456",
  "name": "홍길동",
  "email": "hong@example.com",
  "bio": "긴 소개글...",
  "settings": {...}
}
```

### 6.2 Bucketing 패턴

시계열 데이터를 시간 단위로 묶어 저장합니다.

```json
// 일별 통계 버킷
{
  "_id": "stats_post123_2024-01-15",
  "postId": "post123",
  "date": "2024-01-15",
  "hourlyViews": [
    { "hour": 0, "count": 15 },
    { "hour": 1, "count": 8 },
    { "hour": 2, "count": 3 },
    // ... 24시간
  ],
  "totalViews": 426
}
```

---

## 7. 모델링 결정 플로우차트

```
데이터 조회 패턴 분석
        │
        ▼
┌─────────────────────┐
│ 함께 조회하는가?     │
└─────────────────────┘
    │Yes         │No
    ▼            ▼
┌─────────┐  ┌─────────────────┐
│Embedding │  │ 독립 조회 필요?  │
│ 고려     │  └─────────────────┘
└─────────┘      │Yes      │No
    │            ▼         ▼
    ▼      ┌──────────┐ ┌──────────┐
┌─────────┐│Referencing│ │Embedding │
│크기 체크│└──────────┘ │ 가능     │
└─────────┘              └──────────┘
    │
    ▼
┌─────────────────────┐
│ 16MB 초과 가능성?   │
└─────────────────────┘
    │Yes         │No
    ▼            ▼
┌──────────┐  ┌─────────┐
│Referencing│  │Embedding│
└──────────┘  └─────────┘
```

---

## 8. Anti-Patterns

### 8.1 무한 성장 배열

```json
// Bad: 댓글이 계속 추가되어 문서 크기 폭발
{
  "_id": "post123",
  "comments": [
    { "text": "댓글1" },
    { "text": "댓글2" },
    // ... 수천 개
  ]
}

// Good: 별도 컬렉션
// posts 컬렉션
{ "_id": "post123", "commentCount": 1523 }

// comments 컬렉션
{ "_id": "c1", "postId": "post123", "text": "댓글1" }
```

### 8.2 과도한 정규화

```json
// Bad: RDBMS 스타일 과도한 분리
{
  "_id": "post123",
  "titleId": "title001",      // 불필요한 분리
  "contentId": "content001",  // 불필요한 분리
  "authorId": "user456"
}

// Good: 자연스러운 문서 구조
{
  "_id": "post123",
  "title": "제목",
  "content": "내용",
  "authorId": "user456"
}
```

### 8.3 깊은 중첩

```json
// Bad: 너무 깊은 중첩 (3단계 이상)
{
  "level1": {
    "level2": {
      "level3": {
        "level4": {
          "data": "value"
        }
      }
    }
  }
}

// Good: 평탄화 또는 참조
{
  "metadata": {
    "category": "tech",
    "subCategory": "database"
  },
  "deepDataRef": "deepData123"  // 별도 문서 참조
}
```

---

## 9. 핵심 정리

| 패턴 | 사용 시점 | Portal Universe 예시 |
|------|----------|---------------------|
| **Embedding** | 1:1, 1:Few, 함께 조회 | Post.tags, Post.images |
| **Referencing** | 1:Many, 독립 업데이트 | Comment.postId, Like.postId |
| **역정규화** | 읽기 성능 필요 | Post.authorName, Tag.postCount |
| **Hybrid** | 요약+상세 분리 | Post.authorId + authorName |

---

## 다음 학습

- [MongoDB CRUD 연산](./mongodb-crud-operations.md)
- [MongoDB Aggregation](./mongodb-aggregation.md)
- [MongoDB 인덱스](./mongodb-indexes.md)

---

## 참고 자료

- [MongoDB Data Modeling](https://www.mongodb.com/docs/manual/data-modeling/)
- [Schema Design Best Practices](https://www.mongodb.com/developer/products/mongodb/schema-design-anti-pattern-massive-arrays/)
- [Building with Patterns](https://www.mongodb.com/blog/post/building-with-patterns-a-summary)
