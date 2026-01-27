// blog-frontend/src/api/tags.ts

import apiClient from './index';
import type {
  ApiResponse,
  PageResponse,
  TagResponse,
  TagStatsResponse,
  PostSummaryResponse,
} from '@/types';

// ==================== 경로 상수 ====================
const BASE_PATH = '/api/blog/tags';

// ==================== 태그 조회 ====================

/**
 * 전체 태그 목록 조회
 */
export async function getAllTags(): Promise<TagResponse[]> {
  const response = await apiClient.get<ApiResponse<TagResponse[]>>(BASE_PATH);
  return response.data.data;
}

/**
 * 태그 상세 조회
 * @param tagId 태그 ID
 */
export async function getTagById(tagId: string): Promise<TagResponse> {
  const response = await apiClient.get<ApiResponse<TagResponse>>(`${BASE_PATH}/${tagId}`);
  return response.data.data;
}

/**
 * 태그명으로 태그 조회
 * @param tagName 태그명
 */
export async function getTagByName(tagName: string): Promise<TagResponse> {
  const response = await apiClient.get<ApiResponse<TagResponse>>(`${BASE_PATH}/${tagName}`);
  return response.data.data;
}

/**
 * 태그로 포스트 검색
 * @param tagName 태그명
 * @param page 페이지 번호 (기본값: 0)
 * @param size 페이지 크기 (기본값: 10)
 */
export async function getPostsByTag(
  tagName: string,
  page: number = 0,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    '/api/blog/posts/tags',
    {
      params: { tags: tagName, page, size },
    }
  );
  return response.data.data;
}

/**
 * 인기 태그 목록 조회
 * @param limit 조회할 태그 수 (기본값: 20)
 */
export async function getPopularTags(limit: number = 20): Promise<TagStatsResponse[]> {
  const response = await apiClient.get<ApiResponse<TagStatsResponse[]>>(
    `${BASE_PATH}/popular`,
    {
      params: { limit },
    }
  );
  return response.data.data;
}

/**
 * 태그 검색
 * @param keyword 검색 키워드
 */
export async function searchTags(keyword: string): Promise<TagResponse[]> {
  const response = await apiClient.get<ApiResponse<TagResponse[]>>(`${BASE_PATH}/search`, {
    params: { q: keyword },
  });
  return response.data.data;
}
