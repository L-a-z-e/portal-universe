---
id: api-blog
title: Blog Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-02-17
author: Laze
tags: [api, blog, mongodb, post, comment, series, tag, file, like]
related:
  - PRD-001
---

# Blog Service API

> MongoDB 기반 블로그 콘텐츠 관리 서비스 API 명세서

---

## 개요

| 항목 | 내용 |
|------|------|
| **Base URL (Gateway)** | `/api/v1/blog` |
| **직접 URL** | `http://localhost:8082` |
| **인증** | Bearer Token (JWT) - Gateway에서 `X-User-*` 헤더로 전달 |
| **데이터베이스** | MongoDB |

### Gateway 라우팅

```
Gateway: /api/v1/blog/{path} → StripPrefix=3 → blog-service:8082/{path}
```

- GET 요청은 기본 permit-all (인증 없이 접근 가능)
- POST/PUT/DELETE/PATCH는 인증 필요 (일부 예외)
- 파일 업로드: 별도 라우트, 최대 10MB

---

## API 목록

### Post API (`/posts`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/posts` | 게시물 생성 | ✅ | `PostResponse` |
| GET | `/posts` | 발행 게시물 목록 (페이징) | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/all` | 전체 게시물 조회 | ✅ ADMIN | `Page<PostResponse>` |
| GET | `/posts/{postId}` | 게시물 상세 조회 | ❌ | `PostResponse` |
| GET | `/posts/{postId}/view` | 조회수 증가 + 상세 조회 | 선택 | `PostResponse` |
| PUT | `/posts/{postId}` | 게시물 수정 | ✅ | `PostResponse` |
| DELETE | `/posts/{postId}` | 게시물 삭제 | ✅ | `Void` |
| PATCH | `/posts/{postId}/status` | 게시물 상태 변경 | ✅ | `PostResponse` |
| GET | `/posts/author/{authorId}` | 작성자별 게시물 목록 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/my` | 내 게시물 목록 | ✅ | `Page<PostSummaryResponse>` |
| GET | `/posts/category/{category}` | 카테고리별 게시물 조회 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/tags` | 태그별 게시물 조회 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/popular` | 인기 게시물 조회 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/trending` | 트렌딩 게시물 조회 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/recent` | 최근 게시물 조회 | ❌ | `List<PostSummaryResponse>` |
| GET | `/posts/{postId}/related` | 연관 게시물 조회 | ❌ | `List<PostSummaryResponse>` |
| GET | `/posts/search` | 게시물 단순 검색 | ❌ | `Page<PostSummaryResponse>` |
| POST | `/posts/search/advanced` | 게시물 고급 검색 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/stats/categories` | 카테고리 통계 조회 | ❌ | `List<CategoryStats>` |
| GET | `/posts/stats/tags` | 인기 태그 통계 조회 | ❌ | `List<TagStatsResponse>` |
| GET | `/posts/stats/author/{authorId}` | 작성자 통계 조회 | ❌ | `AuthorStats` |
| GET | `/posts/stats/author/{authorId}/categories` | 작성자별 카테고리 통계 | ❌ | `List<CategoryStats>` |
| GET | `/posts/stats/author/{authorId}/tags` | 작성자별 태그 통계 | ❌ | `List<TagStatsResponse>` |
| GET | `/posts/stats/blog` | 전체 블로그 통계 조회 | ❌ | `BlogStats` |
| GET | `/posts/product/{productId}` | 상품별 게시물 조회 | ❌ | `List<PostResponse>` |
| GET | `/posts/feed` | 피드 게시물 조회 | ❌ | `Page<PostSummaryResponse>` |
| GET | `/posts/{postId}/navigation` | 이전/다음 게시물 네비게이션 | ❌ | `PostNavigationResponse` |

### Like API (`/posts/{postId}`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/posts/{postId}/like` | 좋아요 토글 | ✅ | `LikeToggleResponse` |
| GET | `/posts/{postId}/like` | 좋아요 여부 확인 | ✅ | `LikeStatusResponse` |
| GET | `/posts/{postId}/likes` | 좋아요한 사용자 목록 (페이징) | ❌ | `Page<LikerResponse>` |

### Comment API (`/comments`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/comments` | 댓글 생성 | ✅ | `CommentResponse` |
| GET | `/comments/post/{postId}` | 포스트별 댓글 목록 조회 | ❌ | `List<CommentResponse>` |
| PUT | `/comments/{commentId}` | 댓글 수정 | ✅ | `CommentResponse` |
| DELETE | `/comments/{commentId}` | 댓글 삭제 | ✅ | `Void` |

### Series API (`/series`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/series` | 시리즈 생성 | ✅ | `SeriesResponse` |
| GET | `/series` | 전체 시리즈 목록 조회 | ❌ | `List<SeriesListResponse>` |
| GET | `/series/{seriesId}` | 시리즈 상세 조회 | ❌ | `SeriesResponse` |
| PUT | `/series/{seriesId}` | 시리즈 수정 | ✅ | `SeriesResponse` |
| DELETE | `/series/{seriesId}` | 시리즈 삭제 | ✅ | `Void` |
| GET | `/series/author/{authorId}` | 작성자별 시리즈 목록 조회 | ❌ | `List<SeriesListResponse>` |
| GET | `/series/my` | 내 시리즈 목록 조회 | ✅ | `List<SeriesListResponse>` |
| GET | `/series/{seriesId}/posts` | 시리즈 포스트 목록 조회 | ❌ | `List<PostSummaryResponse>` |
| POST | `/series/{seriesId}/posts/{postId}` | 시리즈에 포스트 추가 | ✅ | `SeriesResponse` |
| DELETE | `/series/{seriesId}/posts/{postId}` | 시리즈에서 포스트 제거 | ✅ | `SeriesResponse` |
| PUT | `/series/{seriesId}/posts/order` | 시리즈 내 포스트 순서 변경 | ✅ | `SeriesResponse` |
| GET | `/series/by-post/{postId}` | 포스트가 포함된 시리즈 조회 | ❌ | `List<SeriesListResponse>` |

### Tag API (`/tags`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/tags` | 태그 생성 | ❌ | `TagResponse` |
| GET | `/tags` | 전체 태그 목록 조회 | ❌ | `List<TagResponse>` |
| GET | `/tags/{tagName}` | 태그 상세 조회 | ❌ | `TagResponse` |
| GET | `/tags/popular` | 인기 태그 조회 | ❌ | `List<TagStatsResponse>` |
| GET | `/tags/recent` | 최근 사용된 태그 조회 | ❌ | `List<TagResponse>` |
| GET | `/tags/search` | 태그 검색 (자동완성) | ❌ | `List<TagResponse>` |
| PATCH | `/tags/{tagName}/description` | 태그 설명 업데이트 | ❌ | `TagResponse` |
| DELETE | `/tags/unused` | 사용되지 않는 태그 일괄 삭제 | ✅ ADMIN | `Void` |
| DELETE | `/tags/{tagName}` | 태그 강제 삭제 | ✅ ADMIN | `Void` |

### File API (`/file`)

| Method | Endpoint | 설명 | 인증 | 반환 타입 |
|--------|----------|------|------|-----------|
| POST | `/file/upload` | 파일 업로드 (S3) | ✅ | `FileUploadResponse` |
| DELETE | `/file/delete` | 파일 삭제 (S3) | ✅ ADMIN | `Void (204)` |

> **참고**: File API는 `ApiResponse` wrapper를 사용하지 않고 직접 `ResponseEntity`를 반환합니다.

---

## DTO 상세

### PostResponse (상세 조회용)

```json
{
  "id": "677ab123c4d5e6f7g8h9i0j1",
  "title": "Spring Boot 완벽 가이드",
  "content": "# Spring Boot란?\n\n스프링 부트는...",
  "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
  "authorId": "user-123",
  "authorName": "홍길동",
  "status": "PUBLISHED",
  "tags": ["spring", "java", "backend"],
  "category": "Backend",
  "metaDescription": "스프링 부트 입문 가이드",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
  "images": ["https://s3.amazonaws.com/bucket/image1.jpg"],
  "viewCount": 150,
  "likeCount": 12,
  "createdAt": "2026-01-18T10:30:00",
  "updatedAt": "2026-01-18T10:30:00",
  "publishedAt": "2026-01-18T10:30:00",
  "productId": "prod-123"
}
```

### PostSummaryResponse (목록 조회용)

```json
{
  "id": "677ab123c4d5e6f7g8h9i0j1",
  "title": "Spring Boot 완벽 가이드",
  "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
  "authorId": "user-123",
  "authorName": "홍길동",
  "tags": ["spring", "java", "backend"],
  "category": "Backend",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
  "images": ["https://s3.amazonaws.com/bucket/image1.jpg"],
  "viewCount": 150,
  "likeCount": 12,
  "commentCount": 5,
  "publishedAt": "2026-01-18T10:30:00",
  "estimatedReadTime": 8
}
```

> **PostResponse vs PostSummaryResponse**: 목록 API는 `PostSummaryResponse`를 반환합니다. `content`, `metaDescription`, `status`, `productId`, `createdAt`, `updatedAt` 필드가 없고, 대신 `commentCount`와 `estimatedReadTime`이 추가됩니다.

---

## Post API

### 1. 게시물 생성

```http
POST /api/v1/blog/posts
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `title` | string | ✅ | 게시물 제목 | 최대 200자 |
| `content` | string | ✅ | 게시물 본문 (Markdown) | - |
| `summary` | string | ❌ | 게시물 요약 | 최대 500자 |
| `tags` | string[] | ❌ | 태그 목록 (중복 제거) | Set, 최대 20개 |
| `category` | string | ❌ | 카테고리 | - |
| `metaDescription` | string | ❌ | SEO 메타 설명 | 최대 160자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |
| `publishImmediately` | boolean | ❌ | 즉시 발행 여부 | 기본값: false |
| `images` | string[] | ❌ | 본문 이미지 URL 목록 | - |
| `productId` | string | ❌ | 연결된 상품 ID | - |

#### Request Example

```json
{
  "title": "Spring Boot 완벽 가이드",
  "content": "# Spring Boot란?\n\n스프링 부트는...",
  "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
  "tags": ["spring", "java", "backend"],
  "category": "Backend",
  "metaDescription": "스프링 부트 입문 가이드 - 기본부터 고급까지",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
  "publishImmediately": true,
  "images": ["https://s3.amazonaws.com/bucket/image1.jpg"],
  "productId": "prod-123"
}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ab123c4d5e6f7g8h9i0j1",
    "title": "Spring Boot 완벽 가이드",
    "content": "# Spring Boot란?\n\n스프링 부트는...",
    "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
    "authorId": "user-123",
    "authorName": "홍길동",
    "status": "PUBLISHED",
    "tags": ["spring", "java", "backend"],
    "category": "Backend",
    "metaDescription": "스프링 부트 입문 가이드 - 기본부터 고급까지",
    "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
    "images": ["https://s3.amazonaws.com/bucket/image1.jpg"],
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": "2026-01-18T10:30:00",
    "updatedAt": "2026-01-18T10:30:00",
    "publishedAt": "2026-01-18T10:30:00",
    "productId": "prod-123"
  },
  "timestamp": "2026-01-18T10:30:00"
}
```

---

### 2. 발행 게시물 목록 조회

```http
GET /api/v1/blog/posts?page=1&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 (1부터) | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK) - `Page<PostSummaryResponse>`

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 완벽 가이드",
        "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
        "authorId": "user-123",
        "authorName": "홍길동",
        "tags": ["spring", "java"],
        "category": "Backend",
        "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
        "images": [],
        "viewCount": 150,
        "likeCount": 12,
        "commentCount": 5,
        "publishedAt": "2026-01-18T10:30:00",
        "estimatedReadTime": 8
      }
    ],
    "page": 1,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5
  },
  "timestamp": "2026-01-18T10:30:00"
}
```

---

### 3. 전체 게시물 조회 (관리자용)

`ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 권한 필요.

```http
GET /api/v1/blog/posts/all?page=1&size=20
Authorization: Bearer {admin-token}
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 | 1 |
| `size` | int | ❌ | 페이지 크기 | 20 |

#### Response (200 OK) - `Page<PostResponse>`

---

### 4. 게시물 상세 조회

조회수는 증가하지 않습니다.

```http
GET /api/v1/blog/posts/{postId}
```

#### Response (200 OK) - `PostResponse`

```json
{
  "success": true,
  "data": {
    "id": "677ab123c4d5e6f7g8h9i0j1",
    "title": "Spring Boot 완벽 가이드",
    "content": "# Spring Boot란?\n\n스프링 부트는...",
    "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
    "authorId": "user-123",
    "authorName": "홍길동",
    "status": "PUBLISHED",
    "tags": ["spring", "java", "backend"],
    "category": "Backend",
    "metaDescription": "스프링 부트 입문 가이드",
    "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
    "images": ["https://s3.amazonaws.com/bucket/image1.jpg"],
    "viewCount": 150,
    "likeCount": 12,
    "createdAt": "2026-01-18T10:30:00",
    "updatedAt": "2026-01-18T10:30:00",
    "publishedAt": "2026-01-18T10:30:00",
    "productId": null
  },
  "timestamp": "2026-01-18T10:30:00"
}
```

---

### 5. 조회수 증가 + 상세 조회

게시물을 조회하면서 조회수를 1 증가시킵니다. 동일 사용자의 중복 조회는 제한됩니다.

```http
GET /api/v1/blog/posts/{postId}/view
Authorization: Bearer {token}  (선택 - userId 기반 중복 방지용)
```

#### Response (200 OK) - `PostResponse`

---

### 6. 게시물 수정

본인만 수정 가능합니다.

```http
PUT /api/v1/blog/posts/{postId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `title` | string | ✅ | 게시물 제목 | 최대 200자 |
| `content` | string | ✅ | 게시물 본문 | - |
| `summary` | string | ❌ | 게시물 요약 | 최대 500자 |
| `tags` | string[] | ❌ | 태그 목록 (중복 제거) | Set, 최대 20개 |
| `category` | string | ❌ | 카테고리 | - |
| `metaDescription` | string | ❌ | SEO 메타 설명 | 최대 160자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |
| `images` | string[] | ❌ | 본문 이미지 URL 목록 | - |

> **참고**: `PostCreateRequest`와 달리 `publishImmediately`, `productId` 필드가 없습니다.

#### Response (200 OK) - `PostResponse`

---

### 7. 게시물 삭제

본인만 삭제 가능합니다.

```http
DELETE /api/v1/blog/posts/{postId}
Authorization: Bearer {token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 8. 게시물 상태 변경

```http
PATCH /api/v1/blog/posts/{postId}/status
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 가능한 값 |
|------|------|------|------|-----------|
| `newStatus` | string | ✅ | 새로운 상태 | `DRAFT`, `PUBLISHED`, `ARCHIVED` |

#### Response (200 OK) - `PostResponse`

---

### 9. 작성자별 게시물 목록

```http
GET /api/v1/blog/posts/author/{authorId}?page=1&size=10
```

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 10. 내 게시물 목록

상태 필터링 가능합니다.

```http
GET /api/v1/blog/posts/my?status=DRAFT&page=1&size=10
Authorization: Bearer {token}
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 가능한 값 |
|----------|------|------|------|-----------|
| `status` | string | ❌ | 상태 필터 | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| `page` | int | ❌ | 페이지 번호 | 기본값: 0 |
| `size` | int | ❌ | 페이지 크기 | 기본값: 10 |

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 11. 카테고리별 게시물 조회

```http
GET /api/v1/blog/posts/category/{category}?page=1&size=10
```

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 12. 태그별 게시물 조회

다중 태그 지원합니다.

```http
GET /api/v1/blog/posts/tags?tags=spring,java&page=1&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `tags` | string[] | ✅ | 태그 목록 (쉼표 구분) |
| `page` | int | ❌ | 페이지 번호 (기본값: 0) |
| `size` | int | ❌ | 페이지 크기 (기본값: 10) |

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 13. 인기 게시물 조회

조회수 기준 인기 게시물을 조회합니다.

```http
GET /api/v1/blog/posts/popular?page=1&size=10
```

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 14. 트렌딩 게시물 조회

기간별 인기 게시물을 조회합니다. viewCount + likeCount 기준.

```http
GET /api/v1/blog/posts/trending?period=week&page=1&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 가능한 값 | 기본값 |
|----------|------|------|------|-----------|--------|
| `period` | string | ❌ | 기간 필터 | `today`, `week`, `month`, `year` | `week` |
| `page` | int | ❌ | 페이지 번호 | - | 0 |
| `size` | int | ❌ | 페이지 크기 | - | 10 |

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 15. 최근 게시물 조회

```http
GET /api/v1/blog/posts/recent?limit=5
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK) - `List<PostSummaryResponse>`

```json
{
  "success": true,
  "data": [
    {
      "id": "677ab123c4d5e6f7g8h9i0j1",
      "title": "최신 Spring Boot 가이드",
      "summary": "...",
      "authorId": "user-123",
      "authorName": "홍길동",
      "tags": ["spring"],
      "category": "Backend",
      "thumbnailUrl": null,
      "images": [],
      "viewCount": 10,
      "likeCount": 2,
      "commentCount": 0,
      "publishedAt": "2026-01-18T10:30:00",
      "estimatedReadTime": 5
    }
  ],
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 16. 연관 게시물 조회

특정 게시물과 관련된 게시물을 태그/카테고리 기반으로 조회합니다.

```http
GET /api/v1/blog/posts/{postId}/related?limit=5
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK) - `List<PostSummaryResponse>`

---

### 17. 게시물 단순 검색

키워드로 게시물을 검색합니다. 제목, 본문, 요약에서 검색합니다.

```http
GET /api/v1/blog/posts/search?keyword=spring&page=1&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `keyword` | string | ✅ | 검색 키워드 | - |
| `page` | int | ❌ | 페이지 번호 | 1 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 18. 게시물 고급 검색

다양한 조건으로 게시물을 검색합니다.

```http
POST /api/v1/blog/posts/search/advanced
Content-Type: application/json
```

#### Request Body (`PostSearchRequest`)

| 필드 | 타입 | 필수 | 설명 | 기본값 |
|------|------|------|------|--------|
| `keyword` | string | ❌ | 검색 키워드 (제목 + 내용) | - |
| `category` | string | ❌ | 카테고리 필터 | - |
| `tags` | string[] | ❌ | 태그 필터 (다중) | - |
| `status` | string | ❌ | 상태 필터 | - |
| `authorId` | string | ❌ | 작성자 ID 필터 | - |
| `startDate` | string | ❌ | 시작 날짜 (ISO 8601) | - |
| `endDate` | string | ❌ | 종료 날짜 (ISO 8601) | - |
| `sortBy` | string | ❌ | 정렬 기준 | `PUBLISHED_AT` |
| `sortDirection` | string | ❌ | 정렬 방향 | `DESC` |
| `page` | int | ❌ | 페이지 번호 | 1 |
| `size` | int | ❌ | 페이지 크기 (최대 50) | 10 |

**sortBy 가능한 값**: `CREATED_AT`, `PUBLISHED_AT`, `VIEW_COUNT`, `LIKE_COUNT`, `TITLE`

**sortDirection 가능한 값**: `ASC`, `DESC`

#### Request Example

```json
{
  "keyword": "spring",
  "category": "Backend",
  "tags": ["java", "spring"],
  "authorId": "user-123",
  "startDate": "2026-01-01T00:00:00",
  "endDate": "2026-01-31T23:59:59",
  "status": "PUBLISHED",
  "sortBy": "VIEW_COUNT",
  "sortDirection": "DESC",
  "page": 0,
  "size": 10
}
```

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 19. 카테고리 통계 조회

```http
GET /api/v1/blog/posts/stats/categories
```

#### Response (200 OK) - `List<CategoryStats>`

```json
{
  "success": true,
  "data": [
    {
      "categoryName": "Backend",
      "postCount": 42,
      "latestPostDate": "2026-01-18T10:30:00"
    },
    {
      "categoryName": "Frontend",
      "postCount": 35,
      "latestPostDate": "2026-01-17T15:00:00"
    }
  ],
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 20. 인기 태그 통계 조회

```http
GET /api/v1/blog/posts/stats/tags?limit=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 10 |

#### Response (200 OK) - `List<TagStatsResponse>`

```json
{
  "success": true,
  "data": [
    {
      "name": "spring",
      "postCount": 28,
      "totalViews": 15200
    },
    {
      "name": "java",
      "postCount": 25,
      "totalViews": 12800
    }
  ],
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 21. 작성자 통계 조회

```http
GET /api/v1/blog/posts/stats/author/{authorId}
```

#### Response (200 OK) - `AuthorStats`

```json
{
  "success": true,
  "data": {
    "authorId": "user-123",
    "authorName": "홍길동",
    "totalPosts": 42,
    "publishedPosts": 38,
    "totalViews": 15234,
    "totalLikes": 856,
    "firstPostDate": "2025-06-15T10:00:00",
    "lastPostDate": "2026-01-18T10:30:00"
  },
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 22. 작성자별 카테고리 통계 조회

특정 작성자의 게시물을 카테고리별로 집계합니다.

```http
GET /api/v1/blog/posts/stats/author/{authorId}/categories
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `authorId` | string | ✅ | 작성자 UUID |

#### Response (200 OK) - `List<CategoryStats>`

```json
{
  "success": true,
  "data": [
    {
      "categoryName": "Backend",
      "postCount": 15,
      "latestPostDate": "2026-02-17T10:30:00"
    },
    {
      "categoryName": "Frontend",
      "postCount": 8,
      "latestPostDate": "2026-02-15T14:00:00"
    }
  ],
  "timestamp": "2026-02-17T11:00:00"
}
```

---

### 23. 작성자별 태그 통계 조회

특정 작성자의 게시물에서 사용된 태그를 빈도순으로 집계합니다.

```http
GET /api/v1/blog/posts/stats/author/{authorId}/tags?limit=20
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `authorId` | string | ✅ | 작성자 UUID |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 20 |

#### Response (200 OK) - `List<TagStatsResponse>`

```json
{
  "success": true,
  "data": [
    {
      "name": "spring",
      "postCount": 12,
      "totalViews": 0
    },
    {
      "name": "java",
      "postCount": 10,
      "totalViews": 0
    }
  ],
  "timestamp": "2026-02-17T11:00:00"
}
```

---

### 24. 전체 블로그 통계 조회

```http
GET /api/v1/blog/posts/stats/blog
```

#### Response (200 OK) - `BlogStats`

```json
{
  "success": true,
  "data": {
    "totalPosts": 250,
    "publishedPosts": 220,
    "totalViews": 125000,
    "totalLikes": 8500,
    "topCategories": ["Backend", "Frontend", "DevOps"],
    "topTags": ["spring", "java", "react"],
    "lastPostDate": "2026-01-18T10:30:00"
  },
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 23. 상품별 게시물 조회

상품 ID로 연결된 게시물 목록을 조회합니다. 기존 API 호환성 용도.

```http
GET /api/v1/blog/posts/product/{productId}
```

#### Response (200 OK) - `List<PostResponse>`

---

### 24. 피드 게시물 조회

팔로잉 중인 사용자의 게시물을 최신순으로 조회합니다.

```http
GET /api/v1/blog/posts/feed?followingIds=user-1,user-2,user-3&page=1&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `followingIds` | string[] | ✅ | 팔로잉 사용자 UUID 목록 (쉼표 구분) | - |
| `page` | int | ❌ | 페이지 번호 | 1 |
| `size` | int | ❌ | 페이지 크기 | 10 |

> **참고**: 인증 어노테이션이 없으므로 Gateway의 GET permit-all 정책에 따라 인증 없이 호출 가능합니다. 클라이언트에서 팔로잉 목록을 직접 전달해야 합니다.

#### Response (200 OK) - `Page<PostSummaryResponse>`

---

### 25. 이전/다음 게시물 네비게이션

```http
GET /api/v1/blog/posts/{postId}/navigation?scope=all
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `scope` | string | ❌ | 네비게이션 범위 | `all` |

**scope 가능한 값**: `all`, `author`, `category`, `series`

#### Response (200 OK) - `PostNavigationResponse`

```json
{
  "success": true,
  "data": {
    "previousPost": {
      "id": "677ab000c4d5e6f7g8h9i0j0",
      "title": "이전 게시물",
      "summary": "...",
      "authorId": "user-123",
      "authorName": "홍길동",
      "tags": [],
      "category": "Backend",
      "thumbnailUrl": null,
      "images": [],
      "viewCount": 50,
      "likeCount": 3,
      "commentCount": 1,
      "publishedAt": "2026-01-17T10:00:00",
      "estimatedReadTime": 5
    },
    "nextPost": {
      "id": "677ab456c4d5e6f7g8h9i0j4",
      "title": "다음 게시물",
      "summary": "...",
      "authorId": "user-123",
      "authorName": "홍길동",
      "tags": [],
      "category": "Backend",
      "thumbnailUrl": null,
      "images": [],
      "viewCount": 30,
      "likeCount": 1,
      "commentCount": 0,
      "publishedAt": "2026-01-19T10:00:00",
      "estimatedReadTime": 7
    },
    "seriesNavigation": {
      "seriesId": "677dd567e8f9g0h1i2j3k4l5",
      "seriesName": "Spring Boot 마스터 시리즈",
      "currentIndex": 2,
      "totalPosts": 5,
      "previousPostId": "677ab000c4d5e6f7g8h9i0j0",
      "nextPostId": "677ab456c4d5e6f7g8h9i0j4"
    }
  },
  "timestamp": "2026-01-26T10:00:00"
}
```

> `previousPost`, `nextPost`, `seriesNavigation`은 각각 null일 수 있습니다.

---

## Like API

### 1. 좋아요 토글

게시물에 좋아요를 추가하거나 취소합니다. 동일 사용자가 다시 호출하면 좋아요가 취소됩니다.

```http
POST /api/v1/blog/posts/{postId}/like
Authorization: Bearer {token}
```

#### Response (200 OK) - `LikeToggleResponse`

```json
{
  "success": true,
  "data": {
    "liked": true,
    "likeCount": 13
  },
  "timestamp": "2026-01-26T10:00:00"
}
```

---

### 2. 좋아요 상태 확인

현재 사용자가 해당 게시물에 좋아요를 눌렀는지와 전체 좋아요 수를 확인합니다.

```http
GET /api/v1/blog/posts/{postId}/like
Authorization: Bearer {token}
```

#### Response (200 OK) - `LikeStatusResponse`

```json
{
  "success": true,
  "data": {
    "liked": true,
    "likeCount": 13
  },
  "timestamp": "2026-01-26T10:00:00"
}
```

---

### 3. 좋아요한 사용자 목록

게시물에 좋아요를 누른 사용자 목록을 **페이징하여** 조회합니다. 최신순 정렬.

```http
GET /api/v1/blog/posts/{postId}/likes?page=1&size=20
```

#### Query Parameters (Pageable)

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 | 1 |
| `size` | int | ❌ | 페이지 크기 | 20 |
| `sort` | string | ❌ | 정렬 기준 | `createdAt,DESC` |

#### Response (200 OK) - `Page<LikerResponse>`

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "userId": "user-123",
        "userName": "홍길동",
        "likedAt": "2026-01-26T10:00:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 13,
    "totalPages": 1
  },
  "timestamp": "2026-01-26T10:00:00"
}
```

---

## Comment API

### 1. 댓글 생성

게시물에 댓글 또는 대댓글을 작성합니다.

```http
POST /api/v1/blog/comments
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body (`CommentCreateRequest`)

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `postId` | string | ✅ | 게시물 ID | - |
| `parentCommentId` | string | ❌ | 부모 댓글 ID (대댓글인 경우) | - |
| `content` | string | ✅ | 댓글 내용 | 최대 2000자 |

#### Response (200 OK) - `CommentResponse`

```json
{
  "success": true,
  "data": {
    "id": "677cc456d7e8f9g0h1i2j3k4",
    "postId": "677ab123c4d5e6f7g8h9i0j1",
    "authorId": "user-456",
    "authorName": "김철수",
    "content": "좋은 글 감사합니다!",
    "parentCommentId": null,
    "likeCount": 0,
    "isDeleted": false,
    "createdAt": "2026-01-18T11:00:00",
    "updatedAt": "2026-01-18T11:00:00"
  },
  "timestamp": "2026-01-18T11:00:00"
}
```

---

### 2. 포스트별 댓글 목록 조회

특정 포스트의 모든 댓글(대댓글 포함)을 조회합니다.

```http
GET /api/v1/blog/comments/post/{postId}
```

#### Response (200 OK) - `List<CommentResponse>`

```json
{
  "success": true,
  "data": [
    {
      "id": "677cc456d7e8f9g0h1i2j3k4",
      "postId": "677ab123c4d5e6f7g8h9i0j1",
      "authorId": "user-456",
      "authorName": "김철수",
      "content": "좋은 글 감사합니다!",
      "parentCommentId": null,
      "likeCount": 3,
      "isDeleted": false,
      "createdAt": "2026-01-18T11:00:00",
      "updatedAt": "2026-01-18T11:00:00"
    },
    {
      "id": "677cc999d7e8f9g0h1i2j3k9",
      "postId": "677ab123c4d5e6f7g8h9i0j1",
      "authorId": "user-789",
      "authorName": "이영희",
      "content": "저도 도움이 되었어요!",
      "parentCommentId": "677cc456d7e8f9g0h1i2j3k4",
      "likeCount": 1,
      "isDeleted": false,
      "createdAt": "2026-01-18T11:05:00",
      "updatedAt": "2026-01-18T11:05:00"
    }
  ],
  "timestamp": "2026-01-18T11:05:00"
}
```

---

### 3. 댓글 수정

본인만 수정 가능합니다.

```http
PUT /api/v1/blog/comments/{commentId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body (`CommentUpdateRequest`)

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `content` | string | ✅ | 수정할 댓글 내용 | 최대 2000자 |

#### Response (200 OK) - `CommentResponse`

---

### 4. 댓글 삭제

본인만 삭제 가능합니다. 소프트 삭제 방식입니다.

```http
DELETE /api/v1/blog/comments/{commentId}
Authorization: Bearer {token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:10:00"
}
```

---

## Series API

### 1. 시리즈 생성

```http
POST /api/v1/blog/series
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body (`SeriesCreateRequest`)

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 시리즈 제목 | 최대 100자 |
| `description` | string | ❌ | 시리즈 설명 | 최대 500자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |

#### Response (200 OK) - `SeriesResponse`

```json
{
  "success": true,
  "data": {
    "id": "677dd567e8f9g0h1i2j3k4l5",
    "name": "Spring Boot 마스터 시리즈",
    "description": "스프링 부트를 처음부터 끝까지 마스터하는 시리즈",
    "authorId": "user-123",
    "authorName": "홍길동",
    "thumbnailUrl": "https://s3.amazonaws.com/bucket/series-thumb.jpg",
    "postIds": [],
    "postCount": 0,
    "createdAt": "2026-01-18T11:15:00",
    "updatedAt": "2026-01-18T11:15:00"
  },
  "timestamp": "2026-01-18T11:15:00"
}
```

---

### 2. 시리즈 상세 조회

```http
GET /api/v1/blog/series/{seriesId}
```

#### Response (200 OK) - `SeriesResponse`

> `postIds` 배열을 포함합니다.

---

### 3. 시리즈 수정

본인만 수정 가능합니다.

```http
PUT /api/v1/blog/series/{seriesId}
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body (`SeriesUpdateRequest`)

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 시리즈 제목 | 최대 100자 |
| `description` | string | ❌ | 시리즈 설명 | 최대 500자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |

#### Response (200 OK) - `SeriesResponse`

---

### 4. 시리즈 삭제

본인만 삭제 가능합니다.

```http
DELETE /api/v1/blog/series/{seriesId}
Authorization: Bearer {token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:25:00"
}
```

---

### 5. 작성자별 시리즈 목록 조회

```http
GET /api/v1/blog/series/author/{authorId}
```

#### Response (200 OK) - `List<SeriesListResponse>`

```json
{
  "success": true,
  "data": [
    {
      "id": "677dd567e8f9g0h1i2j3k4l5",
      "name": "Spring Boot 마스터 시리즈",
      "description": "스프링 부트를 처음부터 끝까지 마스터하는 시리즈",
      "authorId": "user-123",
      "authorName": "홍길동",
      "thumbnailUrl": "https://s3.amazonaws.com/bucket/series-thumb.jpg",
      "postCount": 5,
      "createdAt": "2026-01-18T11:15:00",
      "updatedAt": "2026-01-18T11:20:00"
    }
  ],
  "timestamp": "2026-01-18T11:25:00"
}
```

> **SeriesListResponse vs SeriesResponse**: 목록 조회는 `SeriesListResponse`를 반환합니다. `postIds` 필드가 없습니다.

---

### 6. 내 시리즈 목록 조회

```http
GET /api/v1/blog/series/my
Authorization: Bearer {token}
```

#### Response (200 OK) - `List<SeriesListResponse>`

---

### 7. 시리즈 포스트 목록 조회

시리즈에 포함된 포스트 목록을 순서대로 조회합니다.

```http
GET /api/v1/blog/series/{seriesId}/posts
```

#### Response (200 OK) - `List<PostSummaryResponse>`

---

### 8. 시리즈에 포스트 추가

```http
POST /api/v1/blog/series/{seriesId}/posts/{postId}
Authorization: Bearer {token}
```

#### Response (200 OK) - `SeriesResponse`

---

### 9. 시리즈에서 포스트 제거

```http
DELETE /api/v1/blog/series/{seriesId}/posts/{postId}
Authorization: Bearer {token}
```

#### Response (200 OK) - `SeriesResponse`

---

### 10. 시리즈 내 포스트 순서 변경

```http
PUT /api/v1/blog/series/{seriesId}/posts/order
Content-Type: application/json
Authorization: Bearer {token}
```

#### Request Body (`SeriesPostOrderRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `postIds` | string[] | ✅ | 새로운 순서의 게시물 ID 목록 |

#### Response (200 OK) - `SeriesResponse`

---

### 11. 포스트가 포함된 시리즈 조회

```http
GET /api/v1/blog/series/by-post/{postId}
```

#### Response (200 OK) - `List<SeriesListResponse>`

---

### 12. 전체 시리즈 목록 조회

모든 사용자의 시리즈를 최근 업데이트순으로 조회합니다.

```http
GET /api/v1/blog/series
```

#### Response (200 OK) - `List<SeriesListResponse>`

```json
{
  "success": true,
  "data": [
    {
      "id": "677dd567e8f9g0h1i2j3k4l5",
      "name": "Spring Boot 마스터 시리즈",
      "description": "스프링 부트를 처음부터 끝까지 마스터하는 시리즈",
      "authorId": "user-123",
      "authorName": "홍길동",
      "thumbnailUrl": "https://s3.amazonaws.com/bucket/series-thumb.jpg",
      "postCount": 5,
      "createdAt": "2026-01-18T11:15:00",
      "updatedAt": "2026-02-17T10:00:00"
    }
  ],
  "timestamp": "2026-02-17T11:00:00"
}
```

---

## Tag API

### 1. 태그 생성

```http
POST /api/v1/blog/tags
Content-Type: application/json
```

#### Request Body (`TagCreateRequest`)

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 태그 이름 | 최대 50자 |
| `description` | string | ❌ | 태그 설명 | 최대 200자 |

#### Response (200 OK) - `TagResponse`

```json
{
  "success": true,
  "data": {
    "id": "677ee678f9g0h1i2j3k4l5m6",
    "name": "spring-security",
    "postCount": 0,
    "description": "스프링 시큐리티 관련 태그",
    "createdAt": "2026-01-18T11:40:00",
    "lastUsedAt": null
  },
  "timestamp": "2026-01-18T11:40:00"
}
```

---

### 2. 전체 태그 목록 조회

```http
GET /api/v1/blog/tags
```

#### Response (200 OK) - `List<TagResponse>`

---

### 3. 태그 상세 조회

```http
GET /api/v1/blog/tags/{tagName}
```

#### Response (200 OK) - `TagResponse`

---

### 4. 인기 태그 조회

```http
GET /api/v1/blog/tags/popular?limit=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 10 |

#### Response (200 OK) - `List<TagStatsResponse>`

```json
{
  "success": true,
  "data": [
    {
      "name": "spring",
      "postCount": 42,
      "totalViews": 15200
    }
  ],
  "timestamp": "2026-01-18T11:40:00"
}
```

---

### 5. 최근 사용된 태그 조회

```http
GET /api/v1/blog/tags/recent?limit=10
```

#### Response (200 OK) - `List<TagResponse>`

---

### 6. 태그 검색 (자동완성)

```http
GET /api/v1/blog/tags/search?q=spr&limit=5
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `q` | string | ✅ | 검색 키워드 | - |
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK) - `List<TagResponse>`

---

### 7. 태그 설명 업데이트

```http
PATCH /api/v1/blog/tags/{tagName}/description?description=업데이트된 설명
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `description` | string | ✅ | 새로운 설명 |

#### Response (200 OK) - `TagResponse`

---

### 8. 사용되지 않는 태그 일괄 삭제

`ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 권한 필요.

```http
DELETE /api/v1/blog/tags/unused
Authorization: Bearer {admin-token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:45:00"
}
```

---

### 9. 태그 강제 삭제

`ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 권한 필요.

```http
DELETE /api/v1/blog/tags/{tagName}
Authorization: Bearer {admin-token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:45:00"
}
```

---

## File API

> **주의**: File API는 `ApiResponse` wrapper를 사용하지 않고 직접 `ResponseEntity`를 반환합니다.

### 1. 파일 업로드

S3에 파일을 업로드하고 접근 URL을 반환합니다. 인증된 사용자만 사용 가능합니다.

```http
POST /api/v1/blog/file/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

#### Request

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `file` | file | ✅ | 업로드할 파일 |

#### Response (200 OK) - `FileUploadResponse` (ApiResponse 미사용)

```json
{
  "url": "https://s3.amazonaws.com/bucket/uploads/677ff789g0h1i2j3k4l5m6n7.jpg",
  "filename": "spring-boot-guide.jpg",
  "size": 245678,
  "contentType": "image/jpeg"
}
```

#### cURL Example

```bash
curl -X POST "http://localhost:8082/file/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

---

### 2. 파일 삭제

S3에서 파일을 삭제합니다. `ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 권한 필요.

```http
DELETE /api/v1/blog/file/delete
Content-Type: application/json
Authorization: Bearer {admin-token}
```

#### Request Body (`FileDeleteRequest`)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `url` | string | ✅ | 삭제할 파일의 S3 URL |

#### Response (204 No Content)

```
(응답 본문 없음)
```

---

## 에러 코드

### Blog Service 에러 코드 (`BlogErrorCode`)

#### Post Errors (B001-B004)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B001` | 404 | `POST_NOT_FOUND` | 게시물을 찾을 수 없음 |
| `B002` | 403 | `POST_UPDATE_FORBIDDEN` | 게시물 수정 권한 없음 (본인만 수정 가능) |
| `B003` | 403 | `POST_DELETE_FORBIDDEN` | 게시물 삭제 권한 없음 (본인만 삭제 가능) |
| `B004` | 400 | `POST_NOT_PUBLISHED` | 게시물이 아직 발행되지 않음 |

#### Like Errors (B020-B022)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B020` | 404 | `LIKE_NOT_FOUND` | 좋아요 기록 없음 |
| `B021` | 409 | `LIKE_ALREADY_EXISTS` | 이미 좋아요한 게시물 |
| `B022` | 500 | `LIKE_OPERATION_FAILED` | 좋아요 작업 실패 |

#### Comment Errors (B030-B032)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B030` | 404 | `COMMENT_NOT_FOUND` | 댓글을 찾을 수 없음 |
| `B031` | 403 | `COMMENT_UPDATE_FORBIDDEN` | 댓글 수정 권한 없음 |
| `B032` | 403 | `COMMENT_DELETE_FORBIDDEN` | 댓글 삭제 권한 없음 |

#### Series Errors (B040-B046)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B040` | 404 | `SERIES_NOT_FOUND` | 시리즈를 찾을 수 없음 |
| `B041` | 403 | `SERIES_UPDATE_FORBIDDEN` | 시리즈 수정 권한 없음 |
| `B042` | 403 | `SERIES_DELETE_FORBIDDEN` | 시리즈 삭제 권한 없음 |
| `B043` | 403 | `SERIES_ADD_POST_FORBIDDEN` | 시리즈 포스트 추가 권한 없음 |
| `B044` | 403 | `SERIES_REMOVE_POST_FORBIDDEN` | 시리즈 포스트 제거 권한 없음 |
| `B045` | 403 | `SERIES_REORDER_FORBIDDEN` | 시리즈 순서 변경 권한 없음 |
| `B046` | 409 | `SERIES_CONCURRENT_MODIFICATION` | 시리즈 동시 수정 충돌 (재시도 필요) |

#### Tag Errors (B050-B051)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B050` | 404 | `TAG_NOT_FOUND` | 태그를 찾을 수 없음 |
| `B051` | 409 | `TAG_ALREADY_EXISTS` | 태그가 이미 존재 |

#### File Errors (B060-B065)

| Code | HTTP Status | Enum | 설명 |
|------|-------------|------|------|
| `B060` | 500 | `FILE_UPLOAD_FAILED` | 파일 업로드 실패 |
| `B061` | 400 | `FILE_EMPTY` | 파일이 비어있음 |
| `B062` | 400 | `FILE_SIZE_EXCEEDED` | 파일 크기 초과 |
| `B063` | 400 | `FILE_TYPE_NOT_ALLOWED` | 허용되지 않는 파일 형식 |
| `B064` | 500 | `FILE_DELETE_FAILED` | 파일 삭제 실패 |
| `B065` | 400 | `INVALID_FILE_URL` | 잘못된 파일 URL 형식 |

### 에러 응답 형식

```json
{
  "success": false,
  "code": "B001",
  "message": "Post not found",
  "data": null,
  "timestamp": "2026-01-18T11:45:00"
}
```

---

## 공통 응답 형식

### 성공 응답 (ApiResponse)

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-01-18T11:45:00"
}
```

### 페이징 응답

```json
{
  "success": true,
  "data": {
    "items": [ ... ],
    ,
    "totalElements": 100,
    "totalPages": 10,
    "last": false,
    "first": true,
    "numberOfElements": 10
  },
  "timestamp": "2026-01-18T11:45:00"
}
```

---

## 인증 방법

### JWT Bearer Token

인증이 필요한 API는 Authorization 헤더에 JWT 토큰을 포함해야 합니다.

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 토큰 획득

Auth Service의 OAuth2 엔드포인트를 통해 토큰을 획득합니다.

```http
POST /api/v1/auth/oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=user@example.com&password=password123
```

### 권한 구분

| 인증 표기 | 의미 |
|-----------|------|
| ❌ | 인증 불필요 |
| ✅ | 로그인 필요 (JWT) |
| ✅ ADMIN | `ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 필요 |
| 선택 | 선택적 - 인증 시 사용자별 중복 방지 등 |

---

## 관련 문서

- [Blog Service Architecture](../../architecture/blog-service/system-overview.md)

---

**최종 업데이트**: 2026-02-17
