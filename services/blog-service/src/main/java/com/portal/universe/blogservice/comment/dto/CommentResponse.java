package com.portal.universe.blogservice.comment.dto;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 */
public record CommentResponse(
        String id,
        String postId,
        String authorId,
        String authorUsername,
        String authorNickname,
        String content,
        String parentCommentId,
        Long likeCount,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}