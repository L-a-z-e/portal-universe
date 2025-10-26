import apiClient from "./index.ts";
import type { PostResponse } from '../dto/PostResponse';
import type { PostCreateRequest } from '../dto/PostCreateRequest';
import type { PostUpdateRequest } from "../dto/PostUpdateRequest.ts";

type ApiResponse<T> = {
    success: boolean;
    data: T;
    error: any;
}

export function fetchAllPosts(): Promise<PostResponse[]> {
    return apiClient.get<ApiResponse<PostResponse[]>>('/api/blog').then(res => res.data.data);
}

export function createPost(payload: PostCreateRequest): Promise<PostResponse> {
    return apiClient.post<ApiResponse<PostResponse>>('/api/blog', payload).then(res => res.data.data);
}

export function updatePost(postId: string, payload: PostUpdateRequest): Promise<PostResponse> {
    return apiClient.put<ApiResponse<PostResponse>>(`/api/blog/${postId}`, payload).then(res => res.data.data);
}

export function deletePost(postId: string): Promise<void> {
    return apiClient.delete<ApiResponse<void>>(`/api/blog/${postId}`).then(() => {});
}

export function fetchPostById(postId: string): Promise<PostResponse> {
    return apiClient.get<ApiResponse<PostResponse>>(`/api/blog/${postId}`).then(res => res.data.data);
}