package com.portal.universe.common.event.shopping;

import java.time.LocalDateTime;

/**
 * 타임딜 시작 시 발행되는 이벤트입니다.
 */
public record TimeDealStartedEvent(
        Long timeDealId,
        String timeDealName,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
