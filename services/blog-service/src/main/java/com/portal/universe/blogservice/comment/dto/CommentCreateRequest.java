package com.portal.universe.blogservice.comment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CommentCreateRequest {
    @NotBlank
    private String postId;

    private String parentCommentId; // null이면 root

    @NotBlank
    private String content;
}