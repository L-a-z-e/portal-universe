// blog-frontend/src/types/post.ts

/**
 * 게시물 상태
 */
export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

/**
 * 정렬 타입
 */
export type PostSortType = 'CREATED_AT' | 'PUBLISHED_AT' | 'VIEW_COUNT' | 'LIKE_COUNT' | 'TITLE';

/**
 * 정렬 방향
 */
export type SortDirection = 'ASC' | 'DESC';

/**
 * 게시물 생성 요청
 */
export interface PostCreateRequest {
  title: string;
  content: string;
  summary?: string;
  tags?: string[];
  category?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
  publishImmediately?: boolean;
  productId?: string;
}

/**
 * 게시물 수정 요청
 */
export interface PostUpdateRequest {
  title: string;
  content: string;
  summary?: string;
  tags?: string[];
  category?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
}

/**
 * 게시물 상태 변경 요청
 */
export interface PostStatusChangeRequest {
  status: PostStatus;
}

/**
 * 게시물 검색 요청
 */
export interface PostSearchRequest {
  keyword?: string;
  category?: string;
  tags?: string[];
  authorId?: string;
  status?: PostStatus;
  sortBy?: PostSortType;
  sortDirection?: SortDirection;
  page: number;
  size: number;
}

/**
 * 게시물 상세 응답
 */
export interface PostResponse {
  id: string;
  title: string;
  content: string;
  summary?: string;
  authorId: string;
  authorName: string;
  status: PostStatus;
  tags: string[];
  category?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
  viewCount: number;
  likeCount: number;
  createdAt: string;
  updatedAt: string;
  publishedAt?: string;
  productId?: string;
}

/**
 * 게시물 목록 응답 (간소화)
 */
export interface PostListResponse {
  id: string;
  title: string;
  summary?: string;
  authorId: string;
  authorName: string;
  tags: string[];
  category?: string;
  thumbnailUrl?: string;
  viewCount: number;
  likeCount: number;
  publishedAt?: string;
  estimatedReadTimeMinutes: number;
}

/**
 * 카테고리 통계
 */
export interface CategoryStats {
  categoryName: string;
  postCount: number;
  latestPostDate?: string;
}

/**
 * 태그 통계
 */
export interface TagStats {
  tagName: string;
  postCount: number;
  totalViews: number;
}

/**
 * 작성자 통계
 */
export interface AuthorStats {
  authorId: string;
  authorName?: string;
  totalPosts: number;
  publishedPosts: number;
  totalViews: number;
  totalLikes: number;
  firstPostDate?: string;
  lastPostDate?: string;
}

/**
 * 블로그 전체 통계
 */
export interface BlogStats {
  totalPosts: number;
  publishedPosts: number;
  totalViews: number;
  totalLikes: number;
  topCategories: string[];
  topTags: string[];
  lastPostDate?: string;
}