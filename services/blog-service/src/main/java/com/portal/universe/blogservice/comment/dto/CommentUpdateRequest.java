package com.portal.universe.blogservice.comment.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CommentUpdateRequest {
    @NotBlank
    private String content;
}