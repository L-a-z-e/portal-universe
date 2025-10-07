import apiClient from "./index.ts";
import type { PostResponse } from '../dto/PostResponse';
import type { PostCreateRequest } from '../dto/PostCreateRequest';

export function fetchAllPosts() {
    return apiClient.get<PostResponse[]>('/api/blog').then(res => res.data);
}

export function createPost(payload: PostCreateRequest) {
    return apiClient.post<PostResponse>('/api/blog', payload).then(res => res.data);
}