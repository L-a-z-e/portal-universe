package com.portal.universe.blogservice.comment.controller;

import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.service.CommentService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiResponse<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        String authorName = jwt.getClaim("name");
        CommentResponse response = commentService.createComment(request, authorId, authorName);
        return ApiResponse.success(response);
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        CommentResponse response = commentService.updateComment(commentId, request, authorId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable String commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String authorId = jwt.getSubject();
        commentService.deleteComment(commentId, authorId);
        return ApiResponse.success(null);
    }

    @GetMapping("/post/{postId}")
    public ApiResponse<List<CommentResponse>> getCommentsByPostId(@PathVariable String postId) {
        List<CommentResponse> responses = commentService.getCommentsByPostId(postId);
        return ApiResponse.success(responses);
    }
}
