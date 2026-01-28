// blog-frontend/src/api/likes.ts

import apiClient from './index';
import type {
  ApiResponse,
  PageResponse,
  LikeToggleResponse,
  LikeStatusResponse,
  LikerResponse,
} from '@/types';

// ==================== 경로 상수 ====================
// LikeController는 /posts/{postId} 하위에 매핑됨
const BASE_PATH = '/api/blog/posts';

// ==================== 좋아요 기능 ====================

/**
 * 좋아요 토글 (추가/취소)
 * POST /posts/{postId}/like
 * @param postId 포스트 ID
 */
export async function toggleLike(postId: string): Promise<LikeToggleResponse> {
  const response = await apiClient.post<ApiResponse<LikeToggleResponse>>(
    `${BASE_PATH}/${postId}/like`
  );
  return response.data.data;
}

/**
 * 좋아요 상태 확인
 * GET /posts/{postId}/like
 * @param postId 포스트 ID
 */
export async function getLikeStatus(postId: string): Promise<LikeStatusResponse> {
  const response = await apiClient.get<ApiResponse<LikeStatusResponse>>(
    `${BASE_PATH}/${postId}/like`
  );
  return response.data.data;
}

/**
 * 좋아요한 사용자 목록 조회
 * GET /posts/{postId}/likes
 * @param postId 포스트 ID
 * @param page 페이지 번호
 * @param size 페이지 크기
 */
export async function getLikers(
  postId: string,
  page: number = 0,
  size: number = 20
): Promise<PageResponse<LikerResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<LikerResponse>>>(
    `${BASE_PATH}/${postId}/likes`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}
