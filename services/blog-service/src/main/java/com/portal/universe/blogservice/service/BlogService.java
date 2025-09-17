package com.portal.universe.blogservice.service;

import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;

import java.util.List;

public interface BlogService {
    /**
     * 새로운 게시글을 생성합니다.
     * @param request 생성할 게시글의 정보 (제목, 내용)
     * @param authorId 작성자ID
     * @return 생성된 게시글의 정보
     */
    PostResponse createPost(PostCreateRequest request, String authorId);

    /**
     * 모든 게시글 목록을 조회합니다.
     * @return 게시글 정보 목록
     */
    List<PostResponse> getAllPosts();

    /**
     * postId로 특정 게시글을 조회합니다.
     * @param postId 조회할 게시글의 ID
     * @return 조회된 게시글 정보
     */
    PostResponse getPostById(String postId);
}
