// frontend/blog-frontend/src/dto/tag.ts

/**
 * 태그 응답 DTO
 */
export interface TagResponse {
  id: string;
  name: string;
  postCount: number;
  description: string;
  createdAt: string;
  lastUsedAt: string;
}

/**
 * 태그 생성 요청 DTO
 */
export interface TagCreateRequest {
  name: string;
  description?: string;
}

/**
 * 태그 통계 응답 DTO
 */
export interface TagStatsResponse {
  name: string;
  postCount: number;
  totalViews: number | null;
}