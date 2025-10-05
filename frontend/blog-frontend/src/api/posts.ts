import apiClient from "./index.ts";
import type { PostResponse } from '../dto/PostResponse';

export function fetchAllPosts() {
    return apiClient.get<PostResponse[]>('/api/blog').then(res => res.data);
}