// blog-frontend/src/api/users.ts

import apiClient from './index';
import type { PageResponse } from '@portal/design-core';
import type {
  ApiResponse,
  PostSummaryResponse,
} from '@/types';
import type {
  UserProfileResponse,
  UserProfileUpdateRequest,
  UsernameSetRequest,
  UsernameCheckResponse,
} from '@/dto/user';

// ==================== 경로 상수 ====================
const AUTH_API_BASE = '/api/v1/users'; // Gateway를 통한 auth-service 경로
const BLOG_API_BASE = '/api/v1/blog/posts';

// ==================== 프로필 조회 ====================

/**
 * 공개 프로필 조회 (username 기반)
 */
export async function getPublicProfile(username: string): Promise<UserProfileResponse> {
  const response = await apiClient.get<ApiResponse<UserProfileResponse>>(
    `${AUTH_API_BASE}/${username}`
  );
  return response.data.data;
}

/**
 * 내 프로필 조회 (인증 필요)
 */
export async function getMyProfile(): Promise<UserProfileResponse> {
  const response = await apiClient.get<ApiResponse<UserProfileResponse>>(`${AUTH_API_BASE}/me`);
  return response.data.data;
}

// ==================== 프로필 수정 ====================

/**
 * 프로필 정보 수정
 */
export async function updateProfile(
  request: UserProfileUpdateRequest
): Promise<UserProfileResponse> {
  const response = await apiClient.patch<ApiResponse<UserProfileResponse>>(
    `${AUTH_API_BASE}/me`,
    request
  );
  return response.data.data;
}

// ==================== Username 관리 ====================

/**
 * Username 설정 (최초 1회만)
 */
export async function setUsername(username: string): Promise<UserProfileResponse> {
  const response = await apiClient.post<ApiResponse<UserProfileResponse>>(
    `${AUTH_API_BASE}/me/username`,
    { username } as UsernameSetRequest
  );
  return response.data.data;
}

/**
 * Username 중복 확인
 */
export async function checkUsername(username: string): Promise<UsernameCheckResponse> {
  const response = await apiClient.get<ApiResponse<UsernameCheckResponse>>(
    `${AUTH_API_BASE}/username/${username}/check`
  );
  return response.data.data;
}

// ==================== 사용자 게시글 조회 ====================

/**
 * 특정 사용자의 게시글 조회 (authorId 기반)
 * @param authorId 사용자 ID (숫자)
 * @param page 페이지 번호 (1부터 시작)
 * @param size 페이지 크기
 */
export async function getUserPosts(
  authorId: string,
  page: number = 1,
  size: number = 10
): Promise<PageResponse<PostSummaryResponse>> {
  const response = await apiClient.get<ApiResponse<PageResponse<PostSummaryResponse>>>(
    `${BLOG_API_BASE}/author/${authorId}`,
    {
      params: { page, size },
    }
  );
  return response.data.data;
}
