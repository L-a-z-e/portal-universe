// frontend/blog-frontend/src/dto/like.ts

/**
 * 좋아요 토글 응답 DTO
 */
export interface LikeToggleResponse {
  postId: string;
  userId: string;
  liked: boolean;
  likeCount: number;
  timestamp: string;
}

/**
 * 좋아요 상태 응답 DTO
 */
export interface LikeStatusResponse {
  postId: string;
  userId: string;
  liked: boolean;
  likeCount: number;
}

/**
 * 좋아요한 사용자 응답 DTO
 */
export interface LikerResponse {
  userId: string;
  username: string;
  profileImageUrl?: string;
  likedAt: string;
}
