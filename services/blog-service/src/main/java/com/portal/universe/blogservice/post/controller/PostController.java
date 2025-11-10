package com.portal.universe.blogservice.post.controller;

import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.service.PostService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 블로그 게시물(Post) API Controller
 * RESTful API 설계 원칙에 따라 구성
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ==================== 기본 CRUD ====================

    /**
     * 게시물 생성
     * POST /api/posts
     */
    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        PostResponse response = postService.createPost(request, authorId);
        return ApiResponse.success(response);
    }

    /**
     * 전체 게시물 조회 (관리자용)
     * GET /api/posts/all
     */
    @GetMapping("/all")
    public ApiResponse<List<PostResponse>> getAllPosts() {
        List<PostResponse> posts = postService.getAllPosts();
        return ApiResponse.success(posts);
    }

    /**
     * 게시물 상세 조회
     * GET /api/posts/{postId}
     */
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(@PathVariable String postId) {
        PostResponse response = postService.getPostById(postId);
        return ApiResponse.success(response);
    }

    /**
     * 게시물 상세 조회 (조회수 증가)
     * GET /api/posts/{postId}/view
     */
    @GetMapping("/{postId}/view")
    public ApiResponse<PostResponse> getPostWithViewIncrement(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt != null ? jwt.getSubject() : null;
        PostResponse response = postService.getPostByIdWithViewIncrement(postId, userId);
        return ApiResponse.success(response);
    }

    /**
     * 게시물 수정
     * PUT /api/posts/{postId}
     */
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        PostResponse response = postService.updatePost(postId, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * 게시물 삭제
     * DELETE /api/posts/{postId}
     */
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        postService.deletePost(postId, userId);
        return ApiResponse.success(null);
    }

    // ==================== 게시물 목록 조회 ====================

    /**
     * 발행된 게시물 목록 (페이징)
     * GET /api/posts?page=0&size=10
     */
    @GetMapping
    public ApiResponse<Page<PostListResponse>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPublishedPosts(page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 작성자별 게시물 조회
     * GET /api/posts/author/{authorId}?page=0&size=10
     */
    @GetMapping("/author/{authorId}")
    public ApiResponse<Page<PostListResponse>> getPostsByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByAuthor(authorId, page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 내 게시물 조회 (로그인 사용자)
     * GET /api/posts/my?status=DRAFT&page=0&size=10
     */
    @GetMapping("/my")
    public ApiResponse<Page<PostListResponse>> getMyPosts(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        if (status != null) {
            Page<PostListResponse> posts = postService.getPostsByAuthorAndStatus(authorId, status, page, size);
            return ApiResponse.success(posts);
        } else {
            Page<PostListResponse> posts = postService.getPostsByAuthor(authorId, page, size);
            return ApiResponse.success(posts);
        }
    }

    /**
     * 카테고리별 게시물 조회
     * GET /api/posts/category/{category}?page=0&size=10
     */
    @GetMapping("/category/{category}")
    public ApiResponse<Page<PostListResponse>> getPostsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByCategory(category, page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 태그별 게시물 조회
     * GET /api/posts/tags?tags=vue,spring&page=0&size=10
     */
    @GetMapping("/tags")
    public ApiResponse<Page<PostListResponse>> getPostsByTags(
            @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByTags(tags, page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 인기 게시물 조회
     * GET /api/posts/popular?page=0&size=10
     */
    @GetMapping("/popular")
    public ApiResponse<Page<PostListResponse>> getPopularPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPopularPosts(page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 최근 게시물 조회
     * GET /api/posts/recent?limit=5
     */
    @GetMapping("/recent")
    public ApiResponse<List<PostListResponse>> getRecentPosts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<PostListResponse> posts = postService.getRecentPosts(limit);
        return ApiResponse.success(posts);
    }

    /**
     * 관련 게시물 조회
     * GET /api/posts/{postId}/related?limit=5
     */
    @GetMapping("/{postId}/related")
    public ApiResponse<List<PostListResponse>> getRelatedPosts(
            @PathVariable String postId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<PostListResponse> posts = postService.getRelatedPosts(postId, limit);
        return ApiResponse.success(posts);
    }

    // ==================== 검색 ====================

    /**
     * 간단 검색
     * GET /api/posts/search?keyword=spring&page=0&size=10
     */
    @GetMapping("/search")
    public ApiResponse<Page<PostListResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.searchPosts(keyword, page, size);
        return ApiResponse.success(posts);
    }

    /**
     * 고급 검색
     * POST /api/posts/search/advanced
     */
    @PostMapping("/search/advanced")
    public ApiResponse<Page<PostListResponse>> searchPostsAdvanced(
            @Valid @RequestBody PostSearchRequest searchRequest
    ) {
        Page<PostListResponse> posts = postService.searchPostsAdvanced(searchRequest);
        return ApiResponse.success(posts);
    }

    // ==================== 상태 관리 ====================

    /**
     * 게시물 상태 변경
     * PATCH /api/posts/{postId}/status
     */
    @PatchMapping("/{postId}/status")
    public ApiResponse<PostResponse> changePostStatus(
            @PathVariable String postId,
            @Valid @RequestBody PostStatusChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        PostResponse response = postService.changePostStatus(postId, request.newStatus(), userId);
        return ApiResponse.success(response);
    }

    // ==================== 통계 ====================

    /**
     * 카테고리 통계
     * GET /api/posts/stats/categories
     */
    @GetMapping("/stats/categories")
    public ApiResponse<List<CategoryStats>> getCategoryStats() {
        List<CategoryStats> stats = postService.getCategoryStats();
        return ApiResponse.success(stats);
    }

    /**
     * 인기 태그
     * GET /api/posts/stats/tags?limit=10
     */
    @GetMapping("/stats/tags")
    public ApiResponse<List<TagStats>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagStats> tags = postService.getPopularTags(limit);
        return ApiResponse.success(tags);
    }

    /**
     * 작성자 통계
     * GET /api/posts/stats/author/{authorId}
     */
    @GetMapping("/stats/author/{authorId}")
    public ApiResponse<AuthorStats> getAuthorStats(@PathVariable String authorId) {
        AuthorStats stats = postService.getAuthorStats(authorId);
        return ApiResponse.success(stats);
    }

    /**
     * 전체 블로그 통계
     * GET /api/posts/stats/blog
     */
    @GetMapping("/stats/blog")
    public ApiResponse<BlogStats> getBlogStats() {
        BlogStats stats = postService.getBlogStats();
        return ApiResponse.success(stats);
    }

    // ==================== 기존 호환성 ====================

    /**
     * 상품별 게시물 조회 (기존 API 호환)
     * GET /api/posts/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ApiResponse<List<PostResponse>> getPostsByProductId(@PathVariable String productId) {
        List<PostResponse> posts = postService.getPostsByProductId(productId);
        return ApiResponse.success(posts);
    }
}