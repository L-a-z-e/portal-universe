package com.portal.universe.event.blog;

import java.time.LocalDateTime;

/**
 * 블로그 댓글에 대댓글(답글)이 작성될 때 발행되는 이벤트입니다.
 */
public record CommentRepliedEvent(
        String replyId,
        String postId,
        String parentCommentId,
        String parentCommentAuthorId,  // 원댓글 작성자 (알림 받을 사람)
        String replierId,               // 답글 작성자
        String replierName,
        String content,
        LocalDateTime createdAt
) {}
