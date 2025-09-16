package com.portal.universe.blogservice.dto;

public record PostCreateRequest(
        String title,
        String content
) {}
