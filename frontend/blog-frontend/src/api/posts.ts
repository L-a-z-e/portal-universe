import apiClient from "./index.ts";
import type { PostResponse } from '../dto/PostResponse';
import type { PostCreateRequest } from '../dto/PostCreateRequest';
import type { PostUpdateRequest } from "../dto/PostUpdateRequest.ts";

/**
 * @file api/posts.ts
 * @description 블로그 게시물(Post) 관련 API 호출 함수들을 정의합니다.
 */

// 백엔드의 ApiResponse<T> 형식에 대한 타입 정의
type ApiResponse<T> = {
    success: boolean;
    data: T;
    error: any;
}

/**
 * 모든 게시물 목록을 조회합니다.
 * @returns {Promise<PostResponse[]>} 게시물 목록
 */
export function fetchAllPosts(): Promise<PostResponse[]> {
    return apiClient.get<ApiResponse<PostResponse[]>>('/api/blog').then(res => res.data.data);
}

/**
 * 새로운 게시물을 생성합니다.
 * @param payload 생성할 게시물 정보
 * @returns {Promise<PostResponse>} 생성된 게시물 정보
 */
export function createPost(payload: PostCreateRequest): Promise<PostResponse> {
    return apiClient.post<ApiResponse<PostResponse>>('/api/blog', payload).then(res => res.data.data);
}

/**
 * 기존 게시물을 수정합니다.
 * @param postId 수정할 게시물의 ID
 * @param payload 수정할 내용
 * @returns {Promise<PostResponse>} 수정된 게시물 정보
 */
export function updatePost(postId: string, payload: PostUpdateRequest): Promise<PostResponse> {
    return apiClient.put<ApiResponse<PostResponse>>(`/api/blog/${postId}`, payload).then(res => res.data.data);
}

/**
 * 게시물을 삭제합니다.
 * @param postId 삭제할 게시물의 ID
 * @returns {Promise<void>}
 */
export function deletePost(postId: string): Promise<void> {
    return apiClient.delete<ApiResponse<void>>(`/api/blog/${postId}`).then(() => {});
}

/**
 * ID로 특정 게시물을 조회합니다.
 * @param postId 조회할 게시물의 ID
 * @returns {Promise<PostResponse>} 조회된 게시물 정보
 */
export function fetchPostById(postId: string): Promise<PostResponse> {
    return apiClient.get<ApiResponse<PostResponse>>(`/api/blog/${postId}`).then(res => res.data.data);
}
