package com.portal.universe.event.blog;

import java.time.LocalDateTime;

/**
 * 블로그 포스트에 좋아요가 추가될 때 발행되는 이벤트입니다.
 */
public record PostLikedEvent(
        String likeId,
        String postId,
        String postTitle,
        String authorId,        // 글 작성자 (알림 받을 사람)
        String likerId,         // 좋아요 누른 사람
        String likerName,
        LocalDateTime likedAt
) {}
