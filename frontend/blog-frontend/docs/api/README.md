# API 문서 인덱스

blog-frontend의 API 관련 문서 목록입니다.

---

## 📋 문서 목록

| ID | 제목 | 상태 | 설명 |
|----|------|------|------|
| `api-client` | [Blog Frontend API Client](./client-api.md) | ✅ Current | axios 기반 API 클라이언트 명세서 |

---

## 🔍 빠른 참조

### API 모듈 구조
- **Posts API** (`api/posts.ts`): 게시물 CRUD, 목록, 검색, 통계
- **Comments API** (`api/comments.ts`): 댓글 CRUD
- **Files API** (`api/files.ts`): 파일 업로드/삭제

### Base URL
```
http://localhost:8080/api/blog
```

### 인증
모든 요청은 portal-shell의 apiClient를 통해 JWT 토큰이 자동으로 첨부됩니다.

---

## 🔗 관련 문서
- [아키텍처: 데이터 흐름](../architecture/arch-data-flow.md)
- [Backend: Blog Service API](../../../../services/blog-service/docs/api/README.md)

---

**최종 업데이트**: 2026-01-18
