# API Documentation

Blog Service의 API 명세 문서입니다.

## 📑 API 문서 목록

| 문서 ID                     | 제목 | 버전 | 상태 | 최종 업데이트 |
|---------------------------|------|------|------|---------------|
| [blog-api](./blog-api.md) | Blog Service API | v1 | current | 2026-01-18 |

## 📋 API 개요

### Base URL

- **Gateway 경유**: `/api/v1/blog`
- **직접 접근**: `http://localhost:8082`

### 인증

대부분의 API는 JWT Bearer Token 인증이 필요합니다.

```http
Authorization: Bearer {token}
```

## 🔹 주요 API 카테고리

### Post API (게시물 관리)

- 게시물 CRUD
- 검색 (단순/고급)
- 통계 조회
- 상태 관리

**문서**: [Blog API - Post API](./blog-api.md#-post-api)

### Comment API (댓글 관리)

- 댓글/대댓글 CRUD
- 포스트별 댓글 조회

**문서**: [Blog API - Comment API](./blog-api.md#-comment-api)

### Series API (시리즈 관리)

- 시리즈 CRUD
- 포스트 추가/제거/순서 변경

**문서**: [Blog API - Series API](./blog-api.md#-series-api)

### Tag API (태그 관리)

- 태그 조회/검색
- 인기 태그/최근 태그
- 태그 통계

**문서**: [Blog API - Tag API](./blog-api.md#-tag-api)

### File API (파일 관리)

- S3 파일 업로드
- S3 파일 삭제

**문서**: [Blog API - File API](./blog-api.md#-file-api)

### Like API (좋아요)

- 좋아요 토글
- 상태 확인
- 좋아요한 사용자 목록

**문서**: [Blog API - Like API](./blog-api.md#-like-api)

## 🔗 관련 문서

- [Blog Service Architecture](../../architecture/blog-service/system-overview.md)

---

**최종 업데이트**: 2026-01-26
