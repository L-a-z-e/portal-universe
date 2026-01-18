# Blog Frontend Module

## 개요

**Blog Frontend**는 Portal Universe 플랫폼의 블로그 기능을 담당하는 Vue 3 기반 마이크로 프론트엔드(Remote) 모듈입니다.

- **포트**: 30001
- **기술 스택**: Vue 3, TypeScript, Vite 7.x, Pinia
- **아키텍처**: Module Federation (Micro Frontend)
- **통합 방식**: Portal Shell에서 Remote로 로드되는 독립 애플리케이션

## 핵심 기능

### 1. 게시글 관리
- **목록 조회**: 발행된 게시글 무한 스크롤 페이지네이션
- **상세 조회**: Markdown 렌더링, 댓글, 통계 표시
- **작성**: Toast UI Editor를 활용한 Markdown 작성
- **수정/삭제**: 작성자 권한 확인 후 수정/삭제
- **검색**: 간단 검색 및 고급 검색 (키워드, 태그, 카테고리)

### 2. 사용자 기능
- **인증**: OAuth2 기반 Portal Shell로부터 로그인 정보 공유
- **권한 관리**: 작성자만 수정/삭제 가능
- **프로필**: 작성자 정보 및 통계

### 3. 게시글 확장 기능
- **댓글**: 게시글별 댓글 작성, 수정, 삭제
- **태그/카테고리**: 게시글 분류 및 필터링
- **통계**: 조회수, 좋아요, 인기도 추적
- **시리즈**: 관련 게시글 그룹화

## 작동 모드

### 1. **Embedded 모드** (권장)
Portal Shell에 의해 로드되는 모드입니다.

```javascript
// Portal Shell (Host)에서
const blogApp = await import('blog/bootstrap').then(m => m.mountBlogApp);
const instance = blogApp(container, {
  initialPath: '/123',
  onNavigate: (path) => console.log('Blog navigated to:', path)
});
```

**특징**:
- Memory History 사용 (브라우저 URL 관리 안 함)
- Portal Shell이 URL 상태를 관리
- `window.__POWERED_BY_PORTAL_SHELL__` 플래그 설정 필요
- CSS 오염 방지를 위한 자동 정리 기능

### 2. **Standalone 모드**
직접 브라우저에서 접속하는 모드입니다.

```bash
# http://localhost:30001 에서 실행
npm run dev
```

**특징**:
- Web History 사용 (브라우저 URL 관리)
- 독립적으로 실행 가능
- 개발/테스트용 모드

## 프로젝트 구조

```
frontend/blog-frontend/
├── src/
│   ├── api/              # API 클라이언트
│   │   ├── posts.ts     # 게시글 API
│   │   ├── comments.ts  # 댓글 API
│   │   ├── files.ts     # 파일 업로드 API
│   │   └── index.ts     # Portal Shell의 apiClient 재내보내기
│   ├── components/       # Vue 컴포넌트
│   │   ├── PostCard.vue # 게시글 카드
│   │   └── HelloWorld.vue
│   ├── dto/              # 데이터 전송 객체 (타입 정의)
│   │   ├── post.ts
│   │   ├── comment.ts
│   │   ├── series.ts
│   │   ├── tag.ts
│   │   └── file.ts
│   ├── views/            # 페이지 컴포넌트
│   │   ├── PostListPage.vue    # 게시글 목록
│   │   ├── PostDetailPage.vue  # 게시글 상세
│   │   ├── PostWritePage.vue   # 게시글 작성
│   │   └── PostEditPage.vue    # 게시글 수정
│   ├── stores/           # Pinia 상태 관리
│   │   └── searchStore.ts
│   ├── config/           # 설정 파일
│   │   └── assets.ts    # 기본 이미지 및 상수
│   ├── router/
│   │   └── index.ts     # Vue Router 설정
│   ├── types/            # TypeScript 타입
│   ├── App.vue          # 루트 컴포넌트
│   ├── bootstrap.ts     # Module Federation 부트스트랩
│   ├── main.ts          # Standalone 모드 진입점
│   └── style.css        # 전역 스타일
├── vite.config.ts       # Vite + Federation 설정
├── package.json
└── docs/                # 문서
    ├── README.md        # 이 파일
    ├── ARCHITECTURE.md  # 아키텍처
    ├── FEDERATION.md    # Module Federation 상세
    ├── COMPONENTS.md    # 컴포넌트 가이드
    └── API.md           # API 사용법
```

## 시작하기

### 환경 변수 설정

`.env.local` 또는 `.env.dev` 파일을 생성합니다:

```env
# Portal Shell의 Remote Entry URL
VITE_PORTAL_SHELL_REMOTE_URL=http://localhost:30000

# Shopping Frontend의 Remote Entry URL
VITE_SHOPPING_REMOTE_URL=http://localhost:30002
```

### 개발 서버 실행

```bash
# 전체 frontend 워크스페이스에서
cd frontend
npm install
npm run dev

# 또는 blog-frontend만 실행
cd frontend/blog-frontend
npm run dev         # 30001 포트에서 실행
npm run preview     # 빌드된 버전 미리보기
```

### 빌드

```bash
npm run build:dev       # 개발 환경 빌드
npm run build:docker    # Docker 환경 빌드
npm run build:k8s       # Kubernetes 환경 빌드
```

## API 통신

### API 클라이언트 사용

```typescript
// Portal Shell의 apiClient를 상속받아 사용
import apiClient from '@/api';

// 게시글 목록 조회
import { getPublishedPosts } from '@/api/posts';

const posts = await getPublishedPosts(page, size);
```

### API 경로

```
GET    /api/blog/posts              # 발행된 게시글 목록
GET    /api/blog/posts/:postId      # 게시글 상세
POST   /api/blog/posts              # 게시글 생성
PUT    /api/blog/posts/:postId      # 게시글 수정
DELETE /api/blog/posts/:postId      # 게시글 삭제
GET    /api/blog/posts/search       # 검색
```

자세한 내용은 [API.md](./API.md) 참조

## 라우팅

### Embedded 모드 라우팅

Portal Shell의 경로에서 다음과 같이 라우팅됩니다:

```
/blog/              → 게시글 목록
/blog/:postId       → 게시글 상세
/blog/write         → 게시글 작성
/blog/edit/:postId  → 게시글 수정
```

내부적으로는 Memory History를 사용하여 `/`, `/:postId`, `/write`, `/edit/:postId`로 관리됩니다.

### Standalone 모드 라우팅

직접 브라우저 URL을 통해 라우팅됩니다:

```
http://localhost:30001/              → 게시글 목록
http://localhost:30001/:postId       → 게시글 상세
http://localhost:30001/write         → 게시글 작성
http://localhost:30001/edit/:postId  → 게시글 수정
```

## 상태 관리 (Pinia)

### Search Store

```typescript
import { useSearchStore } from '@/stores/searchStore';

const search = useSearchStore();

// 검색 실행
await search.search('React');

// 더 로드
await search.loadMore();

// 초기화
search.clear();
```

상태:
- `keyword`: 검색어
- `results`: 검색 결과 배열
- `isSearching`: 로딩 상태
- `error`: 에러 메시지
- `hasMore`: 추가 로드 가능 여부

## 스타일 및 디자인

### Design System 활용

```vue
<script setup lang="ts">
import { Button, Card, SearchBar } from '@portal/design-system';
</script>

<template>
  <Button variant="primary">클릭</Button>
  <Card hoverable>콘텐츠</Card>
</template>
```

### 데이터 서비스 속성

Blog 모듈이 활성화되면 HTML 루트 요소에 `data-service="blog"` 속성이 자동으로 설정됩니다. 이를 활용하여 Blog 특화 스타일을 적용할 수 있습니다:

```css
[data-service="blog"] .post-list {
  /* Blog 특화 스타일 */
}
```

## 타입 안전성

모든 API 응답과 데이터는 TypeScript 타입으로 정의되어 있습니다:

```typescript
// src/types/index.ts에서 모든 타입을 재내보냄
export type { PostResponse, PostCreateRequest, ... } from '../dto/post';

// 사용 예
import type { PostResponse } from '@/types';

const post: PostResponse = await getPostById('123');
```

## 성능 최적화

### 1. 무한 스크롤 (PostListPage)
- Intersection Observer API 사용
- 페이징 기반 로드
- 검색 결과와 일반 목록 분리 관리

### 2. 컴포넌트 최적화
- PostCard: 썸네일 이미지 에러 핸들링, 상대 시간 계산
- Lazy Loading: Vue Router의 자동 코드 스플리팅

### 3. Markdown 렌더링
- Toast UI Editor Viewer 사용
- 코드 신택스 하이라이팅 (Prism.js)
- 다크 모드 지원

## 보안 고려사항

### 1. 인증
- Portal Shell의 `authStore` 사용
- 토큰은 Portal Shell에서 관리

### 2. 권한 확인
```typescript
const authStore = useAuthStore();
if (authStore.isAuthenticated) {
  // 인증된 사용자만 접근
}
```

### 3. CSRF 보호
- API Gateway에서 CSRF 토큰 처리 (설정되어 있음)

## 트러블슈팅

### Portal Shell에 로드되지 않음

1. **원격 URL 확인**: `vite.config.ts`의 `remotes` 설정 확인
2. **모드 플래그 확인**: Portal Shell에서 `window.__POWERED_BY_PORTAL_SHELL__ = true` 설정
3. **네트워크 확인**: Console에서 `remoteEntry.js` 로드 상태 확인

### 스타일 오염

- 모듈 언마운트 시 CSS 자동 정리 (bootstrap.ts의 unmount 함수)
- 필요시 CSS 모듈 또는 scoped 스타일 사용

### 라우터 버그

- Embedded 모드: Memory History 사용으로 `pushState` 에러 무시
- Parent 네비게이션: `onParentNavigate()` 함수로 명시적 동기화

## 관련 문서

- [ARCHITECTURE.md](./ARCHITECTURE.md) - 블로그 아키텍처 상세
- [FEDERATION.md](./FEDERATION.md) - Module Federation 설정 및 공유 라이브러리
- [COMPONENTS.md](./COMPONENTS.md) - 주요 컴포넌트 설명
- [API.md](./API.md) - API 클라이언트 사용 가이드

## 라이센스

MIT
