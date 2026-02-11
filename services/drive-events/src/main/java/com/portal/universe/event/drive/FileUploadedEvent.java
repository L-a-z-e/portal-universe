package com.portal.universe.event.drive;

import java.time.LocalDateTime;

public record FileUploadedEvent(
        String fileId,
        String fileName,
        String userId,
        long fileSize,
        String contentType,
        LocalDateTime uploadedAt
) {}
