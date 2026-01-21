// frontend/blog-frontend/src/dto/navigation.ts

/**
 * 포스트 네비게이션 응답 DTO
 */
export interface PostNavigationResponse {
  previousPost?: PostNavigationItem;
  nextPost?: PostNavigationItem;
  scope: 'all' | 'author' | 'category' | 'series';
}

/**
 * 포스트 네비게이션 아이템
 */
export interface PostNavigationItem {
  id: string;
  title: string;
  thumbnailUrl?: string;
  publishedAt: string;
}
