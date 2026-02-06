# Blog Service 도메인 개요

## 학습 목표
- Blog Service의 MongoDB Document 구조 이해
- 각 Document 간의 관계 파악
- Embedding vs Reference 전략 학습

---

## 1. 도메인 맵

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          BLOG SERVICE DOMAIN                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │                         CONTENT DOMAIN                                │  │
│   ├──────────────────────────────────────────────────────────────────────┤  │
│   │                                                                       │  │
│   │   ┌──────────────┐                                                    │  │
│   │   │     Post     │ ◄─── Root Document                                 │  │
│   │   ├──────────────┤                                                    │  │
│   │   │ • title      │                                                    │  │
│   │   │ • content    │                                                    │  │
│   │   │ • summary    │                                                    │  │
│   │   │ • tags[]     │ ◄─── Embedded Array                                │  │
│   │   │ • images[]   │ ◄─── Embedded Array                                │  │
│   │   │ • viewCount  │                                                    │  │
│   │   │ • likeCount  │ ◄─── 역정규화 (Denormalized)                        │  │
│   │   │ • commentCount│                                                   │  │
│   │   └──────────────┘                                                    │  │
│   │          │                                                            │  │
│   │          │ 1:N (Reference)                                            │  │
│   │          ▼                                                            │  │
│   │   ┌──────────────┐     ┌──────────────┐     ┌──────────────┐         │  │
│   │   │   Comment    │     │    Like      │     │   Series     │         │  │
│   │   ├──────────────┤     ├──────────────┤     ├──────────────┤         │  │
│   │   │ • postId     │     │ • postId     │     │ • postIds[]  │         │  │
│   │   │ • authorId   │     │ • userId     │     │ • name       │         │  │
│   │   │ • content    │     └──────────────┘     │ • description│         │  │
│   │   │ • parentId   │ ◄─ Self-reference       └──────────────┘         │  │
│   │   └──────────────┘     (대댓글)                                       │  │
│   │                                                                       │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│   ┌──────────────────────────────────────────────────────────────────────┐  │
│   │                      CLASSIFICATION DOMAIN                            │  │
│   ├──────────────────────────────────────────────────────────────────────┤  │
│   │                                                                       │  │
│   │   ┌──────────────┐                                                    │  │
│   │   │     Tag      │                                                    │  │
│   │   ├──────────────┤                                                    │  │
│   │   │ • name       │ ◄─── unique index                                  │  │
│   │   │ • postCount  │ ◄─── 역정규화                                       │  │
│   │   │ • lastUsedAt │                                                    │  │
│   │   └──────────────┘                                                    │  │
│   │                                                                       │  │
│   └──────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Document 상세

### 2.1 Post (게시물)

Blog Service의 핵심 Document입니다.

```java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;

    @TextIndexed(weight = 2.0f)  // 텍스트 검색 가중치
    private String title;

    @TextIndexed
    private String content;

    private String summary;           // 자동 생성 가능

    @Indexed
    private String authorId;          // 작성자 참조

    private String authorName;        // 표시용 (역정규화)

    @Indexed
    private PostStatus status;        // DRAFT, PUBLISHED

    @Indexed
    private Set<String> tags;         // Embedded (빠른 조회)

    private String category;

    // 카운터 (역정규화 - 조회 성능 최적화)
    private Long viewCount = 0L;
    private Long likeCount = 0L;
    private Long commentCount = 0L;

    @Indexed
    private LocalDateTime publishedAt;

    // SEO
    private String metaDescription;   // 자동 생성 가능

    // 이미지 (Embedded Array)
    private String thumbnailUrl;
    private List<String> images;

    // 선택적 연결
    private String productId;         // Shopping 연동용

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

**상태 전이:**
```
DRAFT ────► PUBLISHED
  ▲              │
  │              │
  └──── unpublish ─┘
```

**인덱스 전략:**

| 인덱스 | 타입 | 목적 |
|--------|------|------|
| `authorId` | 단일 | 작성자별 글 조회 |
| `status` | 단일 | 발행 상태 필터링 |
| `tags` | 다중 | 태그 기반 검색 |
| `publishedAt` | 단일 | 최신순 정렬 |
| `title + content` | 텍스트 | 전문 검색 |

### 2.2 Comment (댓글)

대댓글을 지원하는 Self-referencing 구조입니다.

```java
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @Indexed
    private String postId;           // 게시물 참조

    @Indexed
    private String authorId;
    private String authorName;

    private String content;

    private String parentCommentId;  // null = 루트 댓글

    private Long likeCount = 0L;
    private Boolean isDeleted = false;  // Soft Delete

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**대댓글 구조:**
```
Comment (루트)
├── Comment (1단계 대댓글)
│   └── Comment (2단계 대댓글)
└── Comment (1단계 대댓글)
```

**Soft Delete 전략:**
- 댓글 삭제 시 `isDeleted = true`
- 대댓글이 있는 경우 "삭제된 댓글입니다" 표시
- 실제 데이터는 유지 (감사 추적)

### 2.3 Like (좋아요)

중복 방지를 위한 복합 인덱스를 사용합니다.

```java
@Document(collection = "likes")
@CompoundIndex(
    name = "postId_userId_unique",
    def = "{'postId': 1, 'userId': 1}",
    unique = true
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

**동시성 제어:**
```java
// MongoDB unique index로 중복 방지
// DuplicateKeyException 발생 시 이미 좋아요 누름

try {
    likeRepository.save(like);
    post.incrementLikeCount();  // 역정규화 카운터 증가
} catch (DuplicateKeyException e) {
    throw new AlreadyLikedException();
}
```

### 2.4 Series (시리즈)

순서가 있는 게시물 묶음을 관리합니다.

```java
@Document(collection = "series")
public class Series {
    @Id
    private String id;

    private String name;
    private String description;

    @Indexed
    private String authorId;
    private String authorName;

    private String thumbnailUrl;

    private List<String> postIds;  // 순서 유지 (인덱스 = 순서)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**포스트 순서 관리:**
```java
public void reorderPosts(List<String> newPostIds) {
    // 기존 ID와 동일한지 검증
    if (!this.postIds.containsAll(newPostIds) ||
        !newPostIds.containsAll(this.postIds)) {
        throw new IllegalArgumentException("Post IDs mismatch");
    }
    this.postIds = new ArrayList<>(newPostIds);
}
```

### 2.5 Tag (태그)

역정규화된 카운터로 인기 태그를 빠르게 조회합니다.

```java
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;

    @TextIndexed
    @Indexed(unique = true)
    private String name;

    private Long postCount = 0L;    // 역정규화
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    public static String normalizeName(String name) {
        return name.trim().toLowerCase();
    }
}
```

---

## 3. Embedding vs Reference 전략

### 3.1 Embedding 선택 (Post 내부)

| 필드 | 이유 |
|------|------|
| `tags[]` | 항상 함께 조회, 크기 제한적 |
| `images[]` | 게시물과 생명주기 동일 |
| `authorName` | 자주 표시, 변경 드묾 |

### 3.2 Reference 선택 (별도 Collection)

| Document | 이유 |
|----------|------|
| `Comment` | 수량 무제한, 별도 조회 필요 |
| `Like` | 중복 방지 인덱스 필요 |
| `Series` | 여러 Post 참조, 독립 관리 |
| `Tag` | 정규화 필요, 통계 집계 |

### 3.3 역정규화 전략

```
┌─────────────┐        ┌─────────────┐
│    Post     │        │   Comment   │
├─────────────┤        ├─────────────┤
│ commentCount│◄───────│   postId    │
│             │  count │             │
└─────────────┘        └─────────────┘

┌─────────────┐        ┌─────────────┐
│    Post     │        │    Like     │
├─────────────┤        ├─────────────┤
│  likeCount  │◄───────│   postId    │
│             │  count │             │
└─────────────┘        └─────────────┘

┌─────────────┐        ┌─────────────┐
│    Tag      │        │    Post     │
├─────────────┤        ├─────────────┤
│  postCount  │◄───────│   tags[]    │
│             │  count │             │
└─────────────┘        └─────────────┘
```

**장점:**
- 집계 쿼리 없이 즉시 조회
- 목록 페이지 성능 최적화

**단점:**
- 데이터 불일치 가능성
- 업데이트 트랜잭션 필요

---

## 4. 인덱스 설계

### 4.1 Post Collection

```javascript
// 복합 인덱스: 작성자별 발행 글 최신순
db.posts.createIndex({ authorId: 1, status: 1, publishedAt: -1 })

// 텍스트 인덱스: 제목(가중치 2) + 내용(가중치 1)
db.posts.createIndex(
  { title: "text", content: "text" },
  { weights: { title: 2, content: 1 }, default_language: "korean" }
)

// 태그 검색
db.posts.createIndex({ tags: 1, publishedAt: -1 })
```

### 4.2 Comment Collection

```javascript
// 게시물별 댓글 조회
db.comments.createIndex({ postId: 1, createdAt: 1 })

// 대댓글 조회
db.comments.createIndex({ parentCommentId: 1 })

// 작성자별 댓글 조회
db.comments.createIndex({ authorId: 1, createdAt: -1 })
```

### 4.3 Like Collection

```javascript
// 중복 방지 (복합 unique)
db.likes.createIndex(
  { postId: 1, userId: 1 },
  { unique: true }
)

// 사용자별 좋아요 목록
db.likes.createIndex({ userId: 1, createdAt: -1 })
```

---

## 5. 주요 비즈니스 흐름

### 5.1 게시물 발행 흐름

```
1. 초안 작성
   Post.create(DRAFT)

2. 이미지 업로드
   S3 → images[] 추가

3. 발행
   Post.publish()
   → status = PUBLISHED
   → publishedAt = now()

4. 태그 카운터 업데이트
   Tag.incrementPostCount() (각 태그)
```

### 5.2 좋아요 흐름

```
1. 좋아요 추가
   Like.save()
   → DuplicateKeyException 시 이미 좋아요

2. Post 카운터 증가
   Post.incrementLikeCount()

3. 좋아요 취소
   Like.delete()
   Post.decrementLikeCount()
```

### 5.3 댓글 흐름

```
1. 댓글 작성
   Comment.save()
   Post.incrementCommentCount()

2. 대댓글 작성
   Comment.save(parentCommentId = 부모 ID)
   Post.incrementCommentCount()

3. 댓글 삭제 (Soft)
   Comment.delete() → isDeleted = true
   Post.decrementCommentCount()
```

---

## 6. 성능 최적화 패턴

### 6.1 Projection (필요한 필드만 조회)

```java
// 목록 조회: content 제외
@Query(value = "{ 'status': 'PUBLISHED' }",
       fields = "{ 'content': 0 }")
List<Post> findPublishedPostSummaries();
```

### 6.2 Cursor 기반 페이지네이션

```java
// Offset 방식 (성능 저하)
Page<Post> findAll(Pageable pageable);

// Cursor 방식 (권장)
List<Post> findByPublishedAtLessThan(
    LocalDateTime cursor,
    Pageable pageable
);
```

### 6.3 조회수 일괄 업데이트

```java
// 개별 업데이트 (비효율)
post.incrementViewCount();
postRepository.save(post);

// 일괄 업데이트 (권장)
mongoTemplate.updateFirst(
    Query.query(Criteria.where("id").is(postId)),
    new Update().inc("viewCount", 1),
    Post.class
);
```

---

## 7. 핵심 정리

| 개념 | 설명 |
|------|------|
| **Root Document** | Post - 블로그 콘텐츠의 핵심 |
| **Embedding** | tags[], images[], authorName |
| **Reference** | Comment, Like, Series, Tag |
| **역정규화** | viewCount, likeCount, commentCount, postCount |
| **Soft Delete** | Comment.isDeleted |
| **중복 방지** | Like - 복합 unique index |
| **텍스트 검색** | @TextIndexed (title, content) |

---

## 다음 학습

- [Post Document 심화](./post-document.md)
- [Comment 대댓글 구조](./comment-document.md)
- [MongoDB Aggregation 예제](../mongodb/aggregation-examples.md)
