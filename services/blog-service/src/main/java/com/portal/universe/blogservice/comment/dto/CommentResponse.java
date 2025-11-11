package com.portal.universe.blogservice.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private String id;
    private String postId;
    private String authorId;
    private String authorName;
    private String content;
    private String parentCommentId;
    private Long likeCount;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}