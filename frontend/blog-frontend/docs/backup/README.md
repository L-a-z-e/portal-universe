# Blog Frontend

Vue 3 기반 블로그 마이크로 프론트엔드입니다.

## 개요

Module Federation Remote 모듈로, Portal Shell에 통합되거나 독립 실행됩니다.

## 포트

- 개발 서버: `30001`

## 주요 기능

| 기능 | 설명 |
|------|------|
| 게시물 목록 | 페이징, 검색, 필터링 |
| 게시물 상세 | 조회수, 관련 게시물 |
| 게시물 작성/수정 | 마크다운 에디터 |
| 댓글 | 댓글/대댓글 |

## 기술 스택

- **Framework**: Vue 3 (Composition API)
- **Build**: Vite + Module Federation
- **State**: Pinia
- **Router**: Vue Router (Memory History for Embedded)
- **Editor**: Markdown 에디터

## 프로젝트 구조

```
blog-frontend/
├── src/
│   ├── views/
│   │   ├── PostListPage.vue
│   │   ├── PostDetailPage.vue
│   │   ├── PostWritePage.vue
│   │   └── PostEditPage.vue
│   ├── components/
│   │   ├── PostCard.vue
│   │   └── HelloWorld.vue
│   ├── api/
│   │   ├── index.ts
│   │   ├── posts.ts
│   │   ├── comments.ts
│   │   └── files.ts
│   ├── dto/
│   │   ├── post.ts
│   │   ├── comment.ts
│   │   ├── series.ts
│   │   └── tag.ts
│   ├── stores/
│   │   └── searchStore.ts
│   ├── router/
│   │   └── index.ts
│   ├── bootstrap.ts      # Embedded 모드 진입점
│   ├── main.ts           # Standalone 모드 진입점
│   └── App.vue
└── vite.config.ts
```

## 실행 모드

### Standalone (독립 실행)

```bash
npm run dev:blog
```

`main.ts`를 통해 실행, Browser History 사용

### Embedded (Portal 통합)

Portal Shell에서 `mountBlogApp()` 호출

`bootstrap.ts`를 통해 실행, Memory History 사용

## 라우팅

| 경로 | 컴포넌트 | 설명 |
|------|----------|------|
| `/` | PostListPage | 게시물 목록 |
| `/:id` | PostDetailPage | 게시물 상세 |
| `/write` | PostWritePage | 게시물 작성 |
| `/:id/edit` | PostEditPage | 게시물 수정 |

## Module Federation 설정

```typescript
// vite.config.ts
federation({
  name: 'blogFrontend',
  filename: 'remoteEntry.js',
  exposes: {
    './bootstrap': './src/bootstrap.ts'
  },
  shared: ['vue', 'vue-router', 'pinia']
})
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `VITE_API_BASE_URL` | API Gateway URL | http://localhost:8080 |

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처 상세
