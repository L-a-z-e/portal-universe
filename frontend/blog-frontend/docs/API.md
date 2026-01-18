# API Client Guide

## 개요

Blog Frontend의 API 통신은 Portal Shell에서 제공하는 공유 `apiClient` (axios 인스턴스)를 사용합니다.

```typescript
// src/api/index.ts
import apiClient from 'portal/apiClient';
export default apiClient;
```

## 기본 설정

### API Client 특징

- **Base URL**: `/api`
- **인증**: JWT 토큰 자동 포함 (Portal Shell의 authStore)
- **CORS**: Portal Shell의 설정을 따름
- **CSRF**: API Gateway에서 처리
- **Timeout**: 30초 (기본값)

### 요청/응답 구조

```typescript
// 요청
GET /api/blog/posts?page=0&size=10

// 응답 (ApiResponse 래핑)
{
  "success": true,
  "code": "SUCCESS",
  "message": "성공",
  "data": {
    "content": [/* PostSummaryResponse[] */],
    "number": 0,
    "size": 10,
    "totalElements": 45,
    "totalPages": 5,
    "last": false
  }
}
```

## API 엔드포인트

### Posts API (`src/api/posts.ts`)

#### 기본 CRUD

```typescript
// 게시물 생성
export function createPost(payload: PostCreateRequest): Promise<PostResponse>

// 요청
const post = await createPost({
  title: '새 글',
  content: '# 내용',
  category: 'tech',
  tags: ['react', 'vue'],
  status: 'DRAFT'
});
```

```typescript
// 게시물 조회 (상세)
export function getPostById(postId: string): Promise<PostResponse>

// 요청
const post = await getPostById('123');
```

```typescript
// 게시물 수정
export function updatePost(
  postId: string,
  payload: PostUpdateRequest
): Promise<PostResponse>

// 요청
const updated = await updatePost('123', {
  title: '수정된 제목',
  content: '수정된 내용'
});
```

```typescript
// 게시물 삭제
export function deletePost(postId: string): Promise<void>

// 요청
await deletePost('123');
```

#### 목록 조회

```typescript
// 발행된 게시물 (페이징)
export async function getPublishedPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const page1 = await getPublishedPosts(0, 10);
const page2 = await getPublishedPosts(1, 10);

// 응답
{
  content: [/* 게시글 배열 */],
  number: 0,        // 현재 페이지
  size: 10,         // 페이지 크기
  totalElements: 45,
  totalPages: 5,
  last: false       // 마지막 페이지 여부
}
```

```typescript
// 내 게시물
export async function getMyPosts(
  status?: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const myPosts = await getMyPosts('PUBLISHED', 0, 10);
```

```typescript
// 작성자별 게시물
export async function getPostsByAuthor(
  authorId: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const authorPosts = await getPostsByAuthor('user-123', 0, 10);
```

```typescript
// 카테고리별 게시물
export async function getPostsByCategory(
  category: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const techPosts = await getPostsByCategory('tech', 0, 10);
```

```typescript
// 태그별 게시물
export async function getPostsByTags(
  tags: string[],
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const reactPosts = await getPostsByTags(['react', 'vue'], 0, 10);
```

```typescript
// 인기 게시물
export async function getPopularPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const popular = await getPopularPosts(0, 10);
```

```typescript
// 최근 게시물
export async function getRecentPosts(
  limit: number = 5
): Promise<PostSummaryResponse[]>

// 요청
const recent = await getRecentPosts(5);
```

```typescript
// 관련 게시물
export async function getRelatedPosts(
  postId: string,
  limit: number = 5
): Promise<PostSummaryResponse[]>

// 요청
const related = await getRelatedPosts('123', 5);
```

#### 검색

```typescript
// 간단 검색
export async function searchPosts(
  keyword: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const results = await searchPosts('React', 0, 10);
```

```typescript
// 고급 검색
export async function searchPostsAdvanced(
  searchRequest: PostSearchRequest
): Promise<PageResponse<PostSummaryResponse>>

// 요청
const results = await searchPostsAdvanced({
  keyword: 'React',
  category: 'tech',
  tags: ['frontend'],
  author: 'user-123',
  startDate: '2024-01-01',
  endDate: '2024-12-31',
  page: 0,
  size: 10
});
```

#### 상태 관리

```typescript
// 게시물 상태 변경
export async function changePostStatus(
  postId: string,
  request: PostStatusChangeRequest
): Promise<PostResponse>

// 요청
const published = await changePostStatus('123', {
  status: 'PUBLISHED'
});
```

#### 통계

```typescript
// 카테고리 통계
export async function getCategoryStats(): Promise<CategoryStats[]>

// 응답
[
  { category: 'tech', count: 25 },
  { category: 'travel', count: 12 },
  ...
]
```

```typescript
// 인기 태그
export async function getPopularTags(
  limit: number = 10
): Promise<TagStats[]>

// 응답
[
  { tag: 'react', count: 15 },
  { tag: 'vue', count: 10 },
  ...
]
```

```typescript
// 작성자 통계
export async function getAuthorStats(
  authorId: string
): Promise<AuthorStats>

// 응답
{
  authorId: 'user-123',
  authorName: 'John Doe',
  postCount: 25,
  viewCount: 1500,
  likeCount: 250
}
```

```typescript
// 블로그 전체 통계
export async function getBlogStats(): Promise<BlogStats>

// 응답
{
  totalPosts: 125,
  totalViews: 50000,
  totalLikes: 5000,
  totalComments: 1200,
  totalAuthors: 45
}
```

### Comments API (`src/api/comments.ts`)

```typescript
// 게시물별 댓글 조회
export function getCommentsByPostId(
  postId: string
): Promise<CommentResponse[]>

// 댓글 작성
export function createComment(
  postId: string,
  payload: CommentCreateRequest
): Promise<CommentResponse>

// 댓글 수정
export function updateComment(
  commentId: string,
  payload: CommentUpdateRequest
): Promise<CommentResponse>

// 댓글 삭제
export function deleteComment(commentId: string): Promise<void>
```

### Files API (`src/api/files.ts`)

```typescript
// 파일 업로드
export function uploadFile(file: File): Promise<FileUploadResponse>

// 파일 삭제
export function deleteFile(fileId: string): Promise<void>
```

## 사용 예제

### 게시글 목록 페이지

```typescript
// src/views/PostListPage.vue
import { getPublishedPosts } from '@/api/posts';

const posts = ref([]);
const currentPage = ref(0);
const hasMore = ref(true);

async function loadPosts(page = 0) {
  try {
    const response = await getPublishedPosts(page, 10);
    posts.value = response.content;
    hasMore.value = !response.last;
  } catch (err) {
    console.error('Failed to fetch posts:', err);
  }
}

onMounted(() => {
  loadPosts(0);
});
```

### 검색 기능

```typescript
// src/stores/searchStore.ts
import { searchPosts } from '@/api/posts';

export const useSearchStore = defineStore('search', () => {
  const keyword = ref('');
  const results = ref([]);

  async function search(query: string) {
    try {
      const response = await searchPosts(query, 0, 10);
      results.value = response.content;
    } catch (err) {
      console.error('Search failed:', err);
    }
  }

  return { keyword, results, search };
});
```

### 게시글 작성

```typescript
// src/views/PostWritePage.vue
import { createPost } from '@/api/posts';
import type { PostCreateRequest } from '@/types';

const form = ref<PostCreateRequest>({
  title: '',
  content: '',
  category: '',
  tags: [],
  status: 'DRAFT'
});

async function submit() {
  try {
    const created = await createPost(form.value);
    router.push(`/${created.id}`);
  } catch (err) {
    console.error('Failed to create post:', err);
  }
}
```

### 게시글 수정

```typescript
// src/views/PostEditPage.vue
import { getPostById, updatePost } from '@/api/posts';
import type { PostUpdateRequest } from '@/types';

const postId = route.params.postId as string;
const form = ref<PostUpdateRequest>({
  title: '',
  content: ''
});

onMounted(async () => {
  try {
    const post = await getPostById(postId);
    form.value = {
      title: post.title,
      content: post.content
    };
  } catch (err) {
    console.error('Failed to load post:', err);
  }
});

async function submit() {
  try {
    await updatePost(postId, form.value);
    router.push(`/${postId}`);
  } catch (err) {
    console.error('Failed to update post:', err);
  }
}
```

## 에러 처리

### API 에러 응답

```typescript
// 실패 응답
{
  "success": false,
  "code": "B001",
  "message": "게시물을 찾을 수 없습니다",
  "data": null
}
```

### 에러 처리 패턴

```typescript
try {
  const post = await getPostById('123');
} catch (error: any) {
  // AxiosError
  if (error.response?.status === 404) {
    // 게시물 없음
    error.value = '게시물을 찾을 수 없습니다';
  } else if (error.response?.status === 403) {
    // 권한 없음
    error.value = '접근 권한이 없습니다';
  } else if (error.response?.status === 401) {
    // 인증 필요
    authStore.logout();
  } else {
    // 일반 에러
    error.value = '요청 중 오류가 발생했습니다';
  }
}
```

## 인터셉터

### Portal Shell의 apiClient 인터셉터

```typescript
// Portal Shell에서 설정됨
apiClient.interceptors.request.use(config => {
  // 1. JWT 토큰 추가
  const token = authStore.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  // 2. Base URL 설정
  config.baseURL = '/api';
  
  // 3. CSRF 토큰 추가
  config.headers['X-CSRF-Token'] = getCsrfToken();
  
  return config;
});

apiClient.interceptors.response.use(
  response => response,
  error => {
    // 401: 토큰 만료 → 재발급
    if (error.response?.status === 401) {
      return authStore.refreshToken()
        .then(() => apiClient(error.config));
    }
    
    // 403: 권한 없음
    if (error.response?.status === 403) {
      authStore.logout();
    }
    
    return Promise.reject(error);
  }
);
```

## 타입 정의

### 게시글 타입

```typescript
// src/dto/post.ts

export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'DELETED';

export interface PostResponse {
  id: string;
  title: string;
  content: string;          // Markdown
  summary?: string;
  category: string;
  tags: string[];
  authorId: string;
  authorName: string;
  thumbnailUrl?: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  status: PostStatus;
  createdAt: string;        // ISO 8601
  updatedAt: string;
  publishedAt: string;
}

export interface PostSummaryResponse {
  id: string;
  title: string;
  summary?: string;
  category: string;
  tags: string[];
  authorId: string;
  authorName: string;
  thumbnailUrl?: string;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  publishedAt: string;
}

export interface PostCreateRequest {
  title: string;
  content: string;
  summary?: string;
  category: string;
  tags?: string[];
  thumbnailUrl?: string;
  status?: PostStatus;
}

export interface PostUpdateRequest {
  title?: string;
  content?: string;
  summary?: string;
  category?: string;
  tags?: string[];
  thumbnailUrl?: string;
  status?: PostStatus;
}
```

### 페이징 타입

```typescript
// src/types/common.ts

export interface PageResponse<T> {
  content: T[];
  number: number;      // 현재 페이지 (0-indexed)
  size: number;        // 페이지 크기
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}
```

## 성능 팁

### 1. 요청 캐싱

```typescript
const cache = new Map();

async function getCachedPost(postId: string) {
  const cached = cache.get(postId);
  if (cached) return cached;
  
  const post = await getPostById(postId);
  cache.set(postId, post);
  return post;
}
```

### 2. 요청 취소

```typescript
let abortController: AbortController | null = null;

async function searchPosts(keyword: string) {
  // 이전 요청 취소
  abortController?.abort();
  
  // 새 요청
  abortController = new AbortController();
  const response = await searchPosts(keyword, 0, 10, {
    signal: abortController.signal
  });
  
  return response;
}
```

### 3. 배치 요청

```typescript
// 여러 API 병렬 요청
const [posts, stats, tags] = await Promise.all([
  getPublishedPosts(0, 10),
  getBlogStats(),
  getPopularTags(10)
]);
```

## 관련 문서

- [README.md](./README.md) - 모듈 개요
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 아키텍처
- [COMPONENTS.md](./COMPONENTS.md) - 컴포넌트 가이드
