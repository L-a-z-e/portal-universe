package com.portal.universe.event.blog;

import java.time.LocalDateTime;

/**
 * 사용자가 다른 사용자를 팔로우했을 때 발행되는 이벤트입니다.
 */
public record UserFollowedEvent(
        String followId,
        String followeeId,      // 팔로우 받은 사람 (알림 받을 사람)
        String followerId,      // 팔로우 한 사람
        String followerName,
        LocalDateTime followedAt
) {}
