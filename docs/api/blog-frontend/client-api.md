---
id: api-client
title: Blog Frontend API Client
type: api
status: current
version: v1
created: 2026-01-18
updated: 2026-02-06
author: Claude
tags: [api, axios, typescript, frontend]
related:
  - arch-data-flow
---

# Blog Frontend API Client

> blog-frontend에서 사용하는 axios 기반 API 클라이언트 명세서

---

## 📋 개요

| 항목 | 내용 |
|------|------|
| **Base URL** | `http://localhost:8080/api/v1/blog` |
| **인증** | Bearer Token (자동 첨부) |
| **API Client** | `portal-shell`의 `apiClient` 공유 |
| **응답 형식** | `ApiResponse<T>` 래퍼 |

### API 클라이언트 설정

```typescript
// blog-frontend/src/api/index.ts
import { apiClient } from 'portal/api';

export default apiClient;
```

**특징:**
- portal-shell이 제공하는 axios 인스턴스를 재사용
- JWT 토큰은 axios interceptor에서 자동으로 `Authorization: Bearer {token}` 헤더에 첨부
- 모든 요청은 API Gateway(`localhost:8080`)를 경유하여 `blog-service`로 라우팅

---

## 📑 API 모듈 구조

| 파일 | 설명 | 담당 엔티티 |
|------|------|-------------|
| `api/index.ts` | API 클라이언트 export | - |
| `api/posts.ts` | 게시물 관련 API | Post |
| `api/comments.ts` | 댓글 관련 API | Comment |
| `api/files.ts` | 파일 업로드/삭제 API | File (S3) |
| `api/likes.ts` | 좋아요 관련 API | Like |
| `api/series.ts` | 시리즈 관련 API | Series |
| `api/tags.ts` | 태그 관련 API | Tag |
| `api/follow.ts` | 팔로우 관련 API | Follow |
| `api/users.ts` | 사용자 관련 API | User |

---

## 🔹 Posts API

### 경로 상수
```typescript
const BASE_PATH = '/api/v1/blog/posts';
```

### API 목록

#### 1. 게시물 CRUD

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `createPost` | POST | `/posts` | 게시물 생성 |
| `updatePost` | PUT | `/posts/{id}` | 게시물 수정 |
| `deletePost` | DELETE | `/posts/{id}` | 게시물 삭제 |
| `getPostById` | GET | `/posts/{id}` | 게시물 상세 조회 |
| `getAllPosts` | GET | `/posts/all` | 전체 게시물 조회 (관리자) |

##### `createPost(payload: PostCreateRequest): Promise<PostResponse>`

**Request Body:**
```typescript
interface PostCreateRequest {
  title: string;                    // 제목 (필수)
  content: string;                  // 본문 (필수)
  summary?: string;                 // 요약
  tags?: string[];                  // 태그 배열
  category?: string;                // 카테고리
  metaDescription?: string;         // SEO 메타 설명
  thumbnailUrl?: string;            // 썸네일 URL
  publishImmediately?: boolean;     // 즉시 발행 여부
  images?: string[];                // 본문 이미지 URL 배열
  productId?: string;               // 연관 상품 ID
}
```

**Response:**
```typescript
interface PostResponse {
  id: string;
  title: string;
  content: string;
  summary: string;
  authorId: string;
  authorName: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  tags: string[];
  category: string;
  metaDescription: string;
  thumbnailUrl: string;
  images: string[];
  viewCount: number;
  likeCount: number;
  createdAt: string;      // ISO-8601 형식
  updatedAt: string;
  publishedAt: string;
  productId: string;
}
```

**사용 예시:**
```typescript
const newPost = await createPost({
  title: 'Vue 3 Composition API 가이드',
  content: '# 본문 내용...',
  summary: 'Vue 3의 Composition API를 상세히 알아봅니다',
  tags: ['vue', 'javascript'],
  category: 'Frontend',
  publishImmediately: true,
});
```

##### `updatePost(postId: string, payload: PostUpdateRequest): Promise<PostResponse>`

**Request Body:**
```typescript
interface PostUpdateRequest {
  title: string;
  content: string;
  summary?: string;
  tags?: string[];
  category?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
  images?: string[];
}
```

##### `deletePost(postId: string): Promise<void>`

**사용 예시:**
```typescript
await deletePost('post-123');
```

##### `getPostById(postId: string): Promise<PostResponse>`

**사용 예시:**
```typescript
const post = await getPostById('post-123');
console.log(post.title, post.viewCount);
```

---

#### 2. 게시물 목록 조회

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getPublishedPosts` | GET | `/posts?page={n}&size={m}` | 발행된 게시물 목록 (페이징) |
| `getMyPosts` | GET | `/posts/my?status={s}` | 내 게시물 조회 |
| `getPostsByAuthor` | GET | `/posts/author/{id}` | 작성자별 게시물 |
| `getPostsByCategory` | GET | `/posts/category/{cat}` | 카테고리별 게시물 |
| `getPostsByTags` | GET | `/posts/tags?tags={t1,t2}` | 태그별 게시물 |
| `getPopularPosts` | GET | `/posts/popular` | 인기 게시물 |
| `getTrendingPosts` | GET | `/posts/trending?period={p}` | 트렌딩 게시물 (기간별) |
| `getRecentPosts` | GET | `/posts/recent?limit={n}` | 최근 게시물 |
| `getRelatedPosts` | GET | `/posts/{id}/related?limit={n}` | 관련 게시물 |
| `getPostWithViewIncrement` | GET | `/posts/{id}/view` | 조회수 증가 + 조회 |
| `getPostNavigation` | GET | `/posts/{id}/navigation?scope={s}` | 이전/다음 포스트 |
| `getFeed` | GET | `/posts/feed?followingIds={ids}` | 팔로잉 피드 |

##### `getPublishedPosts(page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**Query Parameters:**
- `page` (number, optional): 페이지 번호 (기본값: 0)
- `size` (number, optional): 페이지 크기 (기본값: 10)

**Response:**
```typescript
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  // ...
}

interface PostSummaryResponse {
  id: string;
  title: string;
  summary: string;
  authorId: string;
  authorName: string;
  tags: string[];
  category: string;
  thumbnailUrl: string;
  images: string[];
  viewCount: number;
  likeCount: number;
  publishedAt: string;
  estimatedReadTime: number;
}
```

**사용 예시:**
```typescript
const pageData = await getPublishedPosts(0, 20);
console.log(`Total: ${pageData.totalElements} posts`);
pageData.content.forEach(post => {
  console.log(post.title, post.authorName);
});
```

##### `getMyPosts(status?: string, page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**Query Parameters:**
- `status` (string, optional): 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
- `page`, `size`: 페이징

**사용 예시:**
```typescript
// 내 DRAFT 게시물만 조회
const draftPosts = await getMyPosts('DRAFT', 0, 10);
```

##### `getPostsByTags(tags: string[], page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**사용 예시:**
```typescript
const vuePosts = await getPostsByTags(['vue', 'typescript'], 0, 10);
```

##### `getRecentPosts(limit?: number): Promise<PostSummaryResponse[]>`

**Query Parameters:**
- `limit` (number, optional): 조회할 게시물 수 (기본값: 5)

**사용 예시:**
```typescript
const recent = await getRecentPosts(5);
// 메인 페이지 사이드바에 최근 글 표시
```

##### `getRelatedPosts(postId: string, limit?: number): Promise<PostSummaryResponse[]>`

**사용 예시:**
```typescript
const related = await getRelatedPosts('post-123', 5);
// 게시물 하단에 관련 글 추천 표시
```

##### `getTrendingPosts(period?: 'today' | 'week' | 'month' | 'year', page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**Query Parameters:**
- `period` (string, optional): 트렌딩 기간 ('today' | 'week' | 'month' | 'year', 기본값: 'week')
- `page`, `size`: 페이징

**사용 예시:**
```typescript
// 이번 주 트렌딩 게시물
const trending = await getTrendingPosts('week', 0, 10);

// 오늘의 인기 게시물
const today = await getTrendingPosts('today', 0, 5);
```

##### `getPostWithViewIncrement(postId: string): Promise<PostResponse>`

**설명:**
- 게시물을 조회하면서 동시에 조회수를 1 증가시킵니다.
- 게시물 상세 페이지에서 사용

**사용 예시:**
```typescript
const post = await getPostWithViewIncrement('post-123');
console.log(`조회수: ${post.viewCount}`);
```

##### `getPostNavigation(postId: string, scope?: 'all' | 'author' | 'category' | 'series'): Promise<PostNavigationResponse>`

**Query Parameters:**
- `scope` (string, optional): 네비게이션 범위 (기본값: 'all')
  - `'all'`: 전체 게시물 기준
  - `'author'`: 같은 작성자의 게시물 기준
  - `'category'`: 같은 카테고리의 게시물 기준
  - `'series'`: 같은 시리즈의 게시물 기준

**Response:**
```typescript
interface PostNavigationResponse {
  previousPost?: PostNavigationItem;
  nextPost?: PostNavigationItem;
  scope: 'all' | 'author' | 'category' | 'series';
}

interface PostNavigationItem {
  id: string;
  title: string;
  thumbnailUrl?: string;
  publishedAt: string;
}
```

**사용 예시:**
```typescript
// 이전/다음 게시물 조회
const nav = await getPostNavigation('post-123', 'category');
console.log('이전 글:', nav.previousPost?.title);
console.log('다음 글:', nav.nextPost?.title);

// 게시물 상세 페이지에서 이전/다음 버튼 구현
```

##### `getFeed(followingIds: string[], page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**Query Parameters:**
- `followingIds` (string[]): 팔로잉 사용자 UUID 목록
- `page`, `size`: 페이징

**사용 예시:**
```typescript
// 내가 팔로우하는 사용자들의 게시물 조회
const followingIds = await getMyFollowingIds();
const feed = await getFeed(followingIds.followingIds, 0, 20);

// 피드 페이지에서 사용
```

---

#### 3. 검색

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `searchPosts` | GET | `/posts/search?keyword={k}` | 간단 검색 |
| `searchPostsAdvanced` | POST | `/posts/search/advanced` | 고급 검색 (필터) |

##### `searchPosts(keyword: string, page?: number, size?: number): Promise<PageResponse<PostSummaryResponse>>`

**Query Parameters:**
- `keyword` (string): 검색 키워드 (제목, 본문 검색)
- `page`, `size`: 페이징

**사용 예시:**
```typescript
const results = await searchPosts('Vue 3', 0, 10);
```

##### `searchPostsAdvanced(searchRequest: PostSearchRequest): Promise<PageResponse<PostSummaryResponse>>`

**Request Body:**
```typescript
interface PostSearchRequest {
  keyword?: string;       // 키워드
  category?: string;      // 카테고리 필터
  tags?: string[];        // 태그 필터
  authorId?: string;      // 작성자 필터
  startDate?: string;     // 시작 날짜 (ISO-8601)
  endDate?: string;       // 종료 날짜
  page?: number;
  size?: number;
}
```

**사용 예시:**
```typescript
const results = await searchPostsAdvanced({
  keyword: 'Vue',
  category: 'Frontend',
  tags: ['typescript'],
  startDate: '2026-01-01T00:00:00Z',
  endDate: '2026-12-31T23:59:59Z',
  page: 0,
  size: 20,
});
```

---

#### 4. 상태 관리

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `changePostStatus` | PATCH | `/posts/{id}/status` | 게시물 상태 변경 |

##### `changePostStatus(postId: string, request: PostStatusChangeRequest): Promise<PostResponse>`

**Request Body:**
```typescript
interface PostStatusChangeRequest {
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
}
```

**사용 예시:**
```typescript
// DRAFT → PUBLISHED로 변경 (게시물 발행)
const published = await changePostStatus('post-123', { status: 'PUBLISHED' });

// PUBLISHED → ARCHIVED로 변경 (게시물 보관)
const archived = await changePostStatus('post-123', { status: 'ARCHIVED' });
```

---

#### 5. 통계

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getCategoryStats` | GET | `/posts/stats/categories` | 카테고리별 게시물 수 |
| `getPopularTags` | GET | `/posts/stats/tags?limit={n}` | 인기 태그 |
| `getAuthorStats` | GET | `/posts/stats/author/{id}` | 작성자 통계 |
| `getBlogStats` | GET | `/posts/stats/blog` | 블로그 전체 통계 |

##### `getCategoryStats(): Promise<CategoryStats[]>`

**Response:**
```typescript
interface CategoryStats {
  category: string;
  count: number;
}
```

**사용 예시:**
```typescript
const stats = await getCategoryStats();
// [{ category: 'Frontend', count: 120 }, { category: 'Backend', count: 80 }]
```

##### `getPopularTags(limit?: number): Promise<TagStats[]>`

**Response:**
```typescript
interface TagStats {
  tag: string;
  count: number;
}
```

**사용 예시:**
```typescript
const tags = await getPopularTags(10);
// 사이드바에 인기 태그 표시
```

##### `getAuthorStats(authorId: string): Promise<AuthorStats>`

**Response:**
```typescript
interface AuthorStats {
  authorId: string;
  authorName: string;
  postCount: number;
  totalViews: number;
  totalLikes: number;
}
```

**사용 예시:**
```typescript
const stats = await getAuthorStats('user-123');
console.log(`${stats.authorName}: 게시물 ${stats.postCount}개, 조회수 ${stats.totalViews}`);
```

##### `getBlogStats(): Promise<BlogStats>`

**Response:**
```typescript
interface BlogStats {
  totalPosts: number;
  totalViews: number;
  totalLikes: number;
  totalComments: number;
}
```

**사용 예시:**
```typescript
const stats = await getBlogStats();
// 대시보드에 전체 통계 표시
```

---

#### 6. 기타

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getPostsByProductId` | GET | `/posts/product/{id}` | 상품별 게시물 조회 |

##### `getPostsByProductId(productId: string): Promise<PostResponse[]>`

**설명:**
- shopping-service와 연동하여 특정 상품과 관련된 게시물을 조회

**사용 예시:**
```typescript
const productPosts = await getPostsByProductId('product-456');
// 상품 상세 페이지에서 관련 리뷰/소개 글 표시
```

---

## 🔹 Comments API

### 경로 상수
```typescript
const BASE_PATH = '/api/v1/blog/comments';
```

### API 목록

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getCommentsByPostId` | GET | `/comments/post/{postId}` | 게시물의 모든 댓글 조회 |
| `createComment` | POST | `/comments` | 댓글 작성 |
| `updateComment` | PUT | `/comments/{id}` | 댓글 수정 |
| `deleteComment` | DELETE | `/comments/{id}` | 댓글 삭제 |

#### DTO 타입

```typescript
interface CommentResponse {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId: string | null;  // 대댓글인 경우 부모 댓글 ID
  likeCount: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

interface CommentCreateRequest {
  postId: string;
  parentCommentId?: string | null;  // 대댓글인 경우 지정
  content: string;
}

interface CommentUpdateRequest {
  content: string;
}
```

#### 사용 예시

```typescript
// 게시물의 모든 댓글 조회
const comments = await getCommentsByPostId('post-123');
console.log(`댓글 ${comments.length}개`);

// 댓글 작성
const newComment = await createComment({
  postId: 'post-123',
  content: '좋은 글 감사합니다!',
});

// 대댓글 작성
const reply = await createComment({
  postId: 'post-123',
  parentCommentId: 'comment-456',
  content: '동의합니다!',
});

// 댓글 수정
const updated = await updateComment('comment-456', {
  content: '수정된 내용입니다.',
});

// 댓글 삭제
await deleteComment('comment-456');
```

---

## 🔹 Files API

### 경로
- Upload: `/api/v1/blog/file/upload`
- Delete: `/api/v1/blog/file/delete`

### API 목록

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `uploadFile` | POST | `/file/upload` | S3에 파일 업로드 |
| `deleteFile` | DELETE | `/file/delete` | S3에서 파일 삭제 (ADMIN) |

#### DTO 타입

```typescript
interface FileUploadResponse {
  url: string;          // S3 업로드 URL
  filename: string;     // 원본 파일명
  size: number;         // 파일 크기 (bytes)
  contentType: string;  // MIME 타입 (예: image/jpeg)
}

interface FileDeleteRequest {
  url: string;  // 삭제할 파일의 S3 URL
}
```

#### 사용 예시

```typescript
// 파일 업로드
const handleFileUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (file) {
    const response = await uploadFile(file);
    console.log('업로드 완료:', response.url);
    console.log('파일명:', response.filename);
    console.log('크기:', response.size, 'bytes');
  }
};

// 파일 삭제 (ADMIN 권한 필요)
await deleteFile('http://localhost:4566/blog-bucket/abc123_image.jpg');
```

**주의사항:**
- 업로드 시 `multipart/form-data` Content-Type이 자동 설정됨
- 파일 삭제는 ADMIN 권한이 필요함
- 로컬 개발 환경에서는 LocalStack S3를 사용

---

## 🔹 Likes API

### 경로 상수
```typescript
const BASE_PATH = '/api/v1/blog/posts';
```

### API 목록

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `toggleLike` | POST | `/posts/{postId}/like` | 좋아요 토글 (추가/취소) |
| `getLikeStatus` | GET | `/posts/{postId}/like` | 좋아요 상태 확인 |
| `getLikers` | GET | `/posts/{postId}/likes` | 좋아요한 사용자 목록 |

#### DTO 타입

```typescript
interface LikeToggleResponse {
  postId: string;
  userId: string;
  liked: boolean;      // 좋아요 상태 (true: 추가됨, false: 취소됨)
  likeCount: number;   // 현재 좋아요 수
  timestamp: string;
}

interface LikeStatusResponse {
  postId: string;
  userId: string;
  liked: boolean;
  likeCount: number;
}

interface LikerResponse {
  userId: string;
  username: string;
  profileImageUrl?: string;
  likedAt: string;
}
```

#### 사용 예시

```typescript
// 좋아요 토글
const handleLike = async (postId: string) => {
  const result = await toggleLike(postId);

  if (result.liked) {
    console.log('좋아요 추가됨');
  } else {
    console.log('좋아요 취소됨');
  }

  console.log(`현재 좋아요 수: ${result.likeCount}`);
};

// 좋아요 상태 확인
const status = await getLikeStatus('post-123');
if (status.liked) {
  // 좋아요 버튼 활성화 상태로 표시
}

// 좋아요한 사용자 목록 조회 (페이징)
const likers = await getLikers('post-123', 0, 20);
console.log(`${likers.totalElements}명이 좋아요를 눌렀습니다`);
likers.content.forEach(liker => {
  console.log(`${liker.username}님이 ${liker.likedAt}에 좋아요`);
});
```

---

## 🔹 Series API

### 경로 상수
```typescript
const BASE_PATH = '/api/v1/blog/series';
```

### API 목록

#### 1. 시리즈 조회

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getSeriesList` | GET | `/series` 또는 `/series/author/{id}` | 시리즈 목록 조회 |
| `getSeriesById` | GET | `/series/{id}` | 시리즈 상세 조회 |
| `getSeriesPosts` | GET | `/series/{id}/posts` | 시리즈의 포스트 목록 |
| `getMySeries` | GET | `/series/my` | 내 시리즈 목록 |
| `getSeriesByPostId` | GET | `/series/by-post/{postId}` | 특정 포스트가 속한 시리즈 |

#### 2. 시리즈 관리 (작성자용)

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `createSeries` | POST | `/series` | 시리즈 생성 |
| `updateSeries` | PUT | `/series/{id}` | 시리즈 수정 |
| `deleteSeries` | DELETE | `/series/{id}` | 시리즈 삭제 |
| `addPostToSeries` | POST | `/series/{id}/posts/{postId}` | 포스트 추가 |
| `removePostFromSeries` | DELETE | `/series/{id}/posts/{postId}` | 포스트 제거 |
| `reorderSeriesPosts` | PUT | `/series/{id}/posts/order` | 포스트 순서 변경 |

#### DTO 타입

```typescript
interface SeriesResponse {
  id: string;
  name: string;
  description: string;
  authorId: string;
  authorName: string;
  thumbnailUrl: string;
  postIds: string[];      // 순서대로 정렬된 포스트 ID 배열
  postCount: number;
  createdAt: string;
  updatedAt: string;
}

interface SeriesListResponse {
  id: string;
  name: string;
  description: string;
  authorName: string;
  thumbnailUrl: string;
  postCount: number;
  updatedAt: string;
}

interface SeriesCreateRequest {
  name: string;
  description?: string;
  thumbnailUrl?: string;
}

interface SeriesUpdateRequest {
  name: string;
  description?: string;
  thumbnailUrl?: string;
}
```

#### 사용 예시

```typescript
// 시리즈 목록 조회 (전체)
const allSeries = await getSeriesList();

// 특정 작성자의 시리즈 조회
const authorSeries = await getSeriesList('author-123');

// 시리즈 상세 조회
const series = await getSeriesById('series-456');
console.log(`${series.name}: ${series.postCount}개 포스트`);

// 시리즈의 포스트 목록
const posts = await getSeriesPosts('series-456');
// 순서대로 정렬된 포스트 목록

// 시리즈 생성
const newSeries = await createSeries({
  name: 'Vue 3 완전 정복',
  description: 'Vue 3를 처음부터 끝까지 배우는 시리즈',
  thumbnailUrl: 'https://...',
});

// 시리즈에 포스트 추가
await addPostToSeries('series-456', 'post-123');

// 시리즈 포스트 순서 변경
await reorderSeriesPosts('series-456', [
  'post-001',
  'post-003',
  'post-002',
]);

// 특정 포스트가 속한 시리즈 조회
const relatedSeries = await getSeriesByPostId('post-123');
```

---

## 🔹 Tags API

### 경로 상수
```typescript
const BASE_PATH = '/api/v1/blog/tags';
```

### API 목록

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getAllTags` | GET | `/tags` | 전체 태그 목록 |
| `getTagById` | GET | `/tags/{id}` | 태그 상세 조회 (ID) |
| `getTagByName` | GET | `/tags/{name}` | 태그 상세 조회 (이름) |
| `getPostsByTag` | GET | `/posts/tags?tags={name}` | 태그로 포스트 검색 |
| `getPopularTags` | GET | `/tags/popular?limit={n}` | 인기 태그 |
| `searchTags` | GET | `/tags/search?q={keyword}` | 태그 검색 |

#### DTO 타입

```typescript
interface TagResponse {
  id: string;
  name: string;
  postCount: number;
  description: string;
  createdAt: string;
  lastUsedAt: string;
}

interface TagStatsResponse {
  name: string;
  postCount: number;
  totalViews: number | null;
}
```

#### 사용 예시

```typescript
// 전체 태그 목록 조회
const allTags = await getAllTags();

// 태그로 포스트 검색
const vuePosts = await getPostsByTag('vue', 0, 10);

// 인기 태그 (사이드바에 표시)
const popularTags = await getPopularTags(20);
popularTags.forEach(tag => {
  console.log(`#${tag.name} (${tag.postCount}개 포스트)`);
});

// 태그 자동완성용 검색
const handleTagSearch = async (keyword: string) => {
  const tags = await searchTags(keyword, 5);
  return tags.map(t => t.name);
};

// 사용자가 "vue"를 입력하면 "vue", "vue3", "vuejs" 등 제안
const suggestions = await handleTagSearch('vue');
```

---

## 🔹 Follow API

### 경로 상수
```typescript
const AUTH_API_BASE = '/api/v1/users';
```

**참고:** Follow API는 auth-service를 호출합니다.

### API 목록

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `toggleFollow` | POST | `/users/{username}/follow` | 팔로우/언팔로우 토글 |
| `getFollowers` | GET | `/users/{username}/followers` | 팔로워 목록 |
| `getFollowings` | GET | `/users/{username}/following` | 팔로잉 목록 |
| `getFollowStatus` | GET | `/users/{username}/follow/status` | 팔로우 상태 확인 |
| `getMyFollowingIds` | GET | `/users/me/following/ids` | 내 팔로잉 ID 목록 |

#### DTO 타입

```typescript
interface FollowResponse {
  following: boolean;      // 팔로우 상태 (true: 팔로우됨, false: 언팔로우됨)
  followerCount: number;   // 대상 사용자의 팔로워 수
  followingCount: number;  // 대상 사용자의 팔로잉 수
}

interface FollowUserResponse {
  uuid: string;
  username: string | null;
  nickname: string;
  profileImageUrl: string | null;
  bio: string | null;
}

interface FollowListResponse {
  users: FollowUserResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

interface FollowStatusResponse {
  isFollowing: boolean;
}

interface FollowingIdsResponse {
  followingIds: string[];  // 팔로잉 사용자들의 UUID 배열
}
```

#### 사용 예시

```typescript
// 팔로우 토글
const handleFollow = async (username: string) => {
  const result = await toggleFollow(username);

  if (result.following) {
    console.log('팔로우했습니다');
  } else {
    console.log('언팔로우했습니다');
  }

  console.log(`팔로워: ${result.followerCount}명`);
};

// 팔로워 목록 조회
const followers = await getFollowers('john_doe', 0, 20);
followers.users.forEach(user => {
  console.log(`@${user.username}: ${user.bio}`);
});

// 팔로잉 목록 조회
const followings = await getFollowings('john_doe', 0, 20);

// 팔로우 상태 확인 (버튼 상태 결정)
const status = await getFollowStatus('john_doe');
if (status.isFollowing) {
  // "언팔로우" 버튼 표시
} else {
  // "팔로우" 버튼 표시
}

// 내 팔로잉 ID 목록 (피드 API 호출용)
const myFollowings = await getMyFollowingIds();
const feed = await getFeed(myFollowings.followingIds, 0, 20);
```

---

## 🔹 Users API

### 경로 상수
```typescript
const AUTH_API_BASE = '/api/v1/users';
const BLOG_API_BASE = '/api/v1/blog/posts';
```

**참고:** 프로필 관련 API는 auth-service, 게시글 조회는 blog-service를 호출합니다.

### API 목록

#### 1. 프로필 조회

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getPublicProfile` | GET | `/users/username/{username}` | 공개 프로필 조회 |
| `getMyProfile` | GET | `/users/me` | 내 프로필 조회 (인증 필요) |

#### 2. 프로필 수정

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `updateProfile` | PATCH | `/users/me` | 프로필 정보 수정 |
| `setUsername` | POST | `/users/me/username` | Username 설정 (최초 1회) |
| `checkUsername` | GET | `/users/username/{username}/check` | Username 중복 확인 |

#### 3. 사용자 게시글

| 함수명 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| `getUserPosts` | GET | `/posts/author/{authorId}` | 특정 사용자의 게시글 |

#### DTO 타입

```typescript
interface UserProfileResponse {
  id: number;
  uuid: string;
  email: string;
  nickname: string;
  username: string | null;  // 최초 설정 전에는 null
  bio: string | null;
  profileImageUrl: string | null;
  website: string | null;
  followerCount: number;
  followingCount: number;
  createdAt: string;
}

interface UserProfileUpdateRequest {
  name?: string;
  bio?: string;
  profileImageUrl?: string;
  website?: string;
}

interface UsernameSetRequest {
  username: string;
}

interface UsernameCheckResponse {
  username: string;
  available: boolean;
  message: string;
}
```

#### 사용 예시

```typescript
// 공개 프로필 조회 (username 기반)
const profile = await getPublicProfile('john_doe');
console.log(`${profile.nickname}님의 프로필`);
console.log(`팔로워: ${profile.followerCount}, 팔로잉: ${profile.followingCount}`);

// 내 프로필 조회
const myProfile = await getMyProfile();
if (!myProfile.username) {
  // Username 설정 유도
}

// 프로필 수정
await updateProfile({
  bio: '안녕하세요! Vue 개발자입니다.',
  website: 'https://myblog.com',
  profileImageUrl: 'https://...',
});

// Username 중복 확인
const checkResult = await checkUsername('john_doe');
if (checkResult.available) {
  // Username 설정 가능
  await setUsername('john_doe');
} else {
  console.error(checkResult.message);
  // "이미 사용 중인 username입니다"
}

// 사용자의 게시글 조회 (authorId 기반)
const userPosts = await getUserPosts(profile.uuid, 0, 10);
console.log(`${profile.nickname}님이 작성한 게시글 ${userPosts.totalElements}개`);
```

---

## 🔒 인증 (Authentication)

### JWT 토큰 자동 첨부

모든 API 요청은 portal-shell의 axios interceptor를 통해 JWT 토큰이 자동으로 첨부됩니다.

```typescript
// portal-shell/src/api/client.ts (참고)
apiClient.interceptors.request.use((config) => {
  const token = authStore.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

**사용자 입장에서는:**
- 토큰 관리를 신경 쓸 필요 없음
- 로그인 후 자동으로 인증된 요청이 전송됨
- 토큰 만료 시 자동으로 리프레시 또는 로그인 페이지로 리다이렉트

---

## ⚠️ 에러 처리

### 에러 응답 형식

```typescript
interface ErrorResponse {
  success: false;
  code: string;
  message: string;
  timestamp: string;
}
```

### 주요 에러 코드

| Code | HTTP Status | 설명 |
|------|-------------|------|
| `B001` | 404 | 게시물을 찾을 수 없음 |
| `B002` | 400 | 중복된 제목 |
| `B003` | 400 | 유효성 검증 실패 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류 |

### 에러 처리 예시

```typescript
try {
  const post = await getPostById('invalid-id');
} catch (error) {
  if (axios.isAxiosError(error)) {
    const errorResponse = error.response?.data as ErrorResponse;

    if (errorResponse.code === 'B001') {
      console.error('게시물을 찾을 수 없습니다');
    } else if (error.response?.status === 401) {
      console.error('로그인이 필요합니다');
      // 로그인 페이지로 리다이렉트
    } else {
      console.error('오류 발생:', errorResponse.message);
    }
  }
}
```

---

## 📊 응답 형식

### 성공 응답

```typescript
interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: any;
  message?: string;
  timestamp?: string;
}
```

**예시:**
```json
{
  "success": true,
  "data": {
    "id": "post-123",
    "title": "Vue 3 가이드",
    "content": "...",
    "viewCount": 1234
  },
  "timestamp": "2026-01-18T10:30:00Z"
}
```

### 페이징 응답

```typescript
interface PageResponse<T> {
  content: T[];           // 실제 데이터 배열
  totalElements: number;  // 전체 요소 수
  totalPages: number;     // 전체 페이지 수
  size: number;           // 페이지 크기
  number: number;         // 현재 페이지 번호 (0부터 시작)
  first: boolean;         // 첫 페이지 여부
  last: boolean;          // 마지막 페이지 여부
  empty: boolean;         // 비어있는지 여부
  // ... 기타 pageable 정보
}
```

**예시:**
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 250,
    "totalPages": 25,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

---

## 💡 사용 가이드

### 1. 기본 CRUD 패턴

```typescript
// 생성
const newPost = await createPost({ title: '...', content: '...' });

// 조회
const post = await getPostById(newPost.id);

// 수정
const updated = await updatePost(post.id, { title: 'Updated Title', content: '...' });

// 삭제
await deletePost(post.id);
```

### 2. 페이징 처리

```typescript
const loadPosts = async (page: number) => {
  const pageData = await getPublishedPosts(page, 20);

  posts.value = pageData.content;
  currentPage.value = pageData.number;
  totalPages.value = pageData.totalPages;
  hasMore.value = !pageData.last;
};
```

### 3. 무한 스크롤

```typescript
const posts = ref<PostSummaryResponse[]>([]);
let currentPage = 0;

const loadMore = async () => {
  const pageData = await getPublishedPosts(currentPage, 10);
  posts.value.push(...pageData.content);

  currentPage++;
  if (pageData.last) {
    // 더 이상 로드할 게시물 없음
  }
};
```

### 4. 검색 필터

```typescript
const searchPosts = async (filters: {
  keyword?: string;
  category?: string;
  tags?: string[];
}) => {
  const results = await searchPostsAdvanced({
    keyword: filters.keyword,
    category: filters.category,
    tags: filters.tags,
    page: 0,
    size: 20,
  });

  return results.content;
};
```

### 5. 댓글 트리 구성

```typescript
const buildCommentTree = (comments: CommentResponse[]) => {
  const rootComments = comments.filter(c => c.parentCommentId === null);

  return rootComments.map(root => ({
    ...root,
    replies: comments.filter(c => c.parentCommentId === root.id),
  }));
};

// 사용
const allComments = await getCommentsByPostId('post-123');
const commentTree = buildCommentTree(allComments);
```

### 6. 파일 업로드 + 게시물 생성

```typescript
const createPostWithImage = async (
  postData: PostCreateRequest,
  thumbnailFile: File
) => {
  // 1. 썸네일 업로드
  const uploaded = await uploadFile(thumbnailFile);

  // 2. 게시물 생성 (썸네일 URL 포함)
  const post = await createPost({
    ...postData,
    thumbnailUrl: uploaded.url,
  });

  return post;
};
```

---

## 🔗 관련 문서

- [아키텍처: 데이터 흐름](../../architecture/) <!-- TODO: verify data flow architecture location -->
- [Backend: Blog Service API 명세](../../api/) <!-- TODO: verify blog service API location -->
- [Design System: API 클라이언트 패턴](../../api/) <!-- TODO: verify design system API location -->

---

**최종 업데이트**: 2026-02-06
