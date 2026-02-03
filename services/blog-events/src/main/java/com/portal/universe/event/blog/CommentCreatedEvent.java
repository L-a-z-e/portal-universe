package com.portal.universe.event.blog;

import java.time.LocalDateTime;

/**
 * 블로그 포스트에 댓글이 작성될 때 발행되는 이벤트입니다.
 */
public record CommentCreatedEvent(
        String commentId,
        String postId,
        String postTitle,
        String authorId,        // 글 작성자 (알림 받을 사람)
        String commenterId,     // 댓글 작성자
        String commenterName,
        String content,
        LocalDateTime createdAt
) {}
