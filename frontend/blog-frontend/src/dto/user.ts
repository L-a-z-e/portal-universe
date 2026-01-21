// frontend/blog-frontend/src/dto/user.ts

/**
 * 사용자 프로필 응답 DTO
 */
export interface UserProfileResponse {
  id: number;
  email: string;
  name: string;
  username: string | null;
  bio: string | null;
  profileImageUrl: string | null;
  website: string | null;
  createdAt: string;
}

/**
 * 프로필 수정 요청 DTO
 */
export interface UserProfileUpdateRequest {
  name?: string;
  bio?: string;
  profileImageUrl?: string;
  website?: string;
}

/**
 * Username 설정 요청 DTO
 */
export interface UsernameSetRequest {
  username: string;
}

/**
 * Username 중복 확인 응답 DTO
 */
export interface UsernameCheckResponse {
  username: string;
  available: boolean;
  message: string;
}
