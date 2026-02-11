package com.portal.universe.event.drive;

import java.time.LocalDateTime;

public record FileDeletedEvent(
        String fileId,
        String userId,
        LocalDateTime deletedAt
) {}
