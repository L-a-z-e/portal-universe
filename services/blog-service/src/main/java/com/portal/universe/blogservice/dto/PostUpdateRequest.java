package com.portal.universe.blogservice.dto;

public record PostUpdateRequest(
        String title,
        String content
) {
}
