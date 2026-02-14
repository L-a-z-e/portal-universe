---
id: arch-data-flow
title: Blog Frontend Data Flow
type: architecture
status: current
created: 2026-01-18
updated: 2026-02-15
author: Laze
tags: [architecture, data-flow, api, pinia, axios, cross-service]
related:
  - arch-system-overview
---

# Blog Frontend Data Flow

## 개요

Blog Frontend는 Vue 3 기반의 마이크로 프론트엔드로, API Gateway를 통해 **blog-service**와 **auth-service** 두 개의 백엔드 서비스와 통신합니다. Pinia를 사용하여 상태를 관리하며 `searchStore`(검색)와 `followStore`(팔로우)를 운영합니다.

**핵심 특징**:
- Portal Shell에서 주입된 `apiClient` (axios 인스턴스) 사용
- **Cross-service 통신**: blog-service (`/api/v1/blog/**`) + auth-service (`/api/v1/users/**`)
- Pinia를 활용한 반응형 상태 관리 (searchStore, followStore)
- followStore의 캐시 전략으로 불필요한 API 호출 최소화

---

## 전체 데이터 흐름 아키텍처

```mermaid
graph TB
    subgraph "Portal Shell"
        PS[Portal Shell App]
        AC[apiClient<br/>axios instance]
        AS[authAdapter<br/>Store Adapter]
    end

    subgraph "Blog Frontend"
        subgraph "Vue Components"
            BC_POST[Post Views<br/>List, Detail, Write, Edit]
            BC_SOCIAL[Social Views<br/>UserBlog, MyPage]
            BC_DISCOVER[Discover Views<br/>Tags, Categories, Series]
            BC_UTIL[Util Views<br/>AdvancedSearch, Stats]
        end

        subgraph "API Functions (8 Modules)"
            API_POSTS[posts.ts<br/>22 functions]
            API_COMMENTS[comments.ts<br/>4 functions]
            API_LIKES[likes.ts<br/>3 functions]
            API_SERIES[series.ts<br/>10 functions]
            API_TAGS[tags.ts<br/>6 functions]
            API_FILES[files.ts<br/>2 functions]
            API_FOLLOW[follow.ts<br/>5 functions]
            API_USERS[users.ts<br/>5 functions]
        end

        subgraph "Pinia Stores"
            SS[searchStore<br/>검색 상태]
            FS[followStore<br/>팔로우 + 캐시]
        end
    end

    subgraph "Backend"
        GW[API Gateway :8080]
        BS[blog-service :8082]
        AUTH[auth-service :8081]
    end

    subgraph "Storage"
        MONGO[(MongoDB)]
        MYSQL[(MySQL)]
        S3[S3]
    end

    PS -->|expose| AC
    PS -->|expose| AS
    AC -->|import| API_POSTS & API_COMMENTS & API_LIKES & API_SERIES & API_TAGS & API_FILES & API_FOLLOW & API_USERS
    AS -->|usePortalAuth composable| BC_POST & BC_SOCIAL & BC_UTIL

    BC_POST -->|call| API_POSTS & API_COMMENTS & API_LIKES & API_SERIES
    BC_SOCIAL -->|call| API_USERS & API_FOLLOW & API_POSTS
    BC_DISCOVER -->|call| API_TAGS & API_SERIES & API_POSTS
    BC_UTIL -->|call| API_POSTS

    BC_POST & BC_UTIL -->|read/write| SS
    BC_SOCIAL -->|read/write| FS

    API_POSTS & API_COMMENTS & API_LIKES & API_SERIES & API_TAGS & API_FILES -->|HTTP| GW
    API_FOLLOW & API_USERS -->|HTTP| GW

    GW -->|/api/v1/blog/**| BS
    GW -->|/api/v1/users/**| AUTH

    BS --> MONGO & S3
    AUTH --> MYSQL
```

---

## 주요 데이터 흐름

### 1. 게시물 목록 조회 (Pagination)

```mermaid
sequenceDiagram
    participant U as User
    participant C as PostListPage
    participant A as getPublishedPosts()
    participant AC as apiClient
    participant G as API Gateway
    participant S as blog-service
    participant D as MongoDB

    U->>C: 페이지 방문
    C->>C: onMounted()
    C->>A: getPublishedPosts(page=0, size=10)
    A->>AC: GET /api/v1/blog/posts?page=0&size=10
    Note over AC: Authorization: Bearer {JWT}
    AC->>G: HTTP Request
    G->>G: JWT 검증
    G->>S: Forward to blog-service
    S->>D: Query posts (page, size)
    D-->>S: PageResponse<PostSummary>
    S-->>G: ApiResponse<PageResponse>
    G-->>AC: HTTP 200 OK
    AC-->>A: axios response
    A-->>C: PageResponse<PostSummaryResponse>
    C->>C: posts.value = response.content
    C->>U: 게시물 목록 렌더링
```

### 2. 게시물 상세 조회 (병렬 로딩)

PostDetailPage는 게시물 본문과 함께 여러 관련 데이터를 **병렬로** 로드합니다.

```mermaid
sequenceDiagram
    participant U as User
    participant C as PostDetailPage
    participant API as API Layer
    participant GW as API Gateway
    participant BS as blog-service

    U->>C: /:postId 방문
    C->>C: onMounted()

    par 병렬 API 호출
        C->>API: getPostById(postId)
        API->>GW: GET /posts/{postId}
        GW->>BS: Forward
        BS-->>C: PostResponse
    and
        C->>API: getCommentsByPostId(postId)
        API->>GW: GET /comments/post/{postId}
        GW->>BS: Forward
        BS-->>C: CommentResponse[]
    and
        C->>API: getLikeStatus(postId)
        API->>GW: GET /posts/{postId}/like
        GW->>BS: Forward
        BS-->>C: LikeStatusResponse
    and
        C->>API: getSeriesByPostId(postId)
        API->>GW: GET /series/by-post/{postId}
        GW->>BS: Forward
        BS-->>C: SeriesListResponse[]
    and
        C->>API: getPostNavigation(postId)
        API->>GW: GET /posts/{postId}/navigation
        GW->>BS: Forward
        BS-->>C: PostNavigationResponse
    and
        C->>API: getRelatedPosts(postId)
        API->>GW: GET /posts/{postId}/related
        GW->>BS: Forward
        BS-->>C: PostSummaryResponse[]
    end

    C->>C: 모든 데이터로 UI 렌더링
    C->>U: 게시물 상세 페이지 표시
```

### 3. 좋아요 토글 (낙관적 업데이트)

```mermaid
sequenceDiagram
    participant U as User
    participant LB as LikeButton
    participant API as toggleLike()
    participant GW as API Gateway
    participant BS as blog-service

    U->>LB: 좋아요 버튼 클릭
    LB->>LB: UI 낙관적 업데이트<br/>(liked 토글, count ±1)
    LB->>API: toggleLike(postId)
    API->>GW: POST /posts/{postId}/like
    GW->>BS: Forward + JWT
    BS->>BS: 좋아요 토글 처리
    BS-->>GW: LikeToggleResponse
    GW-->>API: Response
    API-->>LB: { liked, likeCount }
    LB->>LB: 서버 응답으로 상태 확정
    LB->>U: 최종 UI 반영
```

### 4. 팔로우 흐름 (followStore + auth-service)

```mermaid
sequenceDiagram
    participant U as User
    participant FB as FollowButton
    participant FS as followStore
    participant API as follow.ts
    participant GW as API Gateway
    participant AUTH as auth-service
    participant DB as MySQL

    Note over FS: 초기: loadFollowingIds()
    FS->>API: getMyFollowingIds()
    API->>GW: GET /api/v1/users/me/following/ids
    GW->>AUTH: Forward + JWT
    AUTH->>DB: Query following IDs
    DB-->>AUTH: UUID[]
    AUTH-->>FS: { followingIds: [...] }
    FS->>FS: followingIds.value = response

    U->>FB: 팔로우 버튼 클릭
    FB->>FS: toggleFollow(username, targetUuid)
    FS->>API: toggleFollow(username)
    API->>GW: POST /api/v1/users/{username}/follow
    GW->>AUTH: Forward + JWT
    AUTH->>DB: Toggle follow record
    DB-->>AUTH: Updated
    AUTH-->>GW: FollowResponse
    GW-->>API: Response
    API-->>FS: { following, followerCount, followingCount }
    FS->>FS: followingIds 로컬 업데이트
    FS->>FS: 캐시 무효화 (해당 username)
    FS-->>FB: 상태 변경
    FB->>U: UI 업데이트
```

### 5. 사용자 블로그 (Cross-Service 패턴)

UserBlogPage는 **auth-service**(프로필)와 **blog-service**(게시물)를 동시에 호출합니다.

```mermaid
sequenceDiagram
    participant U as User
    participant C as UserBlogPage
    participant UAPI as users.ts
    participant PAPI as posts.ts
    participant GW as API Gateway
    participant AUTH as auth-service
    participant BS as blog-service

    U->>C: /@username 방문
    C->>C: onMounted()

    par Cross-Service 병렬 호출
        C->>UAPI: getPublicProfile(username)
        UAPI->>GW: GET /api/v1/users/username/{username}
        GW->>AUTH: Forward
        AUTH-->>C: UserProfileResponse (uuid, nickname, bio, followerCount...)
    and
        Note over C: profile.uuid를 받은 후
    end

    C->>PAPI: getPostsByAuthor(profile.uuid)
    PAPI->>GW: GET /api/v1/blog/posts/author/{authorId}
    GW->>BS: Forward
    BS-->>C: PageResponse<PostSummaryResponse>

    C->>U: 프로필 + 게시물 목록 렌더링
```

### 6. 피드 흐름 (followingIds + blog-service)

```mermaid
sequenceDiagram
    participant U as User
    participant C as PostListPage (Feed Mode)
    participant FS as followStore
    participant FAPI as follow.ts
    participant PAPI as posts.ts
    participant GW as API Gateway
    participant AUTH as auth-service
    participant BS as blog-service

    U->>C: 피드 탭 클릭
    C->>FS: loadFollowingIds()
    FS->>FAPI: getMyFollowingIds()
    FAPI->>GW: GET /api/v1/users/me/following/ids
    GW->>AUTH: Forward + JWT
    AUTH-->>FS: { followingIds: [uuid1, uuid2, ...] }

    C->>PAPI: getFeed(followingIds, page, size)
    PAPI->>GW: GET /api/v1/blog/posts/feed?followingIds=...
    GW->>BS: Forward
    BS-->>C: PageResponse<PostSummaryResponse>

    C->>U: 팔로잉 사용자 게시물 목록 렌더링
```

### 7. 시리즈 관리 흐름

```mermaid
sequenceDiagram
    participant U as User
    participant C as MySeriesList
    participant API as series.ts
    participant GW as API Gateway
    participant BS as blog-service

    U->>C: MyPage의 시리즈 탭
    C->>API: getMySeries()
    API->>GW: GET /api/v1/blog/series/my
    GW->>BS: Forward + JWT
    BS-->>C: SeriesListResponse[]
    C->>U: 시리즈 목록 표시

    U->>C: 새 시리즈 생성
    C->>API: createSeries({ name, description })
    API->>GW: POST /api/v1/blog/series
    GW->>BS: Forward + JWT
    BS-->>C: SeriesResponse
    C->>U: 목록에 추가

    U->>C: 포스트 순서 변경 (드래그)
    C->>API: reorderSeriesPosts(seriesId, [postId1, postId2, ...])
    API->>GW: PUT /api/v1/blog/series/{id}/posts/order
    GW->>BS: Forward + JWT
    BS-->>C: SeriesResponse (순서 업데이트됨)
```

### 8. 고급 검색 흐름

```mermaid
sequenceDiagram
    participant U as User
    participant C as AdvancedSearchPage
    participant API as searchPostsAdvanced()
    participant GW as API Gateway
    participant BS as blog-service

    U->>C: 검색 조건 입력
    Note over C: keyword, category,<br/>tags[], authorId,<br/>startDate, endDate

    U->>C: 검색 버튼 클릭
    C->>API: searchPostsAdvanced(searchRequest)
    API->>GW: POST /api/v1/blog/posts/search/advanced
    Note over API: Request Body: PostSearchRequest
    GW->>BS: Forward
    BS->>BS: 복합 쿼리 실행
    BS-->>GW: PageResponse<PostSummaryResponse>
    GW-->>API: Response
    API-->>C: 검색 결과
    C->>U: 결과 렌더링 (PostCard[])
```

### 9. 게시물 작성/수정

```mermaid
sequenceDiagram
    participant U as User
    participant W as PostWritePage
    participant E as ToastUI Editor
    participant A as API Layer
    participant GW as API Gateway
    participant BS as blog-service
    participant S3 as S3

    U->>W: /write 방문
    W->>E: Initialize editor

    U->>E: 이미지 업로드
    E->>A: uploadFile(file)
    A->>GW: POST /api/v1/blog/file/upload (multipart)
    GW->>BS: Forward
    BS->>S3: Upload to S3
    S3-->>BS: File URL
    BS-->>A: FileUploadResponse
    A-->>E: Insert image URL

    U->>E: "저장" 클릭
    E->>W: Get markdown content
    W->>A: createPost({ title, content, tags, category, ... })
    A->>GW: POST /api/v1/blog/posts + JWT
    GW->>BS: Forward
    BS-->>A: PostResponse
    A-->>W: Created post
    W->>W: router.push(`/${response.id}`)
    W->>U: 상세 페이지로 이동
```

---

## 인증 토큰 흐름

```mermaid
sequenceDiagram
    participant PS as Portal Shell
    participant BS as Blog Frontend<br/>bootstrap.ts
    participant UPA as usePortalAuth<br/>composable
    participant AA as authAdapter
    participant AC as apiClient
    participant API as API Functions
    participant GW as API Gateway

    Note over PS: 사용자 로그인 완료<br/>(OAuth2 + JWT 발급)
    PS->>PS: axios interceptor 설정<br/>Authorization: Bearer {JWT}
    PS->>BS: mountBlogApp(el, options)
    BS->>BS: import { apiClient } from 'portal/api'
    Note over BS: Module Federation을 통해<br/>Portal의 apiClient 참조
    BS->>UPA: import { authAdapter } from 'portal/stores'
    UPA->>AA: subscribe(callback)<br/>인증 상태 변경 감지
    Note over UPA: authAdapter.getState()를<br/>Vue reactive ref로 감싸기

    BS->>API: API 함수에서 apiClient 사용
    API->>AC: apiClient.get/post/put/delete
    AC->>AC: interceptor가 JWT 자동 첨부
    AC->>GW: HTTP Request + Authorization: Bearer {JWT}
    GW->>GW: JWT 검증 (Spring Security)

    alt JWT 유효
        GW-->>AC: 200 OK + 응답 데이터
    else JWT 만료/무효
        GW-->>AC: 401 Unauthorized
        AC->>PS: interceptor가 토큰 갱신 또는 로그인 리다이렉트
        PS->>AA: authAdapter.setState()<br/>인증 상태 업데이트
        AA->>UPA: callback 호출
        UPA->>UPA: Vue ref 업데이트
    end
```

---

## Pinia 상태 관리 흐름

### searchStore

```mermaid
graph TB
    subgraph "searchStore State"
        KW[keyword: string]
        RES[results: PostSummaryResponse[]]
        IS[isSearching: boolean]
        ERR[error: string | null]
        CP[currentPage: number]
        TP[totalPages: number]
        HM[hasMore: boolean]
    end

    subgraph "searchStore Actions"
        SEARCH[search<br/>keyword]
        LOAD[loadMore]
        CLEAR[clear]
    end

    subgraph "Components"
        PL[PostListPage]
        SB[SearchBar]
    end

    PL -->|watch| RES
    PL -->|watch| IS
    SB -->|@search| SEARCH
    SB -->|@clear| CLEAR
    PL -->|scroll to bottom| LOAD
```

**동작 흐름**:

1. **새 검색**: `search(keyword)` → `results = []`, `currentPage = 0` → API 호출 → `results = response.content`
2. **추가 로드**: `loadMore()` → API 호출 (currentPage + 1) → `results = [...results, ...new]`
3. **초기화**: `clear()` → 모든 상태 리셋

### followStore

```mermaid
graph TB
    subgraph "followStore State"
        FI[followingIds: string[]]
        FIL[followingIdsLoaded: boolean]
        LD[loading: boolean]
        ER[error: Error | null]
        FC[followersCache: Map]
        FGC[followingsCache: Map]
    end

    subgraph "followStore Getters"
        IF[isFollowing(uuid)]
        FCT[followingCount]
    end

    subgraph "followStore Actions"
        LFI[loadFollowingIds]
        TF[toggleFollow]
        GF[getFollowers]
        GFG[getFollowings]
        CFS[checkFollowStatus]
        CC[clearCache]
        RST[reset]
    end

    subgraph "Components"
        FB[FollowButton]
        FM[FollowerModal]
        UB[UserBlogPage]
    end

    FB -->|call| TF
    FB -->|read| IF
    FM -->|call| GF & GFG
    UB -->|call| LFI

    TF -->|update| FI
    TF -->|invalidate| FC & FGC
    GF -->|cache hit| FC
    GFG -->|cache hit| FGC
```

**캐시 전략**:
- `followersCache` / `followingsCache`: `Map<string, FollowListResponse>` (key: `{username}-{page}-{size}`)
- 캐시 히트 시 API 호출 없이 즉시 반환
- `toggleFollow()` 호출 시 해당 사용자의 캐시 무효화
- `reset()`: 로그아웃 시 전체 상태 + 캐시 초기화

---

## 에러 처리 흐름

### 컴포넌트 레벨 에러 처리

```typescript
const isLoading = ref(false);
const error = ref<string | null>(null);

async function loadPosts() {
  try {
    isLoading.value = true;
    error.value = null;
    const response = await getPublishedPosts(page, size);
    posts.value = response.content;
  } catch (err) {
    console.error('Failed to fetch posts:', err);
    error.value = '게시글 목록을 불러올 수 없습니다.';
  } finally {
    isLoading.value = false;
  }
}
```

### API Gateway 에러 응답

| HTTP Status | 설명 | 처리 |
|-------------|------|------|
| `401 Unauthorized` | JWT 만료 또는 무효 | Portal Shell interceptor가 토큰 갱신 또는 로그인 리다이렉트 |
| `403 Forbidden` | 권한 없음 | 에러 메시지 표시 |
| `404 Not Found` | 리소스 없음 | 에러 메시지 표시 |
| `500 Internal Server Error` | 서버 에러 | 에러 메시지 표시 |

---

## API 엔드포인트 맵핑

### blog-service 엔드포인트 (`/api/v1/blog/**`)

#### Posts API (posts.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `createPost()` | POST | `/api/v1/blog/posts` | 게시물 생성 |
| `updatePost()` | PUT | `/api/v1/blog/posts/{postId}` | 게시물 수정 |
| `deletePost()` | DELETE | `/api/v1/blog/posts/{postId}` | 게시물 삭제 |
| `getPostById()` | GET | `/api/v1/blog/posts/{postId}` | 게시물 상세 조회 |
| `getAllPosts()` | GET | `/api/v1/blog/posts/all` | 전체 게시물 (관리자용) |
| `getPublishedPosts()` | GET | `/api/v1/blog/posts?page&size` | 발행된 게시물 목록 |
| `getMyPosts()` | GET | `/api/v1/blog/posts/my?status&page&size` | 내 게시물 |
| `getPostsByAuthor()` | GET | `/api/v1/blog/posts/author/{authorId}?page&size` | 작성자별 게시물 |
| `getPostsByCategory()` | GET | `/api/v1/blog/posts/category/{category}?page&size` | 카테고리별 게시물 |
| `getPostsByTags()` | GET | `/api/v1/blog/posts/tags?tags&page&size` | 태그별 게시물 |
| `getPopularPosts()` | GET | `/api/v1/blog/posts/popular?page&size` | 인기 게시물 |
| `getTrendingPosts()` | GET | `/api/v1/blog/posts/trending?period&page&size` | 트렌딩 게시물 |
| `getRecentPosts()` | GET | `/api/v1/blog/posts/recent?limit` | 최근 게시물 |
| `getRelatedPosts()` | GET | `/api/v1/blog/posts/{postId}/related?limit` | 관련 게시물 |
| `getPostWithViewIncrement()` | GET | `/api/v1/blog/posts/{postId}/view` | 조회수 증가 + 조회 |
| `searchPosts()` | GET | `/api/v1/blog/posts/search?keyword&page&size` | 간단 검색 |
| `searchPostsAdvanced()` | POST | `/api/v1/blog/posts/search/advanced` | 고급 검색 |
| `changePostStatus()` | PATCH | `/api/v1/blog/posts/{postId}/status` | 상태 변경 |
| `getCategoryStats()` | GET | `/api/v1/blog/posts/stats/categories` | 카테고리 통계 |
| `getPopularTags()` | GET | `/api/v1/blog/posts/stats/tags?limit` | 인기 태그 통계 |
| `getAuthorStats()` | GET | `/api/v1/blog/posts/stats/author/{authorId}` | 작성자 통계 |
| `getBlogStats()` | GET | `/api/v1/blog/posts/stats/blog` | 블로그 전체 통계 |
| `getPostsByProductId()` | GET | `/api/v1/blog/posts/product/{productId}` | 상품별 게시물 |
| `getPostNavigation()` | GET | `/api/v1/blog/posts/{postId}/navigation?scope` | 이전/다음 포스트 |
| `getFeed()` | GET | `/api/v1/blog/posts/feed?followingIds&page&size` | 팔로잉 피드 |

#### Comments API (comments.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `getCommentsByPostId()` | GET | `/api/v1/blog/comments/post/{postId}` | 게시글별 댓글 조회 |
| `createComment()` | POST | `/api/v1/blog/comments` | 댓글 작성 |
| `updateComment()` | PUT | `/api/v1/blog/comments/{commentId}` | 댓글 수정 |
| `deleteComment()` | DELETE | `/api/v1/blog/comments/{commentId}` | 댓글 삭제 |

#### Likes API (likes.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `toggleLike()` | POST | `/api/v1/blog/posts/{postId}/like` | 좋아요 토글 |
| `getLikeStatus()` | GET | `/api/v1/blog/posts/{postId}/like` | 좋아요 상태 확인 |
| `getLikers()` | GET | `/api/v1/blog/posts/{postId}/likes?page&size` | 좋아요 사용자 목록 |

#### Series API (series.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `getSeriesList()` | GET | `/api/v1/blog/series` 또는 `/author/{authorId}` | 시리즈 목록 |
| `getSeriesById()` | GET | `/api/v1/blog/series/{seriesId}` | 시리즈 상세 |
| `getSeriesPosts()` | GET | `/api/v1/blog/series/{seriesId}/posts` | 시리즈 포스트 목록 |
| `getMySeries()` | GET | `/api/v1/blog/series/my` | 내 시리즈 목록 |
| `createSeries()` | POST | `/api/v1/blog/series` | 시리즈 생성 |
| `updateSeries()` | PUT | `/api/v1/blog/series/{seriesId}` | 시리즈 수정 |
| `deleteSeries()` | DELETE | `/api/v1/blog/series/{seriesId}` | 시리즈 삭제 |
| `reorderSeriesPosts()` | PUT | `/api/v1/blog/series/{seriesId}/posts/order` | 포스트 순서 변경 |
| `addPostToSeries()` | POST | `/api/v1/blog/series/{seriesId}/posts/{postId}` | 포스트 추가 |
| `removePostFromSeries()` | DELETE | `/api/v1/blog/series/{seriesId}/posts/{postId}` | 포스트 제거 |
| `getSeriesByPostId()` | GET | `/api/v1/blog/series/by-post/{postId}` | 포스트별 시리즈 |

#### Tags API (tags.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `getAllTags()` | GET | `/api/v1/blog/tags` | 전체 태그 목록 |
| `getTagById()` | GET | `/api/v1/blog/tags/{tagId}` | 태그 상세 |
| `getTagByName()` | GET | `/api/v1/blog/tags/{tagName}` | 태그명 조회 |
| `getPostsByTag()` | GET | `/api/v1/blog/posts/tags?tags&page&size` | 태그별 포스트 |
| `getPopularTags()` | GET | `/api/v1/blog/tags/popular?limit` | 인기 태그 |
| `searchTags()` | GET | `/api/v1/blog/tags/search?q&limit` | 태그 검색 |

#### Files API (files.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `uploadFile()` | POST | `/api/v1/blog/file/upload` | S3 파일 업로드 (multipart) |
| `deleteFile()` | DELETE | `/api/v1/blog/file/delete` | S3 파일 삭제 |

### auth-service 엔드포인트 (`/api/v1/users/**`) - Cross-Service

#### Follow API (follow.ts)

| Frontend 함수 | HTTP Method | 경로 | 설명 |
|---------------|-------------|------|------|
| `toggleFollow()` | POST | `/api/v1/users/{username}/follow` | 팔로우 토글 |
| `getFollowers()` | GET | `/api/v1/users/{username}/followers?page&size` | 팔로워 목록 |
| `getFollowings()` | GET | `/api/v1/users/{username}/following?page&size` | 팔로잉 목록 |
| `getFollowStatus()` | GET | `/api/v1/users/{username}/follow/status` | 팔로우 상태 확인 |
| `getMyFollowingIds()` | GET | `/api/v1/users/me/following/ids` | 내 팔로잉 UUID 목록 |

#### Users API (users.ts)

| Frontend 함수 | HTTP Method | 경로 | 대상 서비스 | 설명 |
|---------------|-------------|------|-------------|------|
| `getPublicProfile()` | GET | `/api/v1/users/username/{username}` | auth-service | 공개 프로필 조회 |
| `getMyProfile()` | GET | `/api/v1/users/me` | auth-service | 내 프로필 조회 |
| `updateProfile()` | PATCH | `/api/v1/users/me` | auth-service | 프로필 수정 |
| `setUsername()` | POST | `/api/v1/users/me/username` | auth-service | Username 설정 |
| `checkUsername()` | GET | `/api/v1/users/username/{username}/check` | auth-service | Username 중복 확인 |
| `getUserPosts()` | GET | `/api/v1/blog/posts/author/{authorId}` | **blog-service** | 사용자 게시물 |

> `users.ts`는 auth-service와 blog-service를 **모두** 호출하는 cross-service 모듈입니다.

---

## 최적화 및 모범 사례

### 1. 무한 스크롤 (Intersection Observer)

```typescript
const observer = new IntersectionObserver(
  (entries) => {
    if (entries[0].isIntersecting && canLoadMore.value) {
      loadMore();
    }
  },
  { rootMargin: '100px', threshold: 0.1 }
);
```

### 2. 낙관적 업데이트 (좋아요, 팔로우)

UI를 먼저 업데이트하고 API 응답으로 확정합니다. 실패 시 롤백합니다.

### 3. followStore 캐시

팔로워/팔로잉 목록을 Map으로 캐시하여 동일 요청 시 API 호출을 생략합니다.

### 4. 병렬 API 호출

PostDetailPage에서 관련 데이터(댓글, 좋아요, 시리즈, 네비게이션, 관련글)를 `Promise.all` 또는 개별 `await` 없이 병렬로 호출합니다.

---

## 관련 문서

- [System Overview](./system-overview.md)
- [Module Federation](./module-federation.md)
- [Blog Service Architecture](../blog-service/)
- [Auth Service Architecture](../auth-service/)

---

**최종 업데이트**: 2026-02-15
