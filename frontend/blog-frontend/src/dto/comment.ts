// frontend/blog-frontend/src/dto/comment.ts

/**
 * 댓글 응답 DTO
 */
export interface CommentResponse {
  id: string;
  postId: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId: string | null;
  likeCount: number;
  isDeleted: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * 댓글 생성 요청 DTO
 */
export interface CommentCreateRequest {
  postId: string;
  parentCommentId?: string | null;
  content: string;
}

/**
 * 댓글 수정 요청 DTO
 */
export interface CommentUpdateRequest {
  content: string;
}