package com.portal.universe.blogservice.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 요청 DTO
 */
public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(max = 2000, message = "댓글은 2000자를 초과할 수 없습니다")
        String content
) {}
