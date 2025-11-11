package com.portal.universe.blogservice.series.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 시리즈 내 포스트 순서 변경 요청 DTO
 */
public record SeriesPostOrderRequest(
        @NotEmpty(message = "포스트 ID 목록은 필수입니다")
        List<String> postIds
) {}