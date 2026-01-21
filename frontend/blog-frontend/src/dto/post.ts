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
  authorName: string;
  status: PostStatus;
  tags: string[];  // Java Set<String> → TS string[]
  category: string;
  metaDescription: string;
  thumbnailUrl: string;
  images: string[];  // Java List<String> → TS string[]
  viewCount: number;  // Java Long → TS number
  likeCount: number;
  createdAt: string;  // Java LocalDateTime → TS string (ISO-8601)
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
  authorName: string;
  status?: PostStatus;  // 내 글 목록에서 상태 표시용
  tags: string[];
  category: string;
  thumbnailUrl: string;
  images: string[];
  viewCount: number;
  likeCount: number;
  commentCount: number;  // Phase 3: 댓글 수 추가
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
  authorName: string;
  postCount: number;
  totalViews: number;
  totalLikes: number;
}

/**
 * 블로그 전체 통계 DTO
 */
export interface BlogStats {
  totalPosts: number;
  totalViews: number;
  totalLikes: number;
  totalComments: number;
}

/**
 * 카테고리 통계 DTO
 */
export interface CategoryStats {
  category: string;
  count: number;
}

/**
 * 태그 통계 DTO
 */
export interface TagStats {
  tag: string;
  count: number;
}