// blog-frontend/src/api/follow.ts

import apiClient from './index';
import type { ApiResponse } from '@/types';
import type {
  FollowResponse,
  FollowListResponse,
  FollowStatusResponse,
  FollowingIdsResponse,
} from '@/dto/follow';

// ==================== 경로 상수 ====================
const AUTH_API_BASE = '/api/v1/users'; // Gateway를 통한 auth-service 경로

// ==================== 팔로우 토글 ====================

/**
 * 팔로우 토글 (팔로우/언팔로우)
 * @param username 대상 사용자의 username
 */
export async function toggleFollow(username: string): Promise<FollowResponse> {
  const response = await apiClient.post<ApiResponse<FollowResponse>>(
    `${AUTH_API_BASE}/${username}/follow`
  );
  return response.data.data;
}

// ==================== 팔로워/팔로잉 목록 조회 ====================

/**
 * 팔로워 목록 조회
 * @param username 사용자의 username
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 */
export async function getFollowers(
  username: string,
  page: number = 0,
  size: number = 20
): Promise<FollowListResponse> {
  const response = await apiClient.get<ApiResponse<FollowListResponse>>(
    `${AUTH_API_BASE}/${username}/followers`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

/**
 * 팔로잉 목록 조회
 * @param username 사용자의 username
 * @param page 페이지 번호 (0부터 시작)
 * @param size 페이지 크기
 */
export async function getFollowings(
  username: string,
  page: number = 0,
  size: number = 20
): Promise<FollowListResponse> {
  const response = await apiClient.get<ApiResponse<FollowListResponse>>(
    `${AUTH_API_BASE}/${username}/following`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}

// ==================== 팔로우 상태 확인 ====================

/**
 * 팔로우 상태 확인
 * @param username 대상 사용자의 username
 */
export async function getFollowStatus(username: string): Promise<FollowStatusResponse> {
  const response = await apiClient.get<ApiResponse<FollowStatusResponse>>(
    `${AUTH_API_BASE}/${username}/follow/status`
  );
  return response.data.data;
}

// ==================== 내 팔로잉 ID 목록 조회 ====================

/**
 * 내가 팔로우하는 사용자들의 UUID 목록 조회
 * 피드 API 호출 시 사용
 */
export async function getMyFollowingIds(): Promise<FollowingIdsResponse> {
  const response = await apiClient.get<ApiResponse<FollowingIdsResponse>>(
    `${AUTH_API_BASE}/me/following/ids`
  );
  return response.data.data;
}
