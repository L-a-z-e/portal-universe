// frontend/blog-frontend/src/dto/post.ts

/**
 * 게시글 상태
 */
export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

/**
 * 게시글 응답 DTO
 */
export interface PostResponse {
  id: string;
  title: string;
  content: string;
  summary: string;
  authorId: string;
  authorUsername: string;
  authorNickname: string;
  status: PostStatus;
  tags: string[];
  category: string;
  metaDescription: string;
  thumbnailUrl: string;
  images: string[];
  viewCount: number;
  likeCount: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string;
  productId: string;
}

/**
 * 게시글 생성 요청 DTO
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
  images?: string[];
  productId?: string;
}

/**
 * 게시글 수정 요청 DTO
 */
export interface PostUpdateRequest {
  title: string;
  content: string;
  summary?: string;
  tags?: string[];
  category?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
  images?: string[];
}

/**
 * 게시글 목록 응답 (요약 정보)
 */
export interface PostSummaryResponse {
  id: string;
  title: string;
  summary: string;
  authorId: string;
  authorUsername: string;
  authorNickname: string;
  status?: PostStatus;
  tags: string[];
  category: string;
  thumbnailUrl: string;
  images: string[];
  viewCount: number;
  likeCount: number;
  commentCount: number;
  publishedAt: string;
  estimatedReadTime: number;
}

/**
 * 게시글 상태 변경 요청 DTO
 */
export interface PostStatusChangeRequest {
  status: PostStatus;
}

/**
 * 게시글 검색 요청 DTO
 */
export interface PostSearchRequest {
  keyword?: string;
  category?: string;
  tags?: string[];
  authorId?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

/**
 * 작성자 통계 DTO
 */
export interface AuthorStats {
  authorId: string;
  authorUsername: string;
  authorNickname: string;
  totalPosts: number;
  publishedPosts: number;
  totalViews: number;
  totalLikes: number;
  firstPostDate: string;
  lastPostDate: string;
}

/**
 * 블로그 전체 통계 DTO
 */
export interface BlogStats {
  totalPosts: number;
  publishedPosts: number;
  totalViews: number;
  totalLikes: number;
  topCategories: string[];
  topTags: string[];
  lastPostDate: string;
}

/**
 * 카테고리 통계 DTO
 */
export interface CategoryStats {
  categoryName: string;
  postCount: number;
  latestPostDate: string;
}

