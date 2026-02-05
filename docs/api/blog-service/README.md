# Blog Service API Documentation

Blog Service의 API 명세 문서입니다.

## API 문서 목록

| 문서 ID                     | 제목 | 버전 | 상태 | 최종 업데이트 |
|---------------------------|------|------|------|---------------|
| [blog-api](./blog-api.md) | Blog Service API | v1 | current | 2026-02-06 |

## API 개요

### Base URL

- **Gateway 경유**: `/api/v1/blog`
- **직접 접근**: `http://localhost:8082`

### Gateway 라우팅

```
/api/v1/blog/{path} → StripPrefix=3 → blog-service:8082/{path}
```

### 인증

대부분의 쓰기 API는 JWT Bearer Token 인증이 필요합니다.
GET 요청은 기본적으로 인증 없이 접근 가능합니다 (permit-all).

```http
Authorization: Bearer {token}
```

### 권한 구분

| 표기 | 의미 |
|------|------|
| ❌ | 인증 불필요 |
| ✅ | 로그인 필요 |
| ✅ ADMIN | `ROLE_BLOG_ADMIN` 또는 `ROLE_SUPER_ADMIN` 필요 |

## 주요 API 카테고리

### Post API (게시물 관리) - 25개 endpoint

- 게시물 CRUD
- 검색 (단순/고급, 정렬/필터)
- 통계 조회 (카테고리, 태그, 작성자, 블로그)
- 상태 관리 (DRAFT/PUBLISHED/ARCHIVED)
- 트렌딩, 피드, 네비게이션

**문서**: [Blog API - Post API](./blog-api.md#post-api)

### Like API (좋아요 관리) - 3개 endpoint

- 좋아요 토글
- 상태 확인 (좋아요 여부 + 좋아요 수)
- 좋아요한 사용자 목록 (페이징)

**문서**: [Blog API - Like API](./blog-api.md#like-api)

### Comment API (댓글 관리) - 4개 endpoint

- 댓글/대댓글 CRUD
- 포스트별 댓글 조회

**문서**: [Blog API - Comment API](./blog-api.md#comment-api)

### Series API (시리즈 관리) - 11개 endpoint

- 시리즈 CRUD
- 포스트 추가/제거/순서 변경
- 시리즈 포스트 목록 조회

**문서**: [Blog API - Series API](./blog-api.md#series-api)

### Tag API (태그 관리) - 9개 endpoint

- 태그 CRUD
- 인기 태그/최근 태그
- 태그 검색 (자동완성)
- 미사용 태그 정리 (관리자)

**문서**: [Blog API - Tag API](./blog-api.md#tag-api)

### File API (파일 관리) - 2개 endpoint

- S3 파일 업로드
- S3 파일 삭제 (관리자)

> File API는 `ApiResponse` wrapper를 사용하지 않고 직접 `ResponseEntity`를 반환합니다.

**문서**: [Blog API - File API](./blog-api.md#file-api)

## 주요 DTO 구분

| DTO | 용도 | 주요 특징 |
|-----|------|-----------|
| `PostResponse` | 상세 조회 | `content`, `status`, `productId` 포함 |
| `PostSummaryResponse` | 목록 조회 | `commentCount`, `estimatedReadTime` 포함 |
| `SeriesResponse` | 시리즈 상세 | `postIds` 배열 포함 |
| `SeriesListResponse` | 시리즈 목록 | `postIds` 없음 |

## 관련 문서

- [Blog Service Architecture](../../architecture/blog-service/system-overview.md)

---

**최종 업데이트**: 2026-02-06
