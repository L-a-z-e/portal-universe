import apiClient from "./index.ts";
import type { PostResponse } from '../dto/PostResponse';
import type { PostCreateRequest } from '../dto/PostCreateRequest';
import type { PostUpdateRequest } from "../dto/PostUpdateRequest.ts";

export function fetchAllPosts() {
    return apiClient.get<PostResponse[]>('/api/blog').then(res => res.data);
}

export function createPost(payload: PostCreateRequest) {
    return apiClient.post<PostResponse>('/api/blog', payload).then(res => res.data);
}

export function updatePost(postId: string, payload: PostUpdateRequest) {
    return apiClient.put<PostResponse>(`/api/blog/${postId}`, payload).then(res => res.data);
}

export function deletePost(postId: string) {
    return apiClient.delete(`/api/blog/${postId}`);
}

export function fetchPostById(postId: string) {
    return apiClient.get<PostResponse>(`/api/blog/${postId}`).then(res => res.data);
}