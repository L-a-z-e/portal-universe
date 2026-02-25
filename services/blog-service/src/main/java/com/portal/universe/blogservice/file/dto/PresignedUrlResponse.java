package com.portal.universe.blogservice.file.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private String uploadUrl;

    private String objectUrl;

    private String key;

    private int expiresInSeconds;
}
