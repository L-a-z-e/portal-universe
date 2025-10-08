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

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public ApiResponse<PostResponse> createPost(
            @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Jwt jwt // jwt 인증 정보를 직접 받아옴
            ) {
        String authorId = jwt.getSubject();
        return ApiResponse.success(blogService.createPost(request, authorId));
    }

    @GetMapping
    public ApiResponse<List<PostResponse>> getAllPosts() {
        return ApiResponse.success(blogService.getAllPosts());
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostResponse> getPostById(@PathVariable String postId) {
        return ApiResponse.success(blogService.getPostById(postId));
    }

    @PutMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String postId,
            @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        return ApiResponse.success(blogService.updatePost(postId, request, userId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        blogService.deletePost(postId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/reviews")
    public ApiResponse<List<PostResponse>> getPostByProductId(@RequestParam String productId) {
        return ApiResponse.success(blogService.getPostsByProductId(productId));
    }
}
