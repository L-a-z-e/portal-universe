# Blog Frontend Architecture

## 시스템 아키텍처

### 고수준 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│                     Portal Shell (Host)                      │
│                                                               │
│  ┌──────────────────┐  ┌─────────────────┐  ┌────────────┐ │
│  │  Auth Store      │  │  API Client     │  │  Router    │ │
│  │  (Shared)        │  │  (Shared)       │  │  (Host)    │ │
│  └──────────────────┘  └─────────────────┘  └────────────┘ │
│                               ▲                              │
└───────────────────────────────┼──────────────────────────────┘
                                │
                    ┌───────────┴────────────┐
                    │ Module Federation      │
                    │ (Shared: vue, pinia)   │
                    └───────────┬────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼──────────────────────────────────┐  ┌────────▼────────┐
│  Blog Frontend (Remote)                  │  │ Shopping        │
│                                          │  │ Frontend        │
│ Pages:                                   │  │ (Remote)        │
│ - PostListPage (Feed/Trending/Recent)    │  │                 │
│ - PostDetailPage (Markdown/Comments)     │  │                 │
│ - PostWritePage / PostEditPage           │  │                 │
│ - TagListPage / TagDetailPage            │  │                 │
│ - SeriesDetailPage                       │  │                 │
│ - MyPage (프로필/게시글/시리즈)            │  │                 │
│ - UserBlogPage (@username)               │  │                 │
└──────────────────────────────────────────┘  └─────────────────┘
```

## 계층 구조

### 1. Presentation Layer (표현층)

#### Views (페이지 — 9개)
- **PostListPage**: 게시글 목록, 탭(Feed/Trending/Recent), 무한 스크롤, 검색
- **PostDetailPage**: 게시글 상세, Markdown 렌더링, 댓글, 좋아요, 시리즈 네비게이션
- **PostWritePage**: 새 게시글 작성 (Toast UI Editor)
- **PostEditPage**: 기존 게시글 수정
- **TagListPage**: 태그 목록, 검색/정렬, 태그 클라우드
- **TagDetailPage**: 태그별 게시글, 무한 스크롤
- **SeriesDetailPage**: 시리즈 상세, 순번 목록
- **MyPage**: 마이페이지 (프로필 조회/수정, 내 게시글, 내 시리즈)
- **UserBlogPage**: 사용자 블로그 (`/@username`)

#### Components (재사용 가능 컴포넌트 — 14개)
- **PostCard**: 게시글 요약 카드 (목록용)
- **PostNavigation**: 이전/다음 게시글 네비게이션
- **RelatedPosts**: 관련 게시글 그리드
- **MyPostList**: 내 게시글 관리 (상태별 필터)
- **CommentList / CommentForm / CommentItem**: 댓글 트리 구조 CRUD
- **LikeButton**: 좋아요 토글 (Optimistic UI)
- **SeriesCard / SeriesBox**: 시리즈 카드 및 네비게이션
- **FollowButton / FollowerModal**: 팔로우 토글 및 목록 모달
- **UserProfileCard / ProfileEditForm**: 프로필 표시 및 수정
- Design System 컴포넌트: Button, Card, Tag, Avatar, SearchBar, Modal, Alert, Spinner

### 2. State Management Layer (상태 관리층)

#### Pinia Stores
```
searchStore
├── keyword: string
├── results: PostSummaryResponse[]
├── isSearching: boolean
├── error: string | null
├── hasMore: boolean
└── Actions: search(), loadMore(), clear()
```

```
followStore
├── followingIds: string[]           # 팔로잉 UUID 배열
├── followingIdsLoaded: boolean
├── loading: boolean
├── error: Error | null
├── followersCache: Map<string, FollowListResponse>
├── followingsCache: Map<string, FollowListResponse>
├── Getters: isFollowing(uuid), followingCount
└── Actions: loadFollowingIds(), toggleFollow(),
             getFollowers(), getFollowings(),
             checkFollowStatus(), clearCache(), reset()
```

#### Shared Stores (Portal Shell)
```
authStore (공유)
├── user: UserInfo
├── isAuthenticated: boolean
├── token: string
└── Actions: login(), logout(), refreshToken()
```

### 3. API Layer (API 통신층)

#### API Endpoints

```typescript
// src/api/posts.ts
├── 기본 CRUD
│   ├── createPost()
│   ├── getPostById()
│   ├── updatePost()
│   └── deletePost()
├── 목록 조회
│   ├── getPublishedPosts()      # 페이징
│   ├── getMyPosts()             # 내 게시글
│   ├── getPostsByAuthor()       # 작성자별
│   ├── getPostsByCategory()     # 카테고리별
│   ├── getPostsByTags()         # 태그별
│   ├── getPopularPosts()        # 인기
│   ├── getRecentPosts()         # 최근
│   └── getRelatedPosts()        # 관련 게시글
├── 검색
│   ├── searchPosts()            # 간단 검색
│   └── searchPostsAdvanced()    # 고급 검색
├── 상태 관리
│   └── changePostStatus()
├── 피드/트렌딩
│   ├── getTrendingPosts()         # 인기 게시글
│   ├── getFeed()                  # 팔로잉 피드
│   └── getPostNavigation()        # 이전/다음 글
└── 통계
    ├── getCategoryStats()
    ├── getPopularTags()
    ├── getAuthorStats()
    └── getBlogStats()

// src/api/comments.ts
├── getCommentsByPostId()
├── createComment()
├── updateComment()
└── deleteComment()

// src/api/files.ts
├── uploadFile()              # multipart/form-data
└── deleteFile()              # ADMIN 권한

// src/api/likes.ts
├── toggleLike()              # 좋아요 토글
├── getLikeStatus()           # 상태 확인
└── getLikers()               # 좋아요한 사용자 목록

// src/api/tags.ts
├── getAllTags()
├── getTagById()
├── getTagByName()
├── getPostsByTag()           # 태그별 게시글
├── getPopularTags()
└── searchTags()

// src/api/series.ts
├── getSeriesList()           # 목록 (작성자 필터)
├── getSeriesById()
├── getSeriesPosts()
├── getMySeries()
├── createSeries()
├── updateSeries()
├── deleteSeries()
└── reorderSeriesPosts()      # 순서 변경

// src/api/users.ts (→ auth-service)
├── getPublicProfile()        # username 기반
├── getMyProfile()
├── updateProfile()
├── setUsername()              # 최초 1회
├── checkUsername()            # 중복 확인
└── getUserPosts()

// src/api/follow.ts (→ auth-service)
├── toggleFollow()
├── getFollowers()
├── getFollowings()
├── getFollowStatus()
└── getMyFollowingIds()       # 피드용 UUID 목록
```

#### API Client 상속

```typescript
// src/api/index.ts
import { apiClient } from 'portal/api';
export default apiClient;

// Portal Shell의 apiClient를 재사용
// - Base URL: /api/v1/blog
// - 인증 토큰 자동 포함
// - CORS/CSRF 처리됨
```

### 4. Type/DTO Layer (타입 정의층)

#### DTOs (Data Transfer Objects)

```
src/dto/
├── post.ts
│   ├── PostResponse          # 게시글 상세
│   ├── PostSummaryResponse   # 게시글 요약
│   ├── PostCreateRequest     # 생성 요청
│   ├── PostUpdateRequest     # 수정 요청
│   └── ...
├── comment.ts
│   ├── CommentResponse
│   ├── CommentCreateRequest
│   └── CommentUpdateRequest
├── series.ts                 # 시리즈
├── tag.ts                    # 태그
├── file.ts                   # 파일
├── user.ts                   # 사용자 프로필, Username
└── follow.ts                 # 팔로우/팔로워

src/types/
├── index.ts                  # 모든 DTO 재내보내기
└── common.ts                 # ApiResponse, PageResponse
```

### 5. Router Layer (라우팅층)

#### 라우트 정의

```typescript
// src/router/index.ts
const routes = [
  { path: '/',                name: 'PostList',      component: PostListPage },
  { path: '/tags',            name: 'TagList',       component: TagListPage },
  { path: '/tags/:tagName',   name: 'TagDetail',     component: TagDetailPage,    props: true },
  { path: '/write',           name: 'PostWrite',     component: PostWritePage },
  { path: '/edit/:postId',    name: 'PostEdit',      component: PostEditPage,     props: true },
  { path: '/series/:seriesId',name: 'SeriesDetail',  component: SeriesDetailPage, props: true },
  { path: '/my',              name: 'MyPage',        component: MyPage,           meta: { requiresAuth: true } },
  { path: '/@:username',      name: 'UserBlog',      component: UserBlogPage,     props: true },
  { path: '/:postId',         name: 'PostDetail',    component: PostDetailPage,   props: true }
];
```

#### 두 가지 라우터 모드

**Embedded 모드 (Memory History)**
- Parent가 URL 상태를 관리
- 내부 네비게이션만 처리
- `onParentNavigate()` 콜백으로 부모 동기화

**Standalone 모드 (Web History)**
- 브라우저 URL 직접 관리
- 새로고침 시에도 상태 유지

## 데이터 플로우

### 게시글 목록 조회 플로우

```
┌─────────────────────┐
│  PostListPage       │ onMounted
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ loadPosts()         │ 함수 호출
└──────────┬──────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ getPublishedPosts(page, size)        │ API 호출
│ (from @/api/posts)                  │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ apiClient.get('/api/v1/blog/posts')    │ HTTP GET
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ API Gateway                         │ /api/v1/blog/** 라우팅
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ blog-service                        │ 게시글 조회
│ /posts (GET)                        │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ PageResponse<PostSummaryResponse>   │ 응답
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ posts.value = response.content      │ 상태 업데이트
│ (reactive ref)                      │
└──────────┬──────────────────────────┘
           │
           ▼
┌─────────────────────────────────────┐
│ <PostCard v-for="post in posts">   │ UI 업데이트
└─────────────────────────────────────┘
```

### 검색 플로우

```
┌──────────────────────┐
│ SearchBar            │ @search 이벤트
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────────┐
│ handleSearch(keyword)            │
│ searchStore.search(keyword)      │
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────┐
│ searchPosts(keyword, page, size) │ API 호출
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────┐
│ results.value = response.content │ 상태 업데이트
│ (Pinia store)                    │
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────┐
│ displayPosts = computed          │ computed로 동적 선택
│ (검색 vs 일반 목록)              │
└──────────┬───────────────────────┘
           │
           ▼
┌──────────────────────────────────┐
│ <PostCard v-for="post in ..."   │ UI 업데이트
└──────────────────────────────────┘
```

## Module Federation Architecture

### 공유 라이브러리

```typescript
// vite.config.ts
shared: ['vue', 'pinia', 'axios']
```

**이점**:
- 중복 번들 크기 감소
- 버전 일관성 보장
- Portal Shell과 Remote 간 상태 공유 가능

### 내보낸 모듈

```typescript
exposes: {
  './bootstrap': './src/bootstrap.ts'  // 부트스트랩 함수
}
```

**사용 예**:
```typescript
const { mountBlogApp } = await import('blog/bootstrap');
const app = mountBlogApp(container, options);
```

## 상태 공유 아키텍처

### Portal Shell (Host) 제공

```typescript
// Portal Shell에서 제공하는 공유 상태
shared: {
  authStore: useAuthStore(),  // 사용자 인증 정보
  apiClient: axiosInstance,   // API 클라이언트
}
```

### Blog Frontend (Remote) 사용

```typescript
// 1. 인증 정보 접근
const authStore = useAuthStore();
if (authStore.isAuthenticated) {
  // 로그인한 사용자만 게시글 작성 가능
}

// 2. API 클라이언트 사용
import { apiClient } from 'portal/api';
const { data } = await apiClient.get('/api/v1/blog/posts');

// 3. 네비게이션 공유
router.afterEach((to) => {
  onNavigate?.(to.path);  // Parent에 경로 변경 알림
});
```

## 성능 최적화 전략

### 1. 코드 분할 (Code Splitting)

```typescript
// Vite + Vue Router 자동 코드 분할
// 9개 라우트 각각 자동으로 분할됨
const routes = [
  { path: '/', component: PostListPage },
  { path: '/tags', component: TagListPage },
  { path: '/tags/:tagName', component: TagDetailPage },
  { path: '/write', component: PostWritePage },
  { path: '/edit/:postId', component: PostEditPage },
  { path: '/series/:seriesId', component: SeriesDetailPage },
  { path: '/my', component: MyPage },
  { path: '/@:username', component: UserBlogPage },
  { path: '/:postId', component: PostDetailPage },
];
```

### 2. 무한 스크롤 최적화

```typescript
// Intersection Observer로 효율적인 로딩
const observer = new IntersectionObserver((entries) => {
  if (entries[0].isIntersecting && canLoadMore.value) {
    loadMore();  // 페이징 기반 로드
  }
}, {
  rootMargin: '100px'  // 미리 로드
});
```

### 3. 이미지 최적화

```typescript
// PostCard에서 썸네일 에러 핸들링
const imgError = ref(false);
const thumbnailSrc = computed(() => {
  return imgError.value ? DEFAULT_THUMBNAIL : post.thumbnailUrl;
});
```

### 4. 메모리 관리

```typescript
// 모듈 언마운트 시 리소스 정리
unmount: () => {
  app.unmount();
  el.innerHTML = '';           // DOM 정리
  // <head>의 Blog CSS 제거
  // Intersection Observer 정리
};
```

## 에러 처리

### API 에러

```typescript
try {
  const posts = await getPublishedPosts(page, size);
} catch (err) {
  error.value = '게시글을 불러올 수 없습니다';
  console.error(err);
}
```

### 네비게이션 에러

```typescript
router.push(path).catch(err => {
  console.error('Navigation failed:', err);
});
```

## 보안 고려사항

### 1. 인증
- Portal Shell의 토큰 사용
- API Gateway에서 JWT 검증

### 2. 권한
```typescript
// 작성자만 수정 가능
if (post.authorId === authStore.user?.id) {
  // 수정/삭제 버튼 표시
}
```

### 3. XSS 방지
- Vue 자동 이스케이프
- Toast UI Editor의 안전한 렌더링

## 관련 문서

- [FEDERATION.md](./FEDERATION.md) - Module Federation 상세 설정
- [COMPONENTS.md](./COMPONENTS.md) - 컴포넌트 가이드
- [API.md](./API.md) - API 사용법
