---
id: blog-frontend-status
title: Blog Frontend 프로젝트 현황
type: guide
status: current
created: 2026-01-26
updated: 2026-01-30
author: Laze
tags:
  - blog-frontend
  - vue3
  - module-federation
  - status
---

# Blog Frontend 프로젝트 현황

> Blog Frontend (Vue 3) 프로젝트의 전체 구현 현황 및 문서화 상태

## 📊 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **Framework** | Vue 3.5.21 |
| **Language** | TypeScript 5.9.3 |
| **Build Tool** | Vite 7.1.7 |
| **Port** | 30001 (Dev) |
| **Module Federation** | Remote (Host: portal-shell) |
| **E2E Testing** | Playwright (9 specs) |

## 🛠 기술 스택

### Core
- **Vue 3.5.21**: Composition API, `<script setup>`
- **TypeScript 5.9.3**: Strict mode
- **Vite 7.1.7**: 빌드 및 개발 서버
- **Vue Router 4.5.0**: SPA 라우팅

### State Management
- **Pinia 3.0.3**: searchStore, followStore

### UI/Styling
- **Tailwind CSS 3.4.15**: 유틸리티 기반 스타일링
- **@portal/design-system-vue**: 디자인 시스템
- **@portal/design-tokens**: 디자인 토큰

### Content Editor
- **Toast UI Editor 3.2.2**: 마크다운 에디터 (게시물 작성/수정)

### HTTP/Auth
- **axios 1.12.2**: HTTP 클라이언트
- **oidc-client-ts 3.3.0**: OAuth2/OIDC 인증

### Module Federation
- **@module-federation/enhanced**: Webpack 5 Module Federation
- **name**: `blog`
- **Remotes**: portal (Host), shopping
- **Exposes**: `./bootstrap` → bootstrap.ts

## 📄 페이지 구성 (9개 라우트)

| 경로 | 컴포넌트 | 인증 | 설명 |
|------|---------|------|------|
| `/` | PostListPage | ❌ | 게시물 목록 (feed/trending/recent 탭) |
| `/:postId` | PostDetailPage | ❌ | 게시물 상세 |
| `/write` | PostWritePage | 권장 | 게시물 작성 |
| `/edit/:postId` | PostEditPage | 소유자 | 게시물 수정 |
| `/tags` | TagListPage | ❌ | 태그 목록 |
| `/tags/:tagName` | TagDetailPage | ❌ | 태그별 게시물 |
| `/series/:seriesId` | SeriesDetailPage | ❌ | 시리즈 상세 |
| `/my` | MyPage | ✅ | 내 페이지 (프로필/게시물) |
| `/@:username` | UserBlogPage | ❌ | 사용자 블로그 |

## 🧩 컴포넌트 현황 (27개)

### Views (9개)
- **PostListPage**: 게시물 목록 (feed/trending/recent 탭 시스템)
- **PostDetailPage**: 게시물 상세
- **PostWritePage**: 게시물 작성
- **PostEditPage**: 게시물 수정
- **TagListPage**: 태그 목록
- **TagDetailPage**: 태그별 게시물
- **SeriesDetailPage**: 시리즈 상세
- **MyPage**: 내 페이지
- **UserBlogPage**: 사용자 블로그

### Components (18개)
- **Post**: PostCard, PostNavigation, RelatedPosts, MyPostList
- **Comment**: CommentList, CommentForm, CommentItem
- **Interaction**: LikeButton, LikersModal
- **Series**: SeriesCard, SeriesBox, MySeriesList
- **Social**: FollowButton, FollowerModal
- **User**: UserProfileCard, ProfileEditForm
- **Tag**: TagAutocomplete
- **Legacy**: HelloWorld (미사용)

## 🔌 API 연동 현황 (8개 모듈, 64개 함수)

| 모듈 | 함수 수 | Base Path | 대상 서비스 |
|------|---------|-----------|------------|
| **posts.ts** | 30 | `/api/blog/posts` | blog-service |
| **comments.ts** | 4 | `/api/blog/comments` | blog-service |
| **likes.ts** | 3 | `/api/blog/likes` | blog-service |
| **tags.ts** | 6 | `/api/blog/tags` | blog-service |
| **series.ts** | 8 | `/api/blog/series` | blog-service |
| **users.ts** | 6 | `/auth-api/users` | auth-service (via Gateway) |
| **follow.ts** | 5 | `/auth-api/users/{username}/follow` | auth-service (via Gateway) |
| **files.ts** | 2 | `/api/blog/file` | blog-service |

### posts.ts 주요 함수 (30개)
- CRUD: `getPosts`, `getPostById`, `createPost`, `updatePost`, `deletePost`
- 검색: `searchPosts`, `searchPostsAdvanced`
- 트렌딩: `getTrendingPosts` (기간별: today/week/month/year)
- 피드: `getFeedPosts` (팔로잉 기반)
- 시리즈: `getPostSeries`, `addPostToSeries`, `removePostFromSeries`
- 네비게이션: `getNextPost`, `getPreviousPost`
- 기타: `getPostsByTag`, `getPostsBySeries`, `getPostsByAuthor`, `getRelatedPosts`

### comments.ts (4개)
- `getComments`, `createComment`, `updateComment`, `deleteComment`

### likes.ts (3개)
- `toggleLike`, `getLikes`, `isLiked`

### tags.ts (6개)
- `getTags`, `getTagByName`, `getPopularTags`, `getPostsByTag`, `searchTags`, `getTagCount`

### series.ts (8개)
- `getSeries`, `getSeriesById`, `createSeries`, `updateSeries`, `deleteSeries`
- `getSeriesPosts`, `addPostToSeries`, `removePostFromSeries`

### users.ts (6개)
- `getUser`, `getUserByUsername`, `getCurrentUser`, `updateUser`, `uploadAvatar`, `deleteAvatar`

### follow.ts (5개)
- `followUser`, `unfollowUser`, `getFollowers`, `getFollowings`, `isFollowing`

### files.ts (2개)
- `uploadFile`, `uploadImage`

## 🗂 상태 관리 (Pinia, 2개 Store)

### searchStore
**State:**
- `keyword: string` - 검색어
- `results: Post[]` - 검색 결과
- `isSearching: boolean` - 검색 중 여부
- `error: Error | null` - 에러
- `currentPage: number` - 현재 페이지
- `totalPages: number` - 전체 페이지
- `hasMore: boolean` - 추가 데이터 존재 여부

**Actions:**
- `search()` - 검색 실행
- `loadMore()` - 추가 로드
- `clear()` - 검색 초기화

### followStore
**State:**
- `followingIds: Set<string>` - 팔로잉 중인 사용자 UUID 집합
- `followingIdsLoaded: boolean` - 팔로잉 목록 로드 완료 여부
- `loading: boolean` - 로딩 상태
- `error: Error | null` - 에러
- `followersCache: Map<string, User[]>` - 팔로워 캐시
- `followingsCache: Map<string, User[]>` - 팔로잉 캐시

**Getters:**
- `isFollowing(uuid: string): boolean` - 팔로잉 여부 확인
- `followingCount: number` - 팔로잉 수

**Actions:**
- `loadFollowingIds()` - 팔로잉 목록 로드
- `toggleFollow(username: string, uuid: string)` - 팔로우 토글
- `getFollowers(username: string)` - 팔로워 목록 조회
- `getFollowings(username: string)` - 팔로잉 목록 조회
- `checkFollowStatus(uuid: string)` - 팔로우 상태 확인
- `clearCache()` - 캐시 초기화
- `reset()` - 전체 초기화

## 🔄 Module Federation 아키텍처

### 설정
```javascript
{
  name: 'blog',
  remotes: {
    portal: 'portal@http://localhost:30000/assets/remoteEntry.js',
    shopping: 'shopping@http://localhost:30002/assets/remoteEntry.js'
  },
  exposes: {
    './bootstrap': './src/bootstrap.ts'
  },
  shared: ['vue', 'pinia', 'axios', ...]
}
```

### Dual Mode 지원
- **Standalone Mode**: Web History Router (독립 실행, 30001 포트)
- **Embedded Mode**: Memory History Router (portal-shell에 통합)

### 라이프사이클
- `bootstrap(options?)` - 앱 초기화 및 마운트
- `unmount()` - 앱 언마운트 및 정리
- **미문서화**: `onActivated()`, `onDeactivated()` 라이프사이클 훅

## ✅ 기능별 구현 상태

| 기능 | Frontend | Backend 연동 | 비고 |
|------|----------|-------------|------|
| **게시물 CRUD** | ✅ | ✅ | Toast UI Editor 사용 |
| **게시물 검색** | ✅ | ✅ | 단순 검색 + 고급 검색 |
| **트렌딩** | ✅ | ✅ | 기간별 (today/week/month/year) |
| **피드** | ✅ | ✅ | 팔로잉 기반 게시물 피드 |
| **댓글** | ✅ | ✅ | 대댓글 지원 |
| **좋아요** | ✅ | ✅ | 토글 방식 |
| **시리즈** | ✅ | ✅ | 조회 + 네비게이션 |
| **태그** | ✅ | ✅ | 목록/상세/검색 |
| **파일 업로드** | ✅ | ✅ | 에디터 내 이미지 업로드 |
| **사용자 프로필** | ✅ | ✅ | auth-service 연동 |
| **팔로우** | ✅ | ✅ | auth-service 연동 |
| **포스트 네비게이션** | ✅ | ✅ | 이전/다음 게시물 |
| **다크모드** | ✅ | N/A | Portal Shell 다크모드와 동기화 |
| **E2E 테스트** | ✅ | N/A | Playwright (9개 spec) |

## 📚 문서화 현황 (Phase 2 대조)

### API 문서 커버리지: 51% (8개 중 3개만 완료)
- ✅ **posts.ts**: 완전 문서화 (30개 함수)
- ✅ **comments.ts**: 완전 문서화 (4개 함수)
- ✅ **files.ts**: 완전 문서화 (2개 함수)
- ❌ **likes.ts**: 문서 없음 (3개 함수)
- ❌ **tags.ts**: 문서 없음 (6개 함수)
- ❌ **series.ts**: 문서 없음 (8개 함수)
- ❌ **users.ts**: 문서 없음 (6개 함수)
- ❌ **follow.ts**: 문서 없음 (5개 함수)

### 컴포넌트 문서 커버리지: 33% (27개 중 9개 완료)
- ✅ **완료 (9개)**: PostCard, PostDetailPage, CommentList, LikeButton, SeriesBox, MyPage, MySeriesList, LikersModal, TagAutocomplete
- ❌ **미문서화 (18개)**: PostListPage, PostWritePage, PostEditPage, TagListPage, TagDetailPage, SeriesDetailPage, UserBlogPage, PostNavigation, RelatedPosts, MyPostList, CommentForm, CommentItem, SeriesCard, FollowButton, FollowerModal, UserProfileCard, ProfileEditForm, HelloWorld

### 라우트 문서 커버리지: 44% (9개 중 4개만 완료)
- ✅ `/`, `/:postId`, `/write`, `/edit/:postId`
- ❌ `/tags`, `/tags/:tagName`, `/series/:seriesId`, `/my`, `/@:username`

### Store 문서 커버리지: 50% (2개 중 1개만 완료)
- ✅ searchStore
- ❌ followStore

### Module Federation 문서 커버리지: 95%
- ✅ 기본 설정, Dual Mode, bootstrap/unmount
- ✅ onActivated/onDeactivated 라이프사이클 훅 (Phase 4에서 추가)

### 기타 문서 이슈
- ✅ ~~API Client import 경로 오류~~: `portal/apiClient` → `portal/api` 수정 완료
- ❌ **PostListPage 탭 시스템 미문서화**: feed/trending/recent 탭 구조
- ❌ **getting-started.md 미완성**: 환경 설정 및 시작 가이드 불완전

## 🚨 알려진 이슈 및 TODO

### 문서화 우선순위
1. ~~**High**: API 문서 5개 모듈 누락~~ → ✅ 완료 (likes, tags, series, users, follow 추가)
2. ~~**High**: 컴포넌트 문서 18개 누락~~ → ✅ 완료 (18개 컴포넌트 문서 추가)
3. ~~**Medium**: followStore 문서화~~ → ✅ 완료 (ARCHITECTURE.md에 추가)
4. ~~**Medium**: 라우트 5개 문서화~~ → ✅ 완료 (ARCHITECTURE.md에 9개 라우트 반영)
5. ~~**Low**: API Client import 경로 수정~~ → ✅ 완료
6. **Low**: PostListPage 탭 시스템 문서화
7. **Low**: getting-started.md 완성

### 코드 개선 TODO
- HelloWorld 컴포넌트 제거 (미사용)
- E2E 테스트 커버리지 확대
- 성능 최적화 (Lazy Loading, Code Splitting)

## 🏗 빌드 & 실행

### 개발 환경
```bash
npm run dev              # Vite dev server (30001 포트)
```

### 빌드
```bash
npm run build            # dev 환경 빌드
npm run build:docker     # Docker 환경 빌드
npm run build:k8s        # Kubernetes 환경 빌드
```

### 테스트
```bash
npm run test:e2e         # Playwright E2E 테스트 (9개 spec)
```

## 📁 디렉토리 구조

```
frontend/blog-frontend/
├── src/
│   ├── api/              # API 클라이언트 (8개 모듈)
│   ├── components/       # 재사용 컴포넌트 (18개)
│   ├── views/            # 페이지 컴포넌트 (9개)
│   ├── stores/           # Pinia stores (2개)
│   ├── router/           # Vue Router 설정
│   ├── types/            # TypeScript 타입 정의
│   ├── utils/            # 유틸리티 함수
│   ├── App.vue           # 루트 컴포넌트
│   ├── main.ts           # 엔트리포인트 (Standalone)
│   └── bootstrap.ts      # Module Federation 엔트리
├── docs/                 # 프로젝트 문서
├── e2e/                  # Playwright E2E 테스트
├── public/               # 정적 자산
├── vite.config.ts        # Vite 설정
├── tailwind.config.js    # Tailwind 설정
└── package.json          # 의존성 관리
```

## 🔗 관련 문서

- [Getting Started](./guides/getting-started.md) - 시작 가이드 (미완성)
- [API Documentation](./api/) - API 문서 (부분 완료)
- [Architecture](./architecture/) - 아키텍처 문서
- [Components](./COMPONENTS.md) - 컴포넌트 가이드
- [Module Federation](./FEDERATION.md) - Module Federation 설정

## 📝 업데이트 이력

- **2026-01-30**: 컴포넌트 3개 추가 (MySeriesList, LikersModal, TagAutocomplete)
- **2026-01-26**: 초기 작성 (Phase 2 코드 분석 결과 기반)

---

**Last Updated**: 2026-01-30
**Maintainer**: Laze
**Status**: 🟡 In Progress (문서화 진행 중)
