package com.portal.universe.blogservice.series.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 시리즈 수정 요청 DTO
 */
public record SeriesUpdateRequest(
        @NotBlank(message = "시리즈 제목은 필수입니다")
        @Size(max = 100, message = "시리즈 제목은 100자를 초과할 수 없습니다")
        String name,

        @Size(max = 500, message = "시리즈 설명은 500자를 초과할 수 없습니다")
        String description,

        String thumbnailUrl
) {}