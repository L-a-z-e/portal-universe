// frontend/blog-frontend/src/dto/follow.ts

/**
 * 팔로우/언팔로우 응답 DTO
 */
export interface FollowResponse {
  following: boolean;
  followerCount: number;
  followingCount: number;
}

/**
 * 팔로워/팔로잉 목록의 사용자 정보 DTO
 */
export interface FollowUserResponse {
  uuid: string;
  username: string | null;
  nickname: string;
  profileImageUrl: string | null;
  bio: string | null;
}

/**
 * 팔로워/팔로잉 목록 응답 DTO
 */
export interface FollowListResponse {
  users: FollowUserResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/**
 * 팔로우 상태 확인 응답 DTO
 */
export interface FollowStatusResponse {
  isFollowing: boolean;
}

/**
 * 내 팔로잉 ID 목록 응답 DTO
 */
export interface FollowingIdsResponse {
  followingIds: string[];
}
