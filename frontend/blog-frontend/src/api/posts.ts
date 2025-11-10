// blog-frontend/src/api/posts.ts

import apiClient from './index';
import type {
  ApiResponse,
  PageResponse,
  PostResponse,
  PostListResponse,
  PostCreateRequest,
  PostUpdateRequest,
  PostStatusChangeRequest,
  PostSearchRequest,
  CategoryStats,
  TagStats,
  AuthorStats,
  BlogStats,
} from '@/types';

// ==================== 경로 상수 ====================

const LEGACY_BASE_PATH = '/api/blog';  // 기존 경로
const BASE_PATH = '/blog-service/api/posts';  // 새 경로

// ==================== 기존 API (하위 호환) ====================

/**
 * 전체 게시물 조회 (기존 API)
 * @deprecated Use getPublishedPosts instead
 */
export function fetchAllPosts(): Promise<PostResponse[]> {
  return apiClient
    .get<ApiResponse<PostResponse[]>>(LEGACY_BASE_PATH)
    .then((res) => res.data.data);
}

/**
 * 게시물 생성
 */
export function createPost(payload: PostCreateRequest): Promise<PostResponse> {
  return apiClient
    .post<ApiResponse<PostResponse>>(BASE_PATH, payload)
    .then((res) => res.data.data);
}

/**
 * 게시물 수정
 */
export function updatePost(postId: string, payload: PostUpdateRequest): Promise<PostResponse> {
  return apiClient
    .put<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}`, payload)
    .then((res) => res.data.data);
}

/**
 * 게시물 삭제
 */
export function deletePost(postId: string): Promise<void> {
  return apiClient
    .delete<ApiResponse<void>>(`${BASE_PATH}/${postId}`)
    .then(() => {});
}

/**
 * 게시물 상세 조회
 */
export function fetchPostById(postId: string): Promise<PostResponse> {
  return apiClient
    .get<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}`)
    .then((res) => res.data.data);
}

// ==================== 확장 API ====================

/**
 * 발행된 게시물 목록 (페이징)
 */
export async function getPublishedPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(BASE_PATH, {
    params: { page, size },
  });
  return response.data.data;
}

/**
 * 내 게시물 조회
 */
export async function getMyPosts(
  status?: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/my`,
    {
      params: { status, page, size },
    }
  );
  return response.data.data;
}

/**
 * 작성자별 게시물 조회
 */
export async function getPostsByAuthor(
  authorId: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/author/${authorId}`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/**
 * 카테고리별 게시물
 */
export async function getPostsByCategory(
  category: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/category/${category}`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/**
 * 태그별 게시물
 */
export async function getPostsByTags(
  tags: string[],
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/tags`,
    {
      params: { tags: tags.join(','), page, size },
    }
  );
  return response.data.data;
}

/**
 * 간단 검색
 */
export async function searchPosts(
  keyword: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/search`,
    {
      params: { keyword, page, size },
    }
  );
  return response.data.data;
}

/**
 * 고급 검색
 */
export async function searchPostsAdvanced(
  searchRequest: PostSearchRequest
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.post<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/search/advanced`,
    searchRequest
  );
  return response.data.data;
}

/**
 * 인기 게시물
 */
export async function getPopularPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostListResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostListResponse>>>(
    `${BASE_PATH}/popular`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/**
 * 최근 게시물
 */
export async function getRecentPosts(limit: number = 5): Promise<PostListResponse[]> {
  const response = await apiClient.get<ApiResponse<PostListResponse[]>>(`${BASE_PATH}/recent`, {
    params: { limit },
  });
  return response.data.data;
}

/**
 * 관련 게시물
 */
export async function getRelatedPosts(
  postId: string,
  limit: number = 5
): Promise<PostListResponse[]> {
  const response = await apiClient.get<ApiResponse<PostListResponse[]>>(
    `${BASE_PATH}/${postId}/related`,
    {
      params: { limit },
    }
  );
  return response.data.data;
}

/**
 * 게시물 조회 (조회수 증가)
 */
export async function getPostWithViewIncrement(postId: string): Promise<PostResponse> {
  const response = await apiClient.get<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}/view`);
  return response.data.data;
}

/**
 * 게시물 상태 변경 (백엔드 DTO와 정확히 일치)
 */
export async function changePostStatus(
  postId: string,
  request: PostStatusChangeRequest  // { newStatus: PostStatus }
): Promise<PostResponse> {
  const response = await apiClient.patch<ApiResponse<PostResponse>>(
    `${BASE_PATH}/${postId}/status`,
    request
  );
  return response.data.data;
}

// ==================== 통계 ====================

/**
 * 카테고리 통계
 */
export async function getCategoryStats(): Promise<CategoryStats[]> {
  const response = await apiClient.get<ApiResponse<CategoryStats[]>>(
    `${BASE_PATH}/stats/categories`
  );
  return response.data.data;
}

/**
 * 인기 태그
 */
export async function getPopularTags(limit: number = 10): Promise<TagStats[]> {
  const response = await apiClient.get<ApiResponse<TagStats[]>>(`${BASE_PATH}/stats/tags`, {
    params: { limit },
  });
  return response.data.data;
}

/**
 * 작성자 통계
 */
export async function getAuthorStats(authorId: string): Promise<AuthorStats> {
  const response = await apiClient.get<ApiResponse<AuthorStats>>(
    `${BASE_PATH}/stats/author/${authorId}`
  );
  return response.data.data;
}

/**
 * 블로그 전체 통계
 */
export async function getBlogStats(): Promise<BlogStats> {
  const response = await apiClient.get<ApiResponse<BlogStats>>(`${BASE_PATH}/stats/blog`);
  return response.data.data;
}

// ==================== 기존 호환성 ====================

/**
 * 상품별 게시물 조회
 */
export async function getPostsByProductId(productId: string): Promise<PostResponse[]> {
  const response = await apiClient.get<ApiResponse<PostResponse[]>>(
    `${BASE_PATH}/product/${productId}`
  );
  return response.data.data;
}