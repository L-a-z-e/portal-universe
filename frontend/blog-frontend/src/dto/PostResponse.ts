// blog-frontend/src/dto/PostResponse.ts

export interface PostResponse {
  id: string;
  title: string;
  content: string;
  summary?: string;
  authorId: string;
  authorName?: string;
  status: string;
  tags?: string[];
  category?: string;
  viewCount?: number;
  likeCount?: number;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  metaDescription?: string;
  thumbnailUrl?: string;
}
