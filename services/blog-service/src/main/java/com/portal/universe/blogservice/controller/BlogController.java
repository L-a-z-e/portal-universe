package com.portal.universe.blogservice.controller;

import com.portal.universe.blogservice.dto.PostCreateRequest;
import com.portal.universe.blogservice.dto.PostResponse;
import com.portal.universe.blogservice.dto.PostUpdateRequest;
import com.portal.universe.blogservice.service.BlogService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 블로그 게시물(Post)에 대한 CRUD API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    /**
     * 새로운 게시물을 생성합니다.
     * ADMIN 역할이 있는 사용자만 호출할 수 있습니다.
     * @param request 생성할 게시물의 제목과 내용을 담은 DTO
     * @param jwt Spring Security가 주입해주는 JWT 객체. 작성자 ID(subject)를 추출하는 데 사용됩니다.
     * @return 생성된 게시물 정보를 담은 ApiResponse
     */
    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        String authorId = jwt.getSubject();
        return ApiResponse.success(blogService.createPost(request, authorId));
    }

    /**
     * 모든 게시물 목록을 조회합니다.
     * 누구나 호출할 수 있습니다.
     * @return 전체 게시물 목록을 담은 ApiResponse
     */
    @GetMapping
    public ApiResponse<List<PostResponse>> getAllPosts() {
        return ApiResponse.success(blogService.getAllPosts());
    }

    /**
     * 특정 ID를 가진 게시물을 조회합니다.
     * 누구나 호출할 수 있습니다.
     * @param postId 조회할 게시물의 ID
     * @return 조회된 게시물 정보를 담은 ApiResponse
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(@PathVariable String postId) {
        return ApiResponse.success(blogService.getPostById(postId));
    }

    /**
     * 특정 게시물을 수정합니다.
     * ADMIN 역할이 있고, 해당 게시물의 작성자 본인만 호출할 수 있습니다.
     * @param postId 수정할 게시물의 ID
     * @param request 수정할 게시물의 제목과 내용을 담은 DTO
     * @param jwt 요청한 사용자의 ID를 확인하기 위한 JWT 객체
     * @return 수정된 게시물 정보를 담은 ApiResponse
     */
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String postId,
            @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return ApiResponse.success(blogService.updatePost(postId, request, userId));
    }

    /**
     * 특정 게시물을 삭제합니다.
     * ADMIN 역할이 있고, 해당 게시물의 작성자 본인만 호출할 수 있습니다.
     * @param postId 삭제할 게시물의 ID
     * @param jwt 요청한 사용자의 ID를 확인하기 위한 JWT 객체
     * @return 성공 응답을 담은 ApiResponse
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        blogService.deletePost(postId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 특정 상품 ID(productId)와 연결된 모든 게시물(리뷰 등)을 조회합니다.
     * @param productId 조회할 상품의 ID
     * @return 해당 상품에 대한 게시물 목록을 담은 ApiResponse
     */
    @GetMapping("/reviews")
    public ApiResponse<List<PostResponse>> getPostByProductId(@RequestParam String productId) {
        return ApiResponse.success(blogService.getPostsByProductId(productId));
    }
}