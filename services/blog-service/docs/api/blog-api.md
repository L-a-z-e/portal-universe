---
id: api-blog
title: Blog Service API
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-01-26
author: Documenter Agent
tags: [api, blog, mongodb, post, comment, series, tag, file]
related:
  - PRD-001
---

# Blog Service API

> MongoDB 기반 블로그 콘텐츠 관리 서비스 API 명세서

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `/api/v1/blog` (Gateway 경유) |
| **직접 URL** | `http://localhost:8082` |
| **인증** | Bearer Token (JWT) 필요 |
| **버전** | v1 |
| **데이터베이스** | MongoDB |

---

## 📑 API 목록

### Post API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/posts` | 게시물 생성 | ✅ |
| GET | `/posts/all` | 전체 게시물 조회 (관리자용) | ❌ |
| GET | `/posts` | 발행 게시물 목록 (페이징) | ❌ |
| GET | `/posts/{postId}` | 게시물 상세 조회 | ❌ |
| GET | `/posts/{postId}/view` | 조회수 증가 + 상세 조회 | 선택 |
| PUT | `/posts/{postId}` | 게시물 수정 | ✅ |
| DELETE | `/posts/{postId}` | 게시물 삭제 | ✅ |
| PATCH | `/posts/{postId}/status` | 게시물 상태 변경 | ✅ |
| GET | `/posts/author/{authorId}` | 작성자별 게시물 목록 | ❌ |
| GET | `/posts/my` | 내 게시물 목록 | ✅ |
| GET | `/posts/category/{category}` | 카테고리별 게시물 조회 | ❌ |
| GET | `/posts/tags` | 태그별 게시물 조회 | ❌ |
| GET | `/posts/popular` | 인기 게시물 조회 | ❌ |
| GET | `/posts/recent` | 최근 게시물 조회 | ❌ |
| GET | `/posts/{postId}/related` | 연관 게시물 조회 | ❌ |
| GET | `/posts/search` | 게시물 단순 검색 | ❌ |
| POST | `/posts/search/advanced` | 게시물 고급 검색 | ❌ |
| GET | `/posts/stats/categories` | 카테고리 통계 조회 | ❌ |
| GET | `/posts/stats/tags` | 인기 태그 통계 조회 | ❌ |
| GET | `/posts/stats/author/{authorId}` | 작성자 통계 조회 | ❌ |
| GET | `/posts/stats/blog` | 전체 블로그 통계 조회 | ❌ |
| GET | `/posts/product/{productId}` | 상품별 게시물 조회 | ❌ |
| GET | `/posts/trending` | 트렌딩 게시물 조회 | ❌ |
| GET | `/posts/feed` | 피드 게시물 조회 (팔로잉 기반) | ✅ |
| GET | `/posts/{postId}/navigation` | 이전/다음 게시물 네비게이션 | ❌ |

### Like API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/posts/{postId}/like` | 좋아요 토글 | ✅ |
| GET | `/posts/{postId}/like` | 좋아요 여부 확인 | ✅ |
| GET | `/posts/{postId}/likes` | 좋아요한 사용자 목록 | ❌ |

### Comment API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/comments` | 댓글 생성 | ✅ |
| GET | `/comments/post/{postId}` | 포스트별 댓글 목록 조회 | ❌ |
| PUT | `/comments/{commentId}` | 댓글 수정 | ✅ |
| DELETE | `/comments/{commentId}` | 댓글 삭제 | ✅ |

### Series API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/series` | 시리즈 생성 | ✅ |
| GET | `/series/{seriesId}` | 시리즈 상세 조회 | ❌ |
| PUT | `/series/{seriesId}` | 시리즈 수정 | ✅ |
| DELETE | `/series/{seriesId}` | 시리즈 삭제 | ✅ |
| GET | `/series/author/{authorId}` | 작성자별 시리즈 목록 조회 | ❌ |
| GET | `/series/my` | 내 시리즈 목록 조회 | ✅ |
| POST | `/series/{seriesId}/posts/{postId}` | 시리즈에 포스트 추가 | ✅ |
| DELETE | `/series/{seriesId}/posts/{postId}` | 시리즈에서 포스트 제거 | ✅ |
| PUT | `/series/{seriesId}/posts/order` | 시리즈 내 포스트 순서 변경 | ✅ |
| GET | `/series/by-post/{postId}` | 포스트가 포함된 시리즈 조회 | ❌ |

### Tag API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/tags` | 태그 생성 | ❌ |
| GET | `/tags` | 전체 태그 목록 조회 | ❌ |
| GET | `/tags/{tagName}` | 태그 상세 조회 | ❌ |
| GET | `/tags/popular` | 인기 태그 조회 | ❌ |
| GET | `/tags/recent` | 최근 사용된 태그 조회 | ❌ |
| GET | `/tags/search` | 태그 검색 (자동완성) | ❌ |
| PATCH | `/tags/{tagName}/description` | 태그 설명 업데이트 | ❌ |
| DELETE | `/tags/unused` | 사용되지 않는 태그 일괄 삭제 | ❌ |
| DELETE | `/tags/{tagName}` | 태그 강제 삭제 | ❌ |

### File API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/file/upload` | 파일 업로드 (S3) | ✅ |
| DELETE | `/file/delete` | 파일 삭제 (S3) | ✅ (ADMIN) |

---

## 🔹 Post API

### 1. 게시물 생성

새 블로그 게시물을 작성합니다.

#### Request

```http
POST /api/v1/blog/posts
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Spring Boot 완벽 가이드",
  "content": "# Spring Boot란?\n\n스프링 부트는...",
  "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
  "tags": ["spring", "java", "backend"],
  "category": "Backend",
  "metaDescription": "스프링 부트 입문 가이드 - 기본부터 고급까지",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
  "publishImmediately": true,
  "images": [
    "https://s3.amazonaws.com/bucket/image1.jpg",
    "https://s3.amazonaws.com/bucket/image2.jpg"
  ],
  "productId": "prod-123"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `title` | string | ✅ | 게시물 제목 | 1~200자 |
| `content` | string | ✅ | 게시물 본문 (Markdown) | - |
| `summary` | string | ❌ | 게시물 요약 | 최대 500자 |
| `tags` | string[] | ❌ | 태그 목록 | - |
| `category` | string | ❌ | 카테고리 | - |
| `metaDescription` | string | ❌ | SEO 메타 설명 | 최대 160자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |
| `publishImmediately` | boolean | ❌ | 즉시 발행 여부 | 기본값: false |
| `images` | string[] | ❌ | 본문 이미지 URL 목록 | - |
| `productId` | string | ❌ | 연결된 상품 ID | - |

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
    "images": [
      "https://s3.amazonaws.com/bucket/image1.jpg",
      "https://s3.amazonaws.com/bucket/image2.jpg"
    ],
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": "2026-01-18T10:30:00Z",
    "updatedAt": "2026-01-18T10:30:00Z",
    "publishedAt": "2026-01-18T10:30:00Z",
    "productId": "prod-123"
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 2. 발행 게시물 목록 조회

발행된 게시물 목록을 페이징하여 조회합니다.

#### Request

```http
GET /api/v1/blog/posts?page=0&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 (0부터 시작) | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 완벽 가이드",
        "summary": "스프링 부트의 기본 개념과 사용법을 소개합니다",
        "authorId": "user-123",
        "authorName": "홍길동",
        "status": "PUBLISHED",
        "tags": ["spring", "java"],
        "category": "Backend",
        "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
        "viewCount": 150,
        "likeCount": 12,
        "createdAt": "2026-01-18T10:30:00Z",
        "publishedAt": "2026-01-18T10:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "offset": 0
    },
    "totalElements": 42,
    "totalPages": 5,
    "last": false,
    "first": true,
    "numberOfElements": 10
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 3. 게시물 상세 조회

게시물 ID로 상세 정보를 조회합니다. 조회수는 증가하지 않습니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

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
    "metaDescription": "스프링 부트 입문 가이드",
    "thumbnailUrl": "https://s3.amazonaws.com/bucket/thumb.jpg",
    "images": [
      "https://s3.amazonaws.com/bucket/image1.jpg"
    ],
    "viewCount": 150,
    "likeCount": 12,
    "createdAt": "2026-01-18T10:30:00Z",
    "updatedAt": "2026-01-18T10:30:00Z",
    "publishedAt": "2026-01-18T10:30:00Z",
    "productId": null
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 4. 조회수 증가 + 상세 조회

게시물을 조회하면서 조회수를 1 증가시킵니다. 동일 사용자의 중복 조회는 제한됩니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}/view
Authorization: Bearer {token}  (선택 사항)
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ab123c4d5e6f7g8h9i0j1",
    "title": "Spring Boot 완벽 가이드",
    "content": "# Spring Boot란?\n\n스프링 부트는...",
    "viewCount": 151,
    "likeCount": 12
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

---

### 5. 게시물 수정

게시물을 수정합니다. 본인만 수정 가능합니다.

#### Request

```http
PUT /api/v1/blog/posts/{postId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "title": "Spring Boot 완벽 가이드 (수정)",
  "content": "# Spring Boot란?\n\n업데이트된 내용...",
  "summary": "업데이트된 요약",
  "tags": ["spring", "java", "backend", "guide"],
  "category": "Backend",
  "metaDescription": "최신 스프링 부트 가이드",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/new-thumb.jpg"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `title` | string | ✅ | 게시물 제목 | 1~200자 |
| `content` | string | ✅ | 게시물 본문 | - |
| `summary` | string | ❌ | 게시물 요약 | 최대 500자 |
| `tags` | string[] | ❌ | 태그 목록 | - |
| `category` | string | ❌ | 카테고리 | - |
| `metaDescription` | string | ❌ | SEO 메타 설명 | 최대 160자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ab123c4d5e6f7g8h9i0j1",
    "title": "Spring Boot 완벽 가이드 (수정)",
    "content": "# Spring Boot란?\n\n업데이트된 내용...",
    "updatedAt": "2026-01-18T11:00:00Z"
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 6. 게시물 삭제

게시물을 삭제합니다. 본인만 삭제 가능합니다.

#### Request

```http
DELETE /api/v1/blog/posts/{postId}
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 7. 게시물 상태 변경

게시물의 상태를 변경합니다. (DRAFT ↔ PUBLISHED ↔ ARCHIVED)

#### Request

```http
PATCH /api/v1/blog/posts/{postId}/status
Content-Type: application/json
Authorization: Bearer {token}

{
  "newStatus": "PUBLISHED"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 가능한 값 |
|------|------|------|------|-----------|
| `newStatus` | string | ✅ | 새로운 상태 | DRAFT, PUBLISHED, ARCHIVED |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ab123c4d5e6f7g8h9i0j1",
    "status": "PUBLISHED",
    "publishedAt": "2026-01-18T11:00:00Z"
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 8. 내 게시물 목록 조회

로그인한 사용자의 게시물 목록을 조회합니다. 상태 필터링 가능합니다.

#### Request

```http
GET /api/v1/blog/posts/my?status=DRAFT&page=0&size=10
Authorization: Bearer {token}
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 가능한 값 |
|----------|------|------|------|-----------|
| `status` | string | ❌ | 상태 필터 | DRAFT, PUBLISHED, ARCHIVED |
| `page` | int | ❌ | 페이지 번호 | 기본값: 0 |
| `size` | int | ❌ | 페이지 크기 | 기본값: 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "작성 중인 글",
        "status": "DRAFT",
        "createdAt": "2026-01-18T10:30:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 9. 카테고리별 게시물 조회

특정 카테고리의 게시물 목록을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/category/{category}?page=0&size=10
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `category` | string | ✅ | 카테고리 이름 |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "category": "Backend"
      }
    ],
    "totalElements": 20
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 10. 태그별 게시물 조회

특정 태그를 포함하는 게시물 목록을 조회합니다. 다중 태그 지원합니다.

#### Request

```http
GET /api/v1/blog/posts/tags?tags=spring,java&page=0&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `tags` | string[] | ✅ | 태그 목록 (쉼표로 구분) | - |
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "tags": ["spring", "java", "backend"]
      }
    ],
    "totalElements": 15
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 11. 인기 게시물 조회

조회수 기준 인기 게시물을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/popular?page=0&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "viewCount": 1250,
        "likeCount": 85
      }
    ]
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 12. 최근 게시물 조회

최근 작성된 게시물을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/recent?limit=5
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677ab123c4d5e6f7g8h9i0j1",
      "title": "최신 Spring Boot 가이드",
      "createdAt": "2026-01-18T10:30:00Z"
    }
  ],
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 13. 연관 게시물 조회

특정 게시물과 관련된 게시물을 태그/카테고리 기반으로 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}/related?limit=5
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677ab999c4d5e6f7g8h9i0j9",
      "title": "Spring Boot JPA 가이드",
      "tags": ["spring", "jpa"]
    }
  ],
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 14. 게시물 단순 검색

키워드로 게시물을 검색합니다. 제목, 본문, 요약에서 검색합니다.

#### Request

```http
GET /api/v1/blog/posts/search?keyword=spring&page=0&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `keyword` | string | ✅ | 검색 키워드 | - |
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "summary": "스프링 부트에 대한 내용..."
      }
    ],
    "totalElements": 8
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 15. 게시물 고급 검색

다양한 조건으로 게시물을 검색합니다.

#### Request

```http
POST /api/v1/blog/posts/search/advanced
Content-Type: application/json

{
  "keyword": "spring",
  "category": "Backend",
  "tags": ["java", "spring"],
  "authorId": "user-123",
  "startDate": "2026-01-01T00:00:00Z",
  "endDate": "2026-01-31T23:59:59Z",
  "status": "PUBLISHED",
  "page": 0,
  "size": 10
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `keyword` | string | ❌ | 검색 키워드 |
| `category` | string | ❌ | 카테고리 필터 |
| `tags` | string[] | ❌ | 태그 필터 |
| `authorId` | string | ❌ | 작성자 ID 필터 |
| `startDate` | string | ❌ | 시작 날짜 (ISO 8601) |
| `endDate` | string | ❌ | 종료 날짜 (ISO 8601) |
| `status` | string | ❌ | 상태 필터 |
| `page` | int | ❌ | 페이지 번호 |
| `size` | int | ❌ | 페이지 크기 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "category": "Backend",
        "tags": ["java", "spring"]
      }
    ],
    "totalElements": 3
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 16. 카테고리 통계 조회

각 카테고리별 게시물 수를 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/stats/categories
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "category": "Backend",
      "count": 42
    },
    {
      "category": "Frontend",
      "count": 35
    }
  ],
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 17. 인기 태그 통계 조회

많이 사용된 태그를 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/stats/tags?limit=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "tag": "spring",
      "count": 28
    },
    {
      "tag": "java",
      "count": 25
    }
  ],
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 18. 작성자 통계 조회

특정 작성자의 게시물 통계를 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/stats/author/{authorId}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `authorId` | string | ✅ | 작성자 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "authorId": "user-123",
    "totalPosts": 42,
    "publishedPosts": 38,
    "draftPosts": 4,
    "totalViews": 15234,
    "totalLikes": 856
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 19. 전체 블로그 통계 조회

전체 블로그의 통계를 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/stats/blog
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "totalPosts": 250,
    "publishedPosts": 220,
    "totalAuthors": 15,
    "totalViews": 125000,
    "totalLikes": 8500,
    "totalComments": 3200
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

## 🔹 Comment API

### 1. 댓글 생성

게시물에 댓글 또는 대댓글을 작성합니다.

#### Request

```http
POST /api/v1/blog/comments
Content-Type: application/json
Authorization: Bearer {token}

{
  "postId": "677ab123c4d5e6f7g8h9i0j1",
  "parentCommentId": null,
  "content": "좋은 글 감사합니다!"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |
| `parentCommentId` | string | ❌ | 부모 댓글 ID (대댓글인 경우) |
| `content` | string | ✅ | 댓글 내용 |

#### Response (200 OK)

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
    "createdAt": "2026-01-18T11:00:00Z",
    "updatedAt": "2026-01-18T11:00:00Z"
  },
  "timestamp": "2026-01-18T11:00:00Z"
}
```

---

### 2. 대댓글 생성

기존 댓글에 대댓글을 작성합니다.

#### Request

```http
POST /api/v1/blog/comments
Content-Type: application/json
Authorization: Bearer {token}

{
  "postId": "677ab123c4d5e6f7g8h9i0j1",
  "parentCommentId": "677cc456d7e8f9g0h1i2j3k4",
  "content": "저도 도움이 되었어요!"
}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677cc999d7e8f9g0h1i2j3k9",
    "postId": "677ab123c4d5e6f7g8h9i0j1",
    "authorId": "user-789",
    "authorName": "이영희",
    "content": "저도 도움이 되었어요!",
    "parentCommentId": "677cc456d7e8f9g0h1i2j3k4",
    "likeCount": 0,
    "isDeleted": false,
    "createdAt": "2026-01-18T11:05:00Z",
    "updatedAt": "2026-01-18T11:05:00Z"
  },
  "timestamp": "2026-01-18T11:05:00Z"
}
```

---

### 3. 포스트별 댓글 목록 조회

특정 포스트의 모든 댓글(대댓글 포함)을 조회합니다.

#### Request

```http
GET /api/v1/blog/comments/post/{postId}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

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
      "createdAt": "2026-01-18T11:00:00Z",
      "updatedAt": "2026-01-18T11:00:00Z"
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
      "createdAt": "2026-01-18T11:05:00Z",
      "updatedAt": "2026-01-18T11:05:00Z"
    }
  ],
  "timestamp": "2026-01-18T11:05:00Z"
}
```

---

### 4. 댓글 수정

작성한 댓글을 수정합니다. 본인만 수정 가능합니다.

#### Request

```http
PUT /api/v1/blog/comments/{commentId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "content": "수정된 댓글 내용입니다."
}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `commentId` | string | ✅ | 댓글 ID |

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `content` | string | ✅ | 수정할 댓글 내용 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677cc456d7e8f9g0h1i2j3k4",
    "content": "수정된 댓글 내용입니다.",
    "updatedAt": "2026-01-18T11:10:00Z"
  },
  "timestamp": "2026-01-18T11:10:00Z"
}
```

---

### 5. 댓글 삭제

댓글을 삭제합니다. 본인만 삭제 가능합니다. 소프트 삭제 방식입니다.

#### Request

```http
DELETE /api/v1/blog/comments/{commentId}
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `commentId` | string | ✅ | 댓글 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:10:00Z"
}
```

---

## 🔹 Like API

### 1. 좋아요 토글

게시물에 좋아요를 추가하거나 취소합니다. 동일 사용자가 다시 호출하면 좋아요가 취소됩니다.

#### Request

```http
POST /api/v1/blog/posts/{postId}/like
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "liked": true,
    "likeCount": 13
  },
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

### 2. 좋아요 여부 확인

현재 사용자가 해당 게시물에 좋아요를 눌렀는지 확인합니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}/like
Authorization: Bearer {token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "liked": true
  },
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

### 3. 좋아요한 사용자 목록

게시물에 좋아요를 누른 사용자 목록을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}/likes
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "userId": "user-123",
      "userName": "홍길동",
      "likedAt": "2026-01-26T10:00:00Z"
    }
  ],
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

## 🔹 Post API (추가)

### 20. 트렌딩 게시물 조회

트렌딩 알고리즘 기반 인기 게시물을 조회합니다.
점수 계산: `score = viewCount×1 + likeCount×3 + commentCount×5` (시간 감쇠 적용)

#### Request

```http
GET /api/v1/blog/posts/trending?period=week&page=0&size=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `period` | string | ❌ | 기간 필터 | week |
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "Spring Boot 가이드",
        "viewCount": 1250,
        "likeCount": 85,
        "commentCount": 12
      }
    ],
    "totalElements": 50
  },
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

### 21. 피드 게시물 조회

팔로잉 중인 사용자의 게시물을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/feed?page=0&size=10
Authorization: Bearer {token}
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `page` | int | ❌ | 페이지 번호 | 0 |
| `size` | int | ❌ | 페이지 크기 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "677ab123c4d5e6f7g8h9i0j1",
        "title": "팔로잉 사용자의 게시물",
        "authorId": "user-456",
        "authorName": "김철수",
        "publishedAt": "2026-01-26T09:00:00Z"
      }
    ],
    "totalElements": 30
  },
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

### 22. 이전/다음 게시물 네비게이션

현재 게시물 기준 이전/다음 게시물을 조회합니다.

#### Request

```http
GET /api/v1/blog/posts/{postId}/navigation?scope=all
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 현재 게시물 ID |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `scope` | string | ❌ | 범위 (all/author/category/series) | all |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "previous": {
      "id": "677ab000c4d5e6f7g8h9i0j0",
      "title": "이전 게시물"
    },
    "next": {
      "id": "677ab456c4d5e6f7g8h9i0j4",
      "title": "다음 게시물"
    }
  },
  "timestamp": "2026-01-26T10:00:00Z"
}
```

---

## 🔹 Series API

### 1. 시리즈 생성

새로운 시리즈를 생성합니다.

#### Request

```http
POST /api/v1/blog/series
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Spring Boot 마스터 시리즈",
  "description": "스프링 부트를 처음부터 끝까지 마스터하는 시리즈",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/series-thumb.jpg"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 시리즈 제목 | 1~100자 |
| `description` | string | ❌ | 시리즈 설명 | 최대 500자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |

#### Response (200 OK)

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
    "createdAt": "2026-01-18T11:15:00Z",
    "updatedAt": "2026-01-18T11:15:00Z"
  },
  "timestamp": "2026-01-18T11:15:00Z"
}
```

---

### 2. 시리즈 상세 조회

시리즈 ID로 상세 정보를 조회합니다.

#### Request

```http
GET /api/v1/blog/series/{seriesId}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |

#### Response (200 OK)

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
    "postIds": [
      "677ab123c4d5e6f7g8h9i0j1",
      "677ab456c4d5e6f7g8h9i0j4"
    ],
    "postCount": 2,
    "createdAt": "2026-01-18T11:15:00Z",
    "updatedAt": "2026-01-18T11:20:00Z"
  },
  "timestamp": "2026-01-18T11:20:00Z"
}
```

---

### 3. 시리즈 수정

시리즈 정보를 수정합니다. 본인만 수정 가능합니다.

#### Request

```http
PUT /api/v1/blog/series/{seriesId}
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "Spring Boot 완전 정복 시리즈",
  "description": "업데이트된 설명",
  "thumbnailUrl": "https://s3.amazonaws.com/bucket/new-series-thumb.jpg"
}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |

#### Request Body

| 필드 | 타입 | 필수 | 설명 | 제약조건 |
|------|------|------|------|----------|
| `name` | string | ✅ | 시리즈 제목 | 1~100자 |
| `description` | string | ❌ | 시리즈 설명 | 최대 500자 |
| `thumbnailUrl` | string | ❌ | 썸네일 이미지 URL | - |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677dd567e8f9g0h1i2j3k4l5",
    "name": "Spring Boot 완전 정복 시리즈",
    "description": "업데이트된 설명",
    "updatedAt": "2026-01-18T11:25:00Z"
  },
  "timestamp": "2026-01-18T11:25:00Z"
}
```

---

### 4. 시리즈 삭제

시리즈를 삭제합니다. 본인만 삭제 가능합니다.

#### Request

```http
DELETE /api/v1/blog/series/{seriesId}
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:25:00Z"
}
```

---

### 5. 내 시리즈 목록 조회

로그인한 사용자의 시리즈 목록을 조회합니다.

#### Request

```http
GET /api/v1/blog/series/my
Authorization: Bearer {token}
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677dd567e8f9g0h1i2j3k4l5",
      "name": "Spring Boot 마스터 시리즈",
      "postCount": 5,
      "createdAt": "2026-01-18T11:15:00Z"
    }
  ],
  "timestamp": "2026-01-18T11:25:00Z"
}
```

---

### 6. 시리즈에 포스트 추가

시리즈에 게시물을 추가합니다.

#### Request

```http
POST /api/v1/blog/series/{seriesId}/posts/{postId}
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677dd567e8f9g0h1i2j3k4l5",
    "name": "Spring Boot 마스터 시리즈",
    "postIds": [
      "677ab123c4d5e6f7g8h9i0j1",
      "677ab456c4d5e6f7g8h9i0j4",
      "677ab789c4d5e6f7g8h9i0j7"
    ],
    "postCount": 3
  },
  "timestamp": "2026-01-18T11:30:00Z"
}
```

---

### 7. 시리즈에서 포스트 제거

시리즈에서 게시물을 제거합니다.

#### Request

```http
DELETE /api/v1/blog/series/{seriesId}/posts/{postId}
Authorization: Bearer {token}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677dd567e8f9g0h1i2j3k4l5",
    "name": "Spring Boot 마스터 시리즈",
    "postIds": [
      "677ab123c4d5e6f7g8h9i0j1",
      "677ab456c4d5e6f7g8h9i0j4"
    ],
    "postCount": 2
  },
  "timestamp": "2026-01-18T11:30:00Z"
}
```

---

### 8. 시리즈 내 포스트 순서 변경

시리즈 내 게시물의 순서를 변경합니다.

#### Request

```http
PUT /api/v1/blog/series/{seriesId}/posts/order
Content-Type: application/json
Authorization: Bearer {token}

{
  "postIds": [
    "677ab456c4d5e6f7g8h9i0j4",
    "677ab123c4d5e6f7g8h9i0j1"
  ]
}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `seriesId` | string | ✅ | 시리즈 ID |

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `postIds` | string[] | ✅ | 새로운 순서의 게시물 ID 목록 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677dd567e8f9g0h1i2j3k4l5",
    "postIds": [
      "677ab456c4d5e6f7g8h9i0j4",
      "677ab123c4d5e6f7g8h9i0j1"
    ],
    "updatedAt": "2026-01-18T11:35:00Z"
  },
  "timestamp": "2026-01-18T11:35:00Z"
}
```

---

### 9. 포스트가 포함된 시리즈 조회

특정 게시물이 포함된 시리즈 목록을 조회합니다.

#### Request

```http
GET /api/v1/blog/series/by-post/{postId}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `postId` | string | ✅ | 게시물 ID |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677dd567e8f9g0h1i2j3k4l5",
      "name": "Spring Boot 마스터 시리즈",
      "postCount": 5
    }
  ],
  "timestamp": "2026-01-18T11:35:00Z"
}
```

---

## 🔹 Tag API

### 1. 태그 생성

새로운 태그를 생성합니다.

#### Request

```http
POST /api/v1/blog/tags
Content-Type: application/json

{
  "name": "spring-security",
  "description": "스프링 시큐리티 관련 태그"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | string | ✅ | 태그 이름 |
| `description` | string | ❌ | 태그 설명 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ee678f9g0h1i2j3k4l5m6",
    "name": "spring-security",
    "description": "스프링 시큐리티 관련 태그",
    "usageCount": 0,
    "createdAt": "2026-01-18T11:40:00Z",
    "lastUsedAt": null
  },
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 2. 전체 태그 목록 조회

모든 태그를 조회합니다.

#### Request

```http
GET /api/v1/blog/tags
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677ee678f9g0h1i2j3k4l5m6",
      "name": "spring",
      "description": "스프링 프레임워크",
      "usageCount": 42
    },
    {
      "id": "677ee999f9g0h1i2j3k4l5m9",
      "name": "java",
      "description": "자바 프로그래밍",
      "usageCount": 38
    }
  ],
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 3. 태그 상세 조회

태그 이름으로 상세 정보를 조회합니다.

#### Request

```http
GET /api/v1/blog/tags/{tagName}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `tagName` | string | ✅ | 태그 이름 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ee678f9g0h1i2j3k4l5m6",
    "name": "spring",
    "description": "스프링 프레임워크",
    "usageCount": 42,
    "createdAt": "2026-01-15T10:00:00Z",
    "lastUsedAt": "2026-01-18T11:00:00Z"
  },
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 4. 인기 태그 조회

많이 사용된 태그를 조회합니다.

#### Request

```http
GET /api/v1/blog/tags/popular?limit=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "name": "spring",
      "count": 42
    },
    {
      "name": "java",
      "count": 38
    }
  ],
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 5. 최근 사용된 태그 조회

최근에 사용된 태그를 조회합니다.

#### Request

```http
GET /api/v1/blog/tags/recent?limit=10
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `limit` | int | ❌ | 조회할 개수 | 10 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677ee678f9g0h1i2j3k4l5m6",
      "name": "spring",
      "usageCount": 42,
      "lastUsedAt": "2026-01-18T11:00:00Z"
    }
  ],
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 6. 태그 검색 (자동완성)

입력한 키워드로 태그를 검색합니다. 자동완성 용도입니다.

#### Request

```http
GET /api/v1/blog/tags/search?q=spr&limit=5
```

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|----------|------|------|------|--------|
| `q` | string | ✅ | 검색 키워드 | - |
| `limit` | int | ❌ | 조회할 개수 | 5 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": "677ee678f9g0h1i2j3k4l5m6",
      "name": "spring",
      "usageCount": 42
    },
    {
      "id": "677ee789f9g0h1i2j3k4l5m7",
      "name": "spring-boot",
      "usageCount": 28
    },
    {
      "id": "677ee890f9g0h1i2j3k4l5m8",
      "name": "spring-security",
      "usageCount": 15
    }
  ],
  "timestamp": "2026-01-18T11:40:00Z"
}
```

---

### 7. 태그 설명 업데이트

태그의 설명을 업데이트합니다.

#### Request

```http
PATCH /api/v1/blog/tags/{tagName}/description?description=업데이트된 설명
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `tagName` | string | ✅ | 태그 이름 |

#### Query Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `description` | string | ✅ | 새로운 설명 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": {
    "id": "677ee678f9g0h1i2j3k4l5m6",
    "name": "spring",
    "description": "업데이트된 설명",
    "updatedAt": "2026-01-18T11:45:00Z"
  },
  "timestamp": "2026-01-18T11:45:00Z"
}
```

---

### 8. 사용되지 않는 태그 일괄 삭제

게시물에 사용되지 않는 태그를 모두 삭제합니다. 관리자용 API입니다.

#### Request

```http
DELETE /api/v1/blog/tags/unused
```

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:45:00Z"
}
```

---

### 9. 태그 강제 삭제

특정 태그를 강제로 삭제합니다.

#### Request

```http
DELETE /api/v1/blog/tags/{tagName}
```

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `tagName` | string | ✅ | 태그 이름 |

#### Response (200 OK)

```json
{
  "success": true,
  "data": null,
  "timestamp": "2026-01-18T11:45:00Z"
}
```

---

## 🔹 File API

### 1. 파일 업로드

S3에 파일을 업로드하고 접근 URL을 반환합니다.

#### Request

```http
POST /api/v1/blog/file/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [binary data]
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `file` | file | ✅ | 업로드할 파일 |

#### Response (200 OK)

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

S3에서 파일을 삭제합니다. ADMIN 권한이 필요합니다.

#### Request

```http
DELETE /api/v1/blog/file/delete
Content-Type: application/json
Authorization: Bearer {token}

{
  "url": "https://s3.amazonaws.com/bucket/uploads/677ff789g0h1i2j3k4l5m6n7.jpg"
}
```

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `url` | string | ✅ | 삭제할 파일의 S3 URL |

#### Response (204 No Content)

```
(No content)
```

---

## ⚠️ 에러 코드

### Blog Service 에러 코드

| Code | HTTP Status | 설명 | 해결 방법 |
|------|-------------|------|-----------|
| `B001` | 404 | Post not found | 유효한 게시물 ID 확인 |
| `B002` | 403 | Post update forbidden | 본인 게시물만 수정 가능 |
| `B003` | 403 | Post delete forbidden | 본인 게시물만 삭제 가능 |
| `B004` | 400 | Post not published | 발행된 게시물만 접근 가능 |
| `B020` | 404 | Like not found | 좋아요 기록 없음 |
| `B021` | 409 | Like already exists | 이미 좋아요한 게시물 |
| `B022` | 500 | Like operation failed | 서버 내부 오류 |
| `B030` | 404 | Comment not found | 유효한 댓글 ID 확인 |
| `B031` | 403 | Comment update forbidden | 본인 댓글만 수정 가능 |
| `B032` | 403 | Comment delete forbidden | 본인 댓글만 삭제 가능 |
| `B040` | 404 | Series not found | 유효한 시리즈 ID 확인 |
| `B041`-`B045` | 403 | Series permission errors | 본인 시리즈만 관리 가능 |
| `B050` | 404 | Tag not found | 유효한 태그명 확인 |
| `B051` | 409 | Tag already exists | 중복 태그 |
| `B060`-`B065` | 4xx/5xx | File errors | 파일 크기/타입/URL 확인 |

### 공통 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `C001` | 400 | Invalid request parameters |
| `C002` | 401 | Unauthorized |
| `C003` | 500 | Internal server error |

### 에러 응답 형식

```json
{
  "success": false,
  "code": "B001",
  "message": "Post not found",
  "data": null,
  "timestamp": "2026-01-18T11:45:00Z"
}
```

---

## 📌 인증 방법

### JWT Bearer Token

모든 인증이 필요한 API는 Authorization 헤더에 JWT 토큰을 포함해야 합니다.

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

---

## 🔄 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-01-18T11:45:00Z"
}
```

### 페이징 응답

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "offset": 0
    },
    "totalElements": 100,
    "totalPages": 10,
    "last": false,
    "first": true
  },
  "timestamp": "2026-01-18T11:45:00Z"
}
```

---

## 📚 관련 문서

- [Blog Service Architecture](../architecture/system-overview.md)

---

**최종 업데이트**: 2026-01-18
