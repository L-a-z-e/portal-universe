// frontend/blog-frontend/src/dto/series.ts

/**
 * 시리즈 응답 DTO
 */
export interface SeriesResponse {
  id: string;
  name: string;
  description: string;
  authorId: string;
  authorUsername: string;
  authorNickname: string;
  thumbnailUrl: string;
  postIds: string[];
  postCount: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 시리즈 생성 요청 DTO
 */
export interface SeriesCreateRequest {
  name: string;
  description?: string;
  thumbnailUrl?: string;
}

/**
 * 시리즈 수정 요청 DTO
 */
export interface SeriesUpdateRequest {
  name: string;
  description?: string;
  thumbnailUrl?: string;
}

/**
 * 시리즈 목록 응답 DTO
 */
export interface SeriesListResponse {
  id: string;
  name: string;
  description: string;
  authorUsername: string;
  authorNickname: string;
  thumbnailUrl: string;
  postCount: number;
  updatedAt: string;
}

/**
 * 시리즈 게시글 순서 변경 요청 DTO
 */
export interface SeriesPostOrderRequest {
  postIds: string[];
}