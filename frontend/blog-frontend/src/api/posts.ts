// blog-frontend/src/api/posts.ts

import apiClient from './index';
import type {
  ApiResponse,
  PageResponse,
  PostResponse,
  PostSummaryResponse,
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
const BASE_PATH = '/api/blog/posts';

// ==================== 기본 CRUD ====================

/** 게시물 생성 */
export function createPost(payload: PostCreateRequest): Promise<PostResponse> {
  return apiClient
    .post<ApiResponse<PostResponse>>(BASE_PATH, payload)
    .then((res) => res.data.data);
}

/** 게시물 수정 */
export function updatePost(postId: string, payload: PostUpdateRequest): Promise<PostResponse> {
  return apiClient
    .put<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}`, payload)
    .then((res) => res.data.data);
}

/** 게시물 삭제 */
export function deletePost(postId: string): Promise<void> {
  return apiClient
    .delete<ApiResponse<void>>(`${BASE_PATH}/${postId}`)
    .then(() => {});
}

/**
 * 게시물 상세 조회
 * @see PostController.getPostById
 */
export function getPostById(postId: string): Promise<PostResponse> {
  return apiClient
    .get<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}`)
    .then((res) => res.data.data);
}

/** 전체 게시물 조회 (관리자용) */
export function getAllPosts(): Promise<PostResponse[]> {
  return apiClient
    .get<ApiResponse<PostResponse[]>>(`${BASE_PATH}/all`)
    .then((res) => res.data.data);
}

// ==================== 게시물 목록 조회 ====================

/** 발행된 게시물 목록 (페이징) */
export async function getPublishedPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(BASE_PATH, {
    params: { page, size },
  });
  return response.data.data;
}

/** 내 게시물 조회 */
export async function getMyPosts(
  status?: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/my`,
    {
      params: { status, page, size },
    }
  );
  return response.data.data;
}

/** 작성자별 게시물 조회 */
export async function getPostsByAuthor(
  authorId: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/author/${authorId}`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/** 카테고리별 게시물 */
export async function getPostsByCategory(
  category: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/category/${category}`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/** 태그별 게시물 */
export async function getPostsByTags(
  tags: string[],
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/tags`,
    {
      params: { tags: tags.join(','), page, size },
    }
  );
  return response.data.data;
}

/** 인기 게시물 */
export async function getPopularPosts(
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/popular`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/** 최근 게시물 */
export async function getRecentPosts(limit: number = 5): Promise<PostSummaryResponse[]> {
  const response = await apiClient.get<ApiResponse<PostSummaryResponse[]>>(`${BASE_PATH}/recent`, {
    params: { limit },
  });
  return response.data.data;
}

/** 관련 게시물 */
export async function getRelatedPosts(
  postId: string,
  limit: number = 5
): Promise<PostSummaryResponse[]> {
  const response = await apiClient.get<ApiResponse<PostSummaryResponse[]>>(
    `${BASE_PATH}/${postId}/related`,
    {
      params: { limit },
    }
  );
  return response.data.data;
}

/** 게시물 조회 (조회수 증가) */
export async function getPostWithViewIncrement(postId: string): Promise<PostResponse> {
  const response = await apiClient.get<ApiResponse<PostResponse>>(`${BASE_PATH}/${postId}/view`);
  return response.data.data;
}

// ==================== 검색 ====================

/** 간단 검색 */
export async function searchPosts(
  keyword: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/search`,
    {
      params: { keyword, page, size },
    }
  );
  return response.data.data;
}

/** 고급 검색 */
export async function searchPostsAdvanced(
  searchRequest: PostSearchRequest
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.post<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BASE_PATH}/search/advanced`,
    searchRequest
  );
  return response.data.data;
}

// ==================== 상태 관리 ====================

/** 게시물 상태 변경 */
export async function changePostStatus(
  postId: string,
  request: PostStatusChangeRequest
): Promise<PostResponse> {
  const response = await apiClient.patch<ApiResponse<PostResponse>>(
    `${BASE_PATH}/${postId}/status`,
    request
  );
  return response.data.data;
}

// ==================== 통계 ====================

/** 카테고리 통계 */
export async function getCategoryStats(): Promise<CategoryStats[]> {
  const response = await apiClient.get<ApiResponse<CategoryStats[]>>(
    `${BASE_PATH}/stats/categories`
  );
  return response.data.data;
}

/** 인기 태그 */
export async function getPopularTags(limit: number = 10): Promise<TagStats[]> {
  const response = await apiClient.get<ApiResponse<TagStats[]>>(`${BASE_PATH}/stats/tags`, {
    params: { limit },
  });
  return response.data.data;
}

/** 작성자 통계 */
export async function getAuthorStats(authorId: string): Promise<AuthorStats> {
  const response = await apiClient.get<ApiResponse<AuthorStats>>(
    `${BASE_PATH}/stats/author/${authorId}`
  );
  return response.data.data;
}

/** 블로그 전체 통계 */
export async function getBlogStats(): Promise<BlogStats> {
  const response = await apiClient.get<ApiResponse<BlogStats>>(`${BASE_PATH}/stats/blog`);
  return response.data.data;
}

// ==================== 기존 호환성 ====================

/** 상품별 게시물 조회 */
export async function getPostsByProductId(productId: string): Promise<PostResponse[]> {
  const response = await apiClient.get<ApiResponse<PostResponse[]>>(
    `${BASE_PATH}/product/${productId}`
  );
  return response.data.data;
}