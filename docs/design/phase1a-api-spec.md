# Phase 1-A Blog API Specification

## 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | 1.0 |
| 작성일 | 2026-01-21 |
| 대상 서비스 | blog-service |
| Phase | 1-A |
| 상태 | Draft |

---

## 1. 개요

### 1.1 목적

Phase 1-A에서 추가되는 블로그 UX 개선 기능의 Backend API 명세입니다.

### 1.2 Base URL

```
http://localhost:8080/api/blog
```

### 1.3 API 구조

```
/posts
├── /{postId}/like              (좋아요 시스템)
│   ├── POST                    - 좋아요 토글
│   └── GET                     - 좋아요 상태 확인
├── /{postId}/likes             (좋아요 사용자 목록)
│   └── GET                     - 좋아요한 사용자 목록
└── /{postId}/navigation        (네비게이션)
    └── GET                     - 이전/다음 포스트 + 시리즈 네비게이션
```

### 1.4 인증

| 엔드포인트 | 인증 필요 | 역할 |
|-----------|----------|------|
| `POST /posts/{postId}/like` | O | USER |
| `GET /posts/{postId}/like` | O | USER |
| `GET /posts/{postId}/likes` | X | Public |
| `GET /posts/{postId}/navigation` | X | Public |

---

## 2. 공통 응답 형식

### 2.1 성공 응답

```json
{
  "success": true,
  "data": {
    // 실제 데이터
  },
  "error": null
}
```

### 2.2 에러 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "B020",
    "message": "Post not found"
  }
}
```

### 2.3 HTTP 상태 코드

| 코드 | 의미 | 사용 상황 |
|------|------|----------|
| 200 | OK | 조회, 좋아요 토글 성공 |
| 400 | Bad Request | 유효하지 않은 요청 |
| 401 | Unauthorized | 인증 토큰 없음/만료 |
| 404 | Not Found | 게시물/좋아요 미존재 |
| 409 | Conflict | 중복 좋아요 (멱등성 처리 시 200 반환) |

---

## 3. 에러 코드 정의

### 3.1 Phase 1-A 에러 코드 (B020-B029)

| 코드 | HTTP | 메시지 | 설명 | 원인 |
|------|------|--------|------|------|
| B020 | 404 | Post not found | 게시물을 찾을 수 없음 | 존재하지 않는 postId |
| B021 | 409 | Already liked this post | 이미 좋아요한 게시물 | 중복 좋아요 시도 (멱등성 처리 시 사용 안함) |
| B022 | 404 | Like not found | 좋아요 기록을 찾을 수 없음 | 좋아요하지 않은 게시물에 취소 시도 |

### 3.2 에러 코드 Java 정의

```java
// BlogErrorCode.java에 추가
/**
 * 좋아요 시스템 에러 코드 (Phase 1-A)
 */
LIKE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B020", "Post not found"),
ALREADY_LIKED(HttpStatus.CONFLICT, "B021", "Already liked this post"),
LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "B022", "Like not found");
```

---

## 4. 좋아요 시스템 API

### 4.1 좋아요 토글

좋아요가 없으면 추가하고, 있으면 취소합니다 (멱등성 보장).

**Endpoint**: `POST /posts/{postId}/like`

**권한**: USER (로그인 필요)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| postId | String | O | 게시물 ID (MongoDB ObjectId) |

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
```

**Request Body**: 없음

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "postId": "678a1b2c3d4e5f6789012345",
    "liked": true,
    "likeCount": 128
  },
  "error": null
}
```

**Response Schema**:

| 필드 | 타입 | 설명 |
|------|------|------|
| postId | String | 게시물 ID |
| liked | Boolean | 현재 좋아요 상태 (true: 좋아요됨, false: 취소됨) |
| likeCount | Long | 현재 총 좋아요 수 |

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 401 | C002 | 인증 토큰 없음 또는 만료 |
| 404 | B020 | 존재하지 않는 게시물 |

**Example - cURL**:
```bash
curl -X POST http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/like \
  -H "Authorization: Bearer eyJhbGc..."
```

**Example - JavaScript (Fetch)**:
```javascript
const response = await fetch(
  `http://localhost:8080/api/blog/posts/${postId}/like`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
const data = await response.json();
// data.data.liked → true (좋아요 추가) 또는 false (좋아요 취소)
```

**비즈니스 로직**:
1. JWT에서 userId 추출
2. Like 존재 여부 확인 (postId + userId 조합)
3. 존재하면 삭제, 없으면 생성 (Toggle)
4. Post.likeCount 업데이트 (increment/decrement)
5. 현재 상태 반환

---

### 4.2 좋아요 상태 확인

현재 사용자의 특정 게시물 좋아요 여부를 확인합니다.

**Endpoint**: `GET /posts/{postId}/like`

**권한**: USER (로그인 필요)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| postId | String | O | 게시물 ID |

**Request Headers**:
```http
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "postId": "678a1b2c3d4e5f6789012345",
    "liked": true,
    "likeCount": 128,
    "likedAt": "2026-01-21T10:30:00"
  },
  "error": null
}
```

**Response Schema**:

| 필드 | 타입 | 설명 |
|------|------|------|
| postId | String | 게시물 ID |
| liked | Boolean | 좋아요 여부 |
| likeCount | Long | 현재 총 좋아요 수 |
| likedAt | LocalDateTime | 좋아요한 시간 (liked=false면 null) |

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 401 | C002 | 인증 토큰 없음 또는 만료 |
| 404 | B020 | 존재하지 않는 게시물 |

**Example - cURL**:
```bash
curl -X GET http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/like \
  -H "Authorization: Bearer eyJhbGc..."
```

---

### 4.3 좋아요한 사용자 목록

특정 게시물에 좋아요한 사용자 목록을 조회합니다.

**Endpoint**: `GET /posts/{postId}/likes`

**권한**: Public (인증 불필요)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| postId | String | O | 게시물 ID |

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Integer | X | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | X | 20 | 페이지 크기 (최대 100) |

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": "user-uuid-1234",
        "username": "johndoe",
        "profileImageUrl": "https://example.com/profile/johndoe.jpg",
        "likedAt": "2026-01-21T10:30:00"
      },
      {
        "userId": "user-uuid-5678",
        "username": "janedoe",
        "profileImageUrl": null,
        "likedAt": "2026-01-21T09:15:00"
      }
    ],
    "totalElements": 128,
    "totalPages": 7,
    "page": 0,
    "size": 20,
    "first": true,
    "last": false
  },
  "error": null
}
```

**Response Schema**:

| 필드 | 타입 | 설명 |
|------|------|------|
| content[].userId | String | 사용자 ID |
| content[].username | String | 사용자 이름 |
| content[].profileImageUrl | String | 프로필 이미지 URL (nullable) |
| content[].likedAt | LocalDateTime | 좋아요한 시간 |
| totalElements | Long | 전체 좋아요 수 |
| totalPages | Integer | 전체 페이지 수 |
| page | Integer | 현재 페이지 번호 |
| size | Integer | 페이지 크기 |
| first | Boolean | 첫 페이지 여부 |
| last | Boolean | 마지막 페이지 여부 |

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 404 | B020 | 존재하지 않는 게시물 |

**Example - cURL**:
```bash
curl -X GET "http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/likes?page=0&size=20"
```

---

## 5. 네비게이션 API

### 5.1 이전/다음 포스트 네비게이션

현재 게시물 기준으로 이전/다음 게시물과 시리즈 정보를 조회합니다.

**Endpoint**: `GET /posts/{postId}/navigation`

**권한**: Public (인증 불필요)

**Path Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| postId | String | O | 현재 게시물 ID |

**Query Parameters**:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| scope | String | X | `all` | 네비게이션 범위 (`all`, `author`, `category`, `series`) |

**Response (200 OK)**:

```json
{
  "success": true,
  "data": {
    "previous": {
      "id": "678a1b2c3d4e5f6789012344",
      "title": "Kubernetes 배포 자동화 기초",
      "publishedAt": "2026-01-20T10:00:00",
      "thumbnailUrl": "https://example.com/thumbnails/k8s-deploy.jpg"
    },
    "next": {
      "id": "678a1b2c3d4e5f6789012346",
      "title": "React Hook 패턴 심화",
      "publishedAt": "2026-01-22T14:00:00",
      "thumbnailUrl": null
    },
    "series": {
      "id": "series-uuid-1234",
      "title": "DevOps 완벽 가이드",
      "description": "DevOps 입문부터 실전까지",
      "totalPosts": 10,
      "currentPosition": 5,
      "previousInSeries": {
        "id": "678a1b2c3d4e5f6789012340",
        "title": "CI/CD 파이프라인 구축",
        "position": 4
      },
      "nextInSeries": {
        "id": "678a1b2c3d4e5f6789012350",
        "title": "모니터링과 로깅",
        "position": 6
      }
    }
  },
  "error": null
}
```

**Response Schema**:

| 필드 | 타입 | 설명 |
|------|------|------|
| previous | Object | 이전 게시물 정보 (없으면 null) |
| previous.id | String | 게시물 ID |
| previous.title | String | 게시물 제목 |
| previous.publishedAt | LocalDateTime | 발행일시 |
| previous.thumbnailUrl | String | 썸네일 URL (nullable) |
| next | Object | 다음 게시물 정보 (없으면 null) |
| next.id | String | 게시물 ID |
| next.title | String | 게시물 제목 |
| next.publishedAt | LocalDateTime | 발행일시 |
| next.thumbnailUrl | String | 썸네일 URL (nullable) |
| series | Object | 시리즈 정보 (소속된 시리즈가 없으면 null) |
| series.id | String | 시리즈 ID |
| series.title | String | 시리즈 제목 |
| series.description | String | 시리즈 설명 |
| series.totalPosts | Integer | 시리즈 내 총 게시물 수 |
| series.currentPosition | Integer | 현재 게시물의 시리즈 내 위치 (1부터 시작) |
| series.previousInSeries | Object | 시리즈 내 이전 게시물 (없으면 null) |
| series.nextInSeries | Object | 시리즈 내 다음 게시물 (없으면 null) |

**Error Responses**:

| HTTP | 에러 코드 | 발생 조건 |
|------|----------|----------|
| 404 | B020 | 존재하지 않는 게시물 |

**Example - cURL**:
```bash
# 전체 게시물 기준 네비게이션
curl -X GET "http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/navigation"

# 같은 작성자 게시물 기준 네비게이션
curl -X GET "http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/navigation?scope=author"

# 같은 카테고리 게시물 기준 네비게이션
curl -X GET "http://localhost:8080/api/blog/posts/678a1b2c3d4e5f6789012345/navigation?scope=category"
```

**Scope별 동작**:

| Scope | 설명 |
|-------|------|
| `all` | 모든 발행된 게시물 기준 (기본값) |
| `author` | 같은 작성자의 게시물 기준 |
| `category` | 같은 카테고리의 게시물 기준 |
| `series` | 시리즈 내 게시물만 (시리즈에 속한 경우) |

**정렬 기준**:
- 기본: `publishedAt DESC` (최신순)
- 이전 게시물: 현재 게시물보다 `publishedAt`이 이전인 것 중 가장 최근
- 다음 게시물: 현재 게시물보다 `publishedAt`이 이후인 것 중 가장 오래된 것

---

## 6. 데이터 모델

### 6.1 Like Entity

**Collection**: `likes`

```javascript
{
  "_id": ObjectId("678a1b2c3d4e5f6789abcdef"),
  "postId": "678a1b2c3d4e5f6789012345",
  "userId": "user-uuid-1234",
  "createdAt": ISODate("2026-01-21T10:30:00Z")
}
```

**Java Entity**:

```java
@Document(collection = "likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String postId;

    @Indexed
    @NotBlank
    private String userId;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Like(String postId, String userId) {
        this.postId = postId;
        this.userId = userId;
    }
}
```

### 6.2 DTO Definitions

**LikeToggleResponse**:
```java
public record LikeToggleResponse(
    String postId,
    boolean liked,
    Long likeCount
) {
    public static LikeToggleResponse of(String postId, boolean liked, Long likeCount) {
        return new LikeToggleResponse(postId, liked, likeCount);
    }
}
```

**LikeStatusResponse**:
```java
public record LikeStatusResponse(
    String postId,
    boolean liked,
    Long likeCount,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime likedAt
) {
    public static LikeStatusResponse of(String postId, boolean liked,
                                         Long likeCount, LocalDateTime likedAt) {
        return new LikeStatusResponse(postId, liked, likeCount, likedAt);
    }
}
```

**LikeUserResponse**:
```java
public record LikeUserResponse(
    String userId,
    String username,
    String profileImageUrl,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime likedAt
) {}
```

**PostNavigationResponse**:
```java
public record PostNavigationResponse(
    NavigationPost previous,
    NavigationPost next,
    SeriesNavigation series
) {
    public record NavigationPost(
        String id,
        String title,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime publishedAt,
        String thumbnailUrl
    ) {}

    public record SeriesNavigation(
        String id,
        String title,
        String description,
        int totalPosts,
        int currentPosition,
        SeriesPost previousInSeries,
        SeriesPost nextInSeries
    ) {}

    public record SeriesPost(
        String id,
        String title,
        int position
    ) {}
}
```

---

## 7. MongoDB 인덱스 전략

### 7.1 likes Collection 인덱스

```javascript
// 1. 유니크 복합 인덱스 (중복 좋아요 방지)
db.likes.createIndex(
  { "postId": 1, "userId": 1 },
  { unique: true, name: "idx_like_unique" }
)

// 2. 사용자별 좋아요 목록 조회 (최신순)
db.likes.createIndex(
  { "userId": 1, "createdAt": -1 },
  { name: "idx_user_likes" }
)

// 3. 게시물별 좋아요 목록 조회 (최신순)
db.likes.createIndex(
  { "postId": 1, "createdAt": -1 },
  { name: "idx_post_likes" }
)
```

### 7.2 posts Collection 인덱스 (추가)

```javascript
// 1. 네비게이션용 (발행일 기준)
db.posts.createIndex(
  { "status": 1, "publishedAt": -1 },
  { name: "idx_nav_all" }
)

// 2. 작성자별 네비게이션
db.posts.createIndex(
  { "status": 1, "authorId": 1, "publishedAt": -1 },
  { name: "idx_nav_author" }
)

// 3. 카테고리별 네비게이션
db.posts.createIndex(
  { "status": 1, "category": 1, "publishedAt": -1 },
  { name: "idx_nav_category" }
)

// 4. 좋아요 수 기반 정렬 (트렌딩용)
db.posts.createIndex(
  { "status": 1, "likeCount": -1, "publishedAt": -1 },
  { name: "idx_trending" }
)
```

### 7.3 인덱스 사용 쿼리 예시

**좋아요 토글 (중복 체크)**:
```javascript
// idx_like_unique 사용
db.likes.findOne({ postId: "...", userId: "..." })
```

**이전 게시물 조회**:
```javascript
// idx_nav_all 사용
db.posts.find({
  status: "PUBLISHED",
  publishedAt: { $lt: currentPostPublishedAt }
}).sort({ publishedAt: -1 }).limit(1)
```

**좋아요한 사용자 목록**:
```javascript
// idx_post_likes 사용
db.likes.find({ postId: "..." })
  .sort({ createdAt: -1 })
  .skip(page * size)
  .limit(size)
```

---

## 8. API 테스트 가이드

### 8.1 Postman Collection

**환경 변수**:
```json
{
  "base_url": "http://localhost:8080/api/blog",
  "user_token": "YOUR_JWT_TOKEN_HERE",
  "post_id": "678a1b2c3d4e5f6789012345"
}
```

### 8.2 테스트 시나리오

#### 시나리오 1: 좋아요 추가 및 취소

```bash
# 1. 좋아요 상태 확인 (초기: liked=false)
curl -X GET "{{base_url}}/posts/{{post_id}}/like" \
  -H "Authorization: Bearer {{user_token}}"

# Expected: { "data": { "liked": false, "likeCount": 0 } }

# 2. 좋아요 토글 (추가)
curl -X POST "{{base_url}}/posts/{{post_id}}/like" \
  -H "Authorization: Bearer {{user_token}}"

# Expected: { "data": { "liked": true, "likeCount": 1 } }

# 3. 좋아요 상태 확인 (변경 후: liked=true)
curl -X GET "{{base_url}}/posts/{{post_id}}/like" \
  -H "Authorization: Bearer {{user_token}}"

# Expected: { "data": { "liked": true, "likeCount": 1, "likedAt": "..." } }

# 4. 좋아요 토글 (취소)
curl -X POST "{{base_url}}/posts/{{post_id}}/like" \
  -H "Authorization: Bearer {{user_token}}"

# Expected: { "data": { "liked": false, "likeCount": 0 } }
```

#### 시나리오 2: 네비게이션 조회

```bash
# 1. 기본 네비게이션 조회
curl -X GET "{{base_url}}/posts/{{post_id}}/navigation"

# Expected: previous, next, series 정보 반환

# 2. 작성자 기준 네비게이션
curl -X GET "{{base_url}}/posts/{{post_id}}/navigation?scope=author"

# Expected: 같은 작성자의 이전/다음 게시물 반환
```

#### 시나리오 3: 비인증 사용자 좋아요 시도

```bash
# 토큰 없이 좋아요 시도
curl -X POST "{{base_url}}/posts/{{post_id}}/like"

# Expected: 401 Unauthorized
# {
#   "success": false,
#   "data": null,
#   "error": {
#     "code": "C002",
#     "message": "Unauthorized"
#   }
# }
```

---

## 9. 구현 체크리스트

### 9.1 Backend 구현 항목

- [ ] **Like Entity** 생성 (`like/domain/Like.java`)
- [ ] **LikeRepository** 생성 (`like/repository/LikeRepository.java`)
- [ ] **LikeService** 생성 (`like/service/LikeService.java`)
- [ ] **LikeController** 생성 (`like/controller/LikeController.java`)
- [ ] **DTO 클래스** 생성
  - [ ] `LikeToggleResponse`
  - [ ] `LikeStatusResponse`
  - [ ] `LikeUserResponse`
  - [ ] `PostNavigationResponse`
- [ ] **BlogErrorCode** 에러 코드 추가 (B020-B022)
- [ ] **PostService** 네비게이션 메서드 추가
- [ ] **MongoDB 인덱스** 생성 스크립트
- [ ] **Unit Tests** 작성
- [ ] **Integration Tests** 작성

### 9.2 디렉토리 구조 (예상)

```
services/blog-service/src/main/java/.../blogservice/
├── like/
│   ├── controller/
│   │   └── LikeController.java
│   ├── domain/
│   │   └── Like.java
│   ├── dto/
│   │   ├── LikeToggleResponse.java
│   │   ├── LikeStatusResponse.java
│   │   └── LikeUserResponse.java
│   ├── repository/
│   │   └── LikeRepository.java
│   └── service/
│       ├── LikeService.java
│       └── LikeServiceImpl.java
├── post/
│   ├── dto/
│   │   └── PostNavigationResponse.java  (추가)
│   └── ...
└── exception/
    └── BlogErrorCode.java  (B020-B022 추가)
```

---

## 10. 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-01-21 | 초기 작성 | Architect Agent |
