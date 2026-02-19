package com.portal.universe.blogservice.comment.controller;

import com.portal.universe.blogservice.comment.dto.*;
import com.portal.universe.blogservice.comment.service.CommentService;
import com.portal.universe.commonlibrary.response.ApiResponse;
import com.portal.universe.commonlibrary.security.context.AuthUser;
import com.portal.universe.commonlibrary.security.context.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "Comment", description = "댓글 및 대댓글 관리 API")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "포스트에 댓글 또는 대댓글을 작성합니다.")
    @PostMapping
    public ApiResponse<CommentResponse> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @CurrentUser AuthUser user
    ) {
        CommentResponse response = commentService.createComment(request, user.uuid(), user.name(), user.nickname());
        return ApiResponse.success(response);
    }

    @Operation(summary = "댓글 수정", description = "작성한 댓글을 수정합니다. 본인만 수정 가능합니다.")
    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable String commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            @CurrentUser AuthUser user
    ) {
        CommentResponse response = commentService.updateComment(commentId, request, user.uuid());
        return ApiResponse.success(response);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다(소프트 삭제). 본인만 삭제 가능합니다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable String commentId,
            @CurrentUser AuthUser user
    ) {
        commentService.deleteComment(commentId, user.uuid());
        return ApiResponse.success(null);
    }

    @Operation(summary = "포스트별 댓글 목록 조회", description = "특정 포스트의 모든 댓글(대댓글 포함)을 조회합니다.")
    @GetMapping("/post/{postId}")
    public ApiResponse<List<CommentResponse>> getCommentsByPostId(
            @Parameter(description = "포스트 ID") @PathVariable String postId
    ) {
        List<CommentResponse> responses = commentService.getCommentsByPostId(postId);
        return ApiResponse.success(responses);
    }
}
