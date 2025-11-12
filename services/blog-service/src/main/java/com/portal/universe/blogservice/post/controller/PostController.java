package com.portal.universe.blogservice.post.controller;

import com.portal.universe.blogservice.post.domain.PostStatus;
import com.portal.universe.blogservice.post.dto.*;
import com.portal.universe.blogservice.post.service.PostService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Post", description = "포스트 관리 및 검색 API")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시물 생성", description = "새 블로그 게시물을 작성한다.")
    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        PostResponse response = postService.createPost(request, authorId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "전체 게시물 조회(관리자용)")
    @GetMapping("/all")
    public ApiResponse<List<PostResponse>> getAllPosts() {
        List<PostResponse> posts = postService.getAllPosts();
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시물 상세 조회")
    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(
            @Parameter(description = "게시물 ID") @PathVariable String postId
    ) {
        PostResponse response = postService.getPostById(postId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "상세 조회 및 조회수 증가")
    @GetMapping("/{postId}/view")
    public ApiResponse<PostResponse> getPostWithViewIncrement(
            @Parameter(description = "게시물 ID") @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt != null ? jwt.getSubject() : null;
        PostResponse response = postService.getPostByIdWithViewIncrement(postId, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "게시물 수정")
    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @Parameter(description = "게시물 ID") @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        PostResponse response = postService.updatePost(postId, request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "게시물 삭제")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @Parameter(description = "게시물 ID") @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        postService.deletePost(postId, userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "발행 게시물 목록 (페이징)")
    @GetMapping
    public ApiResponse<Page<PostListResponse>> getPublishedPosts(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPublishedPosts(page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "작성자별 게시물 목록 조회")
    @GetMapping("/author/{authorId}")
    public ApiResponse<Page<PostListResponse>> getPostsByAuthor(
            @Parameter(description = "작성자 ID") @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByAuthor(authorId, page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "내 게시물 목록 (로그인)")
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

    @Operation(summary = "카테고리별 게시물 조회")
    @GetMapping("/category/{category}")
    public ApiResponse<Page<PostListResponse>> getPostsByCategory(
            @Parameter(description = "카테고리") @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByCategory(category, page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "태그별 게시물 조회")
    @GetMapping("/tags")
    public ApiResponse<Page<PostListResponse>> getPostsByTags(
            @Parameter(description = "태그 목록 (쉼표로 구분)") @RequestParam List<String> tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPostsByTags(tags, page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "인기 게시물 조회")
    @GetMapping("/popular")
    public ApiResponse<Page<PostListResponse>> getPopularPosts(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.getPopularPosts(page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "최근 게시물 조회")
    @GetMapping("/recent")
    public ApiResponse<List<PostListResponse>> getRecentPosts(
            @Parameter(description = "조회할 개수") @RequestParam(defaultValue = "5") int limit
    ) {
        List<PostListResponse> posts = postService.getRecentPosts(limit);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "연관 게시물 조회")
    @GetMapping("/{postId}/related")
    public ApiResponse<List<PostListResponse>> getRelatedPosts(
            @Parameter(description = "게시물 ID") @PathVariable String postId,
            @Parameter(description = "조회할 개수") @RequestParam(defaultValue = "5") int limit
    ) {
        List<PostListResponse> posts = postService.getRelatedPosts(postId, limit);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시물 단순 검색")
    @GetMapping("/search")
    public ApiResponse<Page<PostListResponse>> searchPosts(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PostListResponse> posts = postService.searchPosts(keyword, page, size);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시물 고급 검색")
    @PostMapping("/search/advanced")
    public ApiResponse<Page<PostListResponse>> searchPostsAdvanced(
            @Valid @RequestBody PostSearchRequest searchRequest
    ) {
        Page<PostListResponse> posts = postService.searchPostsAdvanced(searchRequest);
        return ApiResponse.success(posts);
    }

    @Operation(summary = "게시물 상태 변경")
    @PatchMapping("/{postId}/status")
    public ApiResponse<PostResponse> changePostStatus(
            @Parameter(description = "게시물 ID") @PathVariable String postId,
            @Valid @RequestBody PostStatusChangeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        PostResponse response = postService.changePostStatus(postId, request.newStatus(), userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "카테고리 통계 조회")
    @GetMapping("/stats/categories")
    public ApiResponse<List<CategoryStats>> getCategoryStats() {
        List<CategoryStats> stats = postService.getCategoryStats();
        return ApiResponse.success(stats);
    }

    @Operation(summary = "인기 태그 통계 조회")
    @GetMapping("/stats/tags")
    public ApiResponse<List<TagStats>> getPopularTags(
            @Parameter(description = "조회할 개수") @RequestParam(defaultValue = "10") int limit
    ) {
        List<TagStats> tags = postService.getPopularTags(limit);
        return ApiResponse.success(tags);
    }

    @Operation(summary = "작성자 통계 조회")
    @GetMapping("/stats/author/{authorId}")
    public ApiResponse<AuthorStats> getAuthorStats(
            @Parameter(description = "작성자 ID") @PathVariable String authorId
    ) {
        AuthorStats stats = postService.getAuthorStats(authorId);
        return ApiResponse.success(stats);
    }

    @Operation(summary = "전체 블로그 통계 조회")
    @GetMapping("/stats/blog")
    public ApiResponse<BlogStats> getBlogStats() {
        BlogStats stats = postService.getBlogStats();
        return ApiResponse.success(stats);
    }

    @Operation(summary = "상품별 게시물 조회(기존 API 호환성)")
    @GetMapping("/product/{productId}")
    public ApiResponse<List<PostResponse>> getPostsByProductId(
            @Parameter(description = "상품 ID") @PathVariable String productId
    ) {
        List<PostResponse> posts = postService.getPostsByProductId(productId);
        return ApiResponse.success(posts);
    }
}