package com.portal.universe.blogservice.service;

import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;

public interface BlogService {
    /**
     * 새로운 게시글을 생성합니다.
     * @param request 생성할 게시글의 정보 (제목, 내용)
     * @param authorId 작성자ID
     * @return 생성된 게시글의 정보
     */
    PostResponse createPost(PostCreateRequest request, String authorId);
}
