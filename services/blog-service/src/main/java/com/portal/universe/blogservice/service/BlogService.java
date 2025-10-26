package com.portal.universe.blogservice.service;

import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;
import com.portal.universe.blogservice.dto.PostUpdateRequest;

import java.util.List;

/**
 * 블로그 서비스의 비즈니스 로직을 정의하는 인터페이스입니다.
 */
public interface BlogService {
    /**
     * 새로운 게시글을 생성합니다.
     * @param request 생성할 게시글의 정보 (제목, 내용, 상품ID)
     * @param authorId 작성자 ID
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

    /**
     * 기존 게시글을 수정합니다.
     * @param postId 수정할 게시글의 ID
     * @param request 수정할 내용 (제목, 내용)
     * @param userId 요청한 사용자의 ID (권한 확인용)
     * @return 수정된 게시글 정보
     */
    PostResponse updatePost(String postId, PostUpdateRequest request, String userId);

    /**
     * 게시글을 삭제합니다.
     * @param postId 삭제할 게시글의 ID
     * @param userId 요청한 사용자의 ID (권한 확인용)
     */
    void deletePost(String postId, String userId);

    /**
     * productId로 특정 게시글 목록을 조회합니다.
     * @param productId 조회할 상품 ID
     * @return 조회된 게시글 정보 목록
     */
    List<PostResponse> getPostsByProductId(String productId);
}