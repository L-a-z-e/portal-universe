# Blog Service 아키텍처

## 도메인 구조

```
blog-service/
├── post/              # 게시물
│   ├── domain/       # Post, PostStatus, PostSortType
│   ├── dto/          # Request/Response DTOs
│   ├── repository/   # PostRepository (MongoDB)
│   ├── service/      # PostService
│   └── controller/   # PostController
├── comment/          # 댓글
│   ├── domain/       # Comment
│   └── ...
├── series/           # 시리즈
│   ├── domain/       # Series
│   └── ...
├── tag/              # 태그
│   ├── domain/       # Tag
│   └── ...
├── file/             # 파일 업로드
│   ├── config/       # S3Config
│   └── ...
└── config/           # 설정
    ├── MongoConfig
    ├── SecurityConfig
    └── OpenApiConfig
```

## 데이터 모델

### Post

```java
@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    private String title;
    private String content;
    private String authorId;
    private String category;
    private List<String> tags;
    private PostStatus status;        // DRAFT, PUBLISHED, ARCHIVED
    private String seriesId;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
```

### Comment

```java
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;
    private String postId;
    private String authorId;
    private String content;
    private String parentId;          // 대댓글
    private LocalDateTime createdAt;
}
```

### Series

```java
@Document(collection = "series")
public class Series {
    @Id
    private String id;
    private String title;
    private String description;
    private String authorId;
    private List<String> postIds;     // 순서 유지
    private LocalDateTime createdAt;
}
```

## 게시물 상태 흐름

```
     ┌────────────┐
     │   DRAFT    │ ←─────────────────┐
     └─────┬──────┘                   │
           │ publish()                │
           ▼                          │
     ┌────────────┐                   │
     │ PUBLISHED  │                   │ archive()
     └─────┬──────┘                   │ (restore)
           │ archive()                │
           ▼                          │
     ┌────────────┐                   │
     │  ARCHIVED  │ ──────────────────┘
     └────────────┘
```

## 검색 기능

### 단순 검색

```
GET /posts/search?keyword=spring
```

제목, 본문에서 키워드 검색

### 고급 검색

```json
POST /posts/search/advanced
{
  "keyword": "spring",
  "category": "tech",
  "tags": ["java", "backend"],
  "authorId": "user-123",
  "status": "PUBLISHED",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31",
  "sortType": "LATEST",
  "page": 0,
  "size": 10
}
```

### MongoDB 인덱스

```javascript
// 복합 인덱스
db.posts.createIndex({ status: 1, publishedAt: -1 })
db.posts.createIndex({ authorId: 1, status: 1 })
db.posts.createIndex({ tags: 1, status: 1 })

// 텍스트 인덱스 (전문 검색)
db.posts.createIndex({ title: "text", content: "text" })
```

## 통계 기능

### 카테고리 통계

```java
public List<CategoryStats> getCategoryStats() {
    return mongoTemplate.aggregate(
        Aggregation.newAggregation(
            match(Criteria.where("status").is("PUBLISHED")),
            group("category").count().as("postCount"),
            sort(Sort.Direction.DESC, "postCount")
        ),
        "posts",
        CategoryStats.class
    ).getMappedResults();
}
```

### 태그 통계

```json
GET /posts/stats/tags?limit=10

[
  { "tag": "spring", "count": 45 },
  { "tag": "java", "count": 38 },
  { "tag": "kubernetes", "count": 22 }
]
```

## 파일 업로드

### S3 연동

```
┌──────────────┐      ┌────────────────┐
│ Blog Frontend│      │ Blog Service   │
└──────┬───────┘      └───────┬────────┘
       │                      │
       │ 1. Upload Request    │
       │ ─────────────────────▶
       │                      │
       │                      │ 2. Generate Presigned URL
       │                      │    or Direct Upload
       │                      │
       │ 3. S3 Upload         ▼
       │ ─────────────────────────────▶ S3 Bucket
       │
       │ 4. Return URL        │
       │ ◀─────────────────────
```

### 지원 파일 형식

| 타입 | 확장자 | 최대 크기 |
|------|--------|-----------|
| 이미지 | jpg, png, gif, webp | 10MB |
| 문서 | pdf, doc, docx | 50MB |

## 보안 설정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/posts/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            )
            .build();
    }
}
```

## 에러 코드

| 코드 | 설명 |
|------|------|
| B001 | 중복 제목 |
| B002 | 게시물 없음 |
| B003 | 권한 없음 |
| B004 | 댓글 없음 |
| B005 | 시리즈 없음 |
| B006 | 파일 업로드 실패 |
