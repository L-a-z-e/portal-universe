# API 문서 인덱스

blog-frontend의 API 관련 문서 목록입니다.

---

## 📋 문서 목록

| ID | 제목 | 상태 | 설명 |
|----|------|------|------|
| `client-api` | [Blog Frontend API Client](./client-api.md) | ✅ Current | axios 기반 API 클라이언트 명세서 |

---

## 🔍 빠른 참조

### API 모듈 구조
- **Posts API** (`api/posts.ts`): 게시물 CRUD, 목록, 검색, 통계, 네비게이션, 피드
- **Comments API** (`api/comments.ts`): 댓글 CRUD
- **Files API** (`api/files.ts`): 파일 업로드/삭제
- **Likes API** (`api/likes.ts`): 좋아요 토글, 상태 확인, 좋아요한 사용자 목록
- **Series API** (`api/series.ts`): 시리즈 CRUD, 포스트 관리
- **Tags API** (`api/tags.ts`): 태그 조회, 검색, 인기 태그
- **Follow API** (`api/follow.ts`): 팔로우/언팔로우, 팔로워/팔로잉 목록
- **Users API** (`api/users.ts`): 프로필 조회/수정, Username 관리

### Base URL
```
http://localhost:8080/api/v1/blog
```

### 인증
모든 요청은 portal-shell의 apiClient를 통해 JWT 토큰이 자동으로 첨부됩니다.

---

## 🔗 관련 문서
- [아키텍처: 데이터 흐름](../../architecture/blog-frontend/)
- [Backend: Blog Service API](../blog-service/blog-api.md)

---

**최종 업데이트**: 2026-02-06
