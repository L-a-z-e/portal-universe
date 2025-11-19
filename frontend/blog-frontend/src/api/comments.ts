// blog-frontend/src/api/comments.ts

import type {
  CommentResponse,
  CommentCreateRequest,
  CommentUpdateRequest,
} from '@/types';
import type { ApiResponse } from '@/types';
import apiClient from './index';

const BASE_PATH = '/api/blog/comments';

/**
 * 특정 게시글의 모든 댓글 조회
 * GET /comments/post/{postId}
 */
export function getCommentsByPostId(postId: string): Promise<CommentResponse[]> {
  return apiClient
    .get<ApiResponse<CommentResponse[]>>(`${BASE_PATH}/post/${postId}`)
    // ⭐ 수정: /api/blog/comments/post/{postId}
    .then(res => res.data.data);
}

/**
 * 댓글 작성
 * POST /comments
 */
export function createComment(payload: CommentCreateRequest): Promise<CommentResponse> {
  return apiClient
    .post<ApiResponse<CommentResponse>>(`${BASE_PATH}`, payload)
    .then(res => res.data.data);
}

/**
 * 댓글 수정
 * PUT /comments/{commentId}
 */
export function updateComment(commentId: string, payload: CommentUpdateRequest): Promise<CommentResponse> {
  return apiClient
    .put<ApiResponse<CommentResponse>>(`${BASE_PATH}/${commentId}`, payload)
    .then(res => res.data.data);
}

/**
 * 댓글 삭제
 * DELETE /comments/{commentId}
 */
export function deleteComment(commentId: string): Promise<void> {
  return apiClient
    .delete<ApiResponse<void>>(`${BASE_PATH}/${commentId}`)
    .then(() => {});
}