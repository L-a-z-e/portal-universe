// blog-frontend/src/api/series.ts

import apiClient from './index';
import type {
  ApiResponse,
  SeriesResponse,
  SeriesListResponse,
  SeriesCreateRequest,
  SeriesUpdateRequest,
  PostSummaryResponse,
} from '@/types';

// ==================== 경로 상수 ====================
const BASE_PATH = '/api/blog/series';

// ==================== 시리즈 조회 ====================

/**
 * 시리즈 목록 조회
 * @param authorId 작성자 ID (optional)
 */
export async function getSeriesList(authorId?: string): Promise<SeriesListResponse[]> {
  const url = authorId ? `${BASE_PATH}/author/${authorId}` : BASE_PATH;
  const response = await apiClient.get<ApiResponse<SeriesListResponse[]>>(url);
  return response.data.data;
}

/**
 * 시리즈 상세 조회
 * @param seriesId 시리즈 ID
 */
export async function getSeriesById(seriesId: string): Promise<SeriesResponse> {
  const response = await apiClient.get<ApiResponse<SeriesResponse>>(`${BASE_PATH}/${seriesId}`);
  return response.data.data;
}

/**
 * 시리즈에 포함된 포스트 목록 조회
 * @param seriesId 시리즈 ID
 */
export async function getSeriesPosts(seriesId: string): Promise<PostSummaryResponse[]> {
  const response = await apiClient.get<ApiResponse<PostSummaryResponse[]>>(
    `${BASE_PATH}/${seriesId}/posts`
  );
  return response.data.data;
}

/**
 * 내 시리즈 목록 조회
 */
export async function getMySeries(): Promise<SeriesListResponse[]> {
  const response = await apiClient.get<ApiResponse<SeriesListResponse[]>>(`${BASE_PATH}/my`);
  return response.data.data;
}

// ==================== 시리즈 관리 (작성자용) ====================

/**
 * 시리즈 생성
 * @param request 시리즈 생성 요청 데이터
 */
export async function createSeries(request: SeriesCreateRequest): Promise<SeriesResponse> {
  const response = await apiClient.post<ApiResponse<SeriesResponse>>(BASE_PATH, request);
  return response.data.data;
}

/**
 * 시리즈 수정
 * @param seriesId 시리즈 ID
 * @param request 시리즈 수정 요청 데이터
 */
export async function updateSeries(
  seriesId: string,
  request: SeriesUpdateRequest
): Promise<SeriesResponse> {
  const response = await apiClient.put<ApiResponse<SeriesResponse>>(
    `${BASE_PATH}/${seriesId}`,
    request
  );
  return response.data.data;
}

/**
 * 시리즈 삭제
 * @param seriesId 시리즈 ID
 */
export async function deleteSeries(seriesId: string): Promise<void> {
  await apiClient.delete<ApiResponse<void>>(`${BASE_PATH}/${seriesId}`);
}

/**
 * 시리즈 포스트 순서 변경
 * @param seriesId 시리즈 ID
 * @param postIds 정렬된 포스트 ID 배열
 */
export async function reorderSeriesPosts(
  seriesId: string,
  postIds: string[]
): Promise<SeriesResponse> {
  const response = await apiClient.put<ApiResponse<SeriesResponse>>(
    `${BASE_PATH}/${seriesId}/posts/order`,
    { postIds }
  );
  return response.data.data;
}

/**
 * 시리즈에 포스트 추가
 * @param seriesId 시리즈 ID
 * @param postId 포스트 ID
 */
export async function addPostToSeries(
  seriesId: string,
  postId: string
): Promise<SeriesResponse> {
  const response = await apiClient.post<ApiResponse<SeriesResponse>>(
    `${BASE_PATH}/${seriesId}/posts/${postId}`
  );
  return response.data.data;
}

/**
 * 시리즈에서 포스트 제거
 * @param seriesId 시리즈 ID
 * @param postId 포스트 ID
 */
export async function removePostFromSeries(
  seriesId: string,
  postId: string
): Promise<SeriesResponse> {
  const response = await apiClient.delete<ApiResponse<SeriesResponse>>(
    `${BASE_PATH}/${seriesId}/posts/${postId}`
  );
  return response.data.data;
}

/**
 * 특정 포스트가 속한 시리즈 조회
 * @param postId 포스트 ID
 */
export async function getSeriesByPostId(postId: string): Promise<SeriesListResponse[]> {
  const response = await apiClient.get<ApiResponse<SeriesListResponse[]>>(
    `${BASE_PATH}/by-post/${postId}`
  );
  return response.data.data;
}
