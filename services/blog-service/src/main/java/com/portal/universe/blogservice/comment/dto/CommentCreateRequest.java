package com.portal.universe.blogservice.comment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 댓글 생성 요청 DTO
 */
public record CommentCreateRequest(
        @NotBlank(message = "게시물 ID는 필수입니다")
        String postId,

        String parentCommentId, // null이면 루트 댓글

        @NotBlank(message = "댓글 내용은 필수입니다")
        String content
) {}